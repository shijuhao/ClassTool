package com.juhao.classtool.ui.schedule

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import java.time.LocalTime
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

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
    PresetCourse("语文", "#FF7043"),
    PresetCourse("数学", "#66BB6A"),
    PresetCourse("英语", "#42A5F5"),
    PresetCourse("物理", "#AB47BC"),
    PresetCourse("化学", "#FFCA28"),
    PresetCourse("生物", "#78909C"),
    PresetCourse("历史", "#A1887F"),
    PresetCourse("地理", "#26A69A"),
    PresetCourse("政治", "#EC407A"),
    PresetCourse("体育", "#FFA726"),
    PresetCourse("音乐", "#F06292"),
    PresetCourse("美术", "#FF80AB"),
    PresetCourse("信息", "#26C6DA"),
    PresetCourse("心理", "#9CCC65"),
    PresetCourse("通用技术", "#D4E157"),
    PresetCourse("晚自习", "#5C6BC0")
)

private val activityPresets = listOf(
    PresetCourse("升旗", "#EF5350"),
    PresetCourse("运动会", "#66BB6A"),
    PresetCourse("班会", "#5C6BC0"),
    PresetCourse("社团", "#FFA726"),
    PresetCourse("大扫除", "#26A69A"),
    PresetCourse("考试", "#AB47BC"),
    PresetCourse("讲座", "#26C6DA"),
    PresetCourse("实践", "#9CCC65"),
    PresetCourse("联欢", "#EC407A")
)

