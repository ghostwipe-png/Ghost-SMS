package org.fossify.messages.helpers
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import org.fossify.commons.extensions.getContrastColor
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.messages.models.Message
import org.fossify.messages.models.ThreadItem
object GhostBubbleStyler {
    private const val GROUP_WINDOW_SECONDS = 120L
    private val MPESA_AMOUNT_REGEX = Regex("""\b(Ksh|KES)\.?\s?([0-9,]+(?:\.[0-9]{2})?)\b""", RegexOption.IGNORE_CASE)
    private val OTP_CODE_REGEX = Regex("""\b(\d{4,8})\b""")
    private fun isAppDarkTheme(context: Context): Boolean {
        return ColorUtils.calculateLuminance(context.getProperBackgroundColor()) < 0.5
    }
    private fun normalizeSeconds(rawDate: Int): Long {
        val d = rawDate.toLong()
        return if (d > 10_000_000_000L) d / 1000L else d
    }
    fun applyGroupedBubble(
        context: Context,
        textView: TextView,
        items: List<ThreadItem>,
        position: Int,
        message: Message,
        isSent: Boolean,
        fillColor: Int
    ) {
        val density = context.resources.displayMetrics.density
        val rLarge = 20f * density
        val rCluster = 6f * density
        val rTail = 4f * density
        val prevMsg = items.getOrNull(position - 1) as? Message
        val nextMsg = items.getOrNull(position + 1) as? Message
        val msgSec = normalizeSeconds(message.date)
        val sameAsPrev = prevMsg != null &&
            prevMsg.isReceivedMessage() == message.isReceivedMessage() &&
            prevMsg.senderName == message.senderName &&
            kotlin.math.abs(msgSec - normalizeSeconds(prevMsg.date)) <= GROUP_WINDOW_SECONDS
        val sameAsNext = nextMsg != null &&
            nextMsg.isReceivedMessage() == message.isReceivedMessage() &&
            nextMsg.senderName == message.senderName &&
            kotlin.math.abs(normalizeSeconds(nextMsg.date) - msgSec) <= GROUP_WINDOW_SECONDS
        val radii = if (isSent) {
            val topRight = if (sameAsPrev) rCluster else rLarge
            val bottomRight = if (sameAsNext) rCluster else rTail
            floatArrayOf(
                rLarge, rLarge,
                topRight, topRight,
                bottomRight, bottomRight,
                rLarge, rLarge
            )
        } else {
            val topLeft = if (sameAsPrev) rCluster else rLarge
            val bottomLeft = if (sameAsNext) rCluster else rTail
            floatArrayOf(
                topLeft, topLeft,
                rLarge, rLarge,
                rLarge, rLarge,
                bottomLeft, bottomLeft
            )
        }
        val darkTheme = isAppDarkTheme(context)
        val resolvedFill = when {
            isSent -> fillColor
            darkTheme -> Color.parseColor("#1C222B")
            else -> Color.parseColor("#E6ECF4")
        }
        val bubbleDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadii = radii
            setColor(resolvedFill)
            setStroke(0, Color.TRANSPARENT)
        }
        textView.background = bubbleDrawable
        textView.setTextColor(resolvedFill.getContrastColor())
        textView.setLineSpacing(0f, 1.20f)
        val padH = (15 * density).toInt()
        val padV = (10 * density).toInt()
        textView.setPadding(padH, padV, padH, padV)
        (textView.layoutParams as? ViewGroup.MarginLayoutParams)?.let { lp ->
            lp.topMargin = if (sameAsPrev) (2 * density).toInt() else (6 * density).toInt()
            textView.layoutParams = lp
        }
    }
    fun formatRichMessageText(
        context: Context,
        rawText: String,
        isSent: Boolean,
        accentColor: Int
    ): CharSequence {
        if (rawText.isBlank()) return rawText
        val spannable = SpannableStringBuilder(rawText)
        val darkTheme = isAppDarkTheme(context)
        val highlightColor = when {
            isSent -> Color.WHITE
            darkTheme -> ColorUtils.blendARGB(accentColor, Color.WHITE, 0.45f)
            else -> ColorUtils.blendARGB(accentColor, Color.BLACK, 0.25f)
        }
        MPESA_AMOUNT_REGEX.findAll(rawText).forEach { match ->
            spannable.setSpan(StyleSpan(Typeface.BOLD), match.range.first, match.range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(RelativeSizeSpan(1.08f), match.range.first, match.range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(ForegroundColorSpan(highlightColor), match.range.first, match.range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        val lower = rawText.lowercase()
        if (lower.contains("code") || lower.contains("otp") || lower.contains("pin") || lower.contains("verification")) {
            OTP_CODE_REGEX.findAll(rawText).forEach { match ->
                spannable.setSpan(TypefaceSpan("monospace"), match.range.first, match.range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannable.setSpan(StyleSpan(Typeface.BOLD), match.range.first, match.range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannable.setSpan(RelativeSizeSpan(1.14f), match.range.first, match.range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannable.setSpan(ForegroundColorSpan(highlightColor), match.range.first, match.range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        return spannable
    }
}