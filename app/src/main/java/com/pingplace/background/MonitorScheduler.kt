package com.pingplace.background

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class MonitorScheduler(
    context: Context
) {

    private val workManager = WorkManager.getInstance(context)

    fun scheduleMonitoring() {
        val work = PeriodicWorkRequestBuilder<ReminderRefreshWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            MONITOR_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            work
        )
    }

    fun triggerImmediateRefresh() {
        val work = OneTimeWorkRequestBuilder<ReminderRefreshWorker>().build()
        workManager.enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            work
        )
    }

    fun cancelMonitoring() {
        workManager.cancelUniqueWork(IMMEDIATE_WORK_NAME)
        workManager.cancelUniqueWork(MONITOR_WORK_NAME)
    }

    companion object {
        const val MONITOR_WORK_NAME = "pingplace_monitor"
        const val IMMEDIATE_WORK_NAME = "pingplace_monitor_now"
    }
}
