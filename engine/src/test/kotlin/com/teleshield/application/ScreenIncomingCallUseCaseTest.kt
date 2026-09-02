package com.teleshield.application

import com.teleshield.domain.IdentifierNormalizer
import com.teleshield.domain.RuleType
import com.teleshield.domain.PatternExpression
import com.teleshield.domain.ScreeningConfiguration
import com.teleshield.domain.ScreeningEngine
import com.teleshield.domain.ScreeningRule
import com.teleshield.domain.ScreeningVerdict
import com.teleshield.ports.BlockedCallRecordRepository
import com.teleshield.ports.ScreeningRuleRepository
import com.teleshield.ports.SystemConfigurationRepository
import com.teleshield.ports.TelephonyInterceptionPort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScreenIncomingCallUseCaseTest {

    private val normalizer = IdentifierNormalizer()
    private val engine = ScreeningEngine(normalizer)

    @Test
    fun `blocked call increments the rule counter and appends a log`() {
        val rule = blockRule("b1")
        val config = ScreeningConfiguration(
            masterScreeningEnabled = true,
            blockUnknownEnabled = false,
            logRetentionDays = 30,
        )
        val repository = FakeRuleRepository(mutableListOf(rule))
        val logRepository = FakeLogRepository()
        val telephony = FakeTelephony()

        val useCase = ScreenIncomingCallUseCase(
            engine = engine,
            normalizer = normalizer,
            ruleRepository = repository,
            blockRepository = logRepository,
            configurationRepository = FakeConfigRepository(config),
            telephonyPort = telephony,
        )

        val verdict = useCase.execute("15551234567")

        assertTrue(verdict is ScreeningVerdict.Blocked)
        assertEquals(true, repository.incrementedContains("b1"))
        assertEquals(1, logRepository.savedRecords.size)
        assertEquals("b1", logRepository.savedRecords.first().matchedRuleId)
        assertEquals(true, telephony.rejected)
    }

    @Test
    fun `master disabled returns Allowed without side effects`() {
        val config = ScreeningConfiguration(
            masterScreeningEnabled = false,
            blockUnknownEnabled = false,
            logRetentionDays = 30,
        )
        val repository = FakeRuleRepository(mutableListOf(blockRule("b1")))
        val logRepository = FakeLogRepository()
        val telephony = FakeTelephony()

        val useCase = ScreenIncomingCallUseCase(
            engine = engine,
            normalizer = normalizer,
            ruleRepository = repository,
            blockRepository = logRepository,
            configurationRepository = FakeConfigRepository(config),
            telephonyPort = telephony,
        )

        val verdict = useCase.execute("15551234567")

        assertEquals(ScreeningVerdict.Allowed("Master screening disabled"), verdict)
        assertEquals(0, repository.incremented.size)
        assertEquals(0, logRepository.savedRecords.size)
        assertEquals(false, telephony.rejected)
    }

    @Test
    fun `allowed call has no side effects`() {
        val config = ScreeningConfiguration(
            masterScreeningEnabled = true,
            blockUnknownEnabled = false,
            logRetentionDays = 30,
        )
        val repository = FakeRuleRepository(mutableListOf(blockRule("b1")))
        val logRepository = FakeLogRepository()
        val telephony = FakeTelephony()

        val useCase = ScreenIncomingCallUseCase(
            engine = engine,
            normalizer = normalizer,
            ruleRepository = repository,
            blockRepository = logRepository,
            configurationRepository = FakeConfigRepository(config),
            telephonyPort = telephony,
        )

        val verdict = useCase.execute("9999999999")

        assertEquals(ScreeningVerdict.Allowed("No matching rules"), verdict)
        assertEquals(0, repository.incremented.size)
        assertEquals(0, logRepository.savedRecords.size)
        assertEquals(false, telephony.rejected)
    }

    private fun blockRule(id: String): ScreeningRule = ScreeningRule(
        id = id,
        pattern = PatternExpression("15551234567"),
        label = "label",
        ruleType = RuleType.EXACT,
        isWhitelist = false,
        isEnabled = true,
    )

    private class FakeRuleRepository(private val rules: MutableList<ScreeningRule>) : ScreeningRuleRepository {
        val incremented = mutableListOf<String>()
        fun incrementedContains(id: String): Boolean = incremented.contains(id)

        override fun findActiveRules(): List<ScreeningRule> = rules.filter { it.isEnabled }
        override fun findWhitelistRules(): List<ScreeningRule> = rules.filter { it.isWhitelist }
        override fun findById(id: String): ScreeningRule? = rules.firstOrNull { it.id == id }
        override fun save(rule: ScreeningRule): String {
            rules.add(rule)
            return rule.id
        }

        override fun delete(id: String): Boolean = rules.removeIf { it.id == id }
        override fun incrementTriggerCount(id: String, timestamp: Long) {
            incremented.add(id)
        }
    }

    private class FakeLogRepository : BlockedCallRecordRepository {
        val savedRecords = mutableListOf<com.teleshield.domain.BlockedCallRecord>()
        override fun getAllRecords(limit: Int, offset: Int) = savedRecords.toList()
        override fun save(record: com.teleshield.domain.BlockedCallRecord): String {
            savedRecords.add(record)
            return record.id
        }

        override fun delete(id: String): Boolean = savedRecords.removeIf { it.id == id }
        override fun purgeOlderThan(cutoffTimestamp: Long): Int = 0
    }

    private class FakeConfigRepository(private val config: ScreeningConfiguration) : SystemConfigurationRepository {
        override fun load(): ScreeningConfiguration = config
        override fun save(configuration: ScreeningConfiguration) = Unit
    }

    private class FakeTelephony : TelephonyInterceptionPort {
        var rejected = false
        override fun reject() {
            rejected = true
        }

        override fun suppressNotification() = Unit
        override fun suppressCallLog() = Unit
    }
}
