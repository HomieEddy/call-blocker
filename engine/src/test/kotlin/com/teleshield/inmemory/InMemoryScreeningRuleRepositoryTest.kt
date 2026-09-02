package com.teleshield.inmemory

import com.teleshield.domain.RuleType
import com.teleshield.domain.PatternExpression
import com.teleshield.domain.ScreeningRule
import kotlin.test.Test
import kotlin.test.assertEquals

class InMemoryScreeningRuleRepositoryTest {

    private val repository = InMemoryScreeningRuleRepository()

    @Test
    fun `save then findById returns the rule`() {
        val saved = rule("r1", enabled = true)
        repository.save(saved)

        assertEquals(saved, repository.findById("r1"))
    }

    @Test
    fun `saving an existing id overwrites it`() {
        repository.save(rule("r1", enabled = true))
        val updated = ScreeningRule(
            id = "r1",
            pattern = PatternExpression("555*"),
            label = "updated",
            ruleType = RuleType.PREFIX,
            isWhitelist = false,
            isEnabled = true,
        )

        repository.save(updated)

        assertEquals(updated, repository.findById("r1"))
    }

    @Test
    fun `findActiveRules returns only enabled rules`() {
        repository.save(rule("on", enabled = true))
        repository.save(rule("off", enabled = false))

        val active = repository.findActiveRules()

        assertEquals(listOf("on"), active.map { it.id })
    }

    @Test
    fun `findWhitelistRules returns only whitelist rules`() {
        repository.save(rule("w", enabled = true, isWhitelist = true))
        repository.save(rule("b", enabled = true, isWhitelist = false))

        val whitelist = repository.findWhitelistRules()

        assertEquals(listOf("w"), whitelist.map { it.id })
    }

    @Test
    fun `delete removes a rule and reports success`() {
        repository.save(rule("r1", enabled = true))

        assertEquals(true, repository.delete("r1"))
        assertEquals(null, repository.findById("r1"))
        assertEquals(false, repository.delete("r1"))
    }

    @Test
    fun `incrementTriggerCount bumps the counter and sets lastTriggeredAt`() {
        repository.save(rule("r1", enabled = true))

        repository.incrementTriggerCount("r1", timestamp = 99L)

        val updated = repository.findById("r1")!!
        assertEquals(1, updated.timesTriggered)
        assertEquals(99L, updated.lastTriggeredAt)
    }

    private fun rule(id: String, enabled: Boolean, isWhitelist: Boolean = false): ScreeningRule = ScreeningRule(
        id = id,
        pattern = PatternExpression("15551234567"),
        label = "label",
        ruleType = RuleType.EXACT,
        isWhitelist = isWhitelist,
        isEnabled = enabled,
    )
}
