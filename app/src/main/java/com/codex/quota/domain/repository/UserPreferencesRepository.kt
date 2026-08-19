package com.codex.quota.domain.repository

import com.codex.quota.domain.model.AppThemeMode
import com.codex.quota.domain.model.RefreshIntervalMinutes
import com.codex.quota.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    fun observePreferences(): Flow<UserPreferences>
    suspend fun getPreferences(): UserPreferences
    suspend fun setThemeMode(mode: AppThemeMode)
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun setRefreshInterval(interval: RefreshIntervalMinutes)
    suspend fun setRefreshOnAppOpen(enabled: Boolean)
    suspend fun setSignedOutNotificationsEnabled(enabled: Boolean)
    suspend fun setQuotaAlertsEnabled(enabled: Boolean)
    suspend fun setQuotaAlertThresholdPercent(threshold: Int)
    suspend fun setHasCompletedOnboarding(completed: Boolean)
}
