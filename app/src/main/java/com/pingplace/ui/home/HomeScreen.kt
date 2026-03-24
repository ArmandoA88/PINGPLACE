package com.pingplace.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.pingplace.model.ReminderPriority
import com.pingplace.model.TriggerType
import com.pingplace.model.UnitsSystem
import com.pingplace.ui.common.SelectionChip
import com.pingplace.ui.common.formatDistance
import com.pingplace.ui.common.formatDueDate
import com.pingplace.ui.common.formatTrigger
import com.pingplace.ui.theme.PingPlaceTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    innerPadding: PaddingValues,
    viewModel: HomeViewModel,
    isLocationServicesEnabled: Boolean,
    onAddReminder: () -> Unit,
    onBrandClick: (String) -> Unit,
    onEditReminder: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 100.dp
            )
        ) {
            item {
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
                NearbyDashboardSection(
                    uiState = uiState,
                    onRefreshNearby = viewModel::refreshNearbyDashboard,
                    onQuickAddTitleChange = viewModel::updateQuickAddTitle,
                    onQuickAddBrandChange = viewModel::updateQuickAddBrand,
                    onQuickAddBrandSelected = viewModel::selectQuickAddBrand,
                    onSaveQuickReminder = viewModel::saveQuickReminder,
                    onOpenFullEditor = onAddReminder
                )
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
                        onSnooze = { viewModel.snoozeBrand(group) },
                        onEditReminder = onEditReminder,
                        onCompleteReminder = viewModel::completeReminder,
                        onSnoozeReminder = viewModel::snoozeReminder,
                        onDeleteReminder = viewModel::deleteReminder
                    )
                }
            }
        }
    }
}

