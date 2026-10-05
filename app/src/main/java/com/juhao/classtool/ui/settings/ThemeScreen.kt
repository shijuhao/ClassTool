package com.juhao.classtool.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.transformedHeight
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.*
import com.juhao.classtool.datastore.SettingsDataStore
import com.juhao.classtool.navigation.AppKey
import com.juhao.classtool.navigation.CustomThemeNavScreen
import com.juhao.classtool.theme.AppTheme
import com.juhao.classtool.utils.*
import kotlinx.coroutines.launch

@Composable
fun ThemeScreen(
    onNavigate: (AppKey) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { SettingsDataStore(context) }

    val listState = rememberTransformingLazyColumnState()
    val square = LocalScreenShape.current == ScreenShape.SQUARE
    val transformationSpec = rememberAdaptiveTransformationSpec(square)

    val useSystemColor by store.useSystemColorFlow.collectAsState(initial = true)
    val dayMode by store.dayModeFlow.collectAsState(initial = false)
    val appTheme by store.appThemeFlow.collectAsState(initial = null)
    val dynamicTheme by store.dynamicThemeFlow.collectAsState(initial = true)
    val pureBlackBackground by store.pureBlackBackgroundFlow.collectAsState(initial = false)

    val themes = remember { AppTheme.entries.filter { it != AppTheme.CUSTOM } }

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
                ) { Text(text = "主题") }
            }

            item {
                SwitchButton(
                    checked = dynamicTheme,
                    onCheckedChange = { checked ->
                        scope.launch { store.setDynamicTheme(checked) }
                    },
                    label = {
                        Text(
                            text = "跟随事件主题色",
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
                            imageVector = MaterialSymbols.Rounded.Auto_awesome,
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
                    checked = dayMode,
                    onCheckedChange = { checked ->
                        scope.launch {
                            store.setDayMode(checked)
                            if (checked) {
                                store.setPureBlackBackground(false)
                            }
                        }
                    },
                    label = {
                        Text(
                            text = "日间模式",
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = if (dayMode) "使用浅色背景" else "关闭",
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Light_mode,
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
                    checked = pureBlackBackground,
                    onCheckedChange = { checked ->
                        scope.launch { store.setPureBlackBackground(checked) }
                    },
                    enabled = !dayMode,
                    label = {
                        Text(
                            text = "纯黑背景",
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = if (pureBlackBackground) "使用纯黑色背景" else "关闭",
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Dark_mode,
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
                    checked = useSystemColor,
                    onCheckedChange = { checked ->
                        scope.launch {
                            store.setUseSystemColor(checked)
                            if (checked) {
                                store.setAppTheme(null)
                            } else {
                                store.setAppTheme(appTheme ?: AppTheme.MORNING)
                            }
                        }
                    },
                    label = {
                        Text(
                            text = "使用系统动态取色",
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = if (useSystemColor) "跟随系统壁纸颜色" else "关闭",
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Colorize,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                )
            }

            items(themes.size) { index ->
                val theme = themes[index]
                val selected = !useSystemColor && appTheme == theme
                val buttonColors = if (selected) {
                    ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    ButtonDefaults.filledTonalButtonColors()
                }

                FilledTonalButton(
                    onClick = {
                        scope.launch { store.setAppTheme(theme) }
                    },
                    enabled = !useSystemColor,
                    label = {
                        Text(text = theme.displayName)
                    },
                    secondaryLabel = {
                        Text(text = themeSubtitle(theme))
                    },
                    icon = if (selected) {
                        {
                            Icon(
                                imageVector = MaterialSymbols.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize)
                            )
                        }
                    } else {
                        null
                    },
                    colors = buttonColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec),
                    transformation = SurfaceTransformation(transformationSpec)
                )
            }

            item {
                val customSelected = !useSystemColor && appTheme == AppTheme.CUSTOM
                val buttonColors = if (customSelected) {
                    ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    ButtonDefaults.filledTonalButtonColors()
                }

                FilledTonalButton(
                    onClick = {
                        onNavigate(CustomThemeNavScreen)
                    },
                    enabled = !useSystemColor,
                    label = {
                        Text(text = "自定义")
                    },
                    secondaryLabel = {
                        Text(text = "调整 RGB 打造专属主题")
                    },
                    icon = if (customSelected) {
                        {
                            Icon(
                                imageVector = MaterialSymbols.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize)
                            )
                        }
                    } else {
                        {
                            Icon(
                                imageVector = MaterialSymbols.Rounded.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize)
                            )
                        }
                    },
                    colors = buttonColors,
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

private fun themeSubtitle(theme: AppTheme): String = when (theme) {
    AppTheme.MORNING -> "温暖明亮的橙黄色调"
    AppTheme.DUSK -> "柔和静谧的紫罗兰色调"
    AppTheme.BAMBOO -> "清新自然的青绿色调"
    AppTheme.SAKURA -> "柔美浪漫的粉红色调"
    AppTheme.INK -> "深邃冷静的蓝色调"
    AppTheme.CUSTOM -> "自定义 RGB"
}