package com.teleshield.app.data

import com.teleshield.app.data.db.ScreeningRuleDao
import com.teleshield.app.data.mapper.ScreeningRuleMapper
import com.teleshield.domain.ScreeningRule
import com.teleshield.ports.ScreeningRuleRepository
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

class RoomScreeningRuleRepository @Inject constructor(
    private val dao: ScreeningRuleDao,
) : ScreeningRuleRepository {

    override fun findActiveRules(): List<ScreeningRule> =
        runBlocking { dao.findActive().map(ScreeningRuleMapper::toDomain) }

    override fun findWhitelistRules(): List<ScreeningRule> =
        runBlocking { dao.findWhitelist().map(ScreeningRuleMapper::toDomain) }

    override fun findAll(): List<ScreeningRule> =
        runBlocking { dao.findAll().map(ScreeningRuleMapper::toDomain) }

    override fun findById(id: String): ScreeningRule? =
        runBlocking { dao.findById(id)?.let(ScreeningRuleMapper::toDomain) }

    override fun save(rule: ScreeningRule): String {
        runBlocking { dao.insert(ScreeningRuleMapper.toEntity(rule)) }
        return rule.id
    }

    override fun delete(id: String): Boolean = runBlocking { dao.deleteById(id) } > 0

    override fun incrementTriggerCount(id: String, timestamp: Long) {
        runBlocking { dao.incrementTriggerCount(id, timestamp) }
    }
}
