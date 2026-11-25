package org.fossify.messages.helpers
import java.util.regex.Pattern
object GhostOtpHelper {
    private val OTP_KEYWORD_REGEX = Pattern.compile(
        "(?i)\\b(?:otp|code|pin|verification|verify|passcode|security|token|auth|2fa|mfa|login|one[- ]time|secret)\\b"
    )
    private val CODE_BEFORE_KEYWORD = Pattern.compile(
        "(?i)\\b([A-Z0-9]{4,8})\\b(?=[^\\n]{0,35}\\b(?:is your|verification|otp|code|pin|passcode|token)\\b)"
    )
    private val CODE_AFTER_KEYWORD = Pattern.compile(
        "(?i)\\b(?:otp|code|pin|verification|passcode|token|2fa)\\b(?:\\s+(?:is|code|number|:|-|=))*\\s*[:\\-=#]?\\s*\\b([0-9]{4,8})\\b"
    )
    private val STANDALONE_DIGITS = Pattern.compile("(?<![\\d.,+\\-/])\\b([0-9]{4,8})\\b(?![\\d.,])")
    fun extractOtp(body: String): String? {
        if (body.isBlank() || !OTP_KEYWORD_REGEX.matcher(body).find()) return null
        val afterMatcher = CODE_AFTER_KEYWORD.matcher(body)
        if (afterMatcher.find()) {
            return afterMatcher.group(1)
        }
        val beforeMatcher = CODE_BEFORE_KEYWORD.matcher(body)
        if (beforeMatcher.find()) {
            val candidate = beforeMatcher.group(1) ?: return null
            if (candidate.any { it.isDigit() }) return candidate
        }
        val digitMatcher = STANDALONE_DIGITS.matcher(body)
        while (digitMatcher.find()) {
            val candidate = digitMatcher.group(1) ?: continue
            // Avoid matching 4-digit years like 2025/2026 unless no other code exists
            if (candidate.length == 4 && (candidate.startsWith("19") || candidate.startsWith("20"))) {
                continue
            }
            return candidate
        }
        return null
    }
}