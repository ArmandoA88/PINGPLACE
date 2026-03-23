package com.pingplace.ui.completed

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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.pingplace.ui.common.formatDueDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CompletedScreen(
    innerPadding: PaddingValues,
    viewModel: CompletedViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { TopAppBar(title = { Text("Completed") }) }
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
                if (uiState.reminders.isEmpty()) {
                    Text("No completed reminders yet.", modifier = Modifier.padding(vertical = 16.dp))
                }
            }
            items(uiState.reminders) { reminder ->
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
                        Text(reminder.title, style = MaterialTheme.typography.titleLarge)
                        Text(reminder.brandName)
                        reminder.notes.takeIf { it.isNotBlank() }?.let { Text(it) }
                        reminder.checklistItems.forEach { Text("- $it", style = MaterialTheme.typography.bodySmall) }
                        formatDueDate(reminder.dueDateEpochMillis)?.let { Text("Due $it") }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(
                                onClick = { viewModel.restoreReminder(reminder.id) },
                                label = { Text("Restore") }
                            )
                            AssistChip(
                                onClick = { viewModel.deleteReminder(reminder.id) },
                                label = { Text("Delete") }
                            )
                        }
                    }
                }
            }
        }
    }
}
