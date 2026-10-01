package com.juhao.classtool.ui.game

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
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
fun CoinScreen(modifier: Modifier = Modifier) {
    val listState = rememberTransformingLazyColumnState()
    val square = LocalScreenShape.current == ScreenShape.SQUARE
    val transformationSpec = rememberAdaptiveTransformationSpec(square)

    var flipping by remember { mutableStateOf(false) }
    var displayFace by remember { mutableStateOf(0) }
    var resultFace by remember { mutableStateOf(-1) }
    var revealed by remember { mutableStateOf(false) }

    val rotationY by animateFloatAsState(
        targetValue = if (flipping) 720f else 0f,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "coinRotation"
    )

    val scale by animateFloatAsState(
        targetValue = if (flipping) 0.9f else if (revealed) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "coinScale"
    )

    val resultAlpha by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "resultAlpha"
    )

    LaunchedEffect(flipping) {
        if (flipping) {
            revealed = false
            val endTime = System.currentTimeMillis() + 1200L
            while (System.currentTimeMillis() < endTime) {
                displayFace = Random.nextInt(0, 2)
                delay(100L)
            }
            resultFace = Random.nextInt(0, 2)
            displayFace = resultFace
            flipping = false
            revealed = true
        }
    }

    val coinFaces = listOf("正", "反")

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
                TitleCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .transformedHeight(this, transformationSpec),
                    colors = AppCardDefaults.cardColors(),
                    transformation = SurfaceTransformation(transformationSpec),
                    title = { Text(if (revealed) "结果" else "硬币") }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = coinFaces[displayFace],
                            style = MaterialTheme.typography.displayLarge,
                            modifier = Modifier
                                .scale(scale)
                                .graphicsLayer { this.rotationY = rotationY },
                            textAlign = TextAlign.Center
                        )
                    }
                    if (revealed) {
                        Text(
                            text = if (resultFace == 0) "正面" else "背面",
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
                            text = if (flipping) "抛掷中…" else "开抛！",
                            modifier = modifier.fillMaxWidth()
                        )
                    },
                    onClick = {
                        if (!flipping) {
                            flipping = true
                        }
                    },
                    enabled = !flipping,
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