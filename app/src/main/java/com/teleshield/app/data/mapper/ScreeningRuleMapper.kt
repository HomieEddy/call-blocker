package com.teleshield.app.data.mapper

import com.teleshield.app.data.db.ScreeningRuleEntity
import com.teleshield.domain.PatternExpression
import com.teleshield.domain.RuleType
import com.teleshield.domain.ScreeningRule

object ScreeningRuleMapper {

    fun toDomain(entity: ScreeningRuleEntity): ScreeningRule = ScreeningRule(
        id = entity.id,
        pattern = PatternExpression(entity.expression),
        label = entity.label,
        ruleType = RuleType.valueOf(entity.ruleType),
        isWhitelist = entity.isWhitelist,
        isEnabled = entity.isEnabled,
        timesTriggered = entity.timesTriggered,
        createdAt = entity.createdAt,
        lastTriggeredAt = entity.lastTriggeredAt,
    )

    fun toEntity(rule: ScreeningRule): ScreeningRuleEntity = ScreeningRuleEntity(
        id = rule.id,
        expression = rule.pattern.expression,
        ruleType = rule.ruleType.name,
        label = rule.label,
        isWhitelist = rule.isWhitelist,
        isEnabled = rule.isEnabled,
        timesTriggered = rule.timesTriggered,
        createdAt = rule.createdAt,
        lastTriggeredAt = rule.lastTriggeredAt,
    )
}
