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
enum class WeekdayScope {
    WORKDAY,
    WEEKEND,
    CUSTOM
}

@Serializable
data class ScheduleEvent(
    val id: String = UUID.randomUUID().toString(),
    val weekdays: Set<Weekday>,
    val startTime: String,
    val endTime: String,
    val type: ScheduleEventType,
    val courseName: String? = null,
    val courseColor: String? = null,
    val enabled: Boolean = true
)

@Serializable
data class Schedule(
    val events: List<ScheduleEvent> = emptyList()
)

val Context.scheduleDataStore: DataStore<Preferences> by preferencesDataStore(name = "schedule")
val Context.scheduleDataStoreTest: DataStore<Preferences> by preferencesDataStore(name = "schedule_test")

data class ScheduleValidationResult(
    val valid: Boolean,
    val reason: String? = null
)

object ScheduleValidator {

    private val TIME_REGEX = Regex("""^([01]\d|2[0-3]):([0-5]\d)$""")

    fun parseMinutes(time: String): Int? {
        if (!TIME_REGEX.matches(time)) return null
        val parts = time.split(":")
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        return h * 60 + m
    }

    fun validateEvent(event: ScheduleEvent): ScheduleValidationResult {
        if (event.weekdays.isEmpty()) {
            return ScheduleValidationResult(false, "事件未指定任何星期")
        }
        val start = parseMinutes(event.startTime)
            ?: return ScheduleValidationResult(false, "开始时间非法：${event.startTime}")
        val end = parseMinutes(event.endTime)
            ?: return ScheduleValidationResult(false, "结束时间非法：${event.endTime}")
        if (end <= start) {
            return ScheduleValidationResult(false, "结束时间需晚于开始时间")
        }
        if (event.id.isBlank()) {
            return ScheduleValidationResult(false, "事件 ID 不能为空")
        }
        return ScheduleValidationResult(true)
    }

    fun validateSchedule(schedule: Schedule): ScheduleValidationResult {
        val idSet = HashSet<String>()
        for (event in schedule.events) {
            val self = validateEvent(event)
            if (!self.valid) return self
            if (!idSet.add(event.id)) {
                return ScheduleValidationResult(false, "存在重复的事件 ID：${event.id}")
            }
        }

        val enabledEvents = schedule.events.filter { it.enabled }
        val byWeekday = mutableMapOf<Weekday, MutableList<ScheduleEvent>>()
        for (event in enabledEvents) {
            for (day in event.weekdays) {
                byWeekday.getOrPut(day) { mutableListOf() }.add(event)
            }
        }

        for ((day, events) in byWeekday) {
            val sorted = events.sortedBy { parseMinutes(it.startTime) ?: Int.MAX_VALUE }
            for (i in 0 until sorted.size - 1) {
                val a = sorted[i]
                val b = sorted[i + 1]
                val aEnd = parseMinutes(a.endTime) ?: continue
                val bStart = parseMinutes(b.startTime) ?: continue
                if (aEnd > bStart) {
                    val dayLabel = day.name
                    return ScheduleValidationResult(
                        false,
                        "事件时间冲突（$dayLabel）：${a.startTime}-${a.endTime} 与 ${b.startTime}-${b.endTime}"
                    )
                }
            }
        }

        return ScheduleValidationResult(true)
    }

    fun findConflict(
        events: List<ScheduleEvent>,
        candidate: ScheduleEvent
    ): ScheduleValidationResult {
        val self = validateEvent(candidate)
        if (!self.valid) return self

        val newStart = parseMinutes(candidate.startTime)!!
        val newEnd = parseMinutes(candidate.endTime)!!

        for (other in events) {
            if (!other.enabled) continue
            if (other.id == candidate.id) continue
            if (candidate.weekdays.intersect(other.weekdays).isEmpty()) continue

            val os = parseMinutes(other.startTime) ?: continue
            val oe = parseMinutes(other.endTime) ?: continue

            if (newStart < oe && os < newEnd) {
                return ScheduleValidationResult(
                    false,
                    "与已有事件冲突：${other.startTime}-${other.endTime}"
                )
            }
        }
        return ScheduleValidationResult(true)
    }
}

class ScheduleDataStore(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val scheduleKey = stringPreferencesKey("schedule")

    private val dataStore: DataStore<Preferences>
        get() = if (TestModeState.enabled) {
            context.scheduleDataStoreTest
        } else {
            context.scheduleDataStore
        }

    val scheduleFlow: Flow<Schedule> = dataStore.data.map { preferences ->
        val raw = preferences[scheduleKey] ?: return@map Schedule()
        runCatching { json.decodeFromString<Schedule>(raw) }.getOrElse { Schedule() }
    }

    suspend fun getSchedule(): Schedule {
        var result = Schedule()
        dataStore.edit { preferences ->
            val raw = preferences[scheduleKey]
            if (raw != null) {
                result = runCatching { json.decodeFromString<Schedule>(raw) }.getOrElse { Schedule() }
            }
        }
        return result
    }

    suspend fun getEventsByWeekday(weekday: Weekday): List<ScheduleEvent> {
        return getSchedule().events
            .filter { it.enabled && weekday in it.weekdays }
            .sortedBy { it.startTime }
    }

    suspend fun getEvent(id: String): ScheduleEvent? {
        return getSchedule().events.firstOrNull { it.id == id }
    }

    suspend fun setSchedule(schedule: Schedule): ScheduleValidationResult {
        val result = ScheduleValidator.validateSchedule(schedule)
        if (!result.valid) return result
        dataStore.edit { preferences ->
            preferences[scheduleKey] = json.encodeToString(schedule)
        }
        return result
    }

    suspend fun setScheduleUnsafe(schedule: Schedule) {
        dataStore.edit { preferences ->
            preferences[scheduleKey] = json.encodeToString(schedule)
        }
    }

    suspend fun addEvent(event: ScheduleEvent): ScheduleValidationResult {
        return addEvents(listOf(event))
    }

    suspend fun addEvents(events: List<ScheduleEvent>): ScheduleValidationResult {
        if (events.isEmpty()) return ScheduleValidationResult(true)

        val current = getSchedule()
        var working = current

        for (event in events) {
            val conflict = ScheduleValidator.findConflict(working.events, event)
            if (!conflict.valid) return conflict
            working = working.copy(events = working.events + event)
        }

        val full = ScheduleValidator.validateSchedule(working)
        if (!full.valid) return full

        dataStore.edit { preferences ->
            preferences[scheduleKey] = json.encodeToString(working)
        }
        return ScheduleValidationResult(true)
    }

    suspend fun updateEvent(event: ScheduleEvent): ScheduleValidationResult {
        val current = getSchedule()
        val others = current.events.filter { it.id != event.id }
        val conflict = ScheduleValidator.findConflict(others, event)
        if (!conflict.valid) return conflict

        val updated = current.copy(
            events = current.events.map { if (it.id == event.id) event else it }
        )
        val full = ScheduleValidator.validateSchedule(updated)
        if (!full.valid) return full

        dataStore.edit { preferences ->
            preferences[scheduleKey] = json.encodeToString(updated)
        }
        return ScheduleValidationResult(true)
    }

    suspend fun removeEvent(id: String) {
        dataStore.edit { preferences ->
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
        dataStore.edit { preferences ->
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
        dataStore.edit { preferences ->
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
        dataStore.edit { preferences ->
            preferences.remove(scheduleKey)
        }
    }
}