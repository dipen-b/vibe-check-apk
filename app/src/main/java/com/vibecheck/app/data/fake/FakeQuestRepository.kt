package com.vibecheck.app.data.fake

import com.vibecheck.app.core.model.DailyQuestState
import com.vibecheck.app.core.model.Mood
import com.vibecheck.app.core.model.Quest
import com.vibecheck.app.core.model.QuestType
import com.vibecheck.app.core.model.VibeLedger
import com.vibecheck.app.data.LeaderboardEntry
import com.vibecheck.app.data.QuestRepository
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeQuestRepository : QuestRepository {
    private val questState = MutableStateFlow<DailyQuestState?>(null)
    private val ledger = MutableStateFlow(VibeLedger(userId = "user-123", totalGemsEarned = 250, currentStreak = 7))

    override suspend fun getTodayQuests(): Result<DailyQuestState> = runCatching {
        // Return today's quests (or generate if not cached)
        val mood = Mood.HAPPY
        val quests = listOf(
            Quest(
                id = UUID.randomUUID().toString(),
                questNumber = 1,
                mood = mood,
                title = "Gratitude Rush",
                description = "Type 3 things you're grateful for in 60 seconds",
                questType = QuestType.GRATITUDE_TYPING,
            ),
            Quest(
                id = UUID.randomUUID().toString(),
                questNumber = 2,
                mood = mood,
                title = "Tap Dash",
                description = "Tap the dancing dots to score 50+ points",
                questType = QuestType.TAP_GAME,
            ),
            Quest(
                id = UUID.randomUUID().toString(),
                questNumber = 3,
                mood = mood,
                title = "Vibe Spread",
                description = "Record a 5-second laugh or positive sound",
                questType = QuestType.VOICE_RECORD,
            ),
        )

        val state = DailyQuestState(
            userId = "user-123",
            dateMillis = System.currentTimeMillis(),
            mood = mood,
            quests = quests,
            completedCount = 0,
            streakDays = 7,
            totalGemsEarned = 250,
        )
        questState.value = state
        state
    }

    override suspend fun completeQuest(questId: String): Result<Int> = runCatching {
        val gemsEarned = 50 // Base gem reward
        val currentState = questState.value ?: return Result.failure(IllegalStateException("No quest state"))

        // Mark quest as completed
        val updatedQuests = currentState.quests.map { q ->
            if (q.id == questId) q.copy(isCompleted = true) else q
        }
        val completedCount = updatedQuests.count { it.isCompleted }

        questState.update {
            currentState.copy(
                quests = updatedQuests,
                completedCount = completedCount,
                totalGemsEarned = currentState.totalGemsEarned + gemsEarned,
            )
        }

        // Update ledger
        ledger.update { it.copy(totalGemsEarned = it.totalGemsEarned + gemsEarned) }

        gemsEarned
    }

    override suspend fun useReviveOrb(questId: String): Result<Unit> = runCatching {
        // Subtract gem cost or mark as used
        ledger.update { it.copy(totalGemsEarned = maxOf(0, it.totalGemsEarned - 50)) }
    }

    override suspend fun getVibeLedger(): Result<VibeLedger> = runCatching {
        ledger.value
    }

    override suspend fun getLeaderboard(scope: String, limit: Int): Result<List<LeaderboardEntry>> =
        runCatching {
            listOf(
                LeaderboardEntry(rank = 1, username = "You", currentStreak = 7, totalGems = 250, isFriend = true),
                LeaderboardEntry(rank = 2, username = "Alex", currentStreak = 12, totalGems = 520, isFriend = true),
                LeaderboardEntry(rank = 3, username = "Jordan", currentStreak = 5, totalGems = 180, isFriend = false),
                LeaderboardEntry(rank = 4, username = "Morgan", currentStreak = 8, totalGems = 340, isFriend = true),
                LeaderboardEntry(rank = 5, username = "Casey", currentStreak = 3, totalGems = 95, isFriend = false),
            ).take(limit)
        }
}
