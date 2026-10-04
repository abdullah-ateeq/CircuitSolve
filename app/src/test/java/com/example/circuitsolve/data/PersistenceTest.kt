package com.example.circuitsolve.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.example.circuitsolve.domain.CircuitMode
import com.example.circuitsolve.domain.ResistanceUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PersistenceTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()

    private fun dataStore(file: File): DataStore<Preferences> {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob()).also { scopes += it }
        return PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
    }

    @After
    fun tearDown() {
        scopes.forEach { it.cancel() }
    }

    private fun record(id: Long, mode: CircuitMode = CircuitMode.SERIES) = HistoryRecord(
        id = id,
        timestampMillis = 1_700_000_000_000 + id,
        mode = mode,
        resistors = listOf(HistoryResistor("100", ResistanceUnit.OHM), HistoryResistor("4.7", ResistanceUnit.KILOOHM)),
        equivalentOhms = "4800",
        formula = "Rtotal = R1 + R2",
    )

    // 19
    @Test
    fun historySerializationRoundTrip() {
        val records = listOf(record(1), record(2, CircuitMode.PARALLEL))
        val decoded = HistoryCodec.decode(HistoryCodec.encode(records))
        assertEquals(records, decoded)
    }

    @Test
    fun corruptHistoryDecodesToEmpty() {
        assertEquals(emptyList<HistoryRecord>(), HistoryCodec.decode("{not json"))
        assertEquals(emptyList<HistoryRecord>(), HistoryCodec.decode(null))
        assertEquals(emptyList<HistoryRecord>(), HistoryCodec.decode("""[{"id":1,"mode":"UNKNOWN"}]"""))
    }

    @Test
    fun historyKeepsNewestFiftyRecords() {
        var list = emptyList<HistoryRecord>()
        for (id in 1L..51L) list = HistoryCodec.prepend(list, record(id))
        assertEquals(HistoryCodec.MAX_RECORDS, list.size)
        assertEquals(51L, list.first().id)
        assertTrue(list.none { it.id == 1L }) // oldest removed
    }

    @Test
    fun historySurvivesNewDataStoreInstance() = runBlocking {
        val file = File(tmp.root, "history.preferences_pb")
        val first = HistoryRepository(dataStore(file))
        first.add(record(1))
        first.add(record(2))
        first.delete(1)
        scopes.forEach { it.cancel() }

        val reopened = HistoryRepository(dataStore(file))
        assertEquals(listOf(2L), reopened.history.first().map { it.id })
        reopened.clear()
        assertEquals(emptyList<HistoryRecord>(), reopened.history.first())
    }

    // 20
    @Test
    fun settingsPersistence() = runBlocking {
        val file = File(tmp.root, "settings.preferences_pb")
        val repository = SettingsRepository(dataStore(file))
        assertEquals(AppSettings(), repository.settings.first())

        repository.update {
            it.copy(
                themeMode = ThemeMode.DARK,
                defaultMode = CircuitMode.PARALLEL,
                defaultResistorCount = 4,
                defaultUnit = ResistanceUnit.KILOOHM,
                decimalPlaces = 3,
                hapticFeedback = false,
                showFormula = false,
            )
        }
        scopes.forEach { it.cancel() }

        val reopened = SettingsRepository(dataStore(file))
        val loaded = reopened.settings.first()
        assertEquals(ThemeMode.DARK, loaded.themeMode)
        assertEquals(CircuitMode.PARALLEL, loaded.defaultMode)
        assertEquals(4, loaded.defaultResistorCount)
        assertEquals(ResistanceUnit.KILOOHM, loaded.defaultUnit)
        assertEquals(3, loaded.decimalPlaces)
        assertEquals(false, loaded.hapticFeedback)
        assertEquals(false, loaded.showFormula)
        assertEquals(true, loaded.showBreakdown)

        reopened.reset()
        assertEquals(AppSettings(), reopened.settings.first())
    }

    @Test
    fun settingsAreClampedToValidRanges() = runBlocking {
        val repository = SettingsRepository(dataStore(File(tmp.root, "clamp.preferences_pb")))
        repository.update { it.copy(defaultResistorCount = 99, decimalPlaces = 9) }
        val loaded = repository.settings.first()
        assertEquals(10, loaded.defaultResistorCount)
        assertEquals(3, loaded.decimalPlaces)
    }
}
