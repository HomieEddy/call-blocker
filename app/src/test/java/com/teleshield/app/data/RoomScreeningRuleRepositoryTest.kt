package com.teleshield.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.teleshield.app.data.db.TeleShieldDatabase
import com.teleshield.domain.PatternExpression
import com.teleshield.domain.RuleType
import com.teleshield.domain.ScreeningRule
import androidx.room.Room
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class RoomScreeningRuleRepositoryTest {

    private lateinit var db: TeleShieldDatabase
    private lateinit var repository: RoomScreeningRuleRepository

    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TeleShieldDatabase::class.java).build()
        repository = RoomScreeningRuleRepository(db.screeningRuleDao())
    }

    @After fun teardown() {
        db.close()
    }

    @Test
    fun `save then findAll returns the rule`() {
        repository.save(rule("r1", enabled = true))

        val all = repository.findAll()
        assertEquals(listOf("r1"), all.map { it.id })
        assertEquals("15551234567", all.first().pattern.expression)
    }

    @Test
    fun `findActiveRules filters disabled rules`() {
        repository.save(rule("on", enabled = true))
        repository.save(rule("off", enabled = false))

        assertEquals(listOf("on"), repository.findActiveRules().map { it.id })
    }

    @Test
    fun `delete removes a rule`() {
        repository.save(rule("r1", enabled = true))

        assertEquals(true, repository.delete("r1"))
        assertEquals(emptyList(), repository.findAll())
        assertEquals(false, repository.delete("r1"))
    }

    @Test
    fun `incrementTriggerCount bumps the stored counter`() {
        repository.save(rule("r1", enabled = true))

        repository.incrementTriggerCount("r1", 99L)

        assertEquals(1, repository.findById("r1")!!.timesTriggered)
        assertEquals(99L, repository.findById("r1")!!.lastTriggeredAt)
    }

    private fun rule(id: String, enabled: Boolean) = ScreeningRule(
        id = id,
        pattern = PatternExpression("15551234567"),
        label = "label",
        ruleType = RuleType.EXACT,
        isWhitelist = false,
        isEnabled = enabled,
    )
}
