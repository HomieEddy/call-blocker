package com.teleshield.app.data

import com.teleshield.domain.RuleType
import com.teleshield.domain.PatternExpression
import com.teleshield.domain.ScreeningRule
import com.teleshield.ports.ScreeningRuleRepository
import org.junit.Test
import kotlin.test.assertEquals

class CachingScreeningRuleRepositoryTest {

    @Test
    fun `serves reads from an in-memory snapshot`() {
        val delegate = FakeRepo(mutableListOf(rule("r1")))
        val cache = CachingScreeningRuleRepository(delegate)

        assertEquals(listOf("r1"), cache.findAll().map { it.id })
        cache.findAll()
        assertEquals(1, delegate.findAllCalls) // served from memory after first load
        assertEquals(listOf("r1"), cache.snapshot().map { it.id })
    }

    @Test
    fun `save forwards then refreshes the snapshot`() {
        val delegate = FakeRepo(mutableListOf(rule("r1")))
        val cache = CachingScreeningRuleRepository(delegate)
        cache.findAll()

        cache.save(rule("r2"))

        assertEquals(setOf("r1", "r2"), cache.snapshot().map { it.id }.toSet())
        assertEquals(true, delegate.contains("r2"))
    }

    @Test
    fun `delete forwards then refreshes the snapshot`() {
        val delegate = FakeRepo(mutableListOf(rule("r1")))
        val cache = CachingScreeningRuleRepository(delegate)
        cache.findAll()

        cache.delete("r1")

        assertEquals(emptyList(), cache.snapshot())
    }

    @Test
    fun `findActiveRules filters the snapshot`() {
        val delegate = FakeRepo(mutableListOf(rule("on", enabled = true), rule("off", enabled = false)))
        val cache = CachingScreeningRuleRepository(delegate)

        assertEquals(listOf("on"), cache.findActiveRules().map { it.id })
    }

    private fun rule(id: String, enabled: Boolean = true) = ScreeningRule(
        id = id,
        pattern = PatternExpression("15551234567"),
        label = "label",
        ruleType = RuleType.EXACT,
        isWhitelist = false,
        isEnabled = enabled,
    )

    private class FakeRepo(private val rules: MutableList<ScreeningRule>) : ScreeningRuleRepository {
        var findAllCalls = 0
        fun contains(id: String) = rules.any { it.id == id }

        override fun findActiveRules() = rules.filter { it.isEnabled }
        override fun findWhitelistRules() = rules.filter { it.isWhitelist }
        override fun findAll(): List<ScreeningRule> {
            findAllCalls++
            return rules.toList()
        }
        override fun findById(id: String) = rules.firstOrNull { it.id == id }
        override fun save(rule: ScreeningRule): String {
            rules.removeIf { it.id == rule.id }
            rules.add(rule)
            return rule.id
        }
        override fun delete(id: String): Boolean = rules.removeIf { it.id == id }
        override fun incrementTriggerCount(id: String, timestamp: Long) = Unit
    }
}
