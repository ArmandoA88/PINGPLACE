package com.pingplace.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.pingplace.data.local.dao.BlockedTimeWindowDao
import com.pingplace.data.local.dao.BrandVisitStateDao
import com.pingplace.data.local.dao.OfflinePlaceDao
import com.pingplace.data.local.dao.OfflineRegionDao
import com.pingplace.data.local.dao.ReminderDao
import com.pingplace.data.local.dao.UserSettingsDao
import com.pingplace.data.local.entity.BlockedTimeWindowEntity
import com.pingplace.data.local.entity.BrandVisitStateEntity
import com.pingplace.data.local.entity.OfflinePlaceEntity
import com.pingplace.data.local.entity.OfflineRegionEntity
import com.pingplace.data.local.entity.ReminderEntity
import com.pingplace.data.local.entity.UserSettingsEntity

@Database(
    entities = [
        ReminderEntity::class,
        BlockedTimeWindowEntity::class,
        UserSettingsEntity::class,
        BrandVisitStateEntity::class,
        OfflineRegionEntity::class,
        OfflinePlaceEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PingPlaceDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun blockedTimeWindowDao(): BlockedTimeWindowDao
    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun brandVisitStateDao(): BrandVisitStateDao
    abstract fun offlineRegionDao(): OfflineRegionDao
    abstract fun offlinePlaceDao(): OfflinePlaceDao
}
