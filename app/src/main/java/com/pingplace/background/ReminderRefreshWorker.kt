package com.pingplace.background

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pingplace.PingPlaceApplication
import kotlinx.coroutines.CancellationException

/** Recovery when live monitoring is unavailable; also handles geofence wakeups. */
class ReminderRefreshWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as PingPlaceApplication).container
        if (!container.repository.getUserSettings().backgroundLocationEnabled) {
            container.geofenceManager.clear()
            return Result.success()
        }
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return Result.success()
        return try {
            val location = container.locationClient.getCurrentLocation() ?: return Result.retry()
            container.reminderMonitor.evaluate(location)
            Result.success()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
