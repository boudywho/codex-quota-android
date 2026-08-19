package com.codex.quota.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkScheduler {

    fun schedulePeriodicRefresh(context: Context, intervalMinutes: Long) {
        val workManager = WorkManager.getInstance(context)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(false)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<QuotaRefreshWorker>(
            intervalMinutes.coerceAtLeast(15), TimeUnit.MINUTES,
            5, TimeUnit.MINUTES // Flex interval
        )
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            QuotaRefreshWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicWorkRequest
        )
    }

    fun cancelPeriodicRefresh(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(QuotaRefreshWorker.UNIQUE_WORK_NAME)
    }
}
