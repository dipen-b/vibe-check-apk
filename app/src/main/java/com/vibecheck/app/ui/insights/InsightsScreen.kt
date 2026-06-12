package com.vibecheck.app.ui.insights

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibecheck.app.core.model.MoodTrendPoint
import com.vibecheck.app.core.model.WeeklyInsights
import com.vibecheck.app.data.AppContainer
import com.vibecheck.app.ui.theme.ValenceHigh
import com.vibecheck.app.ui.theme.ValenceLow
import com.vibecheck.app.ui.theme.ValenceMid
import kotlinx.coroutines.launch

@Composable
fun InsightsScreen(container: AppContainer, onUpgrade: () -> Unit) {
    var insights by remember { mutableStateOf<WeeklyInsights?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var exporting by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        container.insightsRepository.weeklyInsights().fold(
            onSuccess = { insights = it },
            onFailure = { error = it.message },
        )
        loading = false
    }

    // Builds the CSV via the repository (premium-gated there) and hands it to the
    // system share sheet as plain text — every text-capable target honours EXTRA_TEXT
    // (a text/csv MIME makes many targets drop it), and no FileProvider is needed.
    val onExportCsv: () -> Unit = export@{
        if (exporting) return@export
        exporting = true
        scope.launch {
            container.insightsRepository.exportHistoryCsv().fold(
                onSuccess = { csv ->
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "VibeCheck mood history")
                        putExtra(Intent.EXTRA_TEXT, csv)
                    }
                    runCatching {
                        context.startActivity(Intent.createChooser(send, "Export mood history"))
                    }.onFailure { snackbar.showSnackbar("No app available to share the export.") }
                },
                onFailure = { snackbar.showSnackbar(it.message ?: "Export failed.") },
            )
            exporting = false
        }
    }

    Scaffold(
        // HomeScaffold already applies the system-bar insets to this tab's content,
        // so the nested Scaffold must not add them again (would double-pad the header).
        snackbarHost = { SnackbarHost(snackbar, snackbar = { Snackbar(it) }) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            Text("Your Insights", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text(
                "This week at a glance.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))

            when {
                loading -> Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
                insights != null -> InsightsContent(insights!!, onUpgrade, onExportCsv, exporting)
            }
        }
    }
}

@Composable
private fun InsightsContent(
    insights: WeeklyInsights,
    onUpgrade: () -> Unit,
    onExportCsv: () -> Unit,
    exporting: Boolean,
) {
    val hasData = insights.points.any { it.averageValence != null }

    // Bar chart
    Text("Mood this week", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(12.dp))
    MoodBarChart(insights.points)

    if (!hasData) {
        Spacer(Modifier.height(12.dp))
        Text(
            "No check-ins logged this week yet — your chart fills in as you check in.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Spacer(Modifier.height(20.dp))

    // Best / Toughest chips (only meaningful once there's data)
    if (hasData) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            insights.bestDay?.let {
                AssistChip(onClick = {}, label = { Text("Best: $it") })
            }
            insights.toughestDay?.let {
                AssistChip(onClick = {}, label = { Text("Tough: $it") })
            }
        }
        Spacer(Modifier.height(20.dp))
    }

    // Pattern summary
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Pattern", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            Text(insights.patternSummary, style = MaterialTheme.typography.bodyMedium)
        }
    }
    Spacer(Modifier.height(20.dp))

    // Premium section
    PremiumSection(isPremium = insights.premium, onUpgrade = onUpgrade, onExportCsv = onExportCsv, exporting = exporting)
}

@Composable
private fun MoodBarChart(points: List<MoodTrendPoint>) {
    val maxHeight = 100.dp
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        points.forEach { point ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val valence = point.averageValence
                if (valence != null) {
                    val barHeight = (valence * 80 + 10).dp
                    val color = valenceColor(valence)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(barHeight)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(color),
                    )
                } else {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.outlineVariant),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    point.dayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PremiumSection(
    isPremium: Boolean,
    onUpgrade: () -> Unit,
    onExportCsv: () -> Unit,
    exporting: Boolean,
) {
    Text(
        "30-day history & patterns",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Medium,
    )
    Spacer(Modifier.height(10.dp))

    Box {
        // Fake preview content (always shown, blurred when not premium)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .then(if (!isPremium) Modifier.blurred() else Modifier),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Monthly overview", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text("Your mood has been improving steadily over the past 4 weeks. Mornings show higher valence than evenings.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
                Text("Streak analysis", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text("Longest check-in streak: 14 days · Best month: May · Consistency score: 87%", style = MaterialTheme.typography.bodySmall)
            }
        }

        if (!isPremium) {
            // Paywall overlay
            Box(
                Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    Text("🔒", fontSize = 28.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "VibeCheck Plus",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Unlock 30-day history, pattern insights\nand CSV export.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = onUpgrade) { Text("Upgrade — \$2.99 / £2.49") }
                }
            }
        }
    }

    // CSV export is a Plus feature; only surface the control once unlocked.
    if (isPremium) {
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onExportCsv,
            enabled = !exporting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (exporting) "Preparing export…" else "Export history (CSV)")
        }
    }
}

private fun Modifier.blurred(): Modifier = this.drawWithContent {
    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            asFrameworkPaint().maskFilter =
                android.graphics.BlurMaskFilter(30f, android.graphics.BlurMaskFilter.Blur.NORMAL)
        }
        canvas.saveLayer(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height), paint)
        this@drawWithContent.drawContent()
        canvas.restore()
    }
}

private fun valenceColor(v: Float): Color {
    val c = v.coerceIn(0f, 1f)
    return if (c < 0.5f) lerp(ValenceLow, ValenceMid, c * 2f) else lerp(ValenceMid, ValenceHigh, (c - 0.5f) * 2f)
}

private fun lerp(a: Color, b: Color, t: Float) = androidx.compose.ui.graphics.lerp(a, b, t)
