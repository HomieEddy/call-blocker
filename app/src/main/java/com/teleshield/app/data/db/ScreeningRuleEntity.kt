package com.teleshield.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "screening_rules")
data class ScreeningRuleEntity(
    @PrimaryKey val id: String,
    val expression: String,
    val ruleType: String,
    val label: String,
    val isWhitelist: Boolean,
    val isEnabled: Boolean,
    val timesTriggered: Int,
    val createdAt: Long,
    val lastTriggeredAt: Long?,
)
