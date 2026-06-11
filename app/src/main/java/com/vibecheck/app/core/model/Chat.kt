package com.vibecheck.app.core.model

sealed interface MatchState {
    data object Idle : MatchState
    data object Searching : MatchState
    data class Matched(val sessionId: String) : MatchState
    data object TimedOut : MatchState
    data class Failed(val message: String) : MatchState
}

/**
 * An ephemeral 5-minute chat session. Sessions auto-close at
 * [expiresAtMillis]; the backend deletes all messages shortly after.
 */
data class ChatSession(
    val id: String,
    val startedAtMillis: Long,
    val expiresAtMillis: Long,
    val closed: Boolean,
    val peerMood: Mood?,
)

data class ChatMessage(
    val id: String,
    val sessionId: String,
    val fromMe: Boolean,
    val text: String,
    val sentAtMillis: Long,
)
