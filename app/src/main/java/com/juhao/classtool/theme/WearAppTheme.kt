package com.juhao.classtool.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Typography
import androidx.wear.compose.material3.dynamicColorScheme

@Composable
fun WearAppTheme(
    eventColor: Color? = null,
    eventUrgent: Boolean = false,
    content: @Composable () -> Unit
) {
    val baseScheme = dynamicColorScheme(LocalContext.current) ?: wearColorScheme

    val finalScheme = if (eventColor == null) {
        baseScheme
    } else {
        buildEventColorScheme(baseScheme, eventColor, eventUrgent)
    }

    MaterialTheme(
        colorScheme = finalScheme,
        typography = Typography(),
        content = content
    )
}