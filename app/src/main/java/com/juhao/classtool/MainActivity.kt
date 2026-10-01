package com.juhao.classtool

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.wear.compose.navigation3.rememberSwipeDismissableSceneStrategy
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
import kotlinx.coroutines.flow.collectLatest
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
    var currentTime by remember {
        mutableStateOf(LocalTime.now())
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalTime.now()

            val delayMillis = (1000L - System.currentTimeMillis() % 1000L)
                .coerceAtLeast(1L)

            delay(delayMillis)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 12.dp,
                vertical = 4.dp
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Text(
            text = currentTime.format(
                DateTimeFormatter.ofPattern("HH:mm")
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun WearApp() {
    val backStack = rememberNavBackStack(MenuScreen)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

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

    var currentEventColor by remember {
        mutableStateOf<Color?>(null)
    }

    var currentEventUrgent by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(dynamicThemeEnabled) {
        if (!dynamicThemeEnabled) {
            currentEventColor = null
            currentEventUrgent = false
            return@LaunchedEffect
        }

        val scheduleStore = ScheduleDataStore(context)

        scheduleStore.tablesFlow.collectLatest {
            while (true) {
                val schedule = scheduleStore.getSchedule()
                val events = schedule.events
                val adjustments = schedule.adjustments

                val todayDate = todayDateString()

                val displayWeekday = effectiveWeekdayOnDate(
                    todayDate,
                    adjustments
                ) ?: todayWeekday()

                val nowSec = currentSecondOfDay()
                val nowMinutes = nowSec / 60

                val active = findCurrentEventOnDate(
                    events,
                    adjustments,
                    todayDate,
                    nowMinutes
                )

                val activeColorHex = active?.let {
                    it.courseColorByWeekday[displayWeekday] ?: it.courseColor
                }

                currentEventColor = activeColorHex?.let {
                    parseColor(it)
                }

                val remaining = active?.let {
                    (toMinutes(it.endTime) ?: Int.MAX_VALUE) - nowMinutes
                } ?: Int.MAX_VALUE

                currentEventUrgent =
                    active?.urgent == true && remaining in 0..10

                val nowSecLong = nowSec.toLong()

                val nextBoundary = eventsOnDate(
                    events,
                    adjustments,
                    todayDate
                )
                    .asSequence()
                    .flatMap { effective ->
                        val start = toMinutes(effective.startTime)
                            ?.toLong()
                            ?.times(60L)

                        val end = toMinutes(effective.endTime)
                            ?.toLong()
                            ?.times(60L)

                        sequenceOf(
                            start,
                            end,
                            end?.minus(600L)
                        )
                    }
                    .filterNotNull()
                    .filter { it > nowSecLong }
                    .minOrNull()

                val sleepSec = nextBoundary
                    ?.minus(nowSecLong)
                    ?.coerceAtLeast(1L)
                    ?: (86400L - nowSecLong).coerceAtLeast(60L)

                delay((sleepSec * 1000L).milliseconds)
            }
        }
    }

    val themeEventColor = if (dynamicThemeEnabled) {
        currentEventColor
    } else {
        null
    }

    val themeEventUrgent = if (dynamicThemeEnabled) {
        currentEventUrgent
    } else {
        false
    }

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
                    val globalReminderEnabled =
                        settingsDataStore.getGlobalEventReminder()

                    val onHomePage =
                        backStack.lastOrNull() is MenuScreen

                    if (!globalReminderEnabled || onHomePage) {
                        return@launch
                    }

                    val nowMinutes =
                        currentSecondOfDay() / 60

                    val todayDate = todayDateString()

                    val schedule = scheduleStore.getSchedule()

                    val events = schedule.events
                    val adjustments = schedule.adjustments

                    val displayWeekday = effectiveWeekdayOnDate(
                        todayDate,
                        adjustments
                    ) ?: todayWeekday()

                    val prepEnabled =
                        settingsDataStore.getPrepBell()

                    eventsOnDate(
                        events,
                        adjustments,
                        todayDate
                    )
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

                    if (prepEnabled) {
                        eventsOnDate(
                            events,
                            adjustments,
                            todayDate
                        )
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
        val countdownStore = CountdownDataStore(context)

        val upcoming = countdownStore.getDays()
            .map { it to daysUntil(it.dateMillis) }
            .filter { (_, d) -> d in 0..5 }
            .sortedBy { (_, d) -> d }

        if (upcoming.isNotEmpty()) {
            val (nearest, remain) = upcoming.first()

            val text = when (remain) {
                0L -> "「${nearest.title}」就是今天"
                1L -> "「${nearest.title}」明天到来"
                else -> "「${nearest.title}」还有 $remain 天"
            }

            showMessage(text)
        }
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

            scheduleEntries()

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
                onNavigate = {
                    navigateTo(it)
                }
            )
        }
    }

    WearAppTheme(
        eventColor = themeEventColor,
        eventUrgent = themeEventUrgent
    ) {
        CompositionLocalProvider(
            LocalScreenShape provides screenShape
        ) {
            AppScaffold(
                modifier = Modifier.safeDrawingPadding(),
                timeText = {
                    if (screenShape == ScreenShape.SQUARE) {
                        SquareTimeText()
                    } else {
                        TimeText()
                    }
                }
            ) {
                val swipeDismissableSceneStrategy =
                    rememberSwipeDismissableSceneStrategy<NavKey>()

                NavDisplay(
                    backStack = backStack,
                    entryProvider = entryProvider,
                    sceneStrategies = listOf(
                        swipeDismissableSceneStrategy
                    )
                )
            }
        }
    }
}