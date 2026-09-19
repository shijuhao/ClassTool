package com.juhao.classtool.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.*
import com.juhao.classtool.datastore.ScheduleEvent
import com.juhao.classtool.datastore.ScheduleEventType
import com.juhao.classtool.datastore.Weekday
import java.time.LocalDate
import java.time.LocalTime
import androidx.core.graphics.toColorInt

@Composable
fun ScheduleEventCard(
    modifier: Modifier = Modifier,
    transformation: SurfaceTransformation? = null,
    event: ScheduleEvent,
    highlighted: Boolean = false,
    progress: Float = 0f,
    onClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val color = event.courseColor?.let { parseColor(it) } ?: MaterialTheme.colorScheme.onSurface
    val shape = RoundedCornerShape(20.dp)

    val cardModifier = modifier
        .fillMaxWidth()
        .height(IntrinsicSize.Min)
        .clip(shape)
        .background(MaterialTheme.colorScheme.surfaceContainer)
        .then(
            if (highlighted) {
                Modifier.border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = shape
                )
            } else {
                Modifier
            }
        )

    val content: @Composable () -> Unit = {
        Box(modifier = Modifier.fillMaxSize()) {
            if (highlighted) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        )
                )
                Box(modifier = Modifier.matchParentSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(color)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.courseName ?: when (event.type) {
                            ScheduleEventType.BREAK -> "课间休息"
                            ScheduleEventType.ACTIVITY -> "活动"
                            ScheduleEventType.CLASS -> "未命名"
                        }
                    )
                    Text(
                        text = "${event.startTime} - ${event.endTime}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                trailingContent?.invoke()
            }
        }
    }

    if (onClick != null) {
        Card(
            onClick = onClick,
            transformation = transformation,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(0.dp),
            modifier = cardModifier
        ) { content() }
    } else {
        Card(
            transformation = transformation,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues(0.dp),
            modifier = cardModifier
        ) { content() }
    }
}

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