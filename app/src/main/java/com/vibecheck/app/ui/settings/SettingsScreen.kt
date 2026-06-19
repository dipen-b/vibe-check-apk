package com.vibecheck.app.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibecheck.app.BuildConfig
import com.vibecheck.app.core.model.AgeBracket
import com.vibecheck.app.core.model.ProfileState
import com.vibecheck.app.core.reminder.ReminderScheduler
import com.vibecheck.app.data.AppContainer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    container: AppContainer,
    onOpenSubscription: () -> Unit,
    onOpenPrivacy: () -> Unit = {},
    onOpenTerms: () -> Unit = {},
) {
    val profileState by container.profileRepository.profileState
        .collectAsStateWithLifecycle(initialValue = ProfileState.Loading)
    val profile = (profileState as? ProfileState.Ready)?.profile
    val isSubscribed by container.billingRepository.isSubscribed
        .collectAsStateWithLifecycle(initialValue = false)
    val darkModePref by container.profileRepository.darkMode
        .collectAsStateWithLifecycle(initialValue = null)
    val systemDark = isSystemInDarkTheme()
    val isDark = darkModePref ?: systemDark
    val price by container.billingRepository.monthlyPriceFormatted
        .collectAsStateWithLifecycle(initialValue = "$3.99 / £3.99")
    val todayCheckIn by container.moodRepository.todayCheckIn
        .collectAsStateWithLifecycle(initialValue = null)
    val recentHistory by container.moodRepository.history()
        .collectAsStateWithLifecycle(initialValue = emptyList())

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

    // Schedules the reminder, then flags the case where notifications are switched
    // off at the app/channel level — a granted permission still won't show anything.
    fun scheduleReminder() {
        reminderEnabled = true
        ReminderScheduler.enable(context)
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            scope.launch {
                snackbar.showSnackbar("Reminder set, but notifications are off for VibeCheck — turn them on in system settings.")
            }
        }
    }

    // On Android 13+ the reminder is useless without POST_NOTIFICATIONS, so the
    // toggle requests it before scheduling; the result drives the final state.
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            scheduleReminder()
        } else {
            reminderEnabled = false
            scope.launch {
                snackbar.showSnackbar("Allow notifications in system settings to get daily reminders.")
            }
        }
    }

    fun enableReminder() {
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            scheduleReminder()
        }
    }

    androidx.compose.material3.Scaffold(
        snackbarHost = { SnackbarHost(snackbar, snackbar = { Snackbar(it) }) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

            // Profile section
            SettingsSection(title = "Profile") {
                // Username display
                val displayName = profile?.username?.takeIf { it.isNotBlank() } ?: "Anonymous"
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .background(
                                com.vibecheck.app.ui.theme.Violet.copy(alpha = 0.15f),
                                shape = androidx.compose.foundation.shape.CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            displayName.first().uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = com.vibecheck.app.ui.theme.Violet,
                        )
                    }
                    Column {
                        Text(
                            displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        profile?.ageBracket?.let {
                            Text(
                                it.label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                androidx.compose.material3.HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                // Latest vibe
                val lastCheckIn = todayCheckIn ?: recentHistory.firstOrNull()
                if (lastCheckIn != null) {
                    val isToday = todayCheckIn != null
                    val dayLabel = if (isToday) "Today's vibe" else "Last vibe"
                    Text(
                        dayLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(lastCheckIn.mood.emoji, fontSize = 28.sp)
                        Column {
                            Text(
                                lastCheckIn.mood.label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (!lastCheckIn.note.isNullOrBlank()) {
                                Text(
                                    "\"${lastCheckIn.note}\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    androidx.compose.material3.HorizontalDivider()
                    Spacer(Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = { usernameInput = it.filter { c -> c.isLetterOrDigit() || c == '_' }.take(20) },
                    label = { Text("Change username") },
                    supportingText = { Text("Letters, digits, underscore · max 20 chars") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
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
            }

            // Appearance
            SettingsSection(title = "Appearance") {
                ToggleRow(
                    label = "Dark mode",
                    sub = if (darkModePref == null) "Following system setting"
                          else if (isDark) "Dark" else "Light",
                    checked = isDark,
                    onCheckedChange = { scope.launch { container.profileRepository.setDarkMode(it) } },
                )
            }

            // Notifications
            SettingsSection(title = "Notifications") {
                ToggleRow(
                    label = "Daily reminder",
                    sub = "Nudge at 8:00 pm every day",
                    checked = reminderEnabled,
                    onCheckedChange = { wantOn ->
                        if (wantOn) {
                            enableReminder()
                        } else {
                            reminderEnabled = false
                            ReminderScheduler.disable(context)
                        }
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
                        "Unlock 30-day history, pattern insights, and CSV export.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onOpenSubscription, modifier = Modifier.fillMaxWidth()) {
                        Text("Upgrade to Plus — ${price}/mo")
                    }
                    TextButton(
                        onClick = {
                            scope.launch {
                                container.billingRepository.refresh()
                                val restored = container.billingRepository.isSubscribed.first()
                                snackbar.showSnackbar(
                                    if (restored) "Subscription restored." else "No previous purchase found.",
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Restore purchases") }
                }
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

            // Legal
            SettingsSection(title = "Legal") {
                LegalRow(label = "Privacy Policy", onClick = onOpenPrivacy)
                Spacer(Modifier.height(4.dp))
                androidx.compose.material3.HorizontalDivider()
                Spacer(Modifier.height(4.dp))
                LegalRow(label = "Terms of Service", onClick = onOpenTerms)
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

@Composable
private fun LegalRow(label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val AgeBracket.label get() = when (this) {
    AgeBracket.UNDER_16 -> "Under 16"
    AgeBracket.SIXTEEN_TO_SEVENTEEN -> "16–17"
    AgeBracket.EIGHTEEN_PLUS -> "18+"
}
