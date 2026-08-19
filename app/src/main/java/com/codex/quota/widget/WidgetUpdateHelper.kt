package com.codex.quota.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll

object WidgetUpdateHelper {
    suspend fun updateAllWidgets(context: Context) {
        try {
            SmallQuotaWidget().updateAll(context)
            MediumQuotaWidget().updateAll(context)
            MultiAccountQuotaWidget().updateAll(context)
        } catch (e: Exception) {
            // Glance updates may fail if widget is not active
        }
    }
}
