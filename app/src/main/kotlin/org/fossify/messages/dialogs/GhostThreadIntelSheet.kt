package org.fossify.messages.dialogs
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.Patterns
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
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
import org.fossify.messages.helpers.GhostLinkGuard
import org.fossify.messages.helpers.GhostOtpHelper
import org.fossify.messages.models.Message
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern
class GhostThreadIntelSheet(
    private val activity: BaseSimpleActivity,
    private val threadTitle: String,
    private val messages: List<Message>
) {
    private val TX_CODE_REGEX = Pattern.compile("\\b([A-Z0-9]{10})\\s+Confirmed\\b", Pattern.CASE_INSENSITIVE)
    fun show() {
        val dialog = BottomSheetDialog(activity)
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        val bgColor = activity.getProperBackgroundColor()
        val primaryColor = activity.getProperPrimaryColor()
        val textColor = activity.getProperTextColor()
        val density = activity.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        val codes = ArrayList<Pair<String, String>>()
        val links = ArrayList<Pair<String, String>>()
        var mediaCount = 0
        val dateFmt = SimpleDateFormat("dd MMM, HH:mm", Locale.US)
        for (msg in messages.sortedByDescending { it.date }) {
            val dateLabel = dateFmt.format(Date(msg.date * 1000L))
            val body = msg.body
            if ((msg.attachment?.attachments?.size ?: 0) > 0) {
                mediaCount += msg.attachment!!.attachments.size
            }
            val otp = GhostOtpHelper.extractOtp(body)
            if (otp != null) {
                codes.add(otp to "OTP / Verification • $dateLabel")
            }
            val txMatcher = TX_CODE_REGEX.matcher(body)
            if (txMatcher.find()) {
                val tx = txMatcher.group(1)?.uppercase(Locale.ROOT)
                if (tx != null && codes.none { it.first == tx }) {
                    codes.add(tx to "Transaction Ref • $dateLabel")
                }
            }
            val urlMatcher = Patterns.WEB_URL.matcher(body)
            while (urlMatcher.find()) {
                val raw = urlMatcher.group() ?: continue
                val normalized = if (raw.startsWith("http://", true) || raw.startsWith("https://", true)) raw else "https://$raw"
                if (links.none { it.first == normalized }) {
                    links.add(normalized to dateLabel)
                }
            }
        }
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(24))
            background = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadii = floatArrayOf(dp(22).toFloat(), dp(22).toFloat(), dp(22).toFloat(), dp(22).toFloat(), 0f, 0f, 0f, 0f)
            }
        }
        root.addView(TextView(activity).apply {
            text = "Thread Intel • $threadTitle"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f)
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(textColor)
        })
        root.addView(TextView(activity).apply {
            text = "${messages.size} messages • ${codes.size} codes • ${links.size} links • $mediaCount attachments"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(textColor.adjustAlpha(0.68f))
            setPadding(0, dp(2), 0, dp(14))
        })
        var selectedTab = 0
        val tabRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(12) }
        }
        val listContainer = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        fun renderItems() {
            listContainer.removeAllViews()
            if (selectedTab == 0) {
                if (codes.isEmpty()) {
                    listContainer.addView(TextView(activity).apply {
                        text = "No OTPs or transaction codes detected in this conversation."
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                        setTextColor(textColor.adjustAlpha(0.7f))
                        gravity = Gravity.CENTER
                        setPadding(dp(12), dp(30), dp(12), dp(30))
                    })
                } else {
                    for ((code, meta) in codes) {
                        val row = LinearLayout(activity).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.CENTER_VERTICAL
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
                                val cm = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Ghost Code", code))
                                activity.toast("Copied $code")
                            }
                        }
                        val col = LinearLayout(activity).apply {
                            orientation = LinearLayout.VERTICAL
                            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        }
                        col.addView(TextView(activity).apply {
                            text = code
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                            setTypeface(typeface, Typeface.BOLD)
                            setTextColor(primaryColor)
                        })
                        col.addView(TextView(activity).apply {
                            text = meta
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                            setTextColor(textColor.adjustAlpha(0.68f))
                        })
                        row.addView(col)
                        row.addView(TextView(activity).apply {
                            text = "Copy"
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                            setTypeface(typeface, Typeface.BOLD)
                            setTextColor(primaryColor.getContrastColor())
                            setPadding(dp(12), dp(6), dp(12), dp(6))
                            background = GradientDrawable().apply {
                                setColor(primaryColor)
                                cornerRadius = dp(14).toFloat()
                            }
                        })
                        listContainer.addView(row)
                    }
                }
            } else {
                if (links.isEmpty()) {
                    listContainer.addView(TextView(activity).apply {
                        text = "No links shared in this conversation yet."
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                        setTextColor(textColor.adjustAlpha(0.7f))
                        gravity = Gravity.CENTER
                        setPadding(dp(12), dp(30), dp(12), dp(30))
                    })
                } else {
                    for ((url, dateLabel) in links) {
                        val row = LinearLayout(activity).apply {
                            orientation = LinearLayout.VERTICAL
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
                                GhostLinkGuard.handleLinkClick(activity, url)
                            }
                        }
                        row.addView(TextView(activity).apply {
                            text = url
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                            setTypeface(typeface, Typeface.BOLD)
                            setTextColor(primaryColor)
                        })
                        row.addView(TextView(activity).apply {
                            text = "Shared $dateLabel • Tap to inspect & open"
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                            setTextColor(textColor.adjustAlpha(0.68f))
                        })
                        listContainer.addView(row)
                    }
                }
            }
        }
        fun rebuildTabs() {
            tabRow.removeAllViews()
            listOf("Codes & OTPs (${codes.size})", "Shared Links (${links.size})").forEachIndexed { idx, title ->
                val active = idx == selectedTab
                tabRow.addView(TextView(activity).apply {
                    text = title
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    setTypeface(typeface, Typeface.BOLD)
                    gravity = Gravity.CENTER
                    setTextColor(if (active) primaryColor.getContrastColor() else textColor)
                    setPadding(dp(12), dp(9), dp(12), dp(9))
                    background = GradientDrawable().apply {
                        setColor(if (active) primaryColor else primaryColor.adjustAlpha(0.12f))
                        cornerRadius = dp(16).toFloat()
                    }
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        if (idx == 0) marginEnd = dp(8)
                    }
                    setOnClickListener {
                        if (selectedTab != idx) {
                            selectedTab = idx
                            rebuildTabs()
                            renderItems()
                        }
                    }
                })
            }
        }
        rebuildTabs()
        renderItems()
        root.addView(tabRow)
        val scroll = ScrollView(activity).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(320))
        }
        scroll.addView(listContainer)
        root.addView(scroll)
        dialog.setContentView(root)
        dialog.show()
    }
}