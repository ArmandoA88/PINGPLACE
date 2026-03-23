package com.pingplace.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingplace.background.MonitorScheduler
import com.pingplace.data.local.entity.ReminderEntity
import com.pingplace.data.repository.PingPlaceRepository
import com.pingplace.model.BlockedTimeBehavior
import com.pingplace.model.BrandCatalog
import com.pingplace.model.ReminderPriority
import com.pingplace.model.ReminderRepeatType
import com.pingplace.model.TriggerType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddReminderUiState(
    val reminderId: Long? = null,
    val isEditing: Boolean = false,
    val title: String = "",
    val notes: String = "",
    val brandName: String = BrandCatalog.defaults.first().name,
    val brandQuery: String = BrandCatalog.defaults.first().query,
    val triggerType: TriggerType = TriggerType.DISTANCE,
    val triggerDistanceMeters: Int = 1609,
    val triggerTravelTimeMinutes: Int = 10,
    val requiresDrivingFast: Boolean = false,
    val checklistText: String = "",
    val priority: ReminderPriority = ReminderPriority.NORMAL,
    val repeatType: ReminderRepeatType = ReminderRepeatType.NONE,
    val repeatDays: Set<Int> = emptySet(),
    val dueDateEpochMillis: Long? = null,
    val blockedTimeBehavior: BlockedTimeBehavior = BlockedTimeBehavior.RESPECT_GLOBAL,
    val allowedDays: Set<Int> = setOf(1, 2, 3, 4, 5),
    val allowedStartMinutes: Int = 9 * 60,
    val allowedEndMinutes: Int = 20 * 60,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveCompleted: Boolean = false,
    val errorMessage: String? = null
)

