package com.vibecheck.app.core.model

/**
 * Represents a previous anonymous match that a user can re-connect with.
 * Stored locally for re-match requests.
 */
data class PreviousMatch(
    val sessionId: String,
    val peerMood: Mood,
    val matchedAt: Long,
    val duration: Long, // in seconds
    val wasPositive: Boolean = false, // user rated it positively
)
