package com.pingplace.ui.blocked

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pingplace.ui.add.minutesLabel
import com.pingplace.ui.common.BlockedSelectionChip
import com.pingplace.ui.theme.PingPlaceTheme

private val blockedHourOptions = listOf(0, 360, 450, 510, 720, 990, 1260)
private val blockedDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BlockedTimesScreen(
    innerPadding: PaddingValues,
    viewModel: BlockedTimesViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var label by remember { mutableStateOf("Work") }
    var selectedDay by remember { mutableIntStateOf(1) }
    var selectedStart by remember { mutableIntStateOf(450) }
    var selectedEnd by remember { mutableIntStateOf(990) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { TopAppBar(title = { Text("Blocked times") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 32.dp
            )
        ) {
            item {
                Card(shape = RoundedCornerShape(24.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Add recurring blocked window", style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(
                            value = label,
                            onValueChange = { label = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Label") }
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            blockedDays.forEachIndexed { index, day ->
                                BlockedSelectionChip(
                                    selected = selectedDay == index + 1,
                                    onClick = { selectedDay = index + 1 },
                                    label = day
                                )
                            }
                        }
                        Text("Start")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            blockedHourOptions.forEach { option ->
                                BlockedSelectionChip(
                                    selected = selectedStart == option,
                                    onClick = { selectedStart = option },
                                    label = minutesLabel(option)
                                )
                            }
                        }
                        Text("End")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            blockedHourOptions.forEach { option ->
                                BlockedSelectionChip(
                                    selected = selectedEnd == option,
                                    onClick = { selectedEnd = option },
                                    label = minutesLabel(option)
                                )
                            }
                        }
                        Button(
                            onClick = {
                                viewModel.addWindow(selectedDay, selectedStart, selectedEnd, label)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save blocked window")
                        }
                    }
                }
            }
            items(uiState.windows) { window ->
                Card(shape = RoundedCornerShape(22.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(window.label.ifBlank { "Blocked time" }, style = MaterialTheme.typography.titleLarge)
                        Text("${blockedDays[window.dayOfWeek - 1]} - ${minutesLabel(window.startMinutes)} to ${minutesLabel(window.endMinutes)}")
                        BlockedSelectionChip(
                            onClick = { viewModel.deleteWindow(window.id) },
                            selected = false,
                            label = "Delete"
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BlockedPreview() {
    PingPlaceTheme {
        Card {
            Text("Weekdays 7:30 AM to 4:30 PM", modifier = Modifier.padding(16.dp))
        }
    }
}
