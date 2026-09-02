package com.teleshield.app.screening

import android.telecom.Call
import android.telecom.CallScreeningService
import com.teleshield.domain.CallerIdentifier
import com.teleshield.domain.IdentifierNormalizer
import com.teleshield.domain.ScreeningEngine
import com.teleshield.ports.ScreeningRuleRepository
import com.teleshield.ports.SystemConfigurationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TeleShieldCallScreeningService : CallScreeningService() {

    @Inject lateinit var engine: ScreeningEngine
    @Inject lateinit var normalizer: IdentifierNormalizer
    @Inject lateinit var ruleRepository: ScreeningRuleRepository
    @Inject lateinit var configurationRepository: SystemConfigurationRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            runCatching { ruleRepository.findAll() }
            runCatching { configurationRepository.load() }
        }
    }

    override fun onScreenCall(callDetails: Call.Details) {
        scope.launch {
            val number = CallNumberExtractor.extract(callDetails.handle)
            val config = configurationRepository.load()
            val caller = CallerIdentifier.from(number, normalizer)
            val verdict = engine.screen(
                caller = caller,
                rules = ruleRepository.findAll(),
                masterScreeningEnabled = config.masterScreeningEnabled,
                blockUnknownEnabled = config.blockUnknownEnabled,
            )
            val action = ScreeningActionMapper.toAction(verdict)
            respondToCall(callDetails, callResponseFor(action))
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun callResponseFor(action: ScreeningAction): CallScreeningService.CallResponse = when (action) {
        ScreeningAction.ALLOW -> CallScreeningService.CallResponse.Builder().setDisallowCall(false).build()
        ScreeningAction.REJECT -> CallScreeningService.CallResponse.Builder().setRejectCall(true).build()
    }
}
