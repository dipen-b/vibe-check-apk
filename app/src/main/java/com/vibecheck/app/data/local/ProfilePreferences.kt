package com.vibecheck.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vibecheck.app.core.model.AgeBracket
import com.vibecheck.app.core.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.profileDataStore by preferencesDataStore(name = "vibecheck_prefs")

/**
 * On-device profile store. Holds the anonymous uid plus the few
 * non-identifying fields the app needs — nothing here can identify a person.
 */
class ProfilePreferences(private val context: Context) {

    private object Keys {
        val UID = stringPreferencesKey("uid")
        val USERNAME = stringPreferencesKey("username")
        val AGE_BRACKET = stringPreferencesKey("age_bracket")
        val CHAT_OPT_IN = booleanPreferencesKey("chat_opt_in")
        val CREATED_AT = longPreferencesKey("created_at")
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val TRIAL_CHAT_USED = booleanPreferencesKey("trial_chat_used")
        // Freemium: Streak & Trial
        val CURRENT_STREAK = longPreferencesKey("current_streak")
        val LAST_CHECKIN_DATE = longPreferencesKey("last_checkin_date")
        val LONGEST_STREAK = longPreferencesKey("longest_streak")
        val PRO_TRIAL_STARTED_AT = longPreferencesKey("pro_trial_started_at")
        val PRO_TRIAL_DAYS_REMAINING = longPreferencesKey("pro_trial_days_remaining")
        val IS_PRO_SUBSCRIBER = booleanPreferencesKey("is_pro_subscriber")
        val STREAK_SHIELDS_USED = longPreferencesKey("streak_shields_used")
        // Analytics
        val TOTAL_CHECKINS = longPreferencesKey("total_checkins")
        val TOTAL_MATCHES = longPreferencesKey("total_matches")
        val FIRST_CHECKIN_AT = longPreferencesKey("first_checkin_at")
    }

    /** Emits null until onboarding has completed. */
    val profile: Flow<UserProfile?> = context.profileDataStore.data.map { prefs ->
        if (prefs[Keys.ONBOARDED] != true) return@map null
        val uid = prefs[Keys.UID] ?: return@map null
        val bracket = prefs[Keys.AGE_BRACKET]
            ?.let { runCatching { AgeBracket.valueOf(it) }.getOrNull() }
            ?: return@map null
        UserProfile(
            uid = uid,
            username = prefs[Keys.USERNAME],
            ageBracket = bracket,
            chatOptIn = prefs[Keys.CHAT_OPT_IN] ?: false,
            createdAtMillis = prefs[Keys.CREATED_AT] ?: 0L,
        )
    }

    val trialChatUsed: Flow<Boolean> = context.profileDataStore.data.map { prefs ->
        prefs[Keys.TRIAL_CHAT_USED] ?: false
    }

    // Streak tracking
    val currentStreak: Flow<Long> = context.profileDataStore.data.map { prefs ->
        prefs[Keys.CURRENT_STREAK] ?: 0L
    }

    val longestStreak: Flow<Long> = context.profileDataStore.data.map { prefs ->
        prefs[Keys.LONGEST_STREAK] ?: 0L
    }

    // Pro subscription & trial
    val isProSubscriber: Flow<Boolean> = context.profileDataStore.data.map { prefs ->
        prefs[Keys.IS_PRO_SUBSCRIBER] ?: false
    }

    val proTrialDaysRemaining: Flow<Long> = context.profileDataStore.data.map { prefs ->
        val startedAt = prefs[Keys.PRO_TRIAL_STARTED_AT] ?: return@map 0L
        val daysElapsed = (System.currentTimeMillis() - startedAt) / (24 * 60 * 60 * 1000)
        val remaining = (3 - daysElapsed).coerceAtLeast(0)
        remaining
    }

    val streakShieldsUsed: Flow<Long> = context.profileDataStore.data.map { prefs ->
        prefs[Keys.STREAK_SHIELDS_USED] ?: 0L
    }

    // Analytics
    val totalCheckins: Flow<Long> = context.profileDataStore.data.map { prefs ->
        prefs[Keys.TOTAL_CHECKINS] ?: 0L
    }

    val totalMatches: Flow<Long> = context.profileDataStore.data.map { prefs ->
        prefs[Keys.TOTAL_MATCHES] ?: 0L
    }

    suspend fun save(profile: UserProfile) {
        context.profileDataStore.edit { prefs ->
            prefs[Keys.UID] = profile.uid
            profile.username?.let { prefs[Keys.USERNAME] = it }
                ?: prefs.remove(Keys.USERNAME)
            prefs[Keys.AGE_BRACKET] = profile.ageBracket.name
            prefs[Keys.CHAT_OPT_IN] = profile.chatOptIn
            prefs[Keys.CREATED_AT] = profile.createdAtMillis
            prefs[Keys.ONBOARDED] = true
        }
    }

    suspend fun markTrialChatUsed() {
        context.profileDataStore.edit { prefs ->
            prefs[Keys.TRIAL_CHAT_USED] = true
        }
    }

    suspend fun recordCheckIn() {
        context.profileDataStore.edit { prefs ->
            val today = System.currentTimeMillis() / (24 * 60 * 60 * 1000)
            val lastCheckinDate = prefs[Keys.LAST_CHECKIN_DATE] ?: 0L
            val currentStreak = prefs[Keys.CURRENT_STREAK] ?: 0L
            val longestStreak = prefs[Keys.LONGEST_STREAK] ?: 0L

            val newStreak = if (lastCheckinDate + 1 == today) {
                // Consecutive day
                currentStreak + 1
            } else if (lastCheckinDate == today) {
                // Already checked in today
                currentStreak
            } else {
                // Broke streak
                1L
            }

            prefs[Keys.CURRENT_STREAK] = newStreak
            prefs[Keys.LONGEST_STREAK] = maxOf(newStreak, longestStreak)
            prefs[Keys.LAST_CHECKIN_DATE] = today
            prefs[Keys.TOTAL_CHECKINS] = (prefs[Keys.TOTAL_CHECKINS] ?: 0L) + 1
            if (prefs[Keys.FIRST_CHECKIN_AT] == null) {
                prefs[Keys.FIRST_CHECKIN_AT] = System.currentTimeMillis()
            }
        }
    }

    suspend fun startProTrial() {
        context.profileDataStore.edit { prefs ->
            prefs[Keys.PRO_TRIAL_STARTED_AT] = System.currentTimeMillis()
            prefs[Keys.PRO_TRIAL_DAYS_REMAINING] = 3
        }
    }

    suspend fun activateProSubscription() {
        context.profileDataStore.edit { prefs ->
            prefs[Keys.IS_PRO_SUBSCRIBER] = true
        }
    }

    suspend fun recordMatch() {
        context.profileDataStore.edit { prefs ->
            prefs[Keys.TOTAL_MATCHES] = (prefs[Keys.TOTAL_MATCHES] ?: 0L) + 1
        }
    }

    suspend fun useStreakShield() {
        context.profileDataStore.edit { prefs ->
            prefs[Keys.STREAK_SHIELDS_USED] = (prefs[Keys.STREAK_SHIELDS_USED] ?: 0L) + 1
        }
    }

    suspend fun clear() {
        context.profileDataStore.edit { it.clear() }
    }
}
