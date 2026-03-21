package com.pingplace.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pingplace.data.local.entity.BlockedTimeWindowEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedTimeWindowDao {

    @Query("SELECT * FROM blocked_time_windows ORDER BY dayOfWeek ASC, startMinutes ASC")
    fun observeAll(): Flow<List<BlockedTimeWindowEntity>>

    @Query("SELECT * FROM blocked_time_windows WHERE isEnabled = 1")
    suspend fun getEnabled(): List<BlockedTimeWindowEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(window: BlockedTimeWindowEntity): Long

    @Update
    suspend fun update(window: BlockedTimeWindowEntity)

    @Query("DELETE FROM blocked_time_windows WHERE id = :id")
    suspend fun deleteById(id: Long)
}
