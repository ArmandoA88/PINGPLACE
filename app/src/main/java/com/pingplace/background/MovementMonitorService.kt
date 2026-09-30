package com.pingplace.background

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.pingplace.PingPlaceApplication
import com.pingplace.domain.MonitoringPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

data class MonitoringStatus(val running: Boolean = false, val speedMps: Float = 0f, val hasFix: Boolean = false)

class MovementMonitorService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val fixes = Channel<Location>(Channel.CONFLATED)
    private val client by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private val container get() = (application as PingPlaceApplication).container
    private var requestedInterval = 0L
    private var previousFix: Location? = null
    private var lastFastFix = 0L
    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            val age = (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000
            if (age > 30_000 || (location.hasAccuracy() && location.accuracy > 150f)) return
            val fix = Location(location)
            if (!fix.hasSpeed()) {
                previousFix?.let { previous ->
                    val seconds = (fix.elapsedRealtimeNanos - previous.elapsedRealtimeNanos) / 1_000_000_000.0
                    if (seconds in 1.0..60.0 && previous.distanceTo(fix) > maxOf(previous.accuracy, fix.accuracy)) {
                        fix.speed = (previous.distanceTo(fix) / seconds).toFloat().coerceAtMost(55f)
                    }
                }
            }
            previousFix = fix
            val now = SystemClock.elapsedRealtime()
            if (fix.speed >= MonitoringPolicy.FAST_SPEED_MPS) lastFastFix = now
            // Keep quick fixes through traffic lights and short GPS speed dropouts.
            val interval = if (lastFastFix > 0 && now - lastFastFix < 90_000) 5_000L
                else MonitoringPolicy.intervalMillis(fix.speed)
            requestUpdates(interval)
            _status.value = MonitoringStatus(running = true, speedMps = fix.speed, hasFix = true)
            fixes.trySend(fix)
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID,
                container.notificationHelper.monitoringNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } catch (error: SecurityException) {
            Log.w(TAG, "Location permission is required to monitor", error)
            stopSelf()
            return
        }
        _status.value = MonitoringStatus(running = true)
        scope.launch {
            combine(container.repository.observeUserSettings(), container.repository.observeReminders()) { settings, reminders ->
                settings.backgroundLocationEnabled && reminders.any { !it.isCompleted }
            }.distinctUntilChanged().collect { enabled ->
                if (!enabled) stopSelf() else requestUpdates(5_000L)
            }
        }
        scope.launch(Dispatchers.IO) {
            for (fix in fixes) {
                try {
                    container.reminderMonitor.evaluate(fix)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    Log.w(TAG, "Nearby evaluation will retry on the next fix", error)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestUpdates(interval: Long) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            stopSelf()
            return
        }
        if (requestedInterval == interval) return
        requestedInterval = interval
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, interval)
            .setMinUpdateIntervalMillis(interval / 2)
            .setMaxUpdateDelayMillis(0)
            .setMaxUpdateAgeMillis(0)
            .setWaitForAccurateLocation(false)
            .build()
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
            .addOnFailureListener {
                Log.w(TAG, "Unable to request location updates", it)
                stopSelf()
            }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        client.removeLocationUpdates(callback)
        scope.cancel()
        fixes.close()
        _status.value = MonitoringStatus()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "MovementMonitor"
        const val NOTIFICATION_ID = 7401
        private val _status = MutableStateFlow(MonitoringStatus())
        val status = _status.asStateFlow()
    }
}
