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
        fun liveLookupDeferred(): Result<List<NearbyPlace>> {
            return Result.failure(LiveLookupDeferredException())
        }

        return when (repository.getUserSettings().placeSearchMode) {
            PlaceSearchMode.OFFLINE_ONLY -> offlineProvider.searchNearby(query, currentLocation, radiusMeters)
            PlaceSearchMode.LIVE_ONLY -> {
                if (LiveLookupPolicy.allowsLiveLookup(currentLocation)) {
                    liveProvider.searchNearby(query, currentLocation, radiusMeters)
                } else {
                    liveLookupDeferred()
                }
            }

            PlaceSearchMode.HYBRID -> {
                val offline = offlineProvider.searchNearby(query, currentLocation, radiusMeters)
                val offlinePlaces = offline.getOrNull()
                when {
                    offlinePlaces.isNullOrEmpty().not() -> offline
                    LiveLookupPolicy.allowsLiveLookup(currentLocation) ->
                        liveProvider.searchNearby(query, currentLocation, radiusMeters)

                    else -> liveLookupDeferred()
                }
            }
        }
    }
}
