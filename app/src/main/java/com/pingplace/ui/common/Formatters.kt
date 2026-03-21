package com.pingplace.ui.common

import com.pingplace.model.TriggerType
import com.pingplace.model.UnitsSystem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

fun formatDistance(meters: Int, units: UnitsSystem): String {
    return if (units == UnitsSystem.IMPERIAL) {
        val miles = meters / 1609.34
        when {
            miles < 0.2 -> "${(meters * 3.28084).roundToInt()} ft"
            else -> "%.1f mi".format(miles)
        }
    } else {
        when {
            meters < 1000 -> "$meters m"
            else -> "%.1f km".format(meters / 1000.0)
        }
    }
}

fun formatTrigger(triggerType: TriggerType, meters: Int?, minutes: Int?, units: UnitsSystem): String {
    return when (triggerType) {
        TriggerType.DISTANCE -> "Within ${formatDistance(meters ?: 1609, units)}"
        TriggerType.TRAVEL_TIME -> "About ${minutes ?: 10} min away"
    }
}

fun formatDueDate(epochMillis: Long?): String? {
    if (epochMillis == null) return null
    val formatter = DateTimeFormatter.ofPattern("MMM d")
    return formatter.format(
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    )
}
