package com.example.circuitsolve.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp

private const val DURATION_MS = 1350

/** Maps overall progress into a 0..1 sub-phase. */
private fun phase(progress: Float, start: Float, end: Float): Float =
    ((progress - start) / (end - start)).coerceIn(0f, 1f)

/**
 * Line draws in → three resistors appear in series → they fan out into parallel →
 * an equivalent-resistance element appears → title fades in.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(DURATION_MS, easing = LinearEasing))
        onFinished()
    }
    val wire = MaterialTheme.colorScheme.onSurfaceVariant
    val body = MaterialTheme.colorScheme.primary
    val fill = MaterialTheme.colorScheme.background
    val accent = MaterialTheme.colorScheme.primaryContainer

    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = "CircuitSolve, Series and Parallel Resistance" },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Canvas(Modifier.size(width = 240.dp, height = 120.dp)) {
                drawSplashCircuit(progress.value, wire, body, fill, accent)
            }
            Spacer(Modifier.height(28.dp))
            val titleAlpha = FastOutSlowInEasing.transform(phase(progress.value, 0.7f, 0.95f))
            Text(
                "CIRCUITSOLVE",
                style = MaterialTheme.typography.titleLarge.copy(letterSpacing = MaterialTheme.typography.titleLarge.letterSpacing * 2),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.alpha(titleAlpha),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Series & Parallel Resistance",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alpha(titleAlpha),
            )
        }
    }
}

private fun DrawScope.drawSplashCircuit(p: Float, wire: Color, body: Color, fill: Color, accent: Color) {
    val stroke = 2.dp.toPx()
    val cy = size.height / 2
    val left = 4.dp.toPx()
    val right = size.width - 4.dp.toPx()
    val boxW = 30.dp.toPx()
    val boxH = 12.dp.toPx()
    val busL = size.width * 0.22f
    val busR = size.width * 0.66f
    val branchGap = 30.dp.toPx()

    val lineIn = FastOutSlowInEasing.transform(phase(p, 0f, 0.22f))
    val resistorsIn = phase(p, 0.15f, 0.38f)
    val morph = FastOutSlowInEasing.transform(phase(p, 0.4f, 0.68f))
    val reqIn = FastOutSlowInEasing.transform(phase(p, 0.62f, 0.8f))

    // Main line draws from left to right.
    drawLine(wire, Offset(left, cy), Offset(lerp(left, right, lineIn), cy), stroke)
    if (lineIn >= 1f) drawCircle(wire, 3.5.dp.toPx(), Offset(right, cy))
    drawCircle(wire, 3.5.dp.toPx(), Offset(left, cy))

    // Parallel rails grow in as the resistors fan out.
    if (morph > 0f) {
        val railColor = wire.copy(alpha = morph)
        val spread = branchGap * morph
        drawLine(railColor, Offset(busL, cy - spread), Offset(busL, cy + spread), stroke)
        drawLine(railColor, Offset(busR, cy - spread), Offset(busR, cy + spread), stroke)
        drawLine(railColor, Offset(busL, cy - spread), Offset(busR, cy - spread), stroke)
        drawLine(railColor, Offset(busL, cy + spread), Offset(busR, cy + spread), stroke)
    }

    val seriesX = listOf(0.22f, 0.44f, 0.66f).map { size.width * it }
    val parallelX = (busL + busR) / 2
    val parallelY = listOf(cy - branchGap, cy, cy + branchGap)
    for (i in 0..2) {
        val appear = phase(resistorsIn, i * 0.25f, i * 0.25f + 0.5f)
        if (appear <= 0f) continue
        val x = lerp(seriesX[i], parallelX, morph)
        val y = lerp(cy, parallelY[i], morph)
        drawElement(Offset(x, y), boxW * appear, boxH, body, fill, stroke)
    }

    // Equivalent resistor appears on the output side, filled to set it apart.
    if (reqIn > 0f) {
        val center = Offset(size.width * 0.84f, cy)
        val w = boxW * reqIn
        drawRoundRect(
            lerp(fill, accent, reqIn),
            Offset(center.x - w / 2, center.y - boxH / 2),
            Size(w, boxH),
            CornerRadius(3.dp.toPx()),
        )
        drawRoundRect(
            body.copy(alpha = reqIn),
            Offset(center.x - w / 2, center.y - boxH / 2),
            Size(w, boxH),
            CornerRadius(3.dp.toPx()),
            style = Stroke(stroke),
        )
    }
}

private fun DrawScope.drawElement(center: Offset, width: Float, height: Float, body: Color, fill: Color, stroke: Float) {
    val topLeft = Offset(center.x - width / 2, center.y - height / 2)
    drawRoundRect(fill, topLeft, Size(width, height), CornerRadius(3.dp.toPx()))
    drawRoundRect(body, topLeft, Size(width, height), CornerRadius(3.dp.toPx()), style = Stroke(stroke))
}
