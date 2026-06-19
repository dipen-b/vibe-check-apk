package com.vibecheck.app.ui.gauntlet

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibecheck.app.core.model.DailyQuestState
import com.vibecheck.app.core.model.Quest
import com.vibecheck.app.core.model.VibeLedger
import com.vibecheck.app.data.AppContainer
import com.vibecheck.app.data.LeaderboardEntry
import com.vibecheck.app.ui.theme.Violet
import kotlinx.coroutines.launch

@Composable
fun GauntletScreen(container: AppContainer) {
    var questState by remember { mutableStateOf<DailyQuestState?>(null) }
    var ledger by remember { mutableStateOf<VibeLedger?>(null) }
    var leaderboard by remember { mutableStateOf<List<LeaderboardEntry>?>(null) }
    var loading by remember { mutableStateOf(true) }
    var selectedQuestId by remember { mutableStateOf<String?>(null) }
    var showLeaderboard by remember { mutableStateOf(false) }
    var leaderboardScope by remember { mutableStateOf("global") } // global or friends

    val scope = rememberCoroutineScope()

    // Load quests and leaderboard on screen entry
    LaunchedEffect(Unit) {
        scope.launch {
            container.questRepository.getTodayQuests().onSuccess { qs ->
                questState = qs
            }
            container.questRepository.getVibeLedger().onSuccess { l ->
                ledger = l
            }
            container.questRepository.getLeaderboard(scope = leaderboardScope, limit = 100).onSuccess { lb ->
                leaderboard = lb
            }
            loading = false
        }
    }

    if (loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val state = questState ?: return
    val completedCount = state.quests.count { it.isCompleted }
    val progressPercent = completedCount / 3f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Header
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Violet.copy(alpha = 0.1f))
                    .padding(20.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (showLeaderboard) "🏆 LEADERBOARD" else "⚔️ THE GAUNTLET",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Violet,
                        )
                        Text(
                            if (showLeaderboard) "Global rankings" else "Slay today's mood quests to build your streak",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedButton(onClick = { showLeaderboard = !showLeaderboard }) {
                        Text(if (showLeaderboard) "Quests" else "Rankings")
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (!showLeaderboard) {
                    // Progress Bar
                    Column {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "Daily Progress: $completedCount/3",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "${(progressPercent * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (completedCount == 3) Color(0xFF10B981) else Violet,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progressPercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (completedCount == 3) Color(0xFF10B981) else Violet,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Streak & Gems Stats
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                        modifier = Modifier.weight(1f),
                    ) {
                        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔥", fontSize = 20.sp)
                            Text(
                                "${ledger?.currentStreak ?: 0} day",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "Streak",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                        modifier = Modifier.weight(1f),
                    ) {
                        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("💎", fontSize = 20.sp)
                            Text(
                                "${ledger?.totalGemsEarned ?: 0}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "Vibe Gems",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    }
                } else {
                    // Leaderboard Scope Selector
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = { leaderboardScope = "global" },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(if (leaderboardScope == "global") "🌍 Global" else "Global")
                        }
                        OutlinedButton(
                            onClick = { leaderboardScope = "friends" },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(if (leaderboardScope == "friends") "👥 Friends" else "Friends")
                        }
                    }
                }
            }
        }

        // Quests or Leaderboard Content
        if (!showLeaderboard) {
            // Quests
            item { Spacer(Modifier.height(16.dp)) }
            items(state.quests.size) { idx ->
            val quest = state.quests[idx]
            GauntletQuestCard(
                quest = quest,
                isSelected = selectedQuestId == quest.id,
                onTap = { selectedQuestId = if (selectedQuestId == quest.id) null else quest.id },
                onComplete = {
                    scope.launch {
                        container.questRepository.completeQuest(quest.id).onSuccess {
                            // Update local state
                            val updated = questState?.quests?.map {
                                if (it.id == quest.id) it.copy(isCompleted = true) else it
                            } ?: emptyList()
                            questState = questState?.copy(quests = updated)
                        }
                    }
                },
            )
            Spacer(Modifier.height(12.dp))
        }

            // Completion Banner
            if (completedCount == 3) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF10B981).copy(alpha = 0.1f),
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                "🎉",
                                fontSize = 40.sp,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "You Slayed the Gauntlet!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981),
                            )
                            Text(
                                "Your streak is on fire. Come back tomorrow for new quests!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        } else {
            // Leaderboard View
            if (leaderboard != null) {
                items(leaderboard!!.size) { idx ->
                    val entry = leaderboard!![idx]
                    LeaderboardEntryCard(entry, ledger?.currentStreak ?: 0)
                    Spacer(Modifier.height(8.dp))
                }
            } else {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Loading leaderboard...")
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun GauntletQuestCard(
    quest: Quest,
    isSelected: Boolean,
    onTap: () -> Unit,
    onComplete: () -> Unit,
) {
    val backgroundColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        label = "bgColor",
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(horizontal = 16.dp)
            .clickable { onTap() },
    ) {
        Column(Modifier.padding(16.dp)) {
            // Quest header
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Quest ${quest.questNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.size(8.dp))
                        if (quest.isCompleted) {
                            Text(
                                "✓ Completed",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Text(
                        quest.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    quest.mood.emoji,
                    fontSize = 24.sp,
                )
            }

            Spacer(Modifier.height(8.dp))

            // Quest description
            Text(
                quest.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(12.dp))

            // Expanded view
            if (isSelected && !quest.isCompleted) {
                Button(
                    onClick = onComplete,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Complete Quest +50 💎")
                }
            } else if (quest.isCompleted) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF10B981).copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "✓ Quest Complete",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun LeaderboardEntryCard(
    entry: LeaderboardEntry,
    userCurrentStreak: Long,
) {
    val isCurrentUser = entry.rank == 1 // Demo: rank 1 is current user
    val isTop3 = entry.rank <= 3

    Card(
        colors = CardDefaults.cardColors(
            containerColor = when {
                entry.rank == 1 -> Color(0xFFFFD700).copy(alpha = 0.15f) // Gold for #1
                entry.rank == 2 -> Color(0xFFC0C0C0).copy(alpha = 0.15f) // Silver for #2
                entry.rank == 3 -> Color(0xFFCD7F32).copy(alpha = 0.15f) // Bronze for #3
                else -> MaterialTheme.colorScheme.surface
            },
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .padding(horizontal = 16.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Rank Badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        when (entry.rank) {
                            1 -> Color(0xFFFFD700)
                            2 -> Color(0xFFC0C0C0)
                            3 -> Color(0xFFCD7F32)
                            else -> Violet
                        },
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "#${entry.rank}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }

            // User Info
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        entry.username,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (isCurrentUser) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "You",
                            style = MaterialTheme.typography.labelSmall,
                            color = Violet,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text(
                        "🔥 ${entry.currentStreak}d",
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Text(
                        "💎 ${entry.totalGems}",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }

            // Right icon
            if (isTop3) {
                Text(
                    when (entry.rank) {
                        1 -> "👑"
                        2 -> "🥈"
                        3 -> "🥉"
                        else -> "✨"
                    },
                    fontSize = 20.sp,
                )
            }
        }
    }
}
