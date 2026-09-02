package com.teleshield.app.data

import com.teleshield.domain.ScreeningConfiguration
import com.teleshield.ports.SystemConfigurationRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CachingSystemConfigurationRepository @Inject constructor(
    private val delegate: SystemConfigurationRepository,
) : SystemConfigurationRepository {

    @Volatile
    private var cached: ScreeningConfiguration? = null

    override fun load(): ScreeningConfiguration =
        cached ?: delegate.load().also { cached = it }

    override fun save(configuration: ScreeningConfiguration) {
        delegate.save(configuration)
        cached = configuration
    }

    fun snapshot(): ScreeningConfiguration = load()
}
