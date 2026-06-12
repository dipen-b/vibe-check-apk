package com.vibecheck.app.ui.actions

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibecheck.app.core.model.ActionCategory
import com.vibecheck.app.core.model.MicroAction
import com.vibecheck.app.core.model.Mood
import com.vibecheck.app.data.AppContainer
import kotlinx.coroutines.delay

private const val DURATION_SECONDS = 120

@Composable
fun ActionsScreen(container: AppContainer, mood: Mood, onDone: () -> Unit) {
    val history by container.moodRepository.history().collectAsStateWithLifecycle(initialValue = emptyList())
    val alternatives = remember(mood) { container.microActionEngine.alternativesFor(mood) }
    var index by remember { mutableIntStateOf(0) }
    val action = alternatives[index % alternatives.size]

    var secondsLeft by remember { mutableLongStateOf(DURATION_SECONDS.toLong()) }
    var timerRunning by remember { mutableStateOf(false) }

    LaunchedEffect(timerRunning) {
        if (!timerRunning) return@LaunchedEffect
        while (secondsLeft > 0) {
            delay(1_000)
            secondsLeft--
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        // Header
        Text(
            "Your 2-min action",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
        )
        Text(
            "for feeling ${mood.name.lowercase()}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(28.dp))

        // Action card with slide animation
        AnimatedContent(
            targetState = action,
            transitionSpec = {
                (slideInHorizontally { it / 3 } + fadeIn()) togetherWith
                    (slideOutHorizontally { -it / 3 } + fadeOut())
            },
            label = "action-card",
            modifier = Modifier.weight(1f),
        ) { current ->
            ActionCard(current)
        }

        Spacer(Modifier.height(28.dp))

        // Timer
        if (timerRunning || secondsLeft < DURATION_SECONDS) {
            CountdownTimer(
                secondsLeft = secondsLeft,
                total = DURATION_SECONDS.toLong(),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(20.dp))
        }

        // Buttons
        Button(
            onClick = {
                timerRunning = true
                secondsLeft = DURATION_SECONDS.toLong()
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = !timerRunning,
        ) {
            Text(if (!timerRunning) "Start 2-min timer" else "Timer running…")
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = {
                    timerRunning = false
                    secondsLeft = DURATION_SECONDS.toLong()
                    index++
                },
                modifier = Modifier.weight(1f).height(48.dp),
            ) { Text("Try another") }
            Button(
                onClick = onDone,
                modifier = Modifier.weight(1f).height(48.dp),
            ) { Text(if (secondsLeft == 0L) "Done ✓" else "Skip") }
        }
    }
}

@Composable
private fun ActionCard(action: MicroAction) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(action.category.emoji, fontSize = 32.sp)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                action.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                action.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "~${action.durationMinutes} min · ${action.category.label}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun CountdownTimer(secondsLeft: Long, total: Long, modifier: Modifier = Modifier) {
    val progress = secondsLeft.toFloat() / total.toFloat()
    val mins = secondsLeft / 60
    val secs = secondsLeft % 60
    Box(modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(80.dp),
            strokeWidth = 6.dp,
            color = if (secondsLeft > 30) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
        )
        Text(
            "%d:%02d".format(mins, secs),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

private val ActionCategory.emoji get() = when (this) {
    ActionCategory.BREATHING -> "💨"
    ActionCategory.MOVEMENT  -> "🏃"
    ActionCategory.SOCIAL    -> "💬"
    ActionCategory.GRATITUDE -> "🙏"
    ActionCategory.REST      -> "😴"
    ActionCategory.CREATIVE  -> "🎨"
}

private val ActionCategory.label get() = name.lowercase().replaceFirstChar { it.uppercase() }
