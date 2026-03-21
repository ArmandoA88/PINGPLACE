package com.pingplace.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pingplace.model.BlockedTimeBehavior
import com.pingplace.model.ReminderPriority
import com.pingplace.model.ReminderRepeatType
import com.pingplace.model.TriggerType

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String = "",
    val brandName: String,
    val brandQuery: String,
    val triggerType: TriggerType,
    val triggerDistanceMeters: Int? = null,
    val triggerTravelTimeMinutes: Int? = null,
    val requiresDrivingFast: Boolean = false,
    val checklistItems: List<String> = emptyList(),
    val isCompleted: Boolean = false,
    val isSnoozed: Boolean = false,
    val snoozedUntilEpochMillis: Long? = null,
    val priority: ReminderPriority? = ReminderPriority.NORMAL,
    val dueDateEpochMillis: Long? = null,
    val repeatType: ReminderRepeatType? = ReminderRepeatType.NONE,
    val repeatDaysOfWeek: Set<Int> = emptySet(),
    val respectBlockedTimes: BlockedTimeBehavior = BlockedTimeBehavior.RESPECT_GLOBAL,
    val customAllowedDaysOfWeek: Set<Int> = emptySet(),
    val customAllowedStartMinutes: Int? = null,
    val customAllowedEndMinutes: Int? = null,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)
