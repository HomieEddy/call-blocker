package com.teleshield.inmemory

import com.teleshield.domain.ScreeningConfiguration
import com.teleshield.ports.SystemConfigurationRepository

class InMemorySystemConfigurationRepository(
    initial: ScreeningConfiguration,
) : SystemConfigurationRepository {

    private var configuration: ScreeningConfiguration = initial

    override fun load(): ScreeningConfiguration = configuration

    override fun save(configuration: ScreeningConfiguration) {
        this.configuration = configuration
    }
}
