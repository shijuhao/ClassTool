package com.juhao.classtool.ui.schedule

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
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
import com.juhao.classtool.datastore.SettingsDataStore
import com.juhao.classtool.datastore.Weekday
import com.juhao.classtool.datastore.WeekdayScope
import com.juhao.classtool.ui.components.RoundToast
import com.juhao.classtool.utils.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

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

private fun eventListDisplayName(event: ScheduleEvent): String = when (event.type) {
    ScheduleEventType.BREAK -> "课间休息"
    ScheduleEventType.CLASS -> "上课"
    ScheduleEventType.ACTIVITY -> event.courseName ?: "活动"
}

private fun eventDraftForNew(
    weekday: Weekday,
    schedule: List<ScheduleEvent>,
    classDuration: Int,
    breakDuration: Int
): ScheduleEventDraft {
    val lastEvent = schedule
        .filter { it.enabled }
        .maxByOrNull { toMinutes(it.endTime) ?: Int.MIN_VALUE }

    val type = when (lastEvent?.type) {
        ScheduleEventType.BREAK -> ScheduleEventType.CLASS
        ScheduleEventType.CLASS -> ScheduleEventType.BREAK
        else -> ScheduleEventType.CLASS
    }

    val start = lastEvent?.endTime ?: "08:00"
    val duration = if (type == ScheduleEventType.BREAK) breakDuration else classDuration

    return ScheduleEventDraft(
        weekday = weekday,
        startTime = start,
        endTime = addMinutes(start, duration),
        type = type,
        selectedDays = setOf(weekday),
        scope = WeekdayScope.CUSTOM
    )
}

private fun ScheduleEvent.toDraft(): ScheduleEventDraft =
    ScheduleEventDraft(
        id = id,
        weekday = weekdays.firstOrNull() ?: Weekday.MONDAY,
        startTime = startTime,
        endTime = endTime,
        type = type,
        name = courseName.orEmpty(),
        color = courseColor,
        selectedDays = weekdays,
        scope = scopeOf(weekdays),
        urgent = urgent,
        userEditedEnd = true
    )

private fun scopeOf(days: Set<Weekday>): WeekdayScope = when (days) {
    WORKDAYS -> WeekdayScope.WORKDAY
    WEEKEND -> WeekdayScope.WEEKEND
    else -> WeekdayScope.CUSTOM
}

@Composable
fun TimeTableScreen(
    modifier: Modifier = Modifier,
    onNavigate: (com.juhao.classtool.navigation.AppKey) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { ScheduleDataStore(context) }
    val settingsStore = remember { SettingsDataStore(context) }

    val classDuration by settingsStore.classDurationFlow.collectAsState(initial = 40)
    val breakDuration by settingsStore.breakDurationFlow.collectAsState(initial = 10)
    val scheduleState by store.scheduleFlow.collectAsState(initial = Schedule())
    val schedule = scheduleState.events
    val today = todayWeekday()

    var actionEvent by remember { mutableStateOf<ScheduleEvent?>(null) }
    var deleteEvent by remember { mutableStateOf<ScheduleEvent?>(null) }
    var nowMinutes by remember { mutableIntStateOf(currentMinutes()) }

    LaunchedEffect(Unit) {
        while (true) {
            nowMinutes = currentMinutes()
            delay(30_000L.milliseconds)
        }
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
                    }
                )
            }
        ) {
            item {
                FilledTonalButton(
                    onClick = {
                        actionEvent = null
                        onNavigate(
                            com.juhao.classtool.navigation.EditScheduleEventNavScreen(
                                target.toDraft()
                            )
                        )
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
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
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
                    }
                )
            }
        ) {
            item {
                Button(
                    onClick = {
                        deleteEvent = null
                        scope.launch {
                            store.removeEvent(target.id)
                            RoundToast.show(context, "已删除")
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
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    val listState = rememberTransformingLazyColumnState()
    val square = LocalScreenShape.current == ScreenShape.SQUARE
    val transformationSpec = rememberAdaptiveTransformationSpec(square)
    val displayEvents = remember(schedule) {
        schedule
            .filter { it.enabled }
            .sortedBy { toMinutes(it.startTime) ?: Int.MAX_VALUE }
    }

    ScreenScaffold(scrollState = listState) { contentPadding ->
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
                ) { Text("时间表") }
            }

            item {
                FilledTonalButton(
                    onClick = {
                        onNavigate(
                            com.juhao.classtool.navigation.AddScheduleEventNavScreen(
                                eventDraftForNew(
                                    today,
                                    schedule,
                                    classDuration,
                                    breakDuration
                                )
                            )
                        )
                    },
                    transformation = SurfaceTransformation(transformationSpec),
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Add,
                            contentDescription = "新增事件",
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                    },
                    label = { Text("新增事件") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .then(
                            if (displayEvents.isEmpty()) {
                                Modifier.minimumVerticalContentPadding(
                                    ButtonDefaults.minimumVerticalListContentPadding
                                )
                            } else {
                                Modifier
                            }
                        )
                )
            }

            items(
                count = displayEvents.size,
                key = { displayEvents[it].id }
            ) { index ->
                val event = displayEvents[index]
                val isLast = index == displayEvents.lastIndex
                val start = toMinutes(event.startTime)
                val end = toMinutes(event.endTime)
                val highlighted = today in event.weekdays &&
                    start != null &&
                    end != null &&
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
                            } else {
                                Modifier
                            }
                        ),
                    transformation = SurfaceTransformation(transformationSpec),
                    event = event,
                    highlighted = highlighted,
                    displayName = eventListDisplayName(event),
                    onClick = {
                        onNavigate(
                            com.juhao.classtool.navigation.EditScheduleEventNavScreen(
                                event.toDraft()
                            )
                        )
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
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val dotColor = when (event.type) {
        ScheduleEventType.BREAK -> MaterialTheme.colorScheme.onSurfaceVariant
        ScheduleEventType.CLASS -> MaterialTheme.colorScheme.primary
        ScheduleEventType.ACTIVITY -> event.courseColor?.let(::parseColor)
            ?: MaterialTheme.colorScheme.onSurface
    }

    FilledTonalButton(
        onClick = onClick,
        onLongClick = onLongClick,
        transformation = transformation,
        colors = ButtonDefaults.buttonColors().copy(
            containerColor = lerp(
                MaterialTheme.colorScheme.background,
                dotColor,
                0.2f
            ),
            contentColor = MaterialTheme.colorScheme.onSurface,
            secondaryContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        icon = {
            Icon(
                imageVector = when (event.type) {
                    ScheduleEventType.BREAK -> MaterialSymbols.Rounded.Accessibility
                    ScheduleEventType.CLASS -> MaterialSymbols.Rounded.School
                    ScheduleEventType.ACTIVITY -> MaterialSymbols.Rounded.Flag
                },
                contentDescription = null,
                tint = dotColor,
                modifier = Modifier.size(ButtonDefaults.IconSize)
            )
        },
        label = { Text(displayName) },
        secondaryLabel = {
            Text("${event.startTime} - ${event.endTime}  ${weekdayScopeLabel(event.weekdays)}")
        },
        modifier = modifier.then(
            if (highlighted) {
                Modifier.border(
                    2.dp,
                    dotColor,
                    RoundedCornerShape(50.dp)
                )
            } else {
                Modifier
            }
        )
    )
}
