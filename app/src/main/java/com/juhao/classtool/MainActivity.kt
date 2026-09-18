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
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.navigation3.rememberSwipeDismissableSceneStrategy
import com.juhao.classtool.R

import com.juhao.classtool.ui.coin.AboutScreen
import com.juhao.classtool.ui.coin.CoinScreen

import com.juhao.classtool.theme.AppCardDefaults
import com.juhao.classtool.theme.WearAppTheme
import kotlinx.serialization.Serializable

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WearApp()
        }
    }
}

@Serializable
sealed interface AppKey : NavKey

@Serializable
data object MenuScreen : AppKey

@Serializable
data object AboutNavScreen : AppKey

@Serializable
data object CoinNavScreen : AppKey

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
                        entry<AboutNavScreen> {
                            AboutScreen()
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
    val scrollState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(
        scrollState = scrollState,
        edgeButton = {
            EdgeButton(
                onClick = { onChangePage(AboutNavScreen) },
                buttonSize = EdgeButtonSize.ExtraSmall
            ) {
                Text("关于")
            }
        }
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
                TitleCard(
                    title = {
                        Text("抛硬币")
                    },
                    onClick = { onChangePage(CoinNavScreen) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec),
                    colors = AppCardDefaults.cardColors()
                ) {
                    Text("看看是正面还是背面？")
                }
            }
        }
    }
}