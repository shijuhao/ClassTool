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

private const val ROUND_RATIO_THRESHOLD = 0.45f

@Composable
fun rememberIsSquareScreen(mode: ScreenShapeMode): Boolean {
    if (mode == ScreenShapeMode.FORCE_SQUARE) return true
    if (mode == ScreenShapeMode.FORCE_ROUND) return false
    val context = LocalContext.current
    return remember { context.isSquareScreen() }
}

private fun Context.isSquareScreen(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    return runCatching {
        val display = display ?: return true
        val modeInfo = display.mode ?: return true
        val shortSide = minOf(modeInfo.physicalWidth, modeInfo.physicalHeight).toFloat()
        if (shortSide <= 0f) return true

        val radii = listOf(
            RoundedCorner.POSITION_TOP_LEFT,
            RoundedCorner.POSITION_TOP_RIGHT,
            RoundedCorner.POSITION_BOTTOM_LEFT,
            RoundedCorner.POSITION_BOTTOM_RIGHT,
        ).mapNotNull { display.getRoundedCorner(it)?.radius }

        if (radii.isEmpty()) return true

        val maxRatio = radii.max() / shortSide
        maxRatio < ROUND_RATIO_THRESHOLD
    }.getOrDefault(true)
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