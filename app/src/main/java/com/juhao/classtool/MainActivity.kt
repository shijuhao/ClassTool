package com.juhao.classtool

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.PagerDefaults
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.navigation3.rememberSwipeDismissableSceneStrategy
import com.juhao.classtool.R

import com.juhao.classtool.key.*
import com.juhao.classtool.ui.about.AboutScreen
import com.juhao.classtool.ui.game.coin.CoinScreen
import com.juhao.classtool.ui.game.gamemenu.GameMenu
import com.juhao.classtool.ui.schedule.*
import com.juhao.classtool.ui.settings.SettingsScreen

import androidx.compose.ui.platform.LocalContext
import com.juhao.classtool.datastore.ScheduleDataStore
import com.juhao.classtool.datastore.ScheduleEvent
import com.juhao.classtool.datastore.SettingsDataStore
import kotlinx.coroutines.delay

import com.juhao.classtool.theme.WearAppTheme

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

    WearAppTheme {
        AppScaffold {
            val entryProvider =
                remember {
                    entryProvider<NavKey> {
                        entry<MenuScreen> {
                            GreetingScreen(
                                onChangePage = { backStack.add(it) }
                            )
                        }
                        entry<ViewScheduleNavScreen> {
                            ViewScheduleScreen()
                        }
                        entry<EditScheduleNavScreen> {
                            EditScheduleScreen()
                        }
                        entry<GameMenuNavScreen> {
                            GameMenu(
                                onChangePage = { backStack.add(it) }
                            )
                        }
                        entry<CoinNavScreen> {
                            CoinScreen()
                        }
                    }
                }

            val swipeDismissableSceneStrategy = rememberSwipeDismissableSceneStrategy<NavKey>()

            NavDisplay(
                backStack = backStack,
                entryProvider = entryProvider,
                sceneStrategies = listOf(swipeDismissableSceneStrategy)
            )
        }
    }
}

@Composable
fun GreetingScreen(
    onChangePage: (AppKey) -> Unit,
    modifier: Modifier = Modifier
) {    
    val pagerState = rememberPagerState(pageCount = { 3 })

    HorizontalPagerScaffold(pagerState = pagerState) {
        HorizontalPager(
            state = pagerState,
            flingBehavior =
                PagerDefaults.snapFlingBehavior(
                    state = pagerState,
                    maxFlingPages = 1,
                    snapPositionalThreshold = PagerScaffoldDefaults.HighSnapPositionalThreshold,
                    snapAnimationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                ),
            rotaryScrollableBehavior = null,
        ) { page ->
            AnimatedPage(pageIndex = page, pagerState = pagerState) {
                if (page == 0) {
                    MainScreen(onChangePage)
                } else if (page == 1) {
                    SettingsScreen()
                } else {
                    AboutScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    onChangePage: (AppKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    val settingsStore = remember { SettingsDataStore(context) }
    val scheduleStore = remember { ScheduleDataStore(context) }

    val showEventOnHome by settingsStore.showEventOnHomeFlow.collectAsState(initial = true)
    val schedule by produceState(initialValue = emptyList<ScheduleEvent>()) {
        value = scheduleStore.getSchedule().events
    }

    var nowMinutes by remember { mutableStateOf(currentMinutes()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMinutes = currentMinutes()
            delay(10_000L)
        }
    }

    val today = todayWeekday()
    val currentEvent = remember(schedule, showEventOnHome, nowMinutes, today) {
        if (!showEventOnHome) null
        else schedule.firstOrNull { event ->
            event.weekday == today &&
                toMinutes(event.startTime)?.let { nowMinutes >= it } == true &&
                toMinutes(event.endTime)?.let { nowMinutes < it } == true
        }
    }

    var showChooseEditModeDialog by remember { mutableStateOf(false) }

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

            if (currentEvent != null) {
                item {
                    val start = toMinutes(currentEvent.startTime)
                    val end = toMinutes(currentEvent.endTime)
                    val progress = if (start != null && end != null && end > start) {
                        (nowMinutes - start).toFloat() / (end - start).toFloat()
                    } else {
                        0f
                    }
                    ScheduleEventCard(
                        transformation = SurfaceTransformation(transformationSpec),
                        event = currentEvent,
                        highlighted = true,
                        progress = progress
                    )
                }
            }

            item {
                AlertDialog(
                    visible = showChooseEditModeDialog,
                    onDismissRequest = {
                        showChooseEditModeDialog = false
                    },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.info),
                            contentDescription = null
                        )
                    },
                    title = { Text(text = "选择操作") },
                    text = { Text(text = "想要做什么") },
                    edgeButton = {
                        AlertDialogDefaults.EdgeButton(
                            onClick = {
                                showChooseEditModeDialog = false
                            },
                            content = { Text("关闭") }
                        )
                    }
                ) {
                    item {
                        FilledTonalButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                showChooseEditModeDialog = false
                                onChangePage(ViewScheduleNavScreen)
                            },
                            label = { Text(modifier = Modifier.fillMaxWidth(), text = "查看课程表") },
                        )
                    }
                    item {
                        FilledTonalButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                showChooseEditModeDialog = false
                                onChangePage(EditScheduleNavScreen)
                            },
                            label = { Text(modifier = Modifier.fillMaxWidth(), text = "设置课程表") },
                        )
                    }
                }
                FilledTonalButton(
                    onClick = { showChooseEditModeDialog = true },
                    label = { Text("课程表") },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.date_range),
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    transformation = SurfaceTransformation(transformationSpec)
                )
            }
            
            item {
                FilledTonalButton(
                    onClick = { onChangePage(GameMenuNavScreen) },
                    label = { Text("工具") },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.build),
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
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
                    modifier = Modifier.fillMaxWidth(),
                    transformation = SurfaceTransformation(transformationSpec)
                )
            }
        }
    }
}