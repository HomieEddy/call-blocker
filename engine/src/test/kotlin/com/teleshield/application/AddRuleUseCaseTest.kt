package com.teleshield.application

import com.teleshield.domain.RuleType
import com.teleshield.domain.ScreeningRule
import com.teleshield.ports.ScreeningRuleRepository
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

class AddRuleUseCaseTest {

    @Test
    fun `saves a rule with a generated id and returns it`() {
        val repository = FakeRuleRepository()
        val useCase = AddRuleUseCase(ruleRepository = repository, idGenerator = { "gen-id" })

        val saved = useCase.execute(
            AddRuleUseCase.AddRuleRequest(
                patternExpression = "1555*",
                ruleType = RuleType.WILDCARD,
                label = "Exchange block",
                isWhitelist = false,
            ),
        )

        assertEquals(1, repository.saved.size)
        assertEquals("gen-id", saved.id)
        assertEquals("Exchange block", saved.label)
        assertEquals(RuleType.WILDCARD, saved.ruleType)
        assertEquals("1555*", saved.pattern.expression)
        assertEquals(false, saved.isWhitelist)
        assertEquals(true, saved.isEnabled)
    }

    @Test
    fun `defaults isEnabled to true`() {
        val repository = FakeRuleRepository()
        val useCase = AddRuleUseCase(ruleRepository = repository, idGenerator = { "gen-id" })

        val saved = useCase.execute(
            AddRuleUseCase.AddRuleRequest(
                patternExpression = "15551234567",
                ruleType = RuleType.EXACT,
                label = "Exact",
                isWhitelist = true,
            ),
        )

        assertEquals(true, saved.isEnabled)
    }

    @Test
    fun `uses a provided id instead of generating one`() {
        val repository = FakeRuleRepository()
        val useCase = AddRuleUseCase(ruleRepository = repository, idGenerator = { "gen-id" })

        val saved = useCase.execute(
            AddRuleUseCase.AddRuleRequest(
                patternExpression = "1555*",
                ruleType = RuleType.WILDCARD,
                label = "Exchange block",
                isWhitelist = false,
                id = "existing-id",
            ),
        )

        assertEquals("existing-id", saved.id)
    }

    @Test
    fun `rejects an invalid rule and does not save`() {
        val repository = FakeRuleRepository()
        val useCase = AddRuleUseCase(ruleRepository = repository, idGenerator = { "gen-id" })

        assertThrows<IllegalArgumentException> {
            useCase.execute(
                AddRuleUseCase.AddRuleRequest(
                    patternExpression = "",
                    ruleType = RuleType.EXACT,
                    label = "Empty",
                    isWhitelist = false,
                ),
            )
        }

        assertEquals(0, repository.saved.size)
    }

    private class FakeRuleRepository : ScreeningRuleRepository {
        val saved = mutableListOf<ScreeningRule>()

        override fun findAll(): List<ScreeningRule> = saved.toList()
        override fun findActiveRules(): List<ScreeningRule> = saved.filter { it.isEnabled }
        override fun findWhitelistRules(): List<ScreeningRule> = saved.filter { it.isWhitelist }
        override fun findById(id: String): ScreeningRule? = saved.firstOrNull { it.id == id }
        override fun save(rule: ScreeningRule): String {
            saved.add(rule)
            return rule.id
        }

        override fun delete(id: String): Boolean = saved.removeIf { it.id == id }
        override fun incrementTriggerCount(id: String, timestamp: Long) = Unit
    }
}
