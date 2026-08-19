package com.codex.quota.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.codex.quota.CodexQuotaApplication
import com.codex.quota.domain.model.AccountWithUsage
import com.codex.quota.domain.model.AuthStatus
import com.codex.quota.ui.MainActivity

class MediumQuotaWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as? CodexQuotaApplication
        val accounts = app?.repository?.getAllAccounts().orEmpty()
        val primaryAccount = accounts.firstOrNull()

        provideContent {
            GlanceTheme {
                MediumWidgetContent(context, primaryAccount)
            }
        }
    }

    @Composable
    private fun MediumWidgetContent(context: Context, data: AccountWithUsage?) {
        val intent = if (data != null) {
            Intent(Intent.ACTION_VIEW, Uri.parse("codexquota://account/${data.account.id}"), context, MainActivity::class.java)
        } else {
            Intent(context, MainActivity::class.java)
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
                .cornerRadius(16.dp)
                .padding(14.dp)
                .clickable(actionStartActivity(intent))
        ) {
            if (data == null) {
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Codex Quota Monitor",
                        style = TextStyle(color = ColorProvider(Color.White), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = "No accounts configured. Tap to add.",
                        style = TextStyle(color = ColorProvider(Color(0xFF38BDF8)), fontSize = 13.sp)
                    )
                }
            } else {
                val remainingPercent = data.usage?.remainingPercent?.toInt()
                val status = data.usage?.status ?: data.account.authStatus
                val resetStr = data.usage?.rateLimitInfo?.resetRequestsDuration
                    ?: data.usage?.rateLimitInfo?.resetTokensDuration
                    ?: "Rolling Window"

                Column(modifier = GlanceModifier.fillMaxSize()) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = data.account.nickname,
                                maxLines = 1,
                                style = TextStyle(color = ColorProvider(Color.White), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            )
                            Text(
                                text = data.account.planType.displayName,
                                maxLines = 1,
                                style = TextStyle(color = ColorProvider(Color(0xFF94A3B8)), fontSize = 11.sp)
                            )
                        }

                        if (status == AuthStatus.AUTHENTICATION_REQUIRED) {
                            Text(
                                text = "Sign-In Required",
                                style = TextStyle(color = ColorProvider(Color(0xFFEF4444)), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            )
                        } else {
                            Text(
                                text = if (remainingPercent != null) "$remainingPercent%" else "--%",
                                style = TextStyle(color = ColorProvider(Color(0xFF10B981)), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.height(8.dp))

                    // Progress Bar background
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(Color(0xFF334155))
                            .cornerRadius(3.dp)
                    ) {
                        val fraction = ((remainingPercent ?: 0) / 100f).coerceIn(0f, 1f)
                        if (fraction > 0f) {
                            Box(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .background(Color(0xFF10B981))
                                    .cornerRadius(3.dp)
                            ) {}
                        }
                    }

                    Spacer(modifier = GlanceModifier.height(8.dp))

                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Resets in $resetStr",
                            style = TextStyle(color = ColorProvider(Color(0xFF64748B)), fontSize = 11.sp)
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Text(
                            text = if (data.usage?.isStale == true) "Stale Cache" else "Synced",
                            style = TextStyle(
                                color = ColorProvider(if (data.usage?.isStale == true) Color(0xFFF59E0B) else Color(0xFF94A3B8)),
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

class MediumQuotaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MediumQuotaWidget()
}
