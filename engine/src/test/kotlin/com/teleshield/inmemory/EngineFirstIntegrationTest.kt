package com.teleshield.inmemory

import com.teleshield.application.AddRuleUseCase
import com.teleshield.application.ScreenIncomingCallUseCase
import com.teleshield.domain.IdentifierNormalizer
import com.teleshield.domain.RuleType
import com.teleshield.domain.ScreeningConfiguration
import com.teleshield.domain.ScreeningEngine
import com.teleshield.domain.ScreeningVerdict
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EngineFirstIntegrationTest {

    @Test
    fun `a blocked call flows through use cases and adapters`() {
        val normalizer = IdentifierNormalizer()
        val engine = ScreeningEngine(normalizer)
        val ruleRepository = InMemoryScreeningRuleRepository()
        val configRepository = InMemorySystemConfigurationRepository(
            ScreeningConfiguration(masterScreeningEnabled = true, blockUnknownEnabled = false, logRetentionDays = 30),
        )
        val logRepository = InMemoryBlockedCallRecordRepository()
        val telephony = InMemoryTelephonyInterceptionPort()

        val addRule = AddRuleUseCase(ruleRepository)
        val added = addRule.execute(
            AddRuleUseCase.AddRuleRequest(
                patternExpression = "15551234567",
                ruleType = RuleType.EXACT,
                label = "block exact",
                isWhitelist = false,
            ),
        )

        val screen = ScreenIncomingCallUseCase(engine, normalizer, ruleRepository, logRepository, configRepository, telephony)
        val verdict = screen.execute("15551234567")

        assertTrue(verdict is ScreeningVerdict.Blocked)
        assertEquals(1, ruleRepository.findById(added.id)!!.timesTriggered)
        assertEquals(1, logRepository.getAllRecords(limit = 10, offset = 0).size)
        assertEquals(1, telephony.rejectedCount)
    }
}
