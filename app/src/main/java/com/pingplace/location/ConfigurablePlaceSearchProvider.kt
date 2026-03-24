package com.pingplace.location

import android.location.Location
import com.pingplace.data.repository.PingPlaceRepository
import com.pingplace.model.PlaceSearchMode
import com.pingplace.offline.OfflinePlaceSearchProvider

class ConfigurablePlaceSearchProvider(
    private val repository: PingPlaceRepository,
    private val offlineProvider: OfflinePlaceSearchProvider,
    private val liveProvider: NearbyPlaceSearchProvider
) : NearbyPlaceSearchProvider {

    override suspend fun searchNearby(
        query: String,
        currentLocation: Location,
        radiusMeters: Double
    ): Result<List<NearbyPlace>> {
        return when (repository.getUserSettings().placeSearchMode) {
            PlaceSearchMode.OFFLINE_ONLY -> offlineProvider.searchNearby(query, currentLocation, radiusMeters)
            PlaceSearchMode.LIVE_ONLY -> liveProvider.searchNearby(query, currentLocation, radiusMeters)
            PlaceSearchMode.HYBRID -> {
                val offline = offlineProvider.searchNearby(query, currentLocation, radiusMeters)
                val offlinePlaces = offline.getOrNull()
                when {
                    offlinePlaces.isNullOrEmpty().not() -> offline
                    else -> liveProvider.searchNearby(query, currentLocation, radiusMeters)
                }
            }
        }
    }
}
