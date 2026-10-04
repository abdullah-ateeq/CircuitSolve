package com.example.circuitsolve

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.example.circuitsolve.data.HistoryRepository
import com.example.circuitsolve.data.SettingsRepository

private val Context.settingsDataStore by preferencesDataStore(name = "settings")
private val Context.historyDataStore by preferencesDataStore(name = "history")

class CircuitSolveApplication : Application() {
    val settingsRepository by lazy { SettingsRepository(settingsDataStore) }
    val historyRepository by lazy { HistoryRepository(historyDataStore) }
}
