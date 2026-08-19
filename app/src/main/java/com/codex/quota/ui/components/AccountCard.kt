package com.codex.quota.ui.components

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

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                SignedOutBanner(onSignInClick = onSignInClick)
            } else {
                ActiveQuotaSection(item = item)
            }
        }
    }
}

@Composable
private fun ActiveQuotaSection(item: AccountWithUsage) {
    val usage = item.usage
    val remainingPercent = usage?.remainingPercent
    val rateLimitInfo = usage?.rateLimitInfo

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Circular Gauge
        CircularQuotaGauge(
            remainingPercent = remainingPercent,
            modifier = Modifier.size(80.dp),
            strokeWidth = 9.dp
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Quota details column
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (rateLimitInfo?.limitTokens != null && rateLimitInfo.remainingTokens != null) {
                val formattedTokens = NumberFormat.getNumberInstance(Locale.US).format(rateLimitInfo.remainingTokens)
                val formattedLimit = NumberFormat.getNumberInstance(Locale.US).format(rateLimitInfo.limitTokens)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Token Limit",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$formattedTokens / $formattedLimit",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearQuotaBar(
                        remainingPercent = rateLimitInfo.tokenRemainingPercent,
                        modifier = Modifier.height(6.dp)
                    )
                }
            }

            if (rateLimitInfo?.limitRequests != null && rateLimitInfo.remainingRequests != null) {
                val formattedReqs = NumberFormat.getNumberInstance(Locale.US).format(rateLimitInfo.remainingRequests)
                val formattedLimit = NumberFormat.getNumberInstance(Locale.US).format(rateLimitInfo.limitRequests)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Request Limit",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$formattedReqs / $formattedLimit",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearQuotaBar(
                        remainingPercent = rateLimitInfo.requestRemainingPercent,
                        modifier = Modifier.height(6.dp)
                    )
                }
            }

            if (rateLimitInfo?.limitTokens == null && rateLimitInfo?.limitRequests == null) {
                Text(
                    text = "Subscription Quota Active",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Rolling message limit window",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Footer: Reset countdown & Relative sync timestamp
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val resetDuration = rateLimitInfo?.resetTokensDuration
            ?: rateLimitInfo?.resetRequestsDuration
            ?: (if (usage?.resetAtEpochMs != null) "Rolling window" else "Active")

        Text(
            text = "Resets in: $resetDuration",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        RelativeTimeText(
            epochMs = usage?.fetchedAtEpochMs,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun SignedOutBanner(
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Red500.copy(alpha = 0.1f))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Red500,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Account Signed Out",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Red500
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Credentials have expired or been revoked. Re-authenticate to resume real-time quota tracking.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = onSignInClick,
            colors = ButtonDefaults.buttonColors(containerColor = Red500),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Re-Authenticate Now", fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}
