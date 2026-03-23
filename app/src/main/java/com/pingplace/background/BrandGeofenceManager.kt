package com.pingplace.background

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
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
        if (!hasLocationPermission()) {
            clear()
            return
        }

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
        if (geofences.isEmpty()) {
            clear()
            return
        }

        geofencingClient.removeGeofences(pendingIntent())
            .addOnCompleteListener {
                geofencingClient.addGeofences(
                    GeofencingRequest.Builder()
                        .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                        .addGeofences(geofences)
                        .build(),
                    pendingIntent()
                )
                    .addOnFailureListener { Log.w(TAG, "Failed to register geofences", it) }
            }
    }

    fun clear() {
        geofencingClient.removeGeofences(pendingIntent())
    }

    private fun pendingIntent(): PendingIntent {
        val intent = Intent(appContext, GeofenceBroadcastReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE
            } else {
                0
            }
        return PendingIntent.getBroadcast(
            appContext,
            2001,
            intent,
            flags
        )
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private companion object {
        const val TAG = "BrandGeofenceManager"
    }
}
