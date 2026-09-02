package com.teleshield.app.screening

import com.teleshield.domain.PatternExpression
import com.teleshield.domain.RuleType
import com.teleshield.domain.ScreeningRule
import com.teleshield.domain.ScreeningVerdict
import org.junit.Test
import kotlin.test.assertEquals

class ScreeningActionMapperTest {

    @Test
    fun `blocked maps to REJECT`() {
        assertEquals(ScreeningAction.REJECT, ScreeningActionMapper.toAction(ScreeningVerdict.Blocked(rule(), 0)))
    }

    @Test
    fun `whitelisted maps to ALLOW`() {
        assertEquals(ScreeningAction.ALLOW, ScreeningActionMapper.toAction(ScreeningVerdict.Whitelisted(rule())))
    }

    @Test
    fun `allowed maps to ALLOW`() {
        assertEquals(ScreeningAction.ALLOW, ScreeningActionMapper.toAction(ScreeningVerdict.Allowed("No matching rules")))
    }

    private fun rule() = ScreeningRule(
        id = "r1",
        pattern = PatternExpression("15551234567"),
        label = "label",
        ruleType = RuleType.EXACT,
        isWhitelist = false,
        isEnabled = true,
    )
}
