package com.juhao.classtool.theme

import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme

val primaryDark = Color(0xFFC2D6FF)
val onPrimaryDark = Color(0xFF12345F)
val primaryContainerDark = Color(0xFF2E4A73)
val onPrimaryContainerDark = Color(0xFFE2ECFF)
val secondaryDark = Color(0xFFD2DAEE)
val onSecondaryDark = Color(0xFF354052)
val secondaryContainerDark = Color(0xFF424B5C)
val onSecondaryContainerDark = Color(0xFFE8EEFF)
val tertiaryDark = Color(0xFFECCDEF)
val onTertiaryDark = Color(0xFF4C3552)
val tertiaryContainerDark = Color(0xFF5A4260)
val onTertiaryContainerDark = Color(0xFFFFE4FF)
val errorDark = Color(0xFFFFC4BC)
val onErrorDark = Color(0xFF741210)
val errorContainerDark = Color(0xFF8C1515)
val onErrorContainerDark = Color(0xFFFFE5E1)
val backgroundDark = Color(0xFF181A20)
val onBackgroundDark = Color(0xFFE8E8EF)
val onSurfaceDark = Color(0xFFE8E8EF)
val onSurfaceVariantDark = Color(0xFFD0D2DC)
val outlineDark = Color(0xFF9A9CA5)
val outlineVariantDark = Color(0xFF525560)
val surfaceContainerLowDark = Color(0xFF20232A)
val surfaceContainerDark = Color(0xFF252830)
val surfaceContainerHighDark = Color(0xFF30333B)

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

fun buildEventColorScheme(
    base: ColorScheme,
    eventColor: Color,
    eventUrgent: Boolean = false
): ColorScheme {
    if (eventUrgent) {
        val surfaceContainerHigh = blend(base.surfaceContainerHigh, base.error, 0.08f)
        val surfaceContainer = blend(base.surfaceContainer, base.error, 0.06f)
        val surfaceContainerLow = blend(base.surfaceContainerLow, base.error, 0.03f)

        return base.copy(
            primary = base.error,
            onPrimary = base.onError,
            primaryContainer = base.errorContainer,
            onPrimaryContainer = base.onErrorContainer,
            secondary = base.error,
            onSecondary = base.onError,
            secondaryContainer = base.errorContainer,
            onSecondaryContainer = base.onErrorContainer,
            tertiary = base.error,
            onTertiary = base.onError,
            tertiaryContainer = base.errorContainer,
            onTertiaryContainer = base.onErrorContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainer = surfaceContainer,
            surfaceContainerLow = surfaceContainerLow,
            primaryDim = base.errorContainer,
            secondaryDim = base.errorContainer,
            tertiaryDim = base.errorContainer
        )
    }

    val (eventH, eventS, eventL) = toHsl(eventColor)

    val primary = if (eventL < 0.45f) {
        fromHsl(eventH, eventS, 0.78f)
    } else {
        fromHsl(eventH, eventS, (eventL + 0.15f).coerceAtMost(0.88f))
    }
    val onPrimary = Color(0xFF1F1F1F)

    val primaryContainer = fromHsl(eventH, (eventS * 0.85f).coerceAtLeast(0.35f), 0.22f)
    val onPrimaryContainer = fromHsl(eventH, (eventS * 0.35f).coerceAtLeast(0.12f), 0.90f)

    val secondary = fromHsl(eventH, (eventS * 0.55f).coerceAtLeast(0.25f), 0.82f)
    val onSecondary = Color(0xFF1F1F1F)

    val secondaryContainer = fromHsl(eventH, (eventS * 0.50f).coerceAtLeast(0.22f), 0.26f)
    val onSecondaryContainer = fromHsl(eventH, (eventS * 0.30f).coerceAtLeast(0.10f), 0.91f)

    val tertiary = fromHsl(eventH, (eventS * 0.45f).coerceAtLeast(0.20f), 0.85f)
    val onTertiary = Color(0xFF1F1F1F)

    val tertiaryContainer = fromHsl(eventH, (eventS * 0.40f).coerceAtLeast(0.18f), 0.30f)
    val onTertiaryContainer = fromHsl(eventH, (eventS * 0.28f).coerceAtLeast(0.09f), 0.92f)

    val surfaceContainerHigh = blend(base.surfaceContainerHigh, eventColor, 0.08f)
    val surfaceContainer = blend(base.surfaceContainer, eventColor, 0.06f)
    val surfaceContainerLow = blend(base.surfaceContainerLow, eventColor, 0.03f)

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