package com.codex.quota.data.repository

import com.codex.quota.data.local.DataStoreManager
import com.codex.quota.domain.model.AppThemeMode
import com.codex.quota.domain.model.RefreshIntervalMinutes
import com.codex.quota.domain.model.UserPreferences
import com.codex.quota.domain.model.WidgetThemeMode
import com.codex.quota.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow

class UserPreferencesRepositoryImpl(
    private val dataStoreManager: DataStoreManager
) : UserPreferencesRepository {

    override fun observePreferences(): Flow<UserPreferences> =
        dataStoreManager.userPreferencesFlow

    override suspend fun getPreferences(): UserPreferences =
        dataStoreManager.getPreferences()

    override suspend fun setThemeMode(mode: AppThemeMode) {
        dataStoreManager.setThemeMode(mode)
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        dataStoreManager.setDynamicColor(enabled)
    }

    override suspend fun setWidgetThemeMode(mode: WidgetThemeMode) {
        dataStoreManager.setWidgetThemeMode(mode)
    }

    override suspend fun setBackgroundSyncEnabled(enabled: Boolean) {
        dataStoreManager.setBackgroundSyncEnabled(enabled)
    }

    override suspend fun setRefreshInterval(interval: RefreshIntervalMinutes) {
        dataStoreManager.setRefreshInterval(interval)
    }

    override suspend fun setRefreshOnAppOpen(enabled: Boolean) {
        dataStoreManager.setRefreshOnAppOpen(enabled)
    }

    override suspend fun setSignedOutNotificationsEnabled(enabled: Boolean) {
        dataStoreManager.setSignedOutNotificationsEnabled(enabled)
    }

    override suspend fun setQuotaAlertsEnabled(enabled: Boolean) {
        dataStoreManager.setQuotaAlertsEnabled(enabled)
    }

    override suspend fun setQuotaAlertThresholds(thresholds: Set<Int>) {
        dataStoreManager.setQuotaAlertThresholds(thresholds)
    }

    override suspend fun toggleQuotaAlertThreshold(threshold: Int) {
        dataStoreManager.toggleQuotaAlertThreshold(threshold)
    }

    override suspend fun setHasCompletedOnboarding(completed: Boolean) {
        dataStoreManager.setHasCompletedOnboarding(completed)
    }

    override suspend fun setRenewalBannerDismissed(accountId: String, dismissed: Boolean) {
        dataStoreManager.setRenewalBannerDismissed(accountId, dismissed)
    }
}
