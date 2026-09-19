package com.juhao.classtool.ui.game.gamemenu

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.juhao.classtool.key.*
import com.juhao.classtool.R

@Composable
fun GameMenu(
    onChangePage: (AppKey) -> Unit
) {
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    
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
                    onClick = { onChangePage(CoinNavScreen) },
                    label = { Text("抛硬币") },
                    secondaryLabel = { Text("看看是在正面还是背面？") },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.casino),
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