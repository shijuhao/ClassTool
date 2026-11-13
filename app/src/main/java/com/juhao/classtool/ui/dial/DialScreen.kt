package com.juhao.classtool.ui.dial

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import com.juhao.classtool.datastore.ScheduleAdjustment
import com.juhao.classtool.datastore.ScheduleEvent
import com.juhao.classtool.datastore.ScheduleEventType
import com.juhao.classtool.datastore.Weekday
import com.juhao.classtool.ui.schedule.eventColorFor
import com.juhao.classtool.ui.schedule.eventDisplayNameFor
import com.juhao.classtool.ui.schedule.eventsOnDate
import com.juhao.classtool.ui.schedule.toMinutes
import com.juhao.classtool.ui.schedule.weekdayLabel
import com.juhao.classtool.utils.LocalScreenShape
import com.juhao.classtool.utils.ScreenShape
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

/** 课前预备铃窗口（秒），与主页保持一致。 */
private const val PREP_SECONDS = 180

/** 课程环里，课间最多按 12 分钟等价占位，避免午休把整圈切掉一大块。 */
private const val BREAK_CAP_MINUTES = 12f

/** 弧段之间的缝隙（角度）。 */
private const val RING_GAP_DEGREES = 2.4f

private data class TimedEvent(val event: ScheduleEvent, val start: Float, val end: Float)

private data class RingItem(
    val event: ScheduleEvent?,
    val start: Float,
    val end: Float,
    val duration: Float
)

private data class DialSegment(
    val startFraction: Float,
    val endFraction: Float,
    val color: Color,
    val isPast: Boolean,
    val isNext: Boolean,
    val isCurrent: Boolean,
    val currentFraction: Float
)

private data class DialMarker(val fraction: Float, val color: Color)

private data class DialUiState(
    val timeText: String,
    val dateText: String,
    val segments: List<DialSegment>,
    val marker: DialMarker?,
    val cardTitle: String,
    val cardSubtitle: String,
    val cardLabel: String?,
    val cardValue: String?,
    val cardColor: Color,
    val footerText: String,
    val footerDotColor: Color?
)

/**
 * 表盘页：主界面左滑的第二页。
 *
 * - 圆屏：今日课程环 + 课程卡片 + 大号时间 + 日期 + 下一节；
 * - 方屏：不放环，课程卡片加宽、时间放大，元素与圆屏一一对应。
 */
@Composable
fun DialScreen(
    events: List<ScheduleEvent>,
    adjustments: List<ScheduleAdjustment>,
    todayDate: String,
    nowSecondOfDay: Int,
    weekday: Weekday,
    prepBellEnabled: Boolean,
    squareCornerRadiusDp: Float = 40f,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val state = remember(events, adjustments, todayDate, nowSecondOfDay, weekday, prepBellEnabled) {
        buildDialState(
            events = events,
            adjustments = adjustments,
            todayDate = todayDate,
            nowSecondOfDay = nowSecondOfDay,
            weekday = weekday,
            prepBellEnabled = prepBellEnabled,
            fallbackColor = scheme.primary,
            neutralMarkerColor = scheme.onSurfaceVariant
        )
    }

    if (LocalScreenShape.current == ScreenShape.SQUARE) {
        SquareDial(state, squareCornerRadiusDp, modifier)
    } else {
        RoundDial(state, modifier)
    }
}

// ----------------------------------------------------------------------------
// 圆屏
// ----------------------------------------------------------------------------

