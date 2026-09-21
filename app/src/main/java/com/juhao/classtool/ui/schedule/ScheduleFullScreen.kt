package com.juhao.classtool.ui.schedule

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        schedule = store.getSchedule().events
        while (true) {
            nowSecondOfDay = currentSecondOfDay()
            delay(1000L)
        }
    }

    val today = todayWeekday()
    val nowMinutes = nowSecondOfDay / 60

    val currentEvent = remember(schedule, today, nowMinutes) {
        schedule.firstOrNull { event ->
            event.weekday == today &&
                toMinutes(event.startTime)?.let { nowMinutes >= it } == true &&
                toMinutes(event.endTime)?.let { nowMinutes < it } == true
        }
    }

    val startSec = remember(currentEvent?.id) {
        currentEvent?.let { toMinutes(it.startTime)?.times(60) }
    }
    val endSec = remember(currentEvent?.id) {
        currentEvent?.let { toMinutes(it.endTime)?.times(60) }
    }

    val targetProgress = if (startSec != null && endSec != null && endSec > startSec) {
        ((nowSecondOfDay - startSec).toFloat() / (endSec - startSec).toFloat())
            .coerceIn(0f, 1f)
    } else {
        0f
    }

    val progressAnim = remember { Animatable(0f) }

    LaunchedEffect(currentEvent?.id) {
        if (currentEvent == null) {
            progressAnim.snapTo(0f)
        }
    }

    LaunchedEffect(targetProgress) {
        if (currentEvent != null) {
            progressAnim.snapTo(targetProgress)
        }
    }

    val remainingSec = if (endSec != null) endSec - nowSecondOfDay else null
    val isFinalPart = remainingSec != null && remainingSec in 1..180

    val finalPartProgress by animateFloatAsState(
        targetValue = if (isFinalPart) 1f else 0f,
        animationSpec = tween(400),
        label = "finalPart"
    )

    val displaySize = 32f * (1f - 0.4f * finalPartProgress)
    val titleSize = 20f * (1f - 0.2f * finalPartProgress)
    val mediumSize = 16f * (1f + 1.5f * finalPartProgress)

    val eventColor = currentEvent?.courseColor?.let { parseColor(it) }
        ?: MaterialTheme.colorScheme.primary

    ScreenScaffold {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(CircularProgressIndicatorDefaults.FullScreenPadding),
            contentAlignment = Alignment.Center
        ) {
            val ev = currentEvent
            if (ev != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = ev.courseName
                            ?: when (ev.type) {
                                ScheduleEventType.BREAK -> "课间休息"
                                ScheduleEventType.ACTIVITY -> "活动"
                                ScheduleEventType.CLASS -> "未命名"
                            },
                        style = TextStyle(
                            fontSize = displaySize.sp,
                            lineHeight = (displaySize * 1.2f).sp,
                            fontWeight = FontWeight.Normal
                        ),
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
                        text = "${ev.startTime} - ${ev.endTime}",
                        style = TextStyle(
                            fontSize = titleSize.sp,
                            lineHeight = (titleSize * 1.2f).sp
                        ),
                        textAlign = TextAlign.Center
                    )
                    if (remainingSec != null) {
                        Spacer(Modifier.height(2.dp))
                        val remainingText = if (remainingSec > 0) {
                            if (isFinalPart) {
                                "%02d:%02d".format(remainingSec / 60, remainingSec % 60)
                            } else {
                                "剩余 %02d:%02d".format(remainingSec / 60, remainingSec % 60)
                            }
                        } else {
                            "剩余 00:00"
                        }
                        Text(
                            text = remainingText,
                            style = TextStyle(
                                fontSize = mediumSize.sp,
                                lineHeight = (mediumSize * 1.2f).sp
                            ),
                            textAlign = TextAlign.Center,
                            color = eventColor
                        )
                    }
                }
                CircularProgressIndicator(
                    progress = { progressAnim.value },
                    modifier = Modifier.fillMaxSize(),
                    startAngle = 300f,
                    endAngle = 240f,
                    colors = ProgressIndicatorDefaults.colors(
                        trackColor = eventColor.copy(alpha = 0.4f),
                        indicatorColor = eventColor,
                    )
                )
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