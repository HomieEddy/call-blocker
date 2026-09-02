package com.teleshield.ports

import com.teleshield.domain.ScreeningRule

interface ScreeningRuleRepository {
    fun findAll(): List<ScreeningRule>
    fun findActiveRules(): List<ScreeningRule>
    fun findWhitelistRules(): List<ScreeningRule>
    fun findById(id: String): ScreeningRule?
    fun save(rule: ScreeningRule): String
    fun delete(id: String): Boolean
    fun incrementTriggerCount(id: String, timestamp: Long)
}
