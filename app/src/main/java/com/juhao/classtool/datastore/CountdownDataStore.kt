package com.juhao.classtool.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val Context.countdownDataStore: DataStore<Preferences> by preferencesDataStore(name = "countdown")

@Serializable
data class CountdownDay(
    val id: Long,
    val title: String,
    val dateMillis: Long,
    val note: String = "",
    val progressEnabled: Boolean = false,
    val startDateMillis: Long = 0L
)

@Serializable
data class CountdownData(
    val days: List<CountdownDay> = emptyList()
)

class CountdownDataStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val dataKey = stringPreferencesKey("countdown_data")

    private val dataFlow: Flow<CountdownData> = context.countdownDataStore.data.map { prefs ->
        prefs[dataKey]?.let { raw ->
            runCatching { json.decodeFromString<CountdownData>(raw) }
                .getOrDefault(CountdownData())
        } ?: CountdownData()
    }

    val daysFlow: Flow<List<CountdownDay>> = dataFlow.map { it.days }

    suspend fun getDays(): List<CountdownDay> = daysFlow.first()

    suspend fun getDay(id: Long): CountdownDay? = getDays().firstOrNull { it.id == id }

    suspend fun upsert(day: CountdownDay) {
        context.countdownDataStore.edit { prefs ->
            val current = prefs[dataKey]?.let { raw ->
                runCatching { json.decodeFromString<CountdownData>(raw) }
                    .getOrDefault(CountdownData())
            } ?: CountdownData()

            val updated = current.days.toMutableList().apply {
                val idx = indexOfFirst { it.id == day.id }
                if (idx >= 0) set(idx, day) else add(day)
            }
            prefs[dataKey] = json.encodeToString(CountdownData(updated))
        }
    }

    suspend fun delete(id: Long) {
        context.countdownDataStore.edit { prefs ->
            val current = prefs[dataKey]?.let { raw ->
                runCatching { json.decodeFromString<CountdownData>(raw) }
                    .getOrDefault(CountdownData())
            } ?: CountdownData()
            val updated = current.days.filterNot { it.id == id }
            prefs[dataKey] = json.encodeToString(CountdownData(updated))
        }
    }
}