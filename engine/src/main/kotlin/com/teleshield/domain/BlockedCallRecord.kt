package com.teleshield.domain

data class BlockedCallRecord(
    val id: String,
    val callerIdentifier: String,
    val timestamp: Long,
    val matchedRuleId: String,
    val matchedPatternSnapshot: String,
    val matchedLabelSnapshot: String,
)
