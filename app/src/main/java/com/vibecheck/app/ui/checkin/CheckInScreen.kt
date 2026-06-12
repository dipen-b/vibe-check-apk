package com.vibecheck.app.ui.checkin

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import com.vibecheck.app.core.AppConfig
import com.vibecheck.app.core.model.Mood
import com.vibecheck.app.core.model.MoodCheckIn
import com.vibecheck.app.data.AppContainer
import com.vibecheck.app.ui.components.pressBounce
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun CheckInScreen(container: AppContainer, onCheckedIn: (Mood) -> Unit) {
    val today by container.moodRepository.todayCheckIn.collectAsStateWithLifecycle(initialValue = null)
    val history by container.moodRepository.history().collectAsStateWithLifecycle(initialValue = emptyList())
    var firstEmitArrived by remember { mutableStateOf(false) }
    LaunchedEffect(today, history) { firstEmitArrived = true }

    if (!firstEmitArrived) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        if (today != null) {
            AlreadyCheckedIn(checkIn = today!!, history = history)
        } else {
            NotYetCheckedIn(
                onSubmit = { mood, note ->
                    coroutineScope.launch {
                        val result = container.moodRepository.submitCheckIn(mood, note)
                        result.fold(
                            onSuccess = { onCheckedIn(mood) },
                            onFailure = {
                                snackbarHostState.showSnackbar(it.message ?: "Something went wrong.")
                            },
                        )
                    }
                },
            )
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            snackbar = { Snackbar(it) },
        )
    }
}

@Composable
private fun NotYetCheckedIn(onSubmit: (Mood, String?) -> Unit) {
    var selected by remember { mutableStateOf<Mood?>(null) }
    var note by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    val wordCount = remember(note) {
        if (note.isBlank()) 0 else note.trim().split(Regex("\\s+")).size
    }
    val noteValid = wordCount <= AppConfig.MAX_NOTE_WORDS
    val canSubmit by remember {
        derivedStateOf { selected != null && noteValid && !submitting }
    }
    val nowLabel = remember {
        val zone = ZoneId.systemDefault()
        val fmt = DateTimeFormatter.ofPattern("EEEE, d MMMM · h:mm a", Locale.UK)
        Instant.now().atZone(zone).format(fmt)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Text(
            "How's your vibe right now?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            nowLabel,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().height(220.dp),
        ) {
            items(Mood.entries) { mood ->
                MoodCell(
                    mood = mood,
                    selected = selected == mood,
                    onClick = { selected = mood },
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("Add a few words (optional)") },
            supportingText = {
                Text(
                    "$wordCount/${AppConfig.MAX_NOTE_WORDS} words",
                    color = if (noteValid) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.error,
                )
            },
            isError = !noteValid,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                val mood = selected ?: return@Button
                submitting = true
                onSubmit(mood, note.trim().ifBlank { null })
            },
            enabled = canSubmit,
            modifier = Modifier.fillMaxWidth().height(52.dp).pressBounce(),
        ) {
            if (submitting) CircularProgressIndicator(
                Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp,
            ) else Text("Check in")
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun MoodCell(mood: Mood, selected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (selected) 1.04f else 1f, label = "moodScale")
    val border by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant,
        label = "moodBorder",
    )
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.05f),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(width = if (selected) 2.dp else 1.dp, color = border),
    ) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(mood.emoji, fontSize = (32 * scale).sp)
            Spacer(Modifier.height(4.dp))
            Text(
                mood.localisedLabel,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** UK-tinged labels for mid-Atlantic warmth without changing the data model. */
private val Mood.localisedLabel: String
    get() = when (this) {
        Mood.HAPPY -> "Happy"
        Mood.NEUTRAL -> "Meh"
        Mood.SAD -> "Down"
        Mood.ANGRY -> "Angry"
        Mood.TIRED -> "Knackered"
        Mood.EXCITED -> "Buzzing"
    }

@Composable
private fun AlreadyCheckedIn(checkIn: MoodCheckIn, history: List<MoodCheckIn>) {
    val streak = remember(history) { calculateStreak(history) }
    val zone = ZoneId.systemDefault()
    val todayFmt = remember { DateTimeFormatter.ofPattern("'Today,' h:mm a", Locale.UK) }
    val whenLabel = Instant.ofEpochMilli(checkIn.timestampMillis).atZone(zone).format(todayFmt)

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Text(
            "Vibe logged ✓",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(20.dp)) {
                Text(whenLabel, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(checkIn.mood.emoji, fontSize = 40.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            checkIn.mood.localisedLabel,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium,
                        )
                        checkIn.note?.let {
                            Text(
                                "\"$it\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("🔥 $streak-day streak", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        Text(
            "Last 7 days",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        SevenDayStrip(history)
        Spacer(Modifier.weight(1f))
        Text(
            "Come back tomorrow — one vibe a day keeps it honest.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
        )
    }
}

@Composable
private fun SevenDayStrip(history: List<MoodCheckIn>) {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val days = (6 downTo 0).map { offset -> today.minusDays(offset.toLong()) }
    val byDay = remember(history) {
        history.groupBy { Instant.ofEpochMilli(it.timestampMillis).atZone(zone).toLocalDate() }
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        days.forEach { day ->
            val entry = byDay[day]?.firstOrNull()
            val isToday = day == today
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isToday) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    entry?.mood?.emoji ?: "—",
                    fontSize = 20.sp,
                    color = if (entry == null) MaterialTheme.colorScheme.outline
                    else Color.Unspecified,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    day.dayOfWeek.name.take(1),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun calculateStreak(history: List<MoodCheckIn>): Int {
    if (history.isEmpty()) return 0
    val zone = ZoneId.systemDefault()
    val dates = history
        .map { Instant.ofEpochMilli(it.timestampMillis).atZone(zone).toLocalDate() }
        .toSortedSet(reverseOrder())
    var streak = 0
    var expected = LocalDate.now(zone)
    for (date in dates) {
        if (date == expected) {
            streak++
            expected = expected.minusDays(1)
        } else if (date.isBefore(expected)) {
            break
        }
    }
    return streak
}

