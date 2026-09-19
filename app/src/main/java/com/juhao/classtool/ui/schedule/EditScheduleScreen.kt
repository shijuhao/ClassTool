package com.juhao.classtool.ui.schedule

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.juhao.classtool.R
import com.juhao.classtool.datastore.SettingsDataStore
import com.juhao.classtool.datastore.ScheduleDataStore
import com.juhao.classtool.datastore.ScheduleEvent
import com.juhao.classtool.datastore.ScheduleEventType
import com.juhao.classtool.datastore.Weekday
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

private enum class DialogStage {
    EDIT,
    START_TIME,
    END_TIME
}

private data class PresetCourse(
    val name: String,
    val color: String?
)

private val presetCourses = listOf(
    PresetCourse("语文", "#FF5722"),
    PresetCourse("数学", "#4CAF50"),
    PresetCourse("英语", "#2196F3"),
    PresetCourse("物理", "#9C27B0"),
    PresetCourse("化学", "#FFC107"),
    PresetCourse("生物", "#607D8B"),
    PresetCourse("历史", "#795548"),
    PresetCourse("地理", "#009688"),
    PresetCourse("政治", "#E91E63"),
    PresetCourse("体育", "#FF9800")
)

private val activityPresets = listOf(
    PresetCourse("升旗", "#F44336"),
    PresetCourse("运动会", "#4CAF50"),
    PresetCourse("班会", "#3F51B5"),
    PresetCourse("社团", "#FF9800"),
    PresetCourse("大扫除", "#009688"),
    PresetCourse("考试", "#9C27B0")
)

private val paletteColors = listOf(
    "#FF5722", "#4CAF50", "#2196F3",
    "#9C27B0", "#FFC107", "#607D8B"
)

