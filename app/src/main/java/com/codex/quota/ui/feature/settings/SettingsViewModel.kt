package com.codex.quota.ui.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codex.quota.domain.model.AppThemeMode
import com.codex.quota.domain.model.RefreshIntervalMinutes
import com.codex.quota.domain.model.UserPreferences
import com.codex.quota.domain.model.WidgetThemeMode
import com.codex.quota.domain.repository.CodexAccountRepository
import com.codex.quota.domain.repository.UserPreferencesRepository
import com.codex.quota.widget.WidgetUpdateHelper
import com.codex.quota.worker.WorkScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val accountRepository: CodexAccountRepository
) : ViewModel() {

    val preferencesState: StateFlow<UserPreferences> = preferencesRepository.observePreferences()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDynamicColor(enabled)
        }
    }

    fun setWidgetThemeMode(context: Context, mode: WidgetThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setWidgetThemeMode(mode)
            WidgetUpdateHelper.updateAllWidgets(context)
        }
    }

    fun setRefreshInterval(context: Context, interval: RefreshIntervalMinutes) {
        viewModelScope.launch {
            preferencesRepository.setRefreshInterval(interval)
            WorkScheduler.schedulePeriodicRefresh(context, interval.minutes)
        }
    }

    fun setRefreshOnAppOpen(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setRefreshOnAppOpen(enabled)
        }
    }

    fun setSignedOutNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setSignedOutNotificationsEnabled(enabled)
        }
    }

    fun setQuotaAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setQuotaAlertsEnabled(enabled)
        }
    }

    fun setQuotaAlertThreshold(threshold: Int) {
        viewModelScope.launch {
            preferencesRepository.setQuotaAlertThresholdPercent(threshold)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            accountRepository.clearAllData()
        }
    }
}
