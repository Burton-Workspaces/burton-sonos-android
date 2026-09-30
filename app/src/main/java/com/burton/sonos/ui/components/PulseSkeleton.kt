package com.burton.sonos.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.burton.sonos.ui.theme.BurtonCharcoal
import com.burton.sonos.ui.theme.BurtonElevated
import com.burton.sonos.ui.theme.BurtonGraphite

@Composable
fun PulseBlock(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )
    val glow by transition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow",
    )
    Box(
        modifier = modifier
            .clip(shape)
            .drawBehind {
                val travel = size.width * 1.4f
                val x = (phase * travel) - size.width * 0.2f
                val brush = Brush.linearGradient(
                    colors = listOf(
                        BurtonCharcoal.copy(alpha = 0.9f * glow),
                        BurtonGraphite.copy(alpha = glow),
                        BurtonElevated.copy(alpha = glow),
                        BurtonGraphite.copy(alpha = glow),
                        BurtonCharcoal.copy(alpha = 0.9f * glow),
                    ),
                    start = Offset(x, 0f),
                    end = Offset(x + size.width * 0.55f, size.height),
                )
                drawRect(brush)
            },
    )
}

@Composable
fun RoomsSkeleton(count: Int = 3) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(count) {
            PulseBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp),
                shape = RoundedCornerShape(20.dp),
            )
        }
    }
}

@Composable
fun NowPlayingSkeleton(modifier: Modifier = Modifier) {
    PulseBlock(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(68.dp),
        shape = RoundedCornerShape(18.dp),
    )
}
