package com.pingplace.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pingplace.model.PlaceSearchMode
import com.pingplace.model.TriggerType
import com.pingplace.model.UnitsSystem
import com.pingplace.offline.OfflinePackDescriptor
import com.pingplace.offline.OfflinePackKind
import com.pingplace.ui.common.ReliabilityStatus
import com.pingplace.ui.common.SelectionChip
import com.pingplace.ui.common.formatDateTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    innerPadding: PaddingValues,
    viewModel: SettingsViewModel,
    reliabilityStatus: ReliabilityStatus,
    onRequestNotifications: () -> Unit,
    onRequestFineLocation: () -> Unit,
    onRequestBackgroundLocation: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit
) {
    val settings by viewModel.uiState.collectAsStateWithLifecycle()
    val installedRegions by viewModel.installedRegions.collectAsStateWithLifecycle()
    val offlineState by viewModel.offlineUiState.collectAsStateWithLifecycle()
    val installedIds = installedRegions.map { it.id }.toSet()
    val catalogById = offlineState.catalog.associateBy { it.id }
    val filteredPacks = offlineState.catalog
        .filter { offlineState.selectedKind == null || it.kind == offlineState.selectedKind }
        .filter {
            offlineState.searchQuery.isBlank() ||
                it.displayName.contains(offlineState.searchQuery, ignoreCase = true) ||
                it.subtitle.contains(offlineState.searchQuery, ignoreCase = true) ||
                it.region.contains(offlineState.searchQuery, ignoreCase = true)
        }

    Scaffold(
        modifier = Modifier.padding(innerPadding),
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                SettingsCard("Default trigger mode") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TriggerType.entries.forEach { trigger ->
                            SelectionChip(
                                label = trigger.name.lowercase().replace('_', ' '),
                                selected = settings.defaultTriggerType == trigger,
                                onClick = { viewModel.updateDefaultTrigger(trigger) },
                            )
                        }
                    }
                }
            }
            item {
                SettingsCard("Defaults") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(804, 1609, 3218, 8046).forEach {
                            SelectionChip(
                                label = "${it / 1609.0} mi",
                                selected = settings.defaultDistanceMeters == it,
                                onClick = { viewModel.updateDistance(it) },
                            )
                        }
                    }
                    FlowRow(
                        modifier = Modifier.padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 10, 15, 20).forEach {
                            SelectionChip(
                                label = "$it min",
                                selected = settings.defaultTravelTimeMinutes == it,
                                onClick = { viewModel.updateTravelTime(it) },
                            )
                        }
                    }
                }
            }
            item {
                SettingsCard("Units and appearance") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        UnitsSystem.entries.forEach { units ->
                            SelectionChip(
                                label = units.name,
                                selected = settings.units == units,
                                onClick = { viewModel.updateUnits(units) }
                            )
                        }
                    }
                    SwitchRow(
                        title = "Notifications enabled",
                        checked = settings.notificationsEnabled,
                        onCheckedChange = viewModel::updateNotificationsEnabled
                    )
                    SwitchRow(
                        title = "Notification sound",
                        checked = settings.soundEnabled,
                        onCheckedChange = viewModel::updateSoundEnabled
                    )
                    SwitchRow(
                        title = "Dark mode",
                        checked = settings.darkModeEnabled,
                        onCheckedChange = viewModel::updateDarkMode
                    )
                }
            }
            item {
                SettingsCard("Store lookup source") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PlaceSearchMode.entries.forEach { mode ->
                            SelectionChip(
                                label = mode.name.lowercase().replace('_', ' '),
                                selected = settings.placeSearchMode == mode,
                                onClick = { viewModel.updatePlaceSearchMode(mode) }
                            )
                        }
                    }
                    Text(
                        "Hybrid uses offline packs first, then live lookup. Offline only never calls the Places API.",
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
            item {
                SettingsCard("Reminder behavior") {
                    SwitchRow(
                        title = "Background monitoring",
                        checked = settings.backgroundLocationEnabled,
                        onCheckedChange = viewModel::updateBackgroundLocationEnabled
                    )
                    SwitchRow(
                        title = "Respect blocked times by default",
                        checked = settings.respectBlockedTimesByDefault,
                        onCheckedChange = viewModel::updateRespectBlockedTimesByDefault
                    )
                }
            }
            item {
                SettingsCard("Reliability setup") {
                    Text("Nearby reminders work best when all of these are turned on.")
                    FlowRow(
                        modifier = Modifier.padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReliabilityChip("Location services", reliabilityStatus.locationServicesEnabled)
                        ReliabilityChip("Precise location", reliabilityStatus.fineLocationGranted)
                        ReliabilityChip("Background location", reliabilityStatus.backgroundLocationGranted)
                        ReliabilityChip("Notifications", reliabilityStatus.notificationsGranted)
                        ReliabilityChip("Battery unrestricted", reliabilityStatus.batteryOptimizationDisabled)
                    }
                    if (reliabilityStatus.needsAttention) {
                        Text(
                            "If any of these are off, reminders may not fire while you are near a store.",
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                    FlowRow(
                        modifier = Modifier.padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(onClick = onRequestNotifications) { Text("Allow alerts") }
                        Button(onClick = onRequestFineLocation) { Text("Allow location") }
                        Button(onClick = onRequestBackgroundLocation) { Text("Allow background") }
                    }
                    FlowRow(
                        modifier = Modifier.padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(onClick = onOpenLocationSettings) { Text("Location settings") }
                        Button(onClick = onOpenAppSettings) { Text("App settings") }
                        Button(onClick = onOpenBatterySettings) { Text("Battery settings") }
                    }
                }
            }
            item {
                SettingsCard("Offline region packs") {
                    Text("Browse the map catalog, search by city or region, and install what you need.")
                    offlineState.statusMessage?.let {
                        Text(
                            text = it,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                    OutlinedTextField(
                        value = offlineState.searchQuery,
                        onValueChange = viewModel::updateOfflineSearchQuery,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        label = { Text("Search city or area") }
                    )
                    FlowRow(
                        modifier = Modifier.padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SelectionChip(
                            label = "All",
                            selected = offlineState.selectedKind == null,
                            onClick = { viewModel.updateOfflineKindFilter(null) }
                        )
                        OfflinePackKind.entries.forEach { kind ->
                            SelectionChip(
                                label = kind.name.lowercase().replaceFirstChar { it.titlecase() },
                                selected = offlineState.selectedKind == kind,
                                onClick = { viewModel.updateOfflineKindFilter(kind) }
                            )
                        }
                    }
                    Button(
                        onClick = viewModel::refreshOfflineCatalog,
                        enabled = !offlineState.isLoadingCatalog,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Text(if (offlineState.isLoadingCatalog) "Refreshing..." else "Refresh catalog")
                    }
                    if (offlineState.isLoadingCatalog) {
                        CircularProgressIndicator(modifier = Modifier.padding(top = 12.dp))
                    }
                    PackList(
                        packs = filteredPacks,
                        installedIds = installedIds,
                        isImporting = offlineState.isImporting,
                        activePackId = offlineState.activePackId,
                        onInstall = viewModel::installBundledPack
                    )
                    if (filteredPacks.isEmpty()) {
                        Text("No map packs match the current filter.", modifier = Modifier.padding(top = 12.dp))
                    }
                    Text(
                        "Downloaded maps",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    if (installedRegions.isEmpty()) {
                        Text("No maps downloaded yet.", modifier = Modifier.padding(top = 12.dp))
                    } else {
                        installedRegions.forEach { region ->
                            val descriptor = catalogById[region.id]
                            val isRefreshing = offlineState.isImporting && offlineState.activePackId == region.id
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(region.displayName, style = MaterialTheme.typography.titleMedium)
                                    Text("${region.placeCount} places")
                                    formatDateTime(region.updatedAtEpochMillis ?: region.downloadedAtEpochMillis)?.let {
                                        Text("Last synced: $it")
                                    }
                                    descriptor?.region?.let { Text(it) }
                                    Button(
                                        onClick = { viewModel.refreshInstalledPack(region.id) },
                                        enabled = descriptor != null && !offlineState.isImporting,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(if (isRefreshing) "Updating stores..." else "Update stores")
                                    }
                                    Button(
                                        onClick = { viewModel.removeOfflinePack(region.id) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Remove map")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                SettingsCard("Background help") {
                    Text("PingPlace works best with fine location, background location, and battery optimization disabled for the app.")
                    Button(
                        onClick = viewModel::rerunMonitoring,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Text("Run monitoring now")
                    }
                    Button(
                        onClick = viewModel::triggerTestNotification,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Text("Test reminder")
                    }
                }
            }
        }
    }
}

@Composable
private fun PackList(
    packs: List<OfflinePackDescriptor>,
    installedIds: Set<String>,
    isImporting: Boolean,
    activePackId: String?,
    onInstall: (OfflinePackDescriptor) -> Unit
) {
    packs.forEach { pack ->
        val isDownloading = isImporting && activePackId == pack.id
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(pack.displayName, style = MaterialTheme.typography.titleMedium)
                Text(pack.region)
                Text(pack.subtitle)
                Button(
                    onClick = { onInstall(pack) },
                    enabled = !isImporting && pack.id !in installedIds,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        when {
                            pack.id in installedIds -> "Installed"
                            isDownloading -> "Downloading..."
                            else -> "Download map"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(shape = RoundedCornerShape(24.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Column(modifier = Modifier.padding(top = 12.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ReliabilityChip(label: String, enabled: Boolean) {
    SelectionChip(
        selected = enabled,
        onClick = {},
        label = "$label: ${if (enabled) "On" else "Off"}"
    )
}
