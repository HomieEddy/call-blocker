package com.teleshield.inmemory

import com.teleshield.domain.ScreeningConfiguration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class InMemorySystemConfigurationRepositoryTest {

    @Test
    fun `load returns the initial configuration`() {
        val initial = config()

        val repository = InMemorySystemConfigurationRepository(initial)

        assertEquals(initial, repository.load())
    }

    @Test
    fun `save then load round-trips the configuration`() {
        val repository = InMemorySystemConfigurationRepository(config())
        val updated = config(masterOn = false, blockUnknown = true, retention = 90)

        repository.save(updated)

        assertSame(updated, repository.load())
    }

    private fun config(masterOn: Boolean = true, blockUnknown: Boolean = false, retention: Int = 30) =
        ScreeningConfiguration(
            masterScreeningEnabled = masterOn,
            blockUnknownEnabled = blockUnknown,
            logRetentionDays = retention,
        )
}
