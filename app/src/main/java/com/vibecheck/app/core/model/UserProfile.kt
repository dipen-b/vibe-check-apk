package com.vibecheck.app.core.model

/**
 * Stored age bracket. We deliberately never persist a birth date or year —
 * only the bracket, which is the minimum needed for gating.
 */
enum class AgeBracket { UNDER_16, SIXTEEN_TO_SEVENTEEN, EIGHTEEN_PLUS }

/**
 * The entire user identity: an opaque anonymous uid plus optional
 * non-identifying username. No email, phone, or device identifiers.
 */
data class UserProfile(
    val uid: String,
    val username: String?,
    val ageBracket: AgeBracket,
    val chatOptIn: Boolean,
    val createdAtMillis: Long,
)

sealed interface ProfileState {
    data object Loading : ProfileState
    data object NeedsOnboarding : ProfileState
    data class Ready(val profile: UserProfile) : ProfileState
}
