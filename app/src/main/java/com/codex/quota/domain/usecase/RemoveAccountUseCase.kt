package com.codex.quota.domain.usecase

import com.codex.quota.domain.repository.CodexAccountRepository

class RemoveAccountUseCase(
    private val repository: CodexAccountRepository
) {
    suspend operator fun invoke(accountId: String): Result<Unit> {
        return repository.removeAccount(accountId)
    }
}
