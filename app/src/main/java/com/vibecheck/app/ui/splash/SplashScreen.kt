package com.vibecheck.app.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibecheck.app.core.model.ProfileState
import com.vibecheck.app.ui.navigation.Routes
import com.vibecheck.app.ui.theme.Violet
import com.vibecheck.app.ui.theme.VioletDim
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(profileState: ProfileState, onNavigate: (String) -> Unit) {
    var delayDone by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(3_500)
        delayDone = true
    }

    LaunchedEffect(delayDone, profileState) {
        if (delayDone && profileState !is ProfileState.Loading) {
            onNavigate(if (profileState is ProfileState.Ready) Routes.HOME else Routes.ONBOARDING)
        }
    }

    // Fade + scale in the centre card
    val contentAlpha = remember { Animatable(0f) }
    val contentScale = remember { Animatable(0.82f) }
    LaunchedEffect(Unit) {
        contentAlpha.animateTo(1f, animationSpec = tween(700, easing = EaseOut))
        contentScale.animateTo(
            1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        )
    }

    // Continuous gentle pulse on the emoji
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "emojiPulse",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Violet, VioletDim))
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Centre content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                alpha = contentAlpha.value
                scaleX = contentScale.value
                scaleY = contentScale.value
            },
        ) {
            Text(
                text = "💜",
                fontSize = 80.sp,
                modifier = Modifier.scale(pulse),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "VibeCheck",
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = (-0.5).sp,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "How are you, really?",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.72f),
                textAlign = TextAlign.Center,
            )
        }

        // Bottom tagline
        Text(
            text = "Anonymous · Honest · 30 seconds",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 52.dp)
                .graphicsLayer { alpha = contentAlpha.value },
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.45f),
            letterSpacing = 0.4.sp,
            textAlign = TextAlign.Center,
        )
    }
}
