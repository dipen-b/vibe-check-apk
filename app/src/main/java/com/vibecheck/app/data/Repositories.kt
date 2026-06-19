package com.vibecheck.app.data

import android.app.Activity
import com.vibecheck.app.core.model.AgeBracket
import com.vibecheck.app.core.model.ChatMessage
import com.vibecheck.app.core.model.ChatSession
import com.vibecheck.app.core.model.HeatmapScope
import com.vibecheck.app.core.model.MatchState
import com.vibecheck.app.core.model.Mood
import com.vibecheck.app.core.model.MoodCheckIn
import com.vibecheck.app.core.model.PreviousMatch
import com.vibecheck.app.core.model.ProfileState
import com.vibecheck.app.core.model.RegionInfo
import com.vibecheck.app.core.model.RegionMoodAggregate
import com.vibecheck.app.core.model.UserProfile
import com.vibecheck.app.core.model.WeeklyInsights
import kotlinx.coroutines.flow.Flow

/**
 * Contracts every feature is written against. UI code must depend only on
 * these interfaces (obtained from [AppContainer]) — never on implementations.
 */
interface ProfileRepository {
    val profileState: Flow<ProfileState>

    // Freemium metrics
    val currentStreak: Flow<Long>
    val proTrialDaysRemaining: Flow<Long>

    /** Rejects [AgeBracket.UNDER_16]. [username] is optional and non-identifying. */
    suspend fun completeOnboarding(ageBracket: AgeBracket, username: String?): Result<UserProfile>

    suspend fun setChatOptIn(optIn: Boolean)
    suspend fun setUsername(username: String?): Result<Unit>

    /** Erases everything, locally and remotely, and returns to onboarding. */
    suspend fun deleteAllMyData(): Result<Unit>
}

interface MoodRepository {
    /** Today's check-in, or null if the user hasn't checked in yet. */
    val todayCheckIn: Flow<MoodCheckIn?>

    /** Local history, newest first, up to [AppConfig.HISTORY_DAYS] days. */
    fun history(): Flow<List<MoodCheckIn>>

    /** Validates the 1-5 word note, stamps time + coarse region, stores + syncs. */
    suspend fun submitCheckIn(mood: Mood, note: String?): Result<MoodCheckIn>
}

interface HeatmapRepository {
    suspend fun aggregates(scope: HeatmapScope): Result<List<RegionMoodAggregate>>

    /** Coarse-location lookup snapped to the nearest shipped city bucket. */
    suspend fun resolveMyRegion(): Result<RegionInfo>
}

interface ChatRepository {
    /** Opt-in matching. Emits Searching, then Matched/TimedOut/Failed. */
    fun requestMatch(): Flow<MatchState>
    suspend fun cancelMatch()

    fun sessionState(sessionId: String): Flow<ChatSession?>
    fun messages(sessionId: String): Flow<List<ChatMessage>>

    /** Applies the profanity filter before sending. */
    suspend fun sendMessage(sessionId: String, text: String): Result<Unit>
    suspend fun reportPeer(sessionId: String, reason: String): Result<Unit>
    suspend fun leaveSession(sessionId: String)

    /** Check if user can access match feature (not used trial or is premium). */
    fun canAccessMatch(): Flow<Boolean>

    /** Mark the free trial chat as used. */
    suspend fun markTrialUsed()

    /** Get AI-suggested opening messages for a matched user. */
    suspend fun getOpeningSuggestions(peerMood: Mood, userMood: Mood): Result<List<String>>

    /** Get AI-suggested replies based on chat context. */
    suspend fun getReplySuggestions(sessionId: String, lastMessage: String): Result<List<String>>

    /** Get list of previous matches user can re-connect with (Pro feature). */
    fun previousMatches(): Flow<List<PreviousMatch>>

    /** Record a match for future re-matching. */
    suspend fun savePreviousMatch(match: PreviousMatch)

    /** Request to re-match with a previous peer. */
    suspend fun requestReMatch(previousMatchSessionId: String): Result<String> // returns new sessionId
}

interface BillingRepository {
    val isSubscribed: Flow<Boolean>

    /** Localised price string (e.g. "$2.99"), null until the store responds. */
    val monthlyPriceFormatted: Flow<String?>

    suspend fun launchPurchase(activity: Activity): Result<Unit>
    suspend fun refresh()
}

interface InsightsRepository {
    /** Current-week trend; premium fields populated only when subscribed. */
    suspend fun weeklyInsights(): Result<WeeklyInsights>

    /** Premium: full history as CSV text for the share sheet. */
    suspend fun exportHistoryCsv(): Result<String>
}

/** Rule-based engine choosing a ~2-minute activity for a mood (SOW "AI micro-actions"). */
interface MicroActionEngine {
    fun suggestFor(mood: Mood, recentHistory: List<MoodCheckIn>): com.vibecheck.app.core.model.MicroAction
    fun alternativesFor(mood: Mood): List<com.vibecheck.app.core.model.MicroAction>
}
