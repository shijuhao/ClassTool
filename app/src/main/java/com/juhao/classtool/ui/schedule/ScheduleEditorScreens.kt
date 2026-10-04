package com.juhao.classtool.ui.schedule

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.transformedHeight
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.*
import com.juhao.classtool.datastore.ScheduleDataStore
import com.juhao.classtool.datastore.ScheduleEvent
import com.juhao.classtool.datastore.ScheduleEventType
import com.juhao.classtool.datastore.ScheduleValidator
import com.juhao.classtool.datastore.SettingsDataStore
import com.juhao.classtool.datastore.Weekday
import com.juhao.classtool.datastore.WeekdayScope
import com.juhao.classtool.navigation.*
import com.juhao.classtool.ui.components.RoundToast
import com.juhao.classtool.utils.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.util.UUID

private val WORKDAYS = setOf(
    Weekday.MONDAY,
    Weekday.TUESDAY,
    Weekday.WEDNESDAY,
    Weekday.THURSDAY,
    Weekday.FRIDAY
)

private val WEEKEND = setOf(
    Weekday.SATURDAY,
    Weekday.SUNDAY
)

private val presetCourses = listOf(
    "语文" to "#FF7043",
    "数学" to "#66BB6A",
    "英语" to "#42A5F5",
    "物理" to "#AB47BC",
    "化学" to "#FFCA28",
    "生物" to "#78909C",
    "历史" to "#A1887F",
    "地理" to "#26A69A",
    "政治" to "#EC407A",
    "体育" to "#FFA726",
    "音乐" to "#F06292",
    "美术" to "#FF80AB",
    "信息" to "#26C6DA",
    "心理" to "#9CCC65",
    "通用技术" to "#D4E157",
    "自习" to "#00897B"
)

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

@Serializable
data class ScheduleEventDraft(
    val id: String? = null,
    val weekday: Weekday,
    val startTime: String,
    val endTime: String,
    val type: ScheduleEventType,
    val name: String = "",
    val color: String? = null,
    val selectedDays: Set<Weekday> = emptySet(),
    val scope: WeekdayScope = WeekdayScope.CUSTOM,
    val urgent: Boolean = false,
    val userEditedEnd: Boolean = false
)

@Serializable
data class CourseEditDraft(
    val eventId: String,
    val weekday: Weekday,
    val name: String? = null,
    val color: String? = null
)

@Serializable
sealed interface PresetTarget {
    @Serializable
    data class Activity(
        val draft: ScheduleEventDraft
    ) : PresetTarget

    @Serializable
    data class Course(
        val draft: CourseEditDraft
    ) : PresetTarget
}

fun ScheduleEvent.toEditDraft(): ScheduleEventDraft =
    ScheduleEventDraft(
        id = id,
        weekday = weekdays.firstOrNull() ?: Weekday.MONDAY,
        startTime = startTime,
        endTime = endTime,
        type = type,
        name = courseName.orEmpty(),
        color = courseColor,
        selectedDays = weekdays,
        scope = scopeOfDays(weekdays),
        urgent = urgent,
        userEditedEnd = true
    )

private fun scopeOfDays(days: Set<Weekday>): WeekdayScope = when (days) {
    WORKDAYS -> WeekdayScope.WORKDAY
    WEEKEND -> WeekdayScope.WEEKEND
    else -> WeekdayScope.CUSTOM
}

data class PendingActivitySelection(
    val draft: ScheduleEventDraft
)

data class PendingCourseSelection(
    val draft: CourseEditDraft
)

