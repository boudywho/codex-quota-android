package com.codex.quota.domain.usecase

import com.codex.quota.domain.model.CodexAccount
import com.codex.quota.domain.model.PlanType
import com.codex.quota.domain.repository.CodexAccountRepository

class AddAccountUseCase(
    private val repository: CodexAccountRepository
) {
    suspend operator fun invoke(
        nickname: String,
        email: String?,
        apiKey: String,
        planType: PlanType,
        organizationId: String?,
        colorHex: String,
        isDemoAccount: Boolean = false
    ): Result<CodexAccount> {
        val trimmedKey = apiKey.trim()
        if (!isDemoAccount && trimmedKey.isBlank()) {
            return Result.failure(IllegalArgumentException("API Key cannot be empty."))
        }
        val cleanNickname = nickname.trim().ifBlank { "Codex Account" }
        return repository.addAccount(
            nickname = cleanNickname,
            email = email?.trim()?.ifBlank { null },
            apiKey = trimmedKey,
            planType = planType,
            organizationId = organizationId?.trim()?.ifBlank { null },
            colorHex = colorHex,
            isDemoAccount = isDemoAccount
        )
    }
}
