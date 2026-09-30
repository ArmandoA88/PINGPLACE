package com.pingplace.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pingplace.PingPlaceApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED && intent?.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val container = (appContext as PingPlaceApplication).container
                if (container.repository.getUserSettings().backgroundLocationEnabled) {
                    container.monitorScheduler.scheduleMonitoring()
                    container.monitorScheduler.triggerImmediateRefresh()
                } else {
                    container.monitorScheduler.cancelMonitoring()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
