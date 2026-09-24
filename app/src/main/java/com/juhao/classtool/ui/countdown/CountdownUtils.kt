package com.juhao.classtool.ui.countdown

import java.util.Calendar
fun daysUntil(targetMillis: Long): Long {
    val today = startOfDay(System.currentTimeMillis())
    val target = startOfDay(targetMillis)
    return (target - today) / (24L * 60 * 60 * 1000)
}

fun startOfDay(millis: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

fun formatDate(millis: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = millis }
    return "%04d-%02d-%02d".format(
        c.get(Calendar.YEAR),
        c.get(Calendar.MONTH) + 1,
        c.get(Calendar.DAY_OF_MONTH)
    )
}

fun countdownLabel(days: Long): String = when {
    days < 0 -> "已过 ${-days} 天"
    days == 0L -> "就是今天"
    else -> "还有 $days 天"
}