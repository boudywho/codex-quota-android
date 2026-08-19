package com.codex.quota.ui.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codex.quota.domain.model.AccountWithUsage
import com.codex.quota.domain.usecase.ObserveAccountsUseCase
import com.codex.quota.domain.usecase.RefreshAllAccountsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Success(
        val accounts: List<AccountWithUsage>,
        val isRefreshing: Boolean = false,
        val errorMessage: String? = null
    ) : DashboardUiState
}

class DashboardViewModel(
    private val observeAccountsUseCase: ObserveAccountsUseCase,
    private val refreshAllAccountsUseCase: RefreshAllAccountsUseCase
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val accountsState: StateFlow<List<AccountWithUsage>> = observeAccountsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun refreshAll() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val result = refreshAllAccountsUseCase()
            _isRefreshing.value = false
            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to refresh quotas"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
