package com.example.circuitsolve.ui.calculator

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.circuitsolve.domain.CircuitEvaluation
import com.example.circuitsolve.domain.CircuitMode
import com.example.circuitsolve.domain.EvaluatedResistor
import com.example.circuitsolve.domain.ResistanceUnit
import com.example.circuitsolve.ui.components.CircuitIcons
import com.example.circuitsolve.ui.components.SectionLabel
import com.example.circuitsolve.ui.theme.FormulaStyle

@Composable
fun ModeSelector(mode: CircuitMode, onModeChange: (CircuitMode) -> Unit, modifier: Modifier = Modifier) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        CircuitMode.entries.forEachIndexed { index, option ->
            SegmentedButton(
                selected = mode == option,
                onClick = { if (mode != option) onModeChange(option) },
                shape = SegmentedButtonDefaults.itemShape(index, CircuitMode.entries.size),
                modifier = Modifier.semantics { contentDescription = "${option.label} mode" },
                label = { Text(option.label.uppercase(), style = MaterialTheme.typography.labelLarge) },
            )
        }
    }
}

@Composable
fun ResultCard(
    evaluation: CircuitEvaluation,
    decimals: Int,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatted = evaluation.formattedResult(decimals)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Equivalent resistance", Modifier.weight(1f))
                IconButton(onClick = onCopy, enabled = formatted != null) {
                    Icon(CircuitIcons.Copy, contentDescription = "Copy equivalent resistance", Modifier.size(20.dp))
                }
            }
            AnimatedContent(
                targetState = formatted,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "result",
            ) { value ->
                val description = value?.let { "Equivalent resistance ${it.spoken}" }
                    ?: "Equivalent resistance unavailable. ${evaluation.firstError.orEmpty()}"
                Text(
                    text = value?.text ?: "—",
                    style = MaterialTheme.typography.displayMedium,
                    color = if (value != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .semantics {
                            contentDescription = description
                            liveRegion = LiveRegionMode.Polite
                        },
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${evaluation.mode.label.uppercase()}  ·  ${evaluation.resistors.size} RESISTORS",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = evaluation.firstError ?: evaluation.expression,
                style = MaterialTheme.typography.bodyMedium,
                color = if (evaluation.isValid) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(end = 12.dp),
            )
        }
    }
}

@Composable
fun CompactResultBar(evaluation: CircuitEvaluation, decimals: Int, modifier: Modifier = Modifier) {
    val formatted = evaluation.formattedResult(decimals)
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = MaterialTheme.shapes.medium,
        shadowElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Req · ${evaluation.mode.label.uppercase()}",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatted?.text ?: "—",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics {
                    contentDescription = formatted?.let { "Equivalent resistance ${it.spoken}" } ?: "Equivalent resistance unavailable"
                },
            )
        }
    }
}

@Composable
fun ResistorRow(
    index: Int,
    resistor: EvaluatedResistor,
    canDelete: Boolean,
    onValueChange: (String) -> Unit,
    onUnitChange: (ResistanceUnit) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            text = resistor.name,
            style = FormulaStyle,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .width(36.dp)
                .padding(top = 18.dp),
        )
        OutlinedTextField(
            value = resistor.valueText,
            onValueChange = onValueChange,
            singleLine = true,
            isError = resistor.error != null,
            placeholder = { Text("0") },
            supportingText = resistor.error?.let { error -> { Text(error.message) } },
            trailingIcon = { UnitSelector(resistor.unit, index, onUnitChange) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            textStyle = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = "Resistor ${index + 1} value" },
        )
        Box(Modifier.padding(top = 4.dp)) {
            if (canDelete) {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Delete resistor ${index + 1}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Spacer(Modifier.size(48.dp))
            }
        }
    }
}

@Composable
private fun UnitSelector(unit: ResistanceUnit, index: Int, onUnitChange: (ResistanceUnit) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(
            onClick = { expanded = true },
            modifier = Modifier
                .widthIn(min = 64.dp)
                .semantics { contentDescription = "Resistor ${index + 1} unit, ${unit.spokenName}" },
        ) {
            Text(unit.symbol, style = MaterialTheme.typography.titleMedium)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, Modifier.size(18.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ResistanceUnit.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text("${option.symbol}   ${option.spokenName}") },
                    onClick = {
                        onUnitChange(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun FormulaSection(mode: CircuitMode, evaluation: CircuitEvaluation, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionLabel("Formula")
        Spacer(Modifier.height(6.dp))
        Text(
            text = CircuitEvaluation.generalFormula(mode),
            style = FormulaStyle,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = evaluation.namedFormula,
            style = FormulaStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BreakdownSection(evaluation: CircuitEvaluation, decimals: Int, modifier: Modifier = Modifier) {
    val formatted = evaluation.formattedResult(decimals)
    Column(modifier = modifier.fillMaxWidth()) {
        SectionLabel("Breakdown")
        Spacer(Modifier.height(8.dp))
        val separator = if (evaluation.mode == CircuitMode.SERIES) "+" else "‖"
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            evaluation.resistors.forEachIndexed { index, resistor ->
                if (index > 0) {
                    Text(separator, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                ValueTile(resistor.name, resistor.displayValue ?: "?")
            }
            Text("=", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ValueTile("Req", formatted?.text ?: "—", highlight = true)
        }
        if (evaluation.mode == CircuitMode.PARALLEL && evaluation.isValid) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "1/Rtotal = " + evaluation.resistors.joinToString(" + ") { "1/${it.displayValue}" },
                style = FormulaStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ValueTile(name: String, value: String, highlight: Boolean = false) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (highlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (highlight) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun SubtleDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, color = MaterialTheme.colorScheme.outlineVariant)
}
