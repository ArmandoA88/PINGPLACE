package com.pingplace

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.location.LocationManager
import android.os.PowerManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.lifecycleScope
import com.pingplace.data.repository.PingPlaceRepository
import com.pingplace.ui.common.ReliabilityStatus
import com.pingplace.ui.PingPlaceApp
import android.content.pm.PackageManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var pendingBrandQuery by mutableStateOf<String?>(null)
    private var reliabilityStatus by mutableStateOf(
        ReliabilityStatus(
            locationServicesEnabled = false,
            fineLocationGranted = false,
            backgroundLocationGranted = false,
            notificationsGranted = false,
            batteryOptimizationDisabled = false
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingBrandQuery = intent.getStringExtra(EXTRA_BRAND_QUERY)
        val repository = (application as PingPlaceApplication).container.repository
        refreshReliabilityStatus()

        val notificationPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                updateSettings(repository) { it.copy(notificationsEnabled = granted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) }
                refreshReliabilityStatus()
            }
        val fineLocationPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) {
                    (application as PingPlaceApplication).container.monitorScheduler.triggerImmediateRefresh()
                }
                refreshReliabilityStatus()
            }
        val backgroundLocationPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                updateSettings(repository) { it.copy(backgroundLocationEnabled = granted) }
                if (granted) {
                    (application as PingPlaceApplication).container.monitorScheduler.scheduleMonitoring()
                    (application as PingPlaceApplication).container.monitorScheduler.triggerImmediateRefresh()
                }
                refreshReliabilityStatus()
            }

        setContent {
            PingPlaceApp(
                container = (application as PingPlaceApplication).container,
                initialBrandQuery = pendingBrandQuery,
                reliabilityStatus = reliabilityStatus,
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
                },
                openLocationSettings = {
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                },
                openAppSettings = {
                    startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", packageName, null)
                        }
                    )
                },
                openBatterySettings = {
                    val powerManager = getSystemService(PowerManager::class.java)
                    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                        !powerManager.isIgnoringBatteryOptimizations(packageName)
                    ) {
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:$packageName")
                        }
                    } else {
                        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    }
                    startActivity(intent)
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        refreshReliabilityStatus()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingBrandQuery = intent.getStringExtra(EXTRA_BRAND_QUERY)
    }

    private fun isLocationServicesEnabled(): Boolean {
        val manager = getSystemService(LocationManager::class.java)
        return LocationManagerCompat.isLocationEnabled(manager)
    }

    private fun refreshReliabilityStatus() {
        val powerManager = getSystemService(PowerManager::class.java)
        reliabilityStatus = ReliabilityStatus(
            locationServicesEnabled = isLocationServicesEnabled(),
            fineLocationGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED,
            backgroundLocationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            },
            notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            },
            batteryOptimizationDisabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                powerManager.isIgnoringBatteryOptimizations(packageName)
            } else {
                true
            }
        )
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
