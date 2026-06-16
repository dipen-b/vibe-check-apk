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

    suspend fun clear() {
        context.profileDataStore.edit { it.clear() }
    }
}
