package com.pingplace

import android.location.Location
import junit.framework.TestCase
import com.pingplace.data.local.entity.ReminderEntity
import com.pingplace.data.repository.InMemoryPingPlaceRepository
import com.pingplace.domain.NearbyReminderEvaluator
import com.pingplace.location.ConfigurablePlaceSearchProvider
import com.pingplace.location.NearbyPlace
import com.pingplace.location.NearbyPlaceSearchProvider
import com.pingplace.model.BlockedTimeBehavior
import com.pingplace.model.PlaceSearchMode
import com.pingplace.model.TriggerType
import kotlinx.coroutines.runBlocking

/** Uses real Android Location objects; no live network or changes to the user's database. */
@Suppress("DEPRECATION")
class ReminderMonitoringTest : TestCase() {
    private fun fix(speed: Float? = null) = Location("test").apply {
        latitude = 32.9
        longitude = -97.0
        speed?.let { this.speed = it }
    }
    private fun reminder() = ReminderEntity(
        id = 1, title = "Pick up groceries", brandName = "Store", brandQuery = "Store",
        triggerType = TriggerType.DISTANCE, triggerDistanceMeters = 250,
        respectBlockedTimes = BlockedTimeBehavior.IGNORE_GLOBAL,
        createdAtEpochMillis = 0, updatedAtEpochMillis = 0
    )
    private fun place(distance: Double = 100.0) = NearbyPlace("store-1", "Store", "", 32.9, -97.0, distance, 1)
    private fun provider(result: Result<List<NearbyPlace>>) = object : NearbyPlaceSearchProvider {
        override suspend fun searchNearby(query: String, currentLocation: Location, radiusMeters: Double) = result
    }

    fun testWalkingAndUnknownSpeedCanUseLiveLookup() = runBlocking {
        val repository = InMemoryPingPlaceRepository()
        repository.updateSettings { it.copy(placeSearchMode = PlaceSearchMode.LIVE_ONLY) }
        val search = ConfigurablePlaceSearchProvider(repository, provider(Result.success(emptyList())), provider(Result.success(listOf(place()))))
        assertEquals(1, search.searchNearby("Store", fix(), 1609.0).getOrThrow().size)
        assertEquals(1, search.searchNearby("Store", fix(1.4f), 1609.0).getOrThrow().size)
    }

    fun testHybridFallsBackToOfflineWhenNetworkFails() = runBlocking {
        val repository = InMemoryPingPlaceRepository()
        val search = ConfigurablePlaceSearchProvider(repository, provider(Result.success(listOf(place()))), provider(Result.failure(Exception("offline"))))
        assertEquals(1, search.searchNearby("Store", fix(), 1609.0).getOrThrow().size)
    }

    fun testOfflineOnlyNeverCallsNetwork() = runBlocking {
        val repository = InMemoryPingPlaceRepository()
        repository.updateSettings { it.copy(placeSearchMode = PlaceSearchMode.OFFLINE_ONLY) }
        val network = object : NearbyPlaceSearchProvider {
            override suspend fun searchNearby(query: String, currentLocation: Location, radiusMeters: Double): Result<List<NearbyPlace>> = error("Network must not be called")
        }
        val search = ConfigurablePlaceSearchProvider(repository, provider(Result.success(listOf(place()))), network)
        assertEquals(1, search.searchNearby("Store", fix(30f), 1609.0).getOrThrow().size)
    }

    fun testHighwayApproachAlertsBeforeTheSmallConfiguredRadius() = runBlocking {
        val evaluator = NearbyReminderEvaluator(provider(Result.success(listOf(place(1100.0)))))
        assertTrue(evaluator.evaluate(listOf(reminder()), emptyList(), fix(30f)).single().shouldNotifyNow)
        assertFalse(evaluator.evaluate(listOf(reminder()), emptyList(), fix(1.4f)).single().shouldNotifyNow)
    }

    fun testDrivingOnlyReminderStillHonorsItsSpeedRule() = runBlocking {
        val evaluator = NearbyReminderEvaluator(provider(Result.success(listOf(place()))))
        val driving = reminder().copy(requiresDrivingFast = true)
        assertFalse(evaluator.evaluate(listOf(driving), emptyList(), fix()).single().shouldNotifyNow)
        assertTrue(evaluator.evaluate(listOf(driving), emptyList(), fix(30f)).single().shouldNotifyNow)
    }

    fun testBlockedErrandIsExcludedFromMixedBrandNotification() = runBlocking {
        val blocked = reminder().copy(id = 2, title = "Blocked errand",
            respectBlockedTimes = BlockedTimeBehavior.CUSTOM_ALLOWED_HOURS,
            customAllowedDaysOfWeek = (1..7).toSet(), customAllowedStartMinutes = 0, customAllowedEndMinutes = 0)
        val evaluator = NearbyReminderEvaluator(provider(Result.success(listOf(place()))))
        val match = evaluator.evaluate(listOf(reminder(), blocked), emptyList(), fix()).single()
        assertTrue(match.shouldNotifyNow)
        assertEquals(listOf(1L), match.reminders.map { it.id })
        assertFalse(evaluator.evaluate(listOf(blocked), emptyList(), fix()).single().shouldNotifyNow)
    }

    fun testLookupFailureIsNotTreatedAsLeavingAStore() = runBlocking {
        val evaluator = NearbyReminderEvaluator(provider(Result.failure(Exception("Network unavailable"))))
        val match = evaluator.evaluate(listOf(reminder()), emptyList(), fix()).single()
        assertFalse(match.lookupSucceeded)
        assertFalse(match.shouldNotifyNow)
    }

    fun testGeofencesCoverEveryNearbyBranchAtTheConfiguredDistance() = runBlocking {
        val evaluator = NearbyReminderEvaluator(provider(Result.success(listOf(place(), place(2000.0).copy(id = "store-2")))))
        val match = evaluator.evaluate(listOf(reminder().copy(triggerDistanceMeters = 3218)), emptyList(), fix()).single()
        assertEquals(2, match.geofenceTargets.size)
        assertTrue(match.geofenceTargets.all { it.radiusMeters >= 3218f })
    }
}
