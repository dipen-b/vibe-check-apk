package com.vibecheck.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin

private val confettiColors = listOf(
    Color(0xFF6C4DD3), // Violet
    Color(0xFFFFD700), // Gold
    Color(0xFFFF6B9D), // Pink
    Color(0xFF4DDBCB), // Teal
    Color(0xFFFF8C42), // Orange
    Color(0xFF48C774), // Green
)

private data class Particle(
    val x: Float,       // 0..1 normalised start x
    val angle: Float,   // launch angle in radians
    val speed: Float,   // 0..1 speed factor
    val size: Float,    // dp
    val color: Color,
    val rotation: Float,
    val shape: Int,     // 0=rect, 1=circle
)

@Composable
fun ConfettiEffect(
    trigger: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!trigger) return

    val progress = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(2400))
    }

    val particles = remember {
        List(60) { i ->
            Particle(
                x = (i * 37 % 100) / 100f,
                angle = Math.toRadians((-60 + (i * 97 % 120)).toDouble()).toFloat(),
                speed = 0.4f + (i * 31 % 60) / 100f,
                size = 6f + (i * 13 % 10),
                color = confettiColors[i % confettiColors.size],
                rotation = (i * 47 % 360).toFloat(),
                shape = i % 2,
            )
        }
    }

    Canvas(modifier.fillMaxSize()) {
        val p = progress.value
        if (p >= 1f) return@Canvas

        val gravity = 900f
        val dt = p

        particles.forEach { particle ->
            val vx = cos(particle.angle) * particle.speed * size.width
            val vy = -sin(particle.angle.toDouble()).toFloat() * particle.speed * size.height * 0.8f

            val x = particle.x * size.width + vx * dt
            val y = size.height * 0.1f + vy * dt + 0.5f * gravity * dt * dt
            val alpha = (1f - (p - 0.6f).coerceAtLeast(0f) / 0.4f).coerceIn(0f, 1f)
            val rot = particle.rotation + 360f * dt * particle.speed

            if (y > size.height || alpha <= 0f) return@forEach

            val color = particle.color.copy(alpha = alpha)
            val s = particle.size

            rotate(rot, pivot = Offset(x, y)) {
                if (particle.shape == 0) {
                    drawRect(color, Offset(x - s / 2, y - s / 4), size = androidx.compose.ui.geometry.Size(s, s / 2))
                } else {
                    drawCircle(color, s / 2, Offset(x, y))
                }
            }
        }
    }
}
