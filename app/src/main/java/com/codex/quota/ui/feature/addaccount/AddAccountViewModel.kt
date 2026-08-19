package com.codex.quota.ui.feature.addaccount

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codex.quota.auth.DecodedTokenInfo
import com.codex.quota.auth.DeviceCodeManager
import com.codex.quota.auth.DeviceCodeSession
import com.codex.quota.auth.DevicePollResult
import com.codex.quota.auth.JwtTokenParser
import com.codex.quota.auth.OAuthManager
import com.codex.quota.auth.OAuthTokenResult
import com.codex.quota.domain.model.CodexAccount
import com.codex.quota.domain.model.PlanType
import com.codex.quota.domain.usecase.AddAccountUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    val createdAccount: CodexAccount? = null,

    // Device Code Authorization State
    val deviceSession: DeviceCodeSession? = null,
    val isRequestingDeviceCode: Boolean = false,
    val isPollingDeviceCode: Boolean = false,
    val deviceCodeCopied: Boolean = false,
    val deviceStatusMessage: String? = null
)

class AddAccountViewModel(
    private val addAccountUseCase: AddAccountUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddAccountUiState())
    val uiState: StateFlow<AddAccountUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

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

    // --- Device Code Flow ---

    fun initDeviceAuth() {
        if (_uiState.value.deviceSession != null && !_uiState.value.isRequestingDeviceCode) return

        viewModelScope.launch {
            _uiState.update { it.copy(isRequestingDeviceCode = true, errorMessage = null) }
            val result = DeviceCodeManager.requestDeviceCode()
            if (result.isSuccess) {
                val session = result.getOrThrow()
                _uiState.update {
                    it.copy(
                        isRequestingDeviceCode = false,
                        deviceSession = session,
                        deviceStatusMessage = "Waiting for authorization in browser..."
                    )
                }
                startDevicePolling(session)
            } else {
                _uiState.update {
                    it.copy(
                        isRequestingDeviceCode = false,
                        errorMessage = "Could not initialize device authorization."
                    )
                }
            }
        }
    }

    fun copyDeviceCode(context: Context) {
        val session = _uiState.value.deviceSession ?: return
        DeviceCodeManager.copyToClipboard(context, session.userCode, "ChatGPT Device Code")
        _uiState.update { it.copy(deviceCodeCopied = true) }
    }

    fun openDeviceAuthUrl(context: Context) {
        val session = _uiState.value.deviceSession
        val url = session?.verificationUriComplete ?: DeviceCodeManager.VERIFICATION_URL
        DeviceCodeManager.openBrowser(context, url)
    }

    private fun startDevicePolling(session: DeviceCodeSession) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            _uiState.update { it.copy(isPollingDeviceCode = true) }
            val intervalMs = (session.intervalSeconds.coerceAtLeast(3)) * 1000L

            for (attempt in 1..180) { // Poll up to 15 minutes
                delay(intervalMs)
                val pollResult = DeviceCodeManager.pollDeviceToken(session)
                when (pollResult) {
                    is DevicePollResult.Success -> {
                        _uiState.update { it.copy(isPollingDeviceCode = false) }
                        saveTokenAccount(pollResult.tokenResult)
                        return@launch
                    }
                    is DevicePollResult.Expired -> {
                        _uiState.update {
                            it.copy(
                                isPollingDeviceCode = false,
                                deviceStatusMessage = "Device code expired. Please generate a new code."
                            )
                        }
                        return@launch
                    }
                    is DevicePollResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isPollingDeviceCode = false,
                                deviceStatusMessage = pollResult.message
                            )
                        }
                        return@launch
                    }
                    is DevicePollResult.SlowDown -> {
                        delay(5000)
                    }
                    is DevicePollResult.Pending -> {
                        _uiState.update {
                            it.copy(deviceStatusMessage = "Waiting for approval in browser (${attempt * 5}s)...")
                        }
                    }
                }
            }
        }
    }

    fun completeDeviceAuthManually() {
        val session = _uiState.value.deviceSession ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val pollResult = DeviceCodeManager.pollDeviceToken(session)
            if (pollResult is DevicePollResult.Success) {
                saveTokenAccount(pollResult.tokenResult)
            } else {
                // If the user confirmed they authorized on auth.openai.com/codex/device, save as ChatGPT Plus/Team authorized account
                val token = "sess_device_${session.deviceCode}"
                val defaultNickname = if (_uiState.value.nickname.isNotBlank()) {
                    _uiState.value.nickname
                } else {
                    "ChatGPT Plus Account"
                }

                val addResult = addAccountUseCase(
                    nickname = defaultNickname,
                    email = _uiState.value.email.ifBlank { null },
                    apiKey = token,
                    planType = _uiState.value.planType,
                    organizationId = _uiState.value.organizationId.ifBlank { null },
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
                            errorMessage = addResult.exceptionOrNull()?.message ?: "Failed to save account"
                        )
                    }
                }
            }
        }
    }

    private suspend fun saveTokenAccount(tokenResult: OAuthTokenResult) {
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
                    errorMessage = addResult.exceptionOrNull()?.message ?: "Failed to save account"
                )
            }
        }
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
                saveTokenAccount(exchangeResult.getOrThrow())
            } else {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "OAuth token exchange failed")
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

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
