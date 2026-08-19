package com.codex.quota.domain.usecase

import com.codex.quota.domain.model.CodexUsage
import com.codex.quota.domain.repository.CodexAccountRepository

class RefreshAccountUseCase(
    private val repository: CodexAccountRepository
) {
    suspend operator fun invoke(accountId: String): Result<CodexUsage> {
        return repository.refreshAccount(accountId)
    }
}
