package com.pingplace.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_regions")
data class OfflineRegionEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val downloadedAtEpochMillis: Long,
    val updatedAtEpochMillis: Long? = null,
    val sourceUrl: String,
    val placeCount: Int,
    val minLatitude: Double,
    val maxLatitude: Double,
    val minLongitude: Double,
    val maxLongitude: Double
)