@Composable
fun ScheduleEventEditScreen(
    draft: ScheduleEventDraft,
    onBack: () -> Unit,
    onNavigate: (AppKey) -> Unit,
    activitySelection: MutableState<PendingActivitySelection?>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { ScheduleDataStore(context) }
    val settingsStore = remember { SettingsDataStore(context) }

    val classDuration by settingsStore.classDurationFlow.collectAsState(initial = 40)
    val breakDuration by settingsStore.breakDurationFlow.collectAsState(initial = 10)

    var type by remember(draft) { mutableStateOf(draft.type) }
    var name by remember(draft) { mutableStateOf(draft.name) }
    var color by remember(draft) { mutableStateOf(draft.color) }
    var startTime by remember(draft) { mutableStateOf(draft.startTime) }
    var endTime by remember(draft) { mutableStateOf(draft.endTime) }
    var selectedDays by remember(draft) {
        mutableStateOf(draft.selectedDays.ifEmpty { setOf(draft.weekday) })
    }
    var scopeMode by remember(draft) { mutableStateOf(draft.scope) }
    var urgent by remember(draft) { mutableStateOf(draft.urgent) }
    var userEditedEnd by remember(draft) { mutableStateOf(draft.userEditedEnd) }
    var timePickerStage by remember { mutableStateOf<TimePickerStage?>(null) }

    LaunchedEffect(activitySelection.value) {
        activitySelection.value?.let { selection ->
            val selected = selection.draft
            type = selected.type
            name = selected.name
            color = selected.color
            startTime = selected.startTime
            endTime = selected.endTime
            selectedDays = selected.selectedDays.ifEmpty { setOf(selected.weekday) }
            scopeMode = selected.scope
            urgent = selected.urgent
            userEditedEnd = selected.userEditedEnd
            activitySelection.value = null
        }
    }

    BackHandler {
        if (timePickerStage != null) {
            timePickerStage = null
        } else {
            onBack()
        }
    }

    LaunchedEffect(type) {
        if (!userEditedEnd) {
            val duration = if (type == ScheduleEventType.BREAK) breakDuration else classDuration
            endTime = addMinutes(startTime, duration)
        }
    }

    LaunchedEffect(scopeMode) {
        selectedDays = when (scopeMode) {
            WeekdayScope.WORKDAY -> WORKDAYS
            WeekdayScope.WEEKEND -> WEEKEND
            WeekdayScope.CUSTOM -> selectedDays
        }
    }

    val allEvents by store.scheduleFlow
        .collectAsState(initial = com.juhao.classtool.datastore.Schedule())

    val selfIds = draft.id?.let { setOf(it) } ?: emptySet()
    val conflict = remember(
        startTime,
        endTime,
        selectedDays,
        allEvents.events,
        selfIds
    ) {
        !ScheduleValidator.findConflict(
            events = allEvents.events,
            startTime = startTime,
            endTime = endTime,
            weekdays = selectedDays,
            selfIds = selfIds
        ).valid
    }

    timePickerStage?.let { stage ->
        WearTimePicker(
            initial = if (stage == TimePickerStage.START) startTime else endTime,
            onConfirm = { picked ->
                if (stage == TimePickerStage.START) {
                    startTime = picked
                    if (!userEditedEnd || (toMinutes(endTime) ?: 0) <= (toMinutes(picked) ?: 0)) {
                        val duration = if (type == ScheduleEventType.BREAK) breakDuration else classDuration
                        endTime = addMinutes(picked, duration)
                        userEditedEnd = false
                    }
                } else {
                    endTime = picked
                    userEditedEnd = true
                }
                timePickerStage = null
            },
            onCancel = { timePickerStage = null }
        )
        return
    }

    val title = if (draft.id == null) "新增事件" else "编辑事件"

    val listState = rememberTransformingLazyColumnState()
    val square = LocalScreenShape.current == ScreenShape.SQUARE
    val transformationSpec = rememberAdaptiveTransformationSpec(square)

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(
            contentPadding = contentPadding,
            state = listState
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
                ) { Text(title) }
            }

            item {
                ListSubHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Category,
                            contentDescription = "Event Type"
                        )
                    },
                    label = { Text("事件") }
                )
            }

            item {
                TypeSelector(
                    selected = type,
                    onSelected = {
                        type = it
                        if (it != ScheduleEventType.ACTIVITY) {
                            name = ""
                            color = null
                        }
                        userEditedEnd = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .graphicsLayer {
                            with(transformationSpec) {
                                applyContainerTransformation(scrollProgress)
                            }
                        }
                )
            }

            if (type == ScheduleEventType.CLASS) {
                item {
                    FilledTonalButton(
                        onClick = {
                            onNavigate(
                                CourseTableNavScreen
                            )
                        },
                        label = { Text("课程") },
                        secondaryLabel = { Text("课程名称请在课程表中设置") },
                        icon = {
                            Icon(
                                imageVector = MaterialSymbols.Rounded.School,
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize)
                            )
                        },
                        transformation = SurfaceTransformation(transformationSpec),
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                    )
                }
            }

            if (type == ScheduleEventType.ACTIVITY) {
                item {
                    FilledTonalButton(
                        onClick = {
                            onNavigate(
                                ActivityPresetPickerNavScreen(
                                    draft.copy(
                                        type = ScheduleEventType.ACTIVITY,
                                        name = name,
                                        color = color,
                                        startTime = startTime,
                                        endTime = endTime,
                                        selectedDays = selectedDays,
                                        scope = scopeMode,
                                        urgent = urgent,
                                        userEditedEnd = userEditedEnd
                                    )
                                )
                            )
                        },
                        label = {
                            Text(
                                if (name.isBlank()) "选择活动" else name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        secondaryLabel = {
                            Text(if (color == null) "选择预设或自定义" else "已设置活动颜色")
                        },
                        icon = {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        color?.let(::parseColor)
                                            ?: MaterialTheme.colorScheme.primary
                                    )
                            )
                        },
                        transformation = SurfaceTransformation(transformationSpec),
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                    )
                }
            }

            item {
                ListSubHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Date_range,
                            contentDescription = "Active Range"
                        )
                    },
                    label = { Text("生效范围") }
                )
            }

            item {
                WeekdayScopeSelector(
                    scope = scopeMode,
                    selectedDays = selectedDays,
                    onScopeChanged = { scopeMode = it },
                    onDayChanged = { day ->
                        selectedDays = if (day in selectedDays) {
                            selectedDays - day
                        } else {
                            selectedDays + day
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .graphicsLayer {
                            with(transformationSpec) {
                                applyContainerTransformation(scrollProgress)
                            }
                        }
                )
            }

            item {
                ListSubHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Schedule,
                            contentDescription = "Time"
                        )
                    },
                    label = { Text("时间与选项") }
                )
            }

            item {
                TimeButton(
                    label = "开始时间",
                    time = startTime,
                    onClick = { timePickerStage = TimePickerStage.START },
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                )
            }

            item {
                TimeButton(
                    label = "结束时间",
                    time = endTime,
                    onClick = { timePickerStage = TimePickerStage.END },
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                )
            }

            item {
                SwitchButton(
                    checked = urgent,
                    onCheckedChange = { urgent = it },
                    label = {
                        Text(
                            text = "紧急结束",
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = "事件最后 10 分钟调红页面",
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
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                )
            }

            if (conflict || selectedDays.isEmpty()) {
                item {
                    Text(
                        text = when {
                            selectedDays.isEmpty() -> "请至少选择一天"
                            (toMinutes(endTime) ?: 0) <= (toMinutes(startTime) ?: 0) ->
                                "结束时间需晚于开始时间"
                            else -> "与已有事件时间冲突"
                        },
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .graphicsLayer {
                                with(transformationSpec) {
                                    applyContainerTransformation(scrollProgress)
                                }
                            }
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        if (conflict || selectedDays.isEmpty()) return@Button

                        val existing = draft.id?.let { id ->
                            allEvents.events.firstOrNull { it.id == id }
                        }

                        val event = ScheduleEvent(
                            id = draft.id ?: UUID.randomUUID().toString(),
                            weekdays = selectedDays,
                            startTime = startTime,
                            endTime = endTime,
                            type = type,
                            courseName = when (type) {
                                ScheduleEventType.CLASS -> existing?.courseName
                                ScheduleEventType.ACTIVITY -> name.ifBlank { null }
                                ScheduleEventType.BREAK -> null
                            },
                            courseColor = when (type) {
                                ScheduleEventType.CLASS -> existing?.courseColor
                                ScheduleEventType.ACTIVITY -> color
                                ScheduleEventType.BREAK -> null
                            },
                            courseNameByWeekday = existing?.courseNameByWeekday
                                ?.filterKeys { it in selectedDays }
                                ?: emptyMap(),
                            courseColorByWeekday = existing?.courseColorByWeekday
                                ?.filterKeys { it in selectedDays }
                                ?: emptyMap(),
                            enabled = existing?.enabled ?: true,
                            urgent = urgent,
                            transfers = existing?.transfers ?: emptyList()
                        )

                        scope.launch {
                            val result = if (draft.id == null) {
                                store.addEvent(event)
                            } else {
                                store.updateEvent(event)
                            }

                            if (result.valid) {
                                RoundToast.show(context, "操作成功")
                                onBack()
                            } else {
                                RoundToast.show(
                                    context,
                                    result.reason ?: "操作失败"
                                )
                            }
                        }
                    },
                    label = { Text("保存") },
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                    },
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(
                            ButtonDefaults.minimumVerticalListContentPadding
                        )
                )
            }
        }
    }
}

