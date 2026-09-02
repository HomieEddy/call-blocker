package com.teleshield.app.screening

import android.net.Uri
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class CallNumberExtractorTest {

    @Test
    fun `extracts a tel number`() {
        assertEquals("+15551234567", CallNumberExtractor.extract(Uri.parse("tel:+15551234567")))
    }

    @Test
    fun `returns empty for a null handle`() {
        assertEquals("", CallNumberExtractor.extract(null))
    }

    @Test
    fun `returns empty for a non-tel uri`() {
        assertEquals("", CallNumberExtractor.extract(Uri.parse("urn:anonymous")))
    }
}
