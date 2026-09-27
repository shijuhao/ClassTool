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
data class ScheduleAdjustment(
    val id: String = UUID.randomUUID().toString(),
    val startDate: String,
    val endDate: String,
    val fromWeekday: Weekday,
    val toWeekday: Weekday
)

@Serializable
data class ScheduleEvent(
    val id: String = UUID.randomUUID().toString(),
    val weekdays: Set<Weekday>,
    val startTime: String,
    val endTime: String,
    val type: ScheduleEventType,
    val courseName: String? = null,
    val courseColor: String? = null,
    val enabled: Boolean = true,
    val urgent: Boolean = false,
    val transfers: List<ScheduleTransfer> = emptyList()
)

@Serializable
data class ScheduleTransfer(
    val id: String = UUID.randomUUID().toString(),
    val fromDate: String,
    val toDate: String,
    val toStartTime: String,
    val toEndTime: String,
    val note: String? = null
)

@Serializable
data class Schedule(
    val events: List<ScheduleEvent> = emptyList(),
    val adjustments: List<ScheduleAdjustment> = emptyList()
)

val Context.scheduleDataStore: DataStore<Preferences> by preferencesDataStore(name = "schedule")
val Context.scheduleDataStoreTest: DataStore<Preferences> by preferencesDataStore(name = "schedule_test")

data class ScheduleValidationResult(
    val valid: Boolean,
    val reason: String? = null
)

object ScheduleValidator {

    private val TIME_REGEX = Regex("""^([01]\d|2[0-3]):([0-5]\d)$""")
    private val DATE_REGEX = Regex("""^\d{4}-\d{2}-\d{2}$""")

    fun parseMinutes(time: String): Int? {
        if (!TIME_REGEX.matches(time)) return null
        val parts = time.split(":")
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        return h * 60 + m
    }

    fun isValidDate(date: String): Boolean = DATE_REGEX.matches(date)

    fun validateAdjustment(adjustment: ScheduleAdjustment): ScheduleValidationResult {
        if (!isValidDate(adjustment.startDate)) {
            return ScheduleValidationResult(false, "调休起始日期非法：${adjustment.startDate}")
        }
        if (!isValidDate(adjustment.endDate)) {
            return ScheduleValidationResult(false, "调休结束日期非法：${adjustment.endDate}")
        }
        if (adjustment.startDate > adjustment.endDate) {
            return ScheduleValidationResult(false, "调休结束日期需不早于开始日期")
        }
        if (adjustment.fromWeekday == adjustment.toWeekday) {
            return ScheduleValidationResult(false, "调休前后星期不能相同")
        }
        return ScheduleValidationResult(true)
    }

    fun validateTransfer(transfer: ScheduleTransfer): ScheduleValidationResult {
        if (!isValidDate(transfer.fromDate)) {
            return ScheduleValidationResult(false, "调课起始日期非法：${transfer.fromDate}")
        }
        if (!isValidDate(transfer.toDate)) {
            return ScheduleValidationResult(false, "调课目标日期非法：${transfer.toDate}")
        }
        val start = parseMinutes(transfer.toStartTime)
            ?: return ScheduleValidationResult(false, "调课开始时间非法：${transfer.toStartTime}")
        val end = parseMinutes(transfer.toEndTime)
            ?: return ScheduleValidationResult(false, "调课结束时间非法：${transfer.toEndTime}")
        if (end <= start) {
            return ScheduleValidationResult(false, "调课结束时间需晚于开始时间")
        }
        return ScheduleValidationResult(true)
    }

    fun validateEvent(event: ScheduleEvent): ScheduleValidationResult {
        if (event.weekdays.isEmpty() && event.transfers.isEmpty()) {
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
        val keys = HashSet<String>()
        for (transfer in event.transfers) {
            val self = validateTransfer(transfer)
            if (!self.valid) return self
            val key = "${transfer.fromDate}->${transfer.toDate}"
            if (!keys.add(key)) {
                return ScheduleValidationResult(false, "存在重复调课：$key")
            }
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
                    return ScheduleValidationResult(
                        false,
                        "事件时间冲突（${day.name}）：${a.startTime}-${a.endTime} 与 ${b.startTime}-${b.endTime}"
                    )
                }
            }
        }

