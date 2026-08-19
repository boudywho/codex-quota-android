package com.codex.quota.domain.usecase

import com.codex.quota.domain.model.AccountWithUsage
import com.codex.quota.domain.repository.CodexAccountRepository
import kotlinx.coroutines.flow.Flow

class ObserveAccountsUseCase(
    private val repository: CodexAccountRepository
) {
    operator fun invoke(): Flow<List<AccountWithUsage>> {
        return repository.observeAccounts()
    }
}
