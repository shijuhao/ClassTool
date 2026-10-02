package com.juhao.classtool.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.transformedHeight
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Check
import com.juhao.classtool.datastore.SettingsDataStore
import com.juhao.classtool.theme.AppTheme
import com.juhao.classtool.utils.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun CustomThemeScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { SettingsDataStore(context) }

    val listState = rememberTransformingLazyColumnState()
    val square = LocalScreenShape.current == ScreenShape.SQUARE
    val transformationSpec = rememberAdaptiveTransformationSpec(square)

    val savedColor by store.customColorFlow.collectAsState(initial = Color(0xFF9BD7FF))

    var red by remember(savedColor) { mutableIntStateOf((savedColor.red * 255f).roundToInt()) }
    var green by remember(savedColor) { mutableIntStateOf((savedColor.green * 255f).roundToInt()) }
    var blue by remember(savedColor) { mutableIntStateOf((savedColor.blue * 255f).roundToInt()) }

    val color = Color(red, green, blue)
    val hexText = "#%02X%02X%02X".format(red, green, blue)

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding
        ) {
            item {
                ListHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(
                            ListHeaderDefaults.minimumTopListContentPadding
                        ),
                    transformation = SurfaceTransformation(transformationSpec)
                ) { Text(text = "自定义颜色") }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .graphicsLayer {
                            with(transformationSpec) {
                                applyContainerTransformation(scrollProgress)
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "颜色预览",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .background(color, shape = RoundedCornerShape(16.dp))
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = hexText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            item {
                ChannelRow(
                    modifier = Modifier
                        .transformedHeight(this, transformationSpec)
                        .graphicsLayer {
                            with(transformationSpec) {
                                applyContainerTransformation(scrollProgress)
                            }
                        },
                    label = "红色（R）",
                    value = red,
                    onChange = { red = it }
                )
            }

            item {
                ChannelRow(
                    modifier = Modifier
                        .transformedHeight(this, transformationSpec)
                        .graphicsLayer {
                            with(transformationSpec) {
                                applyContainerTransformation(scrollProgress)
                            }
                        },
                    label = "绿色（G）",
                    value = green,
                    onChange = { green = it }
                )
            }

            item {
                ChannelRow(
                    modifier = Modifier
                        .transformedHeight(this, transformationSpec)
                        .graphicsLayer {
                            with(transformationSpec) {
                                applyContainerTransformation(scrollProgress)
                            }
                        },
                    label = "蓝色（B）",
                    value = blue,
                    onChange = { blue = it }
                )
            }

            item {
                Button(
                    onClick = {
                        scope.launch {
                            store.setCustomColor(color)
                            store.setUseSystemColor(false)
                            store.setAppTheme(AppTheme.CUSTOM)
                        }
                        onBack()
                    },
                    label = { Text(text = "应用") },
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(
                            ButtonDefaults.minimumVerticalListContentPadding
                        ),
                    transformation = SurfaceTransformation(transformationSpec)
                )
            }
        }
    }
}

@Composable
private fun ChannelRow(
    modifier: Modifier = Modifier,
    label: String,
    value: Int,
    onChange: (Int) -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$label · $value",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(6.dp))
        Slider(
            value = value,
            onValueChange = onChange,
            valueProgression = 0..255,
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                            onChange((ratio * 255f).roundToInt())
                        },
                        onHorizontalDrag = { change, _ ->
                            val ratio = (change.position.x / size.width).coerceIn(0f, 1f)
                            onChange((ratio * 255f).roundToInt())
                            change.consume()
                        }
                    )
                },
            segmented = false
        )
    }
}