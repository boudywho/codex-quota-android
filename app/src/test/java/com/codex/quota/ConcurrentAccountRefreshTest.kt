package com.codex.quota

import com.codex.quota.data.local.entity.AccountEntity
import com.codex.quota.data.remote.CodexAccountDataSource
import com.codex.quota.data.repository.CodexAccountRepositoryImpl
import com.codex.quota.domain.model.AuthStatus
import com.codex.quota.domain.model.CodexAccount
import com.codex.quota.domain.model.CodexUsage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class ConcurrentAccountRefreshTest {

    @Test
    fun refreshAllAccounts_runsConcurrentlyWithinBoundAndPreservesOrder() = runTest {
        val accountDao = FakeAccountDao()
        repeat(7) { index ->
            accountDao.insert(accountEntity(index))
        }
        val dataSource = GatedDataSource(failingAccountId = "account-2")
        val repository = CodexAccountRepositoryImpl(
            accountDao = accountDao,
            usageSnapshotDao = FakeUsageSnapshotDao(),
            credentialStore = FakeCredentialStore(),
            realDataSource = dataSource,
            mockDataSource = dataSource,
            refreshScope = this
        )

        val refresh = async { repository.refreshAllAccounts() }
        runCurrent()

        assertEquals(4, dataSource.maximumConcurrentRequests.get())
        dataSource.releaseRequests.complete(Unit)

        val usages = refresh.await().getOrThrow()
        assertEquals(
            listOf("account-0", "account-1", "account-3", "account-4", "account-5", "account-6"),
            usages.map { it.accountId }
        )
        assertTrue(dataSource.maximumConcurrentRequests.get() > 1)
        assertTrue(dataSource.maximumConcurrentRequests.get() <= 4)
    }

    private fun accountEntity(index: Int) = AccountEntity(
        id = "account-$index",
        nickname = "Account $index",
        email = null,
        planType = "PLUS",
        organizationId = null,
        colorHex = "#10B981",
        authStatus = AuthStatus.AUTHENTICATED.name,
        isDemoAccount = false,
        orderIndex = index,
        createdAtEpochMs = index.toLong(),
        lastSuccessfulSyncEpochMs = null
    )

    private class GatedDataSource(
        private val failingAccountId: String
    ) : CodexAccountDataSource {
        val releaseRequests = CompletableDeferred<Unit>()
        val maximumConcurrentRequests = AtomicInteger(0)
        private val concurrentRequests = AtomicInteger(0)

        override suspend fun fetchUsage(account: CodexAccount, apiKey: String): Result<CodexUsage> {
            val concurrent = concurrentRequests.incrementAndGet()
            maximumConcurrentRequests.updateAndGet { current -> maxOf(current, concurrent) }
            return try {
                releaseRequests.await()
                if (account.id == failingAccountId) {
                    Result.failure(IllegalStateException("isolated failure"))
                } else {
                    Result.success(CodexUsage.empty(account.id, AuthStatus.AUTHENTICATED))
                }
            } finally {
                concurrentRequests.decrementAndGet()
            }
        }
    }
}
