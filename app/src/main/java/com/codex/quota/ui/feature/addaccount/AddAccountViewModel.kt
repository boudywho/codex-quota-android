package com.codex.quota.ui.feature.addaccount

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codex.quota.auth.JwtTokenParser
import com.codex.quota.auth.OAuthManager
import com.codex.quota.domain.model.CodexAccount
import com.codex.quota.domain.model.PlanType
import com.codex.quota.domain.usecase.AddAccountUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddAccountUiState(
    val nickname: String = "",
    val email: String = "",
    val apiKey: String = "",
    val planType: PlanType = PlanType.PLUS,
    val organizationId: String = "",
    val selectedColorHex: String = "#10B981",
    val isDemoAccount: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val createdAccount: CodexAccount? = null
)

class AddAccountViewModel(
    private val addAccountUseCase: AddAccountUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddAccountUiState())
    val uiState: StateFlow<AddAccountUiState> = _uiState.asStateFlow()

    val availableColors = listOf(
        "#10B981", // Emerald
        "#38BDF8", // Sky
        "#818CF8", // Indigo
        "#F59E0B", // Amber
        "#EC4899", // Pink
        "#A855F7", // Purple
        "#06B6D4", // Cyan
        "#E11D48"  // Rose
    )

    fun onNicknameChange(value: String) {
        _uiState.update { it.copy(nickname = value, errorMessage = null) }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value) }
    }

    fun onApiKeyChange(value: String) {
        _uiState.update { it.copy(apiKey = value, errorMessage = null) }
        // If user pasted a JWT token, auto-detect plan and email
        val decoded = JwtTokenParser.parseToken(value)
        if (decoded != null) {
            _uiState.update { state ->
                state.copy(
                    email = decoded.email ?: state.email,
                    planType = decoded.planType,
                    nickname = if (state.nickname.isBlank() && decoded.email != null) {
                        "ChatGPT ${decoded.planType.displayName}"
                    } else state.nickname
                )
            }
        }
    }

    fun onPlanTypeChange(value: PlanType) {
        _uiState.update { it.copy(planType = value) }
    }

    fun onOrganizationIdChange(value: String) {
        _uiState.update { it.copy(organizationId = value) }
    }

    fun onColorChange(value: String) {
        _uiState.update { it.copy(selectedColorHex = value) }
    }

    fun startOAuthSignIn(context: Context) {
        OAuthManager.launchOAuthBrowser(context)
    }

    fun handleOAuthCallbackUri(uri: Uri) {
        val code = uri.getQueryParameter("code")
        if (code.isNullOrBlank()) {
            val error = uri.getQueryParameter("error_description") ?: "OAuth sign in was cancelled or failed"
            _uiState.update { it.copy(errorMessage = error) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val exchangeResult = OAuthManager.exchangeCodeForToken(code)
            if (exchangeResult.isSuccess) {
                val tokenResult = exchangeResult.getOrThrow()
                val decoded = tokenResult.decodedInfo
                val defaultNickname = if (_uiState.value.nickname.isNotBlank()) {
                    _uiState.value.nickname
                } else if (decoded?.email != null) {
                    "ChatGPT ${decoded.planType.displayName} (${decoded.email.substringBefore('@')})"
                } else {
                    "ChatGPT Plus Account"
                }

                val addResult = addAccountUseCase(
                    nickname = defaultNickname,
                    email = decoded?.email ?: _uiState.value.email.ifBlank { null },
                    apiKey = tokenResult.accessToken,
                    planType = decoded?.planType ?: _uiState.value.planType,
                    organizationId = decoded?.organizationId ?: _uiState.value.organizationId.ifBlank { null },
                    colorHex = _uiState.value.selectedColorHex,
                    isDemoAccount = false
                )

                if (addResult.isSuccess) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = true,
                            createdAccount = addResult.getOrThrow()
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = addResult.exceptionOrNull()?.message ?: "Failed to save OAuth account"
                        )
                    }
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "OAuth token exchange failed"
                    )
                }
            }
        }
    }

    fun submitAccount(isDemo: Boolean = false) {
        val state = _uiState.value
        val nickname = state.nickname.trim()
        val apiKey = state.apiKey.trim()

        if (nickname.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter an account nickname") }
            return
        }

        if (!isDemo && apiKey.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your OpenAI API key or OAuth Token") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = addAccountUseCase(
                nickname = nickname,
                email = state.email.trim().ifBlank { null },
                apiKey = if (isDemo) "demo_simulation_key" else apiKey,
                planType = state.planType,
                organizationId = state.organizationId.trim().ifBlank { null },
                colorHex = state.selectedColorHex,
                isDemoAccount = isDemo
            )

            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSuccess = true,
                        createdAccount = result.getOrThrow()
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to add account"
                    )
                }
            }
        }
    }
}
