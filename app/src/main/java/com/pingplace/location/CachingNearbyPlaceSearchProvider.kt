package com.pingplace.location

import android.location.Location
import android.os.SystemClock
import com.pingplace.offline.OfflinePackManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Cache coordinates, never old distances: every fix recomputes distance and ETA. */
class CachingNearbyPlaceSearchProvider(
    private val delegate: NearbyPlaceSearchProvider,
    private val offlinePackManager: OfflinePackManager
) : NearbyPlaceSearchProvider {
    private data class Entry(val center: Location, val radius: Double, val savedAt: Long, val places: List<NearbyPlace>)
    private val cache = mutableMapOf<String, Entry>()
    private val failures = mutableMapOf<String, Pair<Long, Throwable>>()
    private val mutex = Mutex()

    override suspend fun searchNearby(query: String, currentLocation: Location, radiusMeters: Double): Result<List<NearbyPlace>> = mutex.withLock {
        val key = query.trim().lowercase(java.util.Locale.ROOT)
        val now = SystemClock.elapsedRealtime()
        val cached = cache[key]
        fun updated(places: List<NearbyPlace>) = places.map { place ->
            val target = Location("place").apply { latitude = place.latitude; longitude = place.longitude }
            val distance = currentLocation.distanceTo(target).toDouble()
            place.copy(distanceMeters = distance, estimatedTravelMinutes = TravelTimeEstimator.estimateMinutes(distance, currentLocation))
        }.filter { it.distanceMeters <= radiusMeters }.sortedBy { it.distanceMeters }
        val covered = cached != null && cached.center.distanceTo(currentLocation) + minOf(radiusMeters, 8_000.0) <= cached.radius
        if (cached != null && covered && now - cached.savedAt < 5 * 60_000) {
            return@withLock Result.success(updated(cached.places))
        }
        failures[key]?.let { (failedAt, error) ->
            if (now - failedAt < 60_000) {
                val fallback = cached?.let { updated(it.places) }.orEmpty()
                return@withLock if (fallback.isNotEmpty()) Result.success(fallback) else Result.failure(error)
            }
        }
        val searchRadius = (radiusMeters + 2_000).coerceAtMost(10_000.0)
        val result = delegate.searchNearby(query, currentLocation, searchRadius)
        result.exceptionOrNull()?.let { error ->
            if (error is CancellationException) throw error
            failures[key] = now to error
            val fallback = cached?.let { updated(it.places) }.orEmpty()
            return@withLock if (fallback.isNotEmpty()) Result.success(fallback) else Result.failure(error)
        }
        val places = result.getOrThrow()
        cache[key] = Entry(Location(currentLocation), searchRadius, now, places)
        failures.remove(key)
        if (places.isNotEmpty()) {
            try {
                offlinePackManager.cacheNearbyResults(query, currentLocation, searchRadius, places)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                // A disk-cache failure must not discard successful live results.
            }
        }
        Result.success(updated(places))
    }
}
