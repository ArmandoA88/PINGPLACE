package com.pingplace.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingplace.background.MonitorScheduler
import com.pingplace.data.local.entity.ReminderEntity
import com.pingplace.data.repository.PingPlaceRepository
import com.pingplace.domain.BlockedTimeEvaluator
import com.pingplace.model.ReminderFilter
import com.pingplace.model.UnitsSystem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BrandGroupUiModel(
    val brandName: String,
    val brandQuery: String,
    val reminders: List<ReminderEntity>,
    val activeCount: Int,
    val snoozedCount: Int,
    val completedCount: Int,
    val suppressedCount: Int,
    val summary: String
)

data class HomeUiState(
    val searchQuery: String = "",
    val filter: ReminderFilter = ReminderFilter.ACTIVE,
    val groups: List<BrandGroupUiModel> = emptyList(),
    val units: UnitsSystem = UnitsSystem.IMPERIAL
)

class HomeViewModel(
    private val repository: PingPlaceRepository,
    private val scheduler: MonitorScheduler
) : ViewModel() {

    private val controls = MutableStateFlow(HomeUiState())

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeReminders(),
        repository.observeBlockedTimeWindows(),
        repository.observeUserSettings(),
        controls
    ) { reminders, blockedWindows, settings, current ->
        val grouped = reminders.groupBy { it.brandQuery }.values.map { brandReminders ->
            val sample = brandReminders.first()
            val active = brandReminders.count { !it.isCompleted && !it.isSnoozed }
            val snoozed = brandReminders.count { it.isSnoozed }
            val completed = brandReminders.count { it.isCompleted }
            val suppressed = brandReminders.count {
                !it.isCompleted && !it.isSnoozed &&
                    !BlockedTimeEvaluator.isReminderAllowedNow(it, blockedWindows)
            }
            BrandGroupUiModel(
                brandName = sample.brandName,
                brandQuery = sample.brandQuery,
                reminders = brandReminders,
                activeCount = active,
                snoozedCount = snoozed,
                completedCount = completed,
                suppressedCount = suppressed,
                summary = "${sample.brandName}, ${brandReminders.size} reminder${if (brandReminders.size == 1) "" else "s"}"
            )
        }
            .filter { group ->
                val matchesSearch = current.searchQuery.isBlank() ||
                    group.brandName.contains(current.searchQuery, ignoreCase = true) ||
                    group.reminders.any { it.title.contains(current.searchQuery, ignoreCase = true) }
                val matchesFilter = when (current.filter) {
                    ReminderFilter.ACTIVE -> group.activeCount > 0
                    ReminderFilter.COMPLETED -> group.completedCount > 0
                    ReminderFilter.SNOOZED -> group.snoozedCount > 0
                    ReminderFilter.SUPPRESSED -> group.suppressedCount > 0
                }
                matchesSearch && matchesFilter
            }
            .sortedBy { it.brandName }

        current.copy(groups = grouped, units = settings.units)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onSearchChanged(value: String) {
        controls.update { it.copy(searchQuery = value) }
    }

    fun onFilterSelected(filter: ReminderFilter) {
        controls.update { it.copy(filter = filter) }
    }

    fun completeBrand(group: BrandGroupUiModel) {
        viewModelScope.launch {
            group.reminders.filterNot { it.isCompleted }.forEach { reminder ->
                repository.setReminderCompleted(reminder.id, true)
            }
            scheduler.triggerImmediateRefresh()
        }
    }

    fun snoozeBrand(group: BrandGroupUiModel) {
        viewModelScope.launch {
            val until = System.currentTimeMillis() + 30 * 60 * 1000
            group.reminders.filterNot { it.isCompleted }.forEach { reminder ->
                repository.snoozeReminder(reminder.id, until)
            }
        }
    }
}
