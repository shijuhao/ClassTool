package com.juhao.classtool.theme

import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme

val defaultSeed = Color(0xFF9BD7FF)

private val neutralBase: ColorScheme = ColorScheme(
    primary = Color(0xFF1F1F1F),
    onPrimary = Color(0xFF1F1F1F),
    primaryContainer = Color(0xFF1F1F1F),
    onPrimaryContainer = Color(0xFF1F1F1F),
    secondary = Color(0xFF1F1F1F),
    onSecondary = Color(0xFF1F1F1F),
    secondaryContainer = Color(0xFF1F1F1F),
    onSecondaryContainer = Color(0xFF1F1F1F),
    tertiary = Color(0xFF1F1F1F),
    onTertiary = Color(0xFF1F1F1F),
    tertiaryContainer = Color(0xFF1F1F1F),
    onTertiaryContainer = Color(0xFF1F1F1F),
    error = Color(0xFFFFC4BC),
    onError = Color(0xFF741210),
    errorContainer = Color(0xFF8C1515),
    onErrorContainer = Color(0xFFFFE5E1),
    background = Color(0xFF181A20),
    onBackground = Color(0xFFE8E8EF),
    onSurface = Color(0xFFE8E8EF),
    onSurfaceVariant = Color(0xFFD0D2DC),
    outline = Color(0xFF9A9CA5),
    outlineVariant = Color(0xFF525560),
    surfaceContainerLow = Color(0xFF20232A),
    surfaceContainer = Color(0xFF252830),
    surfaceContainerHigh = Color(0xFF30333B),
    primaryDim = Color(0xFF2E4A73),
    secondaryDim = Color(0xFF424B5C),
    tertiaryDim = Color(0xFF5A4260)
)

internal val wearColorScheme: ColorScheme = buildEventColorScheme(
    base = neutralBase,
    eventColor = defaultSeed,
    eventUrgent = false
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

fun buildCustomColorScheme(
    base: ColorScheme,
    customColor: Color
): ColorScheme {
    val (h, s, l) = toHsl(customColor)

    val primary = customColor
    val onPrimary = if (l > 0.55f) Color(0xFF1F1F1F) else Color(0xFFFFFFFF)

    val primaryContainer = fromHsl(h, (s * 0.85f).coerceAtLeast(0.35f), 0.22f)
    val onPrimaryContainer = fromHsl(h, (s * 0.35f).coerceAtLeast(0.12f), 0.90f)
    val primaryDim = fromHsl(h, (s * 0.80f).coerceAtLeast(0.30f), 0.28f)

    val secondary = fromHsl(h, (s * 0.55f).coerceAtLeast(0.25f), 0.82f)
    val onSecondary = Color(0xFF1F1F1F)
    val secondaryContainer = fromHsl(h, (s * 0.50f).coerceAtLeast(0.22f), 0.26f)
    val onSecondaryContainer = fromHsl(h, (s * 0.30f).coerceAtLeast(0.10f), 0.91f)
    val secondaryDim = fromHsl(h, (s * 0.50f).coerceAtLeast(0.22f), 0.30f)

    val tertiary = fromHsl(h, (s * 0.45f).coerceAtLeast(0.20f), 0.85f)
    val onTertiary = Color(0xFF1F1F1F)
    val tertiaryContainer = fromHsl(h, (s * 0.40f).coerceAtLeast(0.18f), 0.30f)
    val onTertiaryContainer = fromHsl(h, (s * 0.28f).coerceAtLeast(0.09f), 0.92f)
    val tertiaryDim = fromHsl(h, (s * 0.40f).coerceAtLeast(0.18f), 0.32f)

    val surfaceContainerHigh = blend(base.surfaceContainerHigh, customColor, 0.08f)
    val surfaceContainer = blend(base.surfaceContainer, customColor, 0.06f)
    val surfaceContainerLow = blend(base.surfaceContainerLow, customColor, 0.03f)

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
        primaryDim = primaryDim,
        secondaryDim = secondaryDim,
        tertiaryDim = tertiaryDim
    )
}

enum class AppTheme(
    val displayName: String,
    val seedColor: Color
) {
    MORNING("晨曦", Color(0xFFFFB74D)),
    DUSK("暮云", Color(0xFF9575CD)),
    BAMBOO("青竹", Color(0xFF4DB6AC)),
    SAKURA("绯樱", Color(0xFFF48FB1)),
    INK("墨夜", Color(0xFF64B5F6)),
    CUSTOM("自定义", Color(0xFF9BD7FF))
}

fun buildThemeColorScheme(
    base: ColorScheme,
    theme: AppTheme
): ColorScheme = buildEventColorScheme(
    base = base,
    eventColor = theme.seedColor,
    eventUrgent = false
)

internal val morningScheme: ColorScheme = buildThemeColorScheme(neutralBase, AppTheme.MORNING)
internal val duskScheme: ColorScheme = buildThemeColorScheme(neutralBase, AppTheme.DUSK)
internal val bambooScheme: ColorScheme = buildThemeColorScheme(neutralBase, AppTheme.BAMBOO)
internal val sakuraScheme: ColorScheme = buildThemeColorScheme(neutralBase, AppTheme.SAKURA)
internal val inkScheme: ColorScheme = buildThemeColorScheme(neutralBase, AppTheme.INK)