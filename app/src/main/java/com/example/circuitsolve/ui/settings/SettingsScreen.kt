package com.example.circuitsolve.ui.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.circuitsolve.data.AppSettings
import com.example.circuitsolve.data.ThemeMode
import com.example.circuitsolve.domain.CircuitMode
import com.example.circuitsolve.domain.MAX_RESISTORS
import com.example.circuitsolve.domain.MIN_RESISTORS
import com.example.circuitsolve.domain.ResistanceUnit
import com.example.circuitsolve.ui.calculator.SubtleDivider
import com.example.circuitsolve.ui.components.ScreenHeader
import com.example.circuitsolve.ui.components.SectionLabel

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    onResetSettings: () -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirm by rememberSaveable { mutableStateOf<String?>(null) }

    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ScreenHeader("Settings", "Preferences are stored on this device.")

            SectionLabel("Appearance", Modifier.padding(top = 8.dp))
            ChoiceSetting(
                title = "Theme",
                options = ThemeMode.entries,
                selected = settings.themeMode,
                label = { it.name.lowercase().replaceFirstChar(Char::uppercase) },
                onSelect = { mode -> onUpdate { it.copy(themeMode = mode) } },
            )
            SwitchSetting(
                title = "Dynamic color",
                subtitle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "Use colors from your wallpaper" else "Requires Android 12 or newer",
                checked = settings.dynamicColor,
                enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                onCheckedChange = { value -> onUpdate { it.copy(dynamicColor = value) } },
            )

            SubtleDivider()
            SectionLabel("Calculator defaults")
            ChoiceSetting(
                title = "Default mode",
                options = CircuitMode.entries,
                selected = settings.defaultMode,
                label = { it.label },
                onSelect = { mode -> onUpdate { it.copy(defaultMode = mode) } },
            )
            CountSetting(
                count = settings.defaultResistorCount,
                onChange = { count -> onUpdate { it.copy(defaultResistorCount = count) } },
            )
            ChoiceSetting(
                title = "Default resistance unit",
                options = ResistanceUnit.entries,
                selected = settings.defaultUnit,
                label = { it.symbol },
                onSelect = { unit -> onUpdate { it.copy(defaultUnit = unit) } },
            )
            ChoiceSetting(
                title = "Decimal places",
                options = AppSettings.DECIMAL_OPTIONS,
                selected = settings.decimalPlaces,
                label = { it.toString() },
                onSelect = { places -> onUpdate { it.copy(decimalPlaces = places) } },
            )
            Text(
                "Defaults apply on app start and when the calculator is reset.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SubtleDivider()
            SectionLabel("Display & feedback")
            SwitchSetting("Haptic feedback", null, settings.hapticFeedback) { v -> onUpdate { it.copy(hapticFeedback = v) } }
            SwitchSetting("Show circuit diagram", null, settings.showCircuitDiagram) { v -> onUpdate { it.copy(showCircuitDiagram = v) } }
            SwitchSetting("Show formula", null, settings.showFormula) { v -> onUpdate { it.copy(showFormula = v) } }
            SwitchSetting("Show breakdown", null, settings.showBreakdown) { v -> onUpdate { it.copy(showBreakdown = v) } }

            SubtleDivider()
            SectionLabel("Data")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { confirm = RESET_SETTINGS }, modifier = Modifier.weight(1f)) {
                    Text("RESET SETTINGS")
                }
                OutlinedButton(onClick = { confirm = CLEAR_HISTORY }, modifier = Modifier.weight(1f)) {
                    Text("CLEAR HISTORY")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    when (confirm) {
        RESET_SETTINGS -> ConfirmDialog(
            title = "Reset settings?",
            text = "All settings return to their defaults. History is kept.",
            confirmLabel = "Reset",
            onConfirm = { confirm = null; onResetSettings() },
            onDismiss = { confirm = null },
        )
        CLEAR_HISTORY -> ConfirmDialog(
            title = "Clear history?",
            text = "All saved calculations will be permanently removed.",
            confirmLabel = "Clear",
            onConfirm = { confirm = null; onClearHistory() },
            onDismiss = { confirm = null },
        )
    }
}

private const val RESET_SETTINGS = "reset_settings"
private const val CLEAR_HISTORY = "clear_history"

@Composable
private fun <T> ChoiceSetting(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    icon = {},
                    label = { Text(label(option), maxLines = 1) },
                    modifier = Modifier.semantics { contentDescription = "$title: ${label(option)}" },
                )
            }
        }
    }
}

@Composable
private fun SwitchSetting(
    title: String,
    subtitle: String?,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun CountSetting(count: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text("Default resistor count", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        FilledTonalIconButton(
            onClick = { onChange(count - 1) },
            enabled = count > MIN_RESISTORS,
            modifier = Modifier.semantics { contentDescription = "Decrease default resistor count" },
        ) {
            Text("−", style = MaterialTheme.typography.titleMedium)
        }
        Text(
            count.toString(),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .width(36.dp)
                .semantics { stateDescription = "$count resistors" },
            textAlign = TextAlign.Center,
        )
        FilledTonalIconButton(
            onClick = { onChange(count + 1) },
            enabled = count < MAX_RESISTORS,
            modifier = Modifier.semantics { contentDescription = "Increase default resistor count" },
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
        }
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
