package com.teleshield.benchmark

import com.teleshield.domain.CallerIdentifier
import com.teleshield.domain.IdentifierNormalizer
import com.teleshield.domain.PatternExpression
import com.teleshield.domain.RuleType
import com.teleshield.domain.ScreeningEngine
import com.teleshield.domain.ScreeningRule
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.random.Random

@Tag("benchmark")
class ScreeningBenchmarkTest {

    companion object {
        private const val NFR_BUDGET_MS = 15L
        private const val NFR_BUDGET_NANOS = NFR_BUDGET_MS * 1_000_000L
        private const val RULE_COUNT = 400
        private const val CALLER_COUNT = 2000
        private const val ITERATIONS = 20_000
        private const val WARMUP = 5_000
    }

    @Test
    fun `engine screening verdict stays within the 15ms NFR budget`() {
        val normalizer = IdentifierNormalizer()
        val engine = ScreeningEngine(normalizer)
        val rules = buildRules(RULE_COUNT)
        val callers = buildCallers(CALLER_COUNT, normalizer)

        repeat(WARMUP) { engine.screen(callers[it % callers.size], rules, true, false) }

        val samples = LongArray(ITERATIONS)
        var maxNanos = 0L
        repeat(ITERATIONS) { i ->
            val start = System.nanoTime()
            engine.screen(callers[i % callers.size], rules, true, false)
            val nanos = System.nanoTime() - start
            samples[i] = nanos
            if (nanos > maxNanos) maxNanos = nanos
        }

        val sorted = samples.sorted()
        val p50Nanos = sorted[ITERATIONS / 2]
        val p99Nanos = sorted[(ITERATIONS * 99) / 100]
        val avgNanos = samples.sum().toDouble() / ITERATIONS

        println("ScreeningEngine benchmark")
        println("  rules=$RULE_COUNT callers=${callers.size} iterations=$ITERATIONS")
        println("  avg=${"%.1f".format(avgNanos / 1000.0)}us p50=${p50Nanos / 1000}us p99=${p99Nanos / 1000}us max=${maxNanos / 1000}us")
        println("  budget=${NFR_BUDGET_MS}ms (${NFR_BUDGET_NANOS / 1_000_000}ms = ${NFR_BUDGET_NANOS}ns)")

        require(maxNanos <= NFR_BUDGET_NANOS) {
            "NFR-01 violated: max screening decision ${maxNanos / 1_000_000}ms exceeds ${NFR_BUDGET_MS}ms budget"
        }
    }

    private fun buildRules(count: Int): List<ScreeningRule> {
        return (0 until count).map { i ->
            val type = when (i % 4) {
                0 -> RuleType.EXACT
                1 -> RuleType.PREFIX
                2 -> RuleType.WILDCARD
                else -> RuleType.REGEX
            }
            val expression = when (type) {
                RuleType.EXACT -> "1555${i.toString().padStart(6, '0')}"
                RuleType.PREFIX -> "1555${i.toString().padStart(2, '0')}"
                RuleType.WILDCARD -> "1555*${i.toString().padStart(2, '0')}"
                RuleType.REGEX -> "^155\\d{${2 + i % 3}}$"
                RuleType.UNKNOWN_PRIVATE -> ""
            }
            ScreeningRule(
                id = "r$i",
                pattern = PatternExpression(expression),
                label = "rule $i",
                ruleType = type,
                isWhitelist = false,
                isEnabled = true,
            )
        }
    }

    private fun buildCallers(count: Int, normalizer: IdentifierNormalizer): List<CallerIdentifier> {
        val seed = Random(1337)
        return (0 until count).map {
            val digits = (0 until 10).joinToString("") { seed.nextInt(10).toString() }
            CallerIdentifier.from(digits, normalizer)
        }
    }
}
