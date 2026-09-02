package com.teleshield.domain

data class ScreeningRule(
    val id: String,
    val pattern: PatternExpression,
    val label: String,
    val ruleType: RuleType,
    val isWhitelist: Boolean,
    val isEnabled: Boolean,
    val timesTriggered: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastTriggeredAt: Long? = null,
) {

    init {
        if (ruleType != RuleType.UNKNOWN_PRIVATE && pattern.expression.isBlank()) {
            throw IllegalArgumentException("Pattern must not be empty for $ruleType")
        }
        if (!pattern.isValid(ruleType)) {
            throw IllegalArgumentException("Invalid $ruleType pattern: ${pattern.expression}")
        }
    }

    fun incrementTriggered(at: Long): ScreeningRule = copy(
        timesTriggered = timesTriggered + 1,
        lastTriggeredAt = at,
    )
}
