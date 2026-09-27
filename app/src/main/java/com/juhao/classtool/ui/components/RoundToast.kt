package com.juhao.classtool.ui.components

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import java.lang.ref.WeakReference

fun Context.findActivity(): Activity? {
    var currentContext = this

    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) {
            return currentContext
        }

        currentContext = currentContext.baseContext
    }

    return null
}

object RoundToast {

    const val LENGTH_SHORT = 0
    const val LENGTH_LONG = 1

    private const val SHORT_DELAY = 2000L
    private const val LONG_DELAY = 3500L

    private const val ANIM_DURATION = 400L
    private const val HIDDEN_TRANSLATION_Y = 100f

    private val handler = Handler(Looper.getMainLooper())

    private var currentDialogRef: WeakReference<Dialog>? = null
    private var currentViewRef: WeakReference<RoundToastView>? = null

    private var dismissRunnable: Runnable? = null

    @JvmStatic
    fun show(
        context: Context,
        resId: Int,
        duration: Int = LENGTH_SHORT
    ) {
        show(
            context = context,
            message = context.getString(resId),
            duration = duration
        )
    }

    @JvmStatic
    fun show(
        context: Context,
        message: CharSequence,
        duration: Int = LENGTH_SHORT
    ) {
        if (message.isEmpty()) return

        val activity = context.findActivity() ?: return

        if (Looper.myLooper() != Looper.getMainLooper()) {
            handler.post {
                showInternal(
                    activity = activity,
                    message = message,
                    duration = duration
                )
            }
        } else {
            showInternal(
                activity = activity,
                message = message,
                duration = duration
            )
        }
    }

    private fun showInternal(
        activity: Activity,
        message: CharSequence,
        duration: Int
    ) {
        if (activity.isFinishing || activity.isDestroyed) {
            return
        }

        cancelCurrent()

        val dialog = Dialog(
            activity,
            android.R.style.Theme_Translucent_NoTitleBar_Fullscreen
        )

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val root = FrameLayout(activity).apply {
            setBackgroundColor(Color.TRANSPARENT)
        }

        val toastView = RoundToastView(activity).apply {
            setText(message)

            translationY = HIDDEN_TRANSLATION_Y
            alpha = 0f
        }

        root.addView(
            toastView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        dialog.setContentView(root)

        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)

        val window = dialog.window ?: return

        configureWindow(window)

        currentDialogRef = WeakReference(dialog)
        currentViewRef = WeakReference(toastView)

        try {
            dialog.show()
        } catch (_: Exception) {
            currentDialogRef = null
            currentViewRef = null
            return
        }

        dialog.window?.let(::configureWindow)

        toastView.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(ANIM_DURATION)
            .setInterpolator(
                android.view.animation.OvershootInterpolator(1.2f)
            )
            .start()

        val delay = if (duration == LENGTH_LONG) {
            LONG_DELAY
        } else {
            SHORT_DELAY
        }

        dismissRunnable = Runnable {
            dismiss(dialog, toastView)
        }

        handler.postDelayed(
            dismissRunnable!!,
            delay
        )
    }

    private fun configureWindow(window: Window) {
        window.setBackgroundDrawable(
            ColorDrawable(Color.TRANSPARENT)
        )

        window.setDimAmount(0f)

        window.clearFlags(
            WindowManager.LayoutParams.FLAG_DIM_BEHIND
        )

        window.addFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        window.setGravity(
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        )

        window.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )

        window.setWindowAnimations(0)
    }

    private fun dismiss(
        dialog: Dialog,
        view: RoundToastView
    ) {
        view.animate()
            .translationY(HIDDEN_TRANSLATION_Y)
            .alpha(0f)
            .setDuration(ANIM_DURATION)
            .withEndAction {
                try {
                    if (dialog.isShowing) {
                        dialog.dismiss()
                    }
                } catch (_: Exception) {
                }

                if (currentDialogRef?.get() === dialog) {
                    currentDialogRef = null
                }

                if (currentViewRef?.get() === view) {
                    currentViewRef = null
                }
            }
            .start()
    }

    private fun cancelCurrent() {
        dismissRunnable?.let {
            handler.removeCallbacks(it)
        }

        dismissRunnable = null

        val dialog = currentDialogRef?.get()
        val view = currentViewRef?.get()

        if (dialog != null && view != null) {
            view.animate().cancel()

            try {
                if (dialog.isShowing) {
                    dialog.dismiss()
                }
            } catch (_: Exception) {
            }
        } else if (dialog != null) {
            try {
                if (dialog.isShowing) {
                    dialog.dismiss()
                }
            } catch (_: Exception) {
            }
        }

        currentDialogRef = null
        currentViewRef = null
    }
}