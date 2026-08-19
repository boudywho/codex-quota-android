package com.codex.quota.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.codex.quota.CodexQuotaApplication
import com.codex.quota.domain.model.AuthStatus
import com.codex.quota.notifications.QuotaAlertNotificationManager
import com.codex.quota.notifications.SignedOutNotificationManager
import com.codex.quota.widget.WidgetUpdateHelper

class QuotaRefreshWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

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

        // Check for signed-out transitions (notify only ONCE per sign-out event)
        if (preferences.signedOutNotificationsEnabled) {
            val signedOutAccounts = currentAccounts.filter {
                it.usage?.status == AuthStatus.AUTHENTICATION_REQUIRED ||
                        it.account.authStatus == AuthStatus.AUTHENTICATION_REQUIRED
            }

            for (item in signedOutAccounts) {
                val alreadyNotified = prefsRepo.isSignedOutAlertNotified(item.account.id)
                if (!alreadyNotified) {
                    signedOutNotificationManager.showSignedOutNotification(item.account)
                    prefsRepo.setSignedOutAlertNotified(item.account.id, true)
                }
            }

            // For authenticated accounts, clear the signed-out alert state and dismiss any lingering notification
            val authenticatedAccounts = currentAccounts.filter {
                it.usage?.status == AuthStatus.AUTHENTICATED &&
                        it.account.authStatus == AuthStatus.AUTHENTICATED
            }
            for (item in authenticatedAccounts) {
                val wasNotified = prefsRepo.isSignedOutAlertNotified(item.account.id)
                if (wasNotified) {
                    prefsRepo.setSignedOutAlertNotified(item.account.id, false)
                    signedOutNotificationManager.clearNotification(item.account.id)
                }
            }
        }

        // Check for low quota alerts against multi-select thresholds (notify only ONCE per milestone)
        if (preferences.quotaAlertsEnabled) {
            val thresholds = preferences.quotaAlertThresholds
            val maxThreshold = thresholds.maxOrNull() ?: 25

            for (item in currentAccounts) {
                val usage = item.usage ?: continue
                val remaining = usage.remainingPercent
                if (remaining != null && usage.status == AuthStatus.AUTHENTICATED) {
                    val lastNotifiedThreshold = prefsRepo.getLastNotifiedQuotaThreshold(item.account.id)

                    if (remaining > maxThreshold) {
                        // Quota has recovered/reset back above all alert thresholds: clear notified state for next cycle
                        if (lastNotifiedThreshold != null) {
                            prefsRepo.setLastNotifiedQuotaThreshold(item.account.id, null)
                        }
                    } else {
                        // Find candidate configured thresholds that remaining quota is at or below
                        val candidateThresholds = thresholds.filter { remaining <= it }
                        val currentMilestone = candidateThresholds.minOrNull()

                        if (currentMilestone != null) {
                            // Only trigger notification once when entering this milestone or a lower milestone
                            val shouldNotify = lastNotifiedThreshold == null || currentMilestone < lastNotifiedThreshold
                            if (shouldNotify) {
                                quotaAlertNotificationManager.showLowQuotaAlert(item.account, usage, currentMilestone)
                                prefsRepo.setLastNotifiedQuotaThreshold(item.account.id, currentMilestone)
                            }
                        }
                    }
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
