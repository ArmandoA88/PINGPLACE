package com.pingplace.location

import android.location.Location
import kotlin.math.roundToInt
import kotlin.math.max

object TravelTimeEstimator {

    fun estimateMinutes(distanceMeters: Double, currentLocation: Location): Int {
        val speedMetersPerSecond = when {
            currentLocation.hasSpeed() && currentLocation.speed > 2f -> currentLocation.speed.toDouble()
            else -> 9.8
        }
        return max(1, (distanceMeters / speedMetersPerSecond / 60.0).roundToInt())
    }
}
