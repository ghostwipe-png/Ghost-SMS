package org.fossify.messages.helpers
import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.ActionMode
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.SearchEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.recyclerview.widget.RecyclerView
object GhostHapticEngine {
    private var lastTapMs = 0L
    fun install(application: Application) {
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                wrapWindowCallback(activity)
            }
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityResumed(activity: Activity) {
                GhostMotionEngine.onActivityResumed(activity)
            }
            override fun onActivityPaused(activity: Activity) {
                GhostMotionEngine.onActivityPaused(activity)
            }
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
    private fun wrapWindowCallback(activity: Activity) {
        val window = activity.window ?: return
        val original = window.callback ?: return
        window.callback = object : Window.Callback {
            override fun dispatchTouchEvent(event: MotionEvent): Boolean {
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    val decor = window.decorView
                    if (hasInteractiveTargetAt(decor, event.rawX.toInt(), event.rawY.toInt())) {
                        pulseTap(decor)
                    }
                }
                return original.dispatchTouchEvent(event)
            }
            override fun dispatchKeyEvent(event: KeyEvent) = original.dispatchKeyEvent(event)
            override fun dispatchKeyShortcutEvent(event: KeyEvent) = original.dispatchKeyShortcutEvent(event)
            override fun dispatchTrackballEvent(event: MotionEvent) = original.dispatchTrackballEvent(event)
            override fun dispatchGenericMotionEvent(event: MotionEvent) = original.dispatchGenericMotionEvent(event)
            override fun dispatchPopulateAccessibilityEvent(event: AccessibilityEvent) = original.dispatchPopulateAccessibilityEvent(event)
            override fun onCreatePanelView(featureId: Int) = original.onCreatePanelView(featureId)
            override fun onCreatePanelMenu(featureId: Int, menu: Menu) = original.onCreatePanelMenu(featureId, menu)
            override fun onPreparePanel(featureId: Int, view: View?, menu: Menu) = original.onPreparePanel(featureId, view, menu)
            override fun onMenuOpened(featureId: Int, menu: Menu) = original.onMenuOpened(featureId, menu)
            override fun onMenuItemSelected(featureId: Int, item: MenuItem) = original.onMenuItemSelected(featureId, item)
            override fun onWindowAttributesChanged(attrs: WindowManager.LayoutParams) = original.onWindowAttributesChanged(attrs)
            override fun onContentChanged() = original.onContentChanged()
            override fun onWindowFocusChanged(hasFocus: Boolean) = original.onWindowFocusChanged(hasFocus)
            override fun onAttachedToWindow() = original.onAttachedToWindow()
            override fun onDetachedFromWindow() = original.onDetachedFromWindow()
            override fun onPanelClosed(featureId: Int, menu: Menu) = original.onPanelClosed(featureId, menu)
            override fun onSearchRequested() = original.onSearchRequested()
            override fun onSearchRequested(searchEvent: SearchEvent) = original.onSearchRequested(searchEvent)
            override fun onWindowStartingActionMode(callback: ActionMode.Callback) = original.onWindowStartingActionMode(callback)
            override fun onWindowStartingActionMode(callback: ActionMode.Callback, type: Int) = original.onWindowStartingActionMode(callback, type)
            override fun onActionModeStarted(mode: ActionMode) = original.onActionModeStarted(mode)
            override fun onActionModeFinished(mode: ActionMode) = original.onActionModeFinished(mode)
        }
    }
    private fun hasInteractiveTargetAt(view: View, rawX: Int, rawY: Int): Boolean {
        if (view.visibility != View.VISIBLE) return false
        val loc = IntArray(2)
        view.getLocationOnScreen(loc)
        val within = rawX >= loc[0] && rawX <= loc[0] + view.width &&
            rawY >= loc[1] && rawY <= loc[1] + view.height
        if (!within) return false
        if (view is ViewGroup) {
            for (i in view.childCount - 1 downTo 0) {
                if (hasInteractiveTargetAt(view.getChildAt(i), rawX, rawY)) return true
            }
            if (view is RecyclerView) return true
        }
        return view.isClickable || view.isLongClickable
    }
    fun pulseTap(view: View) {
        val now = System.currentTimeMillis()
        if (now - lastTapMs < 35L) return
        lastTapMs = now
        view.performHapticFeedback(
            HapticFeedbackConstants.CLOCK_TICK,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
        vibrateOneShot(view.context, 9L, 95)
    }
    fun pulseIncomingMessage(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0L, 18L, 45L, 26L)
            val amplitudes = intArrayOf(0, 140, 0, 210)
            runCatching { vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1)) }
        } else {
            @Suppress("DEPRECATION")
            runCatching { vibrator.vibrate(longArrayOf(0L, 18L, 45L, 26L), -1) }
        }
    }
    fun pulseMessageSent(context: Context) {
        vibrateOneShot(context, 16L, 165)
    }
    fun pulseOtpCopied(context: Context) {
        val vibrator = getVibrator(context) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0L, 12L, 30L, 16L), intArrayOf(0, 120, 0, 180), -1))
            }
        }
    }
    private fun vibrateOneShot(context: Context, durationMs: Long, amplitude: Int) {
        val vibrator = getVibrator(context) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching { vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude)) }
        } else {
            @Suppress("DEPRECATION")
            runCatching { vibrator.vibrate(durationMs) }
        }
    }
    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}