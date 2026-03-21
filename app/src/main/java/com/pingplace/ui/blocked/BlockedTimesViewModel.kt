package com.pingplace.ui.blocked

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingplace.background.MonitorScheduler
import com.pingplace.data.local.entity.BlockedTimeWindowEntity
import com.pingplace.data.repository.PingPlaceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BlockedTimesUiState(
    val windows: List<BlockedTimeWindowEntity> = emptyList()
)

class BlockedTimesViewModel(
    private val repository: PingPlaceRepository,
    private val scheduler: MonitorScheduler
) : ViewModel() {

    val uiState: StateFlow<BlockedTimesUiState> = repository.observeBlockedTimeWindows()
        .map { BlockedTimesUiState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BlockedTimesUiState())

    fun addWindow(dayOfWeek: Int, startMinutes: Int, endMinutes: Int, label: String) {
        viewModelScope.launch {
            repository.saveBlockedWindow(
                BlockedTimeWindowEntity(
                    dayOfWeek = dayOfWeek,
                    startMinutes = startMinutes,
                    endMinutes = endMinutes,
                    label = label
                )
            )
            scheduler.triggerImmediateRefresh()
        }
    }

    fun deleteWindow(id: Long) {
        viewModelScope.launch {
            repository.deleteBlockedWindow(id)
            scheduler.triggerImmediateRefresh()
        }
    }
}
