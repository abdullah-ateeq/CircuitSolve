package com.example.circuitsolve.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.circuitsolve.domain.CircuitMode
import com.example.circuitsolve.ui.calculator.CircuitDiagram
import com.example.circuitsolve.ui.calculator.DiagramResistor
import com.example.circuitsolve.ui.calculator.SubtleDivider
import com.example.circuitsolve.ui.components.SAFETY_NOTE
import com.example.circuitsolve.ui.components.SectionLabel
import com.example.circuitsolve.ui.theme.FormulaStyle

private val FEATURES = listOf(
    "Series resistance",
    "Parallel resistance",
    "Multiple resistors (2–10)",
    "Automatic unit conversion",
    "Circuit visualization",
    "Formula display",
    "Calculation breakdown",
    "Calculation history",
    "Works fully offline",
)

@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column {
                Text("CircuitSolve", style = MaterialTheme.typography.headlineSmall)
                Text("Version 1.0", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            CircuitDiagram(
                mode = CircuitMode.PARALLEL,
                resistors = listOf(DiagramResistor("R1", ""), DiagramResistor("R2", "")),
            )
            Text(
                "An offline calculator for finding equivalent resistance in series and parallel resistor networks.",
                style = MaterialTheme.typography.bodyLarge,
            )
            SubtleDivider()
            SectionLabel("Features")
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FEATURES.forEach { feature ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(6.dp)) {}
                        Spacer(Modifier.width(12.dp))
                        Text(feature, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            SubtleDivider()
            SectionLabel("Formulas")
            Text(
                "Series:   Rtotal = R1 + R2 + … + Rn\nParallel: 1/Rtotal = 1/R1 + 1/R2 + … + 1/Rn",
                style = FormulaStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SubtleDivider()
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    SectionLabel("Safety")
                    Spacer(Modifier.height(6.dp))
                    Text(SAFETY_NOTE, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
