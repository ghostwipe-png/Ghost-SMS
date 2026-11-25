package org.fossify.messages.helpers
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
object GhostSheetStyler {
    fun applyBorderlessSheetSurface(context: Context, rootView: View) {
        val density = context.resources.displayMetrics.density
        val rTop = 28f * density
        val darkTheme = GhostInboxStyler.isAppDarkTheme(context)
        val fillColor = if (darkTheme) Color.parseColor("#131820") else Color.parseColor("#F7F9FC")
        rootView.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadii = floatArrayOf(rTop, rTop, rTop, rTop, 0f, 0f, 0f, 0f)
            setColor(fillColor)
            setStroke(0, Color.TRANSPARENT)
        }
    }
    fun applyBorderlessCard(context: Context, cardView: View, elevated: Boolean = false) {
        val density = context.resources.displayMetrics.density
        val darkTheme = GhostInboxStyler.isAppDarkTheme(context)
        val fillColor = when {
            darkTheme && elevated -> Color.parseColor("#1E2633")
            darkTheme -> Color.parseColor("#19202B")
            elevated -> Color.parseColor("#E4ECF7")
            else -> Color.parseColor("#EDF2F8")
        }
        cardView.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 18f * density
            setColor(fillColor)
            setStroke(0, Color.TRANSPARENT)
        }
    }
}