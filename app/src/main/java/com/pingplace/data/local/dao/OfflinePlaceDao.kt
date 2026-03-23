package com.pingplace.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pingplace.data.local.entity.OfflinePlaceEntity

@Dao
interface OfflinePlaceDao {

    @Query(
        """
        SELECT * FROM offline_places
        WHERE latitude BETWEEN :minLatitude AND :maxLatitude
          AND longitude BETWEEN :minLongitude AND :maxLongitude
          AND searchText LIKE '%' || :normalizedQuery || '%'
        LIMIT :limit
        """
    )
    suspend fun searchCandidates(
        normalizedQuery: String,
        minLatitude: Double,
        maxLatitude: Double,
        minLongitude: Double,
        maxLongitude: Double,
        limit: Int
    ): List<OfflinePlaceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(places: List<OfflinePlaceEntity>)

    @Query("DELETE FROM offline_places WHERE regionId = :regionId")
    suspend fun deleteByRegion(regionId: String)
}
