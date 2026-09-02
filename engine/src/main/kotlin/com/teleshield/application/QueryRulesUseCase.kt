package com.teleshield.application

import com.teleshield.domain.ScreeningRule
import com.teleshield.ports.ScreeningRuleRepository

class QueryRulesUseCase(
    private val ruleRepository: ScreeningRuleRepository,
) {
    fun execute(): List<ScreeningRule> = ruleRepository.findAll()
}
