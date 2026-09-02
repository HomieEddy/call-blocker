package com.teleshield.application

import com.teleshield.domain.ScreeningRule
import com.teleshield.ports.ScreeningRuleRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class DeleteRuleUseCaseTest {

    @Test
    fun `delegates delete to the repository and returns its result`() {
        val repo = FakeRepo()
        val useCase = DeleteRuleUseCase(repo)

        assertEquals(true, useCase.execute("r1"))
        assertEquals("r1", repo.deletedId)
        assertEquals(false, useCase.execute("missing"))
    }

    private class FakeRepo : ScreeningRuleRepository {
        var deletedId: String? = null
        override fun findActiveRules(): List<ScreeningRule> = emptyList()
        override fun findWhitelistRules(): List<ScreeningRule> = emptyList()
        override fun findAll(): List<ScreeningRule> = emptyList()
        override fun findById(id: String): ScreeningRule? = null
        override fun save(rule: ScreeningRule): String = rule.id
        override fun delete(id: String): Boolean {
            deletedId = id
            return id == "r1"
        }
        override fun incrementTriggerCount(id: String, timestamp: Long) = Unit
    }
}
