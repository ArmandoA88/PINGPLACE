package com.pingplace.data.repository

import com.pingplace.data.local.entity.BlockedTimeWindowEntity
import com.pingplace.data.local.entity.BrandVisitStateEntity
import com.pingplace.data.local.entity.ReminderEntity
import com.pingplace.data.local.entity.UserSettingsEntity
import kotlinx.coroutines.flow.Flow

interface PingPlaceRepository {
    fun observeReminders(): Flow<List<ReminderEntity>>
    fun observeCompletedReminders(): Flow<List<ReminderEntity>>
    fun observeRemindersByBrand(brandQuery: String): Flow<List<ReminderEntity>>
    fun observeBlockedTimeWindows(): Flow<List<BlockedTimeWindowEntity>>
    fun observeUserSettings(): Flow<UserSettingsEntity>
    suspend fun getUserSettings(): UserSettingsEntity
    suspend fun getEnabledBlockedTimeWindows(): List<BlockedTimeWindowEntity>
    suspend fun getReadyToEvaluateReminders(): List<ReminderEntity>
    suspend fun getActiveBrandQueries(): List<String>
    suspend fun saveReminder(reminder: ReminderEntity): Long
    suspend fun updateReminder(reminder: ReminderEntity)
    suspend fun setReminderCompleted(id: Long, isCompleted: Boolean)
    suspend fun snoozeReminder(id: Long, untilEpochMillis: Long?)
    suspend fun clearExpiredSnoozes(nowEpochMillis: Long)
    suspend fun saveBlockedWindow(window: BlockedTimeWindowEntity): Long
    suspend fun deleteBlockedWindow(id: Long)
    suspend fun updateSettings(transform: (UserSettingsEntity) -> UserSettingsEntity)
    suspend fun getVisitState(brandQuery: String): BrandVisitStateEntity?
    suspend fun saveVisitState(state: BrandVisitStateEntity)
}
