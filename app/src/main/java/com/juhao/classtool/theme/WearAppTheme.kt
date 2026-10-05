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
    dayMode: Boolean = false,
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
            base = if (dayMode) wearDayColorScheme else wearColorScheme,
            customColor = customColor,
            isDay = dayMode
        )
        theme != null -> buildThemeColorScheme(
            base = if (dayMode) wearDayColorScheme else wearColorScheme,
            theme = theme,
            isDay = dayMode
        )
        else -> if (dayMode) wearDayColorScheme else wearColorScheme
    }

    val finalScheme = when {
        eventUrgent -> buildEventColorScheme(
            base = baseScheme,
            eventColor = eventColor ?: baseScheme.error,
            eventUrgent = true,
            isDay = dayMode
        )
        eventColor != null -> buildEventColorScheme(
            base = baseScheme,
            eventColor = eventColor,
            eventUrgent = false,
            isDay = dayMode
        )
        else -> baseScheme
    }

    MaterialTheme(
        colorScheme = finalScheme,
        typography = Typography(),
        content = content
    )
}