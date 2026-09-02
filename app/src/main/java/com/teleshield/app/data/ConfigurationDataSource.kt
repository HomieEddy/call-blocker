package com.teleshield.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.teleshield.domain.ScreeningConfiguration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ConfigurationDataSource(
    private val dataStore: DataStore<Preferences>,
) {

    val configuration: Flow<ScreeningConfiguration> = dataStore.data.map { prefs ->
        ScreeningConfiguration(
            masterScreeningEnabled = prefs[KEY_MASTER] ?: DEFAULT_MASTER,
            blockUnknownEnabled = prefs[KEY_BLOCK_UNKNOWN] ?: DEFAULT_BLOCK_UNKNOWN,
            logRetentionDays = prefs[KEY_RETENTION] ?: DEFAULT_RETENTION,
        )
    }

    suspend fun save(config: ScreeningConfiguration) {
        dataStore.edit { prefs ->
            prefs[KEY_MASTER] = config.masterScreeningEnabled
            prefs[KEY_BLOCK_UNKNOWN] = config.blockUnknownEnabled
            prefs[KEY_RETENTION] = config.logRetentionDays
        }
    }

    companion object {
        private val KEY_MASTER = booleanPreferencesKey("master_screening_enabled")
        private val KEY_BLOCK_UNKNOWN = booleanPreferencesKey("block_unknown_enabled")
        private val KEY_RETENTION = intPreferencesKey("log_retention_days")
        private const val DEFAULT_MASTER = true
        private const val DEFAULT_BLOCK_UNKNOWN = false
        private const val DEFAULT_RETENTION = 30
    }
}
