package com.vibecheck.app.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibecheck.app.core.AppConfig
import com.vibecheck.app.core.model.AgeBracket
import com.vibecheck.app.core.reminder.ReminderScheduler
import com.vibecheck.app.data.AppContainer
import com.vibecheck.app.ui.components.pressBounce
import kotlinx.coroutines.launch

private enum class OnboardingStep { WELCOME, AGE, FINISH }

@Composable
fun OnboardingScreen(container: AppContainer, onComplete: () -> Unit) {
    var step by rememberSaveable { mutableStateOf(OnboardingStep.WELCOME) }
    var ageChoice by rememberSaveable { mutableStateOf<AgeBracket?>(null) }
    var username by rememberSaveable { mutableStateOf("") }
    var reminderOptIn by rememberSaveable { mutableStateOf(false) }
    var submitting by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 32.dp),
        ) {
            ProgressDots(current = step.ordinal, total = OnboardingStep.entries.size)
            Spacer(Modifier.height(24.dp))

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    (slideInHorizontally { it / 4 } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it / 4 } + fadeOut())
                },
                label = "onboarding-step",
                modifier = Modifier.weight(1f),
            ) { current ->
                when (current) {
                    OnboardingStep.WELCOME -> WelcomeStep(onContinue = { step = OnboardingStep.AGE })
                    OnboardingStep.AGE -> AgeStep(
                        selected = ageChoice,
                        onSelect = { ageChoice = it },
                        onBack = { step = OnboardingStep.WELCOME },
                        onContinue = { step = OnboardingStep.FINISH },
                    )
                    OnboardingStep.FINISH -> FinishStep(
                        username = username,
                        onUsernameChange = { username = it.filter { ch -> ch.isLetterOrDigit() || ch == '_' }.take(20) },
                        reminderOptIn = reminderOptIn,
                        onReminderChange = { reminderOptIn = it },
                        submitting = submitting,
                        onBack = { step = OnboardingStep.AGE },
                        onFinish = {
                            val bracket = ageChoice ?: return@FinishStep
                            submitting = true
                            coroutineScope.launch {
                                val result = container.profileRepository.completeOnboarding(
                                    ageBracket = bracket,
                                    username = username.ifBlank { null },
                                )
                                submitting = false
                                result.fold(
                                    onSuccess = {
                                        if (reminderOptIn) ReminderScheduler.enable(context)
                                        onComplete()
                                    },
                                    onFailure = {
                                        snackbarHostState.showSnackbar(it.message ?: "Something went wrong.")
                                    },
                                )
                            }
                        },
                    )
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            snackbar = { Snackbar(it) },
        )
    }
}

@Composable
private fun ProgressDots(current: Int, total: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Spacer(Modifier.weight(1f))
        repeat(total) { index ->
            Box(
                Modifier
                    .size(if (index == current) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == current) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    )
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun WelcomeStep(onContinue: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.weight(1f))
        Text("💜", fontSize = 56.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "Hey. How are you, really?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "VibeCheck is the small daily honest answer — 30 seconds, no audience.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(28.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
            BulletRow(Icons.Outlined.Mood, "One tap a day to log how you're feeling")
            BulletRow(Icons.Outlined.Map, "See how the country's feeling, anonymously")
            BulletRow(Icons.Outlined.Forum, "Talk to someone on your wavelength — anonymously")
        }
        Spacer(Modifier.height(20.dp))
        PrivacyCard()
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(52.dp).pressBounce(),
        ) { Text("Get started") }
        Spacer(Modifier.height(8.dp))
        Text(
            "Not a medical device · 16+",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BulletRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PrivacyCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Anonymous — even to us", fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "No email, no phone, no real name. Built for the US and UK, " +
                        "compliant with UK Online Safety Act + US COPPA.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AgeStep(
    selected: AgeBracket?,
    onSelect: (AgeBracket) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    val under16 = selected == AgeBracket.UNDER_16
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Quick age check",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "VibeCheck is for ages ${AppConfig.MIN_AGE_YEARS} and over in the US and UK.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))

        AgeChoice("I'm 18 or over", selected == AgeBracket.EIGHTEEN_PLUS) {
            onSelect(AgeBracket.EIGHTEEN_PLUS)
        }
        Spacer(Modifier.height(10.dp))
        AgeChoice("I'm 16 or 17", selected == AgeBracket.SIXTEEN_TO_SEVENTEEN) {
            onSelect(AgeBracket.SIXTEEN_TO_SEVENTEEN)
        }
        Spacer(Modifier.height(10.dp))
        AgeChoice("I'm under 16", under16, dimmed = true) {
            onSelect(AgeBracket.UNDER_16)
        }

        if (under16) {
            Spacer(Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Text(
                    "VibeCheck isn't for under-16s yet. Take care of yourself 💜",
                    modifier = Modifier.padding(14.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }

        Spacer(Modifier.weight(1f))
        Row {
            TextButton(onClick = onBack) { Text("Back") }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onContinue,
                enabled = selected != null && !under16,
                modifier = Modifier.height(52.dp),
            ) { Text("Continue") }
        }
    }
}

@Composable
private fun AgeChoice(label: String, selected: Boolean, dimmed: Boolean = false, onClick: () -> Unit) {
    val borderColor = when {
        dimmed && selected -> MaterialTheme.colorScheme.error
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface,
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = borderColor,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            style = MaterialTheme.typography.titleMedium,
            color = if (dimmed) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun FinishStep(
    username: String,
    onUsernameChange: (String) -> Unit,
    reminderOptIn: Boolean,
    onReminderChange: (Boolean) -> Unit,
    submitting: Boolean,
    onBack: () -> Unit,
    onFinish: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Almost done",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            label = { Text("Username (optional)") },
            supportingText = { Text("Never your real name. Letters, digits, underscore.") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Daily reminder", fontWeight = FontWeight.Medium)
                    Text(
                        "A gentle nudge every day at 8:00 pm",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = reminderOptIn, onCheckedChange = onReminderChange)
            }
        }
        Spacer(Modifier.weight(1f))
        Row {
            TextButton(onClick = onBack, enabled = !submitting) { Text("Back") }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onFinish,
                enabled = !submitting,
                modifier = Modifier.height(52.dp),
            ) {
                if (submitting) CircularProgressIndicator(
                    Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp,
                ) else Text("Start vibing")
            }
        }
    }
}
