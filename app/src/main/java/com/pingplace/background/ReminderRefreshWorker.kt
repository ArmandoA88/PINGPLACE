package com.pingplace.background

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pingplace.PingPlaceApplication
import com.pingplace.data.local.entity.BrandVisitStateEntity
import com.pingplace.domain.NearbyReminderEvaluator

class ReminderRefreshWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (!hasLocationPermission()) return Result.success()

        val container = (applicationContext as PingPlaceApplication).container
        val repository = container.repository
        val settings = repository.getUserSettings()
        if (!settings.backgroundLocationEnabled) {
            container.geofenceManager.clear()
            return Result.success()
        }
        repository.clearExpiredSnoozes(System.currentTimeMillis())
        val reminders = repository.getReadyToEvaluateReminders()
            .filterNot { it.isCompleted }
            .filterNot { it.isSnoozed && (it.snoozedUntilEpochMillis ?: Long.MAX_VALUE) > System.currentTimeMillis() }

        if (reminders.isEmpty()) {
            container.geofenceManager.clear()
            return Result.success()
        }

        val location = try {
            container.locationClient.getCurrentLocation()
        } catch (_: Exception) {
            null
        } ?: return Result.retry()

        val matches = NearbyReminderEvaluator(container.placeSearchProvider)
            .evaluate(
                reminders = reminders,
                blockedWindows = repository.getEnabledBlockedTimeWindows(),
                currentLocation = location
            )

        val geofencePlaces = matches.mapNotNull { it.nearestPlace }
        container.geofenceManager.registerPlaces(geofencePlaces)

        matches.forEach { match ->
            val previous = repository.getVisitState(match.brandQuery)
            if (match.shouldNotifyNow) {
                val alreadyActive = previous?.isActive == true &&
                    previous.placeId == match.nearestPlace?.id
                if (!alreadyActive && settings.notificationsEnabled) {
                    container.notificationHelper.showBrandReminder(
                        match = match,
                        soundEnabled = settings.soundEnabled
                    )
                }
                repository.saveVisitState(
                    BrandVisitStateEntity(
                        brandQuery = match.brandQuery,
                        placeId = match.nearestPlace?.id,
                        isActive = true,
                        lastDistanceMeters = match.nearestPlace?.distanceMeters,
                        lastNotifiedAtEpochMillis = if (!alreadyActive) System.currentTimeMillis() else previous?.lastNotifiedAtEpochMillis,
                        lastSeenAtEpochMillis = System.currentTimeMillis()
                    )
                )
            } else {
                repository.saveVisitState(
                    BrandVisitStateEntity(
                        brandQuery = match.brandQuery,
                        placeId = match.nearestPlace?.id,
                        isActive = false,
                        lastDistanceMeters = match.nearestPlace?.distanceMeters,
                        lastNotifiedAtEpochMillis = previous?.lastNotifiedAtEpochMillis,
                        lastSeenAtEpochMillis = System.currentTimeMillis()
                    )
                )
            }
        }
        return Result.success()
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}
