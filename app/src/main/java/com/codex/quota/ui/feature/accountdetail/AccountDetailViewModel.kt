package com.codex.quota.ui.feature.accountdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codex.quota.domain.model.AccountWithUsage
import com.codex.quota.domain.model.CodexUsage
import com.codex.quota.domain.repository.CodexAccountRepository
import com.codex.quota.domain.usecase.RefreshAccountUseCase
import com.codex.quota.domain.usecase.RemoveAccountUseCase
import com.codex.quota.domain.usecase.UpdateAccountUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AccountDetailViewModel(
    private val accountId: String,
    private val repository: CodexAccountRepository,
    private val refreshAccountUseCase: RefreshAccountUseCase,
    private val updateAccountUseCase: UpdateAccountUseCase,
    private val removeAccountUseCase: RemoveAccountUseCase
) : ViewModel() {

    val accountState: StateFlow<AccountWithUsage?> = repository.observeAccount(accountId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    private val _accountDeleted = MutableStateFlow(false)
    val accountDeleted: StateFlow<Boolean> = _accountDeleted.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val result = refreshAccountUseCase(accountId)
            _isRefreshing.value = false
            if (result.isFailure) {
                _uiMessage.value = result.exceptionOrNull()?.message ?: "Failed to refresh account"
            }
        }
    }

    fun updateAccountDetails(newNickname: String, newColorHex: String, newRenewalDateEpochMs: Long?) {
        viewModelScope.launch {
            val result = updateAccountUseCase(accountId, newNickname, newColorHex, newRenewalDateEpochMs)
            if (result.isSuccess) {
                _uiMessage.value = "Account updated"
            } else {
                _uiMessage.value = "Failed to update account"
            }
        }
    }

    fun updateRenewalDate(renewalDateEpochMs: Long?) {
        viewModelScope.launch {
            val result = updateAccountUseCase.setRenewalDate(accountId, renewalDateEpochMs)
            if (result.isSuccess) {
                _uiMessage.value = if (renewalDateEpochMs != null) "Renewal date updated" else "Renewal date removed"
            } else {
                _uiMessage.value = "Failed to update renewal date"
            }
        }
    }

    fun reauthenticate(newApiKey: String) {
        viewModelScope.launch {
            _isRefreshing.value = true
            val result = repository.reauthenticateAccount(accountId, newApiKey)
            _isRefreshing.value = false
            if (result.isSuccess) {
                _uiMessage.value = "Account re-authenticated successfully"
            } else {
                _uiMessage.value = result.exceptionOrNull()?.message ?: "Re-authentication failed"
            }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            val result = removeAccountUseCase(accountId)
            if (result.isSuccess) {
                _accountDeleted.value = true
            } else {
                _uiMessage.value = "Failed to delete account"
            }
        }
    }

    fun clearUiMessage() {
        _uiMessage.value = null
    }
}