        val adjIds = HashSet<String>()
        for (adj in schedule.adjustments) {
            val self = validateAdjustment(adj)
            if (!self.valid) return self
            if (!adjIds.add(adj.id)) {
                return ScheduleValidationResult(false, "存在重复的调休 ID：${adj.id}")
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

    fun findAdjustmentConflict(
        adjustments: List<ScheduleAdjustment>,
        candidate: ScheduleAdjustment
    ): ScheduleValidationResult {
        val self = validateAdjustment(candidate)
        if (!self.valid) return self

        for (other in adjustments) {
            if (other.id == candidate.id) continue
            val overlap = candidate.startDate <= other.endDate &&
                    other.startDate <= candidate.endDate
            if (!overlap) continue
            if (candidate.fromWeekday == other.fromWeekday &&
                candidate.toWeekday == other.toWeekday
            ) {
                return ScheduleValidationResult(
                    false,
                    "与已有调休时间段重叠：${other.startDate} 至 ${other.endDate}"
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

    suspend fun addTransfer(
        eventId: String,
        transfer: ScheduleTransfer
    ): ScheduleValidationResult {
        val event = getEvent(eventId) ?: return ScheduleValidationResult(false, "事件不存在")

        val self = ScheduleValidator.validateTransfer(transfer)
        if (!self.valid) return self

        if (!isEventActiveOnDate(event, transfer.fromDate)) {
            return ScheduleValidationResult(false, "该事件在 ${transfer.fromDate} 不生效")
        }

        val newStart = ScheduleValidator.parseMinutes(transfer.toStartTime)!!
        val newEnd = ScheduleValidator.parseMinutes(transfer.toEndTime)!!

        val schedule = getSchedule()
        for (other in schedule.events) {
            if (!other.enabled) continue
            if (other.id == eventId) continue

            val effective = effectiveEventOnDate(other, transfer.toDate) ?: continue
            val os = ScheduleValidator.parseMinutes(effective.startTime) ?: continue
            val oe = ScheduleValidator.parseMinutes(effective.endTime) ?: continue
            if (newStart < oe && os < newEnd) {
                return ScheduleValidationResult(
                    false,
                    "目标时间与「${effective.courseName ?: "其他事件"}」冲突"
                )
            }
        }

        val existing = event.transfers.filterNot { it.fromDate == transfer.fromDate }
        val updated = event.copy(transfers = (existing + transfer).sortedBy { it.fromDate })
        return updateEvent(updated)
    }

    suspend fun removeTransfer(
        eventId: String,
        transferId: String
    ): ScheduleValidationResult {
        val event = getEvent(eventId) ?: return ScheduleValidationResult(false, "事件不存在")
        val updated = event.copy(
            transfers = event.transfers.filterNot { it.id == transferId }
        )
        return updateEvent(updated)
    }

    suspend fun clearTransfers(eventId: String): ScheduleValidationResult {
        val event = getEvent(eventId) ?: return ScheduleValidationResult(false, "事件不存在")
        return updateEvent(event.copy(transfers = emptyList()))
    }

    suspend fun addAdjustment(adjustment: ScheduleAdjustment): ScheduleValidationResult {
        val current = getSchedule()
        val conflict = ScheduleValidator.findAdjustmentConflict(current.adjustments, adjustment)
        if (!conflict.valid) return conflict

        val updated = current.copy(
            adjustments = (current.adjustments + adjustment).sortedBy { it.startDate }
        )
        val full = ScheduleValidator.validateSchedule(updated)
        if (!full.valid) return full

        dataStore.edit { preferences ->
            preferences[scheduleKey] = json.encodeToString(updated)
        }
        return ScheduleValidationResult(true)
    }

    suspend fun updateAdjustment(adjustment: ScheduleAdjustment): ScheduleValidationResult {
        val current = getSchedule()
        val others = current.adjustments.filter { it.id != adjustment.id }
        val conflict = ScheduleValidator.findAdjustmentConflict(others, adjustment)
        if (!conflict.valid) return conflict

        val updated = current.copy(
            adjustments = current.adjustments.map {
                if (it.id == adjustment.id) adjustment else it
            }.sortedBy { it.startDate }
        )
        val full = ScheduleValidator.validateSchedule(updated)
        if (!full.valid) return full

        dataStore.edit { preferences ->
            preferences[scheduleKey] = json.encodeToString(updated)
        }
        return ScheduleValidationResult(true)
    }

    suspend fun removeAdjustment(id: String): ScheduleValidationResult {
        val current = getSchedule()
        val updated = current.copy(adjustments = current.adjustments.filterNot { it.id == id })
        dataStore.edit { preferences ->
            preferences[scheduleKey] = json.encodeToString(updated)
        }
        return ScheduleValidationResult(true)
    }

    suspend fun clearAdjustments(): ScheduleValidationResult {
        val current = getSchedule()
        val updated = current.copy(adjustments = emptyList())
        dataStore.edit { preferences ->
            preferences[scheduleKey] = json.encodeToString(updated)
        }
        return ScheduleValidationResult(true)
    }

    suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(scheduleKey)
        }
    }

    private fun isEventActiveOnDate(event: ScheduleEvent, date: String): Boolean {
        if (!event.enabled) return false
        val weekday = dateStringToWeekdayInternal(date) ?: return false
        if (weekday !in event.weekdays) return false
        if (event.transfers.any { it.fromDate == date }) return false
        return true
    }

    private fun effectiveEventOnDate(event: ScheduleEvent, date: String): ScheduleEvent? {
        if (!event.enabled) return null

        val incoming = event.transfers.firstOrNull { it.toDate == date }
        if (incoming != null) {
            return event.copy(
                startTime = incoming.toStartTime,
                endTime = incoming.toEndTime
            )
        }

        val weekday = dateStringToWeekdayInternal(date) ?: return null
        if (weekday !in event.weekdays) return null
        if (event.transfers.any { it.fromDate == date }) return null
        return event
    }

    private fun dateStringToWeekdayInternal(date: String): Weekday? {
        return runCatching {
            when (java.time.LocalDate.parse(date).dayOfWeek.value) {
                1 -> Weekday.MONDAY
                2 -> Weekday.TUESDAY
                3 -> Weekday.WEDNESDAY
                4 -> Weekday.THURSDAY
                5 -> Weekday.FRIDAY
                6 -> Weekday.SATURDAY
                else -> Weekday.SUNDAY
            }
        }.getOrNull()
    }
}