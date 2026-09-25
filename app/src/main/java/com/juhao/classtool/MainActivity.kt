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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
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

import com.juhao.classtool.key.*
import com.juhao.classtool.datastore.*
import com.juhao.classtool.ui.about.AboutScreen
import com.juhao.classtool.ui.countdown.*
import com.juhao.classtool.ui.components.RoundToast
import com.juhao.classtool.ui.game.*
import com.juhao.classtool.ui.game.gamemenu.GameMenu
import com.juhao.classtool.ui.schedule.*
import com.juhao.classtool.ui.settings.BackupRestoreScreen
import com.juhao.classtool.ui.settings.SettingsScreen
import com.juhao.classtool.theme.WearAppTheme
import com.juhao.classtool.ui.tool.timer.TimerScreen
import com.juhao.classtool.ui.tool.toolmenu.ToolMenu
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

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

    val uiScale by settingsDataStore.uiScaleFlow.collectAsState(initial = 1.0f)
    val dynamicThemeEnabled by settingsDataStore.dynamicThemeFlow.collectAsState(initial = false)
    var currentEventColor by remember { mutableStateOf<Color?>(null) }

    LaunchedEffect(dynamicThemeEnabled, testModeLoaded) {
        if (!dynamicThemeEnabled || !testModeLoaded) {
            currentEventColor = null
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

                val nowSecLong = nowSec.toLong()

                val nextBoundary = events
                    .asSequence()
                    .filter { it.enabled && today in it.weekdays }
                    .flatMap { event ->
                        sequenceOf(
                            toMinutes(event.startTime)?.toLong()?.times(60L),
                            toMinutes(event.endTime)?.toLong()?.times(60L)
                        )
                    }
                    .filterNotNull()
                    .filter { it > nowSecLong }
                    .minOrNull()

                val sleepSec = nextBoundary
                    ?.minus(nowSecLong)
                    ?.coerceAtLeast(1L)
                    ?: (86400L - nowSecLong).coerceAtLeast(60L)

                delay(sleepSec * 1000L)
            }
        }
    }

    val themeEventColor = if (dynamicThemeEnabled) currentEventColor else null

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
                        onChangePage = { backStack.add(it) }
                    )
                }

                entry<EditScheduleNavScreen> {
                    EditScheduleScreen()
                }
                entry<CourseScheduleNavScreen> {
                    CourseScheduleScreen()
                }

                entry<CountdownNavScreen> {
                    CountdownScreen(
                        onNavigate = { key -> backStack.add(key as NavKey) }
                    )
                }
                entry<AddCountdownNavScreen> {
                    CountdownEditScreen(
                        dayId = null,
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<EditCountdownNavScreen> { key ->
                    CountdownEditScreen(
                        dayId = key.id,
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<CountdownDetailNavScreen> { key ->
                    CountdownDetailScreen(
                        dayId = key.id,
                        onEdit = { backStack.add(EditCountdownNavScreen(key.id)) }, // ← 带上 id
                        onBack = { backStack.removeLastOrNull() }
                    )
                }

                entry<ToolMenuNavScreen> {
                    ToolMenu(onChangePage = { backStack.add(it) })
                }
                entry<TimerNavScreen> {
                    TimerScreen()
                }

                entry<GameMenuNavScreen> {
                    GameMenu(onChangePage = { backStack.add(it) })
                }
                entry<ReactionNavScreen> {
                    ReactionScreen()
                }
                entry<CoinNavScreen> {
                    CoinScreen()
                }
                entry<DiceNavScreen> {
                    DiceScreen()
                }

                entry<SettingsNavScreen> {
                    SettingsScreen(
                        onNavigateToBackup = { backStack.add(BackupRestoreNavScreen) }
                    )
                }
                entry<BackupRestoreNavScreen> {
                    BackupRestoreScreen()
                }
                entry<AboutNavScreen> {
                    AboutScreen()
                }
            }
        }

    WearAppTheme(eventColor = themeEventColor) {
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
    onChangePage: (AppKey) -> Unit
) {
    HorizontalPagerScaffold(pagerState = pagerState) {
        HorizontalPager(
            state = pagerState
        ) { page ->
            when (page) {
                0 -> MainScreen(onChangePage = onChangePage)
                else -> ScheduleFullScreen()
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
                            painter = painterResource(R.drawable.access_time),
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
                            painter = painterResource(R.drawable.date_range),
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
                            painter = painterResource(R.drawable.event),
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
                            painter = painterResource(R.drawable.build),
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
                            painter = painterResource(R.drawable.toys),
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
                            painter = painterResource(R.drawable.settings),
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
                            painter = painterResource(R.drawable.info),
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