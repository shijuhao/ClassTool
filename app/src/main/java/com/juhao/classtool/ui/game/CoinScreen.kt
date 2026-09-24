package com.juhao.classtool.ui.game

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.transformedHeight
import com.juhao.classtool.ui.schedule.*
import com.juhao.classtool.ui.components.RoundToast
import kotlin.random.Random

@Composable
fun CoinScreen(modifier: Modifier = Modifier) {
    val listState = rememberTransformingLazyColumnState()
    val square = LocalScreenShape.current == ScreenShape.SQUARE
    val transformationSpec = rememberAdaptiveTransformationSpec(square)

    val context = LocalContext.current

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
                ) { Text(text = "抛硬币") }
            }
            item {
                Button(
                    label = {
                        Text(
                            text = "开抛！",
                            modifier = modifier.fillMaxWidth()
                        )
                    },
                    onClick = {
                        val positive = Random.nextBoolean()
                        RoundToast.show(
                            context,
                            "硬币在 ${if (positive) "正面" else "背面"}",
                            RoundToast.LENGTH_SHORT
                        )
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                )
            }
        }
    }
}