package com.juhao.classtool

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.PagerState
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.navigation3.rememberSwipeDismissableSceneStrategy
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.*
import com.composables.icons.materialsymbols.roundedfilled.Gamepad

import com.juhao.classtool.ui.countdown.*
import com.juhao.classtool.datastore.*
import com.juhao.classtool.navigation.*
import com.juhao.classtool.utils.*
import com.juhao.classtool.ui.schedule.*
import com.juhao.classtool.ui.components.RoundToast
import com.juhao.classtool.theme.WearAppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.milliseconds

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val scale = runBlocking {
            SettingsDataStore(newBase.applicationContext).getUiScale()
        }
        val config = Configuration(newBase.resources.configuration).apply {
            densityDpi = (newBase.resources.displayMetrics.densityDpi * scale).toInt()
        }
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WearApp()
        }
    }
}

@Composable
fun WearApp() {
    val backStack = rememberNavBackStack(MenuScreen)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun showMessage(text: String) {
        RoundToast.show(context, text, RoundToast.LENGTH_SHORT)
    }

    val settingsDataStore = remember { SettingsDataStore(context) }
    var testModeLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        TestModeState.enabled = settingsDataStore.getTestMode()
        testModeLoaded = true
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { 2 }
    )

    val screenShapeMode by settingsDataStore.screenShapeModeFlow.collectAsState(
        initial = ScreenShapeMode.AUTO
    )
    val isSquare = rememberIsSquareScreen(screenShapeMode)
    val screenShape = if (isSquare) ScreenShape.SQUARE else ScreenShape.ROUND

    val dynamicThemeEnabled by settingsDataStore.dynamicThemeFlow.collectAsState(initial = false)
    var currentEventColor by remember { mutableStateOf<Color?>(null) }
    var currentEventUrgent by remember { mutableStateOf(false) }

    LaunchedEffect(dynamicThemeEnabled, testModeLoaded) {
        if (!dynamicThemeEnabled || !testModeLoaded) {
            currentEventColor = null
            currentEventUrgent = false
            return@LaunchedEffect
        }
        val scheduleStore = ScheduleDataStore(context)
        scheduleStore.scheduleFlow.collectLatest { schedule ->
            while (true) {
                val events = schedule.events
                val today = todayWeekday()
                val nowSec = currentSecondOfDay()
                val nowMinutes = nowSec / 60

                val active = events.firstOrNull { event ->
                    event.enabled &&
                        today in event.weekdays &&
                        toMinutes(event.startTime)?.let { nowMinutes >= it } == true &&
                        toMinutes(event.endTime)?.let { nowMinutes < it } == true
                }
                currentEventColor = active?.courseColor?.let { parseColor(it) }

                val remaining = active?.let {
                    (toMinutes(it.endTime) ?: Int.MAX_VALUE) - nowMinutes
                } ?: Int.MAX_VALUE
                currentEventUrgent = active?.urgent == true && remaining in 0..10

                val nowSecLong = nowSec.toLong()

                val nextBoundary = events
                    .asSequence()
                    .filter { it.enabled && today in it.weekdays }
                    .flatMap { event ->
                        val start = toMinutes(event.startTime)?.toLong()?.times(60L)
                        val end = toMinutes(event.endTime)?.toLong()?.times(60L)
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

    val themeEventColor = if (dynamicThemeEnabled) currentEventColor else null
    val themeEventUrgent = if (dynamicThemeEnabled) currentEventUrgent else false

    if (testModeLoaded) {
        val scheduleStore = remember(TestModeState.enabled) { ScheduleDataStore(context) }

        LaunchedEffect(scheduleStore, settingsDataStore) {
            val events = scheduleStore.getSchedule().events
            val prepBellEnabled = settingsDataStore.getPrepBell()
            val today = todayWeekday()
            val nowSec = currentSecondOfDay()
            val nowMinutes = nowSec / 60
            val hasCurrent = events.any { event ->
                event.enabled &&
                    today in event.weekdays &&
                    toMinutes(event.startTime)?.let { nowMinutes >= it } == true &&
                    toMinutes(event.endTime)?.let { nowMinutes < it } == true
            }
            val inPrep = prepBellEnabled && events.any { event ->
                if (!event.enabled) return@any false
                if (today !in event.weekdays) return@any false
                if (event.type == ScheduleEventType.BREAK) return@any false
                val startSec = toMinutes(event.startTime)?.times(60) ?: return@any false
                nowSec in (startSec - 180) until startSec
            }
            if (hasCurrent || inPrep) {
                pagerState.animateScrollToPage(1)
            }
        }

        DisposableEffect(scheduleStore, settingsDataStore) {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: Intent?) {
                    if (intent?.action != Intent.ACTION_TIME_TICK) return
                    scope.launch {
                        val globalReminderEnabled = settingsDataStore.getGlobalEventReminder()
                        val onHomeSecondPage =
                            backStack.lastOrNull() is MenuScreen && pagerState.currentPage == 1
                        if (!globalReminderEnabled || onHomeSecondPage) return@launch

                        val nowMinutes = currentSecondOfDay() / 60
                        val today = todayWeekday()
                        val events = scheduleStore.getSchedule().events
                        val prepEnabled = settingsDataStore.getPrepBell()

                        events.firstOrNull {
                            it.enabled &&
                                today in it.weekdays &&
                                toMinutes(it.startTime) == nowMinutes
                        }?.let { event ->
                            showMessage("${eventDisplayName(event)} 开始了")
                        }

                        if (prepEnabled) {
                            events.firstOrNull {
                                it.enabled &&
                                    today in it.weekdays &&
                                    it.type != ScheduleEventType.BREAK &&
                                    toMinutes(it.startTime)?.minus(3) == nowMinutes
                            }?.let { event ->
                                val name = event.courseName ?: "下一节课"
                                showMessage("$name 即将开始")
                            }
                        }
                    }
                }
            }
            context.registerReceiver(receiver, IntentFilter(Intent.ACTION_TIME_TICK))
            onDispose {
                context.unregisterReceiver(receiver)
            }
        }
    }

    var startupCountdownChecked by remember { mutableStateOf(false) }
    LaunchedEffect(testModeLoaded) {
        if (!testModeLoaded || startupCountdownChecked) return@LaunchedEffect
        startupCountdownChecked = true

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

    val entryProvider =
        remember {
            entryProvider<NavKey> {
                entry<MenuScreen> {
                    GreetingScreen(
                        pagerState = pagerState,
                        isActive = backStack.lastOrNull() is MenuScreen && pagerState.currentPage == 1,
                        onChangePage = { backStack.add(it) }
                    )
                }
    
                scheduleEntries()
    
                countdownEntries(
                    onBack = { backStack.removeLastOrNull() },
                    onNavigate = { backStack.add(it) },
                )
    
                todoEntries(
                    onBack = { backStack.removeLastOrNull() },
                    onNavigate = { backStack.add(it) },
                )
    
                toolEntries(
                    onNavigate = { backStack.add(it) },
                )
    
                gameEntries(
                    onNavigate = { backStack.add(it) },
                )
    
                settingsEntries(
                    onNavigate = { backStack.add(it) },
                )
            }
        }

    WearAppTheme(eventColor = themeEventColor, eventUrgent = themeEventUrgent) {
        CompositionLocalProvider(
            LocalScreenShape provides screenShape
        ) {
            AppScaffold {
                val swipeDismissableSceneStrategy = rememberSwipeDismissableSceneStrategy<NavKey>()

                NavDisplay(
                    backStack = backStack,
                    entryProvider = entryProvider,
                    sceneStrategies = listOf(swipeDismissableSceneStrategy)
                )
            }
        }
    }
}

@Composable
fun GreetingScreen(
    pagerState: PagerState,
    isActive: Boolean = true,
    onChangePage: (AppKey) -> Unit
) {
    HorizontalPagerScaffold(pagerState = pagerState) {
        HorizontalPager(
            state = pagerState
        ) { page ->
            when (page) {
                0 -> MainScreen(onChangePage = onChangePage)
                else -> ScheduleFullScreen(isActive = isActive)
            }
        }
    }
}

@Composable
fun MainScreen(
    onChangePage: (AppKey) -> Unit
) {
    val scrollState = rememberTransformingLazyColumnState()
    val square = LocalScreenShape.current == ScreenShape.SQUARE
    val transformationSpec = rememberAdaptiveTransformationSpec(square)

    ScreenScaffold(
        scrollState = scrollState
    ) { contentPadding ->
        TransformingLazyColumn(
            state = scrollState,
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
                ) { Text(text = "ClassTool") }
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
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
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
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
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
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
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
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
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
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
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
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
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
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
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
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                )
            }
        }
    }
}