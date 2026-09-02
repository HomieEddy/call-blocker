package com.teleshield.application

import com.teleshield.domain.PatternExpression
import com.teleshield.domain.RuleType
import com.teleshield.domain.ScreeningRule
import com.teleshield.ports.ScreeningRuleRepository

class AddRuleUseCase(
    private val ruleRepository: ScreeningRuleRepository,
    private val idGenerator: () -> String = { java.util.UUID.randomUUID().toString() },
) {

    data class AddRuleRequest(
        val patternExpression: String,
        val ruleType: RuleType,
        val label: String,
        val isWhitelist: Boolean,
        val isEnabled: Boolean = true,
        val id: String? = null,
    )

    fun execute(request: AddRuleRequest): ScreeningRule {
        val rule = ScreeningRule(
            id = request.id ?: idGenerator(),
            pattern = PatternExpression(request.patternExpression),
            label = request.label,
            ruleType = request.ruleType,
            isWhitelist = request.isWhitelist,
            isEnabled = request.isEnabled,
        )
        ruleRepository.save(rule)
        return rule
    }
}
