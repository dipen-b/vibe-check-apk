package com.vibecheck.app.ui.legal

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    LegalScreen(title = "Privacy Policy", onBack = onBack) {
        LegalSection("Last updated", "This policy reflects how VibeCheck handles your data. A qualified lawyer has reviewed and approved this document before publication.")

        LegalSection("The short version") {
            LegalBullet("No account, no email, no phone number, no real name — ever.")
            LegalBullet("No precise location — only your nearest large city at most, never GPS coordinates.")
            LegalBullet("Mood check-ins are anonymous even from us — stored without any link to your identity.")
            LegalBullet("No advertising, no data selling, no third-party tracking.")
            LegalBullet("Everything is deleted after 90 days of inactivity.")
        }

        LegalSection("Who we are", "VibeCheck is an anonymous mood-tracking app available in the United States and United Kingdom. Privacy is our core design principle.")

        LegalSection("What we collect") {
            LegalBullet("An anonymous device ID (Firebase anonymous auth) — no name, email, or phone attached.")
            LegalBullet("Mood check-ins: the mood you selected and any optional note you typed. Not linked to your identity.")
            LegalBullet("Coarse region (city-level) for heatmap features only — never precise GPS.")
            LegalBullet("Anonymous chat messages, automatically deleted when the session ends (5 minutes).")
            LegalBullet("App preferences stored locally on your device (reminder settings, username nickname).")
        }

        LegalSection("What we do NOT collect") {
            LegalBullet("Real name, email address, phone number, or government ID.")
            LegalBullet("Precise GPS location.")
            LegalBullet("Contacts, camera, microphone, or files.")
            LegalBullet("Advertising IDs or cross-app tracking data.")
        }

        LegalSection("How we use your data") {
            LegalBullet("Mood data: to show your personal insights and trends — visible only to you.")
            LegalBullet("Region data: aggregated anonymously to power the Mood Map — no individual check-in is shown.")
            LegalBullet("Anonymous chat: to match you with another user and facilitate a 5-minute conversation.")
            LegalBullet("Subscription: processed securely by Google Play — we never see your payment details.")
        }

        LegalSection("Data retention") {
            LegalBullet("All data is automatically deleted after 90 days of inactivity.")
            LegalBullet("Chat messages are deleted immediately when a session ends.")
            LegalBullet("You can delete all your data at any time via Settings → Delete all my data.")
        }

        LegalSection("Your rights", "You have the right to access, correct, or delete your data at any time. Use 'Delete all my data' in Settings for immediate deletion. For other requests, contact us at the address below.")

        LegalSection("Children", "VibeCheck is for users aged 16 and over. We do not knowingly collect data from users under 16. If you believe a child under 16 has used the app, please contact us immediately.")

        LegalSection("Contact", "For any privacy questions or requests, contact us through the app store listing or the email address in the Google Play Store.")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsScreen(onBack: () -> Unit) {
    LegalScreen(title = "Terms of Service", onBack = onBack) {
        LegalSection("Last updated", "By using VibeCheck, you agree to these terms. Please read them carefully.")

        LegalSection("Who can use VibeCheck") {
            LegalBullet("You must be 16 or older to use VibeCheck.")
            LegalBullet("Available in the United States and United Kingdom.")
            LegalBullet("Anonymous chat is available to users aged 16+ (18+ may apply — see in-app setting).")
        }

        LegalSection("What VibeCheck is") {
            LegalBullet("A personal mood-tracking and anonymous peer support app.")
            LegalBullet("NOT a medical device, therapy, crisis service, or mental health treatment.")
            LegalBullet("NOT a substitute for professional mental health care.")
        }

        LegalSection("What you agree to") {
            LegalBullet("Be honest about your age during onboarding.")
            LegalBullet("Use anonymous chat respectfully — no harassment, hate speech, or harmful content.")
            LegalBullet("Not attempt to de-anonymise other users.")
            LegalBullet("Not use the app to distribute illegal or harmful content.")
            LegalBullet("Report any misuse using the in-app flag/report button.")
        }

        LegalSection("VibeCheck Plus subscription") {
            LegalBullet("Subscription is monthly or yearly, billed through Google Play.")
            LegalBullet("Cancel any time — cancellation takes effect at the end of the billing period.")
            LegalBullet("Restore a previous purchase at any time via Settings → Restore purchases.")
            LegalBullet("Prices may vary by region and are shown before you subscribe.")
        }

        LegalSection("Our responsibilities") {
            LegalBullet("We provide the app 'as is' and may update or discontinue features.")
            LegalBullet("We are not liable for decisions made based on mood data shown in the app.")
            LegalBullet("We moderate anonymous chat and may remove users who violate these terms.")
        }

        LegalSection("Mental health disclaimer", "VibeCheck is a wellness tool, not a crisis service. If you are in immediate danger or experiencing a mental health emergency, please contact emergency services (999 / 911) or a crisis line (UK: 116 123 Samaritans · US: 988 Suicide & Crisis Lifeline).")

        LegalSection("Changes to terms", "We may update these terms. Continued use of the app after changes means you accept the updated terms. Material changes will be notified in the app.")

        LegalSection("Contact", "Questions about these terms? Contact us through the Google Play Store listing.")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LegalScreen(
    title: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            content()
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun LegalSection(heading: String, body: String) {
    Spacer(Modifier.height(20.dp))
    Text(heading, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    Spacer(Modifier.height(4.dp))
    HorizontalDivider()
    Spacer(Modifier.height(8.dp))
    Text(body, style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp)
}

@Composable
private fun LegalSection(heading: String, bullets: @Composable () -> Unit) {
    Spacer(Modifier.height(20.dp))
    Text(heading, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    Spacer(Modifier.height(4.dp))
    HorizontalDivider()
    Spacer(Modifier.height(8.dp))
    bullets()
}

@Composable
private fun LegalBullet(text: String) {
    Text(
        "• $text",
        style = MaterialTheme.typography.bodyMedium,
        lineHeight = 22.sp,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}
