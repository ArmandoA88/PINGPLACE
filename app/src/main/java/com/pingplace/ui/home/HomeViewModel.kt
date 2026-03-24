package com.pingplace.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingplace.background.MonitorScheduler
import com.pingplace.data.local.entity.ReminderEntity
import com.pingplace.data.repository.PingPlaceRepository
import com.pingplace.domain.BlockedTimeEvaluator
import com.pingplace.location.DeviceLocationClient
import com.pingplace.location.LiveLookupDeferredException
import com.pingplace.location.NearbyPlace
import com.pingplace.location.NearbyPlaceSearchProvider
import com.pingplace.location.OpenStreetMapSearchProvider
import com.pingplace.model.ReminderFilter
import com.pingplace.model.ReminderPriority
import com.pingplace.model.BrandCatalog
import com.pingplace.model.BlockedTimeBehavior
import com.pingplace.model.ReminderRepeatType
import com.pingplace.model.TriggerType
import com.pingplace.model.UnitsSystem
import com.pingplace.offline.OfflinePlaceSearchProvider
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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

data class BrandSuggestionUiModel(
    val brandName: String,
    val brandQuery: String
)

data class NearbyStoreUiModel(
    val brandName: String,
    val brandQuery: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Double,
    val estimatedTravelMinutes: Int?
)

data class DashboardMapUiState(
    val isLoading: Boolean = true,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val nearbyStores: List<NearbyStoreUiModel> = emptyList(),
    val searchedBrands: List<BrandSuggestionUiModel> = emptyList(),
    val errorMessage: String? = null
)

