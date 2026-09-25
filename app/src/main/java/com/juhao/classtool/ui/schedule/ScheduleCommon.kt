package com.juhao.classtool.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.*
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.*
import com.juhao.classtool.datastore.ScheduleEvent
import com.juhao.classtool.datastore.ScheduleEventType
import com.juhao.classtool.datastore.Weekday
import java.time.LocalDate
import java.time.LocalTime
import androidx.core.graphics.toColorInt

fun weekdayLabel(weekday: Weekday): String = when (weekday) {
    Weekday.MONDAY -> "周一"
    Weekday.TUESDAY -> "周二"
    Weekday.WEDNESDAY -> "周三"
    Weekday.THURSDAY -> "周四"
    Weekday.FRIDAY -> "周五"
    Weekday.SATURDAY -> "周六"
    Weekday.SUNDAY -> "周日"
}

fun weekdayShortLabel(weekday: Weekday): String = when (weekday) {
    Weekday.MONDAY -> "一"
    Weekday.TUESDAY -> "二"
    Weekday.WEDNESDAY -> "三"
    Weekday.THURSDAY -> "四"
    Weekday.FRIDAY -> "五"
    Weekday.SATURDAY -> "六"
    Weekday.SUNDAY -> "日"
}

fun eventDisplayName(event: ScheduleEvent): String = event.courseName
    ?: when (event.type) {
        ScheduleEventType.BREAK -> "课间休息"
        ScheduleEventType.ACTIVITY -> "活动"
        ScheduleEventType.CLASS -> "未命名"
    }

fun parseColor(hex: String): Color = runCatching {
    Color(hex.toColorInt())
}.getOrElse { Color.DarkGray }

fun contrastColorFor(hex: String): Color {
    val c = parseColor(hex)
    val luminance = 0.299f * c.red + 0.587f * c.green + 0.114f * c.blue
    return if (luminance > 0.6f) Color.Black else Color.White
}

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

fun currentSecondOfDay(): Int {
    val now = LocalTime.now()
    return now.hour * 3600 + now.minute * 60 + now.second
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

val paletteColors = listOf(
    "#EF5350", "#EC407A", "#AB47BC", "#7E57C2",
    "#5C6BC0", "#42A5F5", "#26C6DA", "#26A69A",
    "#66BB6A", "#9CCC65", "#D4E157", "#FFCA28",
    "#FFA726", "#FF7043", "#A1887F", "#78909C",
    "#FF80AB", "#40E0D0", "#B2FF59", "#FFE082"
)

@Composable
fun CustomPresetDialog(
    title: String,
    placeholder: String,
    initialName: String,
    initialColor: String?,
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit
) {
    var customName by remember { mutableStateOf(initialName) }
    var customColor by remember { mutableStateOf(initialColor) }

    AlertDialog(
        visible = true,
        onDismissRequest = onDismiss,
        title = { Text(title) },
        confirmButton = {
            AlertDialogDefaults.ConfirmButton(
                onClick = {
                    if (customName.isNotBlank()) {
                        onConfirm(customName.trim(), customColor)
                    }
                }
            )
        },
        dismissButton = {
            AlertDialogDefaults.DismissButton(onClick = onDismiss)
        }
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = "名称")
                Spacer(Modifier.height(4.dp))
                BasicTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(
                        MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (customName.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }

        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(text = "颜色")
                Spacer(Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    paletteColors.forEach { hex ->
                        val selected = customColor == hex
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(parseColor(hex))
                                .clickable {
                                    customColor = if (selected) null else hex
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = MaterialSymbols.Rounded.Check,
                                    contentDescription = null,
                                    tint = contrastColorFor(hex),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}