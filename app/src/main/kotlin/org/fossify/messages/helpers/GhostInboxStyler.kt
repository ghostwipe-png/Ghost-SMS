package org.fossify.messages.helpers
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.content.res.ColorStateList
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import androidx.core.graphics.ColorUtils
import org.fossify.commons.extensions.getContrastColor
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.messages.models.Conversation
import kotlin.math.abs
object GhostInboxStyler {
    private val MPESA_SNIPPET_REGEX = Regex("""\b(Ksh|KES)\.?\s?([0-9,]+(?:\.[0-9]{2})?)\b""", RegexOption.IGNORE_CASE)
    private val OTP_SNIPPET_REGEX = Regex("""\b(\d{4,8})\b""")
    // Curated Material 3 tonal container palettes (darkContainer, darkOnContainer, lightContainer, lightOnContainer)
    private val MONOGRAM_PALETTES = listOf(
        intArrayOf(0xFF1E3A5F.toInt(), 0xFFD0E4FF.toInt(), 0xFFD3E4FF.toInt(), 0xFF001C38.toInt()), // Sapphire
        intArrayOf(0xFF16423C.toInt(), 0xFFBCECE0.toInt(), 0xFFC4EFD9.toInt(), 0xFF002117.toInt()), // Emerald
        intArrayOf(0xFF3E2C5E.toInt(), 0xFFE9DDFF.toInt(), 0xFFEADDFF.toInt(), 0xFF21005D.toInt()), // Amethyst
        intArrayOf(0xFF4D3419.toInt(), 0xFFFFDDB7.toInt(), 0xFFFFDDB3.toInt(), 0xFF291800.toInt()), // Amber
        intArrayOf(0xFF4F2436.toInt(), 0xFFFFD9E3.toInt(), 0xFFFFD8E4.toInt(), 0xFF31111D.toInt()), // Rose
        intArrayOf(0xFF1A3D47.toInt(), 0xFFBCEBF7.toInt(), 0xFFBCEBF7.toInt(), 0xFF001F26.toInt())  // Cyan
    )
    fun isAppDarkTheme(context: Context): Boolean {
        return ColorUtils.calculateLuminance(context.getProperBackgroundColor()) < 0.5
    }
    fun applyBorderlessCardSurface(
        context: Context,
        cardView: View,
        isUnread: Boolean,
        isSelected: Boolean,
        primaryColor: Int
    ) {
        val density = context.resources.displayMetrics.density
        val radius = 18f * density
        val darkTheme = isAppDarkTheme(context)
        val fillColor = when {
            isSelected -> ColorUtils.setAlphaComponent(primaryColor, if (darkTheme) 68 else 48)
            isUnread -> if (darkTheme) Color.parseColor("#1D2430") else Color.parseColor("#E5EDF8")
            else -> if (darkTheme) Color.parseColor("#141921") else Color.parseColor("#F2F5FA")
        }
        val shape = GradientDrawable().apply {
            this.shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(fillColor)
            // Strictly zero borderlines
            setStroke(0, Color.TRANSPARENT)
        }
        val rippleColor = ColorStateList.valueOf(
            ColorUtils.setAlphaComponent(primaryColor, if (darkTheme) 45 else 30)
        )
        cardView.background = RippleDrawable(rippleColor, shape, shape)
    }
    fun buildMonogramAvatar(context: Context, title: String, isGroup: Boolean): Drawable {
        val density = context.resources.displayMetrics.density
        val sizePx = (48 * density).toInt().coerceAtLeast(96)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cleanTitle = title.trim()
        val hash = abs(cleanTitle.hashCode())
        val palette = MONOGRAM_PALETTES[hash % MONOGRAM_PALETTES.size]
        val darkTheme = isAppDarkTheme(context)
        val bgColor = if (darkTheme) palette[0] else palette[2]
        val fgColor = if (darkTheme) palette[1] else palette[3]
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            style = Paint.Style.FILL
        }
        val radius = sizePx / 2f
        canvas.drawCircle(radius, radius, radius, bgPaint)
        val initials = extractInitials(cleanTitle, isGroup)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fgColor
            textSize = sizePx * (if (initials.length > 1) 0.38f else 0.44f)
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val bounds = Rect()
        textPaint.getTextBounds(initials, 0, initials.length, bounds)
        val y = radius - bounds.exactCenterY()
        canvas.drawText(initials, radius, y, textPaint)
        return BitmapDrawable(context.resources, bitmap)
    }
    private fun extractInitials(title: String, isGroup: Boolean): String {
        if (title.isBlank()) return if (isGroup) "GR" else "#"
        val words = title.split(Regex("""[\s\-_]+""")).filter { it.isNotBlank() }
        val letters = words.mapNotNull { w -> w.firstOrNull { it.isLetter() }?.uppercaseChar() }
        return when {
            letters.size >= 2 -> "${letters[0]}${letters[1]}"
            letters.size == 1 -> "${letters[0]}"
            title.any { it.isDigit() } -> "#"
            else -> title.take(1).uppercase()
        }
    }
    fun formatSnippet(
        context: Context,
        conversation: Conversation,
        rawSnippet: String,
        primaryColor: Int
    ): CharSequence {
        if (rawSnippet.isBlank()) return rawSnippet
        val spannable = SpannableStringBuilder(rawSnippet)
        val darkTheme = isAppDarkTheme(context)
        val highlight = if (darkTheme) {
            ColorUtils.blendARGB(primaryColor, Color.WHITE, 0.42f)
        } else {
            ColorUtils.blendARGB(primaryColor, Color.BLACK, 0.20f)
        }
        MPESA_SNIPPET_REGEX.findAll(rawSnippet).forEach { match ->
            spannable.setSpan(StyleSpan(Typeface.BOLD), match.range.first, match.range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(ForegroundColorSpan(highlight), match.range.first, match.range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        val lower = rawSnippet.lowercase()
        if (lower.contains("code") || lower.contains("otp") || lower.contains("pin") || lower.contains("verification")) {
            OTP_SNIPPET_REGEX.findAll(rawSnippet).forEach { match ->
                spannable.setSpan(StyleSpan(Typeface.BOLD), match.range.first, match.range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                spannable.setSpan(ForegroundColorSpan(highlight), match.range.first, match.range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        return spannable
    }
}