private val paletteColors = listOf(
    "#EF5350", "#EC407A", "#AB47BC", "#7E57C2",
    "#5C6BC0", "#42A5F5", "#26C6DA", "#26A69A",
    "#66BB6A", "#9CCC65", "#D4E157", "#FFCA28",
    "#FFA726", "#FF7043", "#A1887F", "#78909C",
    "#FF80AB", "#40E0D0", "#B2FF59", "#FFE082"
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
    var editMode by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<ScheduleEvent?>(null) }

    var refreshKey by remember { mutableIntStateOf(0) }
    val schedule by produceState(initialValue = emptyList<ScheduleEvent>(), refreshKey) {
        value = store.getSchedule().events
    }

    var nowMinutes by remember { mutableIntStateOf(currentMinutes()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMinutes = currentMinutes()
            delay(30_000L.milliseconds)
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

        val defaultStart = dayEventsForDefault.maxOfOrNull { it.endTime }
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

    var pendingDelete by remember { mutableStateOf<ScheduleEvent?>(null) }

    pendingDelete?.let { target ->
        AlertDialog(
            visible = true,
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除事件") },
            confirmButton = {
                AlertDialogDefaults.ConfirmButton(
                    onClick = {
                        scope.launch {
                            store.removeEvent(target.id)
                            refreshKey++
                        }
                        pendingDelete = null
                    }
                )
            },
            dismissButton = {
                AlertDialogDefaults.DismissButton(
                    onClick = { pendingDelete = null }
                )
            }
        ) {
            item {
                Text(
                    text = target.courseName ?: when (target.type) {
                        ScheduleEventType.BREAK -> "课间休息"
                        ScheduleEventType.ACTIVITY -> "活动"
                        ScheduleEventType.CLASS -> "未命名"
                    }
                )
            }
            item {
                Text(
                    text = "${target.startTime} - ${target.endTime}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
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

            val rawDayEvents = schedule
                .filter { it.weekday == weekday }
                .sortedBy { it.startTime }

            val dayEvents = if (editMode) rawDayEvents else rawDayEvents.hideBreaks()

            ScreenScaffold(
                scrollState = listState
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

                    item {
                        ButtonGroup(
                            modifier =
                                Modifier
                                    .graphicsLayer {
                                        with(transformationSpec) {
                                            applyContainerTransformation(scrollProgress)
                                        }
                                    }.transformedHeight(this, transformationSpec)
                                    .minimumVerticalContentPadding(
                                        ButtonDefaults.minimumVerticalListContentPadding
                                    )
                        ) {
                            FilledTonalIconButton(
                                onClick = { editMode = !editMode },
                                modifier = Modifier.weight(1f),
                                content = {
                                    Icon(
                                        painter = painterResource(
                                            if (editMode) R.drawable.close else R.drawable.edit
                                        ),
                                        contentDescription = "编辑",
                                        modifier = Modifier.size(ButtonDefaults.IconSize)
                                    )
                                }
                            )
                            FilledIconButton(
                                onClick = {
                                    editingEvent = null
                                    showDialog = true
                                },
                                content = {
                                    Icon(
                                        painter = painterResource(R.drawable.add),
                                        contentDescription = "新增事件",
                                        modifier = Modifier.size(ButtonDefaults.IconSize)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
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

                        val progress = if (highlighted && end > start) {
                            ((nowMinutes - start).toFloat() / (end - start).toFloat())
                                .coerceIn(0f, 1f)
                        } else {
                            0f
                        }

                        ScheduleEventCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .transformedHeight(this, transformationSpec),
                            transformation = SurfaceTransformation(transformationSpec),
                            event = event,
                            highlighted = highlighted,
                            progress = progress,
                            editMode = editMode,
                            onClick = {
                                if (editMode) {
                                    editingEvent = event
                                    showDialog = true
                                }
                            },
                            onRequestDelete = { pendingDelete = event }
                        )
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
    highlighted: Boolean = false,
    progress: Float = 0f,
    editMode: Boolean = false,
    onClick: () -> Unit = {},
    onRequestDelete: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
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

    val cardContent: @Composable () -> Unit = {
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

    if (editMode) {
        val revealState = rememberRevealState()
        SwipeToReveal(
            primaryAction = {
                PrimaryActionButton(
                    onClick = {
                        scope.launch { revealState.animateTo(RevealValue.Covered) }
                        onRequestDelete()
                    },
                    icon = { Icon(painterResource(R.drawable.delete), contentDescription = null) },
                    text = { Text("删除") },
                    modifier = Modifier.height(SwipeToRevealDefaults.LargeActionButtonHeight),
                )
            },
            revealState = revealState,
            onSwipePrimaryAction = {
                scope.launch { revealState.animateTo(RevealValue.Covered) }
                onRequestDelete()
            },
            modifier = modifier
        ) {
            cardContent()
        }
    } else {
        Box(modifier = modifier) {
            cardContent()
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
    val initialEnd =
        existing?.endTime ?: addMinutes(initialStart, durationFor(ScheduleEventType.CLASS))

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
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    ScheduleEventType.CLASS to "上课",
                    ScheduleEventType.BREAK to "课间",
                    ScheduleEventType.ACTIVITY to "活动"
                ).forEach { (t, label) ->
                    val selected = type == t
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                            .clickable { type = t }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (type != ScheduleEventType.BREAK) {
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    presets.forEach { preset ->
                        val selected = !isCustom && name == preset.name
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                                .clickable {
                                    name = preset.name
                                    color = preset.color
                                    isCustom = false
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(parseColor(preset.color ?: "#888888"))
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = preset.name,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSecondaryContainer,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isCustom && name.isNotBlank())
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                            .clickable { showCustomDialog = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.edit),
                            contentDescription = null,
                            tint = if (isCustom) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "自定义",
                            color = if (isCustom) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
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
        title = { Text("自定义") },
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
                                    text = "输入名称",
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

private fun List<ScheduleEvent>.hideBreaks(): List<ScheduleEvent> {
    val result = mutableListOf<ScheduleEvent>()
    for (event in this) {
        if (event.type == ScheduleEventType.BREAK) {
            continue
        }
        result.add(event)
    }
    return result
}

private fun addMinutes(time: String, minutes: Int): String {
    val base = toMinutes(time) ?: return "08:45"
    val total = (base + minutes).coerceAtMost(23 * 60 + 59)
    return "%02d:%02d".format(total / 60, total % 60)
}