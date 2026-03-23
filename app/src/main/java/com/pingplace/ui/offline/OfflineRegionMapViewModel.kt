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
    val isShowingTruncatedPlaces: Boolean = false
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

            val places = offlinePackManager.getRegionPlaces(regionId)
            _uiState.value = OfflineRegionMapUiState(
                isLoading = false,
                region = region,
                places = places,
                isShowingTruncatedPlaces = places.size < region.placeCount
            )
        }
    }
}
