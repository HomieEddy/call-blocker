package com.teleshield.application

import com.teleshield.domain.IdentifierNormalizer
import com.teleshield.domain.RuleType
import com.teleshield.domain.PatternExpression
import com.teleshield.domain.ScreeningConfiguration
import com.teleshield.domain.ScreeningEngine
import com.teleshield.domain.ScreeningRule
import com.teleshield.domain.ScreeningVerdict
import com.teleshield.ports.ScreeningRuleRepository
import com.teleshield.ports.SystemConfigurationRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SimulateCallUseCaseTest {

    private val normalizer = IdentifierNormalizer()
    private val engine = ScreeningEngine(normalizer)

    @Test
    fun `simulating a blocked call returns the verdict and non-negative duration`() {
        val useCase = SimulateCallUseCase(
            engine = engine,
            normalizer = normalizer,
            ruleRepository = FakeRuleRepository(mutableListOf(exactRule("b1", "15551234567"))),
            configurationRepository = FakeConfigRepository(config(masterOn = true)),
        )

        val result = useCase.simulate("15551234567")

        assertTrue(result.verdict is ScreeningVerdict.Blocked)
        assertTrue(result.executionDurationMs >= 0)
    }

    @Test
    fun `simulating an allowed call returns Allowed`() {
        val useCase = SimulateCallUseCase(
            engine = engine,
            normalizer = normalizer,
            ruleRepository = FakeRuleRepository(mutableListOf(exactRule("b1", "15551234567"))),
            configurationRepository = FakeConfigRepository(config(masterOn = true)),
        )

        val result = useCase.simulate("9999999999")

        assertEquals(ScreeningVerdict.Allowed("No matching rules"), result.verdict)
    }

    @Test
    fun `simulation performs no counter increments`() {
        val ruleRepository = FakeRuleRepository(mutableListOf(exactRule("b1", "15551234567")))
        val useCase = SimulateCallUseCase(
            engine = engine,
            normalizer = normalizer,
            ruleRepository = ruleRepository,
            configurationRepository = FakeConfigRepository(config(masterOn = true)),
        )

        useCase.simulate("15551234567")

        assertEquals(0, ruleRepository.incrementedCallCount)
    }

    private fun config(masterOn: Boolean) = ScreeningConfiguration(
        masterScreeningEnabled = masterOn,
        blockUnknownEnabled = false,
        logRetentionDays = 30,
    )

    private fun exactRule(id: String, expression: String): ScreeningRule = ScreeningRule(
        id = id,
        pattern = PatternExpression(expression),
        label = "label",
        ruleType = RuleType.EXACT,
        isWhitelist = false,
        isEnabled = true,
    )

    private class FakeRuleRepository(private val rules: MutableList<ScreeningRule>) : ScreeningRuleRepository {
        var incrementedCallCount = 0

        override fun findActiveRules(): List<ScreeningRule> = rules.filter { it.isEnabled }
        override fun findWhitelistRules(): List<ScreeningRule> = rules.filter { it.isWhitelist }
        override fun findById(id: String): ScreeningRule? = rules.firstOrNull { it.id == id }
        override fun save(rule: ScreeningRule): String {
            rules.add(rule)
            return rule.id
        }

        override fun delete(id: String): Boolean = rules.removeIf { it.id == id }
        override fun incrementTriggerCount(id: String, timestamp: Long) {
            incrementedCallCount++
        }
    }

    private class FakeConfigRepository(private val config: ScreeningConfiguration) : SystemConfigurationRepository {
        override fun load(): ScreeningConfiguration = config
        override fun save(configuration: ScreeningConfiguration) = Unit
    }
}
