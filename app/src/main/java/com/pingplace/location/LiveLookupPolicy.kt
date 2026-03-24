package com.pingplace.location

import android.location.Location

object LiveLookupPolicy {

    const val MIN_FAST_MOVEMENT_SPEED_MPS = 8.94f

    fun allowsLiveLookup(currentLocation: Location): Boolean {
        return currentLocation.hasSpeed() && currentLocation.speed >= MIN_FAST_MOVEMENT_SPEED_MPS
    }
}

class LiveLookupDeferredException :
    IllegalStateException("Live nearby lookup waits until fast movement is detected.")
