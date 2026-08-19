package com.codex.quota

import android.app.Application
import com.codex.quota.data.local.AppDatabase
import com.codex.quota.data.local.DataStoreManager
import com.codex.quota.data.remote.MockOpenAiDataSource
import com.codex.quota.data.remote.RealOpenAiDataSource
import com.codex.quota.data.repository.CodexAccountRepositoryImpl
import com.codex.quota.data.repository.UserPreferencesRepositoryImpl
import com.codex.quota.domain.repository.CodexAccountRepository
import com.codex.quota.domain.repository.UserPreferencesRepository
import com.codex.quota.security.EncryptedCredentialStore
import com.codex.quota.security.KeystoreManager
import com.codex.quota.worker.WorkScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CodexQuotaApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var database: AppDatabase
        private set

    lateinit var credentialStore: EncryptedCredentialStore
        private set

    lateinit var dataStoreManager: DataStoreManager
        private set

    lateinit var repository: CodexAccountRepository
        private set

    lateinit var preferencesRepository: UserPreferencesRepository
        private set

    override fun onCreate() {
        super.onCreate()

        database = AppDatabase.getInstance(this)
        val keystoreManager = KeystoreManager(this)
        credentialStore = EncryptedCredentialStore(this, keystoreManager)
        dataStoreManager = DataStoreManager(this)

        repository = CodexAccountRepositoryImpl(
            accountDao = database.accountDao(),
            usageSnapshotDao = database.usageSnapshotDao(),
            credentialStore = credentialStore,
            realDataSource = RealOpenAiDataSource(),
            mockDataSource = MockOpenAiDataSource()
        )

        preferencesRepository = UserPreferencesRepositoryImpl(dataStoreManager)

        // Schedule periodic background refresh
        applicationScope.launch {
            val prefs = preferencesRepository.getPreferences()
            WorkScheduler.schedulePeriodicRefresh(this@CodexQuotaApplication, prefs.refreshInterval.minutes)
        }
    }

    fun markOnboardingComplete() {
        applicationScope.launch {
            preferencesRepository.setHasCompletedOnboarding(true)
        }
    }
}
