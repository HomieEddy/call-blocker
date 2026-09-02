package com.teleshield.domain

class ScreeningEngine(private val normalizer: IdentifierNormalizer) {

    companion object {
        private fun typePriority(type: RuleType): Int = when (type) {
            RuleType.EXACT -> 0
            RuleType.PREFIX -> 1
            RuleType.WILDCARD -> 2
            RuleType.REGEX -> 3
            RuleType.UNKNOWN_PRIVATE -> 4
        }
    }

    fun screen(
        caller: CallerIdentifier,
        rules: List<ScreeningRule>,
        masterScreeningEnabled: Boolean,
        blockUnknownEnabled: Boolean,
    ): ScreeningVerdict {
        if (!masterScreeningEnabled) return ScreeningVerdict.Allowed("Master screening disabled")

        val active = rules.filter { it.isEnabled }

        return try {
            val whitelisted = evaluateWhitelist(active, caller.canonical)
            when {
                whitelisted != null -> ScreeningVerdict.Whitelisted(whitelisted)

                caller.isAnonymous && blockUnknownEnabled ->
                    ScreeningVerdict.Blocked(anonymousRule(active), 0)

                else -> {
                    val matched = evaluateBlock(active, caller.canonical)
                    if (matched != null) ScreeningVerdict.Blocked(matched, 0)
                    else ScreeningVerdict.Allowed("No matching rules")
                }
            }
        } catch (t: Throwable) {
            ScreeningVerdict.Allowed("Rule evaluation failed open: ${t.message}")
        }
    }

    private fun evaluateWhitelist(rules: List<ScreeningRule>, canonical: String): ScreeningRule? =
        rules.firstOrNull { it.isWhitelist && it.pattern.matches(canonical, it.ruleType) }

    private fun evaluateBlock(rules: List<ScreeningRule>, canonical: String): ScreeningRule? =
        rules.filter { !it.isWhitelist }
            .sortedBy { typePriority(it.ruleType) }
            .firstOrNull { it.pattern.matches(canonical, it.ruleType) }

    private fun anonymousRule(rules: List<ScreeningRule>): ScreeningRule =
        rules.firstOrNull { !it.isWhitelist && it.ruleType == RuleType.UNKNOWN_PRIVATE }
            ?: ScreeningRule(
                id = "private",
                pattern = PatternExpression(""),
                label = "Private or unknown caller",
                ruleType = RuleType.UNKNOWN_PRIVATE,
                isWhitelist = false,
                isEnabled = true,
            )
}
