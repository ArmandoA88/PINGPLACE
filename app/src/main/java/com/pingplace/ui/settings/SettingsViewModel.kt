package com.pingplace.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingplace.background.MonitorScheduler
import com.pingplace.background.NotificationHelper
import com.pingplace.data.local.entity.ReminderEntity
import com.pingplace.data.local.entity.UserSettingsEntity
import com.pingplace.data.repository.PingPlaceRepository
import com.pingplace.domain.BrandReminderMatch
import com.pingplace.model.TriggerType
import com.pingplace.model.UnitsSystem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: PingPlaceRepository,
    private val scheduler: MonitorScheduler,
    private val notificationHelper: NotificationHelper
) : ViewModel() {

    val uiState: StateFlow<UserSettingsEntity> = repository.observeUserSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettingsEntity())

    fun updateDefaultTrigger(triggerType: TriggerType) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(defaultTriggerType = triggerType) }
        }
    }

    fun updateDistance(distance: Int) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(defaultDistanceMeters = distance) }
        }
    }

    fun updateTravelTime(minutes: Int) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(defaultTravelTimeMinutes = minutes) }
        }
    }

    fun updateUnits(units: UnitsSystem) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(units = units) }
        }
    }

    fun updateSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(soundEnabled = enabled) }
        }
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(notificationsEnabled = enabled) }
        }
    }

    fun updateBackgroundLocationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(backgroundLocationEnabled = enabled) }
            if (enabled) {
                scheduler.scheduleMonitoring()
                scheduler.triggerImmediateRefresh()
            } else {
                scheduler.cancelMonitoring()
            }
        }
    }

    fun updateRespectBlockedTimesByDefault(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(respectBlockedTimesByDefault = enabled) }
        }
    }

    fun updateDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(darkModeEnabled = enabled) }
        }
    }

    fun triggerTestNotification() {
        if (!uiState.value.notificationsEnabled) return
        notificationHelper.showBrandReminder(
            BrandReminderMatch(
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
                    )
                ),
                nearestPlace = null,
                shouldNotifyNow = true,
                suppressedByBlockedTime = false
            ),
            soundEnabled = uiState.value.soundEnabled
        )
    }

    fun rerunMonitoring() {
        scheduler.triggerImmediateRefresh()
    }
}
