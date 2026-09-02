package com.teleshield.app.screening

import android.telecom.Call
import android.telecom.CallScreeningService
import com.teleshield.app.data.CachingScreeningRuleRepository
import com.teleshield.app.data.CachingSystemConfigurationRepository
import com.teleshield.domain.CallerIdentifier
import com.teleshield.domain.IdentifierNormalizer
import com.teleshield.domain.ScreeningEngine
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TeleShieldCallScreeningService : CallScreeningService() {

    @Inject lateinit var engine: ScreeningEngine
    @Inject lateinit var normalizer: IdentifierNormalizer
    @Inject lateinit var ruleRepository: CachingScreeningRuleRepository
    @Inject lateinit var configurationRepository: CachingSystemConfigurationRepository

    override fun onScreenCall(callDetails: Call.Details) {
        val number = CallNumberExtractor.extract(callDetails.handle)
        val config = configurationRepository.snapshot()
        val caller = CallerIdentifier.from(number, normalizer)
        val verdict = engine.screen(
            caller = caller,
            rules = ruleRepository.snapshot(),
            masterScreeningEnabled = config.masterScreeningEnabled,
            blockUnknownEnabled = config.blockUnknownEnabled,
        )
        val action = ScreeningActionMapper.toAction(verdict)
        respondToCall(callDetails, callResponseFor(action))
    }

    private fun callResponseFor(action: ScreeningAction): CallScreeningService.CallResponse = when (action) {
        ScreeningAction.ALLOW -> CallScreeningService.CallResponse.Builder().setDisallowCall(false).build()
        ScreeningAction.REJECT -> CallScreeningService.CallResponse.Builder().setRejectCall(true).build()
    }
}
