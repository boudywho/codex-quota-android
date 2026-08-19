package com.codex.quota.domain.usecase

import com.codex.quota.domain.repository.CodexAccountRepository

class UpdateAccountUseCase(
    private val repository: CodexAccountRepository
) {
    suspend operator fun invoke(
        accountId: String,
        nickname: String,
        colorHex: String,
        customRenewalDateEpochMs: Long? = null
    ): Result<Unit> {
        val cleanNickname = nickname.trim().ifBlank { "Codex Account" }
        return repository.updateAccount(accountId, cleanNickname, colorHex, customRenewalDateEpochMs)
    }

    suspend fun setRenewalDate(
        accountId: String,
        renewalDateEpochMs: Long?
    ): Result<Unit> {
        return repository.setAccountRenewalDate(accountId, renewalDateEpochMs)
    }
}
