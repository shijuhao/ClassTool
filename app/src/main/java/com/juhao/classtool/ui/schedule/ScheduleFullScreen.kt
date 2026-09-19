package com.juhao.classtool.ui.schedule

import androidx.compose.foundation.background
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

@Composable
fun ScheduleFullScreen() {
    val context = LocalContext.current
    val store = remember { ScheduleDataStore(context) }

    var schedule by remember { mutableStateOf(emptyList<ScheduleEvent>()) }
    var nowMinutes by remember { mutableStateOf(currentMinutes()) }

    LaunchedEffect(Unit) {
        while (true) {
            schedule = store.getSchedule().events
            nowMinutes = currentMinutes()
            delay(10_000L)
        }
    }

    val today = todayWeekday()
    val currentEvent = schedule.firstOrNull { event ->
        event.weekday == today &&
            toMinutes(event.startTime)?.let { nowMinutes >= it } == true &&
            toMinutes(event.endTime)?.let { nowMinutes < it } == true
    }

    val progress = if (currentEvent != null) {
        val start = toMinutes(currentEvent.startTime)
        val end = toMinutes(currentEvent.endTime)
        if (start != null && end != null && end > start) {
            ((nowMinutes - start).toFloat() / (end - start).toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    } else {
        0f
    }

    val eventColor = currentEvent?.courseColor?.let { parseColor(it) }
        ?: MaterialTheme.colorScheme.primary

    ScreenScaffold {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(eventColor.copy(alpha = 0.2f))
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            if (currentEvent != null) {
                CircularProgressIndicator(
                    progress = { progress },
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
                }
            } else {
                Text(
                    text = "当前没有事件",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}