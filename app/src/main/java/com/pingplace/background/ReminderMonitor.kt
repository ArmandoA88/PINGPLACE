package com.pingplace.background

import android.location.Location
import com.pingplace.data.AppContainer
import com.pingplace.data.local.entity.BrandVisitStateEntity
import com.pingplace.domain.MonitoringPolicy
import com.pingplace.domain.NearbyReminderEvaluator
import com.pingplace.domain.BlockedTimeEvaluator
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Serializes worker and live fixes so one visit cannot emit duplicate alerts. */
class ReminderMonitor(private val container: AppContainer) {
    private val mutex = Mutex()

    suspend fun evaluate(location: Location) = mutex.withLock {
        val repository = container.repository
        val settings = repository.getUserSettings()
        if (!settings.backgroundLocationEnabled) {
            container.geofenceManager.clear()
            return@withLock
        }
        val now = System.currentTimeMillis()
        repository.clearExpiredSnoozes(now)
        val reminders = repository.getReadyToEvaluateReminders()
            .filterNot { it.isCompleted || (it.isSnoozed && (it.snoozedUntilEpochMillis ?: Long.MAX_VALUE) > now) }
        if (reminders.isEmpty()) {
            container.geofenceManager.clear()
            return@withLock
        }
        val matches = NearbyReminderEvaluator(container.placeSearchProvider).evaluate(
            reminders, repository.getEnabledBlockedTimeWindows(), location
        )
        currentCoroutineContext().ensureActive()
        // A lookup can finish after the user snoozes/completes an errand or disables alerts.
        val latestSettings = repository.getUserSettings()
        if (!latestSettings.backgroundLocationEnabled) return@withLock
        val readyIds = repository.getReadyToEvaluateReminders().filterNot { it.isCompleted || it.isSnoozed }.map { it.id }.toSet()
        val blockedWindows = repository.getEnabledBlockedTimeWindows()
        // Preserve existing fences on a network failure instead of losing coverage.
        if (matches.all { it.lookupSucceeded }) {
            container.geofenceManager.registerPlaces(matches.flatMap { it.geofenceTargets })
        }
        matches.filter { it.lookupSucceeded }.forEach { original ->
            val allowed = original.reminders.filter { it.id in readyIds && BlockedTimeEvaluator.isReminderAllowedNow(it, blockedWindows) }
            val match = original.copy(reminders = allowed, shouldNotifyNow = original.shouldNotifyNow && allowed.isNotEmpty())
            val previous = repository.getVisitState(match.brandQuery)
            val sent = match.shouldNotifyNow && latestSettings.notificationsEnabled &&
                MonitoringPolicy.shouldAlert(previous, match.nearestPlace?.id, now, location.speed) &&
                container.notificationHelper.showBrandReminder(match, latestSettings.soundEnabled)
            repository.saveVisitState(BrandVisitStateEntity(
                brandQuery = match.brandQuery,
                placeId = if (sent) match.nearestPlace?.id else previous?.placeId,
                isActive = match.shouldNotifyNow,
                lastDistanceMeters = match.nearestPlace?.distanceMeters,
                lastNotifiedAtEpochMillis = if (sent) now else previous?.lastNotifiedAtEpochMillis,
                lastSeenAtEpochMillis = now
            ))
        }
    }
}
