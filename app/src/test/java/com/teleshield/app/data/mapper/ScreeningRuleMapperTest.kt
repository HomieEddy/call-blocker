package com.teleshield.app.data.mapper

import com.teleshield.domain.PatternExpression
import com.teleshield.domain.RuleType
import com.teleshield.domain.ScreeningRule
import org.junit.Test
import kotlin.test.assertEquals

class ScreeningRuleMapperTest {

    @Test
    fun `round-trips a screening rule`() {
        val rule = ScreeningRule(
            id = "r1",
            pattern = PatternExpression("1555*"),
            label = "Exchange block",
            ruleType = RuleType.WILDCARD,
            isWhitelist = false,
            isEnabled = true,
            timesTriggered = 3,
            createdAt = 1000L,
            lastTriggeredAt = 2000L,
        )

        val roundTripped = ScreeningRuleMapper.toDomain(ScreeningRuleMapper.toEntity(rule))

        assertEquals(rule.id, roundTripped.id)
        assertEquals(rule.pattern.expression, roundTripped.pattern.expression)
        assertEquals(rule.ruleType, roundTripped.ruleType)
        assertEquals(rule.label, roundTripped.label)
        assertEquals(rule.isWhitelist, roundTripped.isWhitelist)
        assertEquals(rule.isEnabled, roundTripped.isEnabled)
        assertEquals(rule.timesTriggered, roundTripped.timesTriggered)
        assertEquals(rule.createdAt, roundTripped.createdAt)
        assertEquals(rule.lastTriggeredAt, roundTripped.lastTriggeredAt)
    }
}
