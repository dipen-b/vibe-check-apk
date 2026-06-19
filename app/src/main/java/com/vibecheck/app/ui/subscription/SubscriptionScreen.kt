package com.vibecheck.app.ui.subscription

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
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

private val benefits = listOf(
    "📅 Full mood history (30+ days)",
    "📊 Pattern insights & trends",
    "🎯 Best & worst day analytics",
    "📥 Export data as CSV",
    "⭐ Priority support",
)

enum class PlanType {
    MONTHLY, YEARLY
}

@Composable
fun SubscriptionScreen(container: AppContainer, onBack: () -> Unit) {
    val isSubscribed by container.billingRepository.isSubscribed
        .collectAsStateWithLifecycle(initialValue = false)
    val price by container.billingRepository.monthlyPriceFormatted
        .collectAsStateWithLifecycle(initialValue = null)

    val scope = rememberCoroutineScope()
    val activity = LocalContext.current as? android.app.Activity
    var selectedPlan by remember { mutableStateOf(PlanType.YEARLY) }
    var purchasing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    if (isSubscribed) {
        SubscribedView(onBack = onBack)
    } else {
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            // Hero Section
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("💜", fontSize = 64.sp)
                Spacer(Modifier.height(16.dp))
                Text(
                    "Understand yourself better",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Unlock deeper insights into your emotional patterns with VibeCheck Plus.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(40.dp))

            // Plan Selection
            Text(
                "Choose your plan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                EnhancedPlanCard(
                    title = "Monthly",
                    price = price ?: "$3.99",
                    period = "/month",
                    badge = null,
                    description = "Perfect for trying it out",
                    isSelected = selectedPlan == PlanType.MONTHLY,
                    onClick = { selectedPlan = PlanType.MONTHLY },
                    modifier = Modifier.weight(1f),
                )
                EnhancedPlanCard(
                    title = "Yearly",
                    price = "$29.99",
                    period = "/year",
                    badge = "BEST VALUE",
                    description = "Just $2.50/mo (Save 37%)",
                    isSelected = selectedPlan == PlanType.YEARLY,
                    onClick = { selectedPlan = PlanType.YEARLY },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(32.dp))

            // What's Included
            Text(
                "What you get",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    benefits.forEach { benefit ->
                        BenefitRow(benefit)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Trust Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = Violet.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp),
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = Violet.copy(alpha = 0.05f),
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("✨", fontSize = 24.sp)
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Used by 1000s of people",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "Real people, real insights, real growth",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // CTA Button
            error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                )
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .pressBounce(),
                colors = ButtonDefaults.buttonColors(containerColor = Violet),
                shape = RoundedCornerShape(16.dp),
            ) {
                if (purchasing) {
                    CircularProgressIndicator(
                        Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 3.dp,
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "Start your journey",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "First month free · Cancel anytime",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f),
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Restore & Back
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = { scope.launch { container.billingRepository.refresh() } },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Restore purchase", fontSize = 13.sp)
                }
                TextButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Maybe later", fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Footer
            Text(
                "Auto-renews at the plan price. Cancel anytime in Settings.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun EnhancedPlanCard(
    title: String,
    price: String,
    period: String,
    badge: String?,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.06f else 0.98f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "planScale",
    )

    val shadowElevation by animateFloatAsState(
        targetValue = if (isSelected) 20f else 8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "shadowElev",
    )

    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Violet else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "borderColor",
    )

    val borderWidth by animateFloatAsState(
        targetValue = if (isSelected) 2.5f else 1.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "borderWidth",
    )

    val bgColor by animateColorAsState(
        targetValue = if (isSelected) {
            Violet.copy(alpha = 0.15f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "bgColor",
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "textColor",
    )

    Card(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = shadowElevation.dp,
                shape = RoundedCornerShape(20.dp),
                clip = true,
            )
            .border(
                width = borderWidth.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Badge - More prominent for yearly
            if (badge != null) {
                Box(
                    Modifier
                        .background(
                            Violet,
                            shape = RoundedCornerShape(12.dp),
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            letterSpacing = 0.5.sp,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            } else {
                Spacer(Modifier.height(8.dp))
            }

            // Title
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
            )

            Spacer(Modifier.height(14.dp))

            // Price Row
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    price,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isSelected) Violet else Violet.copy(alpha = 0.8f),
                    fontSize = 28.sp,
                )
                Text(
                    period,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp),
                    fontSize = 13.sp,
                )
            }

            Spacer(Modifier.height(10.dp))

            // Description
            Text(
                description,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
            )

            // Selected Indicator
            if (isSelected) {
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .background(
                            Violet.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(10.dp),
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Violet,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            "Selected",
                            style = MaterialTheme.typography.labelSmall,
                            color = Violet,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BenefitRow(benefit: String) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            benefit,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SubscribedView(onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🎉", fontSize = 80.sp)
        Spacer(Modifier.height(24.dp))
        Text(
            "You're all set!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Welcome to VibeCheck Plus. Unlock the full power of your emotional insights.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(40.dp))
        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Violet),
        ) {
            Text("Explore Plus", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
