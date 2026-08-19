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

class MultiAccountQuotaWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as? CodexQuotaApplication
        val accounts = app?.repository?.getAllAccounts().orEmpty().take(4)

        provideContent {
            GlanceTheme {
                MultiWidgetContent(context, accounts)
            }
        }
    }

    @Composable
    private fun MultiWidgetContent(context: Context, accounts: List<AccountWithUsage>) {
        val mainIntent = Intent(Intent.ACTION_VIEW, Uri.parse("codexquota://dashboard"), context, MainActivity::class.java)

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
                .cornerRadius(16.dp)
                .padding(12.dp)
        ) {
            if (accounts.isEmpty()) {
                Column(
                    modifier = GlanceModifier.fillMaxSize().clickable(actionStartActivity(mainIntent)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Codex Quota",
                        style = TextStyle(color = ColorProvider(Color.White), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = "Tap to add accounts",
                        style = TextStyle(color = ColorProvider(Color(0xFF38BDF8)), fontSize = 13.sp)
                    )
                }
            } else {
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .clickable(actionStartActivity(mainIntent)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Codex Quotas",
                            style = TextStyle(color = ColorProvider(Color.White), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Text(
                            text = "${accounts.size} accounts",
                            style = TextStyle(color = ColorProvider(Color(0xFF94A3B8)), fontSize = 11.sp)
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(6.dp))

                    for (item in accounts) {
                        AccountRow(context, item)
                        Spacer(modifier = GlanceModifier.height(4.dp))
                    }
                }
            }
        }
    }

    @Composable
    private fun AccountRow(context: Context, item: AccountWithUsage) {
        val detailIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("codexquota://account/${item.account.id}"),
            context,
            MainActivity::class.java
        )

        val remainingPercent = item.usage?.remainingPercent?.toInt()
        val isSignedOut = item.usage?.status == AuthStatus.AUTHENTICATION_REQUIRED ||
                item.account.authStatus == AuthStatus.AUTHENTICATION_REQUIRED

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(Color(0xFF1E293B))
                .cornerRadius(8.dp)
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .clickable(actionStartActivity(detailIntent)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.account.nickname,
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(color = ColorProvider(Color.White), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            )

            if (isSignedOut) {
                Text(
                    text = "Sign In",
                    style = TextStyle(color = ColorProvider(Color(0xFFEF4444)), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                )
            } else {
                Text(
                    text = if (remainingPercent != null) "$remainingPercent%" else "--%",
                    style = TextStyle(
                        color = ColorProvider(if ((remainingPercent ?: 100) > 20) Color(0xFF10B981) else Color(0xFFF59E0B)),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

class MultiAccountQuotaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MultiAccountQuotaWidget()
}
