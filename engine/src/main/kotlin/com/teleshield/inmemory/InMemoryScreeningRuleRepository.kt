package com.teleshield.inmemory

import com.teleshield.domain.ScreeningRule
import com.teleshield.ports.ScreeningRuleRepository

class InMemoryScreeningRuleRepository : ScreeningRuleRepository {

    private val store = LinkedHashMap<String, ScreeningRule>()

    override fun findActiveRules(): List<ScreeningRule> = store.values.filter { it.isEnabled }
    override fun findWhitelistRules(): List<ScreeningRule> = store.values.filter { it.isWhitelist }
    override fun findById(id: String): ScreeningRule? = store[id]

    override fun save(rule: ScreeningRule): String {
        store[rule.id] = rule
        return rule.id
    }

    override fun delete(id: String): Boolean = store.remove(id) != null

    override fun incrementTriggerCount(id: String, timestamp: Long) {
        val current = store[id] ?: return
        store[id] = current.incrementTriggered(timestamp)
    }
}
