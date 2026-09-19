package com.juhao.classtool.ui.schedule

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
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
    onClick: () -> Unit = {}
) {
    val color = event.courseColor?.let { parseColor(it) } ?: MaterialTheme.colorScheme.onSurface
    
    val animatedProgress by animateFloatAsState(
        targetValue = if (highlighted) progress.coerceIn(0f, 1f) else 0f,
        animationSpec = tween(500),
        label = "progress"
    )
    
    val baseColor = if (highlighted) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val progressColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    val contentColor = if (highlighted) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    
    FilledTonalButton(
        onClick = onClick,
        transformation = transformation,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = Color.Transparent,
            contentColor = contentColor
        ),
        label = {
            Text(
                text = event.courseName ?: when (event.type) {
                    ScheduleEventType.BREAK -> "课间休息"
                    ScheduleEventType.ACTIVITY -> "活动"
                    ScheduleEventType.CLASS -> "未命名"
                }
            )
        },
        secondaryLabel = {
            Text(text = "${event.startTime} - ${event.endTime}")
        },
        icon = {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color)
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val radius = size.height / 2f
                drawRoundRect(
                    color = baseColor,
                    size = size,
                    cornerRadius = CornerRadius(radius)
                )
                if (animatedProgress > 0f) {
                    drawRoundRect(
                        color = progressColor,
                        size = Size(size.width * animatedProgress, size.height),
                        cornerRadius = CornerRadius(radius)
                    )
                }
            }
            .then(
                if (highlighted) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(50.dp)
                    )
                } else {
                    Modifier
                }
            )
    )
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