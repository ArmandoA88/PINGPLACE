package com.pingplace.domain

import android.location.Location
import com.pingplace.data.local.entity.BlockedTimeWindowEntity
import com.pingplace.data.local.entity.ReminderEntity
import com.pingplace.location.NearbyPlace
import com.pingplace.location.NearbyPlaceSearchProvider
import com.pingplace.model.TriggerType

data class BrandReminderMatch(
    val brandName: String,
    val brandQuery: String,
    val reminders: List<ReminderEntity>,
    val nearestPlace: NearbyPlace?,
    val shouldNotifyNow: Boolean,
    val suppressedByBlockedTime: Boolean
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
                val radius = brandReminders.maxOf { reminder ->
                    when (reminder.triggerType) {
                        TriggerType.DISTANCE -> (reminder.triggerDistanceMeters ?: 1609).toDouble()
                        TriggerType.TRAVEL_TIME -> ((reminder.triggerTravelTimeMinutes ?: 10) * 1600).toDouble()
                    }
                }.coerceAtLeast(1609.0)

                val nearbyPlaces = placeSearchProvider.searchNearby(
                    query = sample.brandQuery,
                    currentLocation = currentLocation,
                    radiusMeters = radius * 2.2
                ).getOrElse { emptyList() }

                val nearest = nearbyPlaces.minByOrNull { it.distanceMeters }
                val withinThreshold = nearest != null && brandReminders.any { reminder ->
                    when (reminder.triggerType) {
                        TriggerType.DISTANCE ->
                            nearest.distanceMeters <= (reminder.triggerDistanceMeters ?: 1609)

                        TriggerType.TRAVEL_TIME ->
                            (nearest.estimatedTravelMinutes ?: Int.MAX_VALUE) <=
                                (reminder.triggerTravelTimeMinutes ?: 10)
                    }
                }
                val suppressed = withinThreshold && brandReminders.all { reminder ->
                    !BlockedTimeEvaluator.isReminderAllowedNow(
                        reminder = reminder,
                        blockedWindows = blockedWindows
                    )
                }

                BrandReminderMatch(
                    brandName = sample.brandName,
                    brandQuery = sample.brandQuery,
                    reminders = brandReminders,
                    nearestPlace = nearest,
                    shouldNotifyNow = withinThreshold && !suppressed,
                    suppressedByBlockedTime = suppressed
                )
            }
    }
}
