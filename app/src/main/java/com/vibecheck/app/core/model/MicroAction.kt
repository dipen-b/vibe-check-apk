package com.vibecheck.app.core.model

enum class ActionCategory { BREATHING, MOVEMENT, SOCIAL, GRATITUDE, REST, CREATIVE }

/** An AI-suggested ~2-minute activity shown after a mood check-in. */
data class MicroAction(
    val id: String,
    val title: String,
    val description: String,
    val durationMinutes: Int,
    val category: ActionCategory,
)
