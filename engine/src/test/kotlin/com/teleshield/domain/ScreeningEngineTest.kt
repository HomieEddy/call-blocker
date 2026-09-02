package com.teleshield.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScreeningEngineTest {

    private val normalizer = IdentifierNormalizer()
    private val engine = ScreeningEngine(normalizer)

    @Test
    fun `master screening disabled returns Allowed`() {
        val verdict = engine.screen(
            caller("15551234567"),
            rules = listOf(rule(RuleType.EXACT, "15551234567")),
            masterScreeningEnabled = false,
            blockUnknownEnabled = false,
        )
        assertEquals(ScreeningVerdict.Allowed("Master screening disabled"), verdict)
    }

    @Test
    fun `whitelist match overrides an otherwise matching block rule`() {
        val verdict = engine.screen(
            caller("15551234567"),
            rules = listOf(
                rule(RuleType.WILDCARD, "1555*"),
                rule(RuleType.EXACT, "15551234567", isWhitelist = true),
            ),
            masterScreeningEnabled = true,
            blockUnknownEnabled = false,
        )
        assertTrue(verdict is ScreeningVerdict.Whitelisted)
        val matched = (verdict as ScreeningVerdict.Whitelisted).rule
        assertEquals(RuleType.EXACT, matched.ruleType)
        assertEquals(true, matched.isWhitelist)
    }

    @Test
    fun `anonymous caller plus blockUnknown returns Blocked`() {
        val verdict = engine.screen(
            caller("Private"),
            rules = emptyList(),
            masterScreeningEnabled = true,
            blockUnknownEnabled = true,
        )
        assertTrue(verdict is ScreeningVerdict.Blocked)
        assertEquals(RuleType.UNKNOWN_PRIVATE, (verdict as ScreeningVerdict.Blocked).matchedRule.ruleType)
    }

    @Test
    fun `anonymous caller without blockUnknown falls through to block rules`() {
        val verdict = engine.screen(
            caller("Private"),
            rules = listOf(rule(RuleType.EXACT, "15551234567")),
            masterScreeningEnabled = true,
            blockUnknownEnabled = false,
        )
        assertEquals(ScreeningVerdict.Allowed("No matching rules"), verdict)
    }

    @Test
    fun `an exact block rule returns Blocked with the matched rule`() {
        val blocked = rule(RuleType.EXACT, "15551234567")
        val verdict = engine.screen(
            caller("15551234567"),
            rules = listOf(blocked),
            masterScreeningEnabled = true,
            blockUnknownEnabled = false,
        )
        assertEquals(ScreeningVerdict.Blocked(blocked, 0), verdict)
    }

    @Test
    fun `more specific exact rule wins over a broader wildcard rule`() {
        val verdict = engine.screen(
            caller("15551234567"),
            rules = listOf(
                rule(RuleType.WILDCARD, "1555*"),
                rule(RuleType.EXACT, "15551234567"),
            ),
            masterScreeningEnabled = true,
            blockUnknownEnabled = false,
        )
        assertEquals(RuleType.EXACT, (verdict as ScreeningVerdict.Blocked).matchedRule.ruleType)
    }

    @Test
    fun `disabled block rules are ignored`() {
        val verdict = engine.screen(
            caller("15551234567"),
            rules = listOf(rule(RuleType.EXACT, "15551234567", enabled = false)),
            masterScreeningEnabled = true,
            blockUnknownEnabled = false,
        )
        assertEquals(ScreeningVerdict.Allowed("No matching rules"), verdict)
    }

    @Test
    fun `a throwing rule fails open to Allowed`() {
        val throwing = ScreeningRule(
            id = "boom",
            pattern = ThrowingExpression(),
            label = "thrower",
            ruleType = RuleType.EXACT,
            isWhitelist = false,
            isEnabled = true,
        )
        val verdict = engine.screen(
            caller("15551234567"),
            rules = listOf(throwing),
            masterScreeningEnabled = true,
            blockUnknownEnabled = false,
        )
        assertTrue(verdict is ScreeningVerdict.Allowed)
    }

    private class ThrowingExpression : PatternExpression("x") {
        override fun matches(target: String, type: RuleType): Boolean = throw RuntimeException("boom")
    }

    private fun caller(raw: String): CallerIdentifier = CallerIdentifier.from(raw, normalizer)

    private fun rule(
        type: RuleType,
        expression: String,
        isWhitelist: Boolean = false,
        enabled: Boolean = true,
    ): ScreeningRule = ScreeningRule(
        id = expression,
        pattern = PatternExpression(expression),
        label = "label",
        ruleType = type,
        isWhitelist = isWhitelist,
        isEnabled = enabled,
    )
}
