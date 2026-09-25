package com.juhao.classtool.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.PI
import kotlin.math.min

class RoundToastView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val path = Path()
    private val arcRect = RectF()

    private var toastText: String = ""

    private val toastColor = 0xCC1A1A1A.toInt()
    private val borderColor = 0x33FFFFFF
    private val textColor = 0xFFFFFFFF.toInt()

    private val textSizeVal = dp(15f)
    private val bgThickness = dp(24f)
    private val borderWidth = dp(0.8f)
    private val bottomMargin = dp(8f)
    private val textPadding = dp(20f)

    private val minSweepAngle = 20f
    private val maxSweepAngle = 140f

    init {
        borderPaint.apply {
            style = Paint.Style.STROKE
            color = borderColor
            strokeWidth = bgThickness + borderWidth * 2
            strokeCap = Paint.Cap.ROUND
        }
        bgPaint.apply {
            style = Paint.Style.STROKE
            color = toastColor
            strokeWidth = bgThickness
            strokeCap = Paint.Cap.ROUND
        }
        textPaint.apply {
            color = textColor
            textSize = textSizeVal
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
            letterSpacing = 0.05f
        }
    }

    fun setText(text: CharSequence) {
        toastText = text.toString()
        if (width > 0 && height > 0) updatePath(width.toFloat(), height.toFloat())
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updatePath(w.toFloat(), h.toFloat())
    }

    private fun updatePath(w: Float, h: Float) {
        path.reset()
        if (toastText.isEmpty()) return

        val screenRadius = min(w, h) / 2f
        val totalThickness = bgThickness + borderWidth * 2
        val arcRadius = screenRadius - bottomMargin - totalThickness / 2f

        val cx = w / 2f
        val cy = h / 2f
        arcRect.set(cx - arcRadius, cy - arcRadius, cx + arcRadius, cy + arcRadius)

        val textWidth = textPaint.measureText(toastText)
        val perimeter = 2 * PI * arcRadius
        val targetArcLength = textWidth + textPadding
        val sweep = (targetArcLength / perimeter * 360)
            .toFloat()
            .coerceIn(minSweepAngle, maxSweepAngle)

        val signedSweep = -sweep
        val startAngle = 90f - signedSweep / 2f
        path.addArc(arcRect, startAngle, signedSweep)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (toastText.isEmpty()) return

        canvas.drawPath(path, borderPaint)
        canvas.drawPath(path, bgPaint)

        val fm = textPaint.fontMetrics
        val textHeight = fm.descent - fm.ascent
        val vOffset = textHeight / 2f - fm.descent
        canvas.drawTextOnPath(toastText, path, 0f, vOffset, textPaint)
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}