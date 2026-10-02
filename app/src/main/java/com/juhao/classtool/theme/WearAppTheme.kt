package com.juhao.classtool.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Typography
import androidx.wear.compose.material3.dynamicColorScheme

@Composable
fun WearAppTheme(
    useSystemColor: Boolean = true,
    theme: AppTheme? = null,
    customColor: Color = Color(0xFF9BD7FF),
    eventColor: Color? = null,
    eventUrgent: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemScheme = dynamicColorScheme(context)

    val baseScheme = when {
        useSystemColor && systemScheme != null -> systemScheme
        theme == AppTheme.CUSTOM -> buildCustomColorScheme(
            base = wearColorScheme,
            customColor = customColor
        )
        theme != null -> buildThemeColorScheme(wearColorScheme, theme)
        else -> wearColorScheme
    }

    val finalScheme = when {
        eventUrgent -> buildEventColorScheme(
            base = baseScheme,
            eventColor = eventColor ?: baseScheme.error,
            eventUrgent = true
        )
        eventColor != null -> buildEventColorScheme(
            base = baseScheme,
            eventColor = eventColor,
            eventUrgent = false
        )
        else -> baseScheme
    }

    MaterialTheme(
        colorScheme = finalScheme,
        typography = Typography(),
        content = content
    )
}