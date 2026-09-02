package com.teleshield.application

import com.teleshield.domain.BlockedCallRecord
import com.teleshield.ports.BlockedCallRecordRepository
import kotlin.test.Test
import kotlin.test.assertEquals

class QueryBlockedLogsUseCaseTest {

    @Test
    fun `forwards pagination to the repository and returns its records`() {
        val records = listOf(
            record("1"),
            record("2"),
        )
        val repository = FakeLogRepository(records)
        val useCase = QueryBlockedLogsUseCase(repository)

        val result = useCase.execute(limit = 2, offset = 0)

        assertEquals(records, result)
        assertEquals(2, repository.lastLimit)
        assertEquals(0, repository.lastOffset)
    }

    @Test
    fun `returns an empty list when no records exist`() {
        val useCase = QueryBlockedLogsUseCase(FakeLogRepository(emptyList()))

        val result = useCase.execute(limit = 50, offset = 10)

        assertEquals(emptyList(), result)
    }

    private fun record(id: String) = BlockedCallRecord(
        id = id,
        callerIdentifier = "15551234567",
        timestamp = 1L,
        matchedRuleId = "r1",
        matchedPatternSnapshot = "15551234567",
        matchedLabelSnapshot = "label",
    )

    private class FakeLogRepository(private val records: List<BlockedCallRecord>) : BlockedCallRecordRepository {
        var lastLimit: Int = -1
        var lastOffset: Int = -1

        override fun getAllRecords(limit: Int, offset: Int): List<BlockedCallRecord> {
            lastLimit = limit
            lastOffset = offset
            return records
        }

        override fun save(record: BlockedCallRecord): String = record.id
        override fun delete(id: String): Boolean = false
        override fun purgeOlderThan(cutoffTimestamp: Long): Int = 0
    }
}
