package com.pingplace.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_time_windows")
data class BlockedTimeWindowEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayOfWeek: Int,
    val startMinutes: Int,
    val endMinutes: Int,
    val label: String = "",
    val isEnabled: Boolean = true
)
