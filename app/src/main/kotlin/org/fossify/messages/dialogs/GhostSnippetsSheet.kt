package org.fossify.messages.dialogs
import android.app.AlertDialog
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.adjustAlpha
import org.fossify.commons.extensions.getContrastColor
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.toast
import org.json.JSONArray
class GhostSnippetsSheet(
    private val activity: BaseSimpleActivity,
    private val onSnippetSelected: (String) -> Unit
) {
    companion object {
        private const val PREFS_NAME = "ghost_snippets_prefs"
        private const val KEY_SNIPPETS = "saved_snippets_json"
        private val DEFAULT_SNIPPETS = listOf(
            "On my way now, will be there in 10 minutes.",
            "Please share your M-Pesa Till / Paybill or phone number.",
            "Payment confirmed, thank you!",
            "Currently in a meeting — will call you right back."
        )
        fun getSnippets(context: Context): MutableList<String> {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val raw = prefs.getString(KEY_SNIPPETS, null) ?: return DEFAULT_SNIPPETS.toMutableList()
            return try {
                val arr = JSONArray(raw)
                MutableList(arr.length()) { idx -> arr.getString(idx) }
            } catch (_: Exception) {
                DEFAULT_SNIPPETS.toMutableList()
            }
        }
        fun saveSnippets(context: Context, list: List<String>) {
            val arr = JSONArray()
            list.forEach { arr.put(it) }
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_SNIPPETS, arr.toString()).apply()
        }
    }
    fun show() {
        val dialog = BottomSheetDialog(activity)
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        val bgColor = activity.getProperBackgroundColor()
        val primaryColor = activity.getProperPrimaryColor()
        val textColor = activity.getProperTextColor()
        val density = activity.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(24))
            background = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadii = floatArrayOf(dp(22).toFloat(), dp(22).toFloat(), dp(22).toFloat(), dp(22).toFloat(), 0f, 0f, 0f, 0f)
            }
        }
        root.addView(TextView(activity).apply {
            text = "Ghost Quick Snippets"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f)
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(textColor)
        })
        root.addView(TextView(activity).apply {
            text = "Tap a template to insert into compose bar • Long-press any snippet to delete"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(textColor.adjustAlpha(0.68f))
            setPadding(0, dp(2), 0, dp(14))
        })
        val listContainer = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        fun rebuildList() {
            listContainer.removeAllViews()
            val snippets = getSnippets(activity)
            for ((index, snippet) in snippets.withIndex()) {
                val card = TextView(activity).apply {
                    text = snippet
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    setTextColor(textColor)
                    setPadding(dp(14), dp(12), dp(14), dp(12))
                    background = GradientDrawable().apply {
                        setColor(textColor.adjustAlpha(0.06f))
                        cornerRadius = dp(12).toFloat()
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = dp(8) }
                    setOnClickListener {
                        onSnippetSelected(snippet)
                        dialog.dismiss()
                    }
                    setOnLongClickListener {
                        val updated = getSnippets(activity)
                        if (index in updated.indices) {
                            updated.removeAt(index)
                            saveSnippets(activity, updated)
                            activity.toast("Snippet removed")
                            rebuildList()
                        }
                        true
                    }
                }
                listContainer.addView(card)
            }
        }
        val addBtn = TextView(activity).apply {
            text = "+ Add Custom Snippet"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setTextColor(primaryColor.getContrastColor())
            setPadding(dp(14), dp(11), dp(14), dp(11))
            background = GradientDrawable().apply {
                setColor(primaryColor)
                cornerRadius = dp(14).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
            setOnClickListener {
                val input = EditText(activity).apply {
                    hint = "Type reusable template (e.g. Paybill / Directions)"
                }
                AlertDialog.Builder(activity)
                    .setTitle("New Ghost Snippet")
                    .setView(input)
                    .setPositiveButton("Save") { _, _ ->
                        val text = input.text?.toString()?.trim().orEmpty()
                        if (text.isNotEmpty()) {
                            val updated = getSnippets(activity)
                            updated.add(0, text)
                            saveSnippets(activity, updated)
                            rebuildList()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
        root.addView(addBtn)
        val scroll = ScrollView(activity).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(260))
        }
        scroll.addView(listContainer)
        root.addView(scroll)
        rebuildList()
        dialog.setContentView(root)
        dialog.show()
    }
}