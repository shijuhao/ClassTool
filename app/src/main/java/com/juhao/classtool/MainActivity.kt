package com.juhao.classtool

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.scene.Scene
import androidx.wear.compose.material3.*
import com.juhao.classtool.datastore.*
import com.juhao.classtool.navigation.*
import com.juhao.classtool.ui.countdown.*
import com.juhao.classtool.ui.schedule.*
import com.juhao.classtool.ui.main.GreetingScreen
import com.juhao.classtool.ui.components.RoundToast
import com.juhao.classtool.theme.WearAppTheme
import com.juhao.classtool.utils.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.time.Duration.Companion.milliseconds

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val scale = runBlocking {
            SettingsDataStore(newBase.applicationContext).getUiScale()
        }

        val config = Configuration(newBase.resources.configuration).apply {
            densityDpi = (newBase.resources.displayMetrics.densityDpi * scale).toInt()
        }

        super.attachBaseContext(
            newBase.createConfigurationContext(config)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WearApp()
        }
    }
}

@Composable
private fun SquareTimeText() {
    var currentTime by remember { mutableStateOf(LocalTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalTime.now()
            val delayMillis = (1000L - System.currentTimeMillis() % 1000L)
                .coerceAtLeast(1L)
            delay(delayMillis)
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Text(
            text = currentTime.format(DateTimeFormatter.ofPattern("HH:mm")),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f))
                .padding(horizontal = 10.dp)
        )
    }
}

private val EaseOut = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private val EaseIn = CubicBezierEasing(0.4f, 0f, 1f, 1f)

private const val DUR_IN = 200
private const val DUR_OUT = 160
private const val FADE_IN = 140
private const val FADE_OUT = 120

private val navTransitionSpec:
    AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform = {
    slideInHorizontally(
        initialOffsetX = { it / 6 },
        animationSpec = tween(DUR_IN, easing = EaseOut)
    ) + fadeIn(
        animationSpec = tween(FADE_IN, easing = EaseOut)
    ) togetherWith slideOutHorizontally(
        targetOffsetX = { -it / 8 },
        animationSpec = tween(DUR_OUT, easing = EaseIn)
    ) + fadeOut(
        animationSpec = tween(FADE_OUT, easing = EaseIn)
    )
}

private val navPopTransitionSpec:
    AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform = {
    fadeIn(
        animationSpec = tween(180, easing = EaseOut)
    ) + scaleIn(
        initialScale = 0.98f,
        animationSpec = tween(180, easing = EaseOut)
    ) togetherWith fadeOut(
        animationSpec = tween(120, easing = EaseIn)
    )
}

private val navPredictivePopTransitionSpec:
    AnimatedContentTransitionScope<Scene<NavKey>>.(Int) -> ContentTransform = { _ ->
    fadeIn(
        animationSpec = tween(160, easing = EaseOut)
    ) togetherWith fadeOut(
        animationSpec = tween(120, easing = EaseIn)
    )
}

