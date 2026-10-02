package com.juhao.classtool

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.juhao.classtool.datastore.ScheduleDataStore
import com.juhao.classtool.ui.schedule.effectiveWeekdayOnDate
import com.juhao.classtool.ui.schedule.eventsOnDate
import com.juhao.classtool.ui.schedule.findCurrentEventOnDate
import com.juhao.classtool.ui.schedule.parseColor
import com.juhao.classtool.ui.schedule.toMinutes
import com.juhao.classtool.ui.schedule.todayDateString
import com.juhao.classtool.ui.schedule.todayWeekday
import com.juhao.classtool.ui.schedule.currentSecondOfDay
import kotlin.time.Duration.Companion.milliseconds

data class DynamicThemeState(
    val color: Color? = null,
    val urgent: Boolean = false
)

@Composable
fun rememberDynamicThemeState(
    enabled: Boolean,
    context: Context
): DynamicThemeState {
    var state by remember(enabled) { mutableStateOf(DynamicThemeState()) }

    LaunchedEffect(enabled) {
        if (!enabled) {
            state = DynamicThemeState()
            return@LaunchedEffect
        }

        val store = ScheduleDataStore(context)
        while (true) {
            val schedule = store.getSchedule()
            val date = todayDateString()
            val displayWeekday = effectiveWeekdayOnDate(
                date,
                schedule.adjustments
            ) ?: todayWeekday()
            val nowSecond = currentSecondOfDay()
            val nowMinute = nowSecond / 60
            val active = findCurrentEventOnDate(
                schedule.events,
                schedule.adjustments,
                date,
                nowMinute
            )

            val color = active
                ?.let { it.courseColorByWeekday[displayWeekday] ?: it.courseColor }
                ?.let(::parseColor)

            val remaining = active?.let {
                (toMinutes(it.endTime) ?: Int.MAX_VALUE) - nowMinute
            } ?: Int.MAX_VALUE

            state = DynamicThemeState(
                color = color,
                urgent = active?.urgent == true && remaining in 0..10
            )

            val nowSecondLong = nowSecond.toLong()
            val nextBoundary = eventsOnDate(
                schedule.events,
                schedule.adjustments,
                date
            )
                .asSequence()
                .flatMap { event ->
                    val start = toMinutes(event.startTime)?.toLong()?.times(60L)
                    val end = toMinutes(event.endTime)?.toLong()?.times(60L)
                    sequenceOf(start, end, end?.minus(600L))
                }
                .filterNotNull()
                .filter { it > nowSecondLong }
                .minOrNull()

            val sleepSeconds = nextBoundary
                ?.minus(nowSecondLong)
                ?.coerceAtLeast(1L)
                ?: (86400L - nowSecondLong).coerceAtLeast(60L)

            kotlinx.coroutines.delay((sleepSeconds * 1000L).milliseconds)
        }
    }

    return state
}
