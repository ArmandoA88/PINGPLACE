package com.pingplace.domain

import com.pingplace.data.local.entity.BrandVisitStateEntity

/** Timing and approach distances shared by live monitoring and periodic recovery. */
object MonitoringPolicy {
    const val FAST_SPEED_MPS = 8.94f

    fun intervalMillis(speedMps: Float): Long = when {
        speedMps >= FAST_SPEED_MPS -> 5_000L
        speedMps >= 1f -> 10_000L
        else -> 30_000L
    }

    fun approachDistanceMeters(configuredMeters: Int, speedMps: Float): Double =
        maxOf(configuredMeters.toDouble(), if (speedMps >= FAST_SPEED_MPS) {
            (speedMps * 45.0).coerceAtMost(2_000.0)
        } else 0.0)

    fun shouldAlert(previous: BrandVisitStateEntity?, placeId: String?, now: Long, speedMps: Float): Boolean {
        val lastAlert = previous?.lastNotifiedAtEpochMillis ?: return true
        // Debounce GPS boundary jitter, even if an intervening fix was outside the radius.
        val cooldown = if (speedMps >= FAST_SPEED_MPS) 3 * 60_000L else 10 * 60_000L
        return previous.placeId != placeId || now - lastAlert >= cooldown
    }
}
