package com.teleshield.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_call_records")
data class BlockedCallRecordEntity(
    @PrimaryKey val id: String,
    val callerIdentifier: String,
    val timestamp: Long,
    val matchedRuleId: String,
    val matchedPatternSnapshot: String,
    val matchedLabelSnapshot: String,
)
