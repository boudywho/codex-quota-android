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
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
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
import com.codex.quota.domain.model.WidgetThemeMode
import com.codex.quota.ui.MainActivity

class MultiAccountQuotaWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as? CodexQuotaApplication
        val accounts = app?.repository?.getAllAccounts().orEmpty().take(5)
        val prefs = try {
            app?.preferencesRepository?.getPreferences()
        } catch (e: Exception) {
            null
        }
        val themeMode = prefs?.widgetThemeMode ?: WidgetThemeMode.DARK_OBSIDIAN

        provideContent {
            GlanceTheme {
                MultiWidgetContent(context, accounts, themeMode)
            }
        }
    }

    @Composable
    private fun MultiWidgetContent(
        context: Context,
        accounts: List<AccountWithUsage>,
        themeMode: WidgetThemeMode
    ) {
        val colors = WidgetThemeHelper.getColors(themeMode)
        val mainIntent = Intent(Intent.ACTION_VIEW, Uri.parse("codexquota://dashboard"), context, MainActivity::class.java)

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(colors.background)
                .cornerRadius(20.dp)
                .padding(12.dp)
        ) {
            if (accounts.isEmpty()) {
                Column(
                    modifier = GlanceModifier.fillMaxSize().clickable(actionStartActivity(mainIntent)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Codex Quotas",
                        style = TextStyle(color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = "Tap to add accounts",
                        style = TextStyle(color = colors.accentBlue, fontSize = 12.sp)
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
                            style = TextStyle(color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Text(
                            text = "${accounts.size} Active",
                            style = TextStyle(color = colors.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(6.dp))

                    for (item in accounts) {
                        AccountRow(context, item, colors)
                        Spacer(modifier = GlanceModifier.height(4.dp))
                    }
                }
            }
        }
    }

    @Composable
    private fun AccountRow(
        context: Context,
        item: AccountWithUsage,
        colors: WidgetColors
    ) {
        val detailIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("codexquota://account/${item.account.id}"),
            context,
            MainActivity::class.java
        )

        val remainingPercent = item.usage?.remainingPercent?.toInt()
        val isSignedOut = item.usage?.status == AuthStatus.AUTHENTICATION_REQUIRED ||
                item.account.authStatus == AuthStatus.AUTHENTICATION_REQUIRED

        val dotColor = try {
            Color(android.graphics.Color.parseColor(item.account.colorHex))
        } catch (e: Exception) {
            Color(0xFF10B981)
        }

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(colors.surface)
                .cornerRadius(10.dp)
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .clickable(actionStartActivity(detailIntent)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .width(6.dp)
                    .height(6.dp)
                    .background(dotColor)
                    .cornerRadius(3.dp)
            ) {}

            Spacer(modifier = GlanceModifier.width(6.dp))

            Text(
                text = item.account.nickname,
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
                style = TextStyle(color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            )

            if (isSignedOut) {
                Text(
                    text = "Sign In",
                    style = TextStyle(color = colors.error, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                )
            } else {
                Text(
                    text = if (remainingPercent != null) "$remainingPercent%" else "--%",
                    style = TextStyle(
                        color = if ((remainingPercent ?: 100) > 20) colors.accent else ColorProvider(Color(0xFFF59E0B)),
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
