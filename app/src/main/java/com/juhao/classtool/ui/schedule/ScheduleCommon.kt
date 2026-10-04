package com.juhao.classtool.ui.schedule

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.wear.compose.material3.*
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.*
import com.juhao.classtool.datastore.ScheduleAdjustment
import com.juhao.classtool.datastore.ScheduleEvent
import com.juhao.classtool.datastore.ScheduleEventType
import com.juhao.classtool.datastore.Weekday
import com.juhao.classtool.utils.*
import java.time.LocalDate
import java.time.LocalTime
val paletteColors = listOf(
    "#EF5350", "#EC407A", "#AB47BC", "#7E57C2",
    "#5C6BC0", "#42A5F5", "#26C6DA", "#26A69A",
    "#66BB6A", "#9CCC65", "#D4E157", "#FFCA28",
    "#FFA726", "#FF7043", "#A1887F", "#78909C",
    "#FF80AB", "#40E0D0", "#B2FF59", "#FFE082"
)

const val PREP_BELL_SECONDS = 180
const val FINAL_SPRINT_SECONDS = 180
const val URGENT_FINAL_SECONDS = 600

private val WORKDAYS_FOR_LABEL = setOf(
    Weekday.MONDAY,
    Weekday.TUESDAY,
    Weekday.WEDNESDAY,
    Weekday.THURSDAY,
    Weekday.FRIDAY
)

private val WEEKEND_FOR_LABEL = setOf(
    Weekday.SATURDAY,
    Weekday.SUNDAY
)

fun weekdayScopeLabel(days: Set<Weekday>): String = when {
    days.isEmpty() -> "未设置"
    days == WORKDAYS_FOR_LABEL -> "工作日"
    days == WEEKEND_FOR_LABEL -> "周末"
    days.size == 7 -> "每天"
    else -> days
        .sortedBy { it.ordinal }
        .joinToString("") { weekdayShortLabel(it) }
}

@Composable
fun ProgressFillCard(
    modifier: Modifier = Modifier,
    transformation: SurfaceTransformation? = null,
    title: String,
    timeRange: String,
    progress: Float = 0f,
    progressLabel: String? = null,
    progressColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    isEnded: Boolean = false,
    onClick: () -> Unit = {}
) {
    val safeProgress = progress.coerceIn(0f, 1f)

    val progressTextColor = contrastColorForColor(progressColor)

    Card(
        onClick = onClick,
        transformation = transformation,
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (isEnded) 0.6f else 1f),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .background(lerp(MaterialTheme.colorScheme.background, progressColor, 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(safeProgress)
                    .background(progressColor)
            )

            ProgressFillCardContent(
                title = title,
                timeRange = timeRange,
                pointColor = progressColor,
                progressLabel = progressLabel,
                textColor = contentColor,
                modifier = Modifier.fillMaxWidth()
            )

            if (safeProgress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawWithContent {
                            val progressX =
                                size.width * safeProgress

                            drawContext.canvas.save()

                            drawContext.canvas.clipRect(
                                0f,
                                0f,
                                progressX,
                                size.height
                            )

                            drawContent()

                            drawContext.canvas.restore()
                        }
                ) {
                    ProgressFillCardContent(
                        title = title,
                        timeRange = timeRange,
                        progressLabel = progressLabel,
                        pointColor = progressColor,
                        textColor = progressTextColor,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressFillCardContent(
    title: String,
    timeRange: String,
    progressLabel: String?,
    textColor: Color,
    pointColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = Modifier.padding(
            horizontal = 14.dp,
            vertical = 10.dp
        )
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
    
            Text(
                text = timeRange,
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
    
            if (progressLabel != null) {
                Spacer(Modifier.width(8.dp))
    
                Text(
                    text = progressLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = textColor,
                    maxLines = 1
                )
            }
        }
        
        Box(
            Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(pointColor)
        )
    }
}

@Composable
fun WearTimePicker(
    initial: String,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit
) {
    val hour = initial.substringBefore(":").toIntOrNull() ?: 8
    val minute = initial.substringAfter(":", "").toIntOrNull() ?: 0

    BackHandler { onCancel() }

    TimePicker(
        initialTime = LocalTime.of(hour, minute),
        onTimePicked = { onConfirm("%02d:%02d".format(it.hour, it.minute)) },
        timePickerType = TimePickerType.HoursMinutes24H
    )
}

@Composable
fun WearDatePicker(
    initial: String,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit
) {
    val initialDate = runCatching {
        LocalDate.parse(initial)
    }.getOrElse {
        LocalDate.now()
    }

    BackHandler {
        onCancel()
    }

    DatePicker(
        initialDate = initialDate,
        onDatePicked = { date ->
            onConfirm(date.toString())
        },
        datePickerType = DatePickerType.YearMonthDay
    )
}