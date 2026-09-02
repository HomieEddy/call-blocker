package com.teleshield.app.data

import com.teleshield.domain.ScreeningRule
import com.teleshield.ports.ScreeningRuleRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CachingScreeningRuleRepository @Inject constructor(
    private val delegate: ScreeningRuleRepository,
) : ScreeningRuleRepository {

    @Volatile
    private var cached: List<ScreeningRule>? = null

    private fun snapshotList(): List<ScreeningRule> =
        cached ?: delegate.findAll().also { cached = it }

    override fun findAll(): List<ScreeningRule> = snapshotList()
    override fun findActiveRules(): List<ScreeningRule> = snapshotList().filter { it.isEnabled }
    override fun findWhitelistRules(): List<ScreeningRule> = snapshotList().filter { it.isWhitelist }
    override fun findById(id: String): ScreeningRule? = snapshotList().firstOrNull { it.id == id }

    override fun save(rule: ScreeningRule): String {
        val id = delegate.save(rule)
        cached = delegate.findAll()
        return id
    }

    override fun delete(id: String): Boolean {
        val deleted = delegate.delete(id)
        cached = delegate.findAll()
        return deleted
    }

    override fun incrementTriggerCount(id: String, timestamp: Long) {
        delegate.incrementTriggerCount(id, timestamp)
    }

    fun snapshot(): List<ScreeningRule> = snapshotList()
}
