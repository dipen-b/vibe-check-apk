package com.vibecheck.app.ui.chat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibecheck.app.core.model.MatchState
import com.vibecheck.app.core.model.ProfileState
import com.vibecheck.app.data.AppContainer
import com.vibecheck.app.ui.theme.Violet
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@Composable
fun MatchScreen(container: AppContainer, onChatStarted: (String) -> Unit) {
    val profileState by container.profileRepository.profileState
        .collectAsStateWithLifecycle(initialValue = null)
    val profile = (profileState as? ProfileState.Ready)?.profile
    val scope = rememberCoroutineScope()

    var matching by remember { mutableStateOf(false) }
    var matchState by remember { mutableStateOf<MatchState>(MatchState.Idle) }
    var chatOptIn by remember(profile) { mutableStateOf(profile?.chatOptIn ?: false) }

    LaunchedEffect(chatOptIn, profile) {
        if (profile != null && chatOptIn != profile.chatOptIn) {
            container.profileRepository.setChatOptIn(chatOptIn)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Anonymous Match",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Chat with someone on your wavelength — no names, no photos.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))

        // Opt-in toggle card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            androidx.compose.foundation.layout.Row(
                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Enable anonymous chat", fontWeight = FontWeight.Medium)
                    Text(
                        "Opt in to be matched with others who feel similarly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = chatOptIn,
                    onCheckedChange = {
                        chatOptIn = it
                        if (!it && matching) {
                            matching = false
                            matchState = MatchState.Idle
                            scope.launch { container.chatRepository.cancelMatch() }
                        }
                    },
                )
            }
        }

        if (chatOptIn) {
            Spacer(Modifier.height(32.dp))

            when (val state = matchState) {
                is MatchState.Idle -> IdleContent(
                    onSearch = {
                        matching = true
                        matchState = MatchState.Searching
                        container.chatRepository.requestMatch()
                            .onEach { matchState = it }
                            .launchIn(scope)
                    },
                )
                is MatchState.Searching -> SearchingContent(
                    onCancel = {
                        matching = false
                        matchState = MatchState.Idle
                        scope.launch { container.chatRepository.cancelMatch() }
                    },
                )
                is MatchState.Matched -> {
                    LaunchedEffect(state.sessionId) { onChatStarted(state.sessionId) }
                }
                is MatchState.TimedOut -> TimedOutContent(onRetry = { matchState = MatchState.Idle })
                is MatchState.Failed -> ErrorContent(state.message, onRetry = { matchState = MatchState.Idle })
            }
        }

        Spacer(Modifier.weight(1f))
        HelplinesFooter()
    }
}

@Composable
private fun IdleContent(onSearch: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("💬", fontSize = 56.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            "Ready to connect?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "We match on mood within the last 2 hours. Chat auto-ends in 5 minutes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onSearch, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Find someone")
        }
    }
}

@Composable
private fun SearchingContent(onCancel: () -> Unit) {
    val infinite = rememberInfiniteTransition(label = "pulse")
    val pulse1 by infinite.animateFloat(
        initialValue = 0.6f, targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "p1",
    )
    val pulse2 by infinite.animateFloat(
        initialValue = 0.6f, targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(1200, 400, LinearEasing), RepeatMode.Restart),
        label = "p2",
    )
    val pulse3 by infinite.animateFloat(
        initialValue = 0.6f, targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(1200, 800, LinearEasing), RepeatMode.Restart),
        label = "p3",
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
            listOf(pulse1, pulse2, pulse3).forEach { scale ->
                Box(
                    Modifier
                        .size(80.dp)
                        .scale(scale)
                        .border(2.dp, Violet.copy(alpha = (1.4f - scale) / 0.8f), CircleShape),
                )
            }
            Box(
                Modifier
                    .size(48.dp)
                    .background(Violet, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("💜", fontSize = 22.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Looking for your match…",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Up to 30 seconds",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Text("Cancel")
        }
    }
}

@Composable
private fun TimedOutContent(onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("⏱️", fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text("No match found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        Text(
            "No one with a similar vibe is online right now. Try again soon.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Try again") }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("⚠️", fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Retry") }
    }
}

@Composable
fun HelplinesFooter() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "Need to talk to someone now?",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                "🇺🇸 988 Suicide & Crisis Lifeline — call or text 988",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "🇬🇧 Samaritans — call 116 123 (free, 24/7)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
