package com.juhao.classtool.ui.game

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.transformedHeight
import com.juhao.classtool.utils.*
import com.juhao.classtool.ui.components.RoundToast
import kotlin.random.Random

@Composable
fun DiceScreen(modifier: Modifier = Modifier) {
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
                ) { Text(text = "掷骰子") }
            }
            item {
                Button(
                    label = {
                        Text(
                            text = "开掷！",
                            modifier = modifier.fillMaxWidth()
                        )
                    },
                    onClick = {
                        val diceValue = Random.nextInt(1, 7)
                        RoundToast.show(
                            context,
                            "骰子点数：$diceValue 点",
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