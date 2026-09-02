package com.teleshield.app.data

import com.teleshield.domain.ScreeningConfiguration
import com.teleshield.ports.SystemConfigurationRepository
import org.junit.Test
import kotlin.test.assertEquals

class CachingSystemConfigurationRepositoryTest {

    @Test
    fun `load caches the delegate value`() {
        val delegate = FakeConfig(ScreeningConfiguration(true, false, 30))
        val cache = CachingSystemConfigurationRepository(delegate)

        assertEquals(ScreeningConfiguration(true, false, 30), cache.load())
        cache.load()
        assertEquals(1, delegate.loadCalls)
    }

    @Test
    fun `save forwards and updates the snapshot`() {
        val delegate = FakeConfig(ScreeningConfiguration(true, false, 30))
        val cache = CachingSystemConfigurationRepository(delegate)
        val updated = ScreeningConfiguration(false, true, 90)

        cache.save(updated)

        assertEquals(updated, cache.snapshot())
        assertEquals(updated, delegate.saved)
    }

    private class FakeConfig(var config: ScreeningConfiguration) : SystemConfigurationRepository {
        var loadCalls = 0
        var saved: ScreeningConfiguration? = null
        override fun load(): ScreeningConfiguration {
            loadCalls++
            return config
        }
        override fun save(configuration: ScreeningConfiguration) {
            config = configuration
            saved = configuration
        }
    }
}
