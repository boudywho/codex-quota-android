package com.codex.quota

import com.codex.quota.data.local.dao.AccountDao
import com.codex.quota.data.local.dao.UsageSnapshotDao
import com.codex.quota.data.local.entity.AccountEntity
import com.codex.quota.data.local.entity.UsageSnapshotEntity
import com.codex.quota.data.remote.MockOpenAiDataSource
import com.codex.quota.data.repository.CodexAccountRepositoryImpl
import com.codex.quota.domain.model.AuthStatus
import com.codex.quota.domain.model.PlanType
import com.codex.quota.security.CredentialStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CodexAccountRepositoryTest {

    private lateinit var fakeAccountDao: FakeAccountDao
    private lateinit var fakeUsageDao: FakeUsageSnapshotDao
    private lateinit var fakeCredentialStore: FakeCredentialStore
    private lateinit var repository: CodexAccountRepositoryImpl

    @Before
    fun setUp() {
        fakeAccountDao = FakeAccountDao()
        fakeUsageDao = FakeUsageSnapshotDao()
        fakeCredentialStore = FakeCredentialStore()
        repository = CodexAccountRepositoryImpl(
            accountDao = fakeAccountDao,
            usageSnapshotDao = fakeUsageDao,
            credentialStore = fakeCredentialStore,
            realDataSource = MockOpenAiDataSource(),
            mockDataSource = MockOpenAiDataSource()
        )
    }

    @Test
    fun addAccount_persistsAccountAndCredentials() = runTest {
        val result = repository.addAccount(
            nickname = "Primary Key",
            email = "user@test.org",
            apiKey = "sk-test-12345",
            planType = PlanType.PLUS,
            organizationId = "org-123",
            colorHex = "#10B981",
            isDemoAccount = true
        )

        assertTrue(result.isSuccess)
        val account = result.getOrThrow()
        assertEquals("Primary Key", account.nickname)

        // Verify stored in credentials
        val storedKey = fakeCredentialStore.getApiKey(account.id)
        assertEquals("sk-test-12345", storedKey)

        // Verify saved in DAO
        val savedEntity = fakeAccountDao.getById(account.id)
        assertNotNull(savedEntity)
        assertEquals("Primary Key", savedEntity?.nickname)
    }

    @Test
    fun removeAccount_cleansUpDatabaseAndCredentials() = runTest {
        val addResult = repository.addAccount(
            nickname = "To Delete",
            email = null,
            apiKey = "sk-delete-me",
            planType = PlanType.PLUS,
            organizationId = null,
            colorHex = "#EF4444",
            isDemoAccount = true
        )
        val accountId = addResult.getOrThrow().id

        assertNotNull(fakeCredentialStore.getApiKey(accountId))

        val removeResult = repository.removeAccount(accountId)
        assertTrue(removeResult.isSuccess)

        assertNull(fakeCredentialStore.getApiKey(accountId))
        assertNull(fakeAccountDao.getById(accountId))
    }

    @Test
    fun updateAccount_updatesNicknameAndColor() = runTest {
        val addResult = repository.addAccount(
            nickname = "Original",
            email = null,
            apiKey = "sk-update",
            planType = PlanType.PLUS,
            organizationId = null,
            colorHex = "#10B981",
            isDemoAccount = true
        )
        val accountId = addResult.getOrThrow().id

        repository.updateAccount(accountId, "New Nickname", "#38BDF8")

        val updated = fakeAccountDao.getById(accountId)
        assertEquals("New Nickname", updated?.nickname)
        assertEquals("#38BDF8", updated?.colorHex)
    }
}

// In-Memory Fakes for deterministic unit testing
class FakeAccountDao : AccountDao {
    private val accounts = MutableStateFlow<Map<String, AccountEntity>>(emptyMap())

    override fun observeAll(): Flow<List<AccountEntity>> =
        accounts.map { it.values.sortedBy { a -> a.orderIndex } }

    override fun observeById(accountId: String): Flow<AccountEntity?> =
        accounts.map { it[accountId] }

    override suspend fun getById(accountId: String): AccountEntity? =
        accounts.value[accountId]

    override suspend fun getAll(): List<AccountEntity> =
        accounts.value.values.sortedBy { it.orderIndex }

    override suspend fun insert(account: AccountEntity) {
        accounts.value = accounts.value + (account.id to account)
    }

    override suspend fun update(account: AccountEntity) {
        accounts.value = accounts.value + (account.id to account)
    }

    override suspend fun updateNicknameAndColor(accountId: String, nickname: String, colorHex: String) {
        val existing = accounts.value[accountId] ?: return
        accounts.value = accounts.value + (accountId to existing.copy(nickname = nickname, colorHex = colorHex))
    }

    override suspend fun updateAuthStatusAndSyncTime(accountId: String, authStatus: String, lastSync: Long?) {
        val existing = accounts.value[accountId] ?: return
        accounts.value = accounts.value + (accountId to existing.copy(authStatus = authStatus, lastSuccessfulSyncEpochMs = lastSync))
    }

    override suspend fun deleteById(accountId: String) {
        accounts.value = accounts.value - accountId
    }

    override suspend fun deleteAll() {
        accounts.value = emptyMap()
    }

    override suspend fun updateOrderIndex(id: String, orderIndex: Int) {
        val existing = accounts.value[id] ?: return
        accounts.value = accounts.value + (id to existing.copy(orderIndex = orderIndex))
    }
}

class FakeUsageSnapshotDao : UsageSnapshotDao {
    private val snapshots = MutableStateFlow<Map<String, UsageSnapshotEntity>>(emptyMap())

    override fun observeAll(): Flow<List<UsageSnapshotEntity>> =
        snapshots.map { it.values.toList() }

    override fun observeByAccountId(accountId: String): Flow<UsageSnapshotEntity?> =
        snapshots.map { it[accountId] }

    override suspend fun getByAccountId(accountId: String): UsageSnapshotEntity? =
        snapshots.value[accountId]

    override suspend fun getAll(): List<UsageSnapshotEntity> =
        snapshots.value.values.toList()

    override suspend fun insertOrUpdate(snapshot: UsageSnapshotEntity) {
        snapshots.value = snapshots.value + (snapshot.accountId to snapshot)
    }

    override suspend fun deleteByAccountId(accountId: String) {
        snapshots.value = snapshots.value - accountId
    }

    override suspend fun deleteAll() {
        snapshots.value = emptyMap()
    }
}

class FakeCredentialStore : CredentialStore {
    private val storage = mutableMapOf<String, String>()

    override fun storeApiKey(accountId: String, apiKey: String) {
        storage[accountId] = apiKey
    }

    override fun getApiKey(accountId: String): String? = storage[accountId]

    override fun removeApiKey(accountId: String) {
        storage.remove(accountId)
    }

    override fun clearAll() {
        storage.clear()
    }
}
