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

val Context.todoDataStore: DataStore<Preferences> by preferencesDataStore(name = "todo")

@Serializable
data class TodoItem(
    val id: Long,
    val title: String,
    val note: String = "",
    val done: Boolean = false,
    val createdMillis: Long = System.currentTimeMillis()
)

@Serializable
data class TodoData(
    val items: List<TodoItem> = emptyList()
)

class TodoDataStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val dataKey = stringPreferencesKey("todo_data")

    private val dataFlow: Flow<TodoData> = context.todoDataStore.data.map { prefs ->
        prefs[dataKey]?.let { raw ->
            runCatching { json.decodeFromString<TodoData>(raw) }
                .getOrDefault(TodoData())
        } ?: TodoData()
    }

    val itemsFlow: Flow<List<TodoItem>> = dataFlow.map { it.items }

    suspend fun getItems(): List<TodoItem> = itemsFlow.first()

    suspend fun getItem(id: Long): TodoItem? = getItems().firstOrNull { it.id == id }

    suspend fun upsert(item: TodoItem) {
        context.todoDataStore.edit { prefs ->
            val current = prefs[dataKey]?.let { raw ->
                runCatching { json.decodeFromString<TodoData>(raw) }
                    .getOrDefault(TodoData())
            } ?: TodoData()

            val updated = current.items.toMutableList().apply {
                val idx = indexOfFirst { it.id == item.id }
                if (idx >= 0) set(idx, item) else add(item)
            }
            prefs[dataKey] = json.encodeToString(TodoData(updated))
        }
    }

    suspend fun toggleDone(id: Long) {
        context.todoDataStore.edit { prefs ->
            val current = prefs[dataKey]?.let { raw ->
                runCatching { json.decodeFromString<TodoData>(raw) }
                    .getOrDefault(TodoData())
            } ?: TodoData()

            val updated = current.items.map {
                if (it.id == id) it.copy(done = !it.done) else it
            }
            prefs[dataKey] = json.encodeToString(TodoData(updated))
        }
    }

    suspend fun delete(id: Long) {
        context.todoDataStore.edit { prefs ->
            val current = prefs[dataKey]?.let { raw ->
                runCatching { json.decodeFromString<TodoData>(raw) }
                    .getOrDefault(TodoData())
            } ?: TodoData()

            val updated = current.items.filterNot { it.id == id }
            prefs[dataKey] = json.encodeToString(TodoData(updated))
        }
    }
}