@Composable
private fun NearbyDashboardSection(
    uiState: HomeUiState,
    onRefreshNearby: () -> Unit,
    onQuickAddTitleChange: (String) -> Unit,
    onQuickAddBrandChange: (String) -> Unit,
    onQuickAddBrandSelected: (BrandSuggestionUiModel) -> Unit,
    onSaveQuickReminder: () -> Unit,
    onOpenFullEditor: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val showSideBySide = maxWidth >= 760.dp
        if (showSideBySide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                NearbyMapCard(
                    modifier = Modifier.weight(1f),
                    mapState = uiState.dashboardMap,
                    units = uiState.units,
                    onRefreshNearby = onRefreshNearby
                )
                QuickAddReminderCard(
                    modifier = Modifier.weight(1f),
                    quickAdd = uiState.quickAdd,
                    onQuickAddTitleChange = onQuickAddTitleChange,
                    onQuickAddBrandChange = onQuickAddBrandChange,
                    onQuickAddBrandSelected = onQuickAddBrandSelected,
                    onSaveQuickReminder = onSaveQuickReminder,
                    onOpenFullEditor = onOpenFullEditor
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NearbyMapCard(
                    modifier = Modifier.fillMaxWidth(),
                    mapState = uiState.dashboardMap,
                    units = uiState.units,
                    onRefreshNearby = onRefreshNearby
                )
                QuickAddReminderCard(
                    modifier = Modifier.fillMaxWidth(),
                    quickAdd = uiState.quickAdd,
                    onQuickAddTitleChange = onQuickAddTitleChange,
                    onQuickAddBrandChange = onQuickAddBrandChange,
                    onQuickAddBrandSelected = onQuickAddBrandSelected,
                    onSaveQuickReminder = onSaveQuickReminder,
                    onOpenFullEditor = onOpenFullEditor
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NearbyMapCard(
    modifier: Modifier,
    mapState: DashboardMapUiState,
    units: UnitsSystem,
    onRefreshNearby: () -> Unit
) {
    Card(modifier = modifier, shape = RoundedCornerShape(28.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Near You", style = MaterialTheme.typography.titleLarge)
                    Text(
                        when {
                            mapState.nearbyStores.isNotEmpty() ->
                                "Showing ${mapState.nearbyStores.size} nearby stores around your current location."

                            mapState.latitude != null ->
                                "Your current location is ready. Refresh to check nearby stores."

                            else -> "We will pin your location here and look for matching stores nearby."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Button(onClick = onRefreshNearby, enabled = !mapState.isLoading) {
                    Text(if (mapState.isLoading) "Refreshing..." else "Refresh")
                }
            }

            if (mapState.searchedBrands.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    mapState.searchedBrands.forEach { brand ->
                        SelectionChip(
                            label = brand.brandName,
                            selected = true,
                            onClick = {}
                        )
                    }
                }
            }

            when {
                mapState.latitude != null && mapState.longitude != null -> {
                    DashboardTileMap(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                        userLatitude = mapState.latitude,
                        userLongitude = mapState.longitude,
                        places = mapState.nearbyStores
                    )
                }

                mapState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                else -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mapState.errorMessage ?: "Nearby map will appear once your location is available.",
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }
                }
            }

            mapState.errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (mapState.nearbyStores.isEmpty()) {
                Text(
                    "No matching stores found nearby yet. Add or refresh reminders to update this snapshot.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                HorizontalDivider()
                mapState.nearbyStores.take(4).forEach { place ->
                    NearbyStoreRow(place = place, units = units)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickAddReminderCard(
    modifier: Modifier,
    quickAdd: QuickAddUiState,
    onQuickAddTitleChange: (String) -> Unit,
    onQuickAddBrandChange: (String) -> Unit,
    onQuickAddBrandSelected: (BrandSuggestionUiModel) -> Unit,
    onSaveQuickReminder: () -> Unit,
    onOpenFullEditor: () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Quick Add Reminder", style = MaterialTheme.typography.titleLarge)
            Text(
                "Create a reminder right from the dashboard, then keep using the full editor for extra details when you need them.",
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedTextField(
                value = quickAdd.title,
                onValueChange = onQuickAddTitleChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Task title") },
                singleLine = true,
                shape = RoundedCornerShape(18.dp)
            )
            if (quickAdd.suggestedBrands.isNotEmpty()) {
                Text("Suggested brands", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    quickAdd.suggestedBrands.forEach { brand ->
                        SelectionChip(
                            label = brand.brandName,
                            selected = quickAdd.brandQuery.equals(brand.brandQuery, ignoreCase = true),
                            onClick = { onQuickAddBrandSelected(brand) }
                        )
                    }
                }
            }
            OutlinedTextField(
                value = quickAdd.brandName,
                onValueChange = onQuickAddBrandChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Brand or place type") },
                singleLine = true,
                shape = RoundedCornerShape(18.dp)
            )
            quickAdd.errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onSaveQuickReminder,
                    enabled = !quickAdd.isSaving
                ) {
                    Text(if (quickAdd.isSaving) "Saving..." else "Save reminder")
                }
                OutlinedButton(onClick = onOpenFullEditor) {
                    Text("Open full editor")
                }
            }
            Text(
                "New reminders use your saved default trigger and blocked-time settings.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun NearbyStoreRow(
    place: NearbyStoreUiModel,
    units: UnitsSystem
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(place.name, style = MaterialTheme.typography.titleMedium)
        Text(
            buildString {
                append(place.brandName)
                append(" | ")
                append(formatDistance(place.distanceMeters.toInt(), units))
                place.estimatedTravelMinutes?.let {
                    append(" | about ")
                    append(it)
                    append(" min")
                }
            },
            style = MaterialTheme.typography.bodyMedium
        )
        if (place.address.isNotBlank()) {
            Text(place.address, style = MaterialTheme.typography.bodySmall)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BrandCard(
    group: BrandGroupUiModel,
    units: UnitsSystem,
    onOpen: () -> Unit,
    onComplete: () -> Unit,
    onSnooze: () -> Unit,
    onEditReminder: (Long) -> Unit,
    onCompleteReminder: (Long) -> Unit,
    onSnoozeReminder: (Long) -> Unit,
    onDeleteReminder: (Long) -> Unit
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
                ReminderSummary(
                    reminder = reminder,
                    units = units,
                    onEdit = { onEditReminder(reminder.id) },
                    onComplete = { onCompleteReminder(reminder.id) },
                    onSnooze = { onSnoozeReminder(reminder.id) },
                    onDelete = { onDeleteReminder(reminder.id) }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectionChip(label = "Complete all", selected = false, onClick = onComplete)
                SelectionChip(label = "Snooze 30 min", selected = false, onClick = onSnooze)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReminderSummary(
    reminder: ReminderEntity,
    units: UnitsSystem,
    onEdit: () -> Unit,
    onComplete: () -> Unit,
    onSnooze: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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
            Text(formatTrigger(reminder.triggerType, reminder.triggerDistanceMeters, reminder.triggerTravelTimeMinutes, units))
            if (reminder.notes.isNotBlank()) {
                Text(reminder.notes, style = MaterialTheme.typography.bodyMedium)
            }
            reminder.checklistItems.forEach {
                Text("- $it", style = MaterialTheme.typography.bodySmall)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectionChip(label = "Edit", selected = false, onClick = onEdit)
                SelectionChip(label = "Done", selected = false, onClick = onComplete)
                SelectionChip(label = "Snooze", selected = false, onClick = onSnooze)
                SelectionChip(label = "Delete", selected = false, onClick = onDelete)
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
                onSnooze = {},
                onEditReminder = {},
                onCompleteReminder = {},
                onSnoozeReminder = {},
                onDeleteReminder = {}
            )
        }
    }
}
