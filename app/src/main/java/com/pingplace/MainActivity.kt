package com.pingplace

import android.Manifest
import android.content.Intent
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.lifecycleScope
import com.pingplace.data.repository.PingPlaceRepository
import com.pingplace.ui.PingPlaceApp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var pendingBrandQuery by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingBrandQuery = intent.getStringExtra(EXTRA_BRAND_QUERY)
        val repository = (application as PingPlaceApplication).container.repository

        val notificationPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                updateSettings(repository) { it.copy(notificationsEnabled = granted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) }
            }
        val fineLocationPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) {
                    (application as PingPlaceApplication).container.monitorScheduler.triggerImmediateRefresh()
                }
            }
        val backgroundLocationPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                updateSettings(repository) { it.copy(backgroundLocationEnabled = granted) }
                if (granted) {
                    (application as PingPlaceApplication).container.monitorScheduler.scheduleMonitoring()
                    (application as PingPlaceApplication).container.monitorScheduler.triggerImmediateRefresh()
                }
            }

        setContent {
            PingPlaceApp(
                container = (application as PingPlaceApplication).container,
                initialBrandQuery = pendingBrandQuery,
                isLocationServicesEnabled = isLocationServicesEnabled(),
                requestNotifications = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                requestFineLocation = {
                    fineLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                },
                requestBackgroundLocation = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        backgroundLocationPermissionLauncher.launch(
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
                    }
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingBrandQuery = intent.getStringExtra(EXTRA_BRAND_QUERY)
    }

    private fun isLocationServicesEnabled(): Boolean {
        val manager = getSystemService(LocationManager::class.java)
        return LocationManagerCompat.isLocationEnabled(manager)
    }

    private fun updateSettings(
        repository: PingPlaceRepository,
        transform: (com.pingplace.data.local.entity.UserSettingsEntity) -> com.pingplace.data.local.entity.UserSettingsEntity
    ) {
        lifecycleScope.launch {
            repository.updateSettings(transform)
        }
    }

    companion object {
        const val EXTRA_BRAND_QUERY = "brand_query"
    }
}
