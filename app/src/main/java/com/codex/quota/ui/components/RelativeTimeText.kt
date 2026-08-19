package com.codex.quota.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RelativeTimeText(
    epochMs: Long?,
    modifier: Modifier = Modifier,
    prefix: String = "Updated ",
    style: TextStyle = MaterialTheme.typography.bodySmall,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val text = if (epochMs == null || epochMs <= 0) {
        "Never synced"
    } else {
        val diff = System.currentTimeMillis() - epochMs
        val relative = when {
            diff < 60_000L -> "just now"
            diff < 3600_000L -> "${(diff / 60_000L)}m ago"
            diff < 86400_000L -> "${(diff / 3600_000L)}h ago"
            else -> SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(epochMs))
        }
        "$prefix$relative"
    }

    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = color
    )
}