@Composable
fun EditScheduleScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { ScheduleDataStore(context) }
    
    val settingsStore = remember { SettingsDataStore(context) }
    val classDuration by settingsStore.classDurationFlow.collectAsState(initial = 40)
    val breakDuration by settingsStore.breakDurationFlow.collectAsState(initial = 10)
    
    val weekdays = Weekday.entries.toList()
    val today = todayWeekday()
    val initialPage = weekdays.indexOf(today).coerceAtLeast(0)
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { weekdays.size }
    )
    
    var showDialog by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<ScheduleEvent?>(null) }

    var refreshKey by remember { mutableStateOf(0) }
    val schedule by produceState(initialValue = emptyList<ScheduleEvent>(), refreshKey) {
        value = store.getSchedule().events
    }

    var nowMinutes by remember { mutableStateOf(currentMinutes()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMinutes = currentMinutes()
            delay(30_000L)
        }
    }

    if (showDialog) {
        BackHandler {
            showDialog = false
        }

        val currentWeekday = weekdays[pagerState.currentPage]
        val dayEventsForDefault = schedule
            .filter { it.weekday == currentWeekday }
            .sortedBy { it.startTime }

        val defaultStart = dayEventsForDefault
            .mapNotNull { it.endTime }
            .maxOrNull()
            ?: "08:00"

        EventEditDialog(
            weekday = currentWeekday,
            existing = editingEvent,
            defaultStartTime = if (editingEvent == null) defaultStart else null,
            classDuration = classDuration,
            breakDuration = breakDuration,
            allEventsOfDay = dayEventsForDefault,
            onDismiss = { showDialog = false },
            onConfirm = { event ->
                scope.launch {
                    if (editingEvent == null) {
                        store.addEvent(event)
                    } else {
                        store.updateEvent(event)
                    }
                    refreshKey++
                }
                showDialog = false
            }
        )
        return
    }

    HorizontalPagerScaffold(
        pagerState = pagerState,
        modifier = modifier
    ) {
        HorizontalPager(
            state = pagerState
        ) { page ->
            val weekday = weekdays[page]
            val listState = rememberTransformingLazyColumnState()
            val transformationSpec = rememberTransformationSpec()
            val dayEvents = schedule
                .filter { it.weekday == weekday }
                .sortedBy { it.startTime }

            ScreenScaffold(
                scrollState = listState,
                edgeButton = {
                    EdgeButton(
                        onClick = {
                            editingEvent = null
                            showDialog = true
                        },
                        buttonSize = EdgeButtonSize.ExtraSmall
                    ) {
                        Text("新增事件")
                    }
                }
            ) { contentPadding ->
                TransformingLazyColumn(
                    state = listState,
                    contentPadding = contentPadding
                ) {
                    item {
                        ListHeader(
                            modifier = Modifier
                                .fillMaxWidth()
                                .transformedHeight(this, transformationSpec)
                                .minimumVerticalContentPadding(
                                    ListHeaderDefaults.minimumTopListContentPadding
                                ),
                            transformation = SurfaceTransformation(transformationSpec)
                        ) { Text(text = weekdayLabel(weekday)) }
                    }

                    items(
                        count = dayEvents.size,
                        key = { index -> dayEvents[index].id }
                    ) { index ->
                        val event = dayEvents[index]
                        val start = toMinutes(event.startTime)
                        val end = toMinutes(event.endTime)
                        val highlighted = weekday == today &&
                            start != null && end != null &&
                            nowMinutes in start until end
                        ScheduleEventCard(
                            transformation = SurfaceTransformation(transformationSpec),
                            event = event,
                            highlighted = highlighted,
                            onClick = {
                                editingEvent = event
                                showDialog = true
                            },
                            onDelete = {
                                scope.launch {
                                    store.removeEvent(event.id)
                                    refreshKey++
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleEventCard(
    modifier: Modifier = Modifier,
    transformation: SurfaceTransformation? = null,
    event: ScheduleEvent,
    highlighted: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val color = event.courseColor?.let { parseColor(it) } ?: Color.DarkGray
    Card(
        onClick = onClick,
        transformation = transformation,
        colors = if (highlighted) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        } else {
            CardDefaults.cardColors()
        },
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (highlighted) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(20.dp)
                    )
                } else {
                    Modifier
                }
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
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
            IconButton(onClick = onDelete) {
                Icon(
                    painter = painterResource(R.drawable.delete),
                    tint = MaterialTheme.colorScheme.error,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun EventEditDialog(
    weekday: Weekday,
    existing: ScheduleEvent?,
    defaultStartTime: String?,
    classDuration: Int,
    breakDuration: Int,
    allEventsOfDay: List<ScheduleEvent>,
    onDismiss: () -> Unit,
    onConfirm: (ScheduleEvent) -> Unit
) {
    fun durationFor(t: ScheduleEventType): Int =
        if (t == ScheduleEventType.BREAK) breakDuration else classDuration

    val initialStart = existing?.startTime ?: defaultStartTime ?: "08:00"
    val initialEnd = existing?.endTime ?: addMinutes(initialStart, durationFor(existing?.type ?: ScheduleEventType.CLASS))

    var type by remember { mutableStateOf(existing?.type ?: ScheduleEventType.CLASS) }
    var name by remember { mutableStateOf(existing?.courseName ?: "") }
    var color by remember { mutableStateOf(existing?.courseColor) }
    var startTime by remember { mutableStateOf(initialStart) }
    var endTime by remember { mutableStateOf(initialEnd) }
    var userEditedEnd by remember { mutableStateOf(existing != null) }
    var stage by remember { mutableStateOf(DialogStage.EDIT) }
    var showCustomDialog by remember { mutableStateOf(false) }

    LaunchedEffect(type) {
        if (!userEditedEnd) {
            endTime = addMinutes(startTime, durationFor(type))
        }
    }
    
    val presets = if (type == ScheduleEventType.ACTIVITY) activityPresets else presetCourses
    var isCustom by remember {
        mutableStateOf(
            existing?.courseName?.let { n ->
                presets.none { it.name == n }
            } == true
        )
    }

    val conflict = remember(startTime, endTime, allEventsOfDay, existing) {
        val newStart = toMinutes(startTime)
        val newEnd = toMinutes(endTime)
        if (newStart == null || newEnd == null || newEnd <= newStart) {
            true
        } else {
            allEventsOfDay.any { other ->
                if (other.id == existing?.id) return@any false
                val os = toMinutes(other.startTime) ?: return@any false
                val oe = toMinutes(other.endTime) ?: return@any false
                newStart < oe && os < newEnd
            }
        }
    }

    when (stage) {
        DialogStage.START_TIME -> {
            WearTimePicker(
                initial = startTime,
                onConfirm = {
                    startTime = it
                    if (!userEditedEnd || (toMinutes(endTime) ?: 0) <= (toMinutes(it) ?: 0)) {
                        endTime = addMinutes(it, durationFor(type))
                        userEditedEnd = false
                    }
                    stage = DialogStage.EDIT
                },
                onCancel = { stage = DialogStage.EDIT }
            )
            return
        }
        DialogStage.END_TIME -> {
            WearTimePicker(
                initial = endTime,
                onConfirm = {
                    endTime = it
                    userEditedEnd = true
                    stage = DialogStage.EDIT
                },
                onCancel = { stage = DialogStage.EDIT }
            )
            return
        }
        DialogStage.EDIT -> Unit
    }

    if (showCustomDialog) {
        CustomCourseDialog(
            initialName = if (presets.any { it.name == name }) "" else name,
            initialColor = color,
            onDismiss = { showCustomDialog = false },
            onConfirm = { customName, customColor ->
                name = customName
                color = customColor
                isCustom = true
                showCustomDialog = false
            }
        )
        return
    }

    AlertDialog(
        visible = true,
        onDismissRequest = onDismiss,
        title = {
            Text(if (existing == null) "新增事件" else "编辑事件")
        },
        confirmButton = {
            AlertDialogDefaults.ConfirmButton(
                onClick = {
                    if (!conflict) {
                        val event = ScheduleEvent(
                            id = existing?.id ?: UUID.randomUUID().toString(),
                            weekday = weekday,
                            startTime = startTime,
                            endTime = endTime,
                            type = type,
                            courseName = if (type != ScheduleEventType.BREAK) name.ifBlank { null } else null,
                            courseColor = if (type != ScheduleEventType.BREAK) color else null
                        )
                        onConfirm(event)
                    }
                }
            )
        },
        dismissButton = {
            AlertDialogDefaults.DismissButton(onClick = onDismiss)
        }
    ) {
        item {
            ButtonGroup(Modifier.fillMaxWidth()) {
                Button(
                    label = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text("上课") } },
                    onClick = { type = ScheduleEventType.CLASS },
                    colors = if (type == ScheduleEventType.CLASS) {
                        ButtonDefaults.buttonColors()
                    } else {
                        ButtonDefaults.filledTonalButtonColors()
                    }
                )
                Button(
                    label = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text("课间") } },
                    onClick = { type = ScheduleEventType.BREAK },
                    colors = if (type == ScheduleEventType.BREAK) {
                        ButtonDefaults.buttonColors()
                    } else {
                        ButtonDefaults.filledTonalButtonColors()
                    }
                )
                Button(
                    label = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text("活动") } },
                    onClick = { type = ScheduleEventType.ACTIVITY },
                    colors = if (type == ScheduleEventType.ACTIVITY) {
                        ButtonDefaults.buttonColors()
                    } else {
                        ButtonDefaults.filledTonalButtonColors()
                    }
                )
            }
        }

        if (type != ScheduleEventType.BREAK) {
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    presets.forEach { preset ->
                        val selected = !isCustom && name == preset.name && color == preset.color
                        Button(
                            label = { Text(preset.name) },
                            onClick = {
                                name = preset.name
                                color = preset.color
                                isCustom = false
                            },
                            colors = if (selected) {
                                ButtonDefaults.buttonColors()
                            } else {
                                ButtonDefaults.filledTonalButtonColors()
                            }
                        )
                    }
                    Button(
                        label = { Text("自定义") },
                        onClick = { showCustomDialog = true },
                        colors = if (isCustom && name.isNotBlank()) {
                            ButtonDefaults.buttonColors()
                        } else {
                            ButtonDefaults.filledTonalButtonColors()
                        }
                    )
                }
            }
        }

        if (conflict) {
            item {
                val invalidRange = (toMinutes(endTime) ?: 0) <= (toMinutes(startTime) ?: 0)
                Text(
                    text = if (invalidRange) "结束时间需晚于开始时间" else "与已有事件时间冲突",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        item {
            FilledTonalButton(
                onClick = { stage = DialogStage.START_TIME },
                label = { Text("开始时间") },
                secondaryLabel = { Text(startTime) },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.access_time),
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            FilledTonalButton(
                onClick = { stage = DialogStage.END_TIME },
                label = { Text("结束时间") },
                secondaryLabel = { Text(endTime) },
                icon = {
                    Icon(
                        painter = painterResource(R.drawable.access_time),
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CustomCourseDialog(
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
        title = { Text("自定义课程") },
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
                Text(text = "课程名称")
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
                                    text = "输入课程名称",
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
                Text(text = "课程颜色")
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                    painter = painterResource(R.drawable.check),
                                    contentDescription = null,
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
private fun WearTimePicker(
    initial: String,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit
) {
    val parts = initial.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

    BackHandler {
        onCancel()
    }

    TimePicker(
        initialTime = LocalTime.of(hour, minute),
        onTimePicked = { time ->
            onConfirm("%02d:%02d".format(time.hour, time.minute))
        },
        timePickerType = TimePickerType.HoursMinutes24H
    )
}

private fun addMinutes(time: String, minutes: Int): String {
    val base = toMinutes(time) ?: return "08:45"
    val total = (base + minutes).coerceAtMost(23 * 60 + 59)
    return "%02d:%02d".format(total / 60, total % 60)
}