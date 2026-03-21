package com.pingplace.data

import android.content.Context
import com.pingplace.background.BrandGeofenceManager
import com.pingplace.background.MonitorScheduler
import com.pingplace.background.NotificationHelper
import com.pingplace.data.repository.InMemoryPingPlaceRepository
import com.pingplace.data.repository.PingPlaceRepository
import com.pingplace.location.DeviceLocationClient
import com.pingplace.location.GooglePlacesSearchProvider
import com.pingplace.location.NearbyPlaceSearchProvider

class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val repository: PingPlaceRepository = InMemoryPingPlaceRepository()

    val locationClient = DeviceLocationClient(appContext)
    val placeSearchProvider: NearbyPlaceSearchProvider = GooglePlacesSearchProvider()
    val notificationHelper = NotificationHelper(appContext)
    val geofenceManager = BrandGeofenceManager(appContext)
    val monitorScheduler = MonitorScheduler(appContext)
}
