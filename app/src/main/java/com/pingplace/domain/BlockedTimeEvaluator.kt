package com.pingplace.domain

import com.pingplace.data.local.entity.BlockedTimeWindowEntity
import com.pingplace.data.local.entity.ReminderEntity
import com.pingplace.model.BlockedTimeBehavior
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

object BlockedTimeEvaluator {

    fun isReminderAllowedNow(
        reminder: ReminderEntity,
        blockedWindows: List<BlockedTimeWindowEntity>,
        zoneId: ZoneId = ZoneId.systemDefault(),
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        return when (reminder.respectBlockedTimes) {
            BlockedTimeBehavior.IGNORE_GLOBAL -> true
            BlockedTimeBehavior.CUSTOM_ALLOWED_HOURS -> isWithinCustomAllowedWindow(reminder, zoneId, nowMillis)
            BlockedTimeBehavior.RESPECT_GLOBAL -> !isBlockedNow(blockedWindows, zoneId, nowMillis)
        }
    }

    fun isBlockedNow(
        blockedWindows: List<BlockedTimeWindowEntity>,
        zoneId: ZoneId = ZoneId.systemDefault(),
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        val now = LocalDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), zoneId)
        val currentDay = now.dayOfWeek.value
        val currentMinutes = now.hour * 60 + now.minute
        val previousDay = if (currentDay == DayOfWeek.MONDAY.value) DayOfWeek.SUNDAY.value else currentDay - 1

        return blockedWindows.any { window ->
            when {
                !window.isEnabled -> false
                window.startMinutes <= window.endMinutes ->
                    window.dayOfWeek == currentDay &&
                        currentMinutes in window.startMinutes until window.endMinutes
                else ->
                    (window.dayOfWeek == currentDay && currentMinutes >= window.startMinutes) ||
                        (window.dayOfWeek == previousDay && currentMinutes < window.endMinutes)
            }
        }
    }

    private fun isWithinCustomAllowedWindow(
        reminder: ReminderEntity,
        zoneId: ZoneId,
        nowMillis: Long
    ): Boolean {
        val days = reminder.customAllowedDaysOfWeek
        val start = reminder.customAllowedStartMinutes ?: return true
        val end = reminder.customAllowedEndMinutes ?: return true
        val now = LocalDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), zoneId)
        val currentMinutes = now.hour * 60 + now.minute
        val currentDay = now.dayOfWeek.value
        val previousDay = if (currentDay == DayOfWeek.MONDAY.value) DayOfWeek.SUNDAY.value else currentDay - 1

        if (days.isEmpty()) return true
        return when {
            start <= end -> currentDay in days && currentMinutes in start until end
            else -> (currentDay in days && currentMinutes >= start) ||
                (previousDay in days && currentMinutes < end)
        }
    }
}
