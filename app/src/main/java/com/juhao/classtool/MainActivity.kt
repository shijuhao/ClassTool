package com.juhao.classtool

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.PagerDefaults
import androidx.wear.compose.foundation.pager.PagerState
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.navigation3.rememberSwipeDismissableSceneStrategy

import com.juhao.classtool.key.*
import com.juhao.classtool.datastore.ScheduleEventType
import com.juhao.classtool.datastore.ScheduleDataStore
import com.juhao.classtool.datastore.SettingsDataStore
import com.juhao.classtool.datastore.TestModeState
import com.juhao.classtool.ui.about.AboutScreen
import com.juhao.classtool.ui.game.coin.CoinScreen
import com.juhao.classtool.ui.game.gamemenu.GameMenu
import com.juhao.classtool.ui.schedule.*
import com.juhao.classtool.ui.settings.SettingsScreen
import com.juhao.classtool.theme.WearAppTheme
import com.juhao.classtool.ui.tool.timer.TimerScreen
import com.juhao.classtool.ui.tool.toolmenu.ToolMenu
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
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

    var confirmationMessage by remember { mutableStateOf<String?>(null) }

    fun showMessage(text: String) {
        confirmationMessage = text
    }

    var testModeLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        TestModeState.enabled = SettingsDataStore(context).getTestMode()
        testModeLoaded = true
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { 2 }
    )

    if (testModeLoaded) {
        val scheduleStore = remember(TestModeState.enabled) { ScheduleDataStore(context) }
        val settingsStore = remember(TestModeState.enabled) { SettingsDataStore(context) }

        LaunchedEffect(scheduleStore, settingsStore) {
            val events = scheduleStore.getSchedule().events
            val prepBellEnabled = settingsStore.getPrepBell()
            val today = todayWeekday()
            val nowSec = currentSecondOfDay()
            val nowMinutes = nowSec / 60
            val hasCurrent = events.any { event ->
                event.weekday == today &&
                    toMinutes(event.startTime)?.let { nowMinutes >= it } == true &&
                    toMinutes(event.endTime)?.let { nowMinutes < it } == true
            }
            val inPrep = prepBellEnabled && events.any { event ->
                if (event.weekday != today) return@any false
                if (event.type == ScheduleEventType.BREAK) return@any false
                val startSec = toMinutes(event.startTime)?.times(60) ?: return@any false
                nowSec in (startSec - 180) until startSec
            }
            if (hasCurrent || inPrep) {
                pagerState.animateScrollToPage(1)
            }
        }

        DisposableEffect(scheduleStore, settingsStore) {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: Intent?) {
                    if (intent?.action != Intent.ACTION_TIME_TICK) return
                    scope.launch {
                        val onHomeSecondPage =
                            backStack.lastOrNull() is MenuScreen && pagerState.currentPage == 1
                        if (onHomeSecondPage) return@launch

                        val nowMinutes = currentSecondOfDay() / 60
                        val today = todayWeekday()
                        val events = scheduleStore.getSchedule().events
                        val prepEnabled = settingsStore.getPrepBell()

                        events.firstOrNull {
                            it.weekday == today && toMinutes(it.startTime) == nowMinutes
                        }?.let { event ->
                            val name = event.courseName ?: when (event.type) {
                                ScheduleEventType.BREAK -> "课间休息"
                                ScheduleEventType.ACTIVITY -> "活动"
                                ScheduleEventType.CLASS -> "未命名"
                            }
                            showMessage("$name 开始了")
                        }

                        if (prepEnabled) {
                            events.firstOrNull {
                                it.weekday == today &&
                                    it.type != ScheduleEventType.BREAK &&
                                    toMinutes(it.startTime)?.minus(3) == nowMinutes
                            }?.let { event ->
                                val name = event.courseName ?: "下一节课"
                                showMessage("$name 预备铃")
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
                entry<ToolMenuNavScreen> {
                    ToolMenu(onChangePage = { backStack.add(it) })
                }
                entry<TimerNavScreen> {
                    TimerScreen()
                }
                entry<GameMenuNavScreen> {
                    GameMenu(onChangePage = { backStack.add(it) })
                }
                entry<CoinNavScreen> {
                    CoinScreen()
                }
                entry<SettingsNavScreen> {
                    SettingsScreen()
                }
                entry<AboutNavScreen> {
                    AboutScreen()
                }
            }
        }

    WearAppTheme {
        AppScaffold {
            val swipeDismissableSceneStrategy = rememberSwipeDismissableSceneStrategy<NavKey>()

            NavDisplay(
                backStack = backStack,
                entryProvider = entryProvider,
                sceneStrategies = listOf(swipeDismissableSceneStrategy)
            )
        }

        confirmationMessage?.let { message ->
            ConfirmationDialog(
                visible = true,
                onDismissRequest = { confirmationMessage = null },
                curvedText = {
                    confirmationDialogCurvedText(
                        message,
                        ConfirmationDialogDefaults.curvedTextStyle
                    )
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.info),
                    contentDescription = null,
                    modifier = Modifier.size(ConfirmationDialogDefaults.SmallIconSize)
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
            state = pagerState,
            flingBehavior = PagerDefaults.snapFlingBehavior(
                state = pagerState,
                maxFlingPages = 1,
                snapPositionalThreshold = PagerScaffoldDefaults.HighSnapPositionalThreshold,
                snapAnimationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
            ),
            rotaryScrollableBehavior = null
        ) { page ->
            AnimatedPage(pageIndex = page, pagerState = pagerState) {
                when (page) {
                    0 -> MainScreen(onChangePage = onChangePage)
                    else -> ScheduleFullScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    onChangePage: (AppKey) -> Unit
) {
    val scrollState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

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