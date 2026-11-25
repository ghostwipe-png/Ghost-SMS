package org.fossify.messages.helpers
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.SpannableString
import android.text.Spanned
import android.text.style.URLSpan
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import org.fossify.commons.extensions.toast
import java.util.Locale
object GhostLinkGuard {
    private val BRAND_KEYWORDS = listOf(
        "mpesa", "m-pesa", "safaricom", "equity", "kcb", "coop", "co-op",
        "ncba", "absa", "stanbic", "airtel", "tkash", "fuliza", "mshwari",
        "paypal", "whatsapp", "google", "kra", "ecitizen", "helb", "nhif", "sha"
    )
    private val OFFICIAL_DOMAINS = setOf(
        "safaricom.co.ke", "www.safaricom.co.ke",
        "equitygroupholdings.com", "www.equitygroupholdings.com",
        "kcbgroup.com", "www.kcbgroup.com",
        "co-opbank.co.ke", "www.co-opbank.co.ke",
        "ncbagroup.com", "www.ncbagroup.com",
        "absabank.co.ke", "www.absabank.co.ke",
        "airtelkenya.com", "www.airtelkenya.com",
        "kra.go.ke", "itax.kra.go.ke",
        "ecitizen.go.ke", "accounts.ecitizen.go.ke",
        "google.com", "www.google.com",
        "whatsapp.com", "www.whatsapp.com",
        "paypal.com", "www.paypal.com"
    )
    private val SHORTENERS = setOf(
        "bit.ly", "tinyurl.com", "t.co", "cutt.ly", "rb.gy", "is.gd", "ow.ly", "shorturl.at", "tiny.cc"
    )
    private val IP_REGEX = Regex("^\\d{1,3}(\\.\\d{1,3}){3}$")
    data class RiskAssessment(
        val isSuspicious: Boolean,
        val host: String,
        val reasons: List<String>
    )
    fun inspectUrl(rawUrl: String): RiskAssessment {
        val uri = try { Uri.parse(rawUrl) } catch (_: Exception) { null }
        val host = (uri?.host ?: "").lowercase(Locale.ROOT)
        val scheme = (uri?.scheme ?: "").lowercase(Locale.ROOT)
        val reasons = ArrayList<String>()
        if (host.isEmpty()) {
            return RiskAssessment(true, rawUrl, listOf("Malformed or hidden destination host"))
        }
        if (scheme == "http") {
            reasons.add("Unencrypted HTTP connection (no TLS lock)")
        }
        if (IP_REGEX.matches(host)) {
            reasons.add("Direct raw IP address instead of a verified domain")
        }
        if (SHORTENERS.contains(host)) {
            reasons.add("Shortened redirect link masking the true destination")
        }
        if (host.startsWith("xn--") || host.contains(".xn--")) {
            reasons.add("Punycode lookalike characters detected in domain")
        }
        val matchedBrand = BRAND_KEYWORDS.firstOrNull { host.contains(it) }
        val isOfficial = OFFICIAL_DOMAINS.any { host == it || host.endsWith(".$it") }
        if (matchedBrand != null && !isOfficial) {
            reasons.add("Mentions \"$matchedBrand\" on an unverified domain ($host)")
        }
        return RiskAssessment(reasons.isNotEmpty(), host, reasons)
    }
    fun attachToTextView(activity: Activity, textView: TextView) {
        val charSeq = textView.text
        if (charSeq !is Spanned) return
        val spans = charSeq.getSpans(0, charSeq.length, URLSpan::class.java)
        if (spans.isEmpty()) return
        val spannable = SpannableString(charSeq)
        for (span in spans) {
            val start = spannable.getSpanStart(span)
            val end = spannable.getSpanEnd(span)
            val flags = spannable.getSpanFlags(span)
            val url = span.url
            spannable.removeSpan(span)
            spannable.setSpan(
                object : URLSpan(url) {
                    override fun onClick(widget: View) {
                        handleLinkClick(activity, url)
                    }
                },
                start,
                end,
                flags
            )
        }
        textView.text = spannable
    }
    fun handleLinkClick(activity: Activity, url: String) {
        if (!GhostPrivacyManager.isLinkGuardEnabled(activity)) {
            openExternally(activity, url)
            return
        }
        val risk = inspectUrl(url)
        if (!risk.isSuspicious) {
            openExternally(activity, url)
            return
        }
        val details = buildString {
            append("Destination Domain:\n${risk.host}\n\n")
            append("Why Ghost Link Shield paused this link:\n")
            risk.reasons.forEach { append("• $it\n") }
            append("\nFull URL:\n$url")
        }
        AlertDialog.Builder(activity)
            .setTitle("Ghost Link Shield Warning")
            .setMessage(details)
            .setPositiveButton("Block & Close", null)
            .setNeutralButton("Copy URL") { _, _ ->
                val cm = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Suspicious URL", url))
                activity.toast("URL copied to clipboard")
            }
            .setNegativeButton("Open Anyway") { _, _ ->
                openExternally(activity, url)
            }
            .show()
    }
    private fun openExternally(activity: Activity, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            activity.startActivity(intent)
        } catch (_: Exception) {
            activity.toast("No app found to open link")
        }
    }
}