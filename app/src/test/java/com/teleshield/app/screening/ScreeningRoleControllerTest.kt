package com.teleshield.app.screening

import android.content.Intent
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ScreeningRoleControllerTest {

    private class FakeRoleController(
        var held: Boolean = false,
        val intent: Intent = Intent(),
    ) : ScreeningRoleController {
        override fun isRoleHeld(): Boolean = held
        override fun requestRoleIntent(): Intent = intent
    }

    @Test
    fun `isRoleHeld reflects the platform state`() {
        assertFalse(FakeRoleController(held = false).isRoleHeld())
        assertTrue(FakeRoleController(held = true).isRoleHeld())
    }

    @Test
    fun `requestRoleIntent returns a launchable intent`() {
        val intent = Intent("android.intent.action.VIEW")
        assertEquals(intent, FakeRoleController(intent = intent).requestRoleIntent())
    }
}
