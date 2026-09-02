package com.teleshield.app.ui.simulator

import com.teleshield.application.SimulateCallUseCase
import com.teleshield.domain.IdentifierNormalizer
import com.teleshield.domain.RuleType
import com.teleshield.domain.PatternExpression
import com.teleshield.domain.ScreeningConfiguration
import com.teleshield.domain.ScreeningEngine
import com.teleshield.domain.ScreeningRule
import com.teleshield.domain.ScreeningVerdict
import com.teleshield.inmemory.InMemoryScreeningRuleRepository
import com.teleshield.inmemory.InMemorySystemConfigurationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SimulatorViewModelTest {

    private val normalizer = IdentifierNormalizer()
    private val engine = ScreeningEngine(normalizer)
    private val rulesRepo = InMemoryScreeningRuleRepository()
    private val configRepo = InMemorySystemConfigurationRepository(
        ScreeningConfiguration(masterScreeningEnabled = true, blockUnknownEnabled = false, logRetentionDays = 30),
    )
    private val simulate = SimulateCallUseCase(engine, normalizer, rulesRepo, configRepo)
    private val testDispatcher = StandardTestDispatcher()

    @Before fun setup() { Dispatchers.setMain(testDispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test
    fun `simulating a blocked number returns a Blocked verdict`() = runTest(testDispatcher) {
        rulesRepo.save(rule("r1", "15551234567"))
        val vm = SimulatorViewModel(simulate, ioDispatcher = testDispatcher)

        vm.simulate("15551234567")
        advanceUntilIdle()

        val result = vm.uiState.value.result!!
        assertTrue(result.verdict is ScreeningVerdict.Blocked)
        assertTrue(result.executionDurationMs >= 0)
    }

    @Test
    fun `simulating an allowed number returns Allowed`() = runTest(testDispatcher) {
        rulesRepo.save(rule("r1", "15551234567"))
        val vm = SimulatorViewModel(simulate, ioDispatcher = testDispatcher)

        vm.simulate("9999999999")
        advanceUntilIdle()

        assertEquals(ScreeningVerdict.Allowed("No matching rules"), vm.uiState.value.result!!.verdict)
    }

    private fun rule(id: String, expression: String) = ScreeningRule(
        id = id,
        pattern = PatternExpression(expression),
        label = "label",
        ruleType = RuleType.EXACT,
        isWhitelist = false,
        isEnabled = true,
    )
}
