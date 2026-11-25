package org.fossify.messages.dialogs
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.adjustAlpha
import org.fossify.commons.extensions.getContrastColor
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.toast
import org.fossify.messages.helpers.GhostPrivacyManager
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.helpers.GhostOtpSweeper
class GhostPrivacySheet(
    private val activity: BaseSimpleActivity,
    private val onStateChanged: () -> Unit = {}
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
            setPadding(dp(20), dp(18), dp(20), dp(26))
            background = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadii = floatArrayOf(dp(22).toFloat(), dp(22).toFloat(), dp(22).toFloat(), dp(22).toFloat(), 0f, 0f, 0f, 0f)
            }
        }
        root.addView(TextView(activity).apply {
            text = "Ghost Mode & Stealth Privacy"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(textColor)
        })
        root.addView(TextView(activity).apply {
            text = "Anti-shoulder-surfing, phishing shield & panic decoy"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(textColor.adjustAlpha(0.68f))
            setPadding(0, dp(2), 0, dp(16))
        })
        fun addToggleCard(
            title: String,
            subtitle: String,
            initialChecked: Boolean,
            onToggle: (Boolean) -> Unit
        ) {
            val card = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
                background = GradientDrawable().apply {
                    setColor(textColor.adjustAlpha(0.06f))
                    cornerRadius = dp(14).toFloat()
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(10) }
            }
            val textCol = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = dp(10)
                }
            }
            textCol.addView(TextView(activity).apply {
                text = title
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(textColor)
            })
            textCol.addView(TextView(activity).apply {
                text = subtitle
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                setTextColor(textColor.adjustAlpha(0.70f))
            })
            card.addView(textCol)
            val toggle = Switch(activity).apply {
                isChecked = initialChecked
                setOnCheckedChangeListener { _, isChecked -> onToggle(isChecked) }
            }
            card.addView(toggle)
            card.setOnClickListener { toggle.isChecked = !toggle.isChecked }
            root.addView(card)
        }
        addToggleCard(
            title = "Recents & Screenshot Shield",
            subtitle = "Blanks app preview in Recent Apps & blocks screenshots",
            initialChecked = GhostPrivacyManager.isStealthScreenEnabled(activity)
        ) { enabled ->
            GhostPrivacyManager.setStealthScreenEnabled(activity, enabled)
            GhostPrivacyManager.applyWindowSecurity(activity)
            activity.toast(if (enabled) "Stealth screen armed" else "Stealth screen disabled")
        }
        addToggleCard(
            title = "Phishing & Link Shield",
            subtitle = "Inspects M-Pesa, bank & shortened URLs before opening",
            initialChecked = GhostPrivacyManager.isLinkGuardEnabled(activity)
        ) { enabled ->
            GhostPrivacyManager.setLinkGuardEnabled(activity, enabled)
            activity.toast(if (enabled) "Link Shield active" else "Link Shield paused")
        }
        addToggleCard(
            title = "Panic / Decoy Vault Mask",
            subtitle = "Makes Ghost Vault appear completely empty when opened",
            initialChecked = GhostPrivacyManager.isPanicModeActive(activity)
        ) { active ->
            GhostPrivacyManager.setPanicModeActive(activity, active)
            onStateChanged()
            activity.toast(if (active) "Panic Mode ON: Vault masked as empty" else "Panic Mode OFF: Real vault restored")
        }
                addToggleCard(
            title = "Auto-Sweep Expired OTPs (24h)",
            subtitle = "Deletes 24h+ old verification codes while preserving receipts",
            initialChecked = GhostOtpSweeper.isAutoSweepEnabled(activity)
        ) { enabled ->
            GhostOtpSweeper.setAutoSweepEnabled(activity, enabled)
            if (enabled) {
                ensureBackgroundThread {
                    val swept = GhostOtpSweeper.sweepExpiredOtps(activity)
                    activity.runOnUiThread {
                        activity.toast(if (swept > 0) "Swept $swept expired OTP messages" else "Auto-sweep armed")
                        onStateChanged()
                    }
                }
            } else {
                activity.toast("OTP auto-sweep disabled")
            }
        }

        // Decoy PIN configuration button
        val currentPin = GhostPrivacyManager.getDecoyPin(activity)
        val pinBtn = TextView(activity).apply {
            text = if (currentPin != null) "Update Decoy Vault PIN (Active: ${"•".repeat(currentPin.length)})" else "Set Decoy Vault PIN"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(primaryColor.getContrastColor())
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(13), dp(16), dp(13))
            background = GradientDrawable().apply {
                setColor(primaryColor)
                cornerRadius = dp(14).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(4) }
            setOnClickListener {
                val input = EditText(activity).apply {
                    hint = "Enter 4-8 digit Decoy PIN (leave blank to clear)"
                    inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
                    setPadding(dp(18), dp(14), dp(18), dp(14))
                }
                AlertDialog.Builder(activity)
                    .setTitle("Decoy Vault PIN")
                    .setMessage("Entering this PIN instead of your real fingerprint/PIN opens a clean, empty vault.")
                    .setView(input)
                    .setPositiveButton("Save PIN") { _, _ ->
                        val entered = input.text.toString().trim()
                        if (entered.isEmpty()) {
                            GhostPrivacyManager.setDecoyPin(activity, null)
                            activity.toast("Decoy PIN cleared")
                        } else if (entered.length < 4) {
                            activity.toast("Use at least 4 digits")
                        } else {
                            GhostPrivacyManager.setDecoyPin(activity, entered)
                            activity.toast("Decoy Vault PIN saved")
                        }
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
        root.addView(pinBtn)
        dialog.setContentView(root)
        dialog.show()
    }
}