data class QuickAddUiState(
    val title: String = "",
    val brandName: String = "",
    val brandQuery: String = "",
    val suggestedBrands: List<BrandSuggestionUiModel> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

data class HomeUiState(
    val searchQuery: String = "",
    val filter: ReminderFilter = ReminderFilter.ACTIVE,
    val groups: List<BrandGroupUiModel> = emptyList(),
    val units: UnitsSystem = UnitsSystem.IMPERIAL,
    val dashboardMap: DashboardMapUiState = DashboardMapUiState(),
    val quickAdd: QuickAddUiState = QuickAddUiState()
)

class HomeViewModel(
    private val repository: PingPlaceRepository,
    private val scheduler: MonitorScheduler,
    private val locationClient: DeviceLocationClient,
    private val placeSearchProvider: NearbyPlaceSearchProvider
) : ViewModel() {

    private data class HomeControlsState(
        val searchQuery: String = "",
        val filter: ReminderFilter = ReminderFilter.ACTIVE,
        val quickAddTitle: String = "",
        val quickAddBrandName: String = "",
        val quickAddBrandQuery: String = "",
        val quickAddErrorMessage: String? = null,
        val isSavingQuickAdd: Boolean = false
    )

    private val controls = MutableStateFlow(HomeControlsState())
    private val dashboardMapState = MutableStateFlow(DashboardMapUiState())

    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeReminders(),
        repository.observeBlockedTimeWindows(),
        repository.observeUserSettings(),
        controls,
        dashboardMapState
    ) { reminders, blockedWindows, settings, current, dashboardMap ->
        val grouped = reminders.groupBy { it.brandQuery }.values.map { brandReminders ->
            val ordered = brandReminders.sortedWith(reminderComparator())
            val sample = ordered.first()
            val active = ordered.count { !it.isCompleted && !it.isSnoozed }
            val snoozed = ordered.count { it.isSnoozed }
            val completed = ordered.count { it.isCompleted }
            val suppressed = ordered.count {
                !it.isCompleted && !it.isSnoozed &&
                    !BlockedTimeEvaluator.isReminderAllowedNow(it, blockedWindows)
            }
            BrandGroupUiModel(
                brandName = sample.brandName,
                brandQuery = sample.brandQuery,
                reminders = ordered,
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

        val suggestedBrands = buildSuggestedBrands(reminders)
        val selectedBrand = resolveQuickAddBrand(
            suggestedBrands = suggestedBrands,
            brandName = current.quickAddBrandName,
            brandQuery = current.quickAddBrandQuery
        )

        HomeUiState(
            searchQuery = current.searchQuery,
            filter = current.filter,
            groups = grouped,
            units = settings.units,
            dashboardMap = dashboardMap,
            quickAdd = QuickAddUiState(
                title = current.quickAddTitle,
                brandName = selectedBrand.brandName,
                brandQuery = selectedBrand.brandQuery,
                suggestedBrands = suggestedBrands,
                isSaving = current.isSavingQuickAdd,
                errorMessage = current.quickAddErrorMessage
            )
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        observeDashboardContent()
    }

    fun onSearchChanged(value: String) {
        controls.update { it.copy(searchQuery = value) }
    }

    fun onFilterSelected(filter: ReminderFilter) {
        controls.update { it.copy(filter = filter) }
    }

    fun updateQuickAddTitle(value: String) {
        controls.update { it.copy(quickAddTitle = value, quickAddErrorMessage = null) }
    }

    fun updateQuickAddBrand(value: String) {
        controls.update {
            it.copy(
                quickAddBrandName = value,
                quickAddBrandQuery = value,
                quickAddErrorMessage = null
            )
        }
    }

    fun selectQuickAddBrand(brand: BrandSuggestionUiModel) {
        controls.update {
            it.copy(
                quickAddBrandName = brand.brandName,
                quickAddBrandQuery = brand.brandQuery,
                quickAddErrorMessage = null
            )
        }
    }

    fun refreshNearbyDashboard() {
        viewModelScope.launch {
            val brands = dashboardMapState.value.searchedBrands.ifEmpty {
                buildFallbackDashboardBrands()
            }
            refreshNearbyDashboard(brands)
        }
    }

    fun saveQuickReminder() {
        val state = uiState.value
        if (state.quickAdd.title.isBlank()) {
            controls.update { it.copy(quickAddErrorMessage = "Task title is required.") }
            return
        }
        if (state.quickAdd.brandQuery.isBlank()) {
            controls.update { it.copy(quickAddErrorMessage = "Choose a brand or type a custom one.") }
            return
        }

        viewModelScope.launch {
            controls.update { it.copy(isSavingQuickAdd = true, quickAddErrorMessage = null) }
            runCatching {
                val settings = repository.getUserSettings()
                val now = System.currentTimeMillis()
                repository.saveReminder(
                    ReminderEntity(
                        title = state.quickAdd.title.trim(),
                        brandName = state.quickAdd.brandName.trim(),
                        brandQuery = state.quickAdd.brandQuery.trim(),
                        triggerType = settings.defaultTriggerType,
                        triggerDistanceMeters = distanceFor(settings.defaultTriggerType, settings.defaultDistanceMeters),
                        triggerTravelTimeMinutes = travelTimeFor(
                            settings.defaultTriggerType,
                            settings.defaultTravelTimeMinutes
                        ),
                        priority = ReminderPriority.NORMAL,
                        repeatType = ReminderRepeatType.NONE,
                        respectBlockedTimes = if (settings.respectBlockedTimesByDefault) {
                            BlockedTimeBehavior.RESPECT_GLOBAL
                        } else {
                            BlockedTimeBehavior.IGNORE_GLOBAL
                        },
                        createdAtEpochMillis = now,
                        updatedAtEpochMillis = now
                    )
                )
                scheduler.scheduleMonitoring()
                scheduler.triggerImmediateRefresh()
            }.onSuccess {
                controls.update {
                    it.copy(
                        quickAddTitle = "",
                        isSavingQuickAdd = false,
                        quickAddErrorMessage = null
                    )
                }
            }.onFailure {
                controls.update {
                    it.copy(
                        isSavingQuickAdd = false,
                        quickAddErrorMessage = "Reminder could not be saved right now."
                    )
                }
            }
        }
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

    private fun reminderComparator() = compareBy<ReminderEntity>(
        { it.isCompleted },
        { it.isSnoozed },
        { it.dueDateEpochMillis ?: Long.MAX_VALUE },
        { priorityRank(it.priority ?: ReminderPriority.NORMAL) }
    ).thenByDescending { it.updatedAtEpochMillis }

    private fun priorityRank(priority: ReminderPriority): Int = when (priority) {
        ReminderPriority.HIGH -> 0
        ReminderPriority.NORMAL -> 1
        ReminderPriority.LOW -> 2
    }

    private fun observeDashboardContent() {
        viewModelScope.launch {
            repository.observeReminders()
                .map { reminders ->
                    val activeBrands = buildActiveDashboardBrands(reminders)
                    if (activeBrands.isEmpty()) {
                        buildFallbackDashboardBrands()
                    } else {
                        activeBrands
                    }
                }
                .distinctUntilChanged()
                .collectLatest { brands ->
                    refreshNearbyDashboard(brands)
                }
        }
    }

    private suspend fun refreshNearbyDashboard(brands: List<BrandSuggestionUiModel>) {
        val previous = dashboardMapState.value
        dashboardMapState.value = previous.copy(
            isLoading = true,
            searchedBrands = brands,
            errorMessage = null
        )

        val location = try {
            locationClient.getCurrentLocation()
        } catch (_: Exception) {
            null
        }

        if (location == null) {
            dashboardMapState.value = previous.copy(
                isLoading = false,
                searchedBrands = brands,
                errorMessage = if (previous.latitude != null && previous.longitude != null) {
                    "Current location is unavailable. Showing the last nearby snapshot."
                } else {
                    "Current location is unavailable."
                }
            )
            return
        }

        val results = buildList {
            for (brand in brands) {
                add(
                    brand to placeSearchProvider.searchNearby(
                        query = brand.brandQuery,
                        currentLocation = location,
                        radiusMeters = DASHBOARD_RADIUS_METERS
                    )
                )
                if (buildNearbyStores(this).size >= MAX_TOTAL_NEARBY_STORES) {
                    break
                }
            }
        }

        val freshNearbyStores = buildNearbyStores(results)
        val firstError = results.firstNotNullOfOrNull { (_, result) ->
            result.exceptionOrNull()
        }
        val showingCachedSnapshot = freshNearbyStores.isEmpty() &&
            firstError != null &&
            previous.nearbyStores.isNotEmpty()
        val nearbyStores = when {
            freshNearbyStores.isNotEmpty() -> freshNearbyStores
            showingCachedSnapshot -> previous.nearbyStores
            else -> emptyList()
        }
        val error = if (freshNearbyStores.isEmpty()) {
            firstError?.let { nearbyErrorMessage(it, showingCachedSnapshot) }
        } else {
            null
        }

        dashboardMapState.value = DashboardMapUiState(
            isLoading = false,
            latitude = location.latitude,
            longitude = location.longitude,
            nearbyStores = nearbyStores,
            searchedBrands = brands,
            errorMessage = error
        )
    }

    private fun buildSuggestedBrands(reminders: List<ReminderEntity>): List<BrandSuggestionUiModel> {
        val reminderBrands = reminders
            .sortedByDescending { it.updatedAtEpochMillis }
            .map { BrandSuggestionUiModel(brandName = it.brandName, brandQuery = it.brandQuery) }

        val defaults = BrandCatalog.defaults.map {
            BrandSuggestionUiModel(brandName = it.name, brandQuery = it.query)
        }

        return (reminderBrands + defaults)
            .distinctBy { it.brandQuery.lowercase() }
            .take(MAX_QUICK_ADD_BRANDS)
    }

    private fun buildActiveDashboardBrands(reminders: List<ReminderEntity>): List<BrandSuggestionUiModel> {
        return reminders
            .filter { !it.isCompleted && !it.isSnoozed }
            .groupBy { it.brandQuery }
            .values
            .sortedWith(
                compareByDescending<List<ReminderEntity>> { it.size }
                    .thenByDescending { brandReminders ->
                        brandReminders.maxOf { it.updatedAtEpochMillis }
                    }
            )
            .map { brandReminders ->
                val sample = brandReminders.first()
                BrandSuggestionUiModel(
                    brandName = sample.brandName,
                    brandQuery = sample.brandQuery
                )
            }
            .take(MAX_DASHBOARD_BRANDS)
    }

    private fun buildFallbackDashboardBrands(): List<BrandSuggestionUiModel> {
        return BrandCatalog.defaults
            .take(MAX_DASHBOARD_BRANDS)
            .map { BrandSuggestionUiModel(brandName = it.name, brandQuery = it.query) }
    }

    private fun resolveQuickAddBrand(
        suggestedBrands: List<BrandSuggestionUiModel>,
        brandName: String,
        brandQuery: String
    ): BrandSuggestionUiModel {
        if (brandName.isNotBlank() && brandQuery.isNotBlank()) {
            return BrandSuggestionUiModel(brandName = brandName, brandQuery = brandQuery)
        }
        return suggestedBrands.firstOrNull()
            ?: BrandSuggestionUiModel(
                brandName = BrandCatalog.defaults.first().name,
                brandQuery = BrandCatalog.defaults.first().query
            )
    }

    private fun buildNearbyStores(
        results: List<Pair<BrandSuggestionUiModel, Result<List<NearbyPlace>>>>
    ): List<NearbyStoreUiModel> {
        val seen = mutableSetOf<String>()
        return buildList {
            results.forEach { (brand, result) ->
                result.getOrNull()
                    .orEmpty()
                    .take(MAX_PLACES_PER_BRAND)
                    .forEach { place ->
                        val key = place.id.ifBlank {
                            "${place.name}|${place.address}|${place.latitude}|${place.longitude}"
                        }
                        if (seen.add(key)) {
                            add(
                                NearbyStoreUiModel(
                                    brandName = brand.brandName,
                                    brandQuery = brand.brandQuery,
                                    name = place.name,
                                    address = place.address,
                                    latitude = place.latitude,
                                    longitude = place.longitude,
                                    distanceMeters = place.distanceMeters,
                                    estimatedTravelMinutes = place.estimatedTravelMinutes
                                )
                            )
                        }
                    }
            }
        }.sortedBy { it.distanceMeters }
            .take(MAX_TOTAL_NEARBY_STORES)
    }

    private fun nearbyErrorMessage(error: Throwable): String {
        return nearbyErrorMessage(error, showingCachedSnapshot = false)
    }

    private fun nearbyErrorMessage(
        error: Throwable,
        showingCachedSnapshot: Boolean
    ): String {
        val snapshotSuffix = if (showingCachedSnapshot) {
            " Showing your last nearby snapshot."
        } else {
            ""
        }

        return when (error) {
            is LiveLookupDeferredException ->
                "Live nearby lookup only runs while you're driving or moving fast. Saved offline stores still match in this area.$snapshotSuffix"

            is OpenStreetMapSearchProvider.RateLimitedException ->
                "Free nearby lookup is busy right now. Try again in a minute.$snapshotSuffix"

            is OpenStreetMapSearchProvider.ServiceBusyException ->
                "Free nearby lookup timed out. Try again in a minute.$snapshotSuffix"

            is OpenStreetMapSearchProvider.RequestFailedException ->
                "Free nearby lookup failed with status ${error.code}.$snapshotSuffix"

            is OfflinePlaceSearchProvider.OfflineCoverageMissingException ->
                "No offline pack is installed for this area.$snapshotSuffix"

            else -> "Nearby stores could not be loaded right now.$snapshotSuffix"
        }
    }

    private fun distanceFor(triggerType: TriggerType, defaultDistanceMeters: Int): Int? {
        return if (triggerType == TriggerType.DISTANCE) defaultDistanceMeters else null
    }

    private fun travelTimeFor(triggerType: TriggerType, defaultTravelTimeMinutes: Int): Int? {
        return if (triggerType == TriggerType.TRAVEL_TIME) defaultTravelTimeMinutes else null
    }

    private companion object {
        const val DASHBOARD_RADIUS_METERS = 10_000.0
        const val MAX_DASHBOARD_BRANDS = 3
        const val MAX_PLACES_PER_BRAND = 3
        const val MAX_TOTAL_NEARBY_STORES = 8
        const val MAX_QUICK_ADD_BRANDS = 6
    }
}
