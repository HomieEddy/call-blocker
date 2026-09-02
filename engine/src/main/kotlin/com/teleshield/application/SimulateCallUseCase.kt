package com.teleshield.application

import com.teleshield.domain.CallerIdentifier
import com.teleshield.domain.IdentifierNormalizer
import com.teleshield.domain.ScreeningEngine
import com.teleshield.domain.ScreeningVerdict
import com.teleshield.ports.ScreeningRuleRepository
import com.teleshield.ports.SystemConfigurationRepository

data class SimulationResult(
    val verdict: ScreeningVerdict,
    val executionDurationMs: Long,
)

class SimulateCallUseCase(
    private val engine: ScreeningEngine,
    private val normalizer: IdentifierNormalizer,
    private val ruleRepository: ScreeningRuleRepository,
    private val configurationRepository: SystemConfigurationRepository,
) {

    fun simulate(callerId: String): SimulationResult {
        val config = configurationRepository.load()
        val rules = ruleRepository.findActiveRules()
        val caller = CallerIdentifier.from(callerId, normalizer)

        val start = System.nanoTime()
        val verdict = engine.screen(
            caller = caller,
            rules = rules,
            masterScreeningEnabled = config.masterScreeningEnabled,
            blockUnknownEnabled = config.blockUnknownEnabled,
        )
        val durationMs = (System.nanoTime() - start) / 1_000_000

        return SimulationResult(verdict = verdict, executionDurationMs = durationMs)
    }
}
