package com.vibecheck.app.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibecheck.app.core.model.ProfileState
import com.vibecheck.app.ui.navigation.Routes
import com.vibecheck.app.ui.theme.Violet
import com.vibecheck.app.ui.theme.VioletDim
import kotlin.math.sin
import kotlinx.coroutines.delay

private val floatingEmojis = listOf("😊", "😴", "🥳", "😔", "😡", "😐", "💜", "😊")

@Composable
fun SplashScreen(profileState: ProfileState, onNavigate: (String) -> Unit) {
    var delayDone by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(3_800)
        delayDone = true
    }
    LaunchedEffect(delayDone, profileState) {
        if (delayDone && profileState !is ProfileState.Loading) {
            onNavigate(if (profileState is ProfileState.Ready) Routes.HOME else Routes.ONBOARDING)
        }
    }

    // ---- Entry choreography ----
    // Heart drops in with a big overshoot bounce.
    val heartScale = remember { Animatable(0f) }
    // Title + subtitle slide up & fade, staggered.
    val titleAlpha = remember { Animatable(0f) }
    val titleOffset = remember { Animatable(40f) }
    val subAlpha = remember { Animatable(0f) }
    val footAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        heartScale.animateTo(
            1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,   // dramatic overshoot
                stiffness = Spring.StiffnessLow,
            ),
        )
    }
    LaunchedEffect(Unit) {
        delay(350)
        titleAlpha.animateTo(1f, tween(600, easing = EaseOut))
    }
    LaunchedEffect(Unit) {
        delay(350)
        titleOffset.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
    }
    LaunchedEffect(Unit) {
        delay(750)
        subAlpha.animateTo(1f, tween(600))
    }
    LaunchedEffect(Unit) {
        delay(1_100)
        footAlpha.animateTo(1f, tween(700))
    }

    // ---- Continuous loops ----
    val infinite = rememberInfiniteTransition(label = "splash-loops")

    // Heartbeat: quick double-thump like a real pulse.
    val heartbeat by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "heartbeat",
    )
    val beatScale = heartbeatScale(heartbeat)

    // Sonar rings expanding outward behind the heart.
    val ring1 by infinite.animateFloat(
        0f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart), label = "r1",
    )
    val ring2 by infinite.animateFloat(
        0f, 1f, infiniteRepeatable(tween(2200, 730, LinearEasing), RepeatMode.Restart), label = "r2",
    )
    val ring3 by infinite.animateFloat(
        0f, 1f, infiniteRepeatable(tween(2200, 1460, LinearEasing), RepeatMode.Restart), label = "r3",
    )

    // Shimmer sweep across the title.
    val shimmer by infinite.animateFloat(
        0f, 1f, infiniteRepeatable(tween(2000, 600, LinearEasing), RepeatMode.Restart), label = "shimmer",
    )

    // Slow breathing of the background gradient.
    val bgShift by infinite.animateFloat(
        0f, 1f, infiniteRepeatable(tween(4000), RepeatMode.Reverse), label = "bg",
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        lerp(Violet, VioletDim, bgShift * 0.4f),
                        lerp(VioletDim, Color(0xFF3A2A6E), bgShift * 0.6f),
                    )
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        val screenH = maxHeight

        // Floating mood emojis drifting upward.
        floatingEmojis.forEachIndexed { i, emoji ->
            FloatingEmoji(
                emoji = emoji,
                index = i,
                screenHeight = screenH.value,
                infinite = infinite,
            )
        }

        // Sonar rings
        listOf(ring1, ring2, ring3).forEach { t ->
            Box(
                Modifier
                    .size(110.dp + (t * 240).dp)
                    .border(2.dp, Color.White.copy(alpha = (1f - t) * 0.35f), CircleShape)
                    .align(Alignment.Center)
                    .offset(y = (-60).dp),
            )
        }

        // Centre content
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "💜",
                fontSize = 84.sp,
                modifier = Modifier
                    .scale(heartScale.value * beatScale)
                    .padding(bottom = 4.dp),
            )
            Spacer(Modifier.height(20.dp))

            // Shimmering brand name
            val shimmerBrush = Brush.linearGradient(
                colors = listOf(
                    Color.White,
                    Color.White.copy(alpha = 0.55f),
                    Color(0xFFE9DDFF),
                    Color.White,
                ),
                start = Offset(shimmer * 900f - 450f, 0f),
                end = Offset(shimmer * 900f, 90f),
            )
            Text(
                text = "VibeCheck",
                style = TextStyle(
                    brush = shimmerBrush,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                ),
                modifier = Modifier.graphicsLayer {
                    alpha = titleAlpha.value
                    translationY = titleOffset.value
                },
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "How are you, really?",
                fontSize = 17.sp,
                color = Color.White.copy(alpha = 0.78f),
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { alpha = subAlpha.value },
            )
        }

        // Bottom tagline + animated dots
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .graphicsLayer { alpha = footAlpha.value },
        ) {
            LoadingDots(infiniteShift = heartbeat)
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Anonymous · Honest · 30 seconds",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.5f),
                letterSpacing = 0.4.sp,
            )
        }
    }
}

/** Real-pulse curve: thump-thump … rest. */
private fun heartbeatScale(t: Float): Float = when {
    t < 0.14f -> 1f + 0.14f * sin(t / 0.14f * Math.PI).toFloat()
    t < 0.28f -> 1f + 0.10f * sin((t - 0.14f) / 0.14f * Math.PI).toFloat()
    else -> 1f
}

@Composable
private fun FloatingEmoji(
    emoji: String,
    index: Int,
    screenHeight: Float,
    infinite: androidx.compose.animation.core.InfiniteTransition,
) {
    val duration = 5200 + index * 700
    val t by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(duration, index * 650, LinearEasing), RepeatMode.Restart,
        ),
        label = "float-$emoji-$index",
    )
    val xFractions = listOf(0.08f, 0.85f, 0.22f, 0.72f, 0.50f, 0.92f, 0.15f, 0.63f)
    val xF = xFractions[index % xFractions.size]
    val drift = sin(t * 2 * Math.PI).toFloat() * 14f

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = (sin(t * Math.PI).toFloat() * 0.35f) },
    ) {
        Text(
            emoji,
            fontSize = (18 + (index % 3) * 6).sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(
                    x = (xF * 340).dp + drift.dp,
                    y = (screenHeight * (1f - t)).dp - 40.dp,
                ),
        )
    }
}

@Composable
private fun LoadingDots(infiniteShift: Float) {
    androidx.compose.foundation.layout.Row {
        repeat(3) { i ->
            val phase = (infiniteShift + i * 0.22f) % 1f
            val bump = if (phase < 0.3f) sin(phase / 0.3f * Math.PI).toFloat() else 0f
            Box(
                Modifier
                    .padding(horizontal = 4.dp)
                    .offset(y = (-6 * bump).dp)
                    .size(7.dp)
                    .background(Color.White.copy(alpha = 0.5f + bump * 0.5f), CircleShape),
            )
        }
    }
}

private fun lerp(a: Color, b: Color, t: Float): Color =
    androidx.compose.ui.graphics.lerp(a, b, t.coerceIn(0f, 1f))
