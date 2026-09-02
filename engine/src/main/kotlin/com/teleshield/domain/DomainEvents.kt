package com.teleshield.domain

data class CallScreenedEvent(
    val callerIdentifier: String,
    val verdict: ScreeningVerdict,
    val timestamp: Long,
    val durationMs: Long,
)

data class CallBlockedEvent(
    val callerIdentifier: String,
    val matchedRuleId: String,
    val timestamp: Long,
)
