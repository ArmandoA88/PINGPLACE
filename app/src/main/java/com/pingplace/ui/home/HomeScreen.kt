package com.pingplace.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pingplace.data.local.entity.ReminderEntity
import com.pingplace.model.ReminderFilter
import com.pingplace.model.TriggerType
import com.pingplace.model.UnitsSystem
import com.pingplace.ui.common.SelectionChip
import com.pingplace.ui.common.formatTrigger
import com.pingplace.ui.theme.PingPlaceTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    innerPadding: PaddingValues,
    viewModel: HomeViewModel,
    isLocationServicesEnabled: Boolean,
    onAddReminder: () -> Unit,
    onBrandClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.padding(innerPadding),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("PingPlace")
                        Text("Brand-based reminders", style = MaterialTheme.typography.labelLarge)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddReminder) {
                Icon(Icons.Outlined.Add, contentDescription = "Add reminder")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                if (!isLocationServicesEnabled) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.LocationOff, contentDescription = null)
                            Text("Location services are off. Nearby alerts are limited until they are back on.")
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchChanged,
                    label = { Text("Search reminders or brands") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReminderFilter.entries.forEach { filter ->
                        SelectionChip(
                            selected = uiState.filter == filter,
                            onClick = { viewModel.onFilterSelected(filter) },
                            label = filter.name.lowercase().replaceFirstChar { it.titlecase() }
                        )
                    }
                }
            }
            if (uiState.groups.isEmpty()) {
                item {
                    EmptyStateCard(onAddReminder = onAddReminder)
                }
            } else {
                items(uiState.groups) { group ->
                    BrandCard(
                        group = group,
                        units = uiState.units,
                        onOpen = { onBrandClick(group.brandQuery) },
                        onComplete = { viewModel.completeBrand(group) },
                        onSnooze = { viewModel.snoozeBrand(group) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStateCard(onAddReminder: () -> Unit) {
    Card(shape = RoundedCornerShape(28.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Nothing active yet", style = MaterialTheme.typography.titleLarge)
            Text("Start with one errand like returning a package at Whole Foods or buying batteries at Costco.")
            Button(onClick = onAddReminder) {
                Text("Create first reminder")
            }
        }
    }
}

@Composable
private fun BrandCard(
    group: BrandGroupUiModel,
    units: UnitsSystem,
    onOpen: () -> Unit,
    onComplete: () -> Unit,
    onSnooze: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        onClick = onOpen
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(group.brandName, style = MaterialTheme.typography.titleLarge)
                    Text(group.summary)
                }
                if (group.suppressedCount > 0) {
                    SelectionChip(label = "Suppressed now", selected = true, onClick = onOpen)
                }
            }

            group.reminders.take(3).forEach { reminder ->
                Text(
                    "- ${reminder.title} - ${formatTrigger(reminder.triggerType, reminder.triggerDistanceMeters, reminder.triggerTravelTimeMinutes, units)}"
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectionChip(label = "Complete all", selected = false, onClick = onComplete)
                SelectionChip(label = "Snooze 30 min", selected = false, onClick = onSnooze)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomePreview() {
    PingPlaceTheme {
        Box(Modifier.fillMaxSize()) {
            BrandCard(
                group = BrandGroupUiModel(
                    brandName = "Whole Foods",
                    brandQuery = "Whole Foods",
                    reminders = listOf(
                        ReminderEntity(
                            id = 1,
                            title = "Return Amazon package",
                            brandName = "Whole Foods",
                            brandQuery = "Whole Foods",
                            triggerType = TriggerType.DISTANCE,
                            triggerDistanceMeters = 1609,
                            createdAtEpochMillis = 0,
                            updatedAtEpochMillis = 0
                        ),
                        ReminderEntity(
                            id = 2,
                            title = "Buy salad",
                            brandName = "Whole Foods",
                            brandQuery = "Whole Foods",
                            triggerType = TriggerType.TRAVEL_TIME,
                            triggerTravelTimeMinutes = 10,
                            createdAtEpochMillis = 0,
                            updatedAtEpochMillis = 0
                        )
                    ),
                    activeCount = 2,
                    snoozedCount = 0,
                    completedCount = 0,
                    suppressedCount = 1,
                    summary = "Whole Foods, 2 reminders"
                ),
                units = UnitsSystem.IMPERIAL,
                onOpen = {},
                onComplete = {},
                onSnooze = {}
            )
        }
    }
}
