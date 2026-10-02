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
import java.time.LocalDate
import java.util.UUID

@Serializable
enum class ScheduleEventType { CLASS, BREAK, ACTIVITY }

@Serializable
enum class Weekday { MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY }

@Serializable
enum class WeekdayScope { WORKDAY, WEEKEND, CUSTOM }

@Serializable
data class ScheduleAdjustment(
    val id: String = UUID.randomUUID().toString(),
    val startDate: String,
    val endDate: String,
    val fromWeekday: Weekday,
    val toWeekday: Weekday
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
data class ScheduleEvent(
    val id: String = UUID.randomUUID().toString(),
    val weekdays: Set<Weekday>,
    val startTime: String,
    val endTime: String,
    val type: ScheduleEventType,
    val courseName: String? = null,
    val courseColor: String? = null,
    val courseNameByWeekday: Map<Weekday, String> = emptyMap(),
    val courseColorByWeekday: Map<Weekday, String> = emptyMap(),
    val enabled: Boolean = true,
    val urgent: Boolean = false,
    val transfers: List<ScheduleTransfer> = emptyList()
)

@Serializable
data class Schedule(
    val events: List<ScheduleEvent> = emptyList(),
    val adjustments: List<ScheduleAdjustment> = emptyList()
)

@Serializable
data class ScheduleTable(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val schedule: Schedule = Schedule(),
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class ScheduleTableCollection(
    val tables: List<ScheduleTable> = emptyList(),
    val activeTableId: String? = null
)

val Context.scheduleDataStore: DataStore<Preferences> by preferencesDataStore(name = "schedule")

data class ScheduleValidationResult(val valid: Boolean, val reason: String? = null) {
    companion object {
        val OK = ScheduleValidationResult(true)
        fun fail(reason: String) = ScheduleValidationResult(false, reason)
    }
}

object ScheduleValidator {

    private val TIME_REGEX = Regex("""^([01]\d|2[0-3]):([0-5]\d)$""")

    fun parseMinutes(time: String): Int? =
        if (!TIME_REGEX.matches(time)) null
        else time.split(":").let { it[0].toInt() * 60 + it[1].toInt() }

    fun isValidDate(date: String): Boolean =
        runCatching { LocalDate.parse(date) }.isSuccess

    fun validateAdjustment(a: ScheduleAdjustment) = when {
        !isValidDate(a.startDate) -> ScheduleValidationResult.fail("调休起始日期非法：${a.startDate}")
        !isValidDate(a.endDate) -> ScheduleValidationResult.fail("调休结束日期非法：${a.endDate}")
        a.startDate > a.endDate -> ScheduleValidationResult.fail("调休结束日期需不早于开始日期")
        a.fromWeekday == a.toWeekday -> ScheduleValidationResult.fail("调休前后星期不能相同")
        else -> ScheduleValidationResult.OK
    }

    fun validateTransfer(t: ScheduleTransfer) = when {
        !isValidDate(t.fromDate) -> ScheduleValidationResult.fail("调课起始日期非法：${t.fromDate}")
        !isValidDate(t.toDate) -> ScheduleValidationResult.fail("调课目标日期非法：${t.toDate}")
        parseMinutes(t.toStartTime) == null -> ScheduleValidationResult.fail("调课开始时间非法：${t.toStartTime}")
        parseMinutes(t.toEndTime) == null -> ScheduleValidationResult.fail("调课结束时间非法：${t.toEndTime}")
        parseMinutes(t.toEndTime)!! <= parseMinutes(t.toStartTime)!! ->
            ScheduleValidationResult.fail("调课结束时间需晚于开始时间")
        else -> ScheduleValidationResult.OK
    }

    fun validateEvent(e: ScheduleEvent): ScheduleValidationResult {
        if (e.weekdays.isEmpty() && e.transfers.isEmpty()) return ScheduleValidationResult.fail("事件未指定任何星期")
        val s = parseMinutes(e.startTime) ?: return ScheduleValidationResult.fail("开始时间非法：${e.startTime}")
        val t = parseMinutes(e.endTime) ?: return ScheduleValidationResult.fail("结束时间非法：${e.endTime}")
        if (t <= s) return ScheduleValidationResult.fail("结束时间需晚于开始时间")
        if (e.id.isBlank()) return ScheduleValidationResult.fail("事件 ID 不能为空")
        val keys = HashSet<String>()
        for (tr in e.transfers) {
            validateTransfer(tr).let { if (!it.valid) return it }
            if (!keys.add("${tr.fromDate}->${tr.toDate}")) return ScheduleValidationResult.fail("存在重复调课：${tr.fromDate}->${tr.toDate}")
        }
        return ScheduleValidationResult.OK
    }

    fun validateSchedule(schedule: Schedule): ScheduleValidationResult {
        val ids = HashSet<String>()
        for (e in schedule.events) {
            validateEvent(e).let { if (!it.valid) return it }
            if (!ids.add(e.id)) return ScheduleValidationResult.fail("存在重复的事件 ID：${e.id}")
        }
        val byDay = mutableMapOf<Weekday, MutableList<ScheduleEvent>>()
        for (e in schedule.events.filter { it.enabled })
            for (d in e.weekdays) byDay.getOrPut(d) { mutableListOf() }.add(e)
        for ((d, list) in byDay) {
            val sorted = list.sortedBy { parseMinutes(it.startTime) ?: Int.MAX_VALUE }
            for (i in 0 until sorted.size - 1) {
                val a = sorted[i]; val b = sorted[i + 1]
                val ae = parseMinutes(a.endTime) ?: continue
                val bs = parseMinutes(b.startTime) ?: continue
                if (ae > bs) return ScheduleValidationResult.fail(
                    "事件时间冲突（${d.name}）：${a.startTime}-${a.endTime} 与 ${b.startTime}-${b.endTime}"
                )
            }
        }
        val adjIds = HashSet<String>()
        for (a in schedule.adjustments) {
            validateAdjustment(a).let { if (!it.valid) return it }
            if (!adjIds.add(a.id)) return ScheduleValidationResult.fail("存在重复的调休 ID：${a.id}")
        }
        return ScheduleValidationResult.OK
    }

    fun findConflict(
        events: List<ScheduleEvent>,
        startTime: String,
        endTime: String,
        weekdays: Set<Weekday>,
        selfIds: Set<String> = emptySet()
    ): ScheduleValidationResult {
        val newStart = parseMinutes(startTime)
            ?: return ScheduleValidationResult.fail("开始时间非法：$startTime")
        val newEnd = parseMinutes(endTime)
            ?: return ScheduleValidationResult.fail("结束时间非法：$endTime")
        if (newEnd <= newStart) {
            return ScheduleValidationResult.fail("结束时间需晚于开始时间")
        }
        if (weekdays.isEmpty()) {
            return ScheduleValidationResult.fail("事件未指定任何星期")
        }

        for (other in events) {
            if (!other.enabled || other.id in selfIds) continue
            if (weekdays.intersect(other.weekdays).isEmpty()) continue

            val otherStart = parseMinutes(other.startTime) ?: continue
            val otherEnd = parseMinutes(other.endTime) ?: continue
            if (newStart < otherEnd && otherStart < newEnd) {
                return ScheduleValidationResult.fail(
                    "与已有事件冲突：${other.startTime}-${other.endTime}"
                )
            }
        }

        return ScheduleValidationResult.OK
    }

    fun findConflict(events: List<ScheduleEvent>, candidate: ScheduleEvent): ScheduleValidationResult {
        validateEvent(candidate).let { if (!it.valid) return it }
        return findConflict(
            events = events,
            startTime = candidate.startTime,
            endTime = candidate.endTime,
            weekdays = candidate.weekdays,
            selfIds = setOf(candidate.id)
        )
    }

    fun findAdjustmentConflict(adjustments: List<ScheduleAdjustment>, candidate: ScheduleAdjustment): ScheduleValidationResult {
        validateAdjustment(candidate).let { if (!it.valid) return it }
        for (o in adjustments) {
            if (o.id == candidate.id) continue
            if (candidate.startDate > o.endDate || o.startDate > candidate.endDate) continue
            if (candidate.fromWeekday == o.fromWeekday && candidate.toWeekday == o.toWeekday)
                return ScheduleValidationResult.fail("与已有调休时间段重叠：${o.startDate} 至 ${o.endDate}")
        }
        return ScheduleValidationResult.OK
    }
}

class ScheduleDataStore(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val scheduleKey = stringPreferencesKey("schedule")
    private val tablesKey = stringPreferencesKey("schedule_tables")
    private val dataStore: DataStore<Preferences> get() = context.scheduleDataStore

    val tablesFlow: Flow<ScheduleTableCollection> = dataStore.data.map { p ->
        p[tablesKey]?.let { runCatching { json.decodeFromString<ScheduleTableCollection>(it) }.getOrNull() }
            ?: ScheduleTableCollection()
    }

    val activeTableFlow: Flow<ScheduleTable?> = tablesFlow.map { c ->
        val id = c.activeTableId ?: c.tables.firstOrNull()?.id
        c.tables.firstOrNull { it.id == id } ?: c.tables.firstOrNull()
    }

    val scheduleFlow: Flow<Schedule> = activeTableFlow.map { it?.schedule ?: Schedule() }

    suspend fun getTableCollection(): ScheduleTableCollection =
        dataStore.data.first()[tablesKey]
            ?.let { runCatching { json.decodeFromString<ScheduleTableCollection>(it) }.getOrNull() }
            ?: ScheduleTableCollection()

    suspend fun getActiveTable(): ScheduleTable? {
        val c = getTableCollection()
        val id = c.activeTableId ?: c.tables.firstOrNull()?.id
        return c.tables.firstOrNull { it.id == id } ?: c.tables.firstOrNull()
    }

    suspend fun getSchedule(): Schedule = getActiveTable()?.schedule ?: Schedule()

    suspend fun getEventsByWeekday(weekday: Weekday): List<ScheduleEvent> =
        getSchedule().events
            .asSequence()
            .filter { it.enabled && weekday in it.weekdays }
            .sortedBy { ScheduleValidator.parseMinutes(it.startTime) ?: Int.MAX_VALUE }
            .toList()

    suspend fun getEvent(id: String): ScheduleEvent? = getSchedule().events.firstOrNull { it.id == id }

    private suspend fun saveCollection(c: ScheduleTableCollection) {
        dataStore.edit { it[tablesKey] = json.encodeToString(c) }
    }

    private suspend fun mutateActive(block: (Schedule) -> Schedule): ScheduleValidationResult {
        val table = getActiveTable() ?: return ScheduleValidationResult.fail("请先前往设置新建或选择日程表")
        val updated = block(table.schedule)
        ScheduleValidator.validateSchedule(updated).let { if (!it.valid) return it }
        val c = getTableCollection()
        val tableIndex = c.tables.indexOfFirst { it.id == table.id }
        if (tableIndex < 0) return ScheduleValidationResult.fail("日程表不存在")
        val tables = c.tables.toMutableList()
        tables[tableIndex] = tables[tableIndex].copy(schedule = updated)
        saveCollection(c.copy(tables = tables))
        return ScheduleValidationResult.OK
    }

    suspend fun setSchedule(schedule: Schedule) = mutateActive { schedule }

    suspend fun updateTableSchedule(tableId: String, schedule: Schedule): ScheduleValidationResult {
        ScheduleValidator.validateSchedule(schedule).let { if (!it.valid) return it }
        val c = getTableCollection()
        saveCollection(c.copy(tables = c.tables.map { if (it.id == tableId) it.copy(schedule = schedule) else it }))
        return ScheduleValidationResult.OK
    }

    suspend fun restoreTableCollection(collection: ScheduleTableCollection): ScheduleValidationResult {
        for (t in collection.tables) {
            ScheduleValidator.validateSchedule(t.schedule).let {
                if (!it.valid) return ScheduleValidationResult.fail("日程表「${t.name}」校验失败：${it.reason}")
            }
        }
        val normalized = if (collection.tables.isEmpty()) {
            val t = ScheduleTable(name = "默认日程表")
            ScheduleTableCollection(listOf(t), t.id)
        } else collection.copy(
            activeTableId = collection.activeTableId?.takeIf { id -> collection.tables.any { it.id == id } }
                ?: collection.tables.first().id
        )
        saveCollection(normalized)
        return ScheduleValidationResult.OK
    }

    suspend fun setActiveTable(tableId: String): ScheduleValidationResult {
        val c = getTableCollection()
        if (c.tables.none { it.id == tableId }) return ScheduleValidationResult.fail("日程表不存在")
        saveCollection(c.copy(activeTableId = tableId))
        return ScheduleValidationResult.OK
    }

    suspend fun addTable(name: String): ScheduleValidationResult {
        val c = getTableCollection()
        if (c.tables.any { it.name == name }) return ScheduleValidationResult.fail("已存在同名日程表")
        val t = ScheduleTable(name = name)
        saveCollection(c.copy(tables = c.tables + t, activeTableId = c.activeTableId ?: t.id))
        return ScheduleValidationResult.OK
    }

    suspend fun removeTable(tableId: String): ScheduleValidationResult {
        val c = getTableCollection()
        if (c.tables.none { it.id == tableId }) {
            return ScheduleValidationResult.fail("日程表不存在")
        }
        if (c.tables.size == 1) {
            return ScheduleValidationResult.fail("至少需要保留一个日程表")
        }
        val remaining = c.tables.filterNot { it.id == tableId }
        val activeId = c.activeTableId
            ?.takeIf { it != tableId && remaining.any { table -> table.id == it } }
            ?: remaining.first().id
        saveCollection(c.copy(
            tables = remaining,
            activeTableId = activeId
        ))
        return ScheduleValidationResult.OK
    }

    suspend fun renameTable(tableId: String, newName: String): ScheduleValidationResult {
        val c = getTableCollection()
        if (c.tables.any { it.id != tableId && it.name == newName }) return ScheduleValidationResult.fail("已存在同名日程表")
        saveCollection(c.copy(tables = c.tables.map { if (it.id == tableId) it.copy(name = newName) else it }))
        return ScheduleValidationResult.OK
    }

    suspend fun addEvent(event: ScheduleEvent) = addEvents(listOf(event))

    suspend fun addEvents(events: List<ScheduleEvent>): ScheduleValidationResult {
        if (events.isEmpty()) return ScheduleValidationResult.OK
        var conflict: ScheduleValidationResult? = null
        val result = mutateActive { current ->
            var w = current
            for (e in events) {
                val c = ScheduleValidator.findConflict(w.events, e)
                if (!c.valid) { conflict = c; return@mutateActive current }
                w = w.copy(events = w.events + e)
            }
            w
        }
        return conflict ?: result
    }

    suspend fun updateEvent(event: ScheduleEvent): ScheduleValidationResult {
        val table = getActiveTable() ?: return ScheduleValidationResult.fail("请先前往设置新建或选择日程表")
        ScheduleValidator.findConflict(table.schedule.events.filter { it.id != event.id }, event)
            .let { if (!it.valid) return it }
        return mutateActive { it.copy(events = it.events.map { e -> if (e.id == event.id) event else e }) }
    }

    suspend fun removeEvent(id: String) { mutateActive { it.copy(events = it.events.filterNot { e -> e.id == id }) } }

    suspend fun setCourse(id: String, name: String, color: String? = null) {
        require(name.isNotBlank())
        mutateActive { it.copy(events = it.events.map { e ->
            if (e.id == id) e.copy(type = ScheduleEventType.CLASS, courseName = name, courseColor = color) else e
        }) }
    }

    suspend fun clearCourse(id: String) {
        mutateActive { it.copy(events = it.events.map { e ->
            if (e.id == id) e.copy(courseName = null, courseColor = null) else e
        }) }
    }

    suspend fun setCourseForWeekday(id: String, weekday: Weekday, name: String, color: String? = null) {
        require(name.isNotBlank())
        mutateActive { it.copy(events = it.events.map { e ->
            if (e.id != id) e else e.copy(
                type = ScheduleEventType.CLASS,
                courseNameByWeekday = e.courseNameByWeekday + (weekday to name),
                courseColorByWeekday = if (color != null) e.courseColorByWeekday + (weekday to color) else e.courseColorByWeekday - weekday
            )
        }) }
    }

    suspend fun clearCourseForWeekday(id: String, weekday: Weekday) {
        mutateActive { it.copy(events = it.events.map { e ->
            if (e.id != id) e else e.copy(
                courseNameByWeekday = e.courseNameByWeekday - weekday,
                courseColorByWeekday = e.courseColorByWeekday - weekday
            )
        }) }
    }

    suspend fun addTransfer(eventId: String, transfer: ScheduleTransfer): ScheduleValidationResult {
        val event = getEvent(eventId) ?: return ScheduleValidationResult.fail("事件不存在")
        ScheduleValidator.validateTransfer(transfer).let { if (!it.valid) return it }
        if (!isEventActiveOnDate(event, transfer.fromDate)) return ScheduleValidationResult.fail("该事件在 ${transfer.fromDate} 不生效")
        val ns = ScheduleValidator.parseMinutes(transfer.toStartTime)!!
        val ne = ScheduleValidator.parseMinutes(transfer.toEndTime)!!
        for (o in getSchedule().events) {
            if (!o.enabled || o.id == eventId) continue
            val eff = effectiveEventOnDate(o, transfer.toDate) ?: continue
            val os = ScheduleValidator.parseMinutes(eff.startTime) ?: continue
            val oe = ScheduleValidator.parseMinutes(eff.endTime) ?: continue
            if (ns < oe && os < ne) return ScheduleValidationResult.fail("目标时间与「${eff.courseName ?: "其他事件"}」冲突")
        }
        val existing = event.transfers.filterNot { it.fromDate == transfer.fromDate }
        return updateEvent(event.copy(transfers = (existing + transfer).sortedBy { it.fromDate }))
    }

    suspend fun removeTransfer(eventId: String, transferId: String): ScheduleValidationResult {
        val event = getEvent(eventId) ?: return ScheduleValidationResult.fail("事件不存在")
        return updateEvent(event.copy(transfers = event.transfers.filterNot { it.id == transferId }))
    }

    suspend fun clearTransfers(eventId: String): ScheduleValidationResult {
        val event = getEvent(eventId) ?: return ScheduleValidationResult.fail("事件不存在")
        return updateEvent(event.copy(transfers = emptyList()))
    }

    suspend fun addAdjustment(adjustment: ScheduleAdjustment): ScheduleValidationResult {
        ScheduleValidator.findAdjustmentConflict(getSchedule().adjustments, adjustment).let { if (!it.valid) return it }
        return mutateActive { it.copy(adjustments = (it.adjustments + adjustment).sortedBy { a -> a.startDate }) }
    }

    suspend fun updateAdjustment(adjustment: ScheduleAdjustment): ScheduleValidationResult {
        ScheduleValidator.findAdjustmentConflict(getSchedule().adjustments.filter { it.id != adjustment.id }, adjustment)
            .let { if (!it.valid) return it }
        return mutateActive { it.copy(adjustments = it.adjustments.map { a -> if (a.id == adjustment.id) adjustment else a }.sortedBy { a -> a.startDate }) }
    }

    suspend fun removeAdjustment(id: String): ScheduleValidationResult =
        mutateActive { it.copy(adjustments = it.adjustments.filterNot { a -> a.id == id }) }

    suspend fun clearAdjustments(): ScheduleValidationResult = mutateActive { it.copy(adjustments = emptyList()) }

    suspend fun clear() { mutateActive { Schedule() } }

    private fun isEventActiveOnDate(e: ScheduleEvent, date: String): Boolean {
        if (!e.enabled) return false
        if (e.transfers.any { it.fromDate == date }) return false
        return dateStringToWeekdayInternal(date) in e.weekdays
    }

    private fun effectiveEventOnDate(e: ScheduleEvent, date: String): ScheduleEvent? {
        if (!e.enabled) return null
        e.transfers.firstOrNull { it.toDate == date }?.let { return e.copy(startTime = it.toStartTime, endTime = it.toEndTime) }
        if (e.transfers.any { it.fromDate == date }) return null
        return if (dateStringToWeekdayInternal(date) in e.weekdays) e else null
    }

    private fun dateStringToWeekdayInternal(date: String): Weekday? = runCatching {
        Weekday.entries[LocalDate.parse(date).dayOfWeek.value - 1]
    }.getOrNull()
}