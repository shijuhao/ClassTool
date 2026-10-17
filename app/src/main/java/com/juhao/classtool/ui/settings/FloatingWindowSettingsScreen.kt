package com.juhao.classtool.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import androidx.wear.compose.material3.lazy.transformedHeight
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.*
import com.juhao.classtool.datastore.SettingsDataStore
import com.juhao.classtool.ui.components.RoundToast
import com.juhao.classtool.utils.*
import kotlinx.coroutines.launch

private fun hasOverlayPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else {
        true
    }
}

private fun requestOverlayPermission(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

private fun isBatteryOptimizationIgnored(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }
    return true
}

private fun requestIgnoreBatteryOptimization(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isBatteryOptimizationIgnored(context)) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

@Composable
fun FloatingWindowSettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { SettingsDataStore(context) }

    val listState = rememberTransformingLazyColumnState()
    val square = LocalScreenShape.current == ScreenShape.SQUARE
    val transformationSpec = rememberAdaptiveTransformationSpec(square)

    val floatingEnabled by store.floatingWindowEnabledFlow.collectAsState(initial = false)

    var batteryOptimizationIgnored by remember { mutableStateOf(isBatteryOptimizationIgnored(context)) }
    var hasOverlayPermission by remember { mutableStateOf(hasOverlayPermission(context)) }

    LaunchedEffect(Unit) {
        batteryOptimizationIgnored = isBatteryOptimizationIgnored(context)
        hasOverlayPermission = hasOverlayPermission(context)
    }

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
                ) { Text(text = "悬浮窗设置") }
            }

            item {
                SwitchButton(
                    checked = floatingEnabled && hasOverlayPermission && square,
                    enabled = hasOverlayPermission && square,
                    onCheckedChange = { checked ->
                        scope.launch {
                            store.setFloatingWindowEnabled(checked)
                            RoundToast.show(
                                context,
                                "重启应用后生效",
                                RoundToast.LENGTH_SHORT
                            )
                        }
                    },
                    label = {
                        Text(
                            text = "悬浮窗",
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    secondaryLabel = {
                        Text(
                            text = if (square) if (hasOverlayPermission) "显示事件状态悬浮窗" else "请授予悬浮窗权限" else "仅支持方屏",
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Picture_in_picture,
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
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .graphicsLayer {
                            with(transformationSpec) {
                                applyContainerTransformation(scrollProgress)
                            }
                        },
                    text = "适配原因，目前仅支持方屏且不确定在 WearOS 上的可用性"
                )
            }

            item {
                FilledTonalButton(
                    onClick = {
                        requestOverlayPermission(context)
                    },
                    label = { Text("悬浮窗权限") },
                    secondaryLabel = {
                        Text(
                            text = if (hasOverlayPermission) "已授权" else "未授权，点击授权"
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Layers,
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

            item {
                FilledTonalButton(
                    onClick = {
                        requestIgnoreBatteryOptimization(context)
                    },
                    label = { Text("电池优化") },
                    secondaryLabel = {
                        Text(
                            text = if (batteryOptimizationIgnored) "已禁用电池优化" else "未禁用，点击设置"
                        )
                    },
                    icon = {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Battery_charging_full,
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
            
            item {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .graphicsLayer {
                            with(transformationSpec) {
                                applyContainerTransformation(scrollProgress)
                            }
                        },
                    text = "关闭电池优化可以更好的防止应用在后台时被杀"
                )
            }
        }
    }
}