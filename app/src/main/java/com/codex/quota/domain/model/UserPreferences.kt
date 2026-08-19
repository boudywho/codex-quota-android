package com.codex.quota.domain.model

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class RefreshIntervalMinutes(val minutes: Long, val label: String) {
    MINUTES_15(15, "15 minutes"),
    MINUTES_30(30, "30 minutes"),
    HOURS_1(60, "1 hour"),
    HOURS_3(180, "3 hours"),
    HOURS_6(360, "6 hours");

    companion object {
        fun fromMinutes(minutes: Long): RefreshIntervalMinutes {
            return entries.find { it.minutes == minutes } ?: MINUTES_30
        }
    }
}

data class UserPreferences(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val refreshInterval: RefreshIntervalMinutes = RefreshIntervalMinutes.MINUTES_30,
    val refreshOnAppOpen: Boolean = true,
    val signedOutNotificationsEnabled: Boolean = true,
    val quotaAlertsEnabled: Boolean = true,
    val quotaAlertThresholdPercent: Int = 10,
    val hasCompletedOnboarding: Boolean = false
)
