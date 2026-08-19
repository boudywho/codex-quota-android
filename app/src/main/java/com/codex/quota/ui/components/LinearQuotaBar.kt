package com.codex.quota.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.codex.quota.ui.theme.Amber500
import com.codex.quota.ui.theme.Cyan400
import com.codex.quota.ui.theme.Emerald400
import com.codex.quota.ui.theme.Red500

@Composable
fun LinearQuotaBar(
    remainingPercent: Double?,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp
) {
    val targetFraction = ((remainingPercent ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 800),
        label = "linear_bar_progress"
    )

    val progressBrush = when {
        remainingPercent == null -> Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.outline))
        remainingPercent < 15.0 -> Brush.horizontalGradient(listOf(Red500, Amber500))
        remainingPercent < 40.0 -> Brush.horizontalGradient(listOf(Amber500, Emerald400))
        else -> Brush.horizontalGradient(listOf(Emerald400, Cyan400))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (animatedFraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedFraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(height / 2))
                    .background(progressBrush)
            )
        }
    }
}
