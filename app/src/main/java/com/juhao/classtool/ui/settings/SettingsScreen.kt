package com.juhao.classtool.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.transformedHeight
import com.juhao.classtool.R
import com.juhao.classtool.datastore.ScreenShapeMode
import com.juhao.classtool.datastore.SettingsDataStore
import com.juhao.classtool.datastore.TestModeState
import com.juhao.classtool.ui.schedule.LocalScreenShape
import com.juhao.classtool.ui.schedule.ScreenShape
import com.juhao.classtool.ui.schedule.rememberAdaptiveTransformationSpec
import kotlinx.coroutines.launch

private val UI_SCALE_STEPS = (5..15 step 1).map { it / 10f }

@Composable
fun SettingsScreen(
    onNavigateToBackup: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { SettingsDataStore(context) }

    val listState = rememberTransformingLazyColumnState()
    val square = LocalScreenShape.current == ScreenShape.SQUARE
    val transformationSpec = rememberAdaptiveTransformationSpec(square)

    val testMode by store.testModeFlow.collectAsState(initial = false)
    val prepBell by store.prepBellFlow.collectAsState(initial = true)
    val globalEventReminder by store.globalEventReminderFlow.collectAsState(initial = true)
    val screenShapeMode by store.screenShapeModeFlow.collectAsState(initial = ScreenShapeMode.AUTO)
    val keepScreenOn by store.keepScreenOnFlow.collectAsState(initial = false)
    val classDuration by store.classDurationFlow.collectAsState(initial = 40)
    val breakDuration by store.breakDurationFlow.collectAsState(initial = 10)
    val uiScale by store.uiScaleFlow.collectAsState(initial = 1.0f)
    val dynamicTheme by store.dynamicThemeFlow.collectAsState(initial = false)

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
                ) { Text(text = "设置") }
            }

            item {
                SwitchButton(
                    checked = prepBell,
                    onCheckedChange = { checked ->
                        scope.launch { store.setPrepBell(checked) }
                    },
                    label = {
                        Text(
                            text = "3 分钟预备铃",
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = if (prepBell) "上课前 3 分钟显示预备" else "关闭",
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.notifications),
                            contentDescription = null
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                )
            }

            item {
                SwitchButton(
                    checked = globalEventReminder,
                    onCheckedChange = { checked ->
                        scope.launch { store.setGlobalEventReminder(checked) }
                    },
                    label = {
                        Text(
                            text = "全局事件提醒",
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = if (globalEventReminder) "事件开始时弹出提示" else "关闭",
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.info),
                            contentDescription = null
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                )
            }

            item {
                SwitchButton(
                    checked = keepScreenOn,
                    onCheckedChange = { checked ->
                        scope.launch { store.setKeepScreenOn(checked) }
                    },
                    label = {
                        Text(
                            text = "课程表常亮",
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = if (keepScreenOn) "停留在课程表页时保持屏幕常亮" else "关闭",
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.lightbulb),
                            contentDescription = null
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                )
            }

            item {
                SwitchButton(
                    checked = dynamicTheme,
                    onCheckedChange = { checked ->
                        scope.launch { store.setDynamicTheme(checked) }
                    },
                    label = {
                        Text(
                            text = "事件动态主题",
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = if (dynamicTheme) "根据当前事件色调整主题" else "关闭",
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.palette),
                            contentDescription = null
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                )
            }

            item {
                FilledTonalButton(
                    onClick = {
                        scope.launch {
                            store.setScreenShapeMode(
                                when (screenShapeMode) {
                                    ScreenShapeMode.AUTO -> ScreenShapeMode.FORCE_SQUARE
                                    ScreenShapeMode.FORCE_SQUARE -> ScreenShapeMode.FORCE_ROUND
                                    ScreenShapeMode.FORCE_ROUND -> ScreenShapeMode.AUTO
                                }
                            )
                        }
                    },
                    label = {
                        Text(text = "屏幕形状")
                    },
                    secondaryLabel = {
                        Text(
                            text = when (screenShapeMode) {
                                ScreenShapeMode.FORCE_SQUARE -> "强制方屏"
                                ScreenShapeMode.FORCE_ROUND -> "强制圆屏"
                                ScreenShapeMode.AUTO -> if (square) "自动（方屏）" else "自动（圆屏）"
                            }
                        )
                    },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.watch),
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                )
            }

            item {
                val steps = ((20..60).step(5)).toList()
                val currentIndex = steps.indexOfFirst { it >= classDuration }
                    .coerceAtLeast(0)

                SliderSettingCard(
                    modifier = Modifier.transformedHeight(this, transformationSpec),
                    title = "上课默认时长（分钟）",
                    transformation = SurfaceTransformation(transformationSpec),
                    valueLabel = "$classDuration min",
                    currentIndex = currentIndex,
                    stepCount = steps.size,
                    onChange = { index ->
                        val safeIndex = index.coerceIn(0, steps.lastIndex)
                        scope.launch { store.setClassDuration(steps[safeIndex]) }
                    }
                )
            }

            item {
                val steps = ((5..60).step(5)).toList()
                val currentIndex = steps.indexOfFirst { it >= breakDuration }
                    .coerceAtLeast(0)

                SliderSettingCard(
                    modifier = Modifier.transformedHeight(this, transformationSpec),
                    title = "课间默认时长（分钟）",
                    transformation = SurfaceTransformation(transformationSpec),
                    valueLabel = "$breakDuration min",
                    currentIndex = currentIndex,
                    stepCount = steps.size,
                    onChange = { index ->
                        val safeIndex = index.coerceIn(0, steps.lastIndex)
                        scope.launch { store.setBreakDuration(steps[safeIndex]) }
                    }
                )
            }

            item {
                val currentIndex = UI_SCALE_STEPS
                    .indexOfFirst { it >= uiScale - 0.001f }
                    .coerceAtLeast(0)
            
                SliderSettingCard(
                    modifier = Modifier.transformedHeight(this, transformationSpec),
                    title = "UI 缩放",
                    transformation = SurfaceTransformation(transformationSpec),
                    valueLabel = "${"%.1f".format(UI_SCALE_STEPS[currentIndex])}x",
                    currentIndex = currentIndex,
                    stepCount = UI_SCALE_STEPS.size,
                    onChange = { index ->
                        val safeIndex = index.coerceIn(0, UI_SCALE_STEPS.lastIndex)
                        scope.launch {
                            store.setUiScale(UI_SCALE_STEPS[safeIndex])
                            (context as? android.app.Activity)?.recreate()
                        }
                    }
                )
            }

            item {
                SwitchButton(
                    checked = testMode,
                    onCheckedChange = { checked ->
                        TestModeState.enabled = checked
                        scope.launch {
                            store.setTestMode(checked)
                        }
                    },
                    label = {
                        Text(
                            text = "测试模式",
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = "使用测试日程数据",
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.settings),
                            contentDescription = null                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                )
            }

            item {
                FilledTonalButton(
                    onClick = onNavigateToBackup,
                    label = { Text("备份与还原") },
                    secondaryLabel = { Text("复制 / 解析 JSON") },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.backup),
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                )
            }
        }
    }
}

@Composable
private fun SliderSettingCard(
    modifier: Modifier = Modifier,
    title: String,
    valueLabel: String,
    currentIndex: Int,
    stepCount: Int,
    onChange: (Int) -> Unit,
    transformation: SurfaceTransformation? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        transformation = transformation,
        colors = CardDefaults.cardColors()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title)
            Spacer(Modifier.height(8.dp))
            Slider(
                value = currentIndex,
                onValueChange = { index: Int -> onChange(index) },
                valueProgression = 0..(stepCount - 1),
                modifier = Modifier.fillMaxWidth(),
                segmented = true
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}