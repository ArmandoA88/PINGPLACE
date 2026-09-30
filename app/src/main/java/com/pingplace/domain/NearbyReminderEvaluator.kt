package com.pingplace.domain

import android.location.Location
import com.pingplace.data.local.entity.BlockedTimeWindowEntity
import com.pingplace.data.local.entity.ReminderEntity
import com.pingplace.background.GeofenceTarget
import com.pingplace.location.NearbyPlace
import com.pingplace.location.NearbyPlaceSearchProvider
import com.pingplace.model.TriggerType

data class BrandReminderMatch(
    val brandName: String,
    val brandQuery: String,
    val reminders: List<ReminderEntity>,
    val nearestPlace: NearbyPlace?,
    val shouldNotifyNow: Boolean,
    val suppressedByBlockedTime: Boolean,
    val lookupSucceeded: Boolean = true,
    val geofenceTargets: List<GeofenceTarget> = emptyList()
)

class NearbyReminderEvaluator(
    private val placeSearchProvider: NearbyPlaceSearchProvider
) {

    suspend fun evaluate(
        reminders: List<ReminderEntity>,
        blockedWindows: List<BlockedTimeWindowEntity>,
        currentLocation: Location
    ): List<BrandReminderMatch> {
        return reminders
            .groupBy { it.brandQuery }
            .values
            .map { brandReminders ->
                val sample = brandReminders.first()
                val isDrivingFast = currentLocation.speed >= MonitoringPolicy.FAST_SPEED_MPS
                val radius = brandReminders.maxOf { reminder ->
                    when (reminder.triggerType) {
                        TriggerType.DISTANCE -> MonitoringPolicy.approachDistanceMeters(reminder.triggerDistanceMeters ?: 1609, currentLocation.speed)
                        TriggerType.TRAVEL_TIME -> ((reminder.triggerTravelTimeMinutes ?: 10) * 1600).toDouble()
                    }
                }.coerceAtLeast(1609.0)

                val lookup = placeSearchProvider.searchNearby(
                    query = sample.brandQuery,
                    currentLocation = currentLocation,
                    radiusMeters = radius * 2.2
                )
                val nearbyPlaces = lookup.getOrElse { emptyList() }

                val nearest = nearbyPlaces.minByOrNull { it.distanceMeters }
                val eligibleReminders = if (nearest == null) {
                    emptyList()
                } else {
                    brandReminders.filter { reminder ->
                        val meetsTrigger = when (reminder.triggerType) {
                            TriggerType.DISTANCE ->
                                nearest.distanceMeters <= MonitoringPolicy.approachDistanceMeters(reminder.triggerDistanceMeters ?: 1609, currentLocation.speed)

                            TriggerType.TRAVEL_TIME ->
                                (nearest.estimatedTravelMinutes ?: Int.MAX_VALUE) <=
                                    (reminder.triggerTravelTimeMinutes ?: 10)
                        }
                        val meetsSpeedRule = !reminder.requiresDrivingFast || isDrivingFast
                        meetsTrigger && meetsSpeedRule
                    }
                }
                val allowedReminders = eligibleReminders.filter { reminder ->
                    BlockedTimeEvaluator.isReminderAllowedNow(reminder, blockedWindows)
                }
                val suppressed = eligibleReminders.isNotEmpty() && allowedReminders.isEmpty()

                BrandReminderMatch(
                    brandName = sample.brandName,
                    brandQuery = sample.brandQuery,
                    reminders = allowedReminders,
                    nearestPlace = nearest,
                    shouldNotifyNow = allowedReminders.isNotEmpty(),
                    suppressedByBlockedTime = suppressed,
                    lookupSucceeded = lookup.isSuccess,
                    geofenceTargets = nearbyPlaces.map { GeofenceTarget(it, radius.coerceIn(250.0, 10_000.0).toFloat()) }
                )
            }
    }
}
