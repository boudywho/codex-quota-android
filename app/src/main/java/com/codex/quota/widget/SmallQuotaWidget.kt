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

class SmallQuotaWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as? CodexQuotaApplication
        val accounts = app?.repository?.getAllAccounts().orEmpty()
        val primaryAccount = accounts.firstOrNull()
        val prefs = try {
            app?.preferencesRepository?.getPreferences()
        } catch (e: Exception) {
            null
        }
        val themeMode = prefs?.widgetThemeMode ?: WidgetThemeMode.DARK_OBSIDIAN

        provideContent {
            GlanceTheme {
                SmallWidgetContent(context, primaryAccount, themeMode)
            }
        }
    }

    @Composable
    private fun SmallWidgetContent(
        context: Context,
        data: AccountWithUsage?,
        themeMode: WidgetThemeMode
    ) {
        val size = LocalSize.current
        val height = size.height
        val colors = WidgetThemeHelper.getColors(themeMode)

        val intent = if (data != null) {
            Intent(Intent.ACTION_VIEW, Uri.parse("codexquota://account/${data.account.id}"), context, MainActivity::class.java)
        } else {
            Intent(context, MainActivity::class.java)
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(colors.background)
                .cornerRadius(18.dp)
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
                        style = TextStyle(color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = "Tap to setup",
                        style = TextStyle(color = colors.accent, fontSize = 11.sp)
                    )
                }
            } else {
                val remainingPercent = data.usage?.remainingPercent?.toInt()
                val status = data.usage?.status ?: data.account.authStatus
                val resetStr = data.usage?.rateLimitInfo?.resetRequestsDuration
                    ?: data.usage?.rateLimitInfo?.resetTokensDuration
                    ?: "Active"

                val dotColor = try {
                    Color(android.graphics.Color.parseColor(data.account.colorHex))
                } catch (e: Exception) {
                    Color(0xFF10B981)
                }

                Column(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .width(7.dp)
                                .height(7.dp)
                                .background(dotColor)
                                .cornerRadius(3.5.dp)
                        ) {}
                        Spacer(modifier = GlanceModifier.width(6.dp))
                        Text(
                            text = data.account.nickname,
                            maxLines = 1,
                            style = TextStyle(color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(4.dp))

                    if (status == AuthStatus.AUTHENTICATION_REQUIRED) {
                        Text(
                            text = "Signed Out",
                            style = TextStyle(color = colors.error, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        )
                    } else {
                        Text(
                            text = if (remainingPercent != null) "$remainingPercent%" else "--%",
                            style = TextStyle(
                                color = colors.accent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                        )
                        Text(
                            text = "quota left",
                            style = TextStyle(color = colors.textSecondary, fontSize = 10.sp)
                        )
                    }

                    if (height > 90.dp) {
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = "Reset: $resetStr",
                            maxLines = 1,
                            style = TextStyle(color = colors.textMuted, fontSize = 9.sp)
                        )
                    }
                }
            }
        }
    }
}

class SmallQuotaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SmallQuotaWidget()
}
