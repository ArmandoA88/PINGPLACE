package com.pingplace.ui.offline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingplace.data.local.entity.OfflinePlaceEntity
import com.pingplace.data.local.entity.OfflineRegionEntity
import com.pingplace.offline.OfflinePackManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OfflineRegionMapUiState(
    val isLoading: Boolean = true,
    val region: OfflineRegionEntity? = null,
    val places: List<OfflinePlaceEntity> = emptyList(),
    val errorMessage: String? = null,
    val isShowingTruncatedPlaces: Boolean = false,
    val availablePlaceCount: Int = 0,
    val fallbackMessage: String? = null
)

class OfflineRegionMapViewModel(
    private val regionId: String,
    private val offlinePackManager: OfflinePackManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OfflineRegionMapUiState())
    val uiState: StateFlow<OfflineRegionMapUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val region = offlinePackManager.getRegion(regionId)
            if (region == null) {
                _uiState.value = OfflineRegionMapUiState(
                    isLoading = false,
                    errorMessage = "This downloaded map is no longer available."
                )
                return@launch
            }

            var currentRegion = region
            var availablePlaceCount = offlinePackManager.getRegionPlaceCount(regionId)
            var places = offlinePackManager.getRegionPlaces(regionId)
            var fallbackMessage: String? = null

            if (currentRegion.placeCount > 0 && places.isEmpty()) {
                val boundsPlaceCount = offlinePackManager.countPlacesInBounds(
                    minLatitude = currentRegion.minLatitude,
                    maxLatitude = currentRegion.maxLatitude,
                    minLongitude = currentRegion.minLongitude,
                    maxLongitude = currentRegion.maxLongitude
                )
                if (boundsPlaceCount > 0) {
                    availablePlaceCount = boundsPlaceCount
                    places = offlinePackManager.getPlacesInBounds(
                        minLatitude = currentRegion.minLatitude,
                        maxLatitude = currentRegion.maxLatitude,
                        minLongitude = currentRegion.minLongitude,
                        maxLongitude = currentRegion.maxLongitude
                    )
                    fallbackMessage =
                        "Showing stores from overlapping downloaded areas inside this map."
                } else {
                    val repairedRegion = offlinePackManager.refreshInstalledRegion(regionId).getOrNull()
                    if (repairedRegion != null) {
                        currentRegion = repairedRegion
                        availablePlaceCount = offlinePackManager.getRegionPlaceCount(regionId)
                        places = offlinePackManager.getRegionPlaces(regionId)
                    }
                }
            }

            if (availablePlaceCount == 0 && places.isNotEmpty()) {
                availablePlaceCount = places.size
            }

            val finalRegion = currentRegion
            val truncated = availablePlaceCount > places.size
            _uiState.value = OfflineRegionMapUiState(
                isLoading = false,
                region = finalRegion,
                places = places,
                isShowingTruncatedPlaces = truncated,
                availablePlaceCount = availablePlaceCount,
                fallbackMessage = fallbackMessage
            )
        }
    }
}
