package com.vibecheck.app.core.model

data class ResonancePost(
    val id: String,
    val mood: Mood,
    val text: String,
    val regionId: String,
    val createdAtMillis: Long,
    val resonateCount: Int = 0,
    val authorId: String = "anon",
    val imageUrl: String? = null,
)