@Composable
private fun RoundDial(state: DialUiState, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val trackColor = scheme.onSurfaceVariant.copy(alpha = 0.12f)
    val tickMajorColor = scheme.onSurfaceVariant.copy(alpha = 0.30f)
    val tickMinorColor = scheme.onSurfaceVariant.copy(alpha = 0.14f)
    val markerColor = state.marker?.color ?: scheme.primary

    BoxWithConstraints(modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        // 圆屏理论上宽高相等；万一被系统 inset 切成非正方，环按短边算，避免被裁。
        val ringExtent = minOf(screenWidth, screenHeight)
        val ringStroke = ringExtent * 0.0264f
        val ringInset = ringExtent * 0.0573f
        val ringRadius = ringExtent / 2 - ringInset - ringStroke / 2

        Canvas(Modifier.fillMaxSize()) {
            drawDialRing(
                segments = state.segments,
                marker = state.marker,
                markerColor = markerColor,
                ringRadius = ringRadius.toPx(),
                ringStroke = ringStroke.toPx(),
                trackColor = trackColor,
                tickMajorColor = tickMajorColor,
                tickMinorColor = tickMinorColor,
                markerBackdrop = scheme.background
            )
        }

        DialCourseCard(
            state = state,
            titleSize = (screenWidth.value * 0.046f).sp,
            subtitleSize = (screenWidth.value * 0.029f).sp,
            valueSize = (screenWidth.value * 0.058f).sp,
            labelSize = (screenWidth.value * 0.023f).sp,
            dotSize = screenWidth * 0.010f,
            horizontalPadding = screenWidth * 0.044f,
            cornerRadius = screenWidth * 0.053f,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = screenHeight * 0.21f)
                .width(screenWidth * 0.545f)
                .height(screenHeight * 0.185f)
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = screenHeight * 0.065f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = state.timeText,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = (screenWidth.value * 0.20f).sp,
                    lineHeight = (screenWidth.value * 0.22f).sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = scheme.onSurface,
                maxLines = 1
            )
            Spacer(Modifier.height(screenHeight * 0.012f))
            Text(
                text = state.dateText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = (screenWidth.value * 0.042f).sp,
                    lineHeight = (screenWidth.value * 0.05f).sp
                ),
                color = scheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        DialFooter(
            state = state,
            textSize = (screenWidth.value * 0.029f).sp,
            dotSize = screenWidth * 0.007f,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = screenHeight * 0.20f)
        )
    }
}

// ----------------------------------------------------------------------------
// 方屏（DW99 这类 Android 手表会被判定为方屏）
// ----------------------------------------------------------------------------

@Composable
private fun SquareDial(state: DialUiState, cornerRadiusDp: Float, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val trackColor = scheme.onSurfaceVariant.copy(alpha = 0.12f)
    val tickMajorColor = scheme.onSurfaceVariant.copy(alpha = 0.30f)
    val tickMinorColor = scheme.onSurfaceVariant.copy(alpha = 0.14f)
    val markerColor = state.marker?.color ?: scheme.primary

    BoxWithConstraints(modifier.fillMaxSize()) {
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        val extent = minOf(screenWidth, screenHeight)

        // 环的几何
        val ringInset = extent * 0.06f
        val ringStroke = extent * 0.024f
        val ringSide = extent - ringInset * 2
        val ringRadius = minOf(cornerRadiusDp.dp, ringSide / 2)

        // 内容要躲开环和圆角：圆角越大，可用区域越小
        val contentInset = ringInset + ringStroke / 2 + ringRadius * 0.293f
        val contentWidth = (screenWidth - contentInset * 2).coerceAtLeast(extent * 0.4f)
        val contentHeight = (screenHeight - contentInset * 2).coerceAtLeast(extent * 0.4f)

        val insetPx = with(density) { ringInset.toPx() }
        val strokePx = with(density) { ringStroke.toPx() }
        val widthPx = with(density) { screenWidth.toPx() }
        val heightPx = with(density) { screenHeight.toPx() }
        val radiusPx = with(density) { ringRadius.toPx() }

        Canvas(Modifier.fillMaxSize()) {
            drawSquareRing(
                segments = state.segments,
                marker = state.marker,
                markerColor = markerColor,
                left = insetPx,
                top = insetPx,
                right = widthPx - insetPx,
                bottom = heightPx - insetPx,
                cornerRadius = radiusPx,
                ringStroke = strokePx,
                trackColor = trackColor,
                tickMajorColor = tickMajorColor,
                tickMinorColor = tickMinorColor,
                markerBackdrop = scheme.background
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = contentInset, vertical = contentInset),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DialCourseCard(
                state = state,
                titleSize = (contentWidth.value * 0.065f).sp,
                subtitleSize = (contentWidth.value * 0.032f).sp,
                valueSize = (contentWidth.value * 0.085f).sp,
                labelSize = (contentWidth.value * 0.026f).sp,
                dotSize = contentWidth * 0.014f,
                horizontalPadding = contentWidth * 0.05f,
                cornerRadius = contentWidth * 0.055f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(contentHeight * 0.24f)
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = state.timeText,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = (contentWidth.value * 0.34f).sp,
                    lineHeight = (contentWidth.value * 0.37f).sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = scheme.onSurface,
                maxLines = 1
            )
            Spacer(Modifier.height(contentHeight * 0.015f))
            Text(
                text = state.dateText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = (contentWidth.value * 0.055f).sp,
                    lineHeight = (contentWidth.value * 0.065f).sp
                ),
                color = scheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(Modifier.weight(0.55f))
            DialFooter(
                state = state,
                textSize = (contentWidth.value * 0.042f).sp,
                dotSize = contentWidth * 0.010f,
                modifier = Modifier
            )
        }
    }
}

