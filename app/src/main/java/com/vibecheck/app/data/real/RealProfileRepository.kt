package com.vibecheck.app.data.real

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.vibecheck.app.core.model.AgeBracket
import com.vibecheck.app.core.model.ProfileState
import com.vibecheck.app.core.model.UserProfile
import com.vibecheck.app.data.ProfileRepository
import com.vibecheck.app.data.local.ProfilePreferences
import com.vibecheck.app.data.local.VibeCheckDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Anonymous-auth backed profile. The Firestore user doc holds only the age
 * bracket, opt-ins and activity timestamps — no email, phone, name, IP or
 * device id (SOW privacy rules).
 */
class RealProfileRepository(
    private val prefs: ProfilePreferences,
    private val db: VibeCheckDatabase,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : ProfileRepository {

    override val profileState: Flow<ProfileState> = prefs.profile.map { profile ->
        if (profile == null) ProfileState.NeedsOnboarding else ProfileState.Ready(profile)
    }

    override val currentStreak: Flow<Long> = prefs.currentStreak

    override val proTrialDaysRemaining: Flow<Long> = prefs.proTrialDaysRemaining

    override suspend fun completeOnboarding(
        ageBracket: AgeBracket,
        username: String?,
    ): Result<UserProfile> = runCatching {
        require(ageBracket != AgeBracket.UNDER_16) { "VibeCheck is for ages 16 and over." }

        val uid = auth.currentUser?.uid
            ?: auth.signInAnonymously().await().user?.uid
            ?: error("Couldn't create an anonymous session. Check your connection.")

        val now = System.currentTimeMillis()
        val profile = UserProfile(
            uid = uid,
            username = username?.takeIf { it.isNotBlank() },
            ageBracket = ageBracket,
            chatOptIn = false,
            createdAtMillis = now,
        )

        // Remote doc is best-effort: offline onboarding still succeeds.
        runCatching {
            firestore.collection("users").document(uid).set(
                mapOf(
                    "ageBracket" to ageBracket.name,
                    "chatOptIn" to false,
                    "createdAt" to now,
                    "lastActiveAt" to now,
                )
            ).await()
        }

        prefs.save(profile)
        // Start 3-day Pro trial on first onboarding
        prefs.startProTrial()
        profile
    }

    override suspend fun setChatOptIn(optIn: Boolean) {
        val current = currentProfile() ?: return
        prefs.save(current.copy(chatOptIn = optIn))
        runCatching {
            firestore.collection("users").document(current.uid)
                .update("chatOptIn", optIn, "lastActiveAt", System.currentTimeMillis())
                .await()
        }
    }

    override suspend fun setUsername(username: String?): Result<Unit> = runCatching {
        val current = currentProfile() ?: error("No profile yet.")
        prefs.save(current.copy(username = username?.takeIf { it.isNotBlank() }))
    }

    override suspend fun deleteAllMyData(): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid
        // Best-effort remote wipe; local wipe always happens.
        if (uid != null) {
            runCatching { firestore.collection("users").document(uid).delete().await() }
            runCatching { auth.currentUser?.delete()?.await() }
        }
        db.moodDao().deleteAll()
        prefs.clear()
        auth.signOut()
    }

    private suspend fun currentProfile(): UserProfile? = prefs.profile.first()
}
