package com.pingplace.location

import android.location.Location

data class NearbyPlace(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Double,
    val estimatedTravelMinutes: Int?
)

interface NearbyPlaceSearchProvider {
    suspend fun searchNearby(
        query: String,
        currentLocation: Location,
        radiusMeters: Double
    ): Result<List<NearbyPlace>>
}
