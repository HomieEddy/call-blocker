package com.teleshield.app.di

import com.teleshield.app.data.CachingScreeningRuleRepository
import com.teleshield.app.data.CachingSystemConfigurationRepository
import com.teleshield.app.data.DataStoreSystemConfigurationRepository
import com.teleshield.app.data.NoOpTelephonyInterceptionPort
import com.teleshield.app.data.RoomBlockedCallRecordRepository
import com.teleshield.app.data.RoomScreeningRuleRepository
import com.teleshield.ports.BlockedCallRecordRepository
import com.teleshield.ports.ScreeningRuleRepository
import com.teleshield.ports.SystemConfigurationRepository
import com.teleshield.ports.TelephonyInterceptionPort
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindBlockedCallRecordRepository(impl: RoomBlockedCallRecordRepository): BlockedCallRecordRepository

    @Binds
    abstract fun bindTelephonyInterceptionPort(impl: NoOpTelephonyInterceptionPort): TelephonyInterceptionPort

    companion object {
        @Provides
        @Singleton
        fun provideCachingScreeningRuleRepository(room: RoomScreeningRuleRepository): ScreeningRuleRepository =
            CachingScreeningRuleRepository(room)

        @Provides
        @Singleton
        fun provideCachingSystemConfigurationRepository(dataStore: DataStoreSystemConfigurationRepository): SystemConfigurationRepository =
            CachingSystemConfigurationRepository(dataStore)
    }
}
