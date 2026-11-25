package org.fossify.messages.receivers
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.extensions.markThreadMessagesRead
import org.fossify.commons.extensions.notificationManager
import org.fossify.messages.extensions.updateConversationArchivedStatus
import org.fossify.messages.helpers.THREAD_ID
class CopyOtpReceiver : BroadcastReceiver() {
    companion object {
        const val EXTRA_OTP_CODE = "extra_otp_code"
    }
    override fun onReceive(context: Context, intent: Intent) {
        val otpCode = intent.getStringExtra(EXTRA_OTP_CODE) ?: return
        val threadId = intent.getLongExtra(THREAD_ID, -1L)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("OTP Code", otpCode))
        Toast.makeText(context, "Copied OTP: $otpCode", Toast.LENGTH_SHORT).show()
        if (threadId != -1L) {
            context.notificationManager.cancel(threadId.hashCode())
            ensureBackgroundThread {
                context.markThreadMessagesRead(threadId)
                context.updateConversationArchivedStatus(threadId, false)
            }
        }
    }
}