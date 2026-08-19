package com.codex.quota.domain.repository

import com.codex.quota.domain.model.AccountWithUsage
import com.codex.quota.domain.model.CodexAccount
import com.codex.quota.domain.model.CodexUsage
import com.codex.quota.domain.model.PlanType
import kotlinx.coroutines.flow.Flow

interface CodexAccountRepository {
    fun observeAccounts(): Flow<List<AccountWithUsage>>
    fun observeAccount(accountId: String): Flow<AccountWithUsage?>
    suspend fun getAccount(accountId: String): AccountWithUsage?
    suspend fun getAllAccounts(): List<AccountWithUsage>
    
    suspend fun addAccount(
        nickname: String,
        email: String?,
        apiKey: String,
        planType: PlanType,
        organizationId: String?,
        colorHex: String,
        isDemoAccount: Boolean = false
    ): Result<CodexAccount>

    suspend fun updateAccount(
        accountId: String,
        nickname: String,
        colorHex: String
    ): Result<Unit>

    suspend fun reauthenticateAccount(
        accountId: String,
        newApiKey: String
    ): Result<CodexUsage>

    suspend fun removeAccount(accountId: String): Result<Unit>
    suspend fun reorderAccounts(accountIdsInOrder: List<String>): Result<Unit>

    suspend fun refreshAccount(accountId: String): Result<CodexUsage>
    suspend fun refreshAllAccounts(): Result<List<CodexUsage>>
    suspend fun clearAllData(): Result<Unit>
}
