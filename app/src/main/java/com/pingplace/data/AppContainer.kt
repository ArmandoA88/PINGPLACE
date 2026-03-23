package com.pingplace.data

import android.content.Context
import androidx.room.Room
import com.pingplace.background.BrandGeofenceManager
import com.pingplace.background.MonitorScheduler
import com.pingplace.background.NotificationHelper
import com.pingplace.data.local.PingPlaceDatabase
import com.pingplace.data.repository.DefaultPingPlaceRepository
import com.pingplace.data.repository.PingPlaceRepository
import com.pingplace.location.ConfigurablePlaceSearchProvider
import com.pingplace.location.DeviceLocationClient
import com.pingplace.location.GooglePlacesSearchProvider
import com.pingplace.location.NearbyPlaceSearchProvider
import com.pingplace.offline.OfflinePackManager
import com.pingplace.offline.OfflinePlaceSearchProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    private val database = Room.databaseBuilder(
        appContext,
        PingPlaceDatabase::class.java,
        "pingplace.db"
    )
        .fallbackToDestructiveMigration()
        .build()
    private val httpClient = OkHttpClient()
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val repository: PingPlaceRepository = DefaultPingPlaceRepository(
        reminderDao = database.reminderDao(),
        blockedTimeWindowDao = database.blockedTimeWindowDao(),
        userSettingsDao = database.userSettingsDao(),
        brandVisitStateDao = database.brandVisitStateDao()
    )

    val locationClient = DeviceLocationClient(appContext)
    val offlinePackManager = OfflinePackManager(
        context = appContext,
        database = database,
        client = httpClient
    )
    private val offlinePlaceSearchProvider = OfflinePlaceSearchProvider(
        offlinePlaceDao = database.offlinePlaceDao(),
        offlineRegionDao = database.offlineRegionDao()
    )
    private val livePlaceSearchProvider = GooglePlacesSearchProvider(client = httpClient)
    val placeSearchProvider: NearbyPlaceSearchProvider = ConfigurablePlaceSearchProvider(
        repository = repository,
        offlineProvider = offlinePlaceSearchProvider,
        liveProvider = livePlaceSearchProvider
    )
    val notificationHelper = NotificationHelper(appContext)
    val geofenceManager = BrandGeofenceManager(appContext)
    val monitorScheduler = MonitorScheduler(appContext)

    init {
        ioScope.launch {
            offlinePackManager.removeLegacySamplePacks()
        }
    }
}
