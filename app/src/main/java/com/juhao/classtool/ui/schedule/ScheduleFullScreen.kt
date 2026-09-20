package com.juhao.classtool.ui.schedule

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.*
import com.juhao.classtool.datastore.ScheduleDataStore
import com.juhao.classtool.datastore.ScheduleEvent
import com.juhao.classtool.datastore.ScheduleEventType
import kotlinx.coroutines.delay
import java.time.LocalTime

@Composable
fun ScheduleFullScreen() {
    val context = LocalContext.current
    val store = remember { ScheduleDataStore(context) }

    var schedule by remember { mutableStateOf(emptyList<ScheduleEvent>()) }
    var nowSecondOfDay by remember { mutableIntStateOf(currentSecondOfDay()) }

    LaunchedEffect(Unit) {
        while (true) {
            schedule = store.getSchedule().events
            nowSecondOfDay = currentSecondOfDay()
            delay(1000L)
        }
    }

    val nowMinutes = nowSecondOfDay / 60
    val today = todayWeekday()

    val currentEvent = schedule.firstOrNull { event ->
        event.weekday == today &&
            toMinutes(event.startTime)?.let { nowMinutes >= it } == true &&
            toMinutes(event.endTime)?.let { nowMinutes < it } == true
    }

    val targetProgress = if (currentEvent != null) {
        val startSec = toMinutes(currentEvent.startTime)?.times(60)
        val endSec = toMinutes(currentEvent.endTime)?.times(60)
        if (startSec != null && endSec != null && endSec > startSec) {
            ((nowSecondOfDay - startSec).toFloat() / (endSec - startSec).toFloat())
                .coerceIn(0f, 1f)
        } else {
            0f
        }
    } else {
        0f
    }

    val progressAnim = remember { Animatable(0f) }

    LaunchedEffect(currentEvent?.id) {
        val ev = currentEvent
        if (ev == null) {
            progressAnim.snapTo(0f)
        }
    }

    LaunchedEffect(targetProgress) {
        if (currentEvent != null) {
            progressAnim.snapTo(targetProgress)
        }
    }

    val remainingText = if (currentEvent != null) {
        val end = toMinutes(currentEvent.endTime)
        if (end != null) {
            val remainingSec = end * 60 - nowSecondOfDay
            if (remainingSec > 0) {
                "剩余 %02d:%02d".format(remainingSec / 60, remainingSec % 60)
            } else {
                "剩余 00:00"
            }
        } else {
            null
        }
    } else {
        null
    }

    val eventColor = currentEvent?.courseColor?.let { parseColor(it) }
        ?: MaterialTheme.colorScheme.primary

    ScreenScaffold {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            if (currentEvent != null) {
                CircularProgressIndicator(
                    progress = { progressAnim.value },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 10.dp,
                    startAngle = 120f,
                    endAngle = 60f,
                    colors = ProgressIndicatorDefaults.colors(
                        trackColor = eventColor.copy(alpha = 0.2f),
                        indicatorColor = eventColor,
                    )
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = currentEvent.courseName
                            ?: when (currentEvent.type) {
                                ScheduleEventType.BREAK -> "课间休息"
                                ScheduleEventType.ACTIVITY -> "活动"
                                ScheduleEventType.CLASS -> "未命名"
                            },
                        style = MaterialTheme.typography.displayMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                repeatDelayMillis = 1000,
                                velocity = 30.dp
                            )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${currentEvent.startTime} - ${currentEvent.endTime}",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    if (remainingText != null) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = remainingText,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            color = eventColor
                        )
                    }
                }
            } else {
                Text(
                    text = "当前没有事件",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun currentSecondOfDay(): Int {
    val now = LocalTime.now()
    return now.hour * 3600 + now.minute * 60 + now.second
}