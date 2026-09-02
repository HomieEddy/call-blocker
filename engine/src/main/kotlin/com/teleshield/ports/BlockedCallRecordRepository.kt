package com.teleshield.ports

import com.teleshield.domain.BlockedCallRecord

interface BlockedCallRecordRepository {
    fun getAllRecords(limit: Int, offset: Int): List<BlockedCallRecord>
    fun save(record: BlockedCallRecord): String
    fun delete(id: String): Boolean
    fun purgeOlderThan(cutoffTimestamp: Long): Int
}
