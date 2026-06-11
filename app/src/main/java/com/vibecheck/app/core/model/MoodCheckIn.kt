package com.vibecheck.app.core.model

/**
 * A single anonymous mood check-in. [note] is the optional 1-5 word text.
 * [regionId] is a coarse city bucket (e.g. "us-nyc"), never coordinates.
 */
data class MoodCheckIn(
    val id: String,
    val mood: Mood,
    val note: String?,
    val timestampMillis: Long,
    val regionId: String?,
)
