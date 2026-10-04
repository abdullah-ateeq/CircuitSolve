package com.example.circuitsolve.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.circuitsolve.domain.CircuitMode
import com.example.circuitsolve.domain.ResistanceUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException

@Serializable
data class HistoryResistor(
    /** Plain decimal string, e.g. "4.7". */
    val value: String,
    val unit: ResistanceUnit,
)

@Serializable
data class HistoryRecord(
    val id: Long,
    val timestampMillis: Long,
    val mode: CircuitMode,
    val resistors: List<HistoryResistor>,
    /** Equivalent resistance in Ohms as a plain decimal string. */
    val equivalentOhms: String,
    val formula: String,
)

object HistoryCodec {
    const val MAX_RECORDS = 50

    private val json = Json { ignoreUnknownKeys = true }

    fun encode(records: List<HistoryRecord>): String = json.encodeToString(records)

    /** Returns an empty list for missing or corrupt data instead of crashing. */
    fun decode(text: String?): List<HistoryRecord> {
        if (text.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<HistoryRecord>>(text)
        } catch (_: SerializationException) {
            emptyList()
        } catch (_: IllegalArgumentException) {
            emptyList()
        }
    }

    /** Adds [record] as the newest entry and drops the oldest beyond [MAX_RECORDS]. */
    fun prepend(records: List<HistoryRecord>, record: HistoryRecord): List<HistoryRecord> =
        (listOf(record) + records).take(MAX_RECORDS)
}

class HistoryRepository(private val dataStore: DataStore<Preferences>) {

    val history: Flow<List<HistoryRecord>> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { HistoryCodec.decode(it[KEY]) }

    suspend fun add(record: HistoryRecord) = modify { HistoryCodec.prepend(it, record) }

    suspend fun delete(id: Long) = modify { records -> records.filterNot { it.id == id } }

    suspend fun clear() {
        dataStore.edit { it.remove(KEY) }
    }

    private suspend fun modify(transform: (List<HistoryRecord>) -> List<HistoryRecord>) {
        dataStore.edit { prefs ->
            prefs[KEY] = HistoryCodec.encode(transform(HistoryCodec.decode(prefs[KEY])))
        }
    }

    private companion object {
        val KEY = stringPreferencesKey("history_json")
    }
}
