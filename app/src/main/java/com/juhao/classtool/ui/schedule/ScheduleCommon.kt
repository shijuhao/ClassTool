package com.juhao.classtool.ui.schedule

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

fun eventDisplayNameFor(event: ScheduleEvent, weekday: Weekday): String {
    val name = event.courseNameByWeekday[weekday] ?: event.courseName
    return name ?: when (event.type) {
        ScheduleEventType.BREAK -> "课间休息"
        ScheduleEventType.ACTIVITY -> "活动"
        ScheduleEventType.CLASS -> "未命名"
    }
}

fun eventColorFor(event: ScheduleEvent, weekday: Weekday): Color {
    val hex = event.courseColorByWeekday[weekday] ?: event.courseColor
    return hex?.let { parseColor(it) } ?: Color.Unspecified
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

fun addMinutes(time: String, minutes: Int): String {
    val base = toMinutes(time) ?: return "08:45"
    val total = (base + minutes).coerceAtMost(23 * 60 + 59)
    return "%02d:%02d".format(total / 60, total % 60)
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

fun todayDateString(): String = LocalDate.now().toString()

fun dateStringToWeekday(date: String): Weekday? {
    return runCatching {
        when (LocalDate.parse(date).dayOfWeek.value) {
            1 -> Weekday.MONDAY
            2 -> Weekday.TUESDAY
            3 -> Weekday.WEDNESDAY
            4 -> Weekday.THURSDAY
            5 -> Weekday.FRIDAY
            6 -> Weekday.SATURDAY
            else -> Weekday.SUNDAY
        }
    }.getOrNull()
}

fun formatDateLabel(date: String): String {
    return runCatching {
        val d = LocalDate.parse(date)
        "${d.monthValue}月${d.dayOfMonth}日"
    }.getOrElse { date }
}

fun formatDateWithWeekday(date: String): String {
    val w = dateStringToWeekday(date) ?: return formatDateLabel(date)
    return "${formatDateLabel(date)} ${weekdayLabel(w)}"
}

fun addDays(date: String, days: Int): String {
    return runCatching {
        LocalDate.parse(date).plusDays(days.toLong()).toString()
    }.getOrElse { date }
}

fun effectiveWeekdayOnDate(
    date: String,
    adjustments: List<ScheduleAdjustment>
): Weekday? {
    val actual = dateStringToWeekday(date) ?: return null
    val adj = adjustments.firstOrNull { adjustment ->
        date >= adjustment.startDate && date <= adjustment.endDate
    } ?: return actual

    return when (actual) {
        adj.fromWeekday -> adj.toWeekday
        adj.toWeekday -> adj.fromWeekday
        else -> actual
    }
}

fun isEventActiveOnDate(event: ScheduleEvent, date: String): Boolean {
    if (!event.enabled) return false
    if (event.transfers.any { it.fromDate == date }) return false
    val weekday = dateStringToWeekday(date) ?: return false
    return weekday in event.weekdays
}

fun isEventTransferredInOnDate(event: ScheduleEvent, date: String): Boolean {
    return event.transfers.any { it.toDate == date }
}

fun effectiveEventOnDate(event: ScheduleEvent, date: String): ScheduleEvent? {
    if (!event.enabled) return null

    val incoming = event.transfers.firstOrNull { it.toDate == date }
    if (incoming != null) {
        return event.copy(
            startTime = incoming.toStartTime,
            endTime = incoming.toEndTime
        )
    }

    if (event.transfers.any { it.fromDate == date }) return null

    val weekday = dateStringToWeekday(date) ?: return null
    if (weekday !in event.weekdays) return null
    return event
}

fun eventsOnDate(
    events: List<ScheduleEvent>,
    adjustments: List<ScheduleAdjustment>,
    date: String
): List<ScheduleEvent> {
    val effectiveWeekday = effectiveWeekdayOnDate(date, adjustments) ?: return emptyList()
    val result = mutableListOf<ScheduleEvent>()

    for (event in events) {
        if (!event.enabled) continue
        if (event.transfers.any { it.fromDate == date }) continue

        val incoming = event.transfers.firstOrNull { it.toDate == date }
        if (incoming != null) {
            result.add(
                event.copy(
                    startTime = incoming.toStartTime,
                    endTime = incoming.toEndTime
                )
            )
            continue
        }

        if (effectiveWeekday in event.weekdays) {
            result.add(event)
        }
    }

    return result.sortedBy { toMinutes(it.startTime) ?: Int.MAX_VALUE }
}

fun eventsForWeekday(
    events: List<ScheduleEvent>,
    adjustments: List<ScheduleAdjustment>,
    weekday: Weekday,
    date: String
): List<ScheduleEvent> {
    val effectiveWeekday = effectiveWeekdayOnDate(date, adjustments) ?: return emptyList()
    val result = mutableListOf<ScheduleEvent>()

    for (event in events) {
        if (!event.enabled) continue

        val incoming = event.transfers.firstOrNull { it.toDate == date }
        if (incoming != null) {
            result.add(
                event.copy(
                    startTime = incoming.toStartTime,
                    endTime = incoming.toEndTime
                )
            )
            continue
        }

        if (event.transfers.any { it.fromDate == date }) continue

        if (effectiveWeekday in event.weekdays) {
            result.add(event)
        }
    }

    return result.sortedBy { toMinutes(it.startTime) ?: Int.MAX_VALUE }
}

fun upcomingEventsForDate(
    events: List<ScheduleEvent>,
    adjustments: List<ScheduleAdjustment>,
    date: String,
    nowMinutes: Int
): List<ScheduleEvent> {
    return eventsOnDate(events, adjustments, date)
        .filter { it.type != ScheduleEventType.BREAK }
        .filter { (toMinutes(it.startTime) ?: Int.MAX_VALUE) > nowMinutes }
        .sortedBy { toMinutes(it.startTime) ?: Int.MAX_VALUE }
}

fun findCurrentEventOnDate(
    events: List<ScheduleEvent>,
    adjustments: List<ScheduleAdjustment>,
    date: String,
    nowMinutes: Int
): ScheduleEvent? {
    return eventsOnDate(events, adjustments, date).firstOrNull { e ->
        val s = toMinutes(e.startTime) ?: return@firstOrNull false
        val t = toMinutes(e.endTime) ?: return@firstOrNull false
        nowMinutes in s until t
    }
}

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

val funnyMessagesFar = listOf(
    "稳如老狗" to "(￣▽￣)",
    "时间还早，摸会儿鱼" to "( ˘ω˘ )",
    "一切尽在掌握" to "(๑•̀ㅂ•́)و"
)

val funnyMessagesMid = listOf(
    "撑住，过半了" to "(ง •_•)ง",
    "还有一阵，别慌" to "(´･ω･`)",
    "保持节奏" to "( •̀ ω •́ )"
)

val funnyMessagesNear = listOf(
    "快下课了，加把劲" to "٩(๑•̀ω•́๑)۶",
    "胜利就在前方" to "(๑•̀ㅂ•́)و✧",
    "再坚持一会儿" to "(｡•̀ᴗ-)✧"
)

val funnyMessagesFinal = listOf(
    "最后冲刺！" to "ヽ(•̀ω•́ )ゝ",
    "马上结束！" to "(ﾉ>ω<)ﾉ",
    "冲鸭！" to "ヾ(≧▽≦*)o"
)

val funnyMessagesPrep = listOf(
    "预备铃响啦，准备上课" to "🔔(•̀ᴗ•́)و",
    "要上课了，收收心" to "(๑•́ ₃ •̀๑)",
    "预备！" to "⏰(ง •̀_•́)ง"
)

val funnyMessagesBreak = listOf(
    "课间休息，活动一下" to "☕(´▽`)",
    "喝口水，放松放松" to "🥤( ˘ω˘ )",
    "下课啦，随便逛逛" to "🐾(￣▽￣)"
)

val funnyMessagesIdle = listOf(
    "摸鱼时间到" to "🐟(￣▽￣)",
    "自由活动，随便浪" to "( ˘ω˘ )",
    "闲着也是闲着" to "(´･ω･`)"
)

fun funnyPool(tier: String): List<Pair<String, String>> = when (tier) {
    "prep" -> funnyMessagesPrep
    "break" -> funnyMessagesBreak
    "idle" -> funnyMessagesIdle
    "final" -> funnyMessagesFinal
    "near" -> funnyMessagesNear
    "mid" -> funnyMessagesMid
    else -> funnyMessagesFar
}

private val WORKDAYS_FOR_LABEL = setOf(
    Weekday.MONDAY, Weekday.TUESDAY, Weekday.WEDNESDAY, Weekday.THURSDAY, Weekday.FRIDAY
)
private val WEEKEND_FOR_LABEL = setOf(Weekday.SATURDAY, Weekday.SUNDAY)

fun weekdayScopeLabel(days: Set<Weekday>): String = when {
    days.isEmpty() -> "未设置"
    days == WORKDAYS_FOR_LABEL -> "工作日"
    days == WEEKEND_FOR_LABEL -> "周末"
    days.size == 7 -> "每天"
    else -> days.sortedBy { it.ordinal }.joinToString("") { weekdayShortLabel(it) }
}

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

@Composable
fun ScheduleEventCard(
    modifier: Modifier = Modifier,
    transformation: SurfaceTransformation? = null,
    event: ScheduleEvent,
    weekday: Weekday? = null,
    highlighted: Boolean = false,
    showWeekdayBadge: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val name = if (weekday != null) {
        event.courseNameByWeekday[weekday] ?: event.courseName
    } else {
        event.courseName
    }
    val colorHex = if (weekday != null) {
        event.courseColorByWeekday[weekday] ?: event.courseColor
    } else {
        event.courseColor
    }
    val dotColor = colorHex?.let { parseColor(it) } ?: MaterialTheme.colorScheme.onSurface
    val displayName = name ?: when (event.type) {
        ScheduleEventType.BREAK -> "课间休息"
        ScheduleEventType.ACTIVITY -> "活动"
        ScheduleEventType.CLASS -> "未命名"
    }

    FilledTonalButton(
        onClick = onClick,
        onLongClick = onLongClick,
        transformation = transformation,
        label = { Text(displayName) },
        secondaryLabel = {
            val time = "${event.startTime} - ${event.endTime}"
            val suffix = if (showWeekdayBadge) "  ${weekdayScopeLabel(event.weekdays)}" else ""
            Text("$time$suffix")
        },
        icon = {
            Box(
                Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(dotColor)
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (highlighted) Modifier.border(
                    2.dp,
                    MaterialTheme.colorScheme.primaryContainer,
                    RoundedCornerShape(50.dp)
                ) else Modifier
            )
    )
}

@Composable
fun WearTimePicker(
    initial: String,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit
) {
    val parts = initial.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

    BackHandler { onCancel() }

    TimePicker(
        initialTime = LocalTime.of(hour, minute),
        onTimePicked = { time -> onConfirm("%02d:%02d".format(time.hour, time.minute)) },
        timePickerType = TimePickerType.HoursMinutes24H
    )
}

@Composable
fun WearDatePicker(
    initial: String,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit
) {
    val initialDate = runCatching { LocalDate.parse(initial) }
        .getOrElse { LocalDate.now() }

    BackHandler { onCancel() }

    DatePicker(
        initialDate = initialDate,
        onDatePicked = { date -> onConfirm(date.toString()) },
        datePickerType = DatePickerType.YearMonthDay
    )
}