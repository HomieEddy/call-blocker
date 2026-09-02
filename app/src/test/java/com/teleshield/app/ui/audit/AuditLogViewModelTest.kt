package com.teleshield.app.ui.audit

import com.teleshield.application.PurgeAuditLogsUseCase
import com.teleshield.application.QueryBlockedLogsUseCase
import com.teleshield.domain.BlockedCallRecord
import com.teleshield.domain.ScreeningConfiguration
import com.teleshield.inmemory.InMemoryBlockedCallRecordRepository
import com.teleshield.inmemory.InMemorySystemConfigurationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class AuditLogViewModelTest {

    private val logRepo = InMemoryBlockedCallRecordRepository()
    private val configRepo = InMemorySystemConfigurationRepository(
        ScreeningConfiguration(masterScreeningEnabled = true, blockUnknownEnabled = false, logRetentionDays = 30),
    )
    private val query = QueryBlockedLogsUseCase(logRepo)
    private val purge = PurgeAuditLogsUseCase(configRepo, logRepo)
    private val testDispatcher = StandardTestDispatcher()

    @Before fun setup() { Dispatchers.setMain(testDispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test
    fun `loads existing records on init`() = runTest(testDispatcher) {
        logRepo.save(record("1", 100L))
        val vm = AuditLogViewModel(query, purge, ioDispatcher = testDispatcher)
        advanceUntilIdle()

        assertEquals(listOf("1"), vm.uiState.value.records.map { it.id })
    }

    @Test
    fun `purge clears the records and refreshes`() = runTest(testDispatcher) {
        logRepo.save(record("1", 100L))
        val vm = AuditLogViewModel(query, purge, ioDispatcher = testDispatcher)
        advanceUntilIdle()

        vm.purge()
        advanceUntilIdle()

        assertEquals(emptyList(), vm.uiState.value.records)
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
