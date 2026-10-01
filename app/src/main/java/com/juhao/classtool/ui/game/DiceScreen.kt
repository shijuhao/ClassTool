package com.juhao.classtool.ui.game

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.transformedHeight
import com.juhao.classtool.theme.AppCardDefaults
import com.juhao.classtool.utils.*
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun DiceScreen(modifier: Modifier = Modifier) {
    val listState = rememberTransformingLazyColumnState()
    val square = LocalScreenShape.current == ScreenShape.SQUARE
    val transformationSpec = rememberAdaptiveTransformationSpec(square)

    var rolling by remember { mutableStateOf(false) }
    var displayValue by remember { mutableIntStateOf(1) }
    var resultValue by remember { mutableIntStateOf(0) }
    var revealed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (rolling) 0.85f else if (revealed) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "diceScale"
    )

    val rotation by animateFloatAsState(
        targetValue = if (rolling) 360f else 0f,
        animationSpec = tween(durationMillis = 600, easing = LinearEasing),
        label = "diceRotation"
    )

    val resultAlpha by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "resultAlpha"
    )

    LaunchedEffect(rolling) {
        if (rolling) {
            revealed = false
            val endTime = System.currentTimeMillis() + 1000L
            while (System.currentTimeMillis() < endTime) {
                displayValue = Random.nextInt(1, 7)
                delay(80L)
            }
            resultValue = Random.nextInt(1, 7)
            displayValue = resultValue
            rolling = false
            revealed = true
        }
    }

    val diceFaces = listOf("⚀", "⚁", "⚂", "⚃", "⚄", "⚅")

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
                TitleCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .transformedHeight(this, transformationSpec),
                    colors = AppCardDefaults.cardColors(),
                    transformation = SurfaceTransformation(transformationSpec),
                    title = { Text(if (revealed) "结果" else "骰子") }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = diceFaces[displayValue - 1],
                            style = MaterialTheme.typography.displayLarge,
                            modifier = Modifier
                                .scale(scale)
                                .graphicsLayer { rotationZ = rotation },
                            textAlign = TextAlign.Center
                        )
                    }
                    if (revealed) {
                        Text(
                            text = "$resultValue 点",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(resultAlpha),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            item {
                Button(
                    label = {
                        Text(
                            text = if (rolling) "掷骰中…" else "开掷！",
                            modifier = modifier.fillMaxWidth()
                        )
                    },
                    onClick = {
                        if (!rolling) {
                            rolling = true
                        }
                    },
                    enabled = !rolling,
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