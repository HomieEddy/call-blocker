package com.teleshield.domain

open class PatternExpression(val expression: String) {

    open fun matches(target: String, type: RuleType): Boolean = when (type) {
        RuleType.EXACT -> target == expression
        RuleType.PREFIX -> target.startsWith(expression)
        RuleType.WILDCARD -> Regex(wildcardRegex(expression)).matches(target)
        RuleType.REGEX -> Regex(expression).matches(target)
        RuleType.UNKNOWN_PRIVATE -> false
    }

    fun isValid(type: RuleType): Boolean = when (type) {
        RuleType.WILDCARD -> runCatching { Regex(wildcardRegex(expression)) }.isSuccess
        RuleType.REGEX -> runCatching { Regex(expression) }.isSuccess
        else -> true
    }

    private fun wildcardRegex(expression: String): String = buildString {
        append('^')
        expression.forEach { ch ->
            when (ch) {
                '*' -> append(".*")
                '?' -> append('.')
                else -> append(Regex.escape(ch.toString()))
            }
        }
        append('$')
    }
}
