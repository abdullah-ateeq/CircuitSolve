package com.example.circuitsolve.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class CircuitSolveCalculatorTest {

    private fun ohms(vararg values: String) = values.map(::BigDecimal)

    /** Numeric comparison: ignores BigDecimal scale (650 vs 650.00). */
    private fun assertNumericEquals(expected: String, actual: BigDecimal) {
        assertTrue("expected $expected but was $actual", BigDecimal(expected).compareTo(actual) == 0)
    }

    private fun assertClose(expected: String, actual: BigDecimal, tolerance: String = "1E-25") {
        val diff = BigDecimal(expected).subtract(actual).abs()
        assertTrue("expected ≈$expected but was $actual", diff <= BigDecimal(tolerance))
    }

    // 1
    @Test
    fun seriesWithTwoResistors() {
        assertNumericEquals("320", CircuitSolveCalculator.calculateSeriesResistance(ohms("100", "220")))
    }

    // 2
    @Test
    fun seriesWithThreeResistors() {
        assertNumericEquals("650", CircuitSolveCalculator.calculateSeriesResistance(ohms("100", "220", "330")))
    }

    @Test
    fun seriesInKiloohms() {
        val values = listOf("1", "2", "3").map { resistanceToOhms(BigDecimal(it), ResistanceUnit.KILOOHM) }
        val total = CircuitSolveCalculator.calculateSeriesResistance(values)
        assertNumericEquals("6000", total)
        assertEquals("6.00 kΩ", formatResistance(total))
    }

    // 3
    @Test
    fun seriesWithTenResistors() {
        val values = (1..10).map { BigDecimal(it * 10) }
        assertNumericEquals("550", CircuitSolveCalculator.calculateSeriesResistance(values))
    }

    // 4
    @Test
    fun parallelWithTwoEqualResistors() {
        assertNumericEquals("50", CircuitSolveCalculator.calculateParallelResistance(ohms("100", "100")))
    }

    @Test
    fun parallelWithTwoUnequalResistors() {
        val total = CircuitSolveCalculator.calculateParallelResistance(ohms("100", "200"))
        assertClose("66.6666666666666666666666666666667", total)
        assertEquals("66.67 Ω", formatResistance(total, 2))
    }

    // 5
    @Test
    fun parallelWithThreeResistors() {
        val total = CircuitSolveCalculator.calculateParallelResistance(ohms("100", "100", "100"))
        assertClose("33.3333333333333333333333333333333", total)
        assertEquals("33.33 Ω", formatResistance(total, 2))
    }

    @Test
    fun parallelReciprocalMatchesProductOverSum() {
        // 3-resistor path uses Σ(1/R): 1/(1/100 + 1/150 + 1/300) = 300/6 = 50 Ω.
        assertClose("50", CircuitSolveCalculator.calculateParallelResistance(ohms("100", "150", "300")))
        // 1/(1/100 + 1/220 + 1/330) = 6600/116 Ω
        val expected = BigDecimal("6600").divide(BigDecimal("116"), CALCULATION_CONTEXT)
        assertClose(expected.toPlainString(), CircuitSolveCalculator.calculateParallelResistance(ohms("100", "220", "330")))
    }

    // 6
    @Test
    fun parallelWithTenResistors() {
        val total = CircuitSolveCalculator.calculateParallelResistance(List(10) { BigDecimal("1000") })
        assertClose("100", total)
        assertEquals("100.00 Ω", formatResistance(total))
    }

    @Test
    fun parallelIsAlwaysBelowSmallestResistor() {
        val values = ohms("4.7", "10000", "220", "1")
        assertTrue(CircuitSolveCalculator.calculateParallelResistance(values) < BigDecimal.ONE)
    }

    @Test(expected = IllegalArgumentException::class)
    fun parallelRejectsZero() {
        CircuitSolveCalculator.calculateParallelResistance(ohms("100", "0"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun seriesRequiresTwoResistors() {
        CircuitSolveCalculator.calculateSeriesResistance(ohms("100"))
    }

    // 12
    @Test
    fun decimalResistance() {
        assertNumericEquals("6.2", CircuitSolveCalculator.calculateSeriesResistance(ohms("1.5", "4.7")))
        val parsed = ResistanceInputParser.parse("4.7")
        assertEquals(ResistanceParseResult.Valid(BigDecimal("4.7")), parsed)
        assertEquals(ResistanceParseResult.Valid(BigDecimal("0.1")), ResistanceInputParser.parse(".1"))
        assertEquals(ResistanceParseResult.Valid(BigDecimal("2.5")), ResistanceInputParser.parse("2,5"))
    }

    // 16
    @Test
    fun verySmallResistance() {
        val tiny = resistanceToOhms(BigDecimal("0.000001"), ResistanceUnit.MILLIOHM) // 1 nΩ
        val series = CircuitSolveCalculator.calculateSeriesResistance(listOf(tiny, tiny))
        val parallel = CircuitSolveCalculator.calculateParallelResistance(listOf(tiny, tiny))
        assertNumericEquals("0.000000002", series)
        assertNumericEquals("0.0000000005", parallel)
        // Never collapses to zero on display.
        assertEquals("0.000002 mΩ", formatResistance(series, 2))
    }

    // 17
    @Test
    fun veryLargeResistance() {
        val huge = resistanceToOhms(BigDecimal("999999999999"), ResistanceUnit.GIGAOHM)
        val values = List(10) { huge }
        val series = CircuitSolveCalculator.calculateSeriesResistance(values)
        val parallel = CircuitSolveCalculator.calculateParallelResistance(values)
        assertNumericEquals("9999999999990000000000", series)
        assertClose("99999999999900000000", parallel, tolerance = "1E-6")
        val text = formatResistance(series)
        assertEquals("9,999,999,999,990.00 GΩ", text)
        assertTrue(!text.contains("E") && !text.contains("Infinity") && !text.contains("NaN"))
    }

    @Test
    fun evaluationProducesNoResultForInvalidInput() {
        val inputs = listOf(
            ResistorInput(1, "100", ResistanceUnit.OHM),
            ResistorInput(2, "", ResistanceUnit.OHM),
        )
        val evaluation = CircuitEvaluation.evaluate(CircuitMode.PARALLEL, inputs)
        assertNull(evaluation.result)
        assertEquals("R2: Enter a resistance value.", evaluation.firstError)
        assertNull(evaluation.formattedResult(2))
    }

    @Test
    fun evaluationBuildsExpressionAndSummary() {
        val inputs = listOf("100", "220", "330").mapIndexed { i, v -> ResistorInput(i.toLong(), v, ResistanceUnit.OHM) }
        val evaluation = CircuitEvaluation.evaluate(CircuitMode.SERIES, inputs)
        assertEquals("100 Ω + 220 Ω + 330 Ω", evaluation.expression)
        assertEquals("Rtotal = R1 + R2 + R3", evaluation.namedFormula)
        assertEquals(
            "Series\nR1 = 100 Ω\nR2 = 220 Ω\nR3 = 330 Ω\nEquivalent = 650.00 Ω",
            evaluation.summary(2),
        )
        val parallel = CircuitEvaluation.evaluate(CircuitMode.PARALLEL, inputs.take(2))
        assertEquals("100 Ω || 220 Ω", parallel.expression)
        assertEquals("1/Rtotal = 1/R1 + 1/R2", parallel.namedFormula)
    }

    @Test
    fun evaluationMixesUnits() {
        val inputs = listOf(
            ResistorInput(1, "1", ResistanceUnit.KILOOHM),
            ResistorInput(2, "500", ResistanceUnit.OHM),
        )
        val evaluation = CircuitEvaluation.evaluate(CircuitMode.SERIES, inputs)
        assertEquals("1.50 kΩ", evaluation.formattedResult(2)?.text)
    }
}
