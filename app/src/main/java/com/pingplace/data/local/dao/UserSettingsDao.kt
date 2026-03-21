package com.pingplace.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pingplace.data.local.entity.UserSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserSettingsDao {

    @Query("SELECT * FROM user_settings WHERE id = 0")
    fun observe(): Flow<UserSettingsEntity?>

    @Query("SELECT * FROM user_settings WHERE id = 0")
    suspend fun get(): UserSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(settings: UserSettingsEntity)

    @Update
    suspend fun update(settings: UserSettingsEntity)
}
