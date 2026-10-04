package com.example.circuitsolve.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.circuitsolve.data.HistoryRecord
import com.example.circuitsolve.domain.CircuitMode
import com.example.circuitsolve.domain.formatInputValue
import com.example.circuitsolve.domain.formatResistanceParts
import com.example.circuitsolve.ui.components.CircuitIcons
import com.example.circuitsolve.ui.components.ScreenHeader
import com.example.circuitsolve.ui.equivalentOhmsOrNull
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(
    history: List<HistoryRecord>,
    decimals: Int,
    onOpen: (HistoryRecord) -> Unit,
    onDelete: (HistoryRecord) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
        ) {
            item(key = "header") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ScreenHeader("History", "${history.size} saved · up to 50", Modifier.weight(1f))
                    if (history.isNotEmpty()) {
                        TextButton(onClick = { confirmClear = true }) { Text("CLEAR ALL") }
                    }
                }
            }
            if (history.isEmpty()) {
                item(key = "empty") { EmptyHistory() }
            }
            items(history, key = { it.id }) { record ->
                HistoryItem(
                    record = record,
                    decimals = decimals,
                    date = dateFormat.format(Date(record.timestampMillis)),
                    onOpen = { onOpen(record) },
                    onDelete = { onDelete(record) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear history?") },
            text = { Text("All saved calculations will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = { confirmClear = false; onClearAll() }) { Text("Clear all") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun HistoryItem(
    record: HistoryRecord,
    decimals: Int,
    date: String,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val operator = if (record.mode == CircuitMode.SERIES) " + " else " || "
    val expression = record.resistors.joinToString(operator) { resistor ->
        resistor.value.toBigDecimalOrNull()?.let { formatInputValue(it, resistor.unit) } ?: "?"
    }
    val equivalent = record.equivalentOhmsOrNull()?.let { formatResistanceParts(it, decimals) }
    val modeLabel = record.mode.label.uppercase()

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClickLabel = "Open in calculator", role = Role.Button, onClick = onOpen),
    ) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.Top) {
            Column(
                Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) {
                        contentDescription = "$modeLabel, $expression, equivalent ${equivalent?.spoken ?: "unavailable"}, saved $date"
                    },
            ) {
                Text(
                    "$modeLabel · ${record.resistors.size} RESISTORS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    expression,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Equivalent: ${equivalent?.text ?: "—"}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete saved calculation", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun EmptyHistory() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            CircuitIcons.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text("No saved calculations", style = MaterialTheme.typography.titleMedium)
        Text(
            "Use SAVE on the calculator to keep a result here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
