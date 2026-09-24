package com.juhao.classtool.ui.schedule

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
import com.juhao.classtool.datastore.ScheduleDataStore
import com.juhao.classtool.datastore.ScheduleEvent
import com.juhao.classtool.datastore.ScheduleEventType
import com.juhao.classtool.datastore.SettingsDataStore
import kotlinx.coroutines.delay

private const val PREP_BELL_SECONDS = 180
private const val FINAL_SPRINT_SECONDS = 180

private val funnyMessagesFar = listOf(
    "稳如老狗" to "(￣▽￣)",
    "时间还早，摸会儿鱼" to "( ˘ω˘ )",
    "一切尽在掌握" to "(๑•̀ㅂ•́)و"
)

private val funnyMessagesMid = listOf(
    "撑住，过半了" to "(ง •_•)ง",
    "还有一阵，别慌" to "(´･ω･`)",
    "保持节奏" to "( •̀ ω •́ )"
)

private val funnyMessagesNear = listOf(
    "快下课了，加把劲" to "٩(๑•̀ω•́๑)۶",
    "胜利就在前方" to "(๑•̀ㅂ•́)و✧",
    "再坚持一会儿" to "(｡•̀ᴗ-)✧"
)

private val funnyMessagesFinal = listOf(
    "最后冲刺！" to "ヽ(•̀ω•́ )ゝ",
    "马上结束！" to "(ﾉ>ω<)ﾉ",
    "冲鸭！" to "ヾ(≧▽≦*)o"
)

private val funnyMessagesPrep = listOf(
    "预备铃响啦，准备上课" to "🔔(•̀ᴗ•́)و",
    "要上课了，收收心" to "(๑•́ ₃ •̀๑)",
    "预备！" to "⏰(ง •̀_•́)ง"
)

private val funnyMessagesBreak = listOf(
    "课间休息，活动一下" to "☕(´▽`)",
    "喝口水，放松放松" to "🥤( ˘ω˘ )",
    "下课啦，随便逛逛" to "🐾(￣▽￣)"
)

private val funnyMessagesIdle = listOf(
    "摸鱼时间到" to "🐟(￣▽￣)",
    "自由活动，随便浪" to "( ˘ω˘ )",
    "闲着也是闲着" to "(´･ω･`)"
)

private fun funnyPool(tier: String): List<Pair<String, String>> = when (tier) {
    "prep" -> funnyMessagesPrep
    "break" -> funnyMessagesBreak
    "idle" -> funnyMessagesIdle
    "final" -> funnyMessagesFinal
    "near" -> funnyMessagesNear
    "mid" -> funnyMessagesMid
    else -> funnyMessagesFar
}

@Composable
fun ScheduleFullScreen(isActive: Boolean = true) {
    val context = LocalContext.current
    val store = remember { ScheduleDataStore(context) }
    val settingsStore = remember { SettingsDataStore(context) }

    val keepScreenOnSetting by settingsStore.keepScreenOnFlow.collectAsState(initial = false)
    KeepScreenOn(enabled = keepScreenOnSetting && isActive)

    var schedule by remember { mutableStateOf(emptyList<ScheduleEvent>()) }
    var prepBellEnabled by remember { mutableStateOf(true) }
    var nowSecondOfDay by remember { mutableIntStateOf(currentSecondOfDay()) }

    LaunchedEffect(Unit) {
        schedule = store.getSchedule().events
        prepBellEnabled = settingsStore.getPrepBell()
        while (true) {
            nowSecondOfDay = currentSecondOfDay()
            delay(1000L)
        }
    }

    val today = todayWeekday()
    val nowMinutes = nowSecondOfDay / 60

    val currentEvent = remember(schedule, today, nowMinutes) {
        schedule.firstOrNull { event ->
            event.enabled &&
                today in event.weekdays &&
                toMinutes(event.startTime)?.let { nowMinutes >= it } == true &&
                toMinutes(event.endTime)?.let { nowMinutes < it } == true
        }
    }

    val prepEvent = remember(schedule, today, nowSecondOfDay, prepBellEnabled) {
        if (!prepBellEnabled) return@remember null
        schedule.firstOrNull { event ->
            if (!event.enabled) return@firstOrNull false
            if (today !in event.weekdays) return@firstOrNull false
            if (event.type == ScheduleEventType.BREAK) return@firstOrNull false
            val startSec = toMinutes(event.startTime)?.times(60) ?: return@firstOrNull false
            nowSecondOfDay in (startSec - PREP_BELL_SECONDS) until startSec
        }
    }

    val isPrep = prepEvent != null
    val displayEvent = prepEvent ?: currentEvent

    val startSec = remember(displayEvent?.id, isPrep) {
        displayEvent?.let { toMinutes(it.startTime)?.times(60) }
    }
    val endSec = remember(displayEvent?.id, isPrep) {
        displayEvent?.let { toMinutes(it.endTime)?.times(60) }
    }

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

    val upcomingEvents = remember(schedule, today, nowMinutes, displayEvent?.id) {
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
            .sortedBy { it.startTime }
    }

    val isBreak = displayEvent?.type == ScheduleEventType.BREAK
    val isActivity = displayEvent?.type == ScheduleEventType.ACTIVITY
    val showFunny = displayEvent != null && !isActivity

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

    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberAdaptiveTransformationSpec(square)

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            val ev = displayEvent
            if (ev != null) {
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
                            text = eventDisplayName(ev),
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
                        text = if (isPrep) "即将开始" else "${ev.startTime} - ${ev.endTime}",
                        style = TextStyle(
                            fontSize = titleSize.sp,
                            lineHeight = (titleSize * 1.2f).sp
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                item {
                    LinearProgressIndicator(
                        progress = { progressAnim.value },
                        modifier = Modifier.fillMaxWidth(),
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
                            modifier = Modifier.fillMaxWidth(),
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
                                label = "funny"
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
                    ) { Text(text = "当前没有事件") }
                }

                item {
                    AnimatedContent(
                        targetState = funnyPair,
                        transitionSpec = {
                            fadeIn(tween(400)) togetherWith fadeOut(tween(400))
                        },
                        label = "funny"
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

                if (upcomingEvents.isEmpty()) {
                    item {
                        Text(
                            text = "今天没有安排，好好休息 ~",
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

            items(
                count = upcomingEvents.size,
                key = { index -> upcomingEvents[index].id }
            ) { index ->
                val event = upcomingEvents[index]
                ScheduleEventCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                    event = event
                )
            }
        }
    }
}