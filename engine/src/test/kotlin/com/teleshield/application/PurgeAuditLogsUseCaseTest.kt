package com.teleshield.application

import com.teleshield.domain.ScreeningConfiguration
import com.teleshield.ports.BlockedCallRecordRepository
import com.teleshield.ports.SystemConfigurationRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class PurgeAuditLogsUseCaseTest {

    @Test
    fun `purges records older than the retention window`() {
        val nowMs = 1_700_000_000_000L
        val logRepository = FakeLogRepository(returned = 4)
        val useCase = PurgeAuditLogsUseCase(
            configurationRepository = FakeConfigRepository(ScreeningConfiguration(
                masterScreeningEnabled = true,
                blockUnknownEnabled = false,
                logRetentionDays = 30,
            )),
            blockRepository = logRepository,
            clock = { nowMs },
        )

        val purged = useCase.purge()

        assertEquals(4, purged)
        assertEquals(nowMs - 30L * 86_400_000L, logRepository.lastCutoff)
    }

    @Test
    fun `zero retention days means never purge`() {
        val logRepository = FakeLogRepository(returned = 0)
        val useCase = PurgeAuditLogsUseCase(
            configurationRepository = FakeConfigRepository(ScreeningConfiguration(
                masterScreeningEnabled = true,
                blockUnknownEnabled = false,
                logRetentionDays = 0,
            )),
            blockRepository = logRepository,
            clock = { 1_700_000_000_000L },
        )

        val purged = useCase.purge()

        assertEquals(0, purged)
        assertEquals(0, logRepository.purgeCalls)
    }

    @Test
    fun `negative retention days means never purge`() {
        val logRepository = FakeLogRepository(returned = 0)
        val useCase = PurgeAuditLogsUseCase(
            configurationRepository = FakeConfigRepository(ScreeningConfiguration(
                masterScreeningEnabled = true,
                blockUnknownEnabled = false,
                logRetentionDays = -1,
            )),
            blockRepository = logRepository,
            clock = { 1_700_000_000_000L },
        )

        val purged = useCase.purge()

        assertEquals(0, purged)
        assertEquals(0, logRepository.purgeCalls)
    }

    private class FakeConfigRepository(private val config: ScreeningConfiguration) : SystemConfigurationRepository {
        override fun load(): ScreeningConfiguration = config
        override fun save(configuration: ScreeningConfiguration) = Unit
    }

    private class FakeLogRepository(private val returned: Int) : BlockedCallRecordRepository {
        var lastCutoff: Long? = null
        var purgeCalls = 0

        override fun getAllRecords(limit: Int, offset: Int): List<com.teleshield.domain.BlockedCallRecord> = emptyList()
        override fun save(record: com.teleshield.domain.BlockedCallRecord): String = record.id
        override fun delete(id: String): Boolean = false
        override fun purgeOlderThan(cutoffTimestamp: Long): Int {
            lastCutoff = cutoffTimestamp
            purgeCalls++
            return returned
        }
    }
}
