package com.juhao.classtool.ui.schedule

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.juhao.classtool.datastore.ScheduleDataStore
import com.juhao.classtool.datastore.ScheduleEvent
import com.juhao.classtool.datastore.ScheduleEventType
import com.juhao.classtool.datastore.Weekday
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun ViewScheduleScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val store = remember { ScheduleDataStore(context) }

    val weekdays = Weekday.entries.toList()
    val today = todayWeekday()
    val initialPage = weekdays.indexOf(today).coerceAtLeast(0)
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { weekdays.size }
    )

    val schedule by produceState(initialValue = emptyList<ScheduleEvent>()) {
        value = store.getSchedule().events
    }

    var nowMinutes by remember { mutableStateOf(currentMinutes()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMinutes = currentMinutes()
            delay(10_000L)
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
            val dayEvents = schedule
                .filter { it.weekday == weekday }
                .sortedBy { it.startTime }

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
                    
                    if (dayEvents.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "当天没有事件",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    items(
                        count = dayEvents.size,
                        key = { index -> dayEvents[index].id }
                    ) { index ->
                        val event = dayEvents[index]
                        val start = toMinutes(event.startTime)
                        val end = toMinutes(event.endTime)
                        val isNow = weekday == today &&
                            start != null && end != null &&
                            nowMinutes in start until end
                        val progress = if (isNow && end > start) {
                            (nowMinutes - start).toFloat() / (end - start).toFloat()
                        } else {
                            0f
                        }
                        ScheduleEventCard(
                            transformation = SurfaceTransformation(transformationSpec),
                            event = event,
                            highlighted = isNow,
                            progress = progress
                        )
                    }
                }
            }
        }
    }
}