package com.codex.quota.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codex.quota.ui.theme.Amber500
import com.codex.quota.ui.theme.Cyan400
import com.codex.quota.ui.theme.Emerald400
import com.codex.quota.ui.theme.Red500

@Composable
fun CircularQuotaGauge(
    remainingPercent: Double?,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    strokeWidth: Dp = 10.dp
) {
    val targetFraction = ((remainingPercent ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 900),
        label = "gauge_sweep"
    )

    val progressColor = when {
        remainingPercent == null -> MaterialTheme.colorScheme.outline
        remainingPercent < 15.0 -> Red500
        remainingPercent < 40.0 -> Amber500
        else -> Emerald400
    }

    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    val description = if (remainingPercent != null) {
        "${remainingPercent.toInt()} percent quota remaining"
    } else {
        "Quota unknown"
    }

    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            // Draw background track (270 degrees arc)
            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = stroke
            )

            // Draw active progress
            if (animatedFraction > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to progressColor,
                        0.75f to Cyan400,
                        1.0f to progressColor
                    ),
                    startAngle = 135f,
                    sweepAngle = 270f * animatedFraction,
                    useCenter = false,
                    style = stroke
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (remainingPercent != null) "${remainingPercent.toInt()}%" else "--%",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.22).sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "left",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = (size.value * 0.11).sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
