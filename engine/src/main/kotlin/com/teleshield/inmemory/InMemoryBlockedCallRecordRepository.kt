package com.teleshield.inmemory

import com.teleshield.domain.BlockedCallRecord
import com.teleshield.ports.BlockedCallRecordRepository

class InMemoryBlockedCallRecordRepository : BlockedCallRecordRepository {

    private val records = mutableListOf<BlockedCallRecord>()

    override fun getAllRecords(limit: Int, offset: Int): List<BlockedCallRecord> {
        if (offset >= records.size) return emptyList()
        val from = offset.coerceAtLeast(0)
        val to = (from + limit).coerceAtMost(records.size)
        return records.subList(from, to).toList()
    }

    override fun save(record: BlockedCallRecord): String {
        records.add(record)
        return record.id
    }

    override fun delete(id: String): Boolean = records.removeIf { it.id == id }

    override fun purgeOlderThan(cutoffTimestamp: Long): Int {
        val before = records.size
        records.removeIf { it.timestamp < cutoffTimestamp }
        return before - records.size
    }
}
