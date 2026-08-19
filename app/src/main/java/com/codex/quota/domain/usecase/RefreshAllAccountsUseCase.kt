package com.codex.quota.domain.usecase

import com.codex.quota.domain.model.CodexUsage
import com.codex.quota.domain.repository.CodexAccountRepository

class RefreshAllAccountsUseCase(
    private val repository: CodexAccountRepository
) {
    suspend operator fun invoke(): Result<List<CodexUsage>> {
        return repository.refreshAllAccounts()
    }
}
