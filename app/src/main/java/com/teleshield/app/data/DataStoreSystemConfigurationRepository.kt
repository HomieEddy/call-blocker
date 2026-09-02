package com.teleshield.app.data

import com.teleshield.domain.ScreeningConfiguration
import com.teleshield.ports.SystemConfigurationRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

class DataStoreSystemConfigurationRepository @Inject constructor(
    private val dataSource: ConfigurationDataSource,
) : SystemConfigurationRepository {

    override fun load(): ScreeningConfiguration =
        runBlocking { dataSource.configuration.first() }

    override fun save(configuration: ScreeningConfiguration) {
        runBlocking { dataSource.save(configuration) }
    }
}
