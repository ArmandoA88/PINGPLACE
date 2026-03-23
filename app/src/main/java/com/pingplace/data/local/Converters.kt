package com.pingplace.data.local

import androidx.room.TypeConverter
import com.pingplace.model.BlockedTimeBehavior
import com.pingplace.model.PlaceSearchMode
import com.pingplace.model.ReminderPriority
import com.pingplace.model.ReminderRepeatType
import com.pingplace.model.TriggerType
import com.pingplace.model.UnitsSystem

class Converters {

    @TypeConverter
    fun fromTriggerType(value: TriggerType?): String? = value?.name

    @TypeConverter
    fun toTriggerType(value: String?): TriggerType? = value?.let(TriggerType::valueOf)

    @TypeConverter
    fun fromPriority(value: ReminderPriority?): String? = value?.name

    @TypeConverter
    fun toPriority(value: String?): ReminderPriority? = value?.let(ReminderPriority::valueOf)

    @TypeConverter
    fun fromRepeatType(value: ReminderRepeatType?): String? = value?.name

    @TypeConverter
    fun toRepeatType(value: String?): ReminderRepeatType? = value?.let(ReminderRepeatType::valueOf)

    @TypeConverter
    fun fromBlockedTimeBehavior(value: BlockedTimeBehavior?): String? = value?.name

    @TypeConverter
    fun toBlockedTimeBehavior(value: String?): BlockedTimeBehavior? =
        value?.let(BlockedTimeBehavior::valueOf)

    @TypeConverter
    fun fromUnits(value: UnitsSystem?): String? = value?.name

    @TypeConverter
    fun toUnits(value: String?): UnitsSystem? = value?.let(UnitsSystem::valueOf)

    @TypeConverter
    fun fromPlaceSearchMode(value: PlaceSearchMode?): String? = value?.name

    @TypeConverter
    fun toPlaceSearchMode(value: String?): PlaceSearchMode? = value?.let(PlaceSearchMode::valueOf)

    @TypeConverter
    fun fromStringList(value: List<String>?): String? = value?.joinToString("\n")

    @TypeConverter
    fun toStringList(value: String?): List<String> =
        value?.takeIf { it.isNotBlank() }?.split("\n") ?: emptyList()

    @TypeConverter
    fun fromIntSet(value: Set<Int>?): String? = value?.sorted()?.joinToString(",")

    @TypeConverter
    fun toIntSet(value: String?): Set<Int> =
        value?.takeIf { it.isNotBlank() }
            ?.split(",")
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet()
            ?: emptySet()
}
