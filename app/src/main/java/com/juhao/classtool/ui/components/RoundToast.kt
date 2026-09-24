package com.juhao.classtool.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import java.lang.ref.WeakReference

fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}

object RoundToast {

    const val LENGTH_SHORT = 0
    const val LENGTH_LONG = 1

    private const val ANIM_DURATION = 400L
    private const val SHORT_DELAY = 2000L
    private const val LONG_DELAY = 3500L
    private const val HIDDEN_TRANSLATION_Y = 100f

    private val handler = Handler(Looper.getMainLooper())
    private var currentViewRef: WeakReference<View>? = null
    private var dismissRunnable: Runnable? = null

    @JvmStatic
    fun show(context: Context, resId: Int, duration: Int = LENGTH_SHORT) {
        show(context, context.getString(resId), duration)
    }

    @JvmStatic
    fun show(context: Context, message: CharSequence, duration: Int = LENGTH_SHORT) {
        if (message.isEmpty()) return

        val activity = context.findActivity() ?: return
        if (Looper.myLooper() != Looper.getMainLooper()) {
            handler.post { showInternal(activity, message, duration) }
        } else {
            showInternal(activity, message, duration)
        }
    }

    private fun showInternal(activity: Activity, message: CharSequence, duration: Int) {
        cancelCurrent()

        val wm = activity.windowManager
        val toastView = RoundToastView(activity).apply {
            setText(message)
            translationY = HIDDEN_TRANSLATION_Y
            alpha = 0f
        }
        currentViewRef = WeakReference(toastView)

        try {
            wm.addView(toastView, buildLayoutParams())
        } catch (e: Exception) {
            e.printStackTrace()
            currentViewRef = null
            return
        }

        toastView.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(ANIM_DURATION)
            .setInterpolator(OvershootInterpolator(1.2f))
            .start()

        val delay = if (duration == LENGTH_LONG) LONG_DELAY else SHORT_DELAY
        dismissRunnable = Runnable { dismiss(wm, toastView) }
        handler.postDelayed(dismissRunnable!!, delay)
    }

    private fun dismiss(wm: WindowManager, view: View) {
        view.animate()
            .translationY(HIDDEN_TRANSLATION_Y)
            .alpha(0f)
            .setDuration(ANIM_DURATION)
            .withEndAction {
                removeViewSafely(wm, view)
                if (currentViewRef?.get() === view) currentViewRef = null
            }
            .start()
    }

    private fun cancelCurrent() {
        dismissRunnable?.let { handler.removeCallbacks(it) }
        dismissRunnable = null

        val view = currentViewRef?.get() ?: return
        val wm = view.context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        if (wm != null) removeViewSafely(wm, view)
        currentViewRef = null
    }

    private fun removeViewSafely(wm: WindowManager, view: View) {
        try {
            wm.removeViewImmediate(view)
        } catch (_: Exception) {
        }
    }

    private fun buildLayoutParams() = WindowManager.LayoutParams().apply {
        height = WindowManager.LayoutParams.WRAP_CONTENT
        width = WindowManager.LayoutParams.MATCH_PARENT
        format = PixelFormat.TRANSLUCENT
        type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        y = 0
        windowAnimations = 0
    }
}