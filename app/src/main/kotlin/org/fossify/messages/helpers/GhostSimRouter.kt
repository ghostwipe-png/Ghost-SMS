package org.fossify.messages.helpers
import org.fossify.messages.models.SIMCard
import java.util.Locale
object GhostSimRouter {
    enum class Carrier { SAFARICOM, AIRTEL, TELKOM, EQUITEL, UNKNOWN }
    fun detectCarrierFromNumber(rawNumber: String): Carrier {
        val digits = rawNumber.filter { it.isDigit() }
        val local = when {
            digits.startsWith("254") && digits.length >= 12 -> "0" + digits.substring(3)
            digits.startsWith("0") && digits.length >= 10 -> digits
            digits.length == 9 -> "0$digits"
            else -> return Carrier.UNKNOWN
        }
        if (local.length < 4) return Carrier.UNKNOWN
        val p4 = local.substring(0, 4).toIntOrNull() ?: return Carrier.UNKNOWN
        return when (p4) {
            in 700..729, in 740..743, in 745..746, 748, in 757..759, in 768..769, in 790..799, in 110..115 -> Carrier.SAFARICOM
            in 730..739, in 750..756, 762, in 780..789, in 100..108 -> Carrier.AIRTEL
            in 770..779 -> Carrier.TELKOM
            in 763..766 -> Carrier.EQUITEL
            else -> Carrier.UNKNOWN
        }
    }
    fun findBestSimIndex(simCards: List<SIMCard>, recipientNumbers: List<String>): Int? {
        if (simCards.size < 2 || recipientNumbers.isEmpty()) return null
        val targetCarrier = recipientNumbers
            .map { detectCarrierFromNumber(it) }
            .firstOrNull { it != Carrier.UNKNOWN } ?: return null
        val matchIdx = simCards.indexOfFirst { card ->
            val label = card.label.lowercase(Locale.ROOT)
            when (targetCarrier) {
                Carrier.SAFARICOM -> label.contains("safaricom") || label.contains("saf")
                Carrier.AIRTEL -> label.contains("airtel")
                Carrier.TELKOM -> label.contains("telkom") || label.contains("orange")
                Carrier.EQUITEL -> label.contains("equitel")
                Carrier.UNKNOWN -> false
            }
        }
        return if (matchIdx >= 0) matchIdx else null
    }
}