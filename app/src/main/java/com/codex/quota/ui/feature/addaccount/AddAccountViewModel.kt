package com.codex.quota.ui.feature.addaccount

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codex.quota.domain.model.CodexAccount
import com.codex.quota.domain.model.PlanType
import com.codex.quota.domain.usecase.AddAccountUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddAccountViewModel(
    private val addAccountUseCase: AddAccountUseCase
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _accountAdded = MutableStateFlow<CodexAccount?>(null)
    val accountAdded: StateFlow<CodexAccount?> = _accountAdded.asStateFlow()

    fun addAccount(
        nickname: String,
        email: String?,
        apiKey: String,
        planType: PlanType,
        organizationId: String?,
        colorHex: String,
        isDemoAccount: Boolean
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = addAccountUseCase(
                nickname = nickname,
                email = email,
                apiKey = apiKey,
                planType = planType,
                organizationId = organizationId,
                colorHex = colorHex,
                isDemoAccount = isDemoAccount
            )
            _isLoading.value = false
            if (result.isSuccess) {
                _accountAdded.value = result.getOrThrow()
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to add account"
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
