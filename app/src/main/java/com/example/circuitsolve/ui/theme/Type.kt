package com.example.circuitsolve.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val base = Typography()

/** Tabular figures keep columns of values aligned. */
private const val TABULAR = "tnum"

val Typography = Typography(
    displayMedium = base.displayMedium.copy(
        fontWeight = FontWeight.Light,
        fontFeatureSettings = TABULAR,
        letterSpacing = (-0.5).sp,
    ),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Medium),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Medium, fontFeatureSettings = TABULAR),
    bodyLarge = base.bodyLarge.copy(fontFeatureSettings = TABULAR),
    bodyMedium = base.bodyMedium.copy(fontFeatureSettings = TABULAR),
    labelMedium = base.labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.4.sp),
    labelSmall = base.labelSmall.copy(fontWeight = FontWeight.Medium, letterSpacing = 1.2.sp),
)

/** Monospaced style for formulas and resistor designators. */
val FormulaStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 13.sp,
    lineHeight = 20.sp,
)
