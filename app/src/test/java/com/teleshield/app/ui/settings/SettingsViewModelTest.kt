package com.teleshield.app.ui.settings

import com.teleshield.domain.ScreeningConfiguration
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
class SettingsViewModelTest {

    private val configRepo = InMemorySystemConfigurationRepository(
        ScreeningConfiguration(masterScreeningEnabled = true, blockUnknownEnabled = false, logRetentionDays = 30),
    )
    private val testDispatcher = StandardTestDispatcher()

    @Before fun setup() { Dispatchers.setMain(testDispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test
    fun `loads the configuration on init`() = runTest(testDispatcher) {
        val vm = SettingsViewModel(configRepo, ioDispatcher = testDispatcher)
        advanceUntilIdle()

        assertEquals(ScreeningConfiguration(true, false, 30), vm.uiState.value.config)
    }

    @Test
    fun `setMasterEnabled persists and updates state`() = runTest(testDispatcher) {
        val vm = SettingsViewModel(configRepo, ioDispatcher = testDispatcher)
        advanceUntilIdle()

        vm.setMasterEnabled(false)
        advanceUntilIdle()

        assertEquals(false, vm.uiState.value.config!!.masterScreeningEnabled)
        assertEquals(false, configRepo.load().masterScreeningEnabled)
    }

    @Test
    fun `setRetention persists`() = runTest(testDispatcher) {
        val vm = SettingsViewModel(configRepo, ioDispatcher = testDispatcher)
        advanceUntilIdle()

        vm.setRetention(90)
        advanceUntilIdle()

        assertEquals(90, vm.uiState.value.config!!.logRetentionDays)
        assertEquals(90, configRepo.load().logRetentionDays)
    }
}
