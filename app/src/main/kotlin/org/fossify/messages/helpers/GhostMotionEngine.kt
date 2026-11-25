package org.fossify.messages.helpers
import android.app.Activity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import org.fossify.messages.activities.ThreadActivity
import org.fossify.messages.models.Message
import java.util.Collections
object GhostMotionEngine {
    @Volatile var foregroundActivityName: String? = null
    @Volatile var activeThreadId: Long = -1L
    private val animatedMessageIds = Collections.synchronizedSet(HashSet<Long>())
    fun onActivityResumed(activity: Activity) {
        foregroundActivityName = activity.javaClass.simpleName
        if (activity is ThreadActivity) {
            activeThreadId = activity.intent?.getLongExtra(THREAD_ID, -1L) ?: -1L
        } else {
            activeThreadId = -1L
        }
    }
    fun onActivityPaused(activity: Activity) {
        if (foregroundActivityName == activity.javaClass.simpleName) {
            foregroundActivityName = null
        }
        if (activity is ThreadActivity) {
            activeThreadId = -1L
        }
    }
    fun isViewingThread(threadId: Long): Boolean {
        return foregroundActivityName == "ThreadActivity" && activeThreadId == threadId
    }
    fun animateIncomingBubbleIfFresh(holderView: View, message: Message) {
        if (!message.isReceivedMessage()) return
        if (animatedMessageIds.contains(message.id)) return
        val nowSec = System.currentTimeMillis() / 1000L
        val msgSec = if (message.date > 10_000_000_000L) message.date / 1000L else message.date.toLong()
        // Only animate newly arrived live messages (within last 12 seconds)
        if (kotlin.math.abs(nowSec - msgSec) > 12L) {
            animatedMessageIds.add(message.id)
            return
        }
        animatedMessageIds.add(message.id)
        val density = holderView.resources.displayMetrics.density
        holderView.animate().cancel()
        holderView.pivotX = 16f * density
        holderView.pivotY = holderView.height.coerceAtLeast(48).toFloat()
        holderView.alpha = 0f
        holderView.scaleX = 0.82f
        holderView.scaleY = 0.82f
        holderView.translationX = -22f * density
        holderView.translationY = 14f * density
        holderView.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .translationX(0f)
            .translationY(0f)
            .setDuration(420L)
            .setInterpolator(OvershootInterpolator(1.35f))
            .start()
        GhostHapticEngine.pulseIncomingMessage(holderView.context)
    }
    fun animatePopupCardEntrance(cardView: View) {
        val density = cardView.resources.displayMetrics.density
        cardView.alpha = 0f
        cardView.scaleX = 0.88f
        cardView.scaleY = 0.88f
        cardView.translationY = -36f * density
        cardView.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .translationY(0f)
            .setDuration(360L)
            .setInterpolator(OvershootInterpolator(1.2f))
            .start()
    }
    fun animatePopupCardDismiss(cardView: View, onEnd: () -> Unit) {
        val density = cardView.resources.displayMetrics.density
        cardView.animate()
            .alpha(0f)
            .scaleX(0.92f)
            .scaleY(0.92f)
            .translationY(-24f * density)
            .setDuration(200L)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction(onEnd)
            .start()
    }
}