package com.pingplace.location

import android.location.Location
import com.pingplace.data.repository.PingPlaceRepository
import com.pingplace.model.PlaceSearchMode

class ConfigurablePlaceSearchProvider(
    private val repository: PingPlaceRepository,
    private val offlineProvider: NearbyPlaceSearchProvider,
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
                val live = liveProvider.searchNearby(query, currentLocation, radiusMeters)
                val places = (offline.getOrNull().orEmpty() + live.getOrNull().orEmpty())
                    .distinctBy { it.id }.sortedBy { it.distanceMeters }
                if (places.isNotEmpty() || live.isSuccess) Result.success(places) else live
            }
        }
    }
}
