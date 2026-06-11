package com.vibecheck.app.core.model

data class MoodTrendPoint(
    val dayLabel: String, // "Mon", "Tue", ...
    val averageValence: Float?, // null = no check-in that day
    val dominantMood: Mood?,
)

/**
 * Weekly pattern analysis. Free tier sees the current-week trend only;
 * [premium] unlocks pattern reasons and history export (SOW subscription tier).
 */
data class WeeklyInsights(
    val weekStartMillis: Long,
    val points: List<MoodTrendPoint>,
    val bestDay: String?,
    val toughestDay: String?,
    val patternSummary: String,
    val premium: Boolean,
)
