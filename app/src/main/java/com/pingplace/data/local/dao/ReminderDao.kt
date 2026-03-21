package com.pingplace.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pingplace.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY updatedAtEpochMillis DESC")
    fun observeAll(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 ORDER BY updatedAtEpochMillis DESC")
    fun observeActive(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 1 ORDER BY updatedAtEpochMillis DESC")
    fun observeCompleted(): Flow<List<ReminderEntity>>

    @Query("SELECT DISTINCT brandQuery FROM reminders WHERE isCompleted = 0 AND isSnoozed = 0")
    suspend fun getActiveBrandQueries(): List<String>

    @Query("SELECT * FROM reminders WHERE brandQuery = :brandQuery ORDER BY updatedAtEpochMillis DESC")
    fun observeByBrand(brandQuery: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 AND isSnoozed = 0")
    suspend fun getReadyToEvaluate(): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity): Long

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Query("UPDATE reminders SET isCompleted = :isCompleted, updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun setCompleted(id: Long, isCompleted: Boolean, updatedAt: Long)

    @Query("UPDATE reminders SET isSnoozed = :isSnoozed, snoozedUntilEpochMillis = :untilEpochMillis, updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun setSnoozed(id: Long, isSnoozed: Boolean, untilEpochMillis: Long?, updatedAt: Long)

    @Query("UPDATE reminders SET isSnoozed = 0, snoozedUntilEpochMillis = NULL, updatedAtEpochMillis = :updatedAt WHERE isSnoozed = 1 AND snoozedUntilEpochMillis IS NOT NULL AND snoozedUntilEpochMillis <= :nowEpochMillis")
    suspend fun clearExpiredSnoozes(nowEpochMillis: Long, updatedAt: Long)
}
