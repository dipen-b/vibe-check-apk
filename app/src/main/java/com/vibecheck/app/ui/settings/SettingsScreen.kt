package com.vibecheck.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibecheck.app.BuildConfig
import com.vibecheck.app.core.model.AgeBracket
import com.vibecheck.app.core.model.ProfileState
import com.vibecheck.app.core.reminder.ReminderScheduler
import com.vibecheck.app.data.AppContainer
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(container: AppContainer, onOpenSubscription: () -> Unit) {
    val profileState by container.profileRepository.profileState
        .collectAsStateWithLifecycle(initialValue = ProfileState.Loading)
    val profile = (profileState as? ProfileState.Ready)?.profile
    val isSubscribed by container.billingRepository.isSubscribed
        .collectAsStateWithLifecycle(initialValue = false)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var reminderEnabled by remember { mutableStateOf(false) }
    var chatOptIn by remember(profile) { mutableStateOf(profile?.chatOptIn ?: false) }
    var usernameInput by remember(profile) { mutableStateOf(profile?.username ?: "") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Sync reminder state from DataStore
    val reminderFlow = remember { ReminderScheduler.isEnabledFlow(context) }
    val reminderState by reminderFlow.collectAsStateWithLifecycle(initialValue = false)
    LaunchedEffect(reminderState) { reminderEnabled = reminderState }

    androidx.compose.material3.Scaffold(
        snackbarHost = { SnackbarHost(snackbar, snackbar = { Snackbar(it) }) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)

            // Profile section
            SettingsSection(title = "Profile") {
                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = { usernameInput = it.filter { c -> c.isLetterOrDigit() || c == '_' }.take(20) },
                    label = { Text("Username (optional)") },
                    supportingText = { Text("Letters, digits, underscore · max 20 chars") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = {
                        scope.launch {
                            container.profileRepository.setUsername(usernameInput.ifBlank { null })
                                .fold(
                                    onSuccess = { snackbar.showSnackbar("Username saved.") },
                                    onFailure = { snackbar.showSnackbar(it.message ?: "Error saving username.") },
                                )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Save username") }

                profile?.ageBracket?.let { bracket ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Age bracket: ${bracket.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Notifications
            SettingsSection(title = "Notifications") {
                ToggleRow(
                    label = "Daily reminder",
                    sub = "Nudge at 8:00 pm every day",
                    checked = reminderEnabled,
                    onCheckedChange = {
                        reminderEnabled = it
                        if (it) ReminderScheduler.enable(context) else ReminderScheduler.disable(context)
                    },
                )
            }

            // Chat
            SettingsSection(title = "Anonymous chat") {
                ToggleRow(
                    label = "Enable matching",
                    sub = "Let us match you with someone on your wavelength",
                    checked = chatOptIn,
                    onCheckedChange = {
                        chatOptIn = it
                        scope.launch { container.profileRepository.setChatOptIn(it) }
                    },
                )
            }

            // Subscription
            SettingsSection(title = "VibeCheck Plus") {
                if (isSubscribed) {
                    Text(
                        "✓ Subscribed — all premium features unlocked.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    )
                } else {
                    Text(
                        "Upgrade for 30-day history, pattern insights, and CSV export.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onOpenSubscription, modifier = Modifier.fillMaxWidth()) {
                        Text("Upgrade — \$2.99 / £2.49 / mo")
                    }
                }
            }

            // Support
            SettingsSection(title = "Need support?") {
                Text(
                    "🇺🇸 988 Suicide & Crisis Lifeline — call or text 988",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "🇬🇧 Samaritans — 116 123 (free, 24/7)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Data & privacy
            SettingsSection(title = "Data & privacy") {
                Text(
                    "No email, no phone, no real name — ever. All data is deleted after 90 days of inactivity.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                ) {
                    Text("Delete all my data", color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }

            // Version
            Text(
                "VibeCheck v${BuildConfig.VERSION_NAME} · Not a medical device · 16+",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
            )
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete all your data?") },
            text = { Text("This permanently removes your profile, check-in history, and all associated data. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    scope.launch { container.profileRepository.deleteAllMyData() }
                }) { Text("Delete everything", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun ToggleRow(label: String, sub: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.Medium)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private val AgeBracket.label get() = when (this) {
    AgeBracket.UNDER_16 -> "Under 16"
    AgeBracket.SIXTEEN_TO_SEVENTEEN -> "16–17"
    AgeBracket.EIGHTEEN_PLUS -> "18+"
}
