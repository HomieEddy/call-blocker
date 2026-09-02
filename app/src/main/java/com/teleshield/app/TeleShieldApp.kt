package com.teleshield.app

import android.app.Application
import com.teleshield.ports.ScreeningRuleRepository
import com.teleshield.ports.SystemConfigurationRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class TeleShieldApp : Application() {

    @Inject lateinit var ruleRepository: ScreeningRuleRepository
    @Inject lateinit var configurationRepository: SystemConfigurationRepository

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { ruleRepository.findAll() }
            runCatching { configurationRepository.load() }
        }
    }
}
