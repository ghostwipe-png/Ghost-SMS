package org.fossify.messages.activities
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.telephony.SmsManager
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.ColorUtils
import org.fossify.commons.extensions.getContrastColor
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.notificationManager
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.extensions.insertNewSMS
import org.fossify.messages.extensions.markThreadMessagesRead
import org.fossify.messages.helpers.GhostBubbleStyler
import org.fossify.messages.helpers.GhostHapticEngine
import org.fossify.messages.helpers.GhostInboxStyler
import org.fossify.messages.helpers.GhostMotionEngine
import org.fossify.messages.helpers.GhostOtpHelper
import org.fossify.messages.helpers.THREAD_ID
import org.fossify.messages.helpers.THREAD_TITLE
import org.fossify.messages.helpers.refreshConversations
import org.fossify.messages.helpers.refreshMessages
class GhostIncomingPopupActivity : Activity() {
    companion object {
        const val EXTRA_ADDRESS = "ghost_popup_address"
        const val EXTRA_SENDER = "ghost_popup_sender"
        const val EXTRA_BODY = "ghost_popup_body"
        const val EXTRA_THREAD_ID = "ghost_popup_thread_id"
        const val EXTRA_SUB_ID = "ghost_popup_sub_id"
        fun launch(
            context: Context,
            address: String,
            senderName: String,
            body: String,
            threadId: Long,
            subscriptionId: Int
        ) {
            val intent = Intent(context, GhostIncomingPopupActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                )
                putExtra(EXTRA_ADDRESS, address)
                putExtra(EXTRA_SENDER, senderName)
                putExtra(EXTRA_BODY, body)
                putExtra(EXTRA_THREAD_ID, threadId)
                putExtra(EXTRA_SUB_ID, subscriptionId)
            }
            runCatching { context.startActivity(intent) }
        }
    }
    private lateinit var rootScrim: FrameLayout
    private lateinit var popupCard: LinearLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.setDimAmount(0.45f)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        renderPopup(intent)
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        renderPopup(intent)
    }
    private fun renderPopup(intent: Intent) {
        val address = intent.getStringExtra(EXTRA_ADDRESS).orEmpty()
        val sender = intent.getStringExtra(EXTRA_SENDER).orEmpty().ifBlank { address }
        val body = intent.getStringExtra(EXTRA_BODY).orEmpty()
        val threadId = intent.getLongExtra(EXTRA_THREAD_ID, -1L)
        val subId = intent.getIntExtra(EXTRA_SUB_ID, -1)
        val density = resources.displayMetrics.density
        val darkTheme = GhostInboxStyler.isAppDarkTheme(this)
        val primaryColor = getProperPrimaryColor()
        val textColor = getProperTextColor()
        val cardFill = if (darkTheme) Color.parseColor("#161C26") else Color.parseColor("#F8FAFD")
        val bubbleFill = if (darkTheme) Color.parseColor("#202836") else Color.parseColor("#E9EFF7")
        val inputFill = if (darkTheme) Color.parseColor("#1E2532") else Color.parseColor("#EDF2F8")
        rootScrim = FrameLayout(this).apply {
            setOnClickListener { dismissSmoothly() }
        }
        popupCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            elevation = 18f * density
            setPadding((18 * density).toInt(), (16 * density).toInt(), (18 * density).toInt(), (16 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 28f * density
                setColor(cardFill)
                // Strictly zero borderlines
                setStroke(0, Color.TRANSPARENT)
            }
            setOnClickListener { /* consume inside card taps */ }
        }
        // Top Header Row: Monogram Avatar + Sender + Close Pill
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val avatarView = ImageView(this).apply {
            setImageDrawable(GhostInboxStyler.buildMonogramAvatar(this@GhostIncomingPopupActivity, sender, false))
        }
        headerRow.addView(avatarView, LinearLayout.LayoutParams((42 * density).toInt(), (42 * density).toInt()))
        val titleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((12 * density).toInt(), 0, (8 * density).toInt(), 0)
        }
        val senderText = TextView(this).apply {
            text = sender
            setTextColor(textColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(typeface, Typeface.BOLD)
            maxLines = 1
        }
        val badgeText = TextView(this).apply {
            text = if (sender != address) "$address • Ghost SMS" else "New Message • Ghost SMS"
            setTextColor(ColorUtils.setAlphaComponent(textColor, 165))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
            maxLines = 1
        }
        titleCol.addView(senderText)
        titleCol.addView(badgeText)
        headerRow.addView(titleCol, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val closeBtn = TextView(this).apply {
            text = "✕"
            gravity = Gravity.CENTER
            setTextColor(ColorUtils.setAlphaComponent(textColor, 200))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(bubbleFill)
                setStroke(0, Color.TRANSPARENT)
            }
            setOnClickListener { dismissSmoothly() }
        }
        headerRow.addView(closeBtn, LinearLayout.LayoutParams((32 * density).toInt(), (32 * density).toInt()))
        popupCard.addView(headerRow)
        // Message Body Tonal Bubble (Borderless M3)
        val bodyBubble = TextView(this).apply {
            text = GhostBubbleStyler.formatRichMessageText(this@GhostIncomingPopupActivity, body, false, primaryColor)
            setTextColor(textColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setLineSpacing(0f, 1.22f)
            setPadding((15 * density).toInt(), (12 * density).toInt(), (15 * density).toInt(), (12 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 20f * density
                setColor(bubbleFill)
                setStroke(0, Color.TRANSPARENT)
            }
        }
        val bodyLp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = (12 * density).toInt()
        }
        popupCard.addView(bodyBubble, bodyLp)
        // Smart Quick Actions Row (Copy OTP if present + Open Thread + Mark Read)
        val actionsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }
        val actionsLp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = (10 * density).toInt()
        }
        val otpCode = GhostOtpHelper.extractOtp(body)
        if (!otpCode.isNullOrBlank()) {
            val copyOtpChip = buildActionPill("Copy $otpCode", primaryColor, primaryColor.getContrastColor()) {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("OTP", otpCode))
                GhostHapticEngine.pulseOtpCopied(this)
                Toast.makeText(this, "Copied $otpCode", Toast.LENGTH_SHORT).show()
                dismissSmoothly()
            }
            actionsRow.addView(copyOtpChip, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginEnd = (8 * density).toInt()
            })
        }
        val openBtn = buildActionPill("Open Chat", bubbleFill, textColor) {
            if (threadId > 0L) {
                notificationManager.cancel(threadId.hashCode())
                val openIntent = Intent(this, ThreadActivity::class.java).apply {
                    putExtra(THREAD_ID, threadId)
                    putExtra(THREAD_TITLE, sender)
                }
                startActivity(openIntent)
            }
            finish()
        }
        actionsRow.addView(openBtn)
        popupCard.addView(actionsRow, actionsLp)
        // Floating Borderless Quick-Reply Capsule
        val replyRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((14 * density).toInt(), (4 * density).toInt(), (6 * density).toInt(), (4 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 26f * density
                setColor(inputFill)
                setStroke(0, Color.TRANSPARENT)
            }
        }
        val replyLp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = (10 * density).toInt()
        }
        val replyInput = EditText(this).apply {
            hint = "Quick reply..."
            setHintTextColor(ColorUtils.setAlphaComponent(textColor, 130))
            setTextColor(textColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f)
            background = null
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            imeOptions = EditorInfo.IME_ACTION_SEND
            maxLines = 3
        }
        replyRow.addView(replyInput, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val sendPill = TextView(this).apply {
            text = "Send"
            setTextColor(primaryColor.getContrastColor())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding((14 * density).toInt(), (8 * density).toInt(), (14 * density).toInt(), (8 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 20f * density
                setColor(primaryColor)
                setStroke(0, Color.TRANSPARENT)
            }
            setOnClickListener {
                val replyText = replyInput.text.toString().trim()
                if (replyText.isNotEmpty() && address.isNotBlank()) {
                    sendQuickReply(address, replyText, threadId, subId)
                }
            }
        }
        replyRow.addView(sendPill)
        popupCard.addView(replyRow, replyLp)
        val cardLp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            marginStart = (14 * density).toInt()
            marginEnd = (14 * density).toInt()
            topMargin = (28 * density).toInt()
        }
        rootScrim.addView(popupCard, cardLp)
        setContentView(rootScrim)
        GhostHapticEngine.pulseIncomingMessage(this)
        GhostMotionEngine.animatePopupCardEntrance(popupCard)
    }
    private fun buildActionPill(label: String, fillColor: Int, labelColor: Int, onClick: () -> Unit): TextView {
        val density = resources.displayMetrics.density
        return TextView(this).apply {
            text = label
            setTextColor(labelColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding((14 * density).toInt(), (7 * density).toInt(), (14 * density).toInt(), (7 * density).toInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 18f * density
                setColor(fillColor)
                setStroke(0, Color.TRANSPARENT)
            }
            setOnClickListener { onClick() }
        }
    }
    private fun sendQuickReply(address: String, text: String, threadId: Long, subId: Int) {
        GhostHapticEngine.pulseMessageSent(this)
        ensureBackgroundThread {
            runCatching {
                val smsManager = if (subId > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    getSystemService(SmsManager::class.java).createForSubscriptionId(subId)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
                val parts = smsManager.divideMessage(text)
                if (parts.size > 1) {
                    smsManager.sendMultipartTextMessage(address, null, parts, null, null)
                } else {
                    smsManager.sendTextMessage(address, null, text, null, null)
                }
                if (threadId > 0L) {
                    insertNewSMS(
                        address = address,
                        subject = "",
                        body = text,
                        date = System.currentTimeMillis(),
                        read = 1,
                        threadId = threadId,
                        type = android.provider.Telephony.Sms.MESSAGE_TYPE_SENT,
                        subscriptionId = subId
                    )
                    markThreadMessagesRead(threadId)
                    notificationManager.cancel(threadId.hashCode())
                }
                refreshMessages()
                refreshConversations()
            }
            runOnUiThread {
                Toast.makeText(this, "Sent", Toast.LENGTH_SHORT).show()
                dismissSmoothly()
            }
        }
    }
    private fun dismissSmoothly() {
        if (::popupCard.isInitialized) {
            GhostMotionEngine.animatePopupCardDismiss(popupCard) {
                finish()
                overridePendingTransition(0, 0)
            }
        } else {
            finish()
        }
    }
}