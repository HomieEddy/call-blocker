package com.teleshield.app.ui.rules

import com.teleshield.application.AddRuleUseCase
import com.teleshield.application.DeleteRuleUseCase
import com.teleshield.application.QueryRulesUseCase
import com.teleshield.domain.RuleType
import com.teleshield.inmemory.InMemoryScreeningRuleRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
class RulesViewModelTest {

    private val repo = InMemoryScreeningRuleRepository()
    private val query = QueryRulesUseCase(repo)
    private val add = AddRuleUseCase(repo, idGenerator = { "id-1" })
    private val delete = DeleteRuleUseCase(repo)
    private val testDispatcher = StandardTestDispatcher()

    @Before fun setup() { Dispatchers.setMain(testDispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test
    fun `starts with an empty list`() = runTest(testDispatcher) {
        val vm = RulesViewModel(query, add, delete, ioDispatcher = testDispatcher)
        advanceUntilIdle()
        assertEquals(emptyList(), vm.uiState.value.rules)
    }

    @Test
    fun `addRule adds a rule and refreshes state`() = runTest(testDispatcher) {
        val vm = RulesViewModel(query, add, delete, ioDispatcher = testDispatcher)
        advanceUntilIdle()

        vm.addRule(AddRuleUseCase.AddRuleRequest("15551234567", RuleType.EXACT, "block", false))
        advanceUntilIdle()

        assertEquals(listOf("id-1"), vm.uiState.value.rules.map { it.id })
    }

    @Test
    fun `deleteRule removes a rule and refreshes state`() = runTest(testDispatcher) {
        val vm = RulesViewModel(query, add, delete, ioDispatcher = testDispatcher)
        advanceUntilIdle()

        vm.addRule(AddRuleUseCase.AddRuleRequest("15551234567", RuleType.EXACT, "block", false))
        advanceUntilIdle()
        vm.deleteRule("id-1")
        advanceUntilIdle()

        assertEquals(emptyList(), vm.uiState.value.rules)
    }
}
