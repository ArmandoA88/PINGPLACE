package com.pingplace.ui.brand

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pingplace.data.local.entity.ReminderEntity
import com.pingplace.model.ReminderPriority
import com.pingplace.ui.common.SelectionChip
import com.pingplace.ui.common.formatDistance
import com.pingplace.ui.common.formatDueDate
import com.pingplace.ui.common.formatTrigger

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BrandDetailScreen(
    innerPadding: PaddingValues,
    viewModel: BrandDetailViewModel,
    onBack: () -> Unit,
    onEditReminder: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.padding(innerPadding),
        topBar = { TopAppBar(title = { Text(uiState.brandName) }) }
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
                Card(shape = RoundedCornerShape(24.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("All reminders", style = MaterialTheme.typography.titleLarge)
                        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SelectionChip(label = "Complete all", selected = false, onClick = viewModel::completeAll)
                            SelectionChip(label = "Snooze 30 min", selected = false, onClick = viewModel::snoozeAll)
                            SelectionChip(label = "Back", selected = false, onClick = onBack)
                        }
                    }
                }
            }
            items(uiState.reminders, key = { it.id }) { reminder ->
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(reminder.title, style = MaterialTheme.typography.titleMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SelectionChip(
                                label = reminder.priority?.name ?: ReminderPriority.NORMAL.name,
                                selected = reminder.priority == ReminderPriority.HIGH,
                                onClick = {}
                            )
                            formatDueDate(reminder.dueDateEpochMillis)?.let {
                                SelectionChip(label = "Due $it", selected = false, onClick = {})
                            }
                            if (reminder.isSnoozed) {
                                SelectionChip(label = "Snoozed", selected = true, onClick = {})
                            }
                        }
                        Text(formatTrigger(reminder.triggerType, reminder.triggerDistanceMeters, reminder.triggerTravelTimeMinutes, uiState.units))
                        if (reminder.notes.isNotBlank()) {
                            Text(reminder.notes)
                        }
                        reminder.checklistItems.forEach { item ->
                            Text("- $item", style = MaterialTheme.typography.bodySmall)
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SelectionChip(label = "Edit", selected = false, onClick = { onEditReminder(reminder.id) })
                            SelectionChip(label = "Done", selected = false, onClick = { viewModel.completeReminder(reminder.id) })
                            SelectionChip(label = "Snooze", selected = false, onClick = { viewModel.snoozeReminder(reminder.id) })
                            SelectionChip(label = "Delete", selected = false, onClick = { viewModel.deleteReminder(reminder.id) })
                        }
                    }
                }
            }
            item {
                Card(shape = RoundedCornerShape(24.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Nearby matches", style = MaterialTheme.typography.titleLarge)
                        androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SelectionChip(
                                label = "Refresh nearby",
                                selected = false,
                                onClick = viewModel::refreshNearbyPlaces
                            )
                        }
                        when {
                            uiState.isLoadingPlaces -> Text("Checking nearby places...")
                            uiState.placeError != null -> Text(uiState.placeError!!)
                            uiState.nearbyPlaces.isEmpty() -> Text("No nearby matches found right now.")
                            else -> uiState.nearbyPlaces.take(5).forEach { place ->
                                Text(
                                    "- ${place.name} - ${formatDistance(place.distanceMeters.toInt(), uiState.units)} - ${place.address}"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
