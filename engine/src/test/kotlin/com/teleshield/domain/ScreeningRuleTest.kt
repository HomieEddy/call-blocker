package com.teleshield.domain

import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.Test

class ScreeningRuleTest {

    @Test
    fun `exact rule requires a non-empty pattern`() {
        assertThrows<IllegalArgumentException> {
            rule(ruleType = RuleType.EXACT, expression = "")
        }
    }

    @Test
    fun `regex rule requires a compilable pattern`() {
        assertThrows<IllegalArgumentException> {
            rule(ruleType = RuleType.REGEX, expression = "[invalid")
        }
    }

    @Test
    fun `unknown private rule allows an empty pattern`() {
        val rule = rule(ruleType = RuleType.UNKNOWN_PRIVATE, expression = "")
        assertEquals(RuleType.UNKNOWN_PRIVATE, rule.ruleType)
    }

    @Test
    fun `incrementTriggered increments the counter and sets lastTriggeredAt`() {
        val rule = rule(ruleType = RuleType.EXACT, expression = "15551234567")
        assertNull(rule.lastTriggeredAt)
        assertEquals(0, rule.timesTriggered)

        val incremented = rule.incrementTriggered(at = 42L)
        assertEquals(1, incremented.timesTriggered)
        assertEquals(42L, incremented.lastTriggeredAt)
    }

    private fun rule(ruleType: RuleType, expression: String): ScreeningRule =
        ScreeningRule(
            id = "r1",
            pattern = PatternExpression(expression),
            label = "label",
            ruleType = ruleType,
            isWhitelist = false,
            isEnabled = true,
        )
}
