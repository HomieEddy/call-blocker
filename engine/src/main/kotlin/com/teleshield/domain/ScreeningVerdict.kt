package com.teleshield.domain

sealed class ScreeningVerdict {
    data class Allowed(val reason: String) : ScreeningVerdict()
    data class Whitelisted(val rule: ScreeningRule) : ScreeningVerdict()
    data class Blocked(val matchedRule: ScreeningRule, val executionDurationMs: Long) : ScreeningVerdict()
}
