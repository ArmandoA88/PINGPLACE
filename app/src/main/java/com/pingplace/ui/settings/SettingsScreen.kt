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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pingplace.model.TriggerType
import com.pingplace.model.UnitsSystem
import com.pingplace.ui.common.SelectionChip

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    innerPadding: PaddingValues,
    viewModel: SettingsViewModel
) {
    val settings by viewModel.uiState.collectAsStateWithLifecycle()

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
