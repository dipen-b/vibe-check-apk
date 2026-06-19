package com.vibecheck.app.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibecheck.app.data.AppContainer
import com.vibecheck.app.ui.chat.MatchScreen
import com.vibecheck.app.ui.checkin.CheckInScreen
import com.vibecheck.app.ui.components.ConfettiEffect
import com.vibecheck.app.ui.heatmap.HeatmapScreen
import com.vibecheck.app.ui.insights.InsightsScreen
import com.vibecheck.app.ui.settings.SettingsScreen

enum class HomeTab(val label: String, val icon: ImageVector) {
    CHECK_IN("Check-in", Icons.Outlined.Mood),
    HEATMAP("Heatmap", Icons.Outlined.Map),
    MATCH("Match", Icons.Outlined.Forum),
    INSIGHTS("Insights", Icons.Outlined.Insights),
    SETTINGS("Settings", Icons.Outlined.Settings),
}

@Composable
fun HomeScaffold(
    container: AppContainer,
    onCheckedIn: (com.vibecheck.app.core.model.Mood) -> Unit,
    onChatStarted: (String) -> Unit,
    onOpenSubscription: () -> Unit,
    onOpenPrivacy: () -> Unit = {},
    onOpenTerms: () -> Unit = {},
) {
    var selectedTab by rememberSaveable { mutableStateOf(HomeTab.CHECK_IN) }
    val currentStreak by container.profileRepository.currentStreak
        .collectAsStateWithLifecycle(initialValue = 0L)
    val trialDaysRemaining by container.profileRepository.proTrialDaysRemaining
        .collectAsStateWithLifecycle(initialValue = 0L)

    val greeting = remember {
        val hour = java.time.LocalTime.now().hour
        when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    // Show confetti once when hitting 7 or 30 day streak milestone
    var lastConfettiStreak by rememberSaveable { mutableStateOf(0L) }
    val showConfetti = currentStreak > 0 &&
        (currentStreak == 7L || currentStreak == 30L) &&
        lastConfettiStreak != currentStreak

    Scaffold(
        topBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        greeting,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (currentStreak > 0) {
                            Text(
                                "🔥 $currentStreak",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        if (trialDaysRemaining > 0) {
                            Text(
                                "💜 ${trialDaysRemaining}d trial",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar {
                HomeTab.entries.forEach { tab ->
                    val selected = selectedTab == tab
                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1.15f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                        label = "tab-icon-${tab.name}",
                    )
                    NavigationBarItem(
                        selected = selected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                tab.icon,
                                contentDescription = tab.label,
                                modifier = Modifier.scale(iconScale),
                            )
                        },
                        label = { Text(tab.label) },
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            AnimatedContent(

                targetState = selectedTab,
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    val dir = if (forward) 1 else -1
                    (slideInHorizontally { (it / 7) * dir } + fadeIn(tween(280)) +
                        scaleIn(initialScale = 0.97f, animationSpec = tween(280))) togetherWith
                        (slideOutHorizontally { (-it / 7) * dir } + fadeOut(tween(180)))
                },
                label = "tab-switch",
            ) { tab ->
                when (tab) {
                    HomeTab.CHECK_IN -> CheckInScreen(container, onCheckedIn)
                    HomeTab.HEATMAP -> HeatmapScreen(container)
                    HomeTab.MATCH -> MatchScreen(container, onChatStarted, onOpenSubscription)
                    HomeTab.INSIGHTS -> InsightsScreen(container, onUpgrade = onOpenSubscription)
                    HomeTab.SETTINGS -> SettingsScreen(
                        container,
                        onOpenSubscription = onOpenSubscription,
                        onOpenPrivacy = onOpenPrivacy,
                        onOpenTerms = onOpenTerms,
                    )
                }
            }

            // Confetti burst on streak milestone
            if (showConfetti) {
                ConfettiEffect(
                    trigger = true,
                    modifier = Modifier.fillMaxSize(),
                )
                lastConfettiStreak = currentStreak
            }
        }
    }
}
