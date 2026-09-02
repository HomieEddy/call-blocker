package com.teleshield.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class IdentifierNormalizerTest {

    private val normalizer = IdentifierNormalizer()

    @Test
    fun `strips spaces hyphens parentheses and dots`() {
        assertEquals("+15551234567", normalizer.normalize("+1 (555) 123-4567"))
        assertEquals("15551234567", normalizer.normalize("1.555.123.4567"))
    }

    @Test
    fun `preserves a single leading plus`() {
        assertEquals("+15551234567", normalizer.normalize("+1 555 123 4567"))
    }

    @Test
    fun `drops any plus that is not the leading character`() {
        assertEquals("15551234567", normalizer.normalize("1+555+1234567"))
    }

    @Test
    fun `keeps only digits when no leading plus present`() {
        assertEquals("5551234567", normalizer.normalize("(555) 123-4567"))
    }

    @Test
    fun `returns empty for non numeric input`() {
        assertEquals("", normalizer.normalize("No Number"))
    }

    @Test
    fun `returns empty for blank input`() {
        assertEquals("", normalizer.normalize("   "))
    }

    @Test
    fun `keeps leading plus when it is the first non-space character`() {
        assertEquals("+15551234567", normalizer.normalize("  +1 (555) 123-4567"))
    }

    @Test
    fun `isAnonymous is true for blank input`() {
        assertEquals(true, normalizer.isAnonymous("   "))
    }

    @Test
    fun `isAnonymous is true for privacy indicator words`() {
        assertEquals(true, normalizer.isAnonymous("Private"))
        assertEquals(true, normalizer.isAnonymous("UNKNOWN"))
        assertEquals(true, normalizer.isAnonymous("anonymous"))
    }

    @Test
    fun `isAnonymous is true when no digits can be extracted`() {
        assertEquals(true, normalizer.isAnonymous("No Number"))
    }

    @Test
    fun `isAnonymous is false for a dialable number`() {
        assertEquals(false, normalizer.isAnonymous("+1 555 123 4567"))
    }
}
