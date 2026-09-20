package com.juhao.classtool

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
import com.juhao.classtool.datastore.ScheduleDataStore
import com.juhao.classtool.ui.about.AboutScreen
import com.juhao.classtool.ui.game.coin.CoinScreen
import com.juhao.classtool.ui.game.gamemenu.GameMenu
import com.juhao.classtool.ui.schedule.*
import com.juhao.classtool.ui.settings.SettingsScreen
import com.juhao.classtool.theme.WearAppTheme
import com.juhao.classtool.ui.tool.timer.TimerScreen
import com.juhao.classtool.ui.tool.toolmenu.ToolMenu

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
    
    val testMode = TestModeState.enabled
    val scheduleStore = remember(testMode) { SettingsDataStore(context) }
    
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { 2 }
    )

    LaunchedEffect(Unit) {
        val events = scheduleStore.getSchedule().events
        val today = todayWeekday()
        val nowMinutes = currentMinutes()
        val hasCurrent = events.any { event ->
            event.weekday == today &&
                toMinutes(event.startTime)?.let { nowMinutes >= it } == true &&
                toMinutes(event.endTime)?.let { nowMinutes < it } == true
        }
        if (hasCurrent) {
            pagerState.animateScrollToPage(1)
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