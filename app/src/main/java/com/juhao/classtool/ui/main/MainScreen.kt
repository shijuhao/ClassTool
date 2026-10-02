package com.juhao.classtool.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.transformedHeight
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.*
import com.composables.icons.materialsymbols.roundedfilled.Gamepad
import com.juhao.classtool.datastore.ScheduleAdjustment
import com.juhao.classtool.datastore.ScheduleDataStore
import com.juhao.classtool.datastore.ScheduleEvent
import com.juhao.classtool.datastore.ScheduleEventType
import com.juhao.classtool.datastore.SettingsDataStore
import com.juhao.classtool.datastore.Weekday
import com.juhao.classtool.navigation.*
import com.juhao.classtool.ui.schedule.*
import com.juhao.classtool.utils.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun GreetingScreen(
    isActive: Boolean = true,
    onChangePage: (AppKey) -> Unit
) {
    val context = LocalContext.current
    val store = remember { ScheduleDataStore(context) }
    val settingsStore = remember { SettingsDataStore(context) }

    val keepScreenOnSetting by settingsStore.keepScreenOnFlow.collectAsState(initial = false)

    val scheduleFlow: Flow<List<ScheduleEvent>> = store.scheduleFlow.map { it.events }
    val schedule: List<ScheduleEvent> by scheduleFlow.collectAsState(initial = emptyList())
    val adjustmentsFlow: Flow<List<ScheduleAdjustment>> = store.scheduleFlow.map { it.adjustments }
    val adjustments: List<ScheduleAdjustment> by adjustmentsFlow.collectAsState(initial = emptyList())

    var prepBellEnabled by remember { mutableStateOf(true) }
    var nowSecondOfDay by remember { mutableIntStateOf(currentSecondOfDay()) }

    LaunchedEffect(Unit) {
        prepBellEnabled = settingsStore.getPrepBell()
        while (true) {
            nowSecondOfDay = currentSecondOfDay()
            delay(1000L.milliseconds)
        }
    }

    val todayDate = todayDateString()
    val displayWeekday = remember(todayDate, adjustments) {
        effectiveWeekdayOnDate(todayDate, adjustments) ?: todayWeekday()
    }
    val nowMinutes = nowSecondOfDay / 60

    val currentEvent: ScheduleEvent? = remember(schedule, adjustments, todayDate, nowMinutes) {
        findCurrentEventOnDate(schedule, adjustments, todayDate, nowMinutes)
    }

    val prepEvent: ScheduleEvent? = remember(schedule, adjustments, todayDate, nowSecondOfDay, prepBellEnabled) {
        if (!prepBellEnabled) return@remember null
        eventsOnDate(schedule, adjustments, todayDate).mapNotNull { event ->
            if (!event.enabled) return@mapNotNull null
            if (event.type == ScheduleEventType.BREAK) return@mapNotNull null
            val startSec = toMinutes(event.startTime)?.times(60) ?: return@mapNotNull null
            if (nowSecondOfDay in (startSec - PREP_BELL_SECONDS) until startSec) event else null
        }.firstOrNull()
    }

    val activeDisplayEvent: ScheduleEvent? = prepEvent ?: currentEvent

    val startSec = activeDisplayEvent?.let { toMinutes(it.startTime)?.times(60) }
    val endSec = activeDisplayEvent?.let { toMinutes(it.endTime)?.times(60) }

    val isPrep = prepEvent != null

    val targetProgress = if (isPrep) {
        val prepStartSec = startSec?.minus(PREP_BELL_SECONDS)
        if (prepStartSec != null && startSec > prepStartSec) {
            val elapsed = (nowSecondOfDay - prepStartSec).toFloat()
            val total = (startSec - prepStartSec).toFloat()
            (1f - elapsed / total).coerceIn(0f, 1f)
        } else 0f
    } else if (startSec != null && endSec != null && endSec > startSec) {
        ((nowSecondOfDay - startSec).toFloat() / (endSec - startSec).toFloat())
            .coerceIn(0f, 1f)
    } else 0f

    val remainingSec = if (isPrep) {
        startSec?.minus(nowSecondOfDay)?.takeIf { it > 0 }
    } else {
        endSec?.minus(nowSecondOfDay)?.takeIf { it > 0 }
    }

    val isFinalPart = remainingSec != null && remainingSec in 1..FINAL_SPRINT_SECONDS

    val upcomingEvents: List<ScheduleEvent> = remember(
        schedule, adjustments, todayDate, nowMinutes, activeDisplayEvent?.id
    ) {
        val activeId = activeDisplayEvent?.id
        upcomingEventsForDate(schedule, adjustments, todayDate, nowMinutes)
            .filter { it.id != activeId }
    }

    val todayAllEvents: List<ScheduleEvent> = remember(schedule, adjustments, todayDate) {
        eventsOnDate(schedule, adjustments, todayDate)
            .filter { it.enabled && it.type != ScheduleEventType.BREAK }
    }

    val pastEvents: List<ScheduleEvent> = remember(
        schedule, adjustments, todayDate, nowMinutes
    ) {
        eventsOnDate(schedule, adjustments, todayDate)
            .filter { it.type != ScheduleEventType.BREAK }
            .filter { (toMinutes(it.endTime) ?: Int.MAX_VALUE) <= nowMinutes }
            .sortedBy { toMinutes(it.endTime) ?: Int.MAX_VALUE }
    }

    val summaryLabel: String = remember(todayAllEvents, pastEvents) {
        when {
            todayAllEvents.isEmpty() -> "今天没有事件，好好休息吧!"
            pastEvents.size >= todayAllEvents.size -> "今日事件已全部完成"
            else -> "${todayAllEvents.size}个事件 ${pastEvents.size}个已完成"
        }
    }

    val pagerState = rememberPagerState(pageCount = { 2 })

    HorizontalPagerScaffold(pagerState = pagerState) {
        HorizontalPager(state = pagerState) { page ->
            KeepScreenOn(enabled = keepScreenOnSetting && isActive && page == 0)
            when (page) {
                0 -> CurrentEventPage(
                    activeDisplayEvent = activeDisplayEvent,
                    isPrep = isPrep,
                    progress = targetProgress,
                    remainingSec = remainingSec,
                    isFinalPart = isFinalPart,
                    displayWeekday = displayWeekday,
                    nowSecondOfDay = nowSecondOfDay,
                    schedule = schedule,
                    adjustments = adjustments,
                    todayDate = todayDate,
                    nowMinutes = nowMinutes,
                    pastEvents = pastEvents,
                    upcomingEvents = upcomingEvents,
                    summaryLabel = summaryLabel
                )
                1 -> MenuPage(onChangePage = onChangePage)
            }
        }
    }
}

