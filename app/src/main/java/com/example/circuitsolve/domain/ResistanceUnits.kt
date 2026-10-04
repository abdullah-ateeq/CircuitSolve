package com.example.circuitsolve.domain

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/** Precision used for every division in the app (34 significant digits). */
val CALCULATION_CONTEXT: MathContext = MathContext.DECIMAL128

/** Converts a value expressed in [unit] into Ohms. Exact: no rounding is applied. */
fun resistanceToOhms(value: BigDecimal, unit: ResistanceUnit): BigDecimal =
    value.multiply(unit.ohms)

/** Converts a value in Ohms into [unit]. */
fun ohmsToDisplay(ohms: BigDecimal, unit: ResistanceUnit): BigDecimal =
    ohms.divide(unit.ohms, CALCULATION_CONTEXT)

/** Picks the most readable unit for a value in Ohms (1 ≤ scaled value < 1000 where possible). */
fun bestUnitFor(ohms: BigDecimal): ResistanceUnit {
    val magnitude = ohms.abs()
    return ResistanceUnit.entries.lastOrNull { magnitude >= it.ohms } ?: ResistanceUnit.MILLIOHM
}

data class FormattedResistance(
    val number: String,
    val unit: ResistanceUnit,
) {
    val text: String get() = "$number ${unit.symbol}"
    val spoken: String get() = "${number.replace(",", "")} ${unit.spokenName}"
}

/**
 * Formats a resistance in Ohms with automatic unit scaling and a fixed number of
 * decimal places, e.g. 4700 Ω → "4.70 kΩ", 0.5 Ω → "500.00 mΩ".
 * Values too small to show at [decimals] places fall back to 3 significant digits
 * so a non-zero resistance is never displayed as zero.
 */
fun formatResistanceParts(ohms: BigDecimal, decimals: Int = 2): FormattedResistance {
    val places = decimals.coerceIn(0, 6)
    var unit = bestUnitFor(ohms)
    var scaled = ohmsToDisplay(ohms, unit).setScale(places, RoundingMode.HALF_UP)
    // Rounding can carry over into the next unit (999.999 Ω → 1000.00 Ω → 1.00 kΩ).
    val next = ResistanceUnit.entries.getOrNull(unit.ordinal + 1)
    if (next != null && scaled.abs() >= THOUSAND) {
        unit = next
        scaled = ohmsToDisplay(ohms, unit).setScale(places, RoundingMode.HALF_UP)
    }
    if (scaled.signum() == 0 && ohms.signum() != 0) {
        val significant = ohmsToDisplay(ohms, unit).round(MathContext(3, RoundingMode.HALF_UP))
        return FormattedResistance(significant.stripTrailingZeros().toPlainString(), unit)
    }
    return FormattedResistance(groupThousands(scaled.toPlainString()), unit)
}

fun formatResistance(ohms: BigDecimal, decimals: Int = 2): String =
    formatResistanceParts(ohms, decimals).text

/** Formats a user-entered value exactly as typed in its own unit, e.g. "4.7 kΩ". */
fun formatInputValue(value: BigDecimal, unit: ResistanceUnit): String =
    "${plainNumber(value)} ${unit.symbol}"

/** Compact form without a space, used inside the circuit diagram, e.g. "4.7kΩ". */
fun formatCompact(value: BigDecimal, unit: ResistanceUnit): String =
    "${plainNumber(value)}${unit.symbol}"

fun plainNumber(value: BigDecimal): String =
    if (value.signum() == 0) "0" else value.stripTrailingZeros().toPlainString()

private val THOUSAND = BigDecimal("1000")

private fun groupThousands(plain: String): String {
    val negative = plain.startsWith("-")
    val unsigned = plain.removePrefix("-")
    val integerPart = unsigned.substringBefore('.')
    val fraction = unsigned.substringAfter('.', missingDelimiterValue = "")
    val grouped = integerPart.reversed().chunked(3).joinToString(",").reversed()
    val sign = if (negative) "-" else ""
    return if (fraction.isEmpty()) "$sign$grouped" else "$sign$grouped.$fraction"
}
