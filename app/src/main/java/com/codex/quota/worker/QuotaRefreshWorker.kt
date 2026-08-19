package com.codex.quota.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.codex.quota.CodexQuotaApplication
import com.codex.quota.domain.model.AuthStatus
import com.codex.quota.domain.usecase.CheckSignedOutAccountsUseCase
import com.codex.quota.notifications.QuotaAlertNotificationManager
import com.codex.quota.notifications.SignedOutNotificationManager
import com.codex.quota.widget.WidgetUpdateHelper

class QuotaRefreshWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val checkSignedOutUseCase = CheckSignedOutAccountsUseCase()

    override suspend fun doWork(): Result {
        val app = applicationContext as? CodexQuotaApplication ?: return Result.failure()
        val repository = app.repository
        val prefsRepo = app.preferencesRepository
        val signedOutNotificationManager = SignedOutNotificationManager(applicationContext)
        val quotaAlertNotificationManager = QuotaAlertNotificationManager(applicationContext)

        val preferences = prefsRepo.getPreferences()

        val refreshResult = repository.refreshAllAccounts()

        // Fetch refreshed accounts
        val currentAccounts = repository.getAllAccounts()

        // Check for signed-out transitions
        if (preferences.signedOutNotificationsEnabled) {
            val signedOutAccounts = currentAccounts.filter {
                it.usage?.status == AuthStatus.AUTHENTICATION_REQUIRED ||
                        it.account.authStatus == AuthStatus.AUTHENTICATION_REQUIRED
            }

            for (item in signedOutAccounts) {
                signedOutNotificationManager.showSignedOutNotification(item.account)
            }
        }

        // Check for low quota alerts
        if (preferences.quotaAlertsEnabled) {
            val threshold = preferences.quotaAlertThresholdPercent
            for (item in currentAccounts) {
                val usage = item.usage ?: continue
                val remaining = usage.remainingPercent
                if (remaining != null && remaining <= threshold && usage.status == AuthStatus.AUTHENTICATED) {
                    quotaAlertNotificationManager.showLowQuotaAlert(item.account, usage, threshold)
                }
            }
        }

        // Update home-screen widgets
        WidgetUpdateHelper.updateAllWidgets(applicationContext)

        return if (refreshResult.isSuccess) {
            Result.success()
        } else {
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "periodic_codex_quota_refresh"
    }
}
