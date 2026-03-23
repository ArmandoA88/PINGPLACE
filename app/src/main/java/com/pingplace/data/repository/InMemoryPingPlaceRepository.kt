package com.pingplace.data.repository

import com.pingplace.data.local.entity.BlockedTimeWindowEntity
import com.pingplace.data.local.entity.BrandVisitStateEntity
import com.pingplace.data.local.entity.ReminderEntity
import com.pingplace.data.local.entity.UserSettingsEntity
import com.pingplace.model.ReminderRepeatType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId

class InMemoryPingPlaceRepository : PingPlaceRepository {

    private val reminders = MutableStateFlow<List<ReminderEntity>>(emptyList())
    private val blockedWindows = MutableStateFlow<List<BlockedTimeWindowEntity>>(emptyList())
    private val settings = MutableStateFlow(UserSettingsEntity())
    private val visitStates = linkedMapOf<String, BrandVisitStateEntity>()

    override fun observeReminders(): Flow<List<ReminderEntity>> = reminders

    override fun observeCompletedReminders(): Flow<List<ReminderEntity>> =
        reminders.map { list -> list.filter { it.isCompleted }.sortedByDescending { it.updatedAtEpochMillis } }

    override fun observeRemindersByBrand(brandQuery: String): Flow<List<ReminderEntity>> =
        reminders.map { list ->
            list.filter { it.brandQuery == brandQuery }.sortedByDescending { it.updatedAtEpochMillis }
        }

    override fun observeReminder(id: Long): Flow<ReminderEntity?> =
        reminders.map { list -> list.firstOrNull { it.id == id } }

    override fun observeBlockedTimeWindows(): Flow<List<BlockedTimeWindowEntity>> = blockedWindows

    override fun observeUserSettings(): Flow<UserSettingsEntity> = settings

    override suspend fun getUserSettings(): UserSettingsEntity = settings.value

    override suspend fun getEnabledBlockedTimeWindows(): List<BlockedTimeWindowEntity> =
        blockedWindows.value.filter { it.isEnabled }

    override suspend fun getReadyToEvaluateReminders(): List<ReminderEntity> =
        reminders.value.filterNot { it.isCompleted }

    override suspend fun getActiveBrandQueries(): List<String> =
        reminders.value.filterNot { it.isCompleted || it.isSnoozed }.map { it.brandQuery }.distinct()

    override suspend fun getReminder(id: Long): ReminderEntity? = reminders.value.firstOrNull { it.id == id }

    override suspend fun saveReminder(reminder: ReminderEntity): Long {
        val nextId = ((reminders.value.maxOfOrNull { it.id } ?: 0L) + 1L)
        reminders.value = (reminders.value + reminder.copy(id = nextId))
            .sortedByDescending { it.updatedAtEpochMillis }
        return nextId
    }

    override suspend fun updateReminder(reminder: ReminderEntity) {
        reminders.value = reminders.value.map { if (it.id == reminder.id) reminder else it }
            .sortedByDescending { it.updatedAtEpochMillis }
    }

    override suspend fun deleteReminder(id: Long) {
        reminders.value = reminders.value.filterNot { it.id == id }
    }

    override suspend fun setReminderCompleted(id: Long, isCompleted: Boolean) {
        val now = System.currentTimeMillis()
        val current = reminders.value.firstOrNull { it.id == id } ?: return
        if (isCompleted) {
            val nextDueDate = nextDueDate(current, now)
            if (nextDueDate != null) {
                updateReminder(
                    current.copy(
                        isCompleted = false,
                        isSnoozed = false,
                        snoozedUntilEpochMillis = null,
                        dueDateEpochMillis = nextDueDate,
                        updatedAtEpochMillis = now
                    )
                )
                return
            }
        }
        reminders.value = reminders.value.map {
            if (it.id == id) {
                it.copy(
                    isCompleted = isCompleted,
                    updatedAtEpochMillis = now
                )
            } else {
                it
            }
        }.sortedByDescending { it.updatedAtEpochMillis }
    }

    override suspend fun snoozeReminder(id: Long, untilEpochMillis: Long?) {
        reminders.value = reminders.value.map {
            if (it.id == id) {
                it.copy(
                    isSnoozed = untilEpochMillis != null,
                    snoozedUntilEpochMillis = untilEpochMillis,
                    updatedAtEpochMillis = System.currentTimeMillis()
                )
            } else {
                it
            }
        }.sortedByDescending { it.updatedAtEpochMillis }
    }

    override suspend fun clearExpiredSnoozes(nowEpochMillis: Long) {
        reminders.value = reminders.value.map {
            if (it.isSnoozed && (it.snoozedUntilEpochMillis ?: Long.MAX_VALUE) <= nowEpochMillis) {
                it.copy(
                    isSnoozed = false,
                    snoozedUntilEpochMillis = null,
                    updatedAtEpochMillis = System.currentTimeMillis()
                )
            } else {
                it
            }
        }.sortedByDescending { it.updatedAtEpochMillis }
    }

    override suspend fun saveBlockedWindow(window: BlockedTimeWindowEntity): Long {
        val nextId = ((blockedWindows.value.maxOfOrNull { it.id } ?: 0L) + 1L)
        blockedWindows.value = (blockedWindows.value + window.copy(id = nextId))
            .sortedWith(compareBy<BlockedTimeWindowEntity> { it.dayOfWeek }.thenBy { it.startMinutes })
        return nextId
    }

    override suspend fun deleteBlockedWindow(id: Long) {
        blockedWindows.value = blockedWindows.value.filterNot { it.id == id }
    }

    override suspend fun updateSettings(transform: (UserSettingsEntity) -> UserSettingsEntity) {
        settings.value = transform(settings.value)
    }

    override suspend fun getVisitState(brandQuery: String): BrandVisitStateEntity? = visitStates[brandQuery]

    override suspend fun saveVisitState(state: BrandVisitStateEntity) {
        visitStates[state.brandQuery] = state
    }

    private fun nextDueDate(reminder: ReminderEntity, nowMillis: Long): Long? {
        val zoneId = ZoneId.systemDefault()
        val base = Instant.ofEpochMilli(reminder.dueDateEpochMillis ?: nowMillis).atZone(zoneId)
        val next = when (reminder.repeatType ?: ReminderRepeatType.NONE) {
            ReminderRepeatType.NONE -> null
            ReminderRepeatType.DAILY -> base.plusDays(1)
            ReminderRepeatType.WEEKDAYS -> {
                var candidate = base.plusDays(1)
                while (candidate.dayOfWeek.value > 5) {
                    candidate = candidate.plusDays(1)
                }
                candidate
            }
            ReminderRepeatType.WEEKLY -> {
                val days = reminder.repeatDaysOfWeek.ifEmpty { setOf(base.dayOfWeek.value) }
                var candidate = base.plusDays(1)
                while (candidate.dayOfWeek.value !in days) {
                    candidate = candidate.plusDays(1)
                }
                candidate
            }
        }
        return next?.toInstant()?.toEpochMilli()
    }
}
