package com.juhao.classtool

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.juhao.classtool.datastore.Schedule
import com.juhao.classtool.datastore.ScheduleDataStore
import com.juhao.classtool.ui.schedule.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.seconds

data class DynamicThemeState(val color: Color? = null, val urgent: Boolean = false)

private const val URGENT_WINDOW = 10
private const val DAY_SECONDS = 86_400L

@Composable
fun rememberDynamicThemeState(enabled: Boolean, context: Context): DynamicThemeState {
    var state by remember(enabled) { mutableStateOf(DynamicThemeState()) }
    LaunchedEffect(enabled, context) {
        if (!enabled) { state = DynamicThemeState(); return@LaunchedEffect }
        ScheduleDataStore(context).scheduleFlow.distinctUntilChanged()
            .collectLatest { runDynamicThemeLoop(it) { s -> state = s } }
    }
    return state
}

private suspend fun runDynamicThemeLoop(schedule: Schedule, onUpdate: (DynamicThemeState) -> Unit) {
    while (currentCoroutineContext().isActive) {
        val date = todayDateString()
        val nowSec = currentSecondOfDay().toLong()
        val nowMin = nowSec / 60
        val active = findCurrentEventOnDate(schedule.events, schedule.adjustments, date, nowMin.toInt())
        val weekday = effectiveWeekdayOnDate(date, schedule.adjustments) ?: dateStringToWeekday(date)
        val endMin = active?.endTime?.let(::toMinutes)

        onUpdate(DynamicThemeState(
            color = active?.let { weekday?.let { w -> eventColorFor(it, w) } },
            urgent = active?.urgent == true && endMin != null && (endMin - nowMin) in 0..URGENT_WINDOW.toLong()
        ))

        delay(nextSleepSeconds(schedule, date, nowSec).seconds)
    }
}

private fun nextSleepSeconds(schedule: Schedule, date: String, nowSec: Long): Long {
    val next = eventsOnDate(schedule.events, schedule.adjustments, date)
        .asSequence()
        .flatMap {
            val s = toMinutes(it.startTime)?.toLong()?.times(60L)
            val t = toMinutes(it.endTime)?.toLong()?.times(60L)
            sequenceOf(s, t, t?.minus(URGENT_WINDOW * 60L))
        }
        .filterNotNull()
        .filter { it > nowSec }
        .minOrNull()
    return next?.minus(nowSec)?.coerceAtLeast(1L) ?: (DAY_SECONDS - nowSec).coerceAtLeast(60L)
}