package com.teleshield.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ScreeningRuleDao {

    @Query("SELECT * FROM screening_rules")
    suspend fun findAll(): List<ScreeningRuleEntity>

    @Query("SELECT * FROM screening_rules WHERE isEnabled = 1")
    suspend fun findActive(): List<ScreeningRuleEntity>

    @Query("SELECT * FROM screening_rules WHERE isWhitelist = 1")
    suspend fun findWhitelist(): List<ScreeningRuleEntity>

    @Query("SELECT * FROM screening_rules WHERE id = :id")
    suspend fun findById(id: String): ScreeningRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ScreeningRuleEntity)

    @Query("DELETE FROM screening_rules WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("UPDATE screening_rules SET timesTriggered = timesTriggered + 1, lastTriggeredAt = :timestamp WHERE id = :id")
    suspend fun incrementTriggerCount(id: String, timestamp: Long)
}
