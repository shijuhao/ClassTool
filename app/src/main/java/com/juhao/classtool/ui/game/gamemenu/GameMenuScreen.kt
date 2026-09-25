package com.juhao.classtool.ui.game.gamemenu

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.transformedHeight
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.*
import com.juhao.classtool.ui.schedule.*
import com.juhao.classtool.key.*

@Composable
fun GameMenu(
    onChangePage: (AppKey) -> Unit
) {
    val listState = rememberTransformingLazyColumnState()
    val square = LocalScreenShape.current == ScreenShape.SQUARE
    val transformationSpec = rememberAdaptiveTransformationSpec(square)
    
    ScreenScaffold(
        scrollState = listState
    ) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
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
                ) { Text(text = "小游戏") }
            }
            item {
                FilledTonalButton(
                    onClick = { onChangePage(ReactionNavScreen) },
                    label = { Text("反应力测试") },
                    secondaryLabel = { Text("变绿后尽快点击") },
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Hourglass,
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
                    onClick = { onChangePage(DiceNavScreen) },
                    label = { Text("掷骰子") },
                    secondaryLabel = { Text("1-6 点随机") },
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Casino,
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
                    onClick = { onChangePage(CoinNavScreen) },
                    label = { Text("抛硬币") },
                    secondaryLabel = { Text("看看是在正面还是背面？") },
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Casino,
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