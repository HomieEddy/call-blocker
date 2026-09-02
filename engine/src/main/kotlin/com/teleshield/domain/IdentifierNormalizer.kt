package com.teleshield.domain

class IdentifierNormalizer {

    companion object {
        private val ANONYMOUS_MARKERS =
            setOf("private", "unknown", "anonymous", "withheld", "hidden", "unavailable", "blocked")
    }

    fun normalize(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""

        val hasLeadingPlus = trimmed.startsWith('+')
        val digits = buildString {
            trimmed.forEach { ch -> if (ch.isDigit()) append(ch) }
        }

        return if (hasLeadingPlus && digits.isNotEmpty()) "+$digits" else digits
    }

    fun isAnonymous(raw: String): Boolean {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return true
        if (trimmed.lowercase() in ANONYMOUS_MARKERS) return true
        return normalize(raw).isEmpty()
    }
}
