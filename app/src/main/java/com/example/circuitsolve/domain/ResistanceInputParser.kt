package com.example.circuitsolve.domain

import java.math.BigDecimal

enum class ResistanceInputError(val message: String) {
    EMPTY("Enter a resistance value."),
    ZERO("Resistance must be greater than zero."),
    NEGATIVE("Resistance cannot be negative."),
    MALFORMED("Enter a valid number."),
}

sealed interface ResistanceParseResult {
    data class Valid(val value: BigDecimal) : ResistanceParseResult
    data class Invalid(val error: ResistanceInputError) : ResistanceParseResult
}

object ResistanceInputParser {

    const val MAX_INPUT_LENGTH = 15

    private val numberPattern = Regex("""^-?(\d+\.?\d*|\.\d+)$""")

    /** Safely parses user text. Never throws; never produces NaN or Infinity. */
    fun parse(text: String): ResistanceParseResult {
        val trimmed = text.trim().replace(',', '.')
        if (trimmed.isEmpty()) return ResistanceParseResult.Invalid(ResistanceInputError.EMPTY)
        if (trimmed == "-" || trimmed == "-.") {
            return ResistanceParseResult.Invalid(ResistanceInputError.NEGATIVE)
        }
        if (!numberPattern.matches(trimmed)) {
            return ResistanceParseResult.Invalid(ResistanceInputError.MALFORMED)
        }
        val value = trimmed.toBigDecimalOrNull()
            ?: return ResistanceParseResult.Invalid(ResistanceInputError.MALFORMED)
        return when {
            value.signum() < 0 -> ResistanceParseResult.Invalid(ResistanceInputError.NEGATIVE)
            value.signum() == 0 -> ResistanceParseResult.Invalid(ResistanceInputError.ZERO)
            else -> ResistanceParseResult.Valid(value)
        }
    }

    /**
     * Filters keyboard input: digits, a single decimal separator, and a leading minus
     * (accepted so that a negative entry can be explained rather than silently dropped).
     * Returns null when the edit should be rejected.
     */
    fun sanitize(text: String): String? {
        val normalized = text.replace(',', '.')
        if (normalized.length > MAX_INPUT_LENGTH) return null
        normalized.forEachIndexed { index, c ->
            val allowed = c.isDigit() || c == '.' || (c == '-' && index == 0)
            if (!allowed) return null
        }
        if (normalized.count { it == '.' } > 1) return null
        return normalized
    }
}
