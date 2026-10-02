package com.juhao.classtool.ui.schedule

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.transformedHeight
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.*
import com.juhao.classtool.datastore.Schedule
import com.juhao.classtool.datastore.ScheduleDataStore
import com.juhao.classtool.datastore.ScheduleEvent
import com.juhao.classtool.datastore.ScheduleEventType
import com.juhao.classtool.datastore.ScheduleValidationResult
import com.juhao.classtool.datastore.ScheduleValidator
import com.juhao.classtool.datastore.SettingsDataStore
import com.juhao.classtool.datastore.Weekday
import com.juhao.classtool.datastore.WeekdayScope
import com.juhao.classtool.ui.components.RoundToast
import com.juhao.classtool.utils.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

private val WORKDAYS = setOf(
    Weekday.MONDAY, Weekday.TUESDAY, Weekday.WEDNESDAY, Weekday.THURSDAY, Weekday.FRIDAY
)
private val WEEKEND = setOf(Weekday.SATURDAY, Weekday.SUNDAY)

private val activityPresets = listOf(
    "升旗" to "#EF5350",
    "运动会" to "#66BB6A",
    "班会" to "#5C6BC0",
    "社团" to "#FFA726",
    "大扫除" to "#26A69A",
    "考试" to "#AB47BC",
    "讲座" to "#26C6DA",
    "实践" to "#9CCC65",
    "联欢" to "#EC407A"
)

private enum class DialogStage { EDIT, START_TIME, END_TIME }

private fun eventListDisplayName(event: ScheduleEvent): String = when (event.type) {
    ScheduleEventType.BREAK -> "课间休息"
    ScheduleEventType.CLASS -> "上课"
    ScheduleEventType.ACTIVITY -> event.courseName ?: "活动"
}

