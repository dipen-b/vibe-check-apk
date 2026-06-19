package com.vibecheck.app.core.model

/**
 * User engagement & monetization metrics for freemium analytics.
 * Tracked locally in DataStore and synced to Firestore for retention/funnel analysis.
 */
data class UserMetrics(
    // Streaks
    val currentStreak: Long = 0,
    val longestStreak: Long = 0,
    val streakShieldsUsed: Long = 0,

    // Engagement
    val totalCheckins: Long = 0,
    val totalMatches: Long = 0,
    val lastCheckinAt: Long = 0,
    val firstCheckinAt: Long = 0,

    // Subscription
    val isProSubscriber: Boolean = false,
    val proTrialStartedAt: Long? = null,
    val proTrialDaysRemaining: Long = 0,
    val subscriptionStartedAt: Long? = null,
    val subscriptionTier: String = "free", // free | pro_monthly | pro_yearly

    // Purchases
    val totalSpentUSD: Double = 0.0,
    val potionsPurchased: Long = 0,
    val lastPurchaseAt: Long? = null,

    // Churn signals
    val daysSinceLastCheckin: Long = 0,
    val is7DayChurnRisk: Boolean = false,
    val is30DayInactive: Boolean = false,

    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

/**
 * Events fired for analytics tracking.
 * Send to Firebase Analytics for funnel / retention / LTV analysis.
 */
sealed class AnalyticsEvent {
    data class CheckinSubmitted(val mood: String, val streakLength: Long) : AnalyticsEvent()
    data class MatchFound(val peerMood: String, val matchDurationSeconds: Long) : AnalyticsEvent()
    data class MatchEnded(val wasContinued: Boolean, val didReMatch: Boolean) : AnalyticsEvent()
    data class ProTrialStarted(val source: String) : AnalyticsEvent() // "onboarding" | "sad_day_trigger" | etc
    data class ProTrialExpired(val didConvert: Boolean) : AnalyticsEvent()
    data class PurchaseInitiated(val productId: String, val price: String) : AnalyticsEvent()
    data class PurchaseCompleted(val productId: String, val price: String) : AnalyticsEvent()
    data class StreakShieldUsed(val streakLength: Long) : AnalyticsEvent()
    data class PotionPurchased(val potionType: String, val price: Double) : AnalyticsEvent()
    data class UpgradePromptShown(val trigger: String) : AnalyticsEvent() // "first_match_tease" | "sad_day" | "streak_saver"
    data class UpgradePromptClicked(val trigger: String, val didConvert: Boolean) : AnalyticsEvent()
    data class StreakLost(val streakLength: Long) : AnalyticsEvent()
}
