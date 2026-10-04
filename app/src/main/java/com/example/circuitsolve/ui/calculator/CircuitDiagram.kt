package com.example.circuitsolve.ui.calculator

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.circuitsolve.domain.CircuitMode
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

data class DiagramResistor(
    val name: String,
    /** Compact value such as "4.7kΩ", or null when the entry is invalid. */
    val label: String?,
)

private val Pad = 10.dp
private val Lead = 18.dp
private val SeriesCellMin = 76.dp
private val SeriesRowHeight = 62.dp
private val ParallelRowHeight = 40.dp
private val BoxHeight = 18.dp

/**
 * Lightweight schematic of the resistor arrangement. Series layouts wrap into a
 * serpentine when the resistors do not fit on one row; parallel layouts stack branches.
 */
@Composable
fun CircuitDiagram(
    mode: CircuitMode,
    resistors: List<DiagramResistor>,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val colors = DiagramColors(
        wire = MaterialTheme.colorScheme.onSurfaceVariant,
        body = MaterialTheme.colorScheme.primary,
        bodyFill = MaterialTheme.colorScheme.surfaceContainerLow,
        error = MaterialTheme.colorScheme.error,
        text = MaterialTheme.colorScheme.onSurface,
        subtleText = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val description = buildString {
        append(if (mode == CircuitMode.SERIES) "Series circuit: " else "Parallel circuit: ")
        append(resistors.joinToString(", ") { "${it.name} ${it.label ?: "no value"}" })
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val width = maxWidth
        val height: Dp = when (mode) {
            CircuitMode.SERIES -> SeriesRowHeight * seriesRows(width, resistors.size).toFloat()
            CircuitMode.PARALLEL -> ParallelRowHeight * resistors.size.toFloat() + 8.dp
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .semantics { contentDescription = description },
        ) {
            when (mode) {
                CircuitMode.SERIES -> drawSeries(resistors, textMeasurer, colors)
                CircuitMode.PARALLEL -> drawParallel(resistors, textMeasurer, colors)
            }
        }
    }
}

private data class DiagramColors(
    val wire: Color,
    val body: Color,
    val bodyFill: Color,
    val error: Color,
    val text: Color,
    val subtleText: Color,
)

private fun seriesPerRow(width: Dp, count: Int): Int {
    val usable = width - Pad * 2 - Lead * 2
    return max(1, floor(usable / SeriesCellMin).toInt()).coerceAtMost(max(count, 1))
}

private fun seriesRows(width: Dp, count: Int): Int {
    val perRow = seriesPerRow(width, count)
    return max(1, ceil(count / perRow.toFloat()).toInt())
}

private fun DrawScope.drawSeries(
    resistors: List<DiagramResistor>,
    measurer: TextMeasurer,
    colors: DiagramColors,
) {
    val n = resistors.size
    if (n == 0) return
    val widthDp = size.width.toDp()
    val rows = seriesRows(widthDp, n)
    // Balance resistors across rows so the last row is not left nearly empty.
    val perRow = ceil(n / rows.toFloat()).toInt()
    val pad = Pad.toPx()
    val lead = Lead.toPx()
    val rowHeight = SeriesRowHeight.toPx()
    val cellStart = pad + lead
    val cellWidth = (size.width - 2 * (pad + lead)) / perRow
    val turnLeft = pad + lead / 2
    val turnRight = size.width - pad - lead / 2
    val stroke = 1.5.dp.toPx()

    for (row in 0 until rows) {
        val y = rowHeight * row + rowHeight / 2
        val leftToRight = row % 2 == 0
        val isLast = row == rows - 1
        val entryX = when {
            row == 0 -> pad
            leftToRight -> turnLeft
            else -> turnRight
        }
        val exitX = when {
            isLast && leftToRight -> size.width - pad
            isLast -> pad
            leftToRight -> turnRight
            else -> turnLeft
        }
        drawLine(colors.wire, Offset(entryX, y), Offset(exitX, y), stroke)
        if (!isLast) {
            drawLine(colors.wire, Offset(exitX, y), Offset(exitX, y + rowHeight), stroke)
        }
        if (row == 0) drawTerminal(Offset(entryX, y), colors.text)
        if (isLast) drawTerminal(Offset(exitX, y), colors.text)

        val first = row * perRow
        val last = min(n, first + perRow)
        for (index in first until last) {
            val slot = index - first
            val column = if (leftToRight) slot else perRow - 1 - slot
            val cx = cellStart + cellWidth * column + cellWidth / 2
            val boxWidth = min(cellWidth - 14.dp.toPx(), 50.dp.toPx())
            val resistor = resistors[index]
            drawResistorBody(Offset(cx, y), boxWidth, resistor, measurer, colors)
            drawLabel(
                text = resistor.label ?: "?",
                center = Offset(cx, y + BoxHeight.toPx() / 2 + 10.dp.toPx()),
                maxWidth = cellWidth - 4.dp.toPx(),
                color = if (resistor.label == null) colors.error else colors.subtleText,
                measurer = measurer,
            )
        }
    }
}

private fun DrawScope.drawParallel(
    resistors: List<DiagramResistor>,
    measurer: TextMeasurer,
    colors: DiagramColors,
) {
    val n = resistors.size
    if (n == 0) return
    val pad = Pad.toPx()
    val lead = Lead.toPx() * 1.5f
    val rowHeight = ParallelRowHeight.toPx()
    val top = 4.dp.toPx()
    val stroke = 1.5.dp.toPx()
    val busLeft = pad + lead
    val busRight = size.width - pad - lead
    val firstY = top + rowHeight / 2
    val lastY = top + rowHeight * (n - 1) + rowHeight / 2
    val midY = (firstY + lastY) / 2
    val boxWidth = 50.dp.toPx()
    val cx = (busLeft + busRight) / 2

    drawLine(colors.wire, Offset(pad, midY), Offset(busLeft, midY), stroke)
    drawLine(colors.wire, Offset(busRight, midY), Offset(size.width - pad, midY), stroke)
    drawLine(colors.wire, Offset(busLeft, firstY), Offset(busLeft, lastY), stroke)
    drawLine(colors.wire, Offset(busRight, firstY), Offset(busRight, lastY), stroke)
    drawTerminal(Offset(pad, midY), colors.text)
    drawTerminal(Offset(size.width - pad, midY), colors.text)

    resistors.forEachIndexed { index, resistor ->
        val y = top + rowHeight * index + rowHeight / 2
        drawLine(colors.wire, Offset(busLeft, y), Offset(busRight, y), stroke)
        drawCircle(colors.wire, radius = 2.5.dp.toPx(), center = Offset(busLeft, y))
        drawCircle(colors.wire, radius = 2.5.dp.toPx(), center = Offset(busRight, y))
        drawResistorBody(Offset(cx, y), boxWidth, resistor, measurer, colors)
        val labelLeft = cx + boxWidth / 2 + 6.dp.toPx()
        val labelWidth = busRight - labelLeft - 6.dp.toPx()
        if (labelWidth > 0) {
            drawLabel(
                text = resistor.label ?: "?",
                center = Offset(labelLeft + labelWidth / 2, y - 9.dp.toPx()),
                maxWidth = labelWidth,
                color = if (resistor.label == null) colors.error else colors.subtleText,
                measurer = measurer,
            )
        }
    }
}

private fun DrawScope.drawResistorBody(
    center: Offset,
    width: Float,
    resistor: DiagramResistor,
    measurer: TextMeasurer,
    colors: DiagramColors,
) {
    val height = BoxHeight.toPx()
    val topLeft = Offset(center.x - width / 2, center.y - height / 2)
    val corner = CornerRadius(3.dp.toPx())
    val invalid = resistor.label == null
    drawRoundRect(colors.bodyFill, topLeft, Size(width, height), corner)
    drawRoundRect(
        color = if (invalid) colors.error else colors.body,
        topLeft = topLeft,
        size = Size(width, height),
        cornerRadius = corner,
        style = Stroke(
            width = 1.5.dp.toPx(),
            pathEffect = if (invalid) PathEffect.dashPathEffect(floatArrayOf(6f, 4f)) else null,
        ),
    )
    drawLabel(
        text = resistor.name,
        center = center,
        maxWidth = width,
        color = colors.text,
        measurer = measurer,
        style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 10.sp, fontWeight = FontWeight.Medium),
    )
}

private fun DrawScope.drawTerminal(at: Offset, color: Color) {
    drawCircle(color, radius = 4.dp.toPx(), center = at)
}

private fun DrawScope.drawLabel(
    text: String,
    center: Offset,
    maxWidth: Float,
    color: Color,
    measurer: TextMeasurer,
    style: TextStyle = TextStyle(fontSize = 11.sp, fontFeatureSettings = "tnum"),
) {
    if (maxWidth <= 0f) return
    val layout = measurer.measure(
        text = text,
        style = style.copy(color = color),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        constraints = Constraints(maxWidth = maxWidth.toInt()),
    )
    drawText(
        layout,
        topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f),
    )
}
