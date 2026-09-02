package com.teleshield.application

import com.teleshield.ports.BlockedCallRecordRepository
import com.teleshield.ports.SystemConfigurationRepository

class PurgeAuditLogsUseCase(
    private val configurationRepository: SystemConfigurationRepository,
    private val blockRepository: BlockedCallRecordRepository,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {

    fun purge(): Int {
        val retentionDays = configurationRepository.load().logRetentionDays
        if (retentionDays <= 0) return 0

        val cutoff = clock() - retentionDays.toLong() * MILLIS_PER_DAY
        return blockRepository.purgeOlderThan(cutoff)
    }

    private companion object {
        const val MILLIS_PER_DAY = 86_400_000L
    }
}
