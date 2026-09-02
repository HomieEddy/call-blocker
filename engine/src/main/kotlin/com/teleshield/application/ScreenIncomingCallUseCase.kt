package com.teleshield.application

import com.teleshield.domain.BlockedCallRecord
import com.teleshield.domain.CallerIdentifier
import com.teleshield.domain.IdentifierNormalizer
import com.teleshield.domain.ScreeningEngine
import com.teleshield.domain.ScreeningVerdict
import com.teleshield.ports.BlockedCallRecordRepository
import com.teleshield.ports.ScreeningRuleRepository
import com.teleshield.ports.SystemConfigurationRepository
import com.teleshield.ports.TelephonyInterceptionPort

class ScreenIncomingCallUseCase(
    private val engine: ScreeningEngine,
    private val normalizer: IdentifierNormalizer,
    private val ruleRepository: ScreeningRuleRepository,
    private val blockRepository: BlockedCallRecordRepository,
    private val configurationRepository: SystemConfigurationRepository,
    private val telephonyPort: TelephonyInterceptionPort,
) {

    fun execute(callerId: String): ScreeningVerdict {
        val config = configurationRepository.load()
        val rules = ruleRepository.findActiveRules()
        val caller = CallerIdentifier.from(callerId, normalizer)
        val verdict = engine.screen(
            caller = caller,
            rules = rules,
            masterScreeningEnabled = config.masterScreeningEnabled,
            blockUnknownEnabled = config.blockUnknownEnabled,
        )

        if (verdict is ScreeningVerdict.Blocked) {
            val now = System.currentTimeMillis()
            val matched = verdict.matchedRule
            ruleRepository.incrementTriggerCount(matched.id, now)
            blockRepository.save(
                BlockedCallRecord(
                    id = now.toString(),
                    callerIdentifier = callerId,
                    timestamp = now,
                    matchedRuleId = matched.id,
                    matchedPatternSnapshot = matched.pattern.expression,
                    matchedLabelSnapshot = matched.label,
                ),
            )
            telephonyPort.reject()
        }

        return verdict
    }
}
