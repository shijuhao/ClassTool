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

    private fun Preferences.decode(): CountdownData =
        this[dataKey]
            ?.let { runCatching { json.decodeFromString<CountdownData>(it) }.getOrNull() }
            ?: CountdownData()

    private val dataFlow: Flow<CountdownData> =
        context.countdownDataStore.data.map { it.decode() }

    val daysFlow: Flow<List<CountdownDay>> = dataFlow.map { it.days }

    suspend fun getDays(): List<CountdownDay> = daysFlow.first()

    suspend fun getDay(id: Long): CountdownDay? = getDays().firstOrNull { it.id == id }

    suspend fun upsert(day: CountdownDay) = mutate { days ->
        days.toMutableList().apply {
            val idx = indexOfFirst { it.id == day.id }
            if (idx >= 0) set(idx, day) else add(day)
        }
    }

    suspend fun delete(id: Long) = mutate { days -> days.filterNot { it.id == id } }

    private suspend fun mutate(block: (List<CountdownDay>) -> List<CountdownDay>) {
        context.countdownDataStore.edit { prefs ->
            prefs[dataKey] = json.encodeToString(CountdownData(block(prefs.decode().days)))
        }
    }
}