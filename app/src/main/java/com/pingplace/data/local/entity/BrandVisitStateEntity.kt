package com.pingplace.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "brand_visit_state")
data class BrandVisitStateEntity(
    @PrimaryKey val brandQuery: String,
    val placeId: String? = null,
    val isActive: Boolean = false,
    val lastDistanceMeters: Double? = null,
    val lastNotifiedAtEpochMillis: Long? = null,
    val lastSeenAtEpochMillis: Long? = null
)
