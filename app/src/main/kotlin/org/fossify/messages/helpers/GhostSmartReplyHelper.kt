package org.fossify.messages.helpers
import java.util.Locale
object GhostSmartReplyHelper {
    fun suggestReplies(latestIncomingBody: String): List<String> {
        val text = latestIncomingBody.trim()
        if (text.isEmpty()) return emptyList()
        val lower = text.lowercase(Locale.ROOT)
        if (GhostOtpHelper.extractOtp(text) != null || lower.contains("do not reply")) {
            return emptyList()
        }
        return when {
            lower.contains("where are you") || lower.contains("wya") || lower.contains("uko wapi") || lower.contains("coming") || lower.contains("arrive") ->
                listOf("On my way now", "Be there in 10 mins", "Just arrived")
            lower.contains("mpesa") || lower.contains("m-pesa") || lower.contains("till") || lower.contains("paybill") || lower.contains("send") || lower.contains("account") ->
                listOf("Confirmed, thanks!", "Sending shortly", "Please share Till/Number")
            lower.contains("call") || lower.contains("busy") || lower.contains("free") || lower.contains("talk") ->
                listOf("Call you shortly", "Free to talk now", "In a meeting, text me")
            lower.contains("thank") || lower.contains("asante") || lower.contains("received") ->
                listOf("You're welcome!", "Anytime 👍", "No problem at all")
            text.endsWith("?") || lower.startsWith("can ") || lower.startsWith("are ") || lower.startsWith("will ") || lower.startsWith("do ") ->
                listOf("Yes, sounds good", "Let me check and revert", "Not right now")
            else ->
                listOf("Got it, thanks", "Okay 👍", "Will get back to you")
        }
    }
}