package com.pingplace.background

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class MonitorScheduler(
    context: Context
) {

    private val appContext = context.applicationContext
    private val workManager = WorkManager.getInstance(context)

    fun scheduleMonitoring() {
        val work = PeriodicWorkRequestBuilder<ReminderRefreshWorker>(15, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork(
            MONITOR_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            work
        )
        startLiveMonitoring()
    }

    fun startLiveMonitoring() {
        try {
            ContextCompat.startForegroundService(appContext, Intent(appContext, MovementMonitorService::class.java))
        } catch (error: IllegalStateException) {
            // Android may restrict a background restart. The activity/next geofence retries.
            Log.w("MonitorScheduler", "Live monitoring will resume when the app opens", error)
        } catch (error: SecurityException) {
            Log.w("MonitorScheduler", "Location permission is required", error)
        }
    }

    fun triggerImmediateRefresh() {
        val work = OneTimeWorkRequestBuilder<ReminderRefreshWorker>().build()
        workManager.enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            work
        )
    }

    fun cancelMonitoring() {
        appContext.stopService(Intent(appContext, MovementMonitorService::class.java))
        workManager.cancelUniqueWork(IMMEDIATE_WORK_NAME)
        workManager.cancelUniqueWork(MONITOR_WORK_NAME)
    }

    companion object {
        const val MONITOR_WORK_NAME = "pingplace_monitor"
        const val IMMEDIATE_WORK_NAME = "pingplace_monitor_now"
    }
}
