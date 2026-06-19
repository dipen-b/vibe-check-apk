package com.vibecheck.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1400f,
        animationSpec = infiniteRepeatable(
            tween(1100, easing = LinearEasing),
            RepeatMode.Restart,
        ),
        label = "shimmerOffset",
    )
    val base = MaterialTheme.colorScheme.surfaceVariant
    return Brush.linearGradient(
        colors = listOf(
            base.copy(alpha = 0.9f),
            base.copy(alpha = 0.3f),
            base.copy(alpha = 0.9f),
        ),
        start = Offset(translateAnim - 400f, 0f),
        end = Offset(translateAnim, 0f),
    )
}

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
) {
    Box(modifier.background(shimmerBrush(), shape))
}

@Composable
fun InsightsSkeleton() {
    Column(Modifier.padding(horizontal = 24.dp, vertical = 28.dp)) {
        ShimmerBox(Modifier.width(160.dp).height(28.dp), RoundedCornerShape(8.dp))
        Spacer(Modifier.height(8.dp))
        ShimmerBox(Modifier.width(100.dp).height(18.dp), RoundedCornerShape(6.dp))
        Spacer(Modifier.height(24.dp))
        // Bar chart skeleton
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            val heights = listOf(60, 40, 80, 50, 90, 30, 70)
            heights.forEach { h ->
                ShimmerBox(
                    Modifier.weight(1f).height(h.dp),
                    RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        ShimmerBox(Modifier.fillMaxWidth().height(100.dp), RoundedCornerShape(14.dp))
        Spacer(Modifier.height(16.dp))
        ShimmerBox(Modifier.fillMaxWidth().height(80.dp), RoundedCornerShape(14.dp))
    }
}

@Composable
fun HeatmapListSkeleton() {
    Column(
        Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        repeat(6) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(shimmerBrush(), RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ShimmerBox(Modifier.width(14.dp).height(14.dp), CircleShape)
                Column(Modifier.weight(1f)) {
                    ShimmerBox(Modifier.width(120.dp).height(14.dp), RoundedCornerShape(4.dp))
                    Spacer(Modifier.height(6.dp))
                    ShimmerBox(Modifier.width(70.dp).height(10.dp), RoundedCornerShape(4.dp))
                }
                ShimmerBox(Modifier.width(60.dp).height(14.dp), RoundedCornerShape(4.dp))
            }
        }
    }
}
