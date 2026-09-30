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
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class GeofenceTarget(val place: NearbyPlace, val radiusMeters: Float)

class BrandGeofenceManager(
    context: Context
) {

    private val appContext = context.applicationContext
    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(appContext)
    private val preferences = appContext.getSharedPreferences("monitor_geofences", Context.MODE_PRIVATE)
    private val mutex = kotlinx.coroutines.sync.Mutex()
    private var registeredSignature: Set<String> = emptySet()

    @SuppressLint("MissingPermission")
    suspend fun registerPlaces(places: List<GeofenceTarget>) = mutex.withLock {
        if (!hasLocationPermission()) return@withLock
        val targets = places.groupBy { it.place.id }.values.map { group -> group.maxBy { it.radiusMeters } }
            .sortedBy { it.place.distanceMeters }.take(90)
        val signature = targets.map { "${it.place.id}:${it.radiusMeters}:${it.place.latitude}:${it.place.longitude}" }.toSet()
        if (signature == registeredSignature) return@withLock
        try {
            val ids = targets.map { it.place.id }.toSet()
            val stale = preferences.getStringSet("ids", emptySet()).orEmpty() - ids
            if (stale.isNotEmpty()) awaitTask(geofencingClient.removeGeofences(stale.toList()))
            if (targets.isNotEmpty()) {
                val geofences = targets.map { target ->
                    Geofence.Builder()
                        .setRequestId(target.place.id)
                        .setCircularRegion(target.place.latitude, target.place.longitude, target.radiusMeters)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                        .setNotificationResponsiveness(5_000)
                        .setExpirationDuration(Geofence.NEVER_EXPIRE)
                        .build()
                }
                awaitTask(geofencingClient.addGeofences(GeofencingRequest.Builder()
                    .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                    .addGeofences(geofences).build(), pendingIntent()))
            }
            registeredSignature = signature
            preferences.edit().putStringSet("ids", ids).apply()
        } catch (error: kotlinx.coroutines.CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.w(TAG, "Geofence registration will retry on the next check", error)
        }
    }

    suspend fun clear() = mutex.withLock {
        registeredSignature = emptySet()
        try {
            awaitTask(geofencingClient.removeGeofences(pendingIntent()))
            preferences.edit().remove("ids").apply()
        } catch (error: kotlinx.coroutines.CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.w(TAG, "Unable to clear geofences", error)
        }
    }

    private suspend fun awaitTask(task: com.google.android.gms.tasks.Task<Void>) =
        kotlinx.coroutines.suspendCancellableCoroutine<Unit> { continuation ->
            task.addOnSuccessListener { if (continuation.isActive) continuation.resume(Unit) }
                .addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
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
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            appContext, Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }


    private companion object {
        const val TAG = "BrandGeofenceManager"
    }
}