class AddReminderViewModel(
    private val repository: PingPlaceRepository,
    private val scheduler: MonitorScheduler,
    private val reminderId: Long? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddReminderUiState())
    val uiState: StateFlow<AddReminderUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = repository.getUserSettings()
            _uiState.update {
                it.copy(
                    triggerType = settings.defaultTriggerType,
                    triggerDistanceMeters = settings.defaultDistanceMeters,
                    triggerTravelTimeMinutes = settings.defaultTravelTimeMinutes,
                    blockedTimeBehavior = if (settings.respectBlockedTimesByDefault) {
                        BlockedTimeBehavior.RESPECT_GLOBAL
                    } else {
                        BlockedTimeBehavior.IGNORE_GLOBAL
                    }
                )
            }
        }
        if (reminderId != null) {
            loadReminder(reminderId)
        }
    }

    fun updateTitle(value: String) = _uiState.update { it.copy(title = value, errorMessage = null) }
    fun updateNotes(value: String) = _uiState.update { it.copy(notes = value) }
    fun updateChecklist(value: String) = _uiState.update { it.copy(checklistText = value) }
    fun updateBrand(name: String, query: String) = _uiState.update { it.copy(brandName = name, brandQuery = query) }
    fun updateTriggerType(value: TriggerType) = _uiState.update { it.copy(triggerType = value) }
    fun updateDistance(value: Int) = _uiState.update { it.copy(triggerDistanceMeters = value) }
    fun updateTravelMinutes(value: Int) = _uiState.update { it.copy(triggerTravelTimeMinutes = value) }
    fun updateRequiresDrivingFast(value: Boolean) = _uiState.update { it.copy(requiresDrivingFast = value) }
    fun updatePriority(value: ReminderPriority) = _uiState.update { it.copy(priority = value) }
    fun updateRepeatType(value: ReminderRepeatType) = _uiState.update { it.copy(repeatType = value) }
    fun toggleRepeatDay(day: Int) = _uiState.update { state ->
        state.copy(repeatDays = state.repeatDays.toggle(day))
    }
    fun updateDueDate(value: Long?) = _uiState.update { it.copy(dueDateEpochMillis = value) }
    fun updateBlockedBehavior(value: BlockedTimeBehavior) = _uiState.update { it.copy(blockedTimeBehavior = value) }
    fun toggleAllowedDay(day: Int) = _uiState.update { state ->
        state.copy(allowedDays = state.allowedDays.toggle(day))
    }
    fun updateAllowedStart(value: Int) = _uiState.update { it.copy(allowedStartMinutes = value) }
    fun updateAllowedEnd(value: Int) = _uiState.update { it.copy(allowedEndMinutes = value) }

    fun saveReminder() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Task title is required.") }
            return
        }
        if (state.brandQuery.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Choose a brand or type a custom one.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val now = System.currentTimeMillis()
            val existing = state.reminderId?.let { repository.getReminder(it) }
            val reminder = ReminderEntity(
                id = existing?.id ?: 0,
                title = state.title.trim(),
                notes = state.notes.trim(),
                brandName = state.brandName.trim(),
                brandQuery = state.brandQuery.trim(),
                triggerType = state.triggerType,
                triggerDistanceMeters = state.triggerDistanceMeters,
                triggerTravelTimeMinutes = state.triggerTravelTimeMinutes,
                requiresDrivingFast = state.requiresDrivingFast,
                checklistItems = state.checklistText.lines().map(String::trim).filter { it.isNotBlank() },
                isCompleted = existing?.isCompleted ?: false,
                isSnoozed = existing?.isSnoozed ?: false,
                snoozedUntilEpochMillis = existing?.snoozedUntilEpochMillis,
                priority = state.priority,
                dueDateEpochMillis = state.dueDateEpochMillis,
                repeatType = state.repeatType,
                repeatDaysOfWeek = state.repeatDays,
                respectBlockedTimes = state.blockedTimeBehavior,
                customAllowedDaysOfWeek = state.allowedDays,
                customAllowedStartMinutes = state.allowedStartMinutes,
                customAllowedEndMinutes = state.allowedEndMinutes,
                createdAtEpochMillis = existing?.createdAtEpochMillis ?: now,
                updatedAtEpochMillis = now
            )
            if (existing == null) {
                repository.saveReminder(reminder)
            } else {
                repository.updateReminder(
                    reminder.copy(
                        isCompleted = false,
                        isSnoozed = false,
                        snoozedUntilEpochMillis = null
                    )
                )
            }
            scheduler.scheduleMonitoring()
            scheduler.triggerImmediateRefresh()
            _uiState.update { it.copy(isSaving = false, saveCompleted = true) }
        }
    }

    fun consumeSaved() = _uiState.update { it.copy(saveCompleted = false) }

    private fun Set<Int>.toggle(value: Int): Set<Int> {
        return if (value in this) this - value else this + value
    }

    private fun loadReminder(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val reminder = repository.getReminder(id)
            _uiState.update { current ->
                if (reminder == null) {
                    current.copy(isLoading = false, errorMessage = "Reminder not found.")
                } else {
                    current.copy(
                        reminderId = reminder.id,
                        isEditing = true,
                        title = reminder.title,
                        notes = reminder.notes,
                        brandName = reminder.brandName,
                        brandQuery = reminder.brandQuery,
                        triggerType = reminder.triggerType,
                        triggerDistanceMeters = reminder.triggerDistanceMeters ?: current.triggerDistanceMeters,
                        triggerTravelTimeMinutes = reminder.triggerTravelTimeMinutes ?: current.triggerTravelTimeMinutes,
                        requiresDrivingFast = reminder.requiresDrivingFast,
                        checklistText = reminder.checklistItems.joinToString("\n"),
                        priority = reminder.priority ?: ReminderPriority.NORMAL,
                        repeatType = reminder.repeatType ?: ReminderRepeatType.NONE,
                        repeatDays = reminder.repeatDaysOfWeek,
                        dueDateEpochMillis = reminder.dueDateEpochMillis,
                        blockedTimeBehavior = reminder.respectBlockedTimes,
                        allowedDays = reminder.customAllowedDaysOfWeek.ifEmpty { current.allowedDays },
                        allowedStartMinutes = reminder.customAllowedStartMinutes ?: current.allowedStartMinutes,
                        allowedEndMinutes = reminder.customAllowedEndMinutes ?: current.allowedEndMinutes,
                        isLoading = false
                    )
                }
            }
        }
    }
}
