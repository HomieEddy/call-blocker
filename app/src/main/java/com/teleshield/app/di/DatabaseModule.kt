package com.teleshield.app.di

import android.content.Context
import androidx.room.Room
import com.teleshield.app.data.db.BlockedCallRecordDao
import com.teleshield.app.data.db.ScreeningRuleDao
import com.teleshield.app.data.db.TeleShieldDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TeleShieldDatabase =
        Room.databaseBuilder(context, TeleShieldDatabase::class.java, "teleshield.db").build()

    @Provides
    fun provideScreeningRuleDao(db: TeleShieldDatabase): ScreeningRuleDao = db.screeningRuleDao()

    @Provides
    fun provideBlockedCallRecordDao(db: TeleShieldDatabase): BlockedCallRecordDao = db.blockedCallRecordDao()
}
