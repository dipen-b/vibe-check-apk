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
import com.vibecheck.app.core.model.Quest
import com.vibecheck.app.core.model.QuestType
import com.vibecheck.app.core.model.DailyQuestState
import com.vibecheck.app.core.model.VibeLedger
import com.vibecheck.app.core.model.RegionInfo
import com.vibecheck.app.core.model.RegionMoodAggregate
import com.vibecheck.app.core.model.ResonancePost
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

    // UI preferences
    val darkMode: Flow<Boolean?>
    suspend fun setDarkMode(enabled: Boolean)

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

enum class ResonanceScope { MY_CITY, GLOBAL }

interface QuestRepository {
    /** Get today's daily quests for the user. */
    suspend fun getTodayQuests(): Result<DailyQuestState>

    /** Mark a quest as completed and award gems. */
    suspend fun completeQuest(questId: String): Result<Int> // Returns gems earned

    /** Use a Revive Orb to retry a quest (consumable IAP). */
    suspend fun useReviveOrb(questId: String): Result<Unit>

    /** Get user's gem balance and streak. */
    suspend fun getVibeLedger(): Result<VibeLedger>

    /** Get global or friend leaderboard. */
    suspend fun getLeaderboard(scope: String = "global", limit: Int = 50): Result<List<LeaderboardEntry>>
}

data class LeaderboardEntry(
    val rank: Int,
    val username: String,
    val currentStreak: Long,
    val totalGems: Int,
    val isFriend: Boolean = false,
)

interface ResonanceRepository {
    /** Fetch posts from a given region/scope (My City = user's region, Global = all). */
    suspend fun feed(regionId: String, scope: ResonanceScope, limit: Int = 50): Result<List<ResonancePost>>

    /** Submit a new anonymous post (1-5 words, max 100 chars) with optional image. [imageUri] is auto-resized to 500x500. */
    suspend fun submitPost(mood: Mood, text: String, regionId: String, imageUri: String? = null): Result<ResonancePost>

    /** Increment the resonate count on a post. */
    suspend fun resonate(postId: String): Result<Unit>
}

/** Rule-based engine choosing a ~2-minute activity for a mood (SOW "AI micro-actions"). */
interface MicroActionEngine {
    fun suggestFor(mood: Mood, recentHistory: List<MoodCheckIn>): com.vibecheck.app.core.model.MicroAction
    fun alternativesFor(mood: Mood): List<com.vibecheck.app.core.model.MicroAction>
}
