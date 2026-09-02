package com.teleshield.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BlockedCallRecordDao {

    @Query("SELECT * FROM blocked_call_records ORDER BY timestamp ASC LIMIT :limit OFFSET :offset")
    suspend fun getAll(limit: Int, offset: Int): List<BlockedCallRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BlockedCallRecordEntity)

    @Query("DELETE FROM blocked_call_records WHERE id = :id")
    suspend fun deleteById(id: String): Int

    @Query("DELETE FROM blocked_call_records WHERE timestamp < :cutoff")
    suspend fun purgeOlderThan(cutoff: Long): Int
}
