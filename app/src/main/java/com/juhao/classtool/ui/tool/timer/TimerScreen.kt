package com.juhao.classtool.ui.tool.timer

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.juhao.classtool.R
import com.juhao.classtool.theme.AppCardDefaults
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun TimerScreen() {
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    var elapsedMs by remember { mutableLongStateOf(0L) }
    var running by remember { mutableStateOf(false) }
    var lastStartMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(running) {
        if (running) {
            lastStartMs = System.currentTimeMillis()
            while (isActive) {
                val now = System.currentTimeMillis()
                elapsedMs += now - lastStartMs
                lastStartMs = now
                delay(16L.milliseconds)
            }
        }
    }

    val totalSeconds = elapsedMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val centis = (elapsedMs % 1000) / 10

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
                ) { Text(text = "秒表") }
            }

            item {
                TitleCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .transformedHeight(this, transformationSpec),
                    colors = AppCardDefaults.cardColors(),
                    transformation = SurfaceTransformation(transformationSpec),
                    title = { Text("时刻") }
                ) {
                    Text(
                        text = "%02d:%02d.%02d".format(minutes, seconds, centis),
                        style = MaterialTheme.typography.displayMedium
                    )
                }
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
                    FilledIconButton(
                        onClick = { running = !running },
                        modifier = Modifier.weight(1f),
                        content = {
                            Icon(
                                painter = painterResource(
                                    if (running) R.drawable.pause else R.drawable.play
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize)
                            )
                        }
                    )
                    FilledTonalIconButton(
                        onClick = {
                            running = false
                            elapsedMs = 0L
                            lastStartMs = 0L
                        },
                        enabled = !running,
                        content = {
                            Icon(
                                painter = painterResource(R.drawable.refresh),
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}