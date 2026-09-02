package com.teleshield.app.data.mapper

import com.teleshield.domain.BlockedCallRecord
import org.junit.Test
import kotlin.test.assertEquals

class BlockedCallRecordMapperTest {

    @Test
    fun `round-trips a blocked call record`() {
        val record = BlockedCallRecord(
            id = "c1",
            callerIdentifier = "15551234567",
            timestamp = 42L,
            matchedRuleId = "r1",
            matchedPatternSnapshot = "1555*",
            matchedLabelSnapshot = "Exchange block",
        )

        val roundTripped = BlockedCallRecordMapper.toDomain(BlockedCallRecordMapper.toEntity(record))

        assertEquals(record, roundTripped)
    }
}
