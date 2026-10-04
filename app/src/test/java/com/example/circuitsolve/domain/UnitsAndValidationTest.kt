package com.example.circuitsolve.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class UnitsAndValidationTest {

    private fun assertNumericEquals(expected: String, actual: BigDecimal) {
        assertTrue("expected $expected but was $actual", BigDecimal(expected).compareTo(actual) == 0)
    }

    // 7
    @Test
    fun milliohmConversion() {
        assertNumericEquals("0.001", resistanceToOhms(BigDecimal.ONE, ResistanceUnit.MILLIOHM))
        assertNumericEquals("1000", ohmsToDisplay(BigDecimal.ONE, ResistanceUnit.MILLIOHM))
    }

    // 8
    @Test
    fun ohmConversion() {
        assertNumericEquals("47", resistanceToOhms(BigDecimal("47"), ResistanceUnit.OHM))
        assertNumericEquals("47", ohmsToDisplay(BigDecimal("47"), ResistanceUnit.OHM))
    }

    // 9
    @Test
    fun kiloohmConversion() {
        assertNumericEquals("4700", resistanceToOhms(BigDecimal("4.7"), ResistanceUnit.KILOOHM))
        assertNumericEquals("4.7", ohmsToDisplay(BigDecimal("4700"), ResistanceUnit.KILOOHM))
    }

    // 10
    @Test
    fun megaohmConversion() {
        assertNumericEquals("1500000", resistanceToOhms(BigDecimal("1.5"), ResistanceUnit.MEGAOHM))
        assertNumericEquals("1.5", ohmsToDisplay(BigDecimal("1500000"), ResistanceUnit.MEGAOHM))
    }

    // 11
    @Test
    fun gigaohmConversion() {
        assertNumericEquals("2000000000", resistanceToOhms(BigDecimal("2"), ResistanceUnit.GIGAOHM))
        assertNumericEquals("0.5", ohmsToDisplay(BigDecimal("500000000"), ResistanceUnit.GIGAOHM))
    }

    // 13
    @Test
    fun zeroResistanceIsRejected() {
        listOf("0", "0.0", "000", ".0", "0.").forEach {
            assertEquals(it, ResistanceParseResult.Invalid(ResistanceInputError.ZERO), ResistanceInputParser.parse(it))
        }
        assertEquals("Resistance must be greater than zero.", ResistanceInputError.ZERO.message)
    }

    // 14
    @Test
    fun negativeResistanceIsRejected() {
        listOf("-1", "-0.5", "-", "-100").forEach {
            assertEquals(it, ResistanceParseResult.Invalid(ResistanceInputError.NEGATIVE), ResistanceInputParser.parse(it))
        }
        assertEquals("Resistance cannot be negative.", ResistanceInputError.NEGATIVE.message)
    }

    // 15
    @Test
    fun emptyInputIsRejected() {
        listOf("", "   ").forEach {
            assertEquals(ResistanceParseResult.Invalid(ResistanceInputError.EMPTY), ResistanceInputParser.parse(it))
        }
        assertEquals("Enter a resistance value.", ResistanceInputError.EMPTY.message)
    }

    @Test
    fun malformedInputIsRejectedWithoutCrashing() {
        listOf(".", "1.2.3", "abc", "1e5", "NaN", "Infinity", "1-2").forEach {
            assertEquals(it, ResistanceParseResult.Invalid(ResistanceInputError.MALFORMED), ResistanceInputParser.parse(it))
        }
    }

    @Test
    fun sanitizeFiltersKeyboardInput() {
        assertEquals("4.7", ResistanceInputParser.sanitize("4,7"))
        assertEquals("-5", ResistanceInputParser.sanitize("-5"))
        assertNull(ResistanceInputParser.sanitize("1.2.3"))
        assertNull(ResistanceInputParser.sanitize("12a"))
        assertNull(ResistanceInputParser.sanitize("5-"))
        assertNull(ResistanceInputParser.sanitize("1".repeat(ResistanceInputParser.MAX_INPUT_LENGTH + 1)))
    }

    // 18
    @Test
    fun resultFormatting() {
        assertEquals("1.00 mΩ", formatResistance(BigDecimal("0.001")))
        assertEquals("500.00 mΩ", formatResistance(BigDecimal("0.5")))
        assertEquals("1.00 Ω", formatResistance(BigDecimal("1")))
        assertEquals("650.00 Ω", formatResistance(BigDecimal("650")))
        assertEquals("1.00 kΩ", formatResistance(BigDecimal("1000")))
        assertEquals("4.70 kΩ", formatResistance(BigDecimal("4700")))
        assertEquals("1.50 MΩ", formatResistance(BigDecimal("1500000")))
        assertEquals("1.00 MΩ", formatResistance(BigDecimal("1000000")))
        assertEquals("2.00 GΩ", formatResistance(BigDecimal("2000000000")))
        assertEquals("4.7 kΩ", formatResistance(BigDecimal("4700"), 1))
        assertEquals("66.667 Ω", formatResistance(BigDecimal("66.6666666"), 3))
    }

    @Test
    fun formattingCarriesRoundingIntoNextUnit() {
        assertEquals("1.00 kΩ", formatResistance(BigDecimal("999.999")))
        assertEquals("1.00 MΩ", formatResistance(BigDecimal("999999.9")))
    }

    @Test
    fun spokenFormatting() {
        assertEquals("650.00 ohms", formatResistanceParts(BigDecimal("650")).spoken)
        assertEquals("4.70 kilohms", formatResistanceParts(BigDecimal("4700")).spoken)
    }

    @Test
    fun inputValueFormatting() {
        assertEquals("100 Ω", formatInputValue(BigDecimal("100.00"), ResistanceUnit.OHM))
        assertEquals("4.7 kΩ", formatInputValue(BigDecimal("4.70"), ResistanceUnit.KILOOHM))
        assertEquals("1000 Ω", formatInputValue(BigDecimal("1000"), ResistanceUnit.OHM))
    }
}
