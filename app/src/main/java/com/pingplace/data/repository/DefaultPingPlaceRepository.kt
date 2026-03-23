package com.pingplace.data.repository

import com.pingplace.data.local.dao.BlockedTimeWindowDao
import com.pingplace.data.local.dao.BrandVisitStateDao
import com.pingplace.data.local.dao.ReminderDao
import com.pingplace.data.local.dao.UserSettingsDao
import com.pingplace.data.local.entity.BlockedTimeWindowEntity
import com.pingplace.data.local.entity.BrandVisitStateEntity
import com.pingplace.data.local.entity.ReminderEntity
import com.pingplace.data.local.entity.UserSettingsEntity
import com.pingplace.model.ReminderRepeatType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId

class DefaultPingPlaceRepository(
    private val reminderDao: ReminderDao,
    private val blockedTimeWindowDao: BlockedTimeWindowDao,
    private val userSettingsDao: UserSettingsDao,
    private val brandVisitStateDao: BrandVisitStateDao
) : PingPlaceRepository {

    override fun observeReminders(): Flow<List<ReminderEntity>> = reminderDao.observeAll()

    override fun observeCompletedReminders(): Flow<List<ReminderEntity>> = reminderDao.observeCompleted()

    override fun observeRemindersByBrand(brandQuery: String): Flow<List<ReminderEntity>> =
        reminderDao.observeByBrand(brandQuery)

    override fun observeReminder(id: Long): Flow<ReminderEntity?> = reminderDao.observeById(id)

    override fun observeBlockedTimeWindows(): Flow<List<BlockedTimeWindowEntity>> =
        blockedTimeWindowDao.observeAll()

    override fun observeUserSettings(): Flow<UserSettingsEntity> =
        userSettingsDao.observe().map { it ?: DEFAULT_SETTINGS }

    override suspend fun getUserSettings(): UserSettingsEntity {
        val current = userSettingsDao.get()
        if (current != null) return current
        userSettingsDao.insert(DEFAULT_SETTINGS)
        return DEFAULT_SETTINGS
    }

    override suspend fun getEnabledBlockedTimeWindows(): List<BlockedTimeWindowEntity> =
        blockedTimeWindowDao.getEnabled()

    override suspend fun getReadyToEvaluateReminders(): List<ReminderEntity> =
        reminderDao.getReadyToEvaluate()

    override suspend fun getActiveBrandQueries(): List<String> = reminderDao.getActiveBrandQueries()

    override suspend fun getReminder(id: Long): ReminderEntity? = reminderDao.getById(id)

    override suspend fun saveReminder(reminder: ReminderEntity): Long = reminderDao.insert(reminder)

    override suspend fun updateReminder(reminder: ReminderEntity) {
        reminderDao.update(reminder)
    }

    override suspend fun deleteReminder(id: Long) {
        reminderDao.deleteById(id)
    }

    override suspend fun setReminderCompleted(id: Long, isCompleted: Boolean) {
        val now = System.currentTimeMillis()
        val existing = reminderDao.getById(id) ?: return
        if (isCompleted) {
            val nextDueDate = nextDueDate(existing, now)
            if (nextDueDate != null) {
                reminderDao.update(
                    existing.copy(
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
        reminderDao.setCompleted(id, isCompleted, now)
    }

    override suspend fun snoozeReminder(id: Long, untilEpochMillis: Long?) {
        reminderDao.setSnoozed(
            id = id,
            isSnoozed = untilEpochMillis != null,
            untilEpochMillis = untilEpochMillis,
            updatedAt = System.currentTimeMillis()
        )
    }

    override suspend fun clearExpiredSnoozes(nowEpochMillis: Long) {
        reminderDao.clearExpiredSnoozes(
            nowEpochMillis = nowEpochMillis,
            updatedAt = System.currentTimeMillis()
        )
    }

    override suspend fun saveBlockedWindow(window: BlockedTimeWindowEntity): Long =
        blockedTimeWindowDao.insert(window)

    override suspend fun deleteBlockedWindow(id: Long) {
        blockedTimeWindowDao.deleteById(id)
    }

    override suspend fun updateSettings(transform: (UserSettingsEntity) -> UserSettingsEntity) {
        val current = getUserSettings()
        userSettingsDao.insert(transform(current))
    }

    override suspend fun getVisitState(brandQuery: String): BrandVisitStateEntity? =
        brandVisitStateDao.getByBrand(brandQuery)

    override suspend fun saveVisitState(state: BrandVisitStateEntity) {
        brandVisitStateDao.insertOrReplace(state)
    }

    private companion object {
        val DEFAULT_SETTINGS = UserSettingsEntity()

        fun nextDueDate(reminder: ReminderEntity, nowMillis: Long): Long? {
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
}
