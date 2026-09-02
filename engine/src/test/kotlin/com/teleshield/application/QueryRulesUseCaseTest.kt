package com.teleshield.application

import com.teleshield.domain.RuleType
import com.teleshield.domain.PatternExpression
import com.teleshield.domain.ScreeningRule
import com.teleshield.ports.ScreeningRuleRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class QueryRulesUseCaseTest {

    @Test
    fun `returns all rules from the repository`() {
        val rules = listOf(rule("a", enabled = true), rule("b", enabled = false))
        val useCase = QueryRulesUseCase(FakeRepo(rules))

        assertEquals(rules, useCase.execute())
    }

    @Test
    fun `returns empty when repository is empty`() {
        assertEquals(emptyList(), QueryRulesUseCase(FakeRepo(emptyList())).execute())
    }

    private fun rule(id: String, enabled: Boolean) = ScreeningRule(
        id = id, pattern = PatternExpression("15551234567"), label = "l",
        ruleType = RuleType.EXACT, isWhitelist = false, isEnabled = enabled,
    )

    private class FakeRepo(private val rules: List<ScreeningRule>) : ScreeningRuleRepository {
        override fun findActiveRules() = rules.filter { it.isEnabled }
        override fun findWhitelistRules() = rules.filter { it.isWhitelist }
        override fun findAll() = rules
        override fun findById(id: String) = rules.firstOrNull { it.id == id }
        override fun save(rule: ScreeningRule): String = rule.id
        override fun delete(id: String): Boolean = false
        override fun incrementTriggerCount(id: String, timestamp: Long) = Unit
    }
}
