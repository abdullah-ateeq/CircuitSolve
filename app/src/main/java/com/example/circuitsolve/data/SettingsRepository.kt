package com.example.circuitsolve.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.circuitsolve.domain.CircuitMode
import com.example.circuitsolve.domain.MAX_RESISTORS
import com.example.circuitsolve.domain.MIN_RESISTORS
import com.example.circuitsolve.domain.ResistanceUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

enum class ThemeMode { LIGHT, DARK, SYSTEM }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val defaultMode: CircuitMode = CircuitMode.SERIES,
    val defaultResistorCount: Int = MIN_RESISTORS,
    val defaultUnit: ResistanceUnit = ResistanceUnit.OHM,
    val decimalPlaces: Int = 2,
    val hapticFeedback: Boolean = true,
    val showCircuitDiagram: Boolean = true,
    val showFormula: Boolean = true,
    val showBreakdown: Boolean = true,
) {
    companion object {
        val DECIMAL_OPTIONS = listOf(1, 2, 3)
    }
}

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<AppSettings> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map(::toSettings)

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { prefs ->
            val updated = transform(toSettings(prefs))
            prefs[Keys.THEME] = updated.themeMode.name
            prefs[Keys.DYNAMIC_COLOR] = updated.dynamicColor
            prefs[Keys.DEFAULT_MODE] = updated.defaultMode.name
            prefs[Keys.DEFAULT_COUNT] = updated.defaultResistorCount.coerceIn(MIN_RESISTORS, MAX_RESISTORS)
            prefs[Keys.DEFAULT_UNIT] = updated.defaultUnit.name
            prefs[Keys.DECIMALS] = updated.decimalPlaces.coerceIn(1, 3)
            prefs[Keys.HAPTICS] = updated.hapticFeedback
            prefs[Keys.SHOW_CIRCUIT] = updated.showCircuitDiagram
            prefs[Keys.SHOW_FORMULA] = updated.showFormula
            prefs[Keys.SHOW_BREAKDOWN] = updated.showBreakdown
        }
    }

    suspend fun reset() {
        dataStore.edit { prefs ->
            Keys.all.forEach { prefs.remove(it) }
        }
    }

    private fun toSettings(prefs: Preferences): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            themeMode = enumOrDefault(prefs[Keys.THEME], defaults.themeMode),
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: defaults.dynamicColor,
            defaultMode = enumOrDefault(prefs[Keys.DEFAULT_MODE], defaults.defaultMode),
            defaultResistorCount = (prefs[Keys.DEFAULT_COUNT] ?: defaults.defaultResistorCount)
                .coerceIn(MIN_RESISTORS, MAX_RESISTORS),
            defaultUnit = enumOrDefault(prefs[Keys.DEFAULT_UNIT], defaults.defaultUnit),
            decimalPlaces = (prefs[Keys.DECIMALS] ?: defaults.decimalPlaces).coerceIn(1, 3),
            hapticFeedback = prefs[Keys.HAPTICS] ?: defaults.hapticFeedback,
            showCircuitDiagram = prefs[Keys.SHOW_CIRCUIT] ?: defaults.showCircuitDiagram,
            showFormula = prefs[Keys.SHOW_FORMULA] ?: defaults.showFormula,
            showBreakdown = prefs[Keys.SHOW_BREAKDOWN] ?: defaults.showBreakdown,
        )
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val DEFAULT_MODE = stringPreferencesKey("default_mode")
        val DEFAULT_COUNT = intPreferencesKey("default_resistor_count")
        val DEFAULT_UNIT = stringPreferencesKey("default_unit")
        val DECIMALS = intPreferencesKey("decimal_places")
        val HAPTICS = booleanPreferencesKey("haptic_feedback")
        val SHOW_CIRCUIT = booleanPreferencesKey("show_circuit")
        val SHOW_FORMULA = booleanPreferencesKey("show_formula")
        val SHOW_BREAKDOWN = booleanPreferencesKey("show_breakdown")

        val all: List<Preferences.Key<*>> = listOf(
            THEME, DYNAMIC_COLOR, DEFAULT_MODE, DEFAULT_COUNT, DEFAULT_UNIT,
            DECIMALS, HAPTICS, SHOW_CIRCUIT, SHOW_FORMULA, SHOW_BREAKDOWN,
        )
    }
}
