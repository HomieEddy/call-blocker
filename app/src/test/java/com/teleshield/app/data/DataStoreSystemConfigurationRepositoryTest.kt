package com.teleshield.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ApplicationProvider
import com.teleshield.domain.ScreeningConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class DataStoreSystemConfigurationRepositoryTest {

    private lateinit var scope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>

    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val file = File(context.filesDir, "test-${System.nanoTime()}.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
    }

    @After fun teardown() {
        scope.cancel()
    }

    @Test
    fun `load returns defaults when nothing saved`() {
        val repo = DataStoreSystemConfigurationRepository(ConfigurationDataSource(dataStore))

        assertEquals(ScreeningConfiguration(true, false, 30), repo.load())
    }

    @Test
    fun `save then load round-trips`() {
        val repo = DataStoreSystemConfigurationRepository(ConfigurationDataSource(dataStore))
        val config = ScreeningConfiguration(masterScreeningEnabled = false, blockUnknownEnabled = true, logRetentionDays = 90)

        repo.save(config)

        assertEquals(config, repo.load())
    }
}
