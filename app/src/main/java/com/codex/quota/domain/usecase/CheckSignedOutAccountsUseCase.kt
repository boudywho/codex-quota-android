package com.codex.quota.domain.usecase

import com.codex.quota.domain.model.AccountWithUsage
import com.codex.quota.domain.model.AuthStatus
import com.codex.quota.domain.model.CodexAccount

class CheckSignedOutAccountsUseCase {
    /**
     * Identifies accounts that are currently in AUTHENTICATION_REQUIRED state
     * and were not previously recorded as already alerted in [previouslyAlertedIds].
     */
    operator fun invoke(
        currentAccounts: List<AccountWithUsage>,
        previouslyAlertedIds: Set<String>
    ): List<CodexAccount> {
        return currentAccounts
            .filter { item ->
                val isSignedOut = item.usage?.status == AuthStatus.AUTHENTICATION_REQUIRED ||
                        item.account.authStatus == AuthStatus.AUTHENTICATION_REQUIRED
                isSignedOut && !previouslyAlertedIds.contains(item.account.id)
            }
            .map { it.account }
    }
}
