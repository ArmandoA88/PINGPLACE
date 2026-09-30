package com.pingplace.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val event = intent?.let { GeofencingEvent.fromIntent(it) } ?: return
        if (event.hasError()) {
            Log.w("GeofenceReceiver", "Geofence error: ${event.errorCode}")
            return
        }
        MonitorScheduler(context).apply {
            startLiveMonitoring()
            triggerImmediateRefresh()
        }
    }
}
