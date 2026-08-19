package com.codex.quota.domain.usecase

import com.codex.quota.domain.repository.CodexAccountRepository

class UpdateAccountUseCase(
    private val repository: CodexAccountRepository
) {
    suspend operator fun invoke(
        accountId: String,
        nickname: String,
        colorHex: String
    ): Result<Unit> {
        val cleanNickname = nickname.trim().ifBlank { "Codex Account" }
        return repository.updateAccount(accountId, cleanNickname, colorHex)
    }
}
