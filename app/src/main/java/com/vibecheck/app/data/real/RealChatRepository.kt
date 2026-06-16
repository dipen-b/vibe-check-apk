package com.vibecheck.app.data.real

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import com.vibecheck.app.core.AppConfig
import com.vibecheck.app.core.model.ChatMessage
import com.vibecheck.app.core.model.ChatSession
import com.vibecheck.app.core.model.MatchState
import com.vibecheck.app.core.model.Mood
import com.vibecheck.app.data.BillingRepository
import com.vibecheck.app.data.ChatRepository
import com.vibecheck.app.data.MoodRepository
import com.vibecheck.app.data.local.ProfilePreferences
import com.vibecheck.app.domain.chat.ProfanityFilter
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Firestore-backed anonymous chat. Matchmaking and session lifecycle go
 * through callable Cloud Functions (clients can't write chatSessions per the
 * rules); messages are written directly to the session subcollection after
 * the profanity filter cleans them.
 */
class RealChatRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    private val moodRepository: MoodRepository,
    private val prefs: ProfilePreferences,
    private val billingRepository: BillingRepository,
) : ChatRepository {

    private val uid: String? get() = auth.currentUser?.uid

    override fun requestMatch(): Flow<MatchState> = callbackFlow {
        trySend(MatchState.Searching)
        val me = uid ?: run {
            trySend(MatchState.Failed("You're not signed in."))
            close()
            return@callbackFlow
        }

        // Match on the user's most recent check-in mood (default neutral).
        val latest = runCatching { moodRepository.history().first().firstOrNull() }.getOrNull()
        val mood = latest?.mood ?: Mood.NEUTRAL

        val result = runCatching {
            functions.getHttpsCallable("requestMatch").call(
                mapOf(
                    "mood" to mood.name,
                    "valence" to mood.valence.toDouble(),
                    "regionId" to latest?.regionId,
                )
            ).await()
        }.getOrNull()

        if (result == null) {
            trySend(MatchState.Failed("Couldn't reach matchmaking. Check your connection."))
            close()
            return@callbackFlow
        }

        @Suppress("UNCHECKED_CAST")
        val data = result.getData() as? Map<String, Any?>
        val immediate = data?.get("sessionId") as? String
        if (immediate != null) {
            trySend(MatchState.Matched(immediate))
            close()
            return@callbackFlow
        }

        // Enqueued — wait for a peer to claim us, or time out.
        val queueDoc = firestore.collection("matchQueue").document(me)
        val registration = queueDoc.addSnapshotListener { snap, _ ->
            val state = snap?.getString("state")
            val sessionId = snap?.getString("sessionId")
            if (state == "matched" && sessionId != null) {
                trySend(MatchState.Matched(sessionId))
                close()
            }
        }

        val timeout = launch {
            delay(AppConfig.MATCH_TIMEOUT_SECONDS * 1000L)
            runCatching { functions.getHttpsCallable("cancelMatch").call().await() }
            trySend(MatchState.TimedOut)
            close()
        }

        awaitClose {
            timeout.cancel()
            registration.remove()
        }
    }

    override suspend fun cancelMatch() {
        runCatching { functions.getHttpsCallable("cancelMatch").call().await() }
    }

    override fun sessionState(sessionId: String): Flow<ChatSession?> = callbackFlow {
        val me = uid
        val ref = firestore.collection("chatSessions").document(sessionId)
        val registration = ref.addSnapshotListener { snap, _ ->
            trySend(snap?.let { toSession(it, me) })
        }
        awaitClose { registration.remove() }
    }

    override fun messages(sessionId: String): Flow<List<ChatMessage>> = callbackFlow {
        val me = uid
        val ref = firestore.collection("chatSessions").document(sessionId)
            .collection("messages")
            .orderBy("sentAt", Query.Direction.ASCENDING)
        val registration = ref.addSnapshotListener { snap, _ ->
            val list = snap?.documents?.map { doc ->
                ChatMessage(
                    id = doc.id,
                    sessionId = sessionId,
                    fromMe = (doc.getString("senderUid") == me),
                    text = doc.getString("text").orEmpty(),
                    sentAtMillis = doc.getLong("sentAt") ?: 0L,
                )
            }.orEmpty()
            trySend(list)
        }
        awaitClose { registration.remove() }
    }

    override suspend fun sendMessage(sessionId: String, text: String): Result<Unit> = runCatching {
        val me = uid ?: error("You're not signed in.")
        val cleaned = ProfanityFilter.clean(text.trim())
        require(cleaned.isNotBlank()) { "Message is empty." }
        firestore.collection("chatSessions").document(sessionId)
            .collection("messages").add(
                mapOf(
                    "senderUid" to me,
                    "text" to cleaned,
                    "sentAt" to System.currentTimeMillis(),
                )
            ).await()
        Unit
    }

    override suspend fun reportPeer(sessionId: String, reason: String): Result<Unit> = runCatching {
        functions.getHttpsCallable("reportPeer")
            .call(mapOf("sessionId" to sessionId, "reason" to reason)).await()
        Unit
    }

    override suspend fun leaveSession(sessionId: String) {
        runCatching {
            functions.getHttpsCallable("leaveSession")
                .call(mapOf("sessionId" to sessionId)).await()
        }
    }

    override fun canAccessMatch(): Flow<Boolean> =
        combine(prefs.trialChatUsed, billingRepository.isSubscribed) { trialUsed, isPremium ->
            !trialUsed || isPremium
        }

    override suspend fun markTrialUsed() {
        prefs.markTrialChatUsed()
    }

    private fun toSession(snap: DocumentSnapshot, me: String?): ChatSession? {
        if (!snap.exists()) return null
        @Suppress("UNCHECKED_CAST")
        val participants = snap.get("participants") as? List<String> ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val moods = snap.get("moods") as? Map<String, String> ?: emptyMap()
        val peerUid = participants.firstOrNull { it != me }
        val peerMood = peerUid?.let { moods[it] }
            ?.let { runCatching { Mood.valueOf(it) }.getOrNull() }
        return ChatSession(
            id = snap.id,
            startedAtMillis = snap.getLong("startedAt") ?: 0L,
            expiresAtMillis = snap.getLong("expiresAt") ?: 0L,
            closed = snap.getBoolean("closed") ?: false,
            peerMood = peerMood,
        )
    }
}
