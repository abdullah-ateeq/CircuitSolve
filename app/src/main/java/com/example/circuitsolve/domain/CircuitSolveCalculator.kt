package com.example.circuitsolve.domain

import java.math.BigDecimal

/**
 * Equivalent-resistance engine. All values are in Ohms.
 * Series sums are exact; parallel results use [CALCULATION_CONTEXT] (34 significant digits).
 */
object CircuitSolveCalculator {

    /** Rtotal = R1 + R2 + … + Rn */
    fun calculateSeriesResistance(resistances: List<BigDecimal>): BigDecimal {
        requireValid(resistances)
        return resistances.fold(BigDecimal.ZERO) { total, r -> total.add(r) }
    }

    /** 1 / Rtotal = 1/R1 + 1/R2 + … + 1/Rn */
    fun calculateParallelResistance(resistances: List<BigDecimal>): BigDecimal {
        requireValid(resistances)
        if (resistances.size == 2) {
            // Product-over-sum needs a single division, so it carries less rounding error.
            val (r1, r2) = resistances
            return r1.multiply(r2).divide(r1.add(r2), CALCULATION_CONTEXT)
        }
        val conductance = resistances.fold(BigDecimal.ZERO) { sum, r ->
            sum.add(BigDecimal.ONE.divide(r, CALCULATION_CONTEXT), CALCULATION_CONTEXT)
        }
        return BigDecimal.ONE.divide(conductance, CALCULATION_CONTEXT)
    }

    fun calculate(mode: CircuitMode, resistancesOhms: List<BigDecimal>): CircuitCalculationResult {
        val equivalent = when (mode) {
            CircuitMode.SERIES -> calculateSeriesResistance(resistancesOhms)
            CircuitMode.PARALLEL -> calculateParallelResistance(resistancesOhms)
        }
        return CircuitCalculationResult(mode, resistancesOhms, equivalent)
    }

    private fun requireValid(resistances: List<BigDecimal>) {
        require(resistances.size >= MIN_RESISTORS) { "At least $MIN_RESISTORS resistors are required." }
        require(resistances.all { it.signum() > 0 }) { "Every resistance must be greater than zero." }
    }
}
