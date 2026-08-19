package com.codex.quota.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.codex.quota.domain.model.AccountWithUsage
import com.codex.quota.domain.model.AuthStatus
import com.codex.quota.ui.theme.Amber500
import com.codex.quota.ui.theme.Red500
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AccountCard(
    item: AccountWithUsage,
    onClick: () -> Unit,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val account = item.account
    val usage = item.usage
    val status = usage?.status ?: account.authStatus
    val isSignedOut = status == AuthStatus.AUTHENTICATION_REQUIRED

    val accountColor = try {
        Color(android.graphics.Color.parseColor(account.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val borderColor = when {
        isSignedOut -> Red500.copy(alpha = 0.5f)
        usage?.isStale == true -> Amber500.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row: Color Indicator, Nickname, Plan, Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(accountColor)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account.nickname,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = account.planType.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusBadge(status = status)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isSignedOut) {
                // Prominent Sign-In Required Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Red500.copy(alpha = 0.1f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = Red500,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Authentication Expired",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = Red500
                        )
                        Text(
                            text = "Credentials are no longer valid. Tap to re-authenticate.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = onSignInClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Red500),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Sign in", color = Color.White, fontSize = 12.sp)
                    }
                }
            } else {
                // Quota Gauges and Key Metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CircularQuotaGauge(
                        remainingPercent = usage?.remainingPercent,
                        size = 84.dp,
                        strokeWidth = 8.dp
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 18.dp)
                    ) {
                        val resetText = usage?.rateLimitInfo?.resetRequestsDuration
                            ?: usage?.rateLimitInfo?.resetTokensDuration
                            ?: if (usage?.resetAtEpochMs != null) {
                                val remainingMin = ((usage.resetAtEpochMs - System.currentTimeMillis()) / 60000L).coerceAtLeast(0)
                                if (remainingMin >= 60) "${remainingMin / 60}h ${remainingMin % 60}m" else "${remainingMin}m"
                            } else "Continuous window"

                        Text(
                            text = "Resets in: $resetText",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        if (usage?.totalLimitTokens != null && usage.usedTokens != null) {
                            val format = NumberFormat.getNumberInstance(Locale.getDefault())
                            Text(
                                text = "Tokens: ${format.format(usage.usedTokens)} / ${format.format(usage.totalLimitTokens)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (usage?.rateLimitInfo?.limitRequests != null && usage.rateLimitInfo.remainingRequests != null) {
                            Text(
                                text = "Requests: ${usage.rateLimitInfo.remainingRequests} / ${usage.rateLimitInfo.limitRequests} left",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (usage?.remainingCredits != null) {
                            Text(
                                text = "Credits: $${String.format(Locale.US, "%.2f", usage.remainingCredits)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                LinearQuotaBar(remainingPercent = usage?.remainingPercent)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer: relative timestamp and stale warning
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RelativeTimeText(
                    epochMs = usage?.fetchedAtEpochMs ?: account.lastSuccessfulSyncEpochMs,
                    style = MaterialTheme.typography.labelSmall
                )

                if (usage?.isStale == true && !isSignedOut) {
                    Text(
                        text = "Stale (offline cache)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Amber500
                    )
                }
            }
        }
    }
}
