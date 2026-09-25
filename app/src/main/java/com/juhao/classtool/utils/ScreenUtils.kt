package com.juhao.classtool.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.RoundedCorner
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material3.lazy.ResponsiveTransformationSpec
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import com.juhao.classtool.datastore.ScreenShapeMode

enum class ScreenShape { ROUND, SQUARE }

val LocalScreenShape = staticCompositionLocalOf { ScreenShape.ROUND }

@Composable
fun rememberIsSquareScreen(mode: ScreenShapeMode): Boolean {
    if (mode == ScreenShapeMode.FORCE_SQUARE) return true
    if (mode == ScreenShapeMode.FORCE_ROUND) return false
    val context = LocalContext.current
    return remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching {
                val display = context.display
                val modeInfo = display.mode
                val shortSide = minOf(modeInfo.physicalWidth, modeInfo.physicalHeight).toFloat()
                if (shortSide <= 0f) return@runCatching false

                val corner = display.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)
                    ?: return@runCatching true

                val ratio = corner.radius / shortSide
                val isRoundWatch = ratio >= 0.35f
                !isRoundWatch
            }.getOrDefault(false)
        } else {
            false
        }
    }
}

@Composable
fun rememberAdaptiveTransformationSpec(square: Boolean): TransformationSpec {
    return if (square) {
        rememberTransformationSpec(
            ResponsiveTransformationSpec.smallScreen(
                minTransitionAreaHeightFraction = 0f,
                maxTransitionAreaHeightFraction = 0f,
            )
        )
    } else {
        rememberTransformationSpec()
    }
}

@Composable
fun KeepScreenOn(enabled: Boolean) {
    val context = LocalContext.current
    val activity = context.findActivity()

    DisposableEffect(enabled, activity) {
        if (activity == null) {
            onDispose { }
        } else {
            val window = activity.window
            if (enabled) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            onDispose {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}