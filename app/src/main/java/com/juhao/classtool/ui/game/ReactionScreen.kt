package com.juhao.classtool.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.transformedHeight
import com.juhao.classtool.ui.schedule.*
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

private enum class ReactionPhase { IDLE, WAITING, READY, RESULT, TOO_SOON }

@Composable
fun ReactionScreen(modifier: Modifier = Modifier) {
    val listState = rememberTransformingLazyColumnState()
    val square = LocalScreenShape.current == ScreenShape.SQUARE
    val transformationSpec = rememberAdaptiveTransformationSpec(square)
    val haptic = LocalHapticFeedback.current

    var phase by remember { mutableStateOf(ReactionPhase.IDLE) }
    var resultMs by remember { mutableLongStateOf(0L) }
    var readyAt by remember { mutableLongStateOf(0L) }

    LaunchedEffect(phase) {
        if (phase == ReactionPhase.WAITING) {
            delay(Random.nextLong(1500, 4000).milliseconds)
            if (phase == ReactionPhase.WAITING) {
                readyAt = System.currentTimeMillis()
                phase = ReactionPhase.READY
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }

    val statusText = when (phase) {
        ReactionPhase.IDLE -> "点击开始"
        ReactionPhase.WAITING -> "等待变绿…"
        ReactionPhase.READY -> "点！"
        ReactionPhase.RESULT -> "用时 $resultMs ms"
        ReactionPhase.TOO_SOON -> "太早了！"
    }

    val buttonColor = when (phase) {
        ReactionPhase.READY -> Color(0xFF2E7D32)
        ReactionPhase.TOO_SOON -> Color(0xFFB71C1C)
        else -> Color.Transparent
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
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .minimumVerticalContentPadding(
                                ListHeaderDefaults.minimumTopListContentPadding
                            ),
                    transformation = SurfaceTransformation(transformationSpec)
                ) { Text(text = "反应力测试") }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(CircleShape)
                        .background(buttonColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = statusText,
                        textAlign = TextAlign.Center
                    )
                }
            }

            item {
                Button(
                    label = {
                        Text(
                            text = when (phase) {
                                ReactionPhase.IDLE -> "开始"
                                ReactionPhase.WAITING -> "别急…"
                                ReactionPhase.READY -> "点！"
                                else -> "再来一次"
                            },
                            modifier = modifier.fillMaxWidth()
                        )
                    },
                    onClick = {
                        when (phase) {
                            ReactionPhase.IDLE, ReactionPhase.RESULT, ReactionPhase.TOO_SOON -> {
                                phase = ReactionPhase.WAITING
                            }
                            ReactionPhase.WAITING -> {
                                phase = ReactionPhase.TOO_SOON
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            ReactionPhase.READY -> {
                                resultMs = System.currentTimeMillis() - readyAt
                                phase = ReactionPhase.RESULT
                            }
                        }
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                )
            }
        }
    }
}