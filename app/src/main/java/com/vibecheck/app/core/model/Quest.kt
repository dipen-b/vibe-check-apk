package com.vibecheck.app.core.model

import com.vibecheck.app.core.model.Mood

data class Quest(
    val id: String,
    val questNumber: Int, // 1, 2, or 3
    val mood: Mood,
    val title: String,
    val description: String,
    val questType: QuestType, // GRATITUDE, TAP_GAME, VOICE_RECORD, etc.
    val isCompleted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
)

enum class QuestType {
    GRATITUDE_TYPING,  // Type 3 things in 60 seconds
    TAP_GAME,          // Tap red dots to score 50+ points
    VOICE_RECORD,      // Record 5-sec laugh/vibe
    REFLECTION,        // Text prompt (sad mood)
    MOVEMENT,          // Stand up, stretch, 30 seconds
    BREATHING,         // Guided breathing exercise
}

data class DailyQuestState(
    val userId: String,
    val dateMillis: Long, // Date stamp for "today"
    val mood: Mood,
    val quests: List<Quest>,
    val completedCount: Int = 0,
    val streakDays: Long = 0,
    val totalGemsEarned: Int = 0,
    val isMegaGauntletDay: Boolean = false, // Saturday special
)

data class VibeLedger(
    val userId: String,
    val totalGemsEarned: Int = 0,
    val currentStreak: Long = 0,
    val longestStreak: Long = 0,
    val totalQuestsCompleted: Int = 0,
    val lastQuestDate: Long = 0,
)