private enum class TimePickerStage {
    START,
    END
}

@Composable
private fun TypeSelector(
    selected: ScheduleEventType,
    onSelected: (ScheduleEventType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
    ) {
        listOf(
            ScheduleEventType.CLASS to "上课",
            ScheduleEventType.BREAK to "课间",
            ScheduleEventType.ACTIVITY to "活动"
        ).forEach { (type, label) ->
            SelectableChip(
                text = label,
                selected = selected == type,
                modifier = Modifier.weight(1f),
                onClick = { onSelected(type) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeekdayScopeSelector(
    scope: WeekdayScope,
    selectedDays: Set<Weekday>,
    onScopeChanged: (WeekdayScope) -> Unit,
    onDayChanged: (Weekday) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(
                WeekdayScope.WORKDAY to "工作日",
                WeekdayScope.WEEKEND to "周末",
                WeekdayScope.CUSTOM to "自定义"
            ).forEach { (value, label) ->
                SelectableChip(
                    text = label,
                    selected = scope == value,
                    modifier = Modifier.weight(1f),
                    onClick = { onScopeChanged(value) }
                )
            }
        }

        if (scope == WeekdayScope.CUSTOM) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Weekday.entries.forEach { day ->
                    SelectableChip(
                        text = weekdayShortLabel(day),
                        selected = day in selectedDays,
                        onClick = { onDayChanged(day) },
                        modifier = Modifier.width(48.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectableChip(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    CompactButton(
        onClick = onClick,
        label = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        colors = if (selected) {
            ButtonDefaults.buttonColors()
        } else {
            ButtonDefaults.filledTonalButtonColors()
        },
        modifier = modifier
    )
}

@Composable
private fun TimeButton(
    label: String,
    time: String,
    onClick: () -> Unit,
    transformation: SurfaceTransformation,
    modifier: Modifier = Modifier
) {
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
        transformation = transformation,
        modifier = modifier
    )
}

@Composable
fun ActivityPresetPickerScreen(
    draft: ScheduleEventDraft,
    onBack: () -> Unit,
    onSelect: (String, String?) -> Unit,
    onCustom: (String, String?) -> Unit
) {
    PresetPickerScreen(
        title = "选择活动",
        currentName = draft.name,
        options = activityPresets,
        onBack = onBack,
        onSelect = onSelect,
        onCustom = onCustom
    )
}

@Composable
fun CoursePresetPickerScreen(
    draft: CourseEditDraft,
    onBack: () -> Unit,
    onSelect: (String, String?) -> Unit,
    onCustom: (String, String?) -> Unit
) {
    PresetPickerScreen(
        title = "${weekdayLabel(draft.weekday)}课程",
        currentName = draft.name,
        options = presetCourses,
        onBack = onBack,
        onSelect = onSelect,
        onCustom = onCustom
    )
}

@Composable
private fun PresetPickerScreen(
    title: String,
    currentName: String?,
    options: List<Pair<String, String>>,
    onBack: () -> Unit,
    onSelect: (String, String?) -> Unit,
    onCustom: (String, String?) -> Unit
) {
    BackHandler(onBack = onBack)

    val selectedIndex = currentName?.let { name ->
        options.indexOfFirst { it.first == name }.takeIf { it >= 0 }?.plus(1)
            ?: options.size + 1
    } ?: 1

    val state = rememberTransformingLazyColumnState(
        initialAnchorItemIndex = selectedIndex
    )
    val square = LocalScreenShape.current == ScreenShape.SQUARE
    val transformationSpec = rememberAdaptiveTransformationSpec(square)

    ScreenScaffold(scrollState = state) { contentPadding ->
        TransformingLazyColumn(
            state = state,
            contentPadding = contentPadding,
            flingBehavior = TransformingLazyColumnDefaults.snapFlingBehavior(state = state),
            rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(
                scrollableState = state
            )
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
                ) { Text(title) }
            }

            options.forEach { (name, color) ->
                item {
                    PresetPickerItem(
                        name = name,
                        color = color,
                        selected = currentName == name,
                        transformation = SurfaceTransformation(transformationSpec),
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                        onClick = {
                            if (currentName == name) {
                                onSelect("", null)
                            } else {
                                onSelect(name, color)
                            }
                        }
                    )
                }
            }

            item {
                PresetPickerItem(
                    name = "自定义",
                    color = null,
                    selected = currentName != null &&
                        options.none { it.first == currentName },
                    icon = MaterialSymbols.Rounded.Edit,
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(
                            ButtonDefaults.minimumVerticalListContentPadding
                        ),
                    onClick = {
                        val customName = currentName
                            ?.takeIf { name -> options.none { it.first == name } }
                            .orEmpty()
                        onCustom(customName, null)
                    }
                )
            }
        }
    }
}

@Composable
private fun PresetPickerItem(
    name: String,
    color: String?,
    selected: Boolean,
    transformation: SurfaceTransformation,
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        transformation = transformation,
        colors = if (selected) {
            ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        } else {
            ButtonDefaults.filledTonalButtonColors()
        },
        icon = {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(parseColor(color ?: "#78909C"))
                )
            }
        },
        label = { Text(name) },
        secondaryLabel = {
            if (selected) Text("当前选择")
        },
        modifier = modifier
    )
}

@Composable
fun CustomPresetScreen(
    target: PresetTarget,
    initialName: String,
    initialColor: String?,
    onBack: () -> Unit,
    onConfirm: (String, String?) -> Unit
) {
    BackHandler(onBack = onBack)

    var name by remember(initialName) { mutableStateOf(initialName) }
    var color by remember(initialColor) { mutableStateOf(initialColor) }

    val listState = rememberTransformingLazyColumnState()
    val square = LocalScreenShape.current == ScreenShape.SQUARE
    val transformationSpec = rememberAdaptiveTransformationSpec(square)

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(
            contentPadding = contentPadding,
            state = listState
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
                ) {
                    Text(
                        if (target is PresetTarget.Course) "自定义课程" else "自定义活动"
                    )
                }
            }

            item {
                ListSubHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Edit,
                            contentDescription = "Name"
                        )
                    },
                    label = { Text("名称") }
                )
            }

            item {
                BasicTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .graphicsLayer {
                            with(transformationSpec) {
                                applyContainerTransformation(scrollProgress)
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (name.isEmpty()) {
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

            item {
                ListSubHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Palette,
                            contentDescription = "Color"
                        )
                    },
                    label = { Text("颜色") }
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .graphicsLayer {
                            with(transformationSpec) {
                                applyContainerTransformation(scrollProgress)
                            }
                        },
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        paletteColors.take(10).forEach { hex ->
                            ColorItem(
                                color = hex,
                                selected = color == hex,
                                onClick = {
                                    color = if (color == hex) null else hex
                                }
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        paletteColors.drop(10).forEach { hex ->
                            ColorItem(
                                color = hex,
                                selected = color == hex,
                                onClick = {
                                    color = if (color == hex) null else hex
                                }
                            )
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onConfirm(name.trim(), color)
                        }
                    },
                    enabled = name.isNotBlank(),
                    label = { Text("保存") },
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                    },
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(
                            ButtonDefaults.minimumVerticalListContentPadding
                        )
                )
            }
        }
    }
}

