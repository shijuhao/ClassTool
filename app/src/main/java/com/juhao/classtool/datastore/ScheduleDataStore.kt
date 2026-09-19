package com.juhao.classtool.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
enum class ScheduleEventType {
    CLASS,
    BREAK,
    ACTIVITY
}

@Serializable
enum class Weekday {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY
}

@Serializable
data class ScheduleEvent(
    val id: String = UUID.randomUUID().toString(),
    val weekday: Weekday,
    val startTime: String,
    val endTime: String,
    val type: ScheduleEventType,
    val courseName: String? = null,
    val courseColor: String? = null
)

@Serializable
data class Schedule(
    val events: List<ScheduleEvent> = emptyList()
)

val Context.scheduleDataStore: DataStore<Preferences> by preferencesDataStore(name = "schedule")

class ScheduleDataStore(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val scheduleKey = stringPreferencesKey("schedule")

    val scheduleFlow: Flow<Schedule> = context.scheduleDataStore.data.map { preferences ->
        val raw = preferences[scheduleKey] ?: return@map Schedule()
        runCatching { json.decodeFromString<Schedule>(raw) }.getOrElse { Schedule() }
    }

    suspend fun getSchedule(): Schedule {
        var result = Schedule()
        context.scheduleDataStore.edit { preferences ->
            val raw = preferences[scheduleKey]
            if (raw != null) {
                result = runCatching { json.decodeFromString<Schedule>(raw) }.getOrElse { Schedule() }
            }
        }
        return result
    }

    suspend fun getEventsByWeekday(weekday: Weekday): List<ScheduleEvent> {
        return getSchedule().events
            .filter { it.weekday == weekday }
            .sortedBy { it.startTime }
    }

    suspend fun getEvent(id: String): ScheduleEvent? {
        return getSchedule().events.firstOrNull { it.id == id }
    }

    suspend fun setSchedule(schedule: Schedule) {
        context.scheduleDataStore.edit { preferences ->
            preferences[scheduleKey] = json.encodeToString(schedule)
        }
    }

    suspend fun addEvent(event: ScheduleEvent) {
        context.scheduleDataStore.edit { preferences ->
            val current = preferences[scheduleKey]
                ?.let { runCatching { json.decodeFromString<Schedule>(it) }.getOrNull() }
                ?: Schedule()
            val updated = current.copy(events = current.events + event)
            preferences[scheduleKey] = json.encodeToString(updated)
        }
    }

    suspend fun updateEvent(event: ScheduleEvent) {
        context.scheduleDataStore.edit { preferences ->
            val current = preferences[scheduleKey]
                ?.let { runCatching { json.decodeFromString<Schedule>(it) }.getOrNull() }
                ?: Schedule()
            val updated = current.copy(
                events = current.events.map { if (it.id == event.id) event else it }
            )
            preferences[scheduleKey] = json.encodeToString(updated)
        }
    }

    suspend fun removeEvent(id: String) {
        context.scheduleDataStore.edit { preferences ->
            val current = preferences[scheduleKey]
                ?.let { runCatching { json.decodeFromString<Schedule>(it) }.getOrNull() }
                ?: Schedule()
            val updated = current.copy(events = current.events.filterNot { it.id == id })
            preferences[scheduleKey] = json.encodeToString(updated)
        }
    }

    suspend fun setCourse(
        id: String,
        name: String,
        color: String? = null
    ) {
        require(name.isNotBlank()) { "name must not be blank" }
        context.scheduleDataStore.edit { preferences ->
            val current = preferences[scheduleKey]
                ?.let { runCatching { json.decodeFromString<Schedule>(it) }.getOrNull() }
                ?: Schedule()
            val updated = current.copy(
                events = current.events.map { event ->
                    if (event.id == id) {
                        event.copy(
                            type = ScheduleEventType.CLASS,
                            courseName = name,
                            courseColor = color
                        )
                    } else {
                        event
                    }
                }
            )
            preferences[scheduleKey] = json.encodeToString(updated)
        }
    }

    suspend fun clearCourse(id: String) {
        context.scheduleDataStore.edit { preferences ->
            val current = preferences[scheduleKey]
                ?.let { runCatching { json.decodeFromString<Schedule>(it) }.getOrNull() }
                ?: Schedule()
            val updated = current.copy(
                events = current.events.map { event ->
                    if (event.id == id) {
                        event.copy(courseName = null, courseColor = null)
                    } else {
                        event
                    }
                }
            )
            preferences[scheduleKey] = json.encodeToString(updated)
        }
    }

    suspend fun clear() {
        context.scheduleDataStore.edit { preferences ->
            preferences.remove(scheduleKey)
        }
    }
}