@Composable
private fun eventColorFor(event: ScheduleEvent, weekday: Weekday): Color {
    val hex = event.courseColorByWeekday[weekday] ?: event.courseColor
    return hex?.let { parseColor(it) } ?: MaterialTheme.colorScheme.primary
}

@Composable
private fun CurrentEventPage(
    activeDisplayEvent: ScheduleEvent?,
    isPrep: Boolean,
    progress: Float,
    remainingSec: Int?,
    isFinalPart: Boolean,
    displayWeekday: Weekday,
    nowSecondOfDay: Int,
    schedule: List<ScheduleEvent>,
    adjustments: List<ScheduleAdjustment>,
    todayDate: String,
    nowMinutes: Int,
    pastEvents: List<ScheduleEvent>,
    upcomingEvents: List<ScheduleEvent>,
    summaryLabel: String
) {
    val scrollState = rememberTransformingLazyColumnState()
    val square = LocalScreenShape.current == ScreenShape.SQUARE
    val transformationSpec = rememberAdaptiveTransformationSpec(square)

    val activeEventIndex = if (activeDisplayEvent != null) 2 + pastEvents.size else null

    LaunchedEffect(activeEventIndex) {
        activeEventIndex?.let { index ->
            scrollState.animateScrollToItem(index)
        }
    }

    val dateLabel = remember(todayDate) {
        runCatching {
            val d = java.time.LocalDate.parse(todayDate)
            val w = dateStringToWeekday(todayDate)
            val weekdayText = w?.let { weekdayLabel(it) } ?: ""
            "${d.year}年${d.monthValue}月${d.dayOfMonth}日 $weekdayText".trim()
        }.getOrElse { todayDate }
    }

    ScreenScaffold(scrollState = scrollState) { contentPadding ->
        TransformingLazyColumn(
            state = scrollState,
            contentPadding = contentPadding
        ) {
            item {
                ListHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(
                            ListHeaderDefaults.minimumTopListContentPadding,
                            ListHeaderDefaults.minimumBottomListContentPadding,
                        ),
                    transformation = SurfaceTransformation(transformationSpec)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "今天")

                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                TitleCard(
                    onClick = { /* Do something */ },
                    title = { Text("今日日程") },
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                ) {
                    Text(summaryLabel)
                }
            }

            if (pastEvents.isNotEmpty()) {
                items(
                    count = pastEvents.size,
                    key = { pastEvents[it].id }
                ) { index ->
                    val event = pastEvents[index]
                    val isLast = index == pastEvents.lastIndex
                    ProgressFillCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .minimumVerticalContentPadding(
                                ButtonDefaults.minimumVerticalListContentPadding
                            )
                            .then(
                                if (isLast) {
                                    Modifier.minimumVerticalContentPadding(
                                        ButtonDefaults.minimumVerticalListContentPadding
                                    )
                                } else Modifier
                            ),
                        transformation = SurfaceTransformation(transformationSpec),
                        title = eventDisplayNameFor(event, displayWeekday),
                        timeRange = "${event.startTime} - ${event.endTime}",
                        progressColor = eventColorFor(event, displayWeekday),
                        progressLabel = "已结束",
                        isEnded = true
                    )
                }
            }

            if (activeDisplayEvent != null) {
                item {
                    val remainingText = if (remainingSec != null) {
                        if (remainingSec > 0) {
                            "剩余 %02d:%02d".format(remainingSec / 60, remainingSec % 60)
                        } else "00:00"
                    } else null

                    ProgressFillCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .minimumVerticalContentPadding(
                                ButtonDefaults.minimumVerticalListContentPadding
                            ),
                        transformation = SurfaceTransformation(transformationSpec),
                        title = eventDisplayNameFor(activeDisplayEvent, displayWeekday),
                        timeRange = if (isPrep) "即将开始" else "${activeDisplayEvent.startTime} - ${activeDisplayEvent.endTime}",
                        progress = progress,
                        progressLabel = remainingText,
                        progressColor = eventColorFor(activeDisplayEvent, displayWeekday),
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (upcomingEvents.isNotEmpty()) {
                items(
                    count = upcomingEvents.size,
                    key = { upcomingEvents[it].id }
                ) { index ->
                    val event = upcomingEvents[index]
                    val isLast = index == upcomingEvents.lastIndex
                    val eventStartSec = toMinutes(event.startTime)?.times(60)
                    val minutesUntil = eventStartSec?.let {
                        ((it - nowSecondOfDay) / 60).coerceAtLeast(0)
                    }

                    ProgressFillCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .minimumVerticalContentPadding(
                                ButtonDefaults.minimumVerticalListContentPadding
                            )
                            .then(
                                if (isLast) {
                                    Modifier.minimumVerticalContentPadding(
                                        ButtonDefaults.minimumVerticalListContentPadding
                                    )
                                } else Modifier
                            ),
                        transformation = SurfaceTransformation(transformationSpec),
                        title = eventDisplayNameFor(event, displayWeekday),
                        timeRange = "${event.startTime} - ${event.endTime}",
                        progressColor = eventColorFor(event, displayWeekday),
                        progressLabel = if (minutesUntil != null && minutesUntil > 0) {
                            "$minutesUntil 分钟后开始"
                        } else "即将开始"
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuPage(onChangePage: (AppKey) -> Unit) {
    val scrollState = rememberTransformingLazyColumnState()
    val square = LocalScreenShape.current == ScreenShape.SQUARE
    val transformationSpec = rememberAdaptiveTransformationSpec(square)

    ScreenScaffold(scrollState = scrollState) { contentPadding ->
        TransformingLazyColumn(
            state = scrollState,
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
                ) { Text(text = "更多") }
            }

            item {
                FilledTonalButton(
                    onClick = { onChangePage(TimeTableNavScreen) },
                    label = { Text("时间表") },
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                )
            }

            item {
                FilledTonalButton(
                    onClick = { onChangePage(CourseTableNavScreen) },
                    label = { Text("课程表") },
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Date_range,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                )
            }

            item {
                FilledTonalButton(
                    onClick = { onChangePage(CountdownNavScreen) },
                    label = { Text("倒计日") },
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Event,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                )
            }

            item {
                FilledTonalButton(
                    onClick = { onChangePage(TodoNavScreen) },
                    label = { Text("待办") },
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Checklist,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                )
            }

            item {
                FilledTonalButton(
                    onClick = { onChangePage(ToolMenuNavScreen) },
                    label = { Text("工具") },
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Handyman,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                )
            }

            item {
                FilledTonalButton(
                    onClick = { onChangePage(GameMenuNavScreen) },
                    label = { Text("小游戏") },
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.RoundedFilled.Gamepad,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                )
            }

            item {
                FilledTonalButton(
                    onClick = { onChangePage(SettingsNavScreen) },
                    label = { Text("设置") },
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                )
            }

            item {
                FilledTonalButton(
                    onClick = { onChangePage(AboutNavScreen) },
                    label = { Text("关于") },
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Info,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(
                            ButtonDefaults.minimumVerticalListContentPadding
                        ),
                    transformation = SurfaceTransformation(transformationSpec)
                )
            }
        }
    }
}