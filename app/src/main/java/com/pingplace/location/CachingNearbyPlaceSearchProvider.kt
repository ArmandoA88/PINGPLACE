package com.pingplace.location

import android.location.Location
import com.pingplace.offline.OfflinePackManager

class CachingNearbyPlaceSearchProvider(
    private val delegate: NearbyPlaceSearchProvider,
    private val offlinePackManager: OfflinePackManager
) : NearbyPlaceSearchProvider {

    override suspend fun searchNearby(
        query: String,
        currentLocation: Location,
        radiusMeters: Double
    ): Result<List<NearbyPlace>> {
        val result = delegate.searchNearby(query, currentLocation, radiusMeters)
        result.getOrNull()
            ?.takeIf { it.isNotEmpty() }
            ?.let { places ->
                runCatching {
                    offlinePackManager.cacheNearbyResults(
                        query = query,
                        currentLocation = currentLocation,
                        radiusMeters = radiusMeters,
                        places = places
                    )
                }
            }
        return result
    }
}
