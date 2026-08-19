package com.codex.quota.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.codex.quota.domain.model.AppThemeMode
import com.codex.quota.domain.model.RefreshIntervalMinutes
import com.codex.quota.domain.model.UserPreferences
import com.codex.quota.domain.model.WidgetThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class DataStoreManager(private val context: Context) {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val WIDGET_THEME_MODE = stringPreferencesKey("widget_theme_mode")
        val REFRESH_INTERVAL = longPreferencesKey("refresh_interval_minutes")
        val REFRESH_ON_APP_OPEN = booleanPreferencesKey("refresh_on_app_open")
        val SIGNED_OUT_NOTIFICATIONS = booleanPreferencesKey("signed_out_notifications_enabled")
        val QUOTA_ALERTS_ENABLED = booleanPreferencesKey("quota_alerts_enabled")
        val QUOTA_ALERT_THRESHOLD = intPreferencesKey("quota_alert_threshold_percent")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val themeModeStr = prefs[PreferencesKeys.THEME_MODE] ?: AppThemeMode.SYSTEM.name
        val themeMode = try {
            AppThemeMode.valueOf(themeModeStr)
        } catch (e: Exception) {
            AppThemeMode.SYSTEM
        }

        val widgetThemeStr = prefs[PreferencesKeys.WIDGET_THEME_MODE] ?: WidgetThemeMode.DARK_OBSIDIAN.name
        val widgetThemeMode = WidgetThemeMode.fromString(widgetThemeStr)

        val refreshIntervalMinutes = prefs[PreferencesKeys.REFRESH_INTERVAL] ?: 30L

        UserPreferences(
            themeMode = themeMode,
            dynamicColor = prefs[PreferencesKeys.DYNAMIC_COLOR] ?: true,
            widgetThemeMode = widgetThemeMode,
            refreshInterval = RefreshIntervalMinutes.fromMinutes(refreshIntervalMinutes),
            refreshOnAppOpen = prefs[PreferencesKeys.REFRESH_ON_APP_OPEN] ?: true,
            signedOutNotificationsEnabled = prefs[PreferencesKeys.SIGNED_OUT_NOTIFICATIONS] ?: true,
            quotaAlertsEnabled = prefs[PreferencesKeys.QUOTA_ALERTS_ENABLED] ?: true,
            quotaAlertThresholdPercent = prefs[PreferencesKeys.QUOTA_ALERT_THRESHOLD] ?: 10,
            hasCompletedOnboarding = prefs[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
        )
    }

    suspend fun getPreferences(): UserPreferences = userPreferencesFlow.first()

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.DYNAMIC_COLOR] = enabled
        }
    }

    suspend fun setWidgetThemeMode(mode: WidgetThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.WIDGET_THEME_MODE] = mode.name
        }
    }

    suspend fun setRefreshInterval(interval: RefreshIntervalMinutes) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.REFRESH_INTERVAL] = interval.minutes
        }
    }

    suspend fun setRefreshOnAppOpen(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.REFRESH_ON_APP_OPEN] = enabled
        }
    }

    suspend fun setSignedOutNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SIGNED_OUT_NOTIFICATIONS] = enabled
        }
    }

    suspend fun setQuotaAlertsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.QUOTA_ALERTS_ENABLED] = enabled
        }
    }

    suspend fun setQuotaAlertThresholdPercent(threshold: Int) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.QUOTA_ALERT_THRESHOLD] = threshold
        }
    }

    suspend fun setHasCompletedOnboarding(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }
}
