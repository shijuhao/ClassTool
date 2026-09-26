package com.juhao.classtool.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.transformedHeight
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.*
import com.composables.icons.materialsymbols.roundedfilled.Gamepad
import com.juhao.classtool.datastore.ScheduleDataStore
import com.juhao.classtool.datastore.ScheduleEvent
import com.juhao.classtool.datastore.ScheduleEventType
import com.juhao.classtool.datastore.SettingsDataStore
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
    KeepScreenOn(enabled = keepScreenOnSetting && isActive)

    val scheduleFlow: Flow<List<ScheduleEvent>> = store.scheduleFlow.map { it.events }
    val schedule: List<ScheduleEvent> by scheduleFlow.collectAsState(initial = emptyList())

    var prepBellEnabled by remember { mutableStateOf(true) }
    var nowSecondOfDay by remember { mutableIntStateOf(currentSecondOfDay()) }

    LaunchedEffect(Unit) {
        prepBellEnabled = settingsStore.getPrepBell()
        while (true) {
            nowSecondOfDay = currentSecondOfDay()
            delay(1000L.milliseconds)
        }
    }

    val today = todayWeekday()
    val nowMinutes = nowSecondOfDay / 60

    val currentEvent: ScheduleEvent? = remember(schedule, today, nowMinutes) {
        schedule.firstOrNull { event ->
            event.enabled &&
                today in event.weekdays &&
                toMinutes(event.startTime)?.let { nowMinutes >= it } == true &&
                toMinutes(event.endTime)?.let { nowMinutes < it } == true
        }
    }

    val prepEvent: ScheduleEvent? = remember(schedule, today, nowSecondOfDay, prepBellEnabled) {
        if (!prepBellEnabled) return@remember null
        schedule.firstOrNull { event ->
            if (!event.enabled) return@firstOrNull false
            if (today !in event.weekdays) return@firstOrNull false
            if (event.type == ScheduleEventType.BREAK) return@firstOrNull false
            val startSec = toMinutes(event.startTime)?.times(60) ?: return@firstOrNull false
            nowSecondOfDay in (startSec - PREP_BELL_SECONDS) until startSec
        }
    }

    val displayEvent: ScheduleEvent? = prepEvent ?: currentEvent

    val startSec = displayEvent?.let { toMinutes(it.startTime)?.times(60) }
    val endSec = displayEvent?.let { toMinutes(it.endTime)?.times(60) }

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

    val progressAnim = remember { Animatable(0f) }
    LaunchedEffect(displayEvent?.id, isPrep) {
        if (displayEvent == null) progressAnim.snapTo(0f)
    }
    LaunchedEffect(targetProgress) {
        if (displayEvent != null) progressAnim.snapTo(targetProgress)
    }

    val remainingSec = if (isPrep) {
        startSec?.minus(nowSecondOfDay)?.takeIf { it > 0 }
    } else {
        endSec?.minus(nowSecondOfDay)?.takeIf { it > 0 }
    }

    val isFinalPart = remainingSec != null && remainingSec in 1..FINAL_SPRINT_SECONDS
    val finalPartProgress by animateFloatAsState(
        targetValue = if (isFinalPart) 1f else 0f,
        animationSpec = tween(400),
        label = "finalPart"
    )

    val square = LocalScreenShape.current == ScreenShape.SQUARE
    val displaySize = if (square) 28f else 32f
    val titleSize = if (square) 18f else 20f
    val mediumSize = 16f * (1f + 1.5f * finalPartProgress)

    val isActivity = displayEvent?.type == ScheduleEventType.ACTIVITY
    val showFunny = displayEvent != null && !isActivity
    val isBreak = displayEvent?.type == ScheduleEventType.BREAK

    val funnyTier = when {
        isPrep -> "prep"
        isBreak -> "break"
        displayEvent == null -> "idle"
        remainingSec == null -> "idle"
        remainingSec <= FINAL_SPRINT_SECONDS -> "final"
        targetProgress >= 0.75f -> "near"
        targetProgress >= 0.5f -> "mid"
        else -> "far"
    }
    val funnyPair = remember(funnyTier, displayEvent?.id) {
        funnyPool(funnyTier).random()
    }

    val nextEvent: ScheduleEvent? =
        remember(schedule, today, nowMinutes, displayEvent?.id) {
            val activeId = displayEvent?.id
            schedule
                .filter {
                    it.enabled &&
                        today in it.weekdays &&
                        it.type != ScheduleEventType.BREAK
                }
                .filter { event ->
                    if (event.id == activeId) return@filter false
                    val start = toMinutes(event.startTime) ?: return@filter false
                    start > nowMinutes
                }
                .minByOrNull { toMinutes(it.startTime) ?: Int.MAX_VALUE }
        }

    val nextEventMinutes: Int? = nextEvent?.let { event ->
        val s = toMinutes(event.startTime)?.times(60)
        if (s == null) null
        else ((s - nowSecondOfDay) / 60).coerceAtLeast(0)
    }

    val scrollState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberAdaptiveTransformationSpec(square)

    ScreenScaffold(scrollState = scrollState) { contentPadding ->
        TransformingLazyColumn(
            state = scrollState,
            contentPadding = contentPadding
        ) {
            if (displayEvent != null) {
                item {
                    ListHeader(
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .minimumVerticalContentPadding(
                                ListHeaderDefaults.minimumTopListContentPadding
                            ),
                        transformation = SurfaceTransformation(transformationSpec)
                    ) {
                        Text(
                            text = eventDisplayName(displayEvent),
                            style = TextStyle(
                                fontSize = displaySize.sp,
                                lineHeight = (displaySize * 1.2f).sp,
                                fontWeight = FontWeight.Normal
                            ),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    repeatDelayMillis = 1000,
                                    velocity = 30.dp
                                )
                        )
                    }
                }

                item {
                    Text(
                        text = if (isPrep) "即将开始"
                        else "${displayEvent.startTime} - ${displayEvent.endTime}",
                        style = TextStyle(
                            fontSize = titleSize.sp,
                            lineHeight = (titleSize * 1.2f).sp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .graphicsLayer {
                                with(transformationSpec) {
                                    applyContainerTransformation(scrollProgress)
                                }
                            },
                        textAlign = TextAlign.Center
                    )
                }

                item {
                    LinearProgressIndicator(
                        progress = { progressAnim.value },
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .graphicsLayer {
                                with(transformationSpec) {
                                    applyContainerTransformation(scrollProgress)
                                }
                            },
                        colors = ProgressIndicatorDefaults.colors(
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                        )
                    )
                }

                if (remainingSec != null) {
                    item {
                        val remainingText = if (remainingSec > 0) {
                            if (isFinalPart) {
                                "%02d:%02d".format(remainingSec / 60, remainingSec % 60)
                            } else {
                                "剩余 %02d:%02d".format(remainingSec / 60, remainingSec % 60)
                            }
                        } else "00:00"
                        Text(
                            text = remainingText,
                            style = TextStyle(
                                fontSize = mediumSize.sp,
                                lineHeight = (mediumSize * 1.2f).sp
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .transformedHeight(this, transformationSpec)
                                .graphicsLayer {
                                    with(transformationSpec) {
                                        applyContainerTransformation(scrollProgress)
                                    }
                                },
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (showFunny) {
                        item {
                            AnimatedContent(
                                targetState = funnyPair,
                                transitionSpec = {
                                    fadeIn(tween(400)) togetherWith fadeOut(tween(400))
                                },
                                label = "funny",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .transformedHeight(this, transformationSpec)
                                    .graphicsLayer {
                                        with(transformationSpec) {
                                            applyContainerTransformation(scrollProgress)
                                        }
                                    }
                            ) { pair ->
                                Text(
                                    text = "${pair.first} ${pair.second}",
                                    style = TextStyle(
                                        fontSize = 13.sp,
                                        lineHeight = (13f * 1.2f).sp
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                item {
                    ListHeader(
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .minimumVerticalContentPadding(
                                ListHeaderDefaults.minimumTopListContentPadding
                            ),
                        transformation = SurfaceTransformation(transformationSpec)
                    ) { Text(text = "ClassTool") }
                }
                item {
                    AnimatedContent(
                        targetState = funnyPair,
                        transitionSpec = {
                            fadeIn(tween(400)) togetherWith fadeOut(tween(400))
                        },
                        label = "funny",
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .graphicsLayer {
                                with(transformationSpec) {
                                    applyContainerTransformation(scrollProgress)
                                }
                            }
                    ) { pair ->
                        Text(
                            text = "${pair.first} ${pair.second}",
                            style = TextStyle(
                                fontSize = 13.sp,
                                lineHeight = (13f * 1.2f).sp
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (nextEvent != null) {
                item {
                    Text(
                        text = "下一事件",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .graphicsLayer {
                                with(transformationSpec) {
                                    applyContainerTransformation(scrollProgress)
                                }
                            },
                        textAlign = TextAlign.Center
                    )
                }

                item {
                    ScheduleEventCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec),
                        event = nextEvent
                    )
                }

                item {
                    Text(
                        text = if (nextEventMinutes != null && nextEventMinutes > 0) {
                            "还有 $nextEventMinutes 分钟开始"
                        } else "即将开始",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .graphicsLayer {
                                with(transformationSpec) {
                                    applyContainerTransformation(scrollProgress)
                                }
                            },
                        textAlign = TextAlign.Center
                    )
                }
            }

            item {
                FilledTonalButton(
                    onClick = { onChangePage(EditScheduleNavScreen) },
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
                    onClick = { onChangePage(CourseScheduleNavScreen) },
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