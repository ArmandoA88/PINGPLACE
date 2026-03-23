package com.pingplace.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "offline_places",
    primaryKeys = ["id"],
    indices = [
        Index("regionId"),
        Index(value = ["latitude", "longitude"]),
        Index("searchText")
    ]
)
data class OfflinePlaceEntity(
    val id: String,
    val regionId: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val searchText: String
)