@Composable
private fun ColorItem(
    color: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(parseColor(color))
            .then(
                if (selected) {
                    Modifier.border(
                        2.dp,
                        contrastColorFor(color),
                        RoundedCornerShape(12.dp)
                    )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = MaterialSymbols.Rounded.Check,
                contentDescription = null,
                tint = contrastColorFor(color),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun CourseEditScreen(
    draft: CourseEditDraft,
    onBack: () -> Unit,
    onNavigate: (AppKey) -> Unit,
    courseSelection: MutableState<PendingCourseSelection?>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember(draft) { mutableStateOf(draft.name) }
    var color by remember(draft) { mutableStateOf(draft.color) }

    LaunchedEffect(courseSelection.value) {
        courseSelection.value?.let { selection ->
            name = selection.draft.name
            color = selection.draft.color
            courseSelection.value = null
        }
    }

    BackHandler(onBack = onBack)

    val listState = rememberTransformingLazyColumnState()
    val square = LocalScreenShape.current == ScreenShape.SQUARE
    val transformationSpec = rememberAdaptiveTransformationSpec(square)

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(
            contentPadding = contentPadding,
            state = listState
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
                ) {
                    Text("${weekdayLabel(draft.weekday)}课程")
                }
            }

            item {
                ListSubHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Schedule,
                            contentDescription = "Course Time"
                        )
                    },
                    label = { Text("课程时间") }
                )
            }

            item {
                FilledTonalButton(
                    onClick = {
                        onNavigate(
                            CoursePresetPickerNavScreen(
                                draft.copy(name = name, color = color)
                            )
                        )
                    },
                    label = {
                        Text(
                            name ?: "选择课程",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    secondaryLabel = {
                        Text("预设课程或自定义")
                    },
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    color?.let(::parseColor)
                                        ?: MaterialTheme.colorScheme.primary
                                )
                        )
                    },
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                )
            }

            if (name != null) {
                item {
                    FilledTonalButton(
                        onClick = {
                            name = null
                            color = null
                        },
                        label = { Text("清除课程") },
                        icon = {
                            Icon(
                                imageVector = MaterialSymbols.Rounded.Close,
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize)
                            )
                        },
                        transformation = SurfaceTransformation(transformationSpec),
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        scope.launch {
                            val store = ScheduleDataStore(context)
                            if (name == null) {
                                store.clearCourseForWeekday(
                                    draft.eventId,
                                    draft.weekday
                                )
                            } else {
                                store.setCourseForWeekday(
                                    draft.eventId,
                                    draft.weekday,
                                    name!!,
                                    color
                                )
                            }
                            RoundToast.show(context, "设置成功")
                            onBack()
                        }
                    },
                    label = { Text("保存") },
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                    },
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(
                            ButtonDefaults.minimumVerticalListContentPadding
                        )
                )
            }
        }
    }
}