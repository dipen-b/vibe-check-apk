package com.vibecheck.app.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import com.vibecheck.app.ui.theme.Violet
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
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            ProgressIndicator(current = step.ordinal, total = OnboardingStep.entries.size)
            Spacer(Modifier.height(32.dp))

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
                                container.profileRepository.completeOnboarding(bracket, username.ifBlank { null })
                                    .onSuccess {
                                        if (reminderOptIn) {
                                            ReminderScheduler.enable(context)
                                        }
                                        onComplete()
                                    }
                                    .onFailure {
                                        snackbarHostState.showSnackbar(it.message ?: "Error")
                                        submitting = false
                                    }
                            }
                        },
                    )
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 16.dp),
                snackbar = { Snackbar(it) },
            )
        }
    }
}

@Composable
private fun ProgressIndicator(current: Int, total: Int) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            repeat(total) { index ->
                Box(
                    Modifier
                        .height(3.dp)
                        .weight(1f)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (index <= current) Violet else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Step ${current + 1} of $total",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WelcomeStep(onContinue: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Spacer(Modifier.height(12.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("💜", fontSize = 72.sp)
            Spacer(Modifier.height(20.dp))
            Text(
                "Hey. How are you,\nreally?",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                fontSize = 32.sp,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Check in daily. See patterns. Connect with others on your wavelength.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            FeatureCard(Icons.Outlined.Mood, "Daily check-in", "One tap to log your vibe — 30 seconds")
            FeatureCard(Icons.Outlined.Map, "See patterns", "Visualize how you've been feeling over time")
            FeatureCard(Icons.Outlined.Forum, "Connect safely", "Talk to others anonymously, match by mood")
        }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            PrivacyCard()
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .pressBounce(),
                colors = ButtonDefaults.buttonColors(containerColor = Violet),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("Get started", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                "Not a medical device · Ages 16+",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@Composable
private fun FeatureCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Violet.copy(alpha = 0.08f),
        ),
        border = androidx.compose.material3.CardDefaults.outlinedCardBorder()
            .copy(brush = androidx.compose.foundation.BorderStroke(1.dp, Violet.copy(alpha = 0.2f)).brush),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Violet,
                modifier = Modifier.size(28.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PrivacyCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Outlined.Lock,
                contentDescription = null,
                tint = Violet,
                modifier = Modifier.size(24.dp),
            )
            Column {
                Text(
                    "Anonymous — even from us",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "No email, phone, or real name. Compliant with UK OSA + US COPPA.",
                    style = MaterialTheme.typography.labelSmall,
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
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                "Age verification",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "VibeCheck is for ages ${AppConfig.MIN_AGE_YEARS}+ in the US and UK.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(28.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AgeChoice(
                    "I'm 18 or older",
                    selected == AgeBracket.EIGHTEEN_PLUS,
                    enabled = true,
                ) {
                    onSelect(AgeBracket.EIGHTEEN_PLUS)
                }
                AgeChoice(
                    "I'm 16 or 17",
                    selected == AgeBracket.SIXTEEN_TO_SEVENTEEN,
                    enabled = true,
                ) {
                    onSelect(AgeBracket.SIXTEEN_TO_SEVENTEEN)
                }
                AgeChoice(
                    "I'm under 16",
                    under16,
                    enabled = true,
                    dimmed = true,
                ) {
                    onSelect(AgeBracket.UNDER_16)
                }
            }

            if (under16) {
                Spacer(Modifier.height(20.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(
                        "VibeCheck isn't available yet for under-16s. Take care 💜",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text("Back", fontSize = 16.sp)
            }
            Button(
                onClick = onContinue,
                enabled = selected != null && !under16,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Violet),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Continue", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AgeChoice(
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    dimmed: Boolean = false,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "ageScale",
    )

    val borderColor by animateColorAsState(
        targetValue = if (selected) Violet else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
        label = "ageBorder",
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .border(
                width = if (selected) 2.dp else 1.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Violet.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        ),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                label,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (dimmed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
            )
            if (selected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = Violet,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
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
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                "Almost there!",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Set up your profile and preferences.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(28.dp))

            Text(
                "Your name (optional)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            OutlinedTextField(
                value = username,
                onValueChange = onUsernameChange,
                placeholder = { Text("E.g., Alex or Kira") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            )

            Spacer(Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Violet.copy(alpha = 0.08f),
                ),
                shape = RoundedCornerShape(14.dp),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable { onReminderChange(!reminderOptIn) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Daily reminder", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(
                            "Get a nudge at 8 PM to check in",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = reminderOptIn, onCheckedChange = onReminderChange)
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onFinish,
                enabled = !submitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .pressBounce(),
                colors = ButtonDefaults.buttonColors(containerColor = Violet),
                shape = RoundedCornerShape(16.dp),
            ) {
                if (submitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text("Let's go", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Back", fontSize = 16.sp)
            }
        }
    }
}
