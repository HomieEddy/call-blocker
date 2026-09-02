package com.teleshield.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ScreeningRuleEntity::class, BlockedCallRecordEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class TeleShieldDatabase : RoomDatabase() {
    abstract fun screeningRuleDao(): ScreeningRuleDao
    abstract fun blockedCallRecordDao(): BlockedCallRecordDao
}
