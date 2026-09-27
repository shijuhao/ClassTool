package com.juhao.classtool.ui.tool

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.transformedHeight
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.*
import com.juhao.classtool.theme.AppCardDefaults
import com.juhao.classtool.utils.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.LocalTime
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ToolCountdownScreen() {
    val listState = rememberTransformingLazyColumnState()
    val square = LocalScreenShape.current == ScreenShape.SQUARE
    val transformationSpec = rememberAdaptiveTransformationSpec(square)

    var settingMode by remember { mutableStateOf(false) }
    var totalMs by remember { mutableLongStateOf(60_000L) }
    var remainingMs by remember { mutableLongStateOf(60_000L) }
    var running by remember { mutableStateOf(false) }
    var lastTickMs by remember { mutableLongStateOf(0L) }

    var pickedTime by remember { mutableStateOf(LocalTime.of(0, 1, 0)) }

    LaunchedEffect(running) {
        if (running) {
            lastTickMs = System.currentTimeMillis()
            while (isActive) {
                delay(16L.milliseconds)
                val now = System.currentTimeMillis()
                val delta = now - lastTickMs
                lastTickMs = now
                remainingMs = (remainingMs - delta).coerceAtLeast(0L)
                if (remainingMs <= 0L) {
                    running = false
                    break
                }
            }
        }
    }

    val displayMs = if (remainingMs <= 0L) 0L else remainingMs
    val totalSeconds = displayMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val centis = (displayMs % 1000) / 10
    val finished = remainingMs <= 0L

    if (settingMode) {
        BackHandler {
            settingMode = false
        }
        TimePicker(
            initialTime = pickedTime,
            timePickerType = TimePickerType.HoursMinutesSeconds24H,
            onTimePicked = {
                pickedTime = it
                var ms = (
                    pickedTime.hour * 3600L +
                    pickedTime.minute * 60L +
                    pickedTime.second
                ) * 1000L
                if (ms < 1000L) ms = 1000L
                totalMs = ms
                remainingMs = ms
                running = false
                settingMode = false
            },
            modifier = Modifier.fillMaxWidth()
        )
        return
    }

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
                ) { Text(text = "倒计时") }
            }

            item {
                TitleCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .transformedHeight(this, transformationSpec),
                    colors = AppCardDefaults.cardColors(),
                    transformation = SurfaceTransformation(transformationSpec),
                    title = { Text(if (finished) "时间到" else "剩余") }
                ) {
                    Text(
                        text = if (hours > 0) {
                            "%d:%02d:%02d.%02d".format(hours, minutes, seconds, centis)
                        } else {
                            "%02d:%02d.%02d".format(minutes, seconds, centis)
                        },
                        style = MaterialTheme.typography.displayMedium
                    )
                }
            }

            item {
                ButtonGroup(
                    modifier = Modifier
                        .graphicsLayer {
                            with(transformationSpec) {
                                applyContainerTransformation(scrollProgress)
                            }
                        }
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(
                            ButtonDefaults.minimumVerticalListContentPadding
                        )
                ) {
                    FilledIconButton(
                        onClick = {
                            if (finished) {
                                remainingMs = totalMs
                                running = true
                            } else {
                                running = !running
                            }
                        },
                        modifier = Modifier.weight(1f),
                        content = {
                            Icon(
                                imageVector = if (running) MaterialSymbols.Rounded.Pause else MaterialSymbols.Rounded.Play_arrow,
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize)
                            )
                        }
                    )
                    FilledTonalIconButton(
                        onClick = {
                            running = false
                            remainingMs = totalMs
                        },
                        enabled = !running,
                        content = {
                            Icon(
                                imageVector = MaterialSymbols.Rounded.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                FilledTonalButton(
                    onClick = {
                        running = false
                        settingMode = true
                    },
                    enabled = !running,
                    label = { Text("设置时长") },
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