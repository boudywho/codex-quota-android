package com.codex.quota.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codex.quota.domain.model.AuthStatus
import com.codex.quota.ui.theme.Amber500
import com.codex.quota.ui.theme.Blue500
import com.codex.quota.ui.theme.Emerald500
import com.codex.quota.ui.theme.Red500

@Composable
fun StatusBadge(
    status: AuthStatus,
    modifier: Modifier = Modifier
) {
    val (dotColor, textColor, text) = when (status) {
        AuthStatus.AUTHENTICATED -> Triple(Emerald500, Emerald500, "Active")
        AuthStatus.REFRESHING -> Triple(Blue500, Blue500, "Syncing…")
        AuthStatus.OFFLINE -> Triple(Color.Gray, Color.Gray, "Offline")
        AuthStatus.TEMPORARY_ERROR -> Triple(Amber500, Amber500, "Rate Limited / Error")
        AuthStatus.AUTHENTICATION_REQUIRED -> Triple(Red500, Red500, "Sign-In Required")
        AuthStatus.UNKNOWN -> Triple(Color.Gray, Color.Gray, "Unknown")
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(dotColor.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}
