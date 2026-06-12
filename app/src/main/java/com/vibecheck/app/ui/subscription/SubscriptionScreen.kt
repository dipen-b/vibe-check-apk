package com.vibecheck.app.ui.subscription

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibecheck.app.data.AppContainer
import com.vibecheck.app.ui.components.pressBounce
import com.vibecheck.app.ui.theme.Violet
import kotlinx.coroutines.launch

private val features = listOf(
    "30-day mood history & calendar",
    "Weekly pattern insights",
    "Best & toughest day analysis",
    "CSV export for your records",
    "Early access to new features",
)

@Composable
fun SubscriptionScreen(container: AppContainer, onBack: () -> Unit) {
    val isSubscribed by container.billingRepository.isSubscribed
        .collectAsStateWithLifecycle(initialValue = false)
    val price by container.billingRepository.monthlyPriceFormatted
        .collectAsStateWithLifecycle(initialValue = null)

    val scope = rememberCoroutineScope()
    val activity = LocalContext.current as? android.app.Activity
    var purchasing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("💜", fontSize = 52.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            "VibeCheck Plus",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Deeper insights into your emotional patterns.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))

        // Feature list
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                features.forEach { feature ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Check,
                            contentDescription = null,
                            tint = Violet,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            feature,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 10.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        if (isSubscribed) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("✓ You're on VibeCheck Plus", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "All premium features are unlocked.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        } else {
            // Price
            Text(
                price ?: "$2.99 / £2.49",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Violet,
            )
            Text(
                "per month · cancel any time",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    if (activity == null) return@Button
                    purchasing = true
                    error = null
                    scope.launch {
                        container.billingRepository.launchPurchase(activity)
                            .onFailure { e -> error = e.message }
                        purchasing = false
                    }
                },
                enabled = !purchasing,
                modifier = Modifier.fillMaxWidth().height(52.dp).pressBounce(),
                colors = ButtonDefaults.buttonColors(containerColor = Violet),
            ) {
                if (purchasing) CircularProgressIndicator(
                    Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp,
                )
                else Text("Subscribe now", fontSize = 16.sp)
            }
            Spacer(Modifier.height(10.dp))
            TextButton(
                onClick = { scope.launch { container.billingRepository.refresh() } },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Restore purchase") }
        }

        Spacer(Modifier.height(20.dp))
        TextButton(onClick = onBack) { Text("Maybe later") }

        Spacer(Modifier.height(16.dp))
        Text(
            "Payment charged to your Google Play account. Subscription auto-renews monthly unless cancelled at least 24 hours before the renewal date.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            fontSize = 10.sp,
        )
    }
}
