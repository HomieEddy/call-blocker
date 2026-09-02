package com.teleshield.application

import com.teleshield.ports.ScreeningRuleRepository

class DeleteRuleUseCase(
    private val ruleRepository: ScreeningRuleRepository,
) {
    fun execute(id: String): Boolean = ruleRepository.delete(id)
}
