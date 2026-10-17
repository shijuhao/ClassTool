package com.juhao.classtool

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.*
import com.juhao.classtool.datastore.Schedule
import com.juhao.classtool.datastore.ScheduleDataStore
import com.juhao.classtool.ui.schedule.*
import kotlinx.coroutines.delay

@Composable
fun FloatingScheduleWindow() {
    val ctx = LocalContext.current
    val store = remember { ScheduleDataStore(ctx) }
    val scheduleState by store.scheduleFlow.collectAsState(initial = Schedule())
    var nowSecond by remember { mutableIntStateOf(currentSecondOfDay()) }

    LaunchedEffect(Unit) {
        while (true) {
            nowSecond = currentSecondOfDay()
            delay(1000L)
        }
    }

    val today = todayDateString()
    val adj = scheduleState.adjustments
    val nowMin = nowSecond / 60
    val currentEvent = remember(scheduleState, today, nowMin) {
        findCurrentEventOnDate(scheduleState.events, adj, today, nowMin)
    }

    val displayWeekday = remember(today, adj) {
        effectiveWeekdayOnDate(today, adj) ?: todayWeekday()
    }

    val startMin = currentEvent?.let { toMinutes(it.startTime) }
    val endMin = currentEvent?.let { toMinutes(it.endTime) }
    val progress = if (startMin != null && endMin != null && endMin > startMin) {
        ((nowMin - startMin).toFloat() / (endMin - startMin).toFloat()).coerceIn(0f, 1f)
    } else 0f

    val eventColor = currentEvent?.let { eventColorFor(it, displayWeekday) } ?: Color(0xFF4CAF50)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        if (currentEvent != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color(0x33FFFFFF), RoundedCornerShape(2.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(4.dp)
                        .background(eventColor, RoundedCornerShape(2.dp))
                )
            }
            Spacer(Modifier.height(6.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (currentEvent != null) {
                Text(
                    text = eventDisplayNameFor(currentEvent, displayWeekday),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${currentEvent.startTime} - ${currentEvent.endTime}",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
            } else {
                Text(
                    text = "暂无事件",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )
            }
        }
    }
}