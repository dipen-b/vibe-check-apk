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
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.AlertDialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibecheck.app.core.model.MatchState
import com.vibecheck.app.core.model.Mood
import com.vibecheck.app.core.model.ProfileState
import com.vibecheck.app.data.AppContainer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.vibecheck.app.ui.components.pressBounce
import com.vibecheck.app.ui.theme.Violet
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun MatchScreen(container: AppContainer, onChatStarted: (String) -> Unit, onOpenSubscription: (() -> Unit)? = null) {
    val profileState by container.profileRepository.profileState
        .collectAsStateWithLifecycle(initialValue = null)
    val profile = (profileState as? ProfileState.Ready)?.profile
    val scope = rememberCoroutineScope()

    val canAccessMatch by container.chatRepository.canAccessMatch()
        .collectAsStateWithLifecycle(initialValue = true)
    val isSubscribed by container.billingRepository.isSubscribed
        .collectAsStateWithLifecycle(initialValue = false)

    val haptic = LocalHapticFeedback.current
    var matching by remember { mutableStateOf(false) }
    var matchState by remember { mutableStateOf<MatchState>(MatchState.Idle) }
    var chatOptIn by remember(profile) { mutableStateOf(profile?.chatOptIn ?: false) }
    var showTrialModal by remember { mutableStateOf(false) }
    var openingSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedOpening by remember { mutableStateOf("") }

    LaunchedEffect(chatOptIn, profile) {
        if (profile != null && chatOptIn != profile.chatOptIn) {
            container.profileRepository.setChatOptIn(chatOptIn)
        }
    }

    // Mark trial as used when a match is successful
    LaunchedEffect(matchState) {
        if (matchState is MatchState.Matched && !isSubscribed && canAccessMatch) {
            container.chatRepository.markTrialUsed()
        }
    }

    // Load opening suggestions when a match is found
    LaunchedEffect(matchState) {
        if (matchState is MatchState.Matched) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            scope.launch {
                try {
                    val sessionId = (matchState as MatchState.Matched).sessionId
                    val session = container.chatRepository.sessionState(sessionId).first()
                    val userMood = container.moodRepository.todayCheckIn.first()?.mood ?: Mood.NEUTRAL

                    if (session != null) {
                        val result = container.chatRepository.getOpeningSuggestions(
                            peerMood = session.peerMood ?: Mood.NEUTRAL,
                            userMood = userMood,
                        )
                        result.getOrNull()?.let { suggestions ->
                            openingSuggestions = suggestions
                            if (suggestions.isNotEmpty()) {
                                selectedOpening = suggestions[0]
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Suggestions failed, continue without them
                }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Anonymous Match",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Connect with someone on your wavelength.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            androidx.compose.foundation.layout.Row(
                Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Enable chat", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Join to match with others who feel similarly",
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

            if (canAccessMatch) {
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
                        if (openingSuggestions.isNotEmpty()) {
                            MatchedWithSuggestionsContent(
                                suggestions = openingSuggestions,
                                selectedSuggestion = selectedOpening,
                                onSuggestionSelected = { selectedOpening = it },
                                onStartChat = { onChatStarted(state.sessionId) }
                            )
                        } else {
                            LaunchedEffect(state.sessionId) { onChatStarted(state.sessionId) }
                        }
                    }
                    is MatchState.TimedOut -> TimedOutContent(onRetry = { matchState = MatchState.Idle })
                    is MatchState.Failed -> ErrorContent(state.message, onRetry = { matchState = MatchState.Idle })
                }
            } else {
                TrialLimitedContent(onUpgrade = { onOpenSubscription?.invoke() })
            }
        }

        Spacer(Modifier.weight(1f))
    }

    // Trial limit modal
    if (showTrialModal) {
        AlertDialog(
            title = { Text("Free trial complete") },
            text = { Text("You've used your free trial chat. Upgrade to VibeCheck Plus for unlimited matches.") },
            onDismissRequest = { showTrialModal = false },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showTrialModal = false
                    onOpenSubscription?.invoke()
                }) { Text("Upgrade") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showTrialModal = false }) { Text("Not now") }
            },
        )
    }
}

@Composable
private fun IdleContent(onSearch: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("💬", fontSize = 60.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "Ready to talk?",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Match on mood. No names, no photos. Chat ends in 5 mins.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onSearch, modifier = Modifier.fillMaxWidth().height(52.dp).pressBounce()) {
            Text("Find someone", fontSize = 16.sp)
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
        Box(Modifier.size(140.dp), contentAlignment = Alignment.Center) {
            listOf(pulse1, pulse2, pulse3).forEach { scale ->
                Box(
                    Modifier
                        .size(90.dp)
                        .scale(scale)
                        .border(2.dp, Violet.copy(alpha = (1.4f - scale) / 0.8f), CircleShape),
                )
            }
            Box(
                Modifier
                    .size(56.dp)
                    .background(Violet, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("💜", fontSize = 24.sp)
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Finding your match",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Usually within 30 seconds",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
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
private fun MatchedWithSuggestionsContent(
    suggestions: List<String>,
    selectedSuggestion: String,
    onSuggestionSelected: (String) -> Unit,
    onStartChat: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("💬", fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            "You matched!",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Start the conversation with a great opening.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))

        // Suggested opening messages
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            suggestions.forEach { suggestion ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSuggestionSelected(suggestion) },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedSuggestion == suggestion)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.surface
                    ),
                ) {
                    Text(
                        suggestion,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp),
                        fontWeight = if (selectedSuggestion == suggestion) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onStartChat,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text("Start chat")
        }
    }
}

@Composable
private fun TrialLimitedContent(onUpgrade: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🔐", fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            "Free trial used",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "You've unlocked one free chat. Upgrade to VibeCheck Plus for unlimited matches.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onUpgrade, modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Text("Upgrade to Plus")
        }
    }
}

