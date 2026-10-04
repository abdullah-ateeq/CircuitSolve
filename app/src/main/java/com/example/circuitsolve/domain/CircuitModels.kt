package com.example.circuitsolve.domain

import java.math.BigDecimal

enum class CircuitMode {
    SERIES,
    PARALLEL;

    val label: String
        get() = when (this) {
            SERIES -> "Series"
            PARALLEL -> "Parallel"
        }
}

/**
 * Supported resistance units. [ohms] is the exact number of Ohms in one unit.
 */
enum class ResistanceUnit(
    val symbol: String,
    val spokenName: String,
    val ohms: BigDecimal,
) {
    MILLIOHM("mΩ", "milliohms", BigDecimal("0.001")),
    OHM("Ω", "ohms", BigDecimal.ONE),
    KILOOHM("kΩ", "kilohms", BigDecimal("1000")),
    MEGAOHM("MΩ", "megohms", BigDecimal("1000000")),
    GIGAOHM("GΩ", "gigohms", BigDecimal("1000000000")),
}

/**
 * A resistor as entered by the user. [valueText] is kept as raw text so that
 * partially typed or invalid input can be shown and validated without loss.
 */
data class ResistorInput(
    val id: Long,
    val valueText: String,
    val unit: ResistanceUnit,
)

data class CircuitCalculationResult(
    val mode: CircuitMode,
    val resistorsOhms: List<BigDecimal>,
    val equivalentResistanceOhms: BigDecimal,
)

const val MIN_RESISTORS = 2
const val MAX_RESISTORS = 10

fun resistorName(index: Int): String = "R${index + 1}"
