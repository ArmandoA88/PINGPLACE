package com.pingplace.ui.add

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pingplace.model.BlockedTimeBehavior
import com.pingplace.model.BrandCatalog
import com.pingplace.model.ReminderPriority
import com.pingplace.model.ReminderRepeatType
import com.pingplace.model.TriggerType
import com.pingplace.ui.common.SelectionChip
import com.pingplace.ui.theme.PingPlaceTheme
import com.pingplace.ui.common.formatDueDate

private val distanceOptions = listOf(150, 500, 804, 1609, 3218, 8046)
private val timeOptions = listOf(5, 10, 15, 20)
private val hourOptions = listOf(360, 420, 480, 540, 720, 900, 1020, 1140, 1260)
private val weekdayLabels = listOf("M", "T", "W", "T", "F", "S", "S")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddReminderScreen(
    innerPadding: PaddingValues,
    viewModel: AddReminderViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.saveCompleted) {
        if (uiState.saveCompleted) {
            viewModel.consumeSaved()
            onBack()
        }
    }

    Scaffold(
        modifier = Modifier.padding(innerPadding),
        topBar = { TopAppBar(title = { Text(if (uiState.isEditing) "Edit reminder" else "Add reminder") }) }
    ) { padding ->
        if (uiState.isLoading) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = viewModel::updateTitle,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Task title") },
                    shape = RoundedCornerShape(18.dp)
                )
            }
            item {
                BrandSection(
                    selectedBrand = uiState.brandName,
                    onBrandSelected = viewModel::updateBrand
                )
            }
            item {
                SectionCard(title = "Trigger") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TriggerType.entries.forEach { type ->
                            SelectionChip(
                                selected = uiState.triggerType == type,
                                onClick = { viewModel.updateTriggerType(type) },
                                label = if (type == TriggerType.DISTANCE) "Distance" else "Travel time"
                            )
                        }
                    }
                    Text(
                        if (uiState.triggerType == TriggerType.DISTANCE) "Notify when within" else "Notify when about",
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (uiState.triggerType == TriggerType.DISTANCE) {
                            distanceOptions.forEach { meters ->
                                SelectionChip(
                                    selected = uiState.triggerDistanceMeters == meters,
                                    onClick = { viewModel.updateDistance(meters) },
                                    label = if (meters < 305) {
                                        "${(meters * 3.28084).toInt()} ft"
                                    } else {
                                        "%.1f mi".format(meters / 1609.0)
                                    }
                                )
                            }
                        } else {
                            timeOptions.forEach { minutes ->
                                SelectionChip(
                                    selected = uiState.triggerTravelTimeMinutes == minutes,
                                    onClick = { viewModel.updateTravelMinutes(minutes) },
                                    label = "$minutes min"
                                )
                            }
                        }
                    }
                    FlowRow(
                        modifier = Modifier.padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SelectionChip(
                            label = "Any movement",
                            selected = !uiState.requiresDrivingFast,
                            onClick = { viewModel.updateRequiresDrivingFast(false) }
                        )
                        SelectionChip(
                            label = "Driving only",
                            selected = uiState.requiresDrivingFast,
                            onClick = { viewModel.updateRequiresDrivingFast(true) }
                        )
                    }
                }
            }
            item {
                SectionCard(title = "Notes and checklist") {
                    OutlinedTextField(
                        value = uiState.notes,
                        onValueChange = viewModel::updateNotes,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Notes") }
                    )
                    OutlinedTextField(
                        value = uiState.checklistText,
                        onValueChange = viewModel::updateChecklist,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        label = { Text("Checklist items, one per line") }
                    )
                }
            }
            item {
                SectionCard(title = "Priority and repeat") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReminderPriority.entries.forEach { priority ->
                            SelectionChip(
                                label = priority.name,
                                selected = uiState.priority == priority,
                                onClick = { viewModel.updatePriority(priority) }
                            )
                        }
                    }
                    FlowRow(
                        modifier = Modifier.padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReminderRepeatType.entries.forEach { repeat ->
                            SelectionChip(
                                label = repeat.name,
                                selected = uiState.repeatType == repeat,
                                onClick = { viewModel.updateRepeatType(repeat) }
                            )
                        }
                    }
                    if (uiState.repeatType == ReminderRepeatType.WEEKLY) {
                        FlowRow(
                            modifier = Modifier.padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            weekdayLabels.forEachIndexed { index, label ->
                                SelectionChip(
                                    selected = index + 1 in uiState.repeatDays,
                                    onClick = { viewModel.toggleRepeatDay(index + 1) },
                                    label = label
                                )
                            }
                        }
                    }
                }
            }
            item {
                SectionCard(title = "Due date") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val dueDate = uiState.dueDateEpochMillis
                        SelectionChip(
                            label = "No due date",
                            selected = dueDate == null,
                            onClick = { viewModel.updateDueDate(null) }
                        )
                        SelectionChip(
                            label = "Today",
                            selected = dueDate != null &&
                                dueDate <= System.currentTimeMillis() + 1_000,
                            onClick = { viewModel.updateDueDate(System.currentTimeMillis()) },
                        )
                        SelectionChip(
                            label = "Tomorrow",
                            selected = dueDate != null &&
                                dueDate > System.currentTimeMillis() + 1_000,
                            onClick = { viewModel.updateDueDate(System.currentTimeMillis() + 86_400_000) },
                        )
                    }
                    Text(
                        text = "Current: ${formatDueDate(uiState.dueDateEpochMillis) ?: "None"}",
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
            item {
                SectionCard(title = "Blocked time behavior") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BlockedTimeBehavior.entries.forEach { behavior ->
                            SelectionChip(
                                selected = uiState.blockedTimeBehavior == behavior,
                                onClick = { viewModel.updateBlockedBehavior(behavior) },
                                label = behavior.name.lowercase().replace('_', ' ')
                            )
                        }
                    }
                    if (uiState.blockedTimeBehavior == BlockedTimeBehavior.CUSTOM_ALLOWED_HOURS) {
                        Text("Allowed days", modifier = Modifier.padding(top = 12.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            weekdayLabels.forEachIndexed { index, label ->
                                SelectionChip(
                                    selected = index + 1 in uiState.allowedDays,
                                    onClick = { viewModel.toggleAllowedDay(index + 1) },
                                    label = label
                                )
                            }
                        }
                        Text("Allowed start", modifier = Modifier.padding(top = 12.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            hourOptions.forEach { option ->
                                SelectionChip(
                                    selected = uiState.allowedStartMinutes == option,
                                    onClick = { viewModel.updateAllowedStart(option) },
                                    label = minutesLabel(option)
                                )
                            }
                        }
                        Text("Allowed end", modifier = Modifier.padding(top = 12.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            hourOptions.forEach { option ->
                                SelectionChip(
                                    selected = uiState.allowedEndMinutes == option,
                                    onClick = { viewModel.updateAllowedEnd(option) },
                                    label = minutesLabel(option)
                                )
                            }
                        }
                    }
                }
            }
            item {
                uiState.errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                Button(
                    onClick = viewModel::saveReminder,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSaving
                ) {
                    Text(
                        when {
                            uiState.isSaving -> "Saving..."
                            uiState.isEditing -> "Update reminder"
                            else -> "Save reminder"
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BrandSection(
    selectedBrand: String,
    onBrandSelected: (String, String) -> Unit
) {
    SectionCard(title = "Brand or place type") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BrandCatalog.defaults.forEach { brand ->
                SelectionChip(
                    selected = selectedBrand == brand.name,
                    onClick = { onBrandSelected(brand.name, brand.query) },
                    label = brand.name
                )
            }
        }
        OutlinedTextField(
            value = selectedBrand,
            onValueChange = { onBrandSelected(it, it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            label = { Text("Custom brand or chain") }
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
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

fun minutesLabel(totalMinutes: Int): String {
    val hour = totalMinutes / 60
    val minute = totalMinutes % 60
    val suffix = if (hour < 12) "AM" else "PM"
    val displayHour = when (val value = hour % 12) {
        0 -> 12
        else -> value
    }
    return "%d:%02d %s".format(displayHour, minute, suffix)
}

@Preview(showBackground = true)
@Composable
private fun AddReminderPreview() {
    PingPlaceTheme {
        SectionCard(title = "Trigger") {
            Text("Preview")
        }
    }
}
