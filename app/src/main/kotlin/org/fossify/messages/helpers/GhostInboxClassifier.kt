package org.fossify.messages.helpers
import org.fossify.messages.models.Conversation
import java.util.regex.Pattern
object GhostInboxClassifier {
    enum class Category(val label: String) {
        ALL("All"),
        PERSONAL("Personal"),
        FINANCE_OTP("OTPs & Finance"),
        PROMOTIONS("Promotions")
    }
    private val FINANCE_OTP_REGEX = Pattern.compile(
        "(?i)\\b(?:otp|verification|verify|passcode|pin|2fa|code|mpesa|m-pesa|confirmed|debited|credited|account|balance|bank|equity|kcb|coop|ncba|absa|stanbic|airtel money|t-kash|transaction|receipt|ksh|kes|usd|eur|gbp|inr|stmt|invoice|payment|withdraw|deposit|fuliza|mshwari)\\b"
    )
    private val PROMO_REGEX = Pattern.compile(
        "(?i)\\b(?:offer|promo|discount|sale|deal|win|bonus|bundle|data|airtime|free|subscribe|unsubscribe|stop|reply stop|cashback|voucher|coupon|limited time|exclusive|shop now|order now|loan limit|pre-approved)\\b"
    )
    fun classify(conversation: Conversation): Category {
        val snippet = conversation.snippet
        val sender = conversation.phoneNumber.trim()
        val isShortCodeOrAlpha = sender.any { it.isLetter() } || (sender.length in 3..7 && !sender.startsWith("+"))
        if (FINANCE_OTP_REGEX.matcher(snippet).find() || GhostOtpHelper.extractOtp(snippet) != null) {
            return Category.FINANCE_OTP
        }
        if (PROMO_REGEX.matcher(snippet).find()) {
            return Category.PROMOTIONS
        }
        if (isShortCodeOrAlpha && conversation.title == sender) {
            return Category.PROMOTIONS
        }
        return Category.PERSONAL
    }
}