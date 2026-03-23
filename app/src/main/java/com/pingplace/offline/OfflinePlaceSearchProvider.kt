package com.pingplace.offline

import android.location.Location
import com.pingplace.data.local.dao.OfflinePlaceDao
import com.pingplace.data.local.dao.OfflineRegionDao
import com.pingplace.location.NearbyPlace
import com.pingplace.location.NearbyPlaceSearchProvider
import com.pingplace.location.TravelTimeEstimator
import kotlin.math.cos

class OfflinePlaceSearchProvider(
    private val offlinePlaceDao: OfflinePlaceDao,
    private val offlineRegionDao: OfflineRegionDao
) : NearbyPlaceSearchProvider {

    override suspend fun searchNearby(
        query: String,
        currentLocation: Location,
        radiusMeters: Double
    ): Result<List<NearbyPlace>> = runCatching {
        val normalizedQuery = query.lowercase().trim()
        val coveringRegions = offlineRegionDao.countCovering(
            latitude = currentLocation.latitude,
            longitude = currentLocation.longitude
        )
        if (coveringRegions == 0) {
            throw OfflineCoverageMissingException()
        }

        val latitudeDelta = radiusMeters / 111_320.0
        val longitudeDelta = radiusMeters / (111_320.0 * cos(Math.toRadians(currentLocation.latitude)).coerceAtLeast(0.1))
        offlinePlaceDao.searchCandidates(
            normalizedQuery = normalizedQuery,
            minLatitude = currentLocation.latitude - latitudeDelta,
            maxLatitude = currentLocation.latitude + latitudeDelta,
            minLongitude = currentLocation.longitude - longitudeDelta,
            maxLongitude = currentLocation.longitude + longitudeDelta,
            limit = 250
        ).map { place ->
            val placeLocation = Location("offline").apply {
                latitude = place.latitude
                longitude = place.longitude
            }
            val distance = currentLocation.distanceTo(placeLocation).toDouble()
            NearbyPlace(
                id = place.id,
                name = place.name,
                address = place.address,
                latitude = place.latitude,
                longitude = place.longitude,
                distanceMeters = distance,
                estimatedTravelMinutes = TravelTimeEstimator.estimateMinutes(distance, currentLocation)
            )
        }
            .filter { it.distanceMeters <= radiusMeters }
            .sortedBy { it.distanceMeters }
            .take(25)
    }

    class OfflineCoverageMissingException :
        IllegalStateException("No offline region is installed for the current location.")
}
