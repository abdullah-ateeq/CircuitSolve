package com.example.circuitsolve.domain

import java.math.BigDecimal

data class EvaluatedResistor(
    val id: Long,
    val name: String,
    val valueText: String,
    val unit: ResistanceUnit,
    val value: BigDecimal?,
    val error: ResistanceInputError?,
) {
    val ohms: BigDecimal? get() = value?.let { resistanceToOhms(it, unit) }
    val displayValue: String? get() = value?.let { formatInputValue(it, unit) }
    val compactValue: String? get() = value?.let { formatCompact(it, unit) }
}

/** Fully validated snapshot of the calculator, ready for display. */
data class CircuitEvaluation(
    val mode: CircuitMode,
    val resistors: List<EvaluatedResistor>,
    val result: CircuitCalculationResult?,
) {
    val isValid: Boolean get() = result != null

    /** First validation problem, prefixed with the resistor name, e.g. "R3: Enter a resistance value." */
    val firstError: String?
        get() = resistors.firstOrNull { it.error != null }?.let { "${it.name}: ${it.error!!.message}" }

    val operator: String get() = if (mode == CircuitMode.SERIES) " + " else " || "

    /** "100 Ω + 220 Ω + 330 Ω" or "100 Ω || 100 Ω". Invalid entries appear as "?". */
    val expression: String
        get() = resistors.joinToString(operator) { it.displayValue ?: "?" }

    fun formattedResult(decimals: Int): FormattedResistance? =
        result?.let { formatResistanceParts(it.equivalentResistanceOhms, decimals) }

    /** Formula using the actual resistor names, e.g. "Rtotal = R1 + R2 + R3". */
    val namedFormula: String
        get() = formulaFor(mode, resistors.map { it.name })

    fun summary(decimals: Int): String? {
        val formatted = formattedResult(decimals) ?: return null
        return buildString {
            appendLine(mode.label)
            resistors.forEach { appendLine("${it.name} = ${it.displayValue}") }
            append("Equivalent = ${formatted.text}")
        }
    }

    companion object {
        fun evaluate(mode: CircuitMode, inputs: List<ResistorInput>): CircuitEvaluation {
            val evaluated = inputs.mapIndexed { index, input ->
                when (val parsed = ResistanceInputParser.parse(input.valueText)) {
                    is ResistanceParseResult.Valid ->
                        EvaluatedResistor(input.id, resistorName(index), input.valueText, input.unit, parsed.value, null)
                    is ResistanceParseResult.Invalid ->
                        EvaluatedResistor(input.id, resistorName(index), input.valueText, input.unit, null, parsed.error)
                }
            }
            val ohms = evaluated.map { it.ohms }
            val result = if (ohms.size >= MIN_RESISTORS && ohms.all { it != null }) {
                CircuitSolveCalculator.calculate(mode, ohms.filterNotNull())
            } else {
                null
            }
            return CircuitEvaluation(mode, evaluated, result)
        }

        /** General textbook formula for a mode. */
        fun generalFormula(mode: CircuitMode): String = when (mode) {
            CircuitMode.SERIES -> "Rtotal = R1 + R2 + … + Rn"
            CircuitMode.PARALLEL -> "1/Rtotal = 1/R1 + 1/R2 + … + 1/Rn"
        }

        fun formulaFor(mode: CircuitMode, names: List<String>): String = when (mode) {
            CircuitMode.SERIES -> "Rtotal = " + names.joinToString(" + ")
            CircuitMode.PARALLEL -> "1/Rtotal = " + names.joinToString(" + ") { "1/$it" }
        }
    }
}
