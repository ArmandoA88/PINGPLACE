package com.pingplace.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingplace.background.MonitorScheduler
import com.pingplace.background.NotificationHelper
import com.pingplace.data.local.entity.OfflineRegionEntity
import com.pingplace.data.local.entity.ReminderEntity
import com.pingplace.data.local.entity.UserSettingsEntity
import com.pingplace.data.repository.PingPlaceRepository
import com.pingplace.domain.BrandReminderMatch
import com.pingplace.model.PlaceSearchMode
import com.pingplace.model.TriggerType
import com.pingplace.model.UnitsSystem
import com.pingplace.offline.OfflinePackDescriptor
import com.pingplace.offline.OfflinePackKind
import com.pingplace.offline.OfflinePackManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OfflinePackUiState(
    val catalog: List<OfflinePackDescriptor> = emptyList(),
    val isLoadingCatalog: Boolean = false,
    val isImporting: Boolean = false,
    val activePackId: String? = null,
    val statusMessage: String? = null,
    val searchQuery: String = "",
    val selectedKind: OfflinePackKind? = null
)

class SettingsViewModel(
    private val repository: PingPlaceRepository,
    private val scheduler: MonitorScheduler,
    private val notificationHelper: NotificationHelper,
    private val offlinePackManager: OfflinePackManager
) : ViewModel() {

    val uiState: StateFlow<UserSettingsEntity> = repository.observeUserSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserSettingsEntity())
    val installedRegions: StateFlow<List<OfflineRegionEntity>> = offlinePackManager.observeInstalledRegions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _offlineUiState = MutableStateFlow(OfflinePackUiState())
    val offlineUiState: StateFlow<OfflinePackUiState> = _offlineUiState.asStateFlow()

    init {
        refreshOfflineCatalog()
    }

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

    fun updatePlaceSearchMode(mode: PlaceSearchMode) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(placeSearchMode = mode) }
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

    fun installBundledPack(descriptor: OfflinePackDescriptor) {
        viewModelScope.launch {
            _offlineUiState.value = _offlineUiState.value.copy(
                isImporting = true,
                activePackId = descriptor.id,
                statusMessage = null
            )
            val result = offlinePackManager.importCatalogPack(descriptor)
            _offlineUiState.value = _offlineUiState.value.copy(
                isImporting = false,
                activePackId = null,
                statusMessage = result.fold(
                    onSuccess = { "Installed ${it.displayName} with ${it.placeCount} places." },
                    onFailure = { "Offline pack install failed: ${it.message ?: "unknown error"}" }
                )
            )
        }
    }

    fun refreshInstalledPack(regionId: String) {
        val descriptor = offlineUiState.value.catalog.firstOrNull { it.id == regionId } ?: return
        installBundledPack(descriptor)
    }

    fun refreshOfflineCatalog() {
        viewModelScope.launch {
            _offlineUiState.value = _offlineUiState.value.copy(isLoadingCatalog = true)
            val result = offlinePackManager.loadCatalog()
            _offlineUiState.value = _offlineUiState.value.copy(
                isLoadingCatalog = false,
                catalog = result.getOrElse { emptyList() },
                statusMessage = result.exceptionOrNull()?.message
            )
        }
    }

    fun updateOfflineSearchQuery(value: String) {
        _offlineUiState.value = _offlineUiState.value.copy(searchQuery = value)
    }

    fun updateOfflineKindFilter(kind: OfflinePackKind?) {
        _offlineUiState.value = _offlineUiState.value.copy(selectedKind = kind)
    }

    fun importOfflinePack(url: String) {
        if (url.isBlank()) {
            _offlineUiState.value = _offlineUiState.value.copy(statusMessage = "Pack URL is required.")
            return
        }
        viewModelScope.launch {
            _offlineUiState.value = _offlineUiState.value.copy(
                isImporting = true,
                activePackId = "manual",
                statusMessage = null
            )
            val result = offlinePackManager.importPackFromUrl(url.trim())
            _offlineUiState.value = _offlineUiState.value.copy(
                isImporting = false,
                activePackId = null,
                statusMessage = result.fold(
                    onSuccess = { "Installed ${it.displayName} with ${it.placeCount} places." },
                    onFailure = { "Offline pack import failed: ${it.message ?: "unknown error"}" }
                )
            )
        }
    }

    fun removeOfflinePack(regionId: String) {
        viewModelScope.launch {
            offlinePackManager.removePack(regionId)
            _offlineUiState.value = _offlineUiState.value.copy(statusMessage = "Removed offline pack.")
        }
    }

    fun clearOfflineStatus() {
        _offlineUiState.value = _offlineUiState.value.copy(statusMessage = null)
    }
}