@Composable
fun TimeTableScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { ScheduleDataStore(context) }
    val settingsStore = remember { SettingsDataStore(context) }

    val classDuration by settingsStore.classDurationFlow.collectAsState(initial = 40)
    val breakDuration by settingsStore.breakDurationFlow.collectAsState(initial = 10)

    val today = todayWeekday()

    var showDialog by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<ScheduleEvent?>(null) }

    var actionEvent by remember { mutableStateOf<ScheduleEvent?>(null) }
    var deleteEvent by remember { mutableStateOf<ScheduleEvent?>(null) }

    val scheduleState by store.scheduleFlow.collectAsState(initial = Schedule())
    val schedule = scheduleState.events
    val adjustments = scheduleState.adjustments
    
    var nowMinutes by remember { mutableIntStateOf(currentMinutes()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMinutes = currentMinutes()
            delay(30_000L.milliseconds)
        }
    }

    if (showDialog) {
        BackHandler { showDialog = false }
        val lastEvent = remember(schedule, editingEvent) {
            if (editingEvent != null) null
            else schedule
                .filter { it.enabled }
                .maxByOrNull { toMinutes(it.endTime) ?: Int.MIN_VALUE }
        }
        val defaultStart = lastEvent?.endTime ?: "08:00"
        val defaultType = when (lastEvent?.type) {
            ScheduleEventType.BREAK -> ScheduleEventType.CLASS
            ScheduleEventType.CLASS -> ScheduleEventType.BREAK
            else -> ScheduleEventType.CLASS
        }

        EventEditDialog(
            weekday = today,
            existing = editingEvent,
            defaultStartTime = if (editingEvent == null) defaultStart else null,
            defaultType = if (editingEvent == null) defaultType else null,
            classDuration = classDuration,
            breakDuration = breakDuration,
            allEvents = schedule,
            onDismiss = { showDialog = false },
            onConfirm = { event ->
                scope.launch {
                    val result = if (event == null) {
                        ScheduleValidationResult(true)
                    } else if (editingEvent != null) {
                        store.updateEvent(event)
                    } else {
                        store.addEvent(event)
                    }
                    if (result.valid) {
                        showDialog = false
                        RoundToast.show(context, "操作成功")
                    } else {
                        RoundToast.show(context, result.reason ?: "操作失败")
                    }
                }
            }
        )
        return
    }

    actionEvent?.let { target ->
        AlertDialog(
            visible = true,
            onDismissRequest = { actionEvent = null },
            title = { Text(eventListDisplayName(target)) },
            text = {
                Text(
                    text = "${target.startTime} - ${target.endTime}",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            edgeButton = {
                AlertDialogDefaults.EdgeButton(
                    onClick = { actionEvent = null },
                    content = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Close,
                            contentDescription = null
                        )
                    },
                )
            },
        ) {
            item {
                FilledTonalButton(
                    onClick = {
                        actionEvent = null
                        editingEvent = target
                        showDialog = true
                    },
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                    },
                    label = { Text("编辑") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Button(
                    onClick = {
                        actionEvent = null
                        deleteEvent = target
                    },
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                    },
                    label = { Text("删除") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    deleteEvent?.let { target ->
        AlertDialog(
            visible = true,
            onDismissRequest = { deleteEvent = null },
            title = { Text("删除事件") },
            text = { Text("确定要删除「${eventListDisplayName(target)}」吗？此操作无法撤销。") },
            edgeButton = {
                AlertDialogDefaults.EdgeButton(
                    onClick = { deleteEvent = null },
                    content = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Close,
                            contentDescription = null
                        )
                    },
                )
            },
        ) {
            item {
                Button(
                    onClick = {
                        deleteEvent = null
                        scope.launch {
                            store.removeEvent(target.id)
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                    },
                    label = { Text("删除") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    val listState = rememberTransformingLazyColumnState()
    val square = LocalScreenShape.current == ScreenShape.SQUARE
    val transformationSpec = rememberAdaptiveTransformationSpec(square)

    val displayEvents = remember(schedule) {
        schedule.filter { it.enabled }.sortedBy { toMinutes(it.startTime) ?: Int.MAX_VALUE }
    }
    val addButtonIsLast = displayEvents.isEmpty()

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item {
                ListHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(
                            ListHeaderDefaults.minimumTopListContentPadding
                        ),
                    transformation = SurfaceTransformation(transformationSpec)
                ) { Text("时间表") }
            }

            item {
                FilledTonalButton(
                    onClick = {
                        editingEvent = null
                        showDialog = true
                    },
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .then(
                            if (addButtonIsLast) {
                                Modifier.minimumVerticalContentPadding(
                                    ButtonDefaults.minimumVerticalListContentPadding
                                )
                            } else Modifier
                        ),
                    label = { Text("新增事件") },
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Add,
                            contentDescription = "新增事件",
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                    }
                )
            }

            items(count = displayEvents.size, key = { displayEvents[it].id }) { index ->
                val event = displayEvents[index]
                val isLast = index == displayEvents.lastIndex
                val start = toMinutes(event.startTime)
                val end = toMinutes(event.endTime)
                val highlighted = today in event.weekdays &&
                        start != null && end != null &&
                        nowMinutes in start until end

                EventListCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .then(
                            if (isLast) {
                                Modifier.minimumVerticalContentPadding(
                                    ButtonDefaults.minimumVerticalListContentPadding
                                )
                            } else Modifier
                        ),
                    transformation = SurfaceTransformation(transformationSpec),
                    event = event,
                    highlighted = highlighted,
                    displayName = eventListDisplayName(event),
                    onClick = {
                        editingEvent = event
                        showDialog = true
                    },
                    onLongClick = { actionEvent = event }
                )
            }
        }
    }
}

@Composable
private fun EventListCard(
    modifier: Modifier = Modifier,
    transformation: SurfaceTransformation? = null,
    event: ScheduleEvent,
    highlighted: Boolean = false,
    displayName: String,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val dotColor = when (event.type) {
        ScheduleEventType.BREAK -> MaterialTheme.colorScheme.onSurfaceVariant
        ScheduleEventType.CLASS -> MaterialTheme.colorScheme.primary
        ScheduleEventType.ACTIVITY -> event.courseColor?.let { parseColor(it) }
            ?: MaterialTheme.colorScheme.onSurface
    }

    FilledTonalButton(
        onClick = onClick,
        onLongClick = onLongClick,
        transformation = transformation,
        label = { Text(displayName) },
        secondaryLabel = {
            val time = "${event.startTime} - ${event.endTime}"
            val suffix = "  ${weekdayScopeLabel(event.weekdays)}"
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
                    dotColor,
                    RoundedCornerShape(50.dp)
                ) else Modifier
            )
    )
}

@Composable
private fun EventEditDialog(
    weekday: Weekday,
    existing: ScheduleEvent?,
    defaultStartTime: String?,
    defaultType: ScheduleEventType?,
    classDuration: Int,
    breakDuration: Int,
    allEvents: List<ScheduleEvent>,
    onDismiss: () -> Unit,
    onConfirm: (ScheduleEvent?) -> Unit
) {
    fun durationFor(t: ScheduleEventType) =
        if (t == ScheduleEventType.BREAK) breakDuration else classDuration

    val initialStart = existing?.startTime ?: defaultStartTime ?: "08:00"
    val initialType = existing?.type ?: defaultType ?: ScheduleEventType.CLASS
    val initialEnd = existing?.endTime ?: addMinutes(initialStart, durationFor(initialType))

    var type by remember { mutableStateOf(initialType) }
    var name by remember { mutableStateOf(existing?.courseName ?: "") }
    var color by remember { mutableStateOf(existing?.courseColor) }
    var startTime by remember { mutableStateOf(initialStart) }
    var endTime by remember { mutableStateOf(initialEnd) }
    var userEditedEnd by remember { mutableStateOf(existing != null) }
    var stage by remember { mutableStateOf(DialogStage.EDIT) }
    var scopeMode by remember {
        mutableStateOf(existing?.let { scopeOf(it.weekdays) } ?: WeekdayScope.WORKDAY)
    }
    var selectedDays by remember { mutableStateOf(existing?.weekdays ?: setOf(weekday)) }
    var showCustomActivityDialog by remember { mutableStateOf(false) }
    var urgent by remember { mutableStateOf(existing?.urgent ?: false) }

    LaunchedEffect(defaultStartTime, defaultType) {
        if (defaultStartTime != null && existing == null) {
            startTime = defaultStartTime
            if (!userEditedEnd) {
                endTime = addMinutes(defaultStartTime, durationFor(type))
            }
        }
    }

    LaunchedEffect(type) {
        if (!userEditedEnd) endTime = addMinutes(startTime, durationFor(type))
    }

    LaunchedEffect(scopeMode) {
        selectedDays = when (scopeMode) {
            WeekdayScope.WORKDAY -> WORKDAYS
            WeekdayScope.WEEKEND -> WEEKEND
            WeekdayScope.CUSTOM -> selectedDays
        }
    }

    val selfIds = existing?.let { setOf(it.id) } ?: emptySet()
    val conflict = remember(startTime, endTime, allEvents, selfIds, selectedDays) {
        hasConflict(startTime, endTime, selectedDays, allEvents, selfIds)
    }

    if (stage == DialogStage.START_TIME || stage == DialogStage.END_TIME) {
        val isStart = stage == DialogStage.START_TIME
        val initial = if (isStart) startTime else endTime
        WearTimePicker(
            initial = initial,
            onConfirm = { picked ->
                if (isStart) {
                    startTime = picked
                    if (!userEditedEnd || (toMinutes(endTime) ?: 0) <= (toMinutes(picked) ?: 0)) {
                        endTime = addMinutes(picked, durationFor(type))
                        userEditedEnd = false
                    }
                } else {
                    endTime = picked
                    userEditedEnd = true
                }
                stage = DialogStage.EDIT
            },
            onCancel = { stage = DialogStage.EDIT }
        )
        return
    }

    if (showCustomActivityDialog) {
        CustomPresetDialog(
            title = "自定义活动",
            placeholder = "输入活动名称",
            initialName = if (name.isNotBlank() && activityPresets.none { it.first == name }) name else "",
            initialColor = color,
            onDismiss = { showCustomActivityDialog = false },
            onConfirm = { customName, customColor ->
                name = customName
                color = customColor
                showCustomActivityDialog = false
            }
        )
        return
    }

    AlertDialog(
        visible = true,
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "新增事件" else "编辑事件") },
        confirmButton = {
            AlertDialogDefaults.ConfirmButton(
                onClick = {
                    if (conflict || selectedDays.isEmpty()) return@ConfirmButton

                    val resolvedName = when (type) {
                        ScheduleEventType.CLASS -> existing?.courseName
                        ScheduleEventType.ACTIVITY -> name.ifBlank { null }
                        ScheduleEventType.BREAK -> null
                    }
                    val resolvedColor = when (type) {
                        ScheduleEventType.CLASS -> existing?.courseColor
                        ScheduleEventType.ACTIVITY -> color
                        ScheduleEventType.BREAK -> null
                    }

                    val filteredNameByWeekday = existing?.courseNameByWeekday
                        ?.filterKeys { it in selectedDays } ?: emptyMap()
                    val filteredColorByWeekday = existing?.courseColorByWeekday
                        ?.filterKeys { it in selectedDays } ?: emptyMap()

                    val event = ScheduleEvent(
                        id = existing?.id ?: UUID.randomUUID().toString(),
                        weekdays = selectedDays,
                        startTime = startTime,
                        endTime = endTime,
                        type = type,
                        courseName = resolvedName,
                        courseColor = resolvedColor,
                        courseNameByWeekday = filteredNameByWeekday,
                        courseColorByWeekday = filteredColorByWeekday,
                        enabled = true,
                        urgent = urgent,
                        transfers = existing?.transfers ?: emptyList()
                    )
                    onConfirm(event)
                }
            )
        },
        dismissButton = { AlertDialogDefaults.DismissButton(onClick = onDismiss) }
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
                    SelectableChip(
                        text = label,
                        selected = type == t,
                        modifier = Modifier.weight(1f),
                        onClick = { type = t }
                    )
                }
            }
        }

        if (type == ScheduleEventType.CLASS) {
            item {
                Text(
                    "课程请在课程表中设置",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (type == ScheduleEventType.ACTIVITY) {
            item {
                Column(Modifier.fillMaxWidth()) {
                    Text("活动名称", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        activityPresets.forEach { (presetName, presetColor) ->
                            val selected = name == presetName
                            ActivityChip(
                                text = presetName,
                                colorHex = presetColor,
                                selected = selected,
                                onClick = {
                                    if (selected) {
                                        name = ""; color = null
                                    } else {
                                        name = presetName; color = presetColor
                                    }
                                }
                            )
                        }

                        val isCustom = name.isNotBlank() && activityPresets.none { it.first == name }
                        ActivityChip(
                            text = "自定义",
                            colorHex = null,
                            selected = isCustom,
                            leadingIcon = {
                                Icon(
                                    imageVector = MaterialSymbols.Rounded.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            onClick = { showCustomActivityDialog = true }
                        )
                    }
                }
            }
        }

        item {
            Column(Modifier.fillMaxWidth()) {
                Text("生效范围", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        WeekdayScope.WORKDAY to "工作日",
                        WeekdayScope.WEEKEND to "周末",
                        WeekdayScope.CUSTOM to "自定义"
                    ).forEach { (s, label) ->
                        SelectableChip(
                            text = label,
                            selected = scopeMode == s,
                            modifier = Modifier.weight(1f),
                            onClick = { scopeMode = s }
                        )
                    }
                }
            }
        }

        if (scopeMode == WeekdayScope.CUSTOM) {
            item {
                Column(Modifier.fillMaxWidth()) {
                    Text("星期", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Weekday.entries.forEach { day ->
                            val selected = day in selectedDays
                            SelectableChip(
                                text = weekdayShortLabel(day),
                                selected = selected,
                                horizontalPadding = 12.dp,
                                onClick = {
                                    selectedDays = if (selected) selectedDays - day
                                    else selectedDays + day
                                }
                            )
                        }
                    }
                }
            }
        }

        item {
            SwitchButton(
                checked = urgent,
                onCheckedChange = { checked ->
                    urgent = checked
                },
                label = {
                    Text(
                        text = "紧急结束",
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                secondaryLabel = {
                    Text(
                        text = "事件最后 10 分钟调红页面（仅开启跟随事件颜色时生效）",
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                colors = SwitchButtonDefaults.switchButtonColors().copy(
                    checkedContainerColor = MaterialTheme.colorScheme.errorContainer,
                    checkedContentColor = MaterialTheme.colorScheme.onErrorContainer,
                    checkedSecondaryContentColor = MaterialTheme.colorScheme.onErrorContainer,
                    checkedIconColor = MaterialTheme.colorScheme.onErrorContainer,
                    checkedThumbColor = MaterialTheme.colorScheme.errorContainer,
                    checkedThumbIconColor = MaterialTheme.colorScheme.onErrorContainer,
                    checkedTrackColor = MaterialTheme.colorScheme.onErrorContainer,
                    checkedTrackBorderColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                icon = {
                    Icon(
                        imageVector = MaterialSymbols.Rounded.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (conflict) {
            item {
                val invalidRange = (toMinutes(endTime) ?: 0) <= (toMinutes(startTime) ?: 0)
                Text(
                    text = when {
                        selectedDays.isEmpty() -> "请至少选择一天"
                        invalidRange -> "结束时间需晚于开始时间"
                        else -> "与已有事件时间冲突"
                    },
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        item {
            TimeButton("开始时间", startTime) { stage = DialogStage.START_TIME }
        }
        item {
            TimeButton("结束时间", endTime) { stage = DialogStage.END_TIME }
        }
    }
}

@Composable
private fun SelectableChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 10.dp,
    verticalPadding: Dp = 6.dp,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .clickable(onClick = onClick)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ActivityChip(
    text: String,
    colorHex: String?,
    selected: Boolean,
    leadingIcon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        val tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant

        if (leadingIcon != null) {
            CompositionLocalProvider(LocalContentColor provides tint) { leadingIcon() }
        } else if (colorHex != null) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(parseColor(colorHex))
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            color = tint,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun TimeButton(label: String, time: String, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        label = { Text(label) },
        secondaryLabel = { Text(time) },
        icon = {
            Icon(
                imageVector = MaterialSymbols.Rounded.Schedule,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
        },
        modifier = Modifier.fillMaxWidth()
    )
}

private fun hasConflict(
    startTime: String,
    endTime: String,
    selectedDays: Set<Weekday>,
    allEvents: List<ScheduleEvent>,
    selfIds: Set<String>
): Boolean =
    !ScheduleValidator.findConflict(
        events = allEvents,
        startTime = startTime,
        endTime = endTime,
        weekdays = selectedDays,
        selfIds = selfIds
    ).valid

private fun scopeOf(days: Set<Weekday>): WeekdayScope = when (days) {
    WORKDAYS -> WeekdayScope.WORKDAY
    WEEKEND -> WeekdayScope.WEEKEND
    else -> WeekdayScope.CUSTOM
}