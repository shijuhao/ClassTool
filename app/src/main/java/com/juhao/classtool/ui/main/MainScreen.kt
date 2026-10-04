package com.juhao.classtool.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.transformedHeight
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.*
import com.composables.icons.materialsymbols.roundedfilled.Gamepad
import com.juhao.classtool.datastore.*
import com.juhao.classtool.navigation.*
import com.juhao.classtool.ui.schedule.*
import com.juhao.classtool.utils.*
import kotlinx.coroutines.delay
import java.time.LocalDate
import kotlin.time.Duration.Companion.milliseconds

private data class MenuEntry(val label: String, val icon: ImageVector, val key: AppKey)

private val menuEntries = listOf(
    MenuEntry("时间表", MaterialSymbols.Rounded.Schedule, TimeTableNavScreen),
    MenuEntry("课程表", MaterialSymbols.Rounded.Date_range, CourseTableNavScreen),
    MenuEntry("倒计日", MaterialSymbols.Rounded.Event, CountdownNavScreen),
    MenuEntry("待办", MaterialSymbols.Rounded.Checklist, TodoNavScreen),
    MenuEntry("工具", MaterialSymbols.Rounded.Handyman, ToolMenuNavScreen),
    MenuEntry("小游戏", MaterialSymbols.RoundedFilled.Gamepad, GameMenuNavScreen),
    MenuEntry("设置", MaterialSymbols.Rounded.Settings, SettingsNavScreen),
    MenuEntry("关于", MaterialSymbols.Rounded.Info, AboutNavScreen),
)

@Composable
fun GreetingScreen(
    isActive: Boolean = true,
    onChangePage: (AppKey) -> Unit
) {
    val context = LocalContext.current
    val store = remember { ScheduleDataStore(context) }
    val settingsStore = remember { SettingsDataStore(context) }

    val keepScreenOnSetting by settingsStore.keepScreenOnFlow.collectAsState(initial = false)

    val scheduleState by store.scheduleFlow.collectAsState(initial = Schedule())
    val schedule = scheduleState.events
    val adjustments = scheduleState.adjustments

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

    val currentEvent = remember(schedule, adjustments, todayDate, nowMinutes) {
        findCurrentEventOnDate(schedule, adjustments, todayDate, nowMinutes)
    }

    val prepEvent = remember(schedule, adjustments, todayDate, nowSecondOfDay, prepBellEnabled) {
        if (!prepBellEnabled) return@remember null
        eventsOnDate(schedule, adjustments, todayDate).firstOrNull { event ->
            if (event.type == ScheduleEventType.BREAK) return@firstOrNull false
            val startSec = toMinutes(event.startTime)?.times(60) ?: return@firstOrNull false
            nowSecondOfDay in (startSec - PREP_BELL_SECONDS) until startSec
        }
    }

    val activeDisplayEvent = prepEvent ?: currentEvent
    val isPrep = prepEvent != null

    val startSec = activeDisplayEvent?.let { toMinutes(it.startTime)?.times(60) }
    val endSec = activeDisplayEvent?.let { toMinutes(it.endTime)?.times(60) }

    val targetProgress = when {
        isPrep && startSec != null -> {
            val prepStart = startSec - PREP_BELL_SECONDS
            ((nowSecondOfDay - prepStart).toFloat() / (startSec - prepStart).toFloat()).coerceIn(0f, 1f)
        }
        startSec != null && endSec != null && endSec > startSec ->
            ((nowSecondOfDay - startSec).toFloat() / (endSec - startSec).toFloat()).coerceIn(0f, 1f)
        else -> 0f
    }

    val remainingSec = (if (isPrep) startSec else endSec)
        ?.minus(nowSecondOfDay)
        ?.takeIf { it > 0 }

    val isFinalPart = remainingSec != null && remainingSec in 1..FINAL_SPRINT_SECONDS

    val todayEvents = remember(schedule, adjustments, todayDate) {
        eventsOnDate(schedule, adjustments, todayDate)
            .filter { it.type != ScheduleEventType.BREAK }
    }

    val pastEvents = remember(todayEvents, nowMinutes) {
        todayEvents
            .filter { (toMinutes(it.endTime) ?: Int.MAX_VALUE) <= nowMinutes }
            .sortedBy { toMinutes(it.endTime) ?: Int.MAX_VALUE }
    }

    val upcomingEvents = remember(todayEvents, nowMinutes, activeDisplayEvent?.id) {
        todayEvents
            .filter { (toMinutes(it.startTime) ?: Int.MAX_VALUE) > nowMinutes }
            .filter { it.id != activeDisplayEvent?.id }
            .sortedBy { toMinutes(it.startTime) ?: Int.MAX_VALUE }
    }

    val summaryLabel = remember(todayEvents, pastEvents) {
        when {
            todayEvents.isEmpty() -> "今天没有事件，好好休息吧!"
            pastEvents.size >= todayEvents.size -> "今日事件已全部完成"
            else -> "${todayEvents.size}个事件 ${pastEvents.size}个已完成"
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
    todayDate: String,
    nowMinutes: Int,
    pastEvents: List<ScheduleEvent>,
    upcomingEvents: List<ScheduleEvent>,
    summaryLabel: String
) {
    val scrollState = rememberTransformingLazyColumnState()
    val square = LocalScreenShape.current == ScreenShape.SQUARE
    val transformationSpec = rememberAdaptiveTransformationSpec(square)

    val activeEventIndex = activeDisplayEvent?.let { 2 + pastEvents.size }

    LaunchedEffect(activeEventIndex) {
        activeEventIndex?.let { scrollState.animateScrollToItem(it) }
    }

    val dateLabel = remember(todayDate) {
        runCatching {
            val d = LocalDate.parse(todayDate)
            val w = dateStringToWeekday(todayDate)
            val weekdayText = w?.let { weekdayLabel(it) } ?: ""
            "${d.year}年${d.monthValue}月${d.dayOfMonth}日 $weekdayText".trim()
        }.getOrElse { todayDate }
    }

    ScreenScaffold(scrollState = scrollState) { contentPadding ->
        TransformingLazyColumn(
            state = scrollState,
            contentPadding = contentPadding,
            flingBehavior = TransformingLazyColumnDefaults.snapFlingBehavior(state = scrollState),
            rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(
                scrollableState = scrollState
            )
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
                    onClick = { },
                    title = { Text("今日日程") },
                    transformation = SurfaceTransformation(transformationSpec),
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                ) {
                    Text(summaryLabel)
                }
            }

            items(
                count = pastEvents.size,
                key = { pastEvents[it].id }
            ) { index ->
                val event = pastEvents[index]
                ProgressFillCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(
                            ButtonDefaults.minimumVerticalListContentPadding
                        ),
                    transformation = SurfaceTransformation(transformationSpec),
                    title = eventDisplayNameFor(event, displayWeekday),
                    timeRange = "${event.startTime} - ${event.endTime}",
                    progressColor = eventColorFor(event, displayWeekday),
                    progressLabel = "已结束",
                    isEnded = true
                )
            }

            if (activeDisplayEvent != null) {
                item {
                    val remainingText = remainingSec?.let {
                        "剩余 %02d:%02d".format(it / 60, it % 60)
                    }
                    ProgressFillCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .minimumVerticalContentPadding(
                                ButtonDefaults.minimumVerticalListContentPadding
                            ),
                        transformation = SurfaceTransformation(transformationSpec),
                        title = eventDisplayNameFor(activeDisplayEvent, displayWeekday),
                        timeRange = if (isPrep) "即将开始"
                            else "${activeDisplayEvent.startTime} - ${activeDisplayEvent.endTime}",
                        progress = progress,
                        progressLabel = remainingText,
                        progressColor = eventColorFor(activeDisplayEvent, displayWeekday),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            items(
                count = upcomingEvents.size,
                key = { upcomingEvents[it].id }
            ) { index ->
                val event = upcomingEvents[index]
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

@Composable
private fun MenuPage(onChangePage: (AppKey) -> Unit) {
    val scrollState = rememberTransformingLazyColumnState()
    val square = LocalScreenShape.current == ScreenShape.SQUARE
    val transformationSpec = rememberAdaptiveTransformationSpec(square)

    ScreenScaffold(scrollState = scrollState) { contentPadding ->
        TransformingLazyColumn(state = scrollState, contentPadding = contentPadding) {
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

            items(
                count = menuEntries.size,
                key = { menuEntries[it].label }
            ) { index ->
                val entry = menuEntries[index]
                FilledTonalButton(
                    onClick = { onChangePage(entry.key) },
                    label = { Text(entry.label) },
                    icon = {
                        Icon(
                            imageVector = entry.icon,
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