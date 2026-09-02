package com.teleshield.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.teleshield.app.data.db.TeleShieldDatabase
import com.teleshield.domain.BlockedCallRecord
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class RoomBlockedCallRecordRepositoryTest {

    private lateinit var db: TeleShieldDatabase
    private lateinit var repository: RoomBlockedCallRecordRepository

    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TeleShieldDatabase::class.java).build()
        repository = RoomBlockedCallRecordRepository(db.blockedCallRecordDao())
    }

    @After fun teardown() {
        db.close()
    }

    @Test
    fun `save then getAllRecords returns the record`() {
        repository.save(record("1", 100L))

        assertEquals(listOf("1"), repository.getAllRecords(10, 0).map { it.id })
    }

    @Test
    fun `getAllRecords respects limit and offset`() {
        repository.save(record("1", 1L))
        repository.save(record("2", 2L))
        repository.save(record("3", 3L))

        assertEquals(listOf("2", "3"), repository.getAllRecords(2, 1).map { it.id })
    }

    @Test
    fun `purgeOlderThan removes only older records`() {
        repository.save(record("old", 100L))
        repository.save(record("new", 500L))

        assertEquals(1, repository.purgeOlderThan(200L))
        assertEquals(listOf("new"), repository.getAllRecords(10, 0).map { it.id })
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
