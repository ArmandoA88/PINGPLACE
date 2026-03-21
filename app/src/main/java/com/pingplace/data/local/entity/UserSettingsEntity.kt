package com.pingplace.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pingplace.model.TriggerType
import com.pingplace.model.UnitsSystem

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: Int = 0,
    val defaultTriggerType: TriggerType = TriggerType.DISTANCE,
    val defaultDistanceMeters: Int = 1609,
    val defaultTravelTimeMinutes: Int = 10,
    val units: UnitsSystem = UnitsSystem.IMPERIAL,
    val notificationsEnabled: Boolean = true,
    val backgroundLocationEnabled: Boolean = false,
    val respectBlockedTimesByDefault: Boolean = true,
    val soundEnabled: Boolean = true,
    val darkModeEnabled: Boolean = false,
    val onboardingComplete: Boolean = false
)
