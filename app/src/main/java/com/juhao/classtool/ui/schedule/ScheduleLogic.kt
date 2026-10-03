package com.juhao.classtool.ui.schedule

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import com.juhao.classtool.datastore.ScheduleAdjustment
import com.juhao.classtool.datastore.ScheduleEvent
import com.juhao.classtool.datastore.ScheduleEventType
import com.juhao.classtool.datastore.Weekday
import java.time.LocalDate
import java.time.LocalTime

fun weekdayLabel(weekday: Weekday): String = when (weekday) {
    Weekday.MONDAY -> "周一"
    Weekday.TUESDAY -> "周二"
    Weekday.WEDNESDAY -> "周三"
    Weekday.THURSDAY -> "周四"
    Weekday.FRIDAY -> "周五"
    Weekday.SATURDAY -> "周六"
    Weekday.SUNDAY -> "周日"
}

fun weekdayShortLabel(weekday: Weekday): String = when (weekday) {
    Weekday.MONDAY -> "一"
    Weekday.TUESDAY -> "二"
    Weekday.WEDNESDAY -> "三"
    Weekday.THURSDAY -> "四"
    Weekday.FRIDAY -> "五"
    Weekday.SATURDAY -> "六"
    Weekday.SUNDAY -> "日"
}

fun eventDisplayName(event: ScheduleEvent): String = event.courseName
    ?: when (event.type) {
        ScheduleEventType.BREAK -> "课间休息"
        ScheduleEventType.ACTIVITY -> "活动"
        ScheduleEventType.CLASS -> "未命名"
    }

fun eventDisplayNameFor(event: ScheduleEvent, weekday: Weekday): String {
    val name = event.courseNameByWeekday[weekday] ?: event.courseName
    return name ?: when (event.type) {
        ScheduleEventType.BREAK -> "课间休息"
        ScheduleEventType.ACTIVITY -> "活动"
        ScheduleEventType.CLASS -> "未命名"
    }
}

fun eventColorFor(event: ScheduleEvent, weekday: Weekday): Color {
    val hex = event.courseColorByWeekday[weekday] ?: event.courseColor
    return hex?.let { parseColor(it) } ?: Color.Unspecified
}

fun parseColor(hex: String): Color = runCatching {
    Color(hex.toColorInt())
}.getOrElse { Color.DarkGray }

fun contrastColorFor(hex: String): Color = contrastColorForColor(parseColor(hex))

fun contrastColorForColor(color: Color): Color {
    val luminance = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
    return if (luminance > 0.6f) Color.Black else Color.White
}

fun toMinutes(time: String): Int? {
    val parts = time.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: return null
    val m = parts.getOrNull(1)?.toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    return h * 60 + m
}

fun addMinutes(time: String, minutes: Int): String {
    val base = toMinutes(time) ?: return "08:45"
    val total = (base + minutes).coerceAtMost(23 * 60 + 59)
    return "%02d:%02d".format(total / 60, total % 60)
}

fun currentMinutes(): Int {
    val now = LocalTime.now()
    return now.hour * 60 + now.minute
}

fun currentSecondOfDay(): Int {
    val now = LocalTime.now()
    return now.hour * 3600 + now.minute * 60 + now.second
}

fun dateStringToWeekday(date: String): Weekday? = runCatching {
    Weekday.entries[LocalDate.parse(date).dayOfWeek.value - 1]
}.getOrNull()

fun todayWeekday(): Weekday =
    dateStringToWeekday(todayDateString()) ?: Weekday.MONDAY

fun todayDateString(): String = LocalDate.now().toString()

fun formatDateLabel(date: String): String = runCatching {
    val d = LocalDate.parse(date)
    "${d.monthValue}月${d.dayOfMonth}日"
}.getOrElse { date }

fun formatDateWithWeekday(date: String): String {
    val w = dateStringToWeekday(date) ?: return formatDateLabel(date)
    return "${formatDateLabel(date)} ${weekdayLabel(w)}"
}

fun addDays(date: String, days: Int): String = runCatching {
    LocalDate.parse(date).plusDays(days.toLong()).toString()
}.getOrElse { date }

fun effectiveWeekdayOnDate(
    date: String,
    adjustments: List<ScheduleAdjustment>
): Weekday? {
    val actual = dateStringToWeekday(date) ?: return null
    val adj = adjustments.firstOrNull { adjustment ->
        date >= adjustment.startDate && date <= adjustment.endDate
    } ?: return actual

    return when (actual) {
        adj.fromWeekday -> adj.toWeekday
        adj.toWeekday -> adj.fromWeekday
        else -> actual
    }
}

fun effectiveEventOnDate(event: ScheduleEvent, date: String): ScheduleEvent? {
    if (!event.enabled) return null

    event.transfers.firstOrNull { it.toDate == date }?.let {
        return event.copy(startTime = it.toStartTime, endTime = it.toEndTime)
    }

    if (event.transfers.any { it.fromDate == date }) return null

    val weekday = dateStringToWeekday(date) ?: return null
    return if (weekday in event.weekdays) event else null
}

fun eventsOnDate(
    events: List<ScheduleEvent>,
    adjustments: List<ScheduleAdjustment>,
    date: String
): List<ScheduleEvent> {
    val effectiveWeekday = effectiveWeekdayOnDate(date, adjustments) ?: return emptyList()
    return events
        .filter { it.enabled }
        .mapNotNull { event ->
            event.transfers.firstOrNull { it.toDate == date }?.let {
                return@mapNotNull event.copy(startTime = it.toStartTime, endTime = it.toEndTime)
            }
            if (event.transfers.any { it.fromDate == date }) return@mapNotNull null
            if (effectiveWeekday in event.weekdays) event else null
        }
        .sortedBy { toMinutes(it.startTime) ?: Int.MAX_VALUE }
}

fun findCurrentEventOnDate(
    events: List<ScheduleEvent>,
    adjustments: List<ScheduleAdjustment>,
    date: String,
    nowMinutes: Int
): ScheduleEvent? = eventsOnDate(events, adjustments, date).firstOrNull { e ->
    val s = toMinutes(e.startTime) ?: return@firstOrNull false
    val t = toMinutes(e.endTime) ?: return@firstOrNull false
    nowMinutes in s until t
}