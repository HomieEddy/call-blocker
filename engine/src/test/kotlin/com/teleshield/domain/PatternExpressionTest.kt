package com.teleshield.domain

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals

class PatternExpressionTest {

    @ParameterizedTest
    @MethodSource("matchingCases")
    fun `matches according to its rule type`(case: Case) {
        val expression = PatternExpression(case.expression)
        assertEquals(case.expected, expression.matches(case.target, case.type))
    }

    data class Case(val type: RuleType, val expression: String, val target: String, val expected: Boolean)

    companion object {
        @JvmStatic
        fun matchingCases(): List<Case> = listOf(
            Case(RuleType.EXACT, "15551234567", "15551234567", true),
            Case(RuleType.EXACT, "15551234567", "15551234568", false),
            Case(RuleType.PREFIX, "1555", "15551234567", true),
            Case(RuleType.PREFIX, "1555", "15561234567", false),
            Case(RuleType.WILDCARD, "1555*4567", "15551234567", true),
            Case(RuleType.WILDCARD, "1555?4567", "155524567", true),
            Case(RuleType.WILDCARD, "1555?4567", "15551234567", false),
            Case(RuleType.REGEX, "^1555\\d{4}$", "15551234", true),
            Case(RuleType.REGEX, "^1555\\d{4}$", "155512345", false),
        )
    }
}
