package com.teleshield.ports

import com.teleshield.domain.ScreeningConfiguration

interface SystemConfigurationRepository {
    fun load(): ScreeningConfiguration
    fun save(configuration: ScreeningConfiguration)
}
