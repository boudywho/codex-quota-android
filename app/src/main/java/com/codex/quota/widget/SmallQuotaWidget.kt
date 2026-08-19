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
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
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

class SmallQuotaWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as? CodexQuotaApplication
        val accounts = app?.repository?.getAllAccounts().orEmpty()
        val primaryAccount = accounts.firstOrNull()

        provideContent {
            GlanceTheme {
                SmallWidgetContent(context, primaryAccount)
            }
        }
    }

    @Composable
    private fun SmallWidgetContent(context: Context, data: AccountWithUsage?) {
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
                .padding(12.dp)
                .clickable(actionStartActivity(intent)),
            contentAlignment = Alignment.Center
        ) {
            if (data == null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Codex Quota",
                        style = TextStyle(color = ColorProvider(Color.White), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = "Tap to setup",
                        style = TextStyle(color = ColorProvider(Color(0xFF10B981)), fontSize = 12.sp)
                    )
                }
            } else {
                val remainingPercent = data.usage?.remainingPercent?.toInt()
                val status = data.usage?.status ?: data.account.authStatus
                val resetStr = data.usage?.rateLimitInfo?.resetRequestsDuration
                    ?: data.usage?.rateLimitInfo?.resetTokensDuration
                    ?: "Active"

                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = data.account.nickname,
                        maxLines = 1,
                        style = TextStyle(color = ColorProvider(Color.White), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    )

                    Spacer(modifier = GlanceModifier.height(4.dp))

                    if (status == AuthStatus.AUTHENTICATION_REQUIRED) {
                        Text(
                            text = "Signed Out",
                            style = TextStyle(color = ColorProvider(Color(0xFFEF4444)), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        )
                    } else {
                        Text(
                            text = if (remainingPercent != null) "$remainingPercent%" else "--%",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF10B981)),
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            )
                        )
                        Text(
                            text = "remaining",
                            style = TextStyle(color = ColorProvider(Color(0xFF94A3B8)), fontSize = 11.sp)
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(4.dp))

                    Text(
                        text = "Reset: $resetStr",
                        maxLines = 1,
                        style = TextStyle(color = ColorProvider(Color(0xFF64748B)), fontSize = 10.sp)
                    )
                }
            }
        }
    }
}

class SmallQuotaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SmallQuotaWidget()
}
