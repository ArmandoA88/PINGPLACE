package com.pingplace.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class DeviceLocationClient(context: Context) {

    private val fusedLocationProviderClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        val freshLocation = runCatching { requestCurrentLocation() }.getOrNull()
        if (freshLocation != null) return freshLocation

        val cachedLocation = runCatching { requestLastLocation() }.getOrNull()
        return cachedLocation?.takeIf { isUsableFallback(it) }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestCurrentLocation(): Location? = suspendCancellableCoroutine { continuation ->
        val cancellation = CancellationTokenSource()
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setDurationMillis(12_000)
            .setMaxUpdateAgeMillis(15_000)
            .build()

        fusedLocationProviderClient
            .getCurrentLocation(request, cancellation.token)
            .addOnSuccessListener { continuation.resume(it) }
            .addOnFailureListener { continuation.resumeWithException(it) }

        continuation.invokeOnCancellation { cancellation.cancel() }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestLastLocation(): Location? = suspendCancellableCoroutine { continuation ->
        fusedLocationProviderClient.lastLocation
            .addOnSuccessListener { continuation.resume(it) }
            .addOnFailureListener { continuation.resumeWithException(it) }
    }

    private fun isUsableFallback(location: Location): Boolean {
        val ageMillis = (System.currentTimeMillis() - location.time).coerceAtLeast(0L)
        return ageMillis <= MAX_FALLBACK_AGE_MILLIS || location.isMock
    }

    private companion object {
        const val MAX_FALLBACK_AGE_MILLIS = 5 * 60 * 1000L
    }
}
