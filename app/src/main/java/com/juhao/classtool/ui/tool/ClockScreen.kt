package com.juhao.classtool.ui.tool

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.transformedHeight
import com.juhao.classtool.theme.AppCardDefaults
import com.juhao.classtool.utils.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ClockScreen() {
    val listState = rememberTransformingLazyColumnState()
    val square = LocalScreenShape.current == ScreenShape.SQUARE
    val transformationSpec = rememberAdaptiveTransformationSpec(square)

    var currentDateTime by remember { mutableStateOf(LocalDateTime.now()) }

    KeepScreenOn()

    LaunchedEffect(Unit) {
        while (isActive) {
            currentDateTime = LocalDateTime.now()
            val delayMs = 1000L - (System.currentTimeMillis() % 1000L)
            delay(delayMs.milliseconds)
        }
    }

    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val weekFormatter = DateTimeFormatter.ofPattern("EEEE")

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
                ) { Text(text = "时钟") }
            }

            item {
                TitleCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .transformedHeight(this, transformationSpec),
                    colors = AppCardDefaults.cardColors(),
                    transformation = SurfaceTransformation(transformationSpec),
                    title = { Text("当前时间") }
                ) {
                    Text(
                        text = currentDateTime.format(timeFormatter),
                        style = MaterialTheme.typography.displayMedium
                    )
                }
            }

            item {
                TitleCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .transformedHeight(this, transformationSpec),
                    colors = AppCardDefaults.cardColors(),
                    transformation = SurfaceTransformation(transformationSpec),
                    title = { Text("日期") }
                ) {
                    Column {
                        Text(
                            text = currentDateTime.format(dateFormatter),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = currentDateTime.format(weekFormatter),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}