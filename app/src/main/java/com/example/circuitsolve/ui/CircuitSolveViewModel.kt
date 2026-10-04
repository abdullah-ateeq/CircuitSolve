package com.example.circuitsolve.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.circuitsolve.CircuitSolveApplication
import com.example.circuitsolve.data.AppSettings
import com.example.circuitsolve.data.HistoryRecord
import com.example.circuitsolve.data.HistoryRepository
import com.example.circuitsolve.data.HistoryResistor
import com.example.circuitsolve.data.SettingsRepository
import com.example.circuitsolve.domain.CircuitEvaluation
import com.example.circuitsolve.domain.CircuitMode
import com.example.circuitsolve.domain.MAX_RESISTORS
import com.example.circuitsolve.domain.MIN_RESISTORS
import com.example.circuitsolve.domain.ResistanceInputParser
import com.example.circuitsolve.domain.ResistanceUnit
import com.example.circuitsolve.domain.ResistorInput
import com.example.circuitsolve.domain.plainNumber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class CalculatorState(
    val mode: CircuitMode,
    val resistors: List<ResistorInput>,
) {
    val canAdd: Boolean get() = resistors.size < MAX_RESISTORS
    val canDelete: Boolean get() = resistors.size > MIN_RESISTORS
}

class CircuitSolveViewModel(
    private val settingsRepository: SettingsRepository,
    private val historyRepository: HistoryRepository,
) : ViewModel() {

    private var nextId = 0L

    private val _calculator = MutableStateFlow(defaultState(AppSettings()))
    val calculator: StateFlow<CalculatorState> = _calculator

    /** Live evaluation: recomputed on every edit, add, delete and mode change. */
    val evaluation: StateFlow<CircuitEvaluation> = _calculator
        .map { CircuitEvaluation.evaluate(it.mode, it.resistors) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, CircuitEvaluation.evaluate(_calculator.value.mode, _calculator.value.resistors))

    val settings: StateFlow<AppSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val history: StateFlow<List<HistoryRecord>> = historyRepository.history
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var userEdited = false

    init {
        viewModelScope.launch {
            val initial = settingsRepository.settings.first()
            if (!userEdited) _calculator.value = defaultState(initial)
        }
    }

    // region Calculator

    fun setMode(mode: CircuitMode) = edit { it.copy(mode = mode) }

    fun addResistor() = edit { state ->
        if (!state.canAdd) return@edit state
        val unit = state.resistors.lastOrNull()?.unit ?: currentSettings().defaultUnit
        state.copy(resistors = state.resistors + ResistorInput(newId(), "", unit))
    }

    fun deleteResistor(id: Long) = edit { state ->
        if (!state.canDelete) state else state.copy(resistors = state.resistors.filterNot { it.id == id })
    }

    fun updateValue(id: Long, text: String) {
        val sanitized = ResistanceInputParser.sanitize(text) ?: return
        edit { state -> state.copy(resistors = state.resistors.map { if (it.id == id) it.copy(valueText = sanitized) else it }) }
    }

    fun updateUnit(id: Long, unit: ResistanceUnit) = edit { state ->
        state.copy(resistors = state.resistors.map { if (it.id == id) it.copy(unit = unit) else it })
    }

    fun reset() = edit { defaultState(currentSettings()) }

    private inline fun edit(crossinline transform: (CalculatorState) -> CalculatorState) {
        userEdited = true
        _calculator.update { transform(it) }
    }

    private fun defaultState(settings: AppSettings) = CalculatorState(
        mode = settings.defaultMode,
        resistors = List(settings.defaultResistorCount) {
            ResistorInput(newId(), DEFAULT_VALUE, settings.defaultUnit)
        },
    )

    private fun newId(): Long = nextId++

    private fun currentSettings(): AppSettings = settings.value ?: AppSettings()

    // endregion

    // region History

    /** Saves the current calculation. Returns false when there is no valid result to save. */
    fun saveCalculation(): Boolean {
        val evaluation = evaluation.value
        val result = evaluation.result ?: return false
        val now = System.currentTimeMillis()
        val record = HistoryRecord(
            id = maxOf(now, (history.value.maxOfOrNull { it.id } ?: 0L) + 1),
            timestampMillis = now,
            mode = evaluation.mode,
            resistors = evaluation.resistors.map { HistoryResistor(plainNumber(it.value!!), it.unit) },
            equivalentOhms = result.equivalentResistanceOhms.toPlainString(),
            formula = evaluation.namedFormula,
        )
        viewModelScope.launch { historyRepository.add(record) }
        return true
    }

    fun restore(record: HistoryRecord) = edit {
        CalculatorState(
            mode = record.mode,
            resistors = record.resistors.take(MAX_RESISTORS)
                .map { ResistorInput(newId(), it.value, it.unit) }
                .let { restored ->
                    restored + List((MIN_RESISTORS - restored.size).coerceAtLeast(0)) {
                        ResistorInput(newId(), DEFAULT_VALUE, currentSettings().defaultUnit)
                    }
                },
        )
    }

    fun deleteHistory(id: Long) {
        viewModelScope.launch { historyRepository.delete(id) }
    }

    fun clearHistory() {
        viewModelScope.launch { historyRepository.clear() }
    }

    // endregion

    // region Settings

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }

    fun resetSettings() {
        viewModelScope.launch { settingsRepository.reset() }
    }

    // endregion

    companion object {
        const val DEFAULT_VALUE = "100"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CircuitSolveApplication
                CircuitSolveViewModel(app.settingsRepository, app.historyRepository)
            }
        }
    }
}

/** Parses a stored history value; used for display in the History list. */
fun HistoryRecord.equivalentOhmsOrNull(): BigDecimal? = equivalentOhms.toBigDecimalOrNull()