@Composable
fun WearApp() {
    val backStack = rememberNavBackStack(MenuScreen)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activitySelection = remember { mutableStateOf<PendingActivitySelection?>(null) }
    val courseSelection = remember { mutableStateOf<PendingCourseSelection?>(null) }

    fun navigateTo(key: NavKey) {
        if (backStack.lastOrNull() != key) {
            backStack.add(key)
        }
    }

    fun showMessage(text: String) {
        RoundToast.show(
            context,
            text,
            RoundToast.LENGTH_SHORT
        )
    }

    val settingsDataStore = remember {
        SettingsDataStore(context)
    }

    val screenShapeMode by settingsDataStore
        .screenShapeModeFlow
        .collectAsState(
            initial = ScreenShapeMode.AUTO
        )

    val isSquare = rememberIsSquareScreen(screenShapeMode)

    val screenShape = if (isSquare) {
        ScreenShape.SQUARE
    } else {
        ScreenShape.ROUND
    }

    LaunchedEffect(isSquare) {
        RoundToast.squareMode = isSquare
    }

    val dynamicThemeEnabled by settingsDataStore
        .dynamicThemeFlow
        .collectAsState(
            initial = false
        )

    val useSystemColor by settingsDataStore
        .useSystemColorFlow
        .collectAsState(
            initial = true
        )

    val dayMode by settingsDataStore
        .dayModeFlow
        .collectAsState(
            initial = false
        )

    val appTheme by settingsDataStore
        .appThemeFlow
        .collectAsState(
            initial = null
        )

    val customColor by settingsDataStore
        .customColorFlow
        .collectAsState(
            initial = Color(0xFF9BD7FF)
        )

    val pureBlackBackground by settingsDataStore
        .pureBlackBackgroundFlow
        .collectAsState(
            initial = false
        )

    val dynamicThemeState = rememberDynamicThemeState(
        enabled = dynamicThemeEnabled,
        context = context
    )

    val scheduleStore = remember {
        ScheduleDataStore(context)
    }

    DisposableEffect(
        scheduleStore,
        settingsDataStore
    ) {
        val receiver = object : BroadcastReceiver() {

            override fun onReceive(
                c: Context?,
                intent: Intent?
            ) {
                if (intent?.action != Intent.ACTION_TIME_TICK) {
                    return
                }

                scope.launch {
                    if (!settingsDataStore.getGlobalEventReminder()) {
                        return@launch
                    }

                    if (backStack.lastOrNull() is MenuScreen) {
                        return@launch
                    }

                    val nowMinutes =
                        currentSecondOfDay() / 60

                    val todayDate = todayDateString()

                    val (events, adjustments) =
                        scheduleStore.getSchedule()

                    val displayWeekday = effectiveWeekdayOnDate(
                        todayDate,
                        adjustments
                    ) ?: todayWeekday()

                    val todayEvents = eventsOnDate(
                        events,
                        adjustments,
                        todayDate
                    )

                    todayEvents
                        .firstOrNull {
                            toMinutes(it.startTime) == nowMinutes
                        }
                        ?.let { event ->
                            showMessage(
                                "${
                                    eventDisplayNameFor(
                                        event,
                                        displayWeekday
                                    )
                                } 开始了"
                            )
                        }

                    if (settingsDataStore.getPrepBell()) {
                        todayEvents
                            .firstOrNull { event ->
                                if (event.type == ScheduleEventType.BREAK) {
                                    return@firstOrNull false
                                }

                                val startMin = toMinutes(event.startTime)
                                    ?: return@firstOrNull false

                                startMin - 3 == nowMinutes
                            }
                            ?.let { event ->
                                val name = event.courseNameByWeekday[
                                    displayWeekday
                                ] ?: event.courseName ?: "下一节课"

                                showMessage("$name 即将开始")
                            }
                    }
                }
            }
        }

        context.registerReceiver(
            receiver,
            IntentFilter(Intent.ACTION_TIME_TICK)
        )

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    LaunchedEffect(Unit) {
        val nearest = CountdownDataStore(context)
            .getDays()
            .map { it to daysUntil(it.dateMillis) }
            .filter { (_, d) -> d in 0..5 }
            .minByOrNull { (_, d) -> d }
            ?: return@LaunchedEffect

        val (item, remain) = nearest

        val text = when (remain) {
            0L -> "「${item.title}」就是今天"
            1L -> "「${item.title}」明天到来"
            else -> "「${item.title}」还有 $remain 天"
        }

        showMessage(text)
    }

    val entryProvider = remember {
        entryProvider<NavKey> {

            entry<MenuScreen> {
                GreetingScreen(
                    isActive = backStack.lastOrNull() is MenuScreen,
                    onChangePage = {
                        navigateTo(it)
                    }
                )
            }

            scheduleEntries(
                onBack = {
                    backStack.removeLastOrNull()
                },
                onNavigate = {
                    navigateTo(it)
                },
                activitySelection = activitySelection,
                courseSelection = courseSelection
            )

            countdownEntries(
                onBack = {
                    backStack.removeLastOrNull()
                },
                onNavigate = {
                    navigateTo(it)
                }
            )

            todoEntries(
                onBack = {
                    backStack.removeLastOrNull()
                },
                onNavigate = {
                    navigateTo(it)
                }
            )

            toolEntries(
                onNavigate = {
                    navigateTo(it)
                }
            )

            gameEntries(
                onNavigate = {
                    navigateTo(it)
                }
            )

            settingsEntries(
                onBack = {
                    backStack.removeLastOrNull()
                },
                onNavigate = {
                    navigateTo(it)
                }
            )
        }
    }

    WearAppTheme(
        useSystemColor = useSystemColor,
        dayMode = dayMode,
        theme = appTheme,
        customColor = customColor,
        eventColor = dynamicThemeState.color,
        eventUrgent = dynamicThemeState.urgent
    ) {
        CompositionLocalProvider(
            LocalScreenShape provides screenShape
        ) {
            AppScaffold(
                modifier = Modifier
                    .then(
                        if (pureBlackBackground && !dayMode) {
                            Modifier.background(Color.Black)
                        } else {
                            Modifier
                        }
                    )
                    .safeDrawingPadding(),
                timeText = {
                    if (screenShape == ScreenShape.SQUARE) {
                        SquareTimeText()
                    } else {
                        TimeText()
                    }
                }
            ) {
                NavDisplay(
                    backStack = backStack,
                    entryProvider = entryProvider,
                    transitionSpec = navTransitionSpec,
                    popTransitionSpec = navPopTransitionSpec,
                    predictivePopTransitionSpec = navPredictivePopTransitionSpec
                )
            }
        }
    }
}