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
import androidx.glance.layout.width
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
                .background(Color(0xFF111827))
                .cornerRadius(20.dp)
                .padding(16.dp)
                .clickable(actionStartActivity(intent))
        ) {
            if (data == null) {
                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Codex Quotas",
                        style = TextStyle(color = ColorProvider(Color.White), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    )
                    Spacer(modifier = GlanceModifier.height(6.dp))
                    Text(
                        text = "Tap to add your first account",
                        style = TextStyle(color = ColorProvider(Color(0xFF38BDF8)), fontSize = 13.sp)
                    )
                }
            } else {
                val remainingPercent = data.usage?.remainingPercent?.toInt()
                val usedPercent = data.usage?.usedPercent?.toInt() ?: remainingPercent?.let { (100 - it).coerceIn(0, 100) }
                val status = data.usage?.status ?: data.account.authStatus
                val resetStr = data.usage?.rateLimitInfo?.resetRequestsDuration
                    ?: data.usage?.rateLimitInfo?.resetTokensDuration
                    ?: "Active Window"

                val dotColor = try {
                    Color(android.graphics.Color.parseColor(data.account.colorHex))
                } catch (e: Exception) {
                    Color(0xFF10B981)
                }

                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Header Row: Dot + Nickname + Plan + Large Percentage
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .width(8.dp)
                                .height(8.dp)
                                .background(dotColor)
                                .cornerRadius(4.dp)
                        ) {}

                        Spacer(modifier = GlanceModifier.width(8.dp))

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

                        Spacer(modifier = GlanceModifier.width(8.dp))

                        if (status == AuthStatus.AUTHENTICATION_REQUIRED) {
                            Text(
                                text = "Sign In",
                                style = TextStyle(color = ColorProvider(Color(0xFFEF4444)), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            )
                        } else {
                            Text(
                                text = if (remainingPercent != null) "$remainingPercent%" else "--%",
                                style = TextStyle(color = ColorProvider(Color(0xFF10B981)), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.height(10.dp))

                    // Progress Bar
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(Color(0xFF334155))
                            .cornerRadius(4.dp)
                    ) {
                        val fraction = ((remainingPercent ?: 0) / 100f).coerceIn(0f, 1f)
                        if (fraction > 0f) {
                            Box(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .background(Color(0xFF10B981))
                                    .cornerRadius(4.dp)
                            ) {}
                        }
                    }

                    Spacer(modifier = GlanceModifier.height(12.dp))

                    // Detail Metrics Box
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E293B))
                            .cornerRadius(10.dp)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = "USED",
                                style = TextStyle(color = ColorProvider(Color(0xFF64748B)), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (usedPercent != null) "$usedPercent%" else "--%",
                                style = TextStyle(color = ColorProvider(Color.White), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            )
                        }

                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                text = "RESET IN",
                                style = TextStyle(color = ColorProvider(Color(0xFF64748B)), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = resetStr,
                                maxLines = 1,
                                style = TextStyle(color = ColorProvider(Color(0xFF38BDF8)), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.height(10.dp))

                    // Footer Row
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Codex Quotas",
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
