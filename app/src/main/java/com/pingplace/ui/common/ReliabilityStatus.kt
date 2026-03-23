package com.pingplace.ui.common

data class ReliabilityStatus(
    val locationServicesEnabled: Boolean,
    val fineLocationGranted: Boolean,
    val backgroundLocationGranted: Boolean,
    val notificationsGranted: Boolean,
    val batteryOptimizationDisabled: Boolean
) {
    val needsAttention: Boolean
        get() = !locationServicesEnabled ||
            !fineLocationGranted ||
            !backgroundLocationGranted ||
            !notificationsGranted ||
            !batteryOptimizationDisabled
}
