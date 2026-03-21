package com.pingplace.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pingplace.background.MonitorScheduler
import com.pingplace.data.repository.PingPlaceRepository
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val repository: PingPlaceRepository,
    private val scheduler: MonitorScheduler
) : ViewModel() {

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(notificationsEnabled = enabled) }
        }
    }

    fun setBackgroundLocationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings { it.copy(backgroundLocationEnabled = enabled) }
            if (enabled) {
                scheduler.scheduleMonitoring()
            }
        }
    }

    fun finishOnboarding() {
        viewModelScope.launch {
            repository.updateSettings { it.copy(onboardingComplete = true) }
            if (repository.getUserSettings().backgroundLocationEnabled) {
                scheduler.scheduleMonitoring()
            }
            scheduler.triggerImmediateRefresh()
        }
    }
}
