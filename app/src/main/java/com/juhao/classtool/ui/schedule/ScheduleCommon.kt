package com.juhao.classtool.ui.schedule

import com.juhao.classtool.datastore.Weekday
import java.time.LocalDate
import java.time.LocalTime
import androidx.core.graphics.toColorInt

fun weekdayLabel(weekday: Weekday): String = when (weekday) {
    Weekday.MONDAY -> "周一"
    Weekday.TUESDAY -> "周二"
    Weekday.WEDNESDAY -> "周三"
    Weekday.THURSDAY -> "周四"
    Weekday.FRIDAY -> "周五"
    Weekday.SATURDAY -> "周六"
    Weekday.SUNDAY -> "周日"
}

fun parseColor(hex: String): Color = runCatching {
    Color(hex.toColorInt())
}.getOrElse { Color.DarkGray }

fun toMinutes(time: String): Int? {
    val parts = time.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: return null
    val m = parts.getOrNull(1)?.toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    return h * 60 + m
}

fun currentMinutes(): Int {
    val now = LocalTime.now()
    return now.hour * 60 + now.minute
}

fun todayWeekday(): Weekday = when (LocalDate.now().dayOfWeek.value) {
    1 -> Weekday.MONDAY
    2 -> Weekday.TUESDAY
    3 -> Weekday.WEDNESDAY
    4 -> Weekday.THURSDAY
    5 -> Weekday.FRIDAY
    6 -> Weekday.SATURDAY
    else -> Weekday.SUNDAY
}