package com.pingplace.ui.brand

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingplace.background.MonitorScheduler
import com.pingplace.data.local.entity.ReminderEntity
import com.pingplace.data.repository.PingPlaceRepository
import com.pingplace.location.DeviceLocationClient
import com.pingplace.location.LiveLookupDeferredException
import com.pingplace.location.NearbyPlace
import com.pingplace.location.NearbyPlaceSearchProvider
import com.pingplace.location.OpenStreetMapSearchProvider
import com.pingplace.model.ReminderPriority
import com.pingplace.model.UnitsSystem
import com.pingplace.offline.OfflinePlaceSearchProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BrandDetailUiState(
    val brandName: String = "",
    val reminders: List<ReminderEntity> = emptyList(),
    val nearbyPlaces: List<NearbyPlace> = emptyList(),
    val isLoadingPlaces: Boolean = false,
    val placeError: String? = null,
    val units: UnitsSystem = UnitsSystem.IMPERIAL
)

class BrandDetailViewModel(
    private val brandQuery: String,
    private val repository: PingPlaceRepository,
    private val scheduler: MonitorScheduler,
    private val locationClient: DeviceLocationClient,
    private val placeSearchProvider: NearbyPlaceSearchProvider
) : ViewModel() {

    private val placeState = MutableStateFlow(BrandDetailUiState(isLoadingPlaces = true))

    val uiState: StateFlow<BrandDetailUiState> = combine(
        repository.observeRemindersByBrand(brandQuery),
        repository.observeUserSettings(),
        placeState
    ) { reminders, settings, places ->
        places.copy(
            brandName = reminders.firstOrNull()?.brandName ?: brandQuery,
            reminders = reminders.sortedWith(
                compareBy<ReminderEntity>(
                    { it.isCompleted },
                    { it.isSnoozed },
                    { it.dueDateEpochMillis ?: Long.MAX_VALUE },
                    {
                        when (it.priority ?: ReminderPriority.NORMAL) {
                            ReminderPriority.HIGH -> 0
                            ReminderPriority.NORMAL -> 1
                            ReminderPriority.LOW -> 2
                        }
                    }
                ).thenByDescending { it.updatedAtEpochMillis }
            ),
            units = settings.units
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BrandDetailUiState())

    init {
        refreshNearbyPlaces()
    }

    fun refreshNearbyPlaces() {
        viewModelScope.launch {
            placeState.value = placeState.value.copy(isLoadingPlaces = true, placeError = null)
            val location = try {
                locationClient.getCurrentLocation()
            } catch (_: Exception) {
                null
            }
            if (location == null) {
                placeState.value = placeState.value.copy(
                    isLoadingPlaces = false,
                    placeError = "Current location is unavailable."
                )
                return@launch
            }
            val result = placeSearchProvider.searchNearby(brandQuery, location, 10_000.0)
            placeState.value = result.fold(
                onSuccess = {
                    placeState.value.copy(
                        nearbyPlaces = it,
                        isLoadingPlaces = false,
                        placeError = null
                    )
                },
                onFailure = {
                    val message = when (it) {
                        is LiveLookupDeferredException ->
                            "Live nearby lookup only runs while you're driving or moving fast. Saved offline stores still match here."

                        is OpenStreetMapSearchProvider.RateLimitedException ->
                            "Free nearby lookup is busy right now. Try again in a minute."

                        is OpenStreetMapSearchProvider.ServiceBusyException ->
                            "Free nearby lookup timed out. Try again in a minute."

                        is OpenStreetMapSearchProvider.RequestFailedException ->
                            "Free nearby lookup failed with status ${it.code}."

                        is OfflinePlaceSearchProvider.OfflineCoverageMissingException ->
                            "No offline pack is installed for this area."

                        else -> "Nearby places could not be loaded right now."
                    }
                    placeState.value.copy(
                        isLoadingPlaces = false,
                        placeError = message
                    )
                }
            )
        }
    }

    fun completeAll() {
        viewModelScope.launch {
            uiState.value.reminders.filterNot { it.isCompleted }.forEach {
                repository.setReminderCompleted(it.id, true)
            }
            scheduler.triggerImmediateRefresh()
        }
    }

    fun snoozeAll() {
        viewModelScope.launch {
            val until = System.currentTimeMillis() + 30 * 60 * 1000
            uiState.value.reminders.filterNot { it.isCompleted }.forEach {
                repository.snoozeReminder(it.id, until)
            }
            scheduler.triggerImmediateRefresh()
        }
    }

    fun completeReminder(id: Long) {
        viewModelScope.launch {
            repository.setReminderCompleted(id, true)
            scheduler.triggerImmediateRefresh()
        }
    }

    fun snoozeReminder(id: Long) {
        viewModelScope.launch {
            repository.snoozeReminder(id, System.currentTimeMillis() + 30 * 60 * 1000)
            scheduler.triggerImmediateRefresh()
        }
    }

    fun deleteReminder(id: Long) {
        viewModelScope.launch {
            repository.deleteReminder(id)
            scheduler.triggerImmediateRefresh()
        }
    }
}
