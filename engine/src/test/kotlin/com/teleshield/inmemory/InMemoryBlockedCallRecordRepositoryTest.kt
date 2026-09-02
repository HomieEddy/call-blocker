package com.teleshield.inmemory

import com.teleshield.domain.BlockedCallRecord
import kotlin.test.Test
import kotlin.test.assertEquals

class InMemoryBlockedCallRecordRepositoryTest {

    private val repository = InMemoryBlockedCallRecordRepository()

    @Test
    fun `save stores a record and returns its id`() {
        val saved = record("1", timestamp = 100L)

        assertEquals("1", repository.save(saved))
        assertEquals(listOf(saved), repository.getAllRecords(limit = 10, offset = 0))
    }

    @Test
    fun `getAllRecords respects offset and limit`() {
        repository.save(record("1", timestamp = 1L))
        repository.save(record("2", timestamp = 2L))
        repository.save(record("3", timestamp = 3L))

        assertEquals(listOf("2", "3"), repository.getAllRecords(limit = 2, offset = 1).map { it.id })
        assertEquals(emptyList(), repository.getAllRecords(limit = 10, offset = 10))
    }

    @Test
    fun `delete removes a record`() {
        repository.save(record("1", timestamp = 1L))

        assertEquals(true, repository.delete("1"))
        assertEquals(emptyList(), repository.getAllRecords(limit = 10, offset = 0))
        assertEquals(false, repository.delete("1"))
    }

    @Test
    fun `purgeOlderThan removes only records older than the cutoff`() {
        repository.save(record("old", timestamp = 100L))
        repository.save(record("new", timestamp = 500L))

        val purged = repository.purgeOlderThan(cutoffTimestamp = 200L)

        assertEquals(1, purged)
        assertEquals(listOf("new"), repository.getAllRecords(limit = 10, offset = 0).map { it.id })
    }

    private fun record(id: String, timestamp: Long) = BlockedCallRecord(
        id = id,
        callerIdentifier = "15551234567",
        timestamp = timestamp,
        matchedRuleId = "r1",
        matchedPatternSnapshot = "15551234567",
        matchedLabelSnapshot = "label",
    )
}
