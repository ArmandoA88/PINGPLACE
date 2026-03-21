package com.pingplace.ui.completed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingplace.background.MonitorScheduler
import com.pingplace.data.local.entity.ReminderEntity
import com.pingplace.data.repository.PingPlaceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CompletedUiState(
    val reminders: List<ReminderEntity> = emptyList()
)

class CompletedViewModel(
    private val repository: PingPlaceRepository,
    private val scheduler: MonitorScheduler
) : ViewModel() {

    val uiState: StateFlow<CompletedUiState> = repository.observeCompletedReminders()
        .map { CompletedUiState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CompletedUiState())

    fun restoreReminder(id: Long) {
        viewModelScope.launch {
            repository.setReminderCompleted(id, false)
            scheduler.triggerImmediateRefresh()
        }
    }
}
