package com.pingplace.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pingplace.data.local.entity.OfflineRegionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineRegionDao {

    @Query("SELECT * FROM offline_regions ORDER BY displayName ASC")
    fun observeAll(): Flow<List<OfflineRegionEntity>>

    @Query("SELECT * FROM offline_regions ORDER BY displayName ASC")
    suspend fun getAll(): List<OfflineRegionEntity>

    @Query("SELECT * FROM offline_regions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): OfflineRegionEntity?

    @Query(
        """
        SELECT id FROM offline_regions
        WHERE sourceUrl LIKE 'asset://offline-packs/%'
          AND updatedAtEpochMillis = :legacyUpdatedAtEpochMillis
        """
    )
    suspend fun getLegacySampleIds(legacyUpdatedAtEpochMillis: Long): List<String>

    @Query(
        """
        SELECT COUNT(*) FROM offline_regions
        WHERE :latitude BETWEEN minLatitude AND maxLatitude
          AND :longitude BETWEEN minLongitude AND maxLongitude
        """
    )
    suspend fun countCovering(latitude: Double, longitude: Double): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(region: OfflineRegionEntity)

    @Query("DELETE FROM offline_regions WHERE id = :id")
    suspend fun deleteById(id: String)
}
