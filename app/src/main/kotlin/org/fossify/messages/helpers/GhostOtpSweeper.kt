package org.fossify.messages.helpers
import android.content.Context
import org.fossify.messages.extensions.messagesDB
import java.util.Locale
object GhostOtpSweeper {
    private const val PREFS_NAME = "ghost_hygiene_prefs"
    private const val KEY_AUTO_SWEEP_OTP = "auto_sweep_expired_otps"
    private const val TWENTY_FOUR_HOURS_SEC = 24L * 3600L
    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    fun isAutoSweepEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO_SWEEP_OTP, false)
    fun setAutoSweepEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUTO_SWEEP_OTP, enabled).apply()
    }
    fun sweepExpiredOtps(context: Context): Int {
        val cutoffSeconds = (System.currentTimeMillis() / 1000L) - TWENTY_FOUR_HOURS_SEC
        val vaultedIds = GhostVaultManager.getVaultedThreadIds(context)
        val allMessages = try {
            context.messagesDB.getAll()
        } catch (_: Exception) {
            return 0
        }
        var swept = 0
        for (msg in allMessages) {
            if (vaultedIds.contains(msg.threadId)) continue
            if (msg.date >= cutoffSeconds) continue
            if (!msg.isReceivedMessage()) continue
            val lower = msg.body.lowercase(Locale.ROOT)
            val isFinancialReceipt = lower.contains("confirmed") &&
                (lower.contains("balance") || lower.contains("mpesa") || lower.contains("m-pesa") || lower.contains("credited") || lower.contains("debited"))
            if (isFinancialReceipt) continue
            val isOtpMessage = GhostOtpHelper.extractOtp(msg.body) != null &&
                (lower.contains("otp") || lower.contains("code") || lower.contains("verification") || lower.contains("verify") || lower.contains("2fa") || lower.contains("pin"))
            if (isOtpMessage) {
                try {
                    context.messagesDB.delete(msg.id)
                    swept++
                } catch (_: Exception) {
                }
            }
        }
        return swept
    }
}