// ----------------------------------------------------------------------------
// 课程卡片 / 页脚
// ----------------------------------------------------------------------------

@Composable
private fun DialCourseCard(
    state: DialUiState,
    titleSize: TextUnit,
    subtitleSize: TextUnit,
    valueSize: TextUnit,
    labelSize: TextUnit,
    dotSize: Dp,
    horizontalPadding: Dp,
    cornerRadius: Dp,
    modifier: Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(cornerRadius)
    val containerColor = lerp(scheme.background, state.cardColor, 0.19f)
    val borderColor = state.cardColor.copy(alpha = 0.38f)

    Row(
        modifier = modifier
            .clip(shape)
            .background(containerColor)
            .border(1.dp, borderColor, shape)
            .padding(horizontal = horizontalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(dotSize)
                .clip(CircleShape)
                .background(state.cardColor)
        ) {}
        Spacer(Modifier.width(horizontalPadding * 0.5f))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = state.cardTitle,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = titleSize,
                    lineHeight = (titleSize.value * 1.18f).sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (state.cardSubtitle.isNotEmpty()) {
                Spacer(Modifier.height(1.dp))
                Text(
                    text = state.cardSubtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = subtitleSize,
                        lineHeight = (subtitleSize.value * 1.2f).sp
                    ),
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        val value = state.cardValue
        if (value != null) {
            Spacer(Modifier.width(horizontalPadding * 0.4f))
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                val label = state.cardLabel
                if (label != null) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = labelSize,
                            lineHeight = (labelSize.value * 1.2f).sp
                        ),
                        color = scheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = valueSize,
                        lineHeight = (valueSize.value * 1.1f).sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = scheme.onSurface,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun DialFooter(
    state: DialUiState,
    textSize: TextUnit,
    dotSize: Dp,
    modifier: Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        val dotColor = state.footerDotColor
        if (dotColor != null) {
            Box(
                Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(dotColor)
            ) {}
            Spacer(Modifier.width(dotSize))
        }
        Text(
            text = state.footerText,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = textSize,
                lineHeight = (textSize.value * 1.2f).sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ----------------------------------------------------------------------------
// 环的绘制
// ----------------------------------------------------------------------------

private fun DrawScope.drawDialRing(
    segments: List<DialSegment>,
    marker: DialMarker?,
    markerColor: Color,
    ringRadius: Float,
    ringStroke: Float,
    trackColor: Color,
    tickMajorColor: Color,
    tickMinorColor: Color,
    markerBackdrop: Color
) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val ringTopLeft = Offset(center.x - ringRadius, center.y - ringRadius)
    val ringSize = Size(ringRadius * 2f, ringRadius * 2f)

    // 底轨
    drawCircle(
        color = trackColor,
        radius = ringRadius,
        center = center,
        style = Stroke(width = ringStroke)
    )

    // 装饰刻度（不表示具体时间）
    val tickInner = ringRadius + ringStroke / 2f + size.width * 0.012f
    val tickOuterMinor = tickInner + size.width * 0.011f
    val tickOuterMajor = tickInner + size.width * 0.022f
    for (index in 0 until 60) {
        val degrees = index * 6f
        val major = index % 5 == 0
        val radians = Math.toRadians((degrees - 90f).toDouble())
        val dx = cos(radians).toFloat()
        val dy = sin(radians).toFloat()
        val outer = if (major) tickOuterMajor else tickOuterMinor
        drawLine(
            color = if (major) tickMajorColor else tickMinorColor,
            start = Offset(center.x + dx * tickInner, center.y + dy * tickInner),
            end = Offset(center.x + dx * outer, center.y + dy * outer),
            strokeWidth = if (major) size.width * 0.0044f else size.width * 0.0024f,
            cap = StrokeCap.Round
        )
    }

    // 课程弧段
    segments.forEach { segment ->
        val startAngle = -90f + segment.startFraction * 360f + RING_GAP_DEGREES / 2f
        val endAngle = -90f + segment.endFraction * 360f - RING_GAP_DEGREES / 2f
        if (endAngle <= startAngle) return@forEach
        when {
            segment.isPast -> drawArc(
                color = segment.color.copy(alpha = 0.24f),
                startAngle = startAngle,
                sweepAngle = endAngle - startAngle,
                useCenter = false,
                topLeft = ringTopLeft,
                size = ringSize,
                style = Stroke(width = ringStroke, cap = StrokeCap.Round)
            )

            segment.isCurrent -> {
                val nowAngle = -90f + segment.currentFraction * 360f
                if (endAngle > nowAngle) {
                    drawArc(
                        color = segment.color.copy(alpha = 0.28f),
                        startAngle = nowAngle,
                        sweepAngle = endAngle - nowAngle,
                        useCenter = false,
                        topLeft = ringTopLeft,
                        size = ringSize,
                        style = Stroke(width = ringStroke, cap = StrokeCap.Round)
                    )
                }
                if (nowAngle > startAngle) {
                    drawArc(
                        color = segment.color,
                        startAngle = startAngle,
                        sweepAngle = nowAngle - startAngle,
                        useCenter = false,
                        topLeft = ringTopLeft,
                        size = ringSize,
                        style = Stroke(width = ringStroke, cap = StrokeCap.Round)
                    )
                }
            }

            else -> drawArc(
                color = segment.color.copy(alpha = if (segment.isNext) 0.66f else 0.46f),
                startAngle = startAngle,
                sweepAngle = endAngle - startAngle,
                useCenter = false,
                topLeft = ringTopLeft,
                size = ringSize,
                style = Stroke(width = ringStroke, cap = StrokeCap.Round)
            )
        }
    }

    // 「现在」标记
    if (marker != null) {
        val markerAngle = -90f + marker.fraction * 360f
        val radians = Math.toRadians(markerAngle.toDouble())
        val markerX = center.x + cos(radians).toFloat() * ringRadius
        val markerY = center.y + sin(radians).toFloat() * ringRadius
        drawCircle(
            color = markerColor.copy(alpha = 0.16f),
            radius = ringStroke * 2.6f,
            center = Offset(markerX, markerY)
        )
        drawCircle(
            color = markerBackdrop,
            radius = ringStroke * 0.82f,
            center = Offset(markerX, markerY)
        )
        drawCircle(
            color = markerColor,
            radius = ringStroke * 0.46f,
            center = Offset(markerX, markerY)
        )
    }
}

private const val RING_SAMPLE_STEP_PX = 2f

/**
 * 圆角矩形环的路径参数化：从 12 点方向开始、顺时针按弧长取点。
 * 圆角 = 短边一半时退化成圆，所以方环和圆环可以共用同一套分段逻辑。
 */
private class RoundedRectRing(
    private val left: Float,
    private val top: Float,
    private val right: Float,
    private val bottom: Float,
    cornerRadius: Float
) {
    val radius: Float = cornerRadius.coerceIn(0f, minOf(right - left, bottom - top) / 2f)
    val length: Float

    private class Piece(
        val isArc: Boolean,
        val start: Offset,
        val end: Offset,
        val arcRadius: Float,
        val startAngle: Float,
        val sweepAngle: Float,
        val length: Float
    )

    private val pieces = ArrayList<Piece>(9)

    init {
        val r = radius
        val centerX = (left + right) / 2f

        fun line(ax: Float, ay: Float, bx: Float, by: Float) {
            pieces.add(
                Piece(
                    isArc = false,
                    start = Offset(ax, ay),
                    end = Offset(bx, by),
                    arcRadius = 0f,
                    startAngle = 0f,
                    sweepAngle = 0f,
                    length = hypot(bx - ax, by - ay)
                )
            )
        }

        fun arc(centerX0: Float, centerY0: Float, startDegrees: Float, sweepDegrees: Float) {
            pieces.add(
                Piece(
                    isArc = true,
                    start = Offset(centerX0, centerY0),
                    end = Offset.Zero,
                    arcRadius = r,
                    startAngle = startDegrees,
                    sweepAngle = sweepDegrees,
                    length = Math.toRadians(abs(sweepDegrees).toDouble()).toFloat() * r
                )
            )
        }

        line(centerX, top, right - r, top)
        arc(right - r, top + r, -90f, 90f)
        line(right, top + r, right, bottom - r)
        arc(right - r, bottom - r, 0f, 90f)
        line(right - r, bottom, left + r, bottom)
        arc(left + r, bottom - r, 90f, 90f)
        line(left, bottom - r, left, top + r)
        arc(left + r, top + r, 180f, 90f)
        line(left + r, top, centerX, top)

        length = pieces.fold(0f) { acc, piece -> acc + piece.length }
    }

    fun pointAt(distance: Float): Offset {
        if (length <= 0f) return Offset(left, top)
        var rest = distance % length
        if (rest < 0f) rest += length
        for (piece in pieces) {
            if (piece.length <= 0f) continue
            if (rest <= piece.length) {
                val progress = rest / piece.length
                return if (piece.isArc) {
                    val radians = Math.toRadians(
                        (piece.startAngle + piece.sweepAngle * progress).toDouble()
                    )
                    Offset(
                        piece.start.x + piece.arcRadius * cos(radians).toFloat(),
                        piece.start.y + piece.arcRadius * sin(radians).toFloat()
                    )
                } else {
                    Offset(
                        piece.start.x + (piece.end.x - piece.start.x) * progress,
                        piece.start.y + (piece.end.y - piece.start.y) * progress
                    )
                }
            }
            rest -= piece.length
        }
        return Offset(left, top)
    }

    fun tangentAt(distance: Float): Offset {
        val delta = max(0.5f, length * 0.0005f)
        val before = pointAt(distance - delta)
        val after = pointAt(distance + delta)
        val dx = after.x - before.x
        val dy = after.y - before.y
        val norm = hypot(dx, dy)
        return if (norm <= 0f) Offset(1f, 0f) else Offset(dx / norm, dy / norm)
    }

    fun pathBetween(startDistance: Float, endDistance: Float): Path {
        val path = Path()
        val span = endDistance - startDistance
        if (span <= 0f) return path
        val steps = max(2, (span / RING_SAMPLE_STEP_PX).toInt())
        for (index in 0..steps) {
            val point = pointAt(startDistance + span * index / steps)
            if (index == 0) {
                path.moveTo(point.x, point.y)
            } else {
                path.lineTo(point.x, point.y)
            }
        }
        return path
    }
}

private fun DrawScope.drawSquareRing(
    segments: List<DialSegment>,
    marker: DialMarker?,
    markerColor: Color,
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    cornerRadius: Float,
    ringStroke: Float,
    trackColor: Color,
    tickMajorColor: Color,
    tickMinorColor: Color,
    markerBackdrop: Color
) {
    val ring = RoundedRectRing(left, top, right, bottom, cornerRadius)

    // 底轨
    drawRoundRect(
        color = trackColor,
        topLeft = Offset(left, top),
        size = Size(right - left, bottom - top),
        cornerRadius = CornerRadius(ring.radius, ring.radius),
        style = Stroke(width = ringStroke)
    )

    // 装饰刻度：沿周长均匀分布
    val tickCount = 72
    for (index in 0 until tickCount) {
        val distance = ring.length * index / tickCount
        val point = ring.pointAt(distance)
        val tangent = ring.tangentAt(distance)
        val normalX = tangent.y
        val normalY = -tangent.x
        val major = index % 6 == 0
        val inner = ringStroke * 0.85f
        val outer = inner + if (major) ringStroke * 1.15f else ringStroke * 0.6f
        drawLine(
            color = if (major) tickMajorColor else tickMinorColor,
            start = Offset(point.x + normalX * inner, point.y + normalY * inner),
            end = Offset(point.x + normalX * outer, point.y + normalY * outer),
            strokeWidth = if (major) ringStroke * 0.22f else ringStroke * 0.12f,
            cap = StrokeCap.Round
        )
    }

    // 课程弧段
    val segmentGap = ringStroke * 0.7f
    segments.forEach { segment ->
        val startDistance = ring.length * segment.startFraction + segmentGap / 2f
        val endDistance = ring.length * segment.endFraction - segmentGap / 2f
        if (endDistance <= startDistance) return@forEach
        when {
            segment.isPast -> drawPath(
                path = ring.pathBetween(startDistance, endDistance),
                color = segment.color.copy(alpha = 0.24f),
                style = Stroke(width = ringStroke, cap = StrokeCap.Round)
            )

            segment.isCurrent -> {
                val nowDistance = ring.length * segment.currentFraction
                if (endDistance > nowDistance) {
                    drawPath(
                        path = ring.pathBetween(nowDistance, endDistance),
                        color = segment.color.copy(alpha = 0.28f),
                        style = Stroke(width = ringStroke, cap = StrokeCap.Round)
                    )
                }
                if (nowDistance > startDistance) {
                    drawPath(
                        path = ring.pathBetween(startDistance, nowDistance),
                        color = segment.color,
                        style = Stroke(width = ringStroke, cap = StrokeCap.Round)
                    )
                }
            }

            else -> drawPath(
                path = ring.pathBetween(startDistance, endDistance),
                color = segment.color.copy(alpha = if (segment.isNext) 0.66f else 0.46f),
                style = Stroke(width = ringStroke, cap = StrokeCap.Round)
            )
        }
    }

    // 「现在」标记
    if (marker != null) {
        val point = ring.pointAt(ring.length * marker.fraction)
        drawCircle(
            color = markerColor.copy(alpha = 0.16f),
            radius = ringStroke * 2.6f,
            center = point
        )
        drawCircle(
            color = markerBackdrop,
            radius = ringStroke * 0.82f,
            center = point
        )
        drawCircle(
            color = markerColor,
            radius = ringStroke * 0.46f,
            center = point
        )
    }
}

// ----------------------------------------------------------------------------
// 状态计算
// ----------------------------------------------------------------------------

private fun resolveEventColor(event: ScheduleEvent, weekday: Weekday, fallback: Color): Color {
    val color = eventColorFor(event, weekday)
    return if (color == Color.Unspecified) fallback else color
}

private fun formatRemaining(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    return "%02d:%02d".format(safe / 60, safe % 60)
}

private fun formatTimeOfDay(secondOfDay: Int): String {
    val seconds = ((secondOfDay % 86400) + 86400) % 86400
    return "%02d:%02d".format(seconds / 3600, (seconds % 3600) / 60)
}

private fun formatDateLabel(date: String, weekday: Weekday): String = runCatching {
    val parsed = LocalDate.parse(date)
    "${parsed.monthValue}月${parsed.dayOfMonth}日 ${weekdayLabel(weekday)}"
}.getOrElse {
    weekdayLabel(weekday)
}

private fun buildRing(
    courseEvents: List<ScheduleEvent>,
    weekday: Weekday,
    nowMinute: Float,
    fallbackColor: Color,
    neutralMarkerColor: Color
): Pair<List<DialSegment>, DialMarker?> {
    val timed = courseEvents.mapNotNull { event ->
        val start = toMinutes(event.startTime) ?: return@mapNotNull null
        val end = toMinutes(event.endTime) ?: return@mapNotNull null
        if (end <= start) null else TimedEvent(event, start.toFloat(), end.toFloat())
    }
    if (timed.isEmpty()) return emptyList<DialSegment>() to null

    val items = mutableListOf<RingItem>()
    timed.forEachIndexed { index, item ->
        items += RingItem(item.event, item.start, item.end, item.end - item.start)
        val next = timed.getOrNull(index + 1) ?: return@forEachIndexed
        val gap = next.start - item.end
        if (gap > 0f) {
            items += RingItem(null, item.end, next.start, minOf(gap, BREAK_CAP_MINUTES))
        }
    }

    val total = items.fold(0f) { acc, item -> acc + item.duration }.coerceAtLeast(1f)
    val nextStart = timed.firstOrNull { it.start > nowMinute }?.start

    var cursor = 0f
    val segments = mutableListOf<DialSegment>()
    var marker: DialMarker? = null

    items.forEach { item ->
        val startFraction = cursor / total
        cursor += item.duration
        val endFraction = cursor / total

        val isInside = nowMinute >= item.start && nowMinute < item.end
        val progress = if (isInside) {
            ((nowMinute - item.start) / (item.end - item.start)).coerceIn(0f, 1f)
        } else {
            0f
        }
        val nowFraction = startFraction + (endFraction - startFraction) * progress

        val event = item.event
        if (isInside) {
            marker = DialMarker(
                fraction = nowFraction,
                color = if (event != null) {
                    resolveEventColor(event, weekday, fallbackColor)
                } else {
                    neutralMarkerColor
                }
            )
        }
        if (event == null) return@forEach

        segments += DialSegment(
            startFraction = startFraction,
            endFraction = endFraction,
            color = resolveEventColor(event, weekday, fallbackColor),
            isPast = nowMinute >= item.end,
            isNext = nextStart != null && item.start == nextStart,
            isCurrent = isInside,
            currentFraction = nowFraction
        )
    }

    return segments to marker
}

private fun buildDialState(
    events: List<ScheduleEvent>,
    adjustments: List<ScheduleAdjustment>,
    todayDate: String,
    nowSecondOfDay: Int,
    weekday: Weekday,
    prepBellEnabled: Boolean,
    fallbackColor: Color,
    neutralMarkerColor: Color
): DialUiState {
    val todayEvents = eventsOnDate(events, adjustments, todayDate)
    val courseEvents = todayEvents.filter { it.type != ScheduleEventType.BREAK }
    val nowMin = nowSecondOfDay / 60
    val nowMinute = nowSecondOfDay / 60f

    val currentEvent = todayEvents.firstOrNull { event ->
        val start = toMinutes(event.startTime) ?: return@firstOrNull false
        val end = toMinutes(event.endTime) ?: return@firstOrNull false
        nowMin in start until end
    }
    val nextCourse = courseEvents.firstOrNull { event ->
        val start = toMinutes(event.startTime) ?: return@firstOrNull false
        start > nowMin
    }
    val prepEvent = if (!prepBellEnabled) {
        null
    } else {
        courseEvents.firstOrNull { event ->
            val start = toMinutes(event.startTime) ?: return@firstOrNull false
            nowSecondOfDay >= start * 60 - PREP_SECONDS && nowSecondOfDay < start * 60
        }
    }

    val (segments, marker) = buildRing(
        courseEvents = courseEvents,
        weekday = weekday,
        nowMinute = nowMinute,
        fallbackColor = fallbackColor,
        neutralMarkerColor = neutralMarkerColor
    )

    var cardTitle = ""
    var cardSubtitle = ""
    var cardLabel: String? = null
    var cardValue: String? = null
    var cardColor = fallbackColor

    when {
        prepEvent != null -> {
            val start = toMinutes(prepEvent.startTime) ?: 0
            cardTitle = eventDisplayNameFor(prepEvent, weekday)
            cardSubtitle = "${prepEvent.startTime} - ${prepEvent.endTime}"
            cardLabel = "即将开始"
            cardValue = formatRemaining(start * 60 - nowSecondOfDay)
            cardColor = resolveEventColor(prepEvent, weekday, fallbackColor)
        }

        currentEvent != null && currentEvent.type != ScheduleEventType.BREAK -> {
            val end = toMinutes(currentEvent.endTime) ?: 0
            cardTitle = eventDisplayNameFor(currentEvent, weekday)
            cardSubtitle = "${currentEvent.startTime} - ${currentEvent.endTime}"
            cardLabel = "剩余"
            cardValue = formatRemaining(end * 60 - nowSecondOfDay)
            cardColor = resolveEventColor(currentEvent, weekday, fallbackColor)
        }

        else -> {
            val nextStartSec = nextCourse?.let { toMinutes(it.startTime)?.times(60) }
            cardTitle = if (courseEvents.isEmpty()) "今天没有课程" else "课间休息"
            cardSubtitle = when {
                nextCourse != null ->
                    "下一节 ${nextCourse.startTime} · ${eventDisplayNameFor(nextCourse, weekday)}"

                courseEvents.isEmpty() -> "今天没有安排，好好休息"
                else -> "今天课程已结束"
            }
            if (nextCourse != null && nextStartSec != null) {
                cardLabel = "距上课"
                cardValue = formatRemaining(nextStartSec - nowSecondOfDay)
            }
            cardColor = nextCourse?.let { resolveEventColor(it, weekday, fallbackColor) }
                ?: fallbackColor
        }
    }

    val finishedCount = courseEvents.count { event ->
        (toMinutes(event.endTime) ?: Int.MAX_VALUE) <= nowMin
    }
    val footerText = when {
        // 预备铃时卡片已经在倒数到上课，页脚改成今日进度，避免重复
        prepEvent != null -> "今日 ${courseEvents.size} 节 · 已完成 $finishedCount 节"

        nextCourse != null ->
            "下一节 ${nextCourse.startTime} · ${eventDisplayNameFor(nextCourse, weekday)}"

        courseEvents.isNotEmpty() -> "今日 ${courseEvents.size} 节 · 已完成 $finishedCount 节"

        else -> "今天没有安排，好好休息"
    }

    return DialUiState(
        timeText = formatTimeOfDay(nowSecondOfDay),
        dateText = formatDateLabel(todayDate, weekday),
        segments = segments,
        marker = marker,
        cardTitle = cardTitle,
        cardSubtitle = cardSubtitle,
        cardLabel = cardLabel,
        cardValue = cardValue,
        cardColor = cardColor,
        footerText = footerText,
        footerDotColor = nextCourse?.let { resolveEventColor(it, weekday, fallbackColor) }
    )
}
