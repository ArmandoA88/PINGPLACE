package com.pingplace.ui.brand

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingplace.background.MonitorScheduler
import com.pingplace.data.local.entity.ReminderEntity
import com.pingplace.data.repository.PingPlaceRepository
import com.pingplace.location.DeviceLocationClient
import com.pingplace.location.NearbyPlace
import com.pingplace.location.NearbyPlaceSearchProvider
import com.pingplace.model.UnitsSystem
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
            reminders = reminders,
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
                    placeState.value.copy(
                        isLoadingPlaces = false,
                        placeError = "Nearby places need a valid Places API key in BuildConfig."
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
        }
    }
}
