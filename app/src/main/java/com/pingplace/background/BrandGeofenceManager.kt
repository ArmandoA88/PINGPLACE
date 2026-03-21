package com.pingplace.background

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.pingplace.location.NearbyPlace

class BrandGeofenceManager(
    context: Context
) {

    private val appContext = context.applicationContext
    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(appContext)

    @SuppressLint("MissingPermission")
    fun registerPlaces(places: List<NearbyPlace>) {
        if (places.isEmpty() || !hasLocationPermission()) return

        val geofences = places.distinctBy { it.id }.take(20).map { place ->
            Geofence.Builder()
                .setRequestId(place.id)
                .setCircularRegion(
                    place.latitude,
                    place.longitude,
                    250f
                )
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .setLoiteringDelay(0)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .build()
        }
        if (geofences.isEmpty()) return

        geofencingClient.addGeofences(
            GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(geofences)
                .build(),
            pendingIntent()
        )
    }

    fun clear() {
        geofencingClient.removeGeofences(pendingIntent())
    }

    private fun pendingIntent(): PendingIntent {
        val intent = Intent(appContext, GeofenceBroadcastReceiver::class.java)
        return PendingIntent.getBroadcast(
            appContext,
            2001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}
