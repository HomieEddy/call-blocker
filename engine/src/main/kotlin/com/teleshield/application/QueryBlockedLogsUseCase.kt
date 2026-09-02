package com.teleshield.application

import com.teleshield.domain.BlockedCallRecord
import com.teleshield.ports.BlockedCallRecordRepository

class QueryBlockedLogsUseCase(
    private val blockRepository: BlockedCallRecordRepository,
) {

    fun execute(limit: Int, offset: Int): List<BlockedCallRecord> =
        blockRepository.getAllRecords(limit = limit, offset = offset)
}
