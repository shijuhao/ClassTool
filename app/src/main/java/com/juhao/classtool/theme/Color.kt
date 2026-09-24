package com.juhao.classtool.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.wear.compose.material3.ColorScheme

val primaryDark = Color(0xFFAAC7FF)
val onPrimaryDark = Color(0xFF0A305F)
val primaryContainerDark = Color(0xFF284777)
val onPrimaryContainerDark = Color(0xFFD6E3FF)
val secondaryDark = Color(0xFFBEC6DC)
val onSecondaryDark = Color(0xFF283141)
val secondaryContainerDark = Color(0xFF3E4759)
val onSecondaryContainerDark = Color(0xFFDAE2F9)
val tertiaryDark = Color(0xFFDDBCE0)
val onTertiaryDark = Color(0xFF3F2844)
val tertiaryContainerDark = Color(0xFF573E5C)
val onTertiaryContainerDark = Color(0xFFFAD8FD)
val errorDark = Color(0xFFFFB4AB)
val onErrorDark = Color(0xFF690005)
val errorContainerDark = Color(0xFF93000A)
val onErrorContainerDark = Color(0xFFFFDAD6)
val backgroundDark = Color(0xFF111318)
val onBackgroundDark = Color(0xFFE2E2E9)
val onSurfaceDark = Color(0xFFE2E2E9)
val onSurfaceVariantDark = Color(0xFFC4C6D0)
val outlineDark = Color(0xFF8E9099)
val outlineVariantDark = Color(0xFF44474E)
val surfaceContainerLowDark = Color(0xFF191C20)
val surfaceContainerDark = Color(0xFF1D2024)
val surfaceContainerHighDark = Color(0xFF282A2F)
internal val wearColorScheme: ColorScheme =
    ColorScheme(
        primary = primaryDark,
        onPrimary = onPrimaryDark,
        primaryContainer = primaryContainerDark,
        onPrimaryContainer = onPrimaryContainerDark,
        secondary = secondaryDark,
        onSecondary = onSecondaryDark,
        secondaryContainer = secondaryContainerDark,
        onSecondaryContainer = onSecondaryContainerDark,
        tertiary = tertiaryDark,
        onTertiary = onTertiaryDark,
        tertiaryContainer = tertiaryContainerDark,
        onTertiaryContainer = onTertiaryContainerDark,
        error = errorDark,
        onError = onErrorDark,
        errorContainer = errorContainerDark,
        onErrorContainer = onErrorContainerDark,
        background = backgroundDark,
        onBackground = onBackgroundDark,
        onSurface = onSurfaceDark,
        onSurfaceVariant = onSurfaceVariantDark,
        outline = outlineDark,
        outlineVariant = outlineVariantDark,
        surfaceContainerLow = surfaceContainerLowDark,
        surfaceContainer = surfaceContainerDark,
        surfaceContainerHigh = surfaceContainerHighDark,
        primaryDim = primaryContainerDark,
        secondaryDim = secondaryContainerDark,
        tertiaryDim = tertiaryContainerDark
    )

private fun blend(base: Color, tint: Color, amount: Float): Color = Color(
    red = base.red * (1f - amount) + tint.red * amount,
    green = base.green * (1f - amount) + tint.green * amount,
    blue = base.blue * (1f - amount) + tint.blue * amount,
    alpha = 1f
)

private fun contentOn(color: Color): Color =
    if (color.luminance() > 0.45f) Color(0xFF1A1A1A) else Color(0xFFF5F5F5)

private fun toHsl(color: Color): Triple<Float, Float, Float> {
    val r = color.red
    val g = color.green
    val b = color.blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val l = (max + min) / 2f
    if (max == min) return Triple(0f, 0f, l)
    val d = max - min
    val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
    val h = when (max) {
        r -> ((g - b) / d + if (g < b) 6f else 0f) / 6f
        g -> ((b - r) / d + 2f) / 6f
        else -> ((r - g) / d + 4f) / 6f
    }
    return Triple(h, s, l)
}

private fun hueToRgb(p: Float, q: Float, t0: Float): Float {
    var t = t0
    if (t < 0f) t += 1f
    if (t > 1f) t -= 1f
    return when {
        t < 1f / 6f -> p + (q - p) * 6f * t
        t < 1f / 2f -> q
        t < 2f / 3f -> p + (q - p) * (2f / 3f - t) * 6f
        else -> p
    }
}

private fun fromHsl(h: Float, s: Float, l: Float): Color {
    if (s == 0f) return Color(l, l, l, 1f)
    val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
    val p = 2f * l - q
    return Color(
        red = hueToRgb(p, q, h + 1f / 3f),
        green = hueToRgb(p, q, h),
        blue = hueToRgb(p, q, h - 1f / 3f),
        alpha = 1f
    )
}

private fun tuneLightness(color: Color, targetL: Float): Color {
    val (h, s, _) = toHsl(color)
    return fromHsl(h, s, targetL.coerceIn(0f, 1f))
}

fun buildEventColorScheme(base: ColorScheme, eventColor: Color): ColorScheme {
    val (eventH, eventS, eventL) = toHsl(eventColor)

    val primary = if (eventL < 0.35f) {
        fromHsl(eventH, eventS, 0.65f)
    } else {
        eventColor
    }
    val onPrimary = contentOn(primary)

    val primaryContainer = fromHsl(eventH, (eventS * 0.85f).coerceAtLeast(0.35f), 0.28f)
    val onPrimaryContainer = contentOn(primaryContainer)

    val secondary = fromHsl(eventH, (eventS * 0.55f).coerceAtLeast(0.25f), 0.72f)
    val onSecondary = contentOn(secondary)

    val secondaryContainer = fromHsl(eventH, (eventS * 0.50f).coerceAtLeast(0.22f), 0.32f)
    val onSecondaryContainer = contentOn(secondaryContainer)

    val tertiary = fromHsl(eventH, (eventS * 0.45f).coerceAtLeast(0.20f), 0.75f)
    val onTertiary = contentOn(tertiary)

    val tertiaryContainer = fromHsl(eventH, (eventS * 0.40f).coerceAtLeast(0.18f), 0.36f)
    val onTertiaryContainer = contentOn(tertiaryContainer)

    val surfaceContainerHigh = blend(base.surfaceContainerHigh, eventColor, 0.14f)
    val surfaceContainer = blend(base.surfaceContainer, eventColor, 0.10f)
    val surfaceContainerLow = blend(base.surfaceContainerLow, eventColor, 0.06f)

    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainer = surfaceContainer,
        surfaceContainerLow = surfaceContainerLow,
        primaryDim = primaryContainer,
        secondaryDim = secondaryContainer,
        tertiaryDim = tertiaryContainer
    )
}