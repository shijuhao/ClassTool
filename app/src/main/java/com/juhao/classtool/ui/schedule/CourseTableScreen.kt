package com.juhao.classtool.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.transformedHeight
import com.juhao.classtool.datastore.Schedule
import com.juhao.classtool.datastore.ScheduleDataStore
import com.juhao.classtool.datastore.ScheduleEvent
import com.juhao.classtool.datastore.ScheduleEventType
import com.juhao.classtool.datastore.Weekday
import com.juhao.classtool.navigation.AppKey
import com.juhao.classtool.navigation.CoursePresetPickerNavScreen
import com.juhao.classtool.utils.*

@Composable
fun CourseTableScreen(
    modifier: Modifier = Modifier,
    onNavigate: (AppKey) -> Unit
) {
    val context = LocalContext.current
    val store = remember { ScheduleDataStore(context) }
    val weekdays = Weekday.entries.toList()
    val today = todayWeekday()
    val pagerState = rememberPagerState(
        initialPage = weekdays.indexOf(today).coerceAtLeast(0),
        pageCount = { weekdays.size }
    )
    val scheduleState by store.scheduleFlow.collectAsState(initial = Schedule())
    val schedule = scheduleState.events
    val adjustments = scheduleState.adjustments

    HorizontalPagerScaffold(
        pagerState = pagerState,
        modifier = modifier
    ) {
        HorizontalPager(state = pagerState) { page ->
            val weekday = weekdays[page]
            val date = dateForWeekdayThisWeek(weekday)
            val listState = rememberTransformingLazyColumnState()
            val square = LocalScreenShape.current == ScreenShape.SQUARE
            val transformationSpec = rememberAdaptiveTransformationSpec(square)
            val dayClasses = remember(schedule, adjustments, date) {
                eventsOnDate(schedule, adjustments, date)
                    .filter { it.type == ScheduleEventType.CLASS }
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
                        ) { Text("${weekdayLabel(weekday)} 课程") }
                    }

                    if (dayClasses.isEmpty()) {
                        item {
                            Text(
                                text = "这一天没有上课事件",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .transformedHeight(this, transformationSpec)
                                    .graphicsLayer {
                                        with(transformationSpec) {
                                            applyContainerTransformation(scrollProgress)
                                        }
                                    }
                            )
                        }
                    }

                    items(
                        count = dayClasses.size,
                        key = { dayClasses[it].id }
                    ) { index ->
                        val event = dayClasses[index]
                        val isLast = index == dayClasses.lastIndex

                        CourseEditButton(
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
                            weekday = weekday,
                            onClick = {
                                onNavigate(
                                    CoursePresetPickerNavScreen(
                                        CourseEditDraft(
                                            eventId = event.id,
                                            weekday = weekday,
                                            name = event.courseNameByWeekday[weekday]
                                                ?: event.courseName,
                                            color = event.courseColorByWeekday[weekday]
                                                ?: event.courseColor
                                        ),
                                        directSave = true
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

fun dateForWeekdayThisWeek(weekday: Weekday): String {
    val today = java.time.LocalDate.now()
    val diff = weekday.ordinal + 1 - today.dayOfWeek.value
    return today.plusDays(diff.toLong()).toString()
}

@Composable
private fun CourseEditButton(
    modifier: Modifier = Modifier,
    transformation: SurfaceTransformation? = null,
    event: ScheduleEvent,
    weekday: Weekday,
    onClick: () -> Unit
) {
    val name = event.courseNameByWeekday[weekday] ?: event.courseName
    val colorHex = event.courseColorByWeekday[weekday] ?: event.courseColor
    val color = colorHex?.let(::parseColor)
        ?: MaterialTheme.colorScheme.onSurfaceVariant

    FilledTonalButton(
        onClick = onClick,
        transformation = transformation,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        label = { Text(name ?: "点击设置课程") },
        secondaryLabel = {
            Text("${event.startTime} - ${event.endTime}")
        },
        icon = {
            Box(
                Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color)
            )
        },
        modifier = modifier
    )
}
