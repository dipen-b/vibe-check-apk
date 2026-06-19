package com.vibecheck.app.data.real

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vibecheck.app.core.model.DailyQuestState
import com.vibecheck.app.core.model.Mood
import com.vibecheck.app.core.model.Quest
import com.vibecheck.app.core.model.QuestType
import com.vibecheck.app.core.model.VibeLedger
import com.vibecheck.app.data.LeaderboardEntry
import com.vibecheck.app.data.QuestRepository
import kotlinx.coroutines.tasks.await
import java.util.UUID

class RealQuestRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
) : QuestRepository {

    override suspend fun getTodayQuests(): Result<DailyQuestState> = runCatching {
        val uid = auth.currentUser?.uid ?: "anon"
        val todayKey = getTodayKey()

        // Fetch from Firestore or generate new
        val doc = firestore.collection("users").document(uid)
            .collection("daily_quests").document(todayKey).get().await()

        if (doc.exists()) {
            doc.toObject(DailyQuestState::class.java) ?: generateNewQuests(uid)
        } else {
            generateNewQuests(uid).also { state ->
                // Save to Firestore
                doc.reference.set(state).await()
            }
        }
    }

    override suspend fun completeQuest(questId: String): Result<Int> = runCatching {
        val uid = auth.currentUser?.uid ?: "anon"
        val todayKey = getTodayKey()

        val gemsEarned = 50
        firestore.collection("users").document(uid)
            .collection("daily_quests").document(todayKey)
            .update("quests", com.google.firebase.firestore.FieldValue.increment(gemsEarned.toLong())).await()

        // Update vibe ledger
        firestore.collection("users").document(uid)
            .collection("ledger").document("current")
            .update("totalGemsEarned", com.google.firebase.firestore.FieldValue.increment(gemsEarned.toLong())).await()

        gemsEarned
    }

    override suspend fun useReviveOrb(questId: String): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: "anon"

        // Deduct gem cost (50 gems per Revive Orb)
        firestore.collection("users").document(uid)
            .collection("ledger").document("current")
            .update("totalGemsEarned", com.google.firebase.firestore.FieldValue.increment(-50L)).await()
    }

    override suspend fun getVibeLedger(): Result<VibeLedger> = runCatching {
        val uid = auth.currentUser?.uid ?: "anon"

        val doc = firestore.collection("users").document(uid)
            .collection("ledger").document("current").get().await()

        doc.toObject(VibeLedger::class.java) ?: VibeLedger(userId = uid)
    }

    override suspend fun getLeaderboard(scope: String, limit: Int): Result<List<LeaderboardEntry>> =
        runCatching {
            val query = firestore.collection("leaderboard")
                .orderBy("currentStreak", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit.toLong())

            val docs = query.get().await()
            docs.documents.mapNotNull { doc ->
                try {
                    LeaderboardEntry(
                        rank = doc.getLong("rank")?.toInt() ?: 0,
                        username = doc.getString("username") ?: "Unknown",
                        currentStreak = doc.getLong("currentStreak") ?: 0,
                        totalGems = doc.getLong("totalGems")?.toInt() ?: 0,
                    )
                } catch (e: Exception) {
                    null
                }
            }
        }

    private fun generateNewQuests(uid: String): DailyQuestState {
        val moods = Mood.values()
        val mood = moods.random()
        val questTypes = when (mood) {
            Mood.SAD -> listOf(
                QuestType.GRATITUDE_TYPING,
                QuestType.REFLECTION,
                QuestType.BREATHING,
            )
            Mood.ANGRY -> listOf(
                QuestType.TAP_GAME,
                QuestType.MOVEMENT,
                QuestType.BREATHING,
            )
            Mood.HAPPY -> listOf(
                QuestType.VOICE_RECORD,
                QuestType.GRATITUDE_TYPING,
                QuestType.MOVEMENT,
            )
            else -> listOf(
                QuestType.GRATITUDE_TYPING,
                QuestType.TAP_GAME,
                QuestType.VOICE_RECORD,
            )
        }

        val quests = questTypes.mapIndexed { idx, type ->
            Quest(
                id = UUID.randomUUID().toString(),
                questNumber = idx + 1,
                mood = mood,
                title = getTitleForType(type),
                description = getDescriptionForType(type),
                questType = type,
            )
        }

        return DailyQuestState(
            userId = uid,
            dateMillis = System.currentTimeMillis(),
            mood = mood,
            quests = quests,
            completedCount = 0,
            streakDays = 0,
            totalGemsEarned = 0,
        )
    }

    private fun getTitleForType(type: QuestType): String = when (type) {
        QuestType.GRATITUDE_TYPING -> "Gratitude Rush"
        QuestType.TAP_GAME -> "Tap Dash"
        QuestType.VOICE_RECORD -> "Vibe Spread"
        QuestType.REFLECTION -> "Mind Reflection"
        QuestType.MOVEMENT -> "Energy Burst"
        QuestType.BREATHING -> "Breath Flow"
    }

    private fun getDescriptionForType(type: QuestType): String = when (type) {
        QuestType.GRATITUDE_TYPING -> "Type 3 things you're grateful for in 60 seconds"
        QuestType.TAP_GAME -> "Tap the dancing dots to score 50+ points"
        QuestType.VOICE_RECORD -> "Record a 5-second laugh or positive sound"
        QuestType.REFLECTION -> "Reflect on what's bothering you"
        QuestType.MOVEMENT -> "Stand up and stretch for 30 seconds"
        QuestType.BREATHING -> "Follow the guided breathing exercise"
    }

    private fun getTodayKey(): String {
        val today = System.currentTimeMillis()
        return (today / (24 * 60 * 60 * 1000)).toString() // Date stamp
    }
}
