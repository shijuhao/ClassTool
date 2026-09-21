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
import com.juhao.classtool.datastore.SettingsDataStore
import kotlinx.coroutines.delay
import java.time.LocalTime

private const val PREP_BELL_SECONDS = 180

@Composable
fun ScheduleFullScreen() {
    val context = LocalContext.current
    val store = remember { ScheduleDataStore(context) }
    val settingsStore = remember { SettingsDataStore(context) }

    var schedule by remember { mutableStateOf(emptyList<ScheduleEvent>()) }
    var prepBellEnabled by remember { mutableStateOf(true) }
    var nowSecondOfDay by remember { mutableIntStateOf(currentSecondOfDay()) }

    LaunchedEffect(Unit) {
        schedule = store.getSchedule().events
        prepBellEnabled = settingsStore.getPrepBell()
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

    val prepEvent = remember(schedule, today, nowSecondOfDay, prepBellEnabled) {
        if (!prepBellEnabled) return@remember null
        schedule.firstOrNull { event ->
            if (event.weekday != today) return@firstOrNull false
            if (event.type == ScheduleEventType.BREAK) return@firstOrNull false
            val startSec = toMinutes(event.startTime)?.times(60) ?: return@firstOrNull false
            nowSecondOfDay in (startSec - PREP_BELL_SECONDS) until startSec
        }
    }

    val isPrep = prepEvent != null
    val displayEvent = prepEvent ?: currentEvent

    val startSec = remember(displayEvent?.id, isPrep) {
        displayEvent?.let { toMinutes(it.startTime)?.times(60) }
    }
    val endSec = remember(displayEvent?.id, isPrep) {
        displayEvent?.let { toMinutes(it.endTime)?.times(60) }
    }

    val targetProgress = if (isPrep) {
        val prepStartSec = startSec?.minus(PREP_BELL_SECONDS)
        if (prepStartSec != null && startSec != null && startSec > prepStartSec) {
            ((nowSecondOfDay - prepStartSec).toFloat() / (startSec - prepStartSec).toFloat())
                .coerceIn(0f, 1f)
        } else {
            0f
        }
    } else if (startSec != null && endSec != null && endSec > startSec) {
        ((nowSecondOfDay - startSec).toFloat() / (endSec - startSec).toFloat())
            .coerceIn(0f, 1f)
    } else {
        0f
    }

    val progressAnim = remember { Animatable(0f) }

    LaunchedEffect(displayEvent?.id, isPrep) {
        if (displayEvent == null) {
            progressAnim.snapTo(0f)
        }
    }

    LaunchedEffect(targetProgress) {
        if (displayEvent != null) {
            progressAnim.snapTo(targetProgress)
        }
    }

    val remainingSec = if (isPrep) {
        startSec?.minus(nowSecondOfDay)
    } else {
        endSec?.minus(nowSecondOfDay)
    }

    val isFinalPart = remainingSec != null && remainingSec in 1..PREP_BELL_SECONDS

    val finalPartProgress by animateFloatAsState(
        targetValue = if (isFinalPart) 1f else 0f,
        animationSpec = tween(400),
        label = "finalPart"
    )

    val displaySize = 32f * (1f - 0.4f * finalPartProgress)
    val titleSize = 20f * (1f - 0.2f * finalPartProgress)
    val mediumSize = 16f * (1f + 1.5f * finalPartProgress)

    val eventColor = displayEvent?.courseColor?.let { parseColor(it) }
        ?: MaterialTheme.colorScheme.primary

    ScreenScaffold {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(CircularProgressIndicatorDefaults.FullScreenPadding),
            contentAlignment = Alignment.Center
        ) {
            val ev = displayEvent
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
                        text = if (isPrep) "即将开始" else "${ev.startTime} - ${ev.endTime}",
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