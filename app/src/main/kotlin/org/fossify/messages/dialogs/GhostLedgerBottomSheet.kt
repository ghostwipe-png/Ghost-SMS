package org.fossify.messages.dialogs
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.helpers.GhostLedgerParser
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
class GhostLedgerBottomSheet(
    private val activity: BaseSimpleActivity,
    private val summary: GhostLedgerParser.LedgerSummary
) {
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
            setPadding(dp(18), dp(16), dp(18), dp(24))
            background = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadii = floatArrayOf(dp(22).toFloat(), dp(22).toFloat(), dp(22).toFloat(), dp(22).toFloat(), 0f, 0f, 0f, 0f)
            }
        }
        // Header row: Title + Export CSV pill
        val headerRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(12))
        }
        val titleCol = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        titleCol.addView(TextView(activity).apply {
            text = "Ghost Ledger"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(textColor)
        })
        titleCol.addView(TextView(activity).apply {
            text = "On-device Mobile Money & Bank tracker"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(textColor.adjustAlpha(0.68f))
        })
        headerRow.addView(titleCol)
        val exportBtn = TextView(activity).apply {
            text = "Export CSV"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(primaryColor.getContrastColor())
            setPadding(dp(14), dp(8), dp(14), dp(8))
            background = GradientDrawable().apply {
                setColor(primaryColor)
                cornerRadius = dp(18).toFloat()
            }
            setOnClickListener {
                if (summary.entries.isEmpty()) {
                    activity.toast("No financial messages to export yet")
                } else {
                    ensureBackgroundThread {
                        val savedPath = GhostLedgerParser.exportToCsv(activity, summary)
                        activity.runOnUiThread {
                            if (savedPath != null) {
                                activity.toast("Saved to $savedPath")
                            } else {
                                activity.toast("Could not export CSV")
                            }
                        }
                    }
                }
            }
        }
        headerRow.addView(exportBtn)
        root.addView(headerRow)
        // Latest Wallet Balance Card
        if (summary.latestBalance != null) {
            val balCard = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16), dp(14), dp(16), dp(14))
                background = GradientDrawable().apply {
                    setColor(primaryColor.adjustAlpha(0.14f))
                    setStroke(0, android.graphics.Color.TRANSPARENT)
                    cornerRadius = dp(16).toFloat()
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(10) }
            }
            balCard.addView(TextView(activity).apply {
                text = "LATEST DETECTED BALANCE"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(primaryColor)
            })
            balCard.addView(TextView(activity).apply {
                text = GhostLedgerParser.formatMoney(summary.latestBalance, summary.currency)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(textColor)
            })
            root.addView(balCard)
        }
        // Money In vs Money Out 30-Day Summary Row
        val flowRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(14) }
        }
        val greenColor = Color.parseColor("#22C55E")
        val redColor = Color.parseColor("#EF4444")
        fun buildStatBox(label: String, value: String, accent: Int, addEndMargin: Boolean): LinearLayout {
            return LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
                background = GradientDrawable().apply {
                    setColor(accent.adjustAlpha(0.12f))
                    setStroke(0, android.graphics.Color.TRANSPARENT)
                    cornerRadius = dp(14).toFloat()
                }
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    if (addEndMargin) marginEnd = dp(8)
                }
                addView(TextView(activity).apply {
                    text = label
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(accent)
                })
                addView(TextView(activity).apply {
                    text = value
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(textColor)
                })
            }
        }
        flowRow.addView(buildStatBox("MONEY IN (30D)", GhostLedgerParser.formatMoney(summary.totalIn, summary.currency), greenColor, true))
        flowRow.addView(buildStatBox("MONEY OUT (30D)", GhostLedgerParser.formatMoney(summary.totalOut, summary.currency), redColor, false))
        root.addView(flowRow)
        // Transactions list
        val scroll = ScrollView(activity).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(340)
            )
        }
        val listContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
        }
        if (summary.entries.isEmpty()) {
            listContainer.addView(TextView(activity).apply {
                text = "No M-Pesa or bank transaction SMS messages found yet."
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setTextColor(textColor.adjustAlpha(0.7f))
                gravity = Gravity.CENTER
                setPadding(dp(16), dp(36), dp(16), dp(36))
            })
        } else {
            val dateFmt = SimpleDateFormat("dd MMM, HH:mm", Locale.US)
            for (entry in summary.entries) {
                val accent = when (entry.direction) {
                    GhostLedgerParser.Direction.IN -> greenColor
                    GhostLedgerParser.Direction.OUT -> redColor
                    GhostLedgerParser.Direction.INFO -> primaryColor
                }
                val prefix = when (entry.direction) {
                    GhostLedgerParser.Direction.IN -> "+"
                    GhostLedgerParser.Direction.OUT -> "-"
                    GhostLedgerParser.Direction.INFO -> ""
                }
                val card = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(14), dp(11), dp(14), dp(11))
                    background = GradientDrawable().apply {
                        setColor(textColor.adjustAlpha(0.05f))
                        cornerRadius = dp(12).toFloat()
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = dp(8) }
                    setOnClickListener {
                        val cm = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("Transaction Code", entry.txCode))
                        activity.toast("Copied code ${entry.txCode}")
                    }
                }
                val topLine = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                topLine.addView(TextView(activity).apply {
                    text = entry.counterparty
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(textColor)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                topLine.addView(TextView(activity).apply {
                    text = "$prefix${GhostLedgerParser.formatMoney(entry.amount, entry.currency)}"
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(accent)
                })
                card.addView(topLine)
                val subLine = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, dp(3), 0, 0)
                }
                subLine.addView(TextView(activity).apply {
                    text = "${entry.txCode} • ${dateFmt.format(Date(entry.timestampSeconds * 1000L))}"
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    setTextColor(textColor.adjustAlpha(0.65f))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                if (entry.balance != null) {
                    subLine.addView(TextView(activity).apply {
                        text = "Bal: ${GhostLedgerParser.formatMoney(entry.balance, entry.currency)}"
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                        setTextColor(textColor.adjustAlpha(0.65f))
                    })
                }
                card.addView(subLine)
                listContainer.addView(card)
            }
        }
        scroll.addView(listContainer)
        root.addView(scroll)
        dialog.setContentView(root)
        dialog.show()
    }
}