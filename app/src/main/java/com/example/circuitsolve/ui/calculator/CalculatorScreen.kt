package com.example.circuitsolve.ui.calculator

import android.content.ClipData
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.circuitsolve.data.AppSettings
import com.example.circuitsolve.domain.CircuitEvaluation
import com.example.circuitsolve.domain.CircuitMode
import com.example.circuitsolve.domain.MAX_RESISTORS
import com.example.circuitsolve.domain.ResistanceUnit
import com.example.circuitsolve.ui.CalculatorState
import com.example.circuitsolve.ui.CircuitSolveViewModel
import com.example.circuitsolve.ui.components.CircuitIcons
import com.example.circuitsolve.ui.components.SafetyNote
import com.example.circuitsolve.ui.components.ScreenHeader
import com.example.circuitsolve.ui.components.SectionLabel
import kotlinx.coroutines.launch

private const val RESULT_KEY = "result"

@Composable
fun CalculatorScreen(
    viewModel: CircuitSolveViewModel,
    settings: AppSettings,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.calculator.collectAsStateWithLifecycle()
    val evaluation by viewModel.evaluation.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    fun tick() {
        if (settings.hapticFeedback) haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
    }

    fun copy(text: String, message: String) {
        scope.launch {
            clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("CircuitSolve", text)))
            onMessage(message)
        }
    }

    val actions = CalculatorActions(
        onModeChange = { tick(); viewModel.setMode(it) },
        onAdd = { tick(); viewModel.addResistor() },
        onDelete = { tick(); viewModel.deleteResistor(it) },
        onValueChange = viewModel::updateValue,
        onUnitChange = viewModel::updateUnit,
        onCopyResult = {
            evaluation.formattedResult(settings.decimalPlaces)?.let { copy(it.text, "Resistance copied") }
        },
        onCopySummary = {
            evaluation.summary(settings.decimalPlaces)?.let { copy(it, "Summary copied") }
        },
        onSave = {
            if (viewModel.saveCalculation()) {
                tick()
                onMessage("Calculation saved")
            } else {
                onMessage(evaluation.firstError ?: "Nothing to save")
            }
        },
        onReset = { tick(); viewModel.reset() },
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        if (maxWidth >= 600.dp) {
            WideCalculator(state, evaluation, settings, actions)
        } else {
            PhoneCalculator(state, evaluation, settings, actions)
        }
    }
}

private class CalculatorActions(
    val onModeChange: (CircuitMode) -> Unit,
    val onAdd: () -> Unit,
    val onDelete: (Long) -> Unit,
    val onValueChange: (Long, String) -> Unit,
    val onUnitChange: (Long, ResistanceUnit) -> Unit,
    val onCopyResult: () -> Unit,
    val onCopySummary: () -> Unit,
    val onSave: () -> Unit,
    val onReset: () -> Unit,
)

private val ScreenPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp)

@Composable
private fun PhoneCalculator(
    state: CalculatorState,
    evaluation: CircuitEvaluation,
    settings: AppSettings,
    actions: CalculatorActions,
) {
    val listState = rememberLazyListState()
    val resultHidden by remember {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.none { it.key == RESULT_KEY } && listState.firstVisibleItemIndex > 0
        }
    }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = ScreenPadding,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            headerItems(state, actions)
            resultItem(evaluation, settings, actions)
            diagramItem(evaluation, settings)
            resistorItems(state, evaluation, actions)
            detailItems(evaluation, settings, actions)
        }
        AnimatedVisibility(
            visible = resultHidden,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            CompactResultBar(evaluation, settings.decimalPlaces)
        }
    }
}

@Composable
private fun WideCalculator(
    state: CalculatorState,
    evaluation: CircuitEvaluation,
    settings: AppSettings,
    actions: CalculatorActions,
) {
    Row(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = ScreenPadding,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f),
        ) {
            headerItems(state, actions)
            resistorItems(state, evaluation, actions)
        }
        LazyColumn(
            contentPadding = ScreenPadding,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f),
        ) {
            resultItem(evaluation, settings, actions)
            diagramItem(evaluation, settings)
            detailItems(evaluation, settings, actions)
        }
    }
}

private fun LazyListScope.headerItems(state: CalculatorState, actions: CalculatorActions) {
    item(key = "header") {
        ScreenHeader("CircuitSolve", "Calculate equivalent resistance.")
    }
    item(key = "mode") {
        ModeSelector(state.mode, actions.onModeChange)
    }
}

private fun LazyListScope.resultItem(evaluation: CircuitEvaluation, settings: AppSettings, actions: CalculatorActions) {
    item(key = RESULT_KEY) {
        ResultCard(evaluation, settings.decimalPlaces, actions.onCopyResult)
    }
}

private fun LazyListScope.diagramItem(evaluation: CircuitEvaluation, settings: AppSettings) {
    if (!settings.showCircuitDiagram) return
    item(key = "diagram") {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(vertical = 12.dp, horizontal = 8.dp)) {
                SectionLabel("Circuit", Modifier.padding(start = 8.dp, bottom = 8.dp))
                CircuitDiagram(
                    mode = evaluation.mode,
                    resistors = evaluation.resistors.map { DiagramResistor(it.name, it.compactValue) },
                )
            }
        }
    }
}

private fun LazyListScope.resistorItems(
    state: CalculatorState,
    evaluation: CircuitEvaluation,
    actions: CalculatorActions,
) {
    item(key = "resistors-label") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Resistors", Modifier.weight(1f))
            Text(
                "${state.resistors.size} / $MAX_RESISTORS",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    itemsIndexed(evaluation.resistors, key = { _, r -> r.id }) { index, resistor ->
        ResistorRow(
            index = index,
            resistor = resistor,
            canDelete = state.canDelete,
            onValueChange = { actions.onValueChange(resistor.id, it) },
            onUnitChange = { actions.onUnitChange(resistor.id, it) },
            onDelete = { actions.onDelete(resistor.id) },
            modifier = Modifier.animateItem(),
        )
    }
    item(key = "add") {
        androidx.compose.foundation.layout.Column {
            OutlinedButton(
                onClick = actions.onAdd,
                enabled = state.canAdd,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "Add resistor" },
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("ADD RESISTOR")
            }
            if (!state.canAdd) {
                Text(
                    "Maximum of $MAX_RESISTORS resistors.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, start = 4.dp),
                )
            }
        }
    }
}

private fun LazyListScope.detailItems(evaluation: CircuitEvaluation, settings: AppSettings, actions: CalculatorActions) {
    if (settings.showFormula) {
        item(key = "formula") { FormulaSection(evaluation.mode, evaluation) }
    }
    if (settings.showBreakdown) {
        item(key = "breakdown") { BreakdownSection(evaluation, settings.decimalPlaces) }
    }
    item(key = "actions") {
        androidx.compose.foundation.layout.Column {
            SubtleDivider(Modifier.padding(bottom = 12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = actions.onSave,
                    enabled = evaluation.isValid,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Save calculation" },
                ) {
                    Icon(CircuitIcons.Save, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("SAVE")
                }
                OutlinedButton(
                    onClick = actions.onCopySummary,
                    enabled = evaluation.isValid,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "Copy calculation summary" },
                ) {
                    Text("COPY SUMMARY")
                }
            }
            Spacer(Modifier.height(4.dp))
            TextButton(
                onClick = actions.onReset,
                modifier = Modifier.semantics { contentDescription = "Reset calculator" },
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("RESET")
            }
        }
    }
    item(key = "safety") { SafetyNote() }
}
