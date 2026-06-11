package com.vibecheck.app.core.model

/**
 * The six SOW-mandated moods. [valence] maps mood to a 0..1 score used for
 * heatmap colouring (red=0, green=1) and trend analysis.
 */
enum class Mood(val emoji: String, val label: String, val valence: Float) {
    HAPPY("😊", "Happy", 0.9f),
    NEUTRAL("😐", "Meh", 0.5f),
    SAD("😔", "Down", 0.1f),
    ANGRY("😡", "Angry", 0.15f),
    TIRED("😴", "Tired", 0.35f),
    EXCITED("🥳", "Excited", 1.0f);
}
