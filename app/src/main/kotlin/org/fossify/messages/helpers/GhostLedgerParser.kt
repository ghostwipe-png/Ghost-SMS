package org.fossify.messages.helpers
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import org.fossify.messages.models.Message
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern
object GhostLedgerParser {
    enum class Direction { IN, OUT, INFO }
    data class LedgerEntry(
        val messageId: Long,
        val threadId: Long,
        val txCode: String,
        val direction: Direction,
        val amount: Double,
        val currency: String,
        val counterparty: String,
        val balance: Double?,
        val timestampSeconds: Int,
        val rawBody: String
    )
    data class LedgerSummary(
        val totalIn: Double,
        val totalOut: Double,
        val latestBalance: Double?,
        val currency: String,
        val entries: List<LedgerEntry>
    )
    private val TX_CODE_PATTERN = Pattern.compile("^([A-Z0-9]{10})\\s+Confirmed\\b", Pattern.CASE_INSENSITIVE)
    private val AMOUNT_PATTERN = Pattern.compile("(?i)\\b(Ksh|KES|USD|TZS|UGX)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)")
    private val BALANCE_PATTERN = Pattern.compile("(?i)(?:new\\s+(?:m-?pesa|airtel\\s+money|account|available)?\\s*balance\\s*(?:is|:)?|bal\\s*(?:is|:))\\s*(Ksh|KES|USD|TZS|UGX)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)")
    private val COUNTERPARTY_PATTERN = Pattern.compile("(?i)(?:sent to|paid to|received from|from|to|bought|withdraw(?:n)? from)\\s+([A-Z0-9 &'._-]{2,38}?)(?=\\s+(?:on|at|for|new|account|via|using|m-pesa|ref|till|paybill|\\d{1,2}/\\d{1,2})|[.,]|$)")
    fun parseMessages(messages: List<Message>): LedgerSummary {
        val entries = ArrayList<LedgerEntry>()
        val sorted = messages.sortedByDescending { it.date }
        for (msg in sorted) {
            val body = msg.body.trim()
            if (body.length < 18) continue
            val amountMatcher = AMOUNT_PATTERN.matcher(body)
            if (!amountMatcher.find()) continue
            val lower = body.lowercase(Locale.ROOT)
            val isFinancial = lower.contains("confirmed") ||
                lower.contains("m-pesa") ||
                lower.contains("mpesa") ||
                lower.contains("balance") ||
                lower.contains("debited") ||
                lower.contains("credited") ||
                lower.contains("airtel money") ||
                lower.contains("transaction")
            if (!isFinancial) continue
            val currency = amountMatcher.group(1)?.uppercase(Locale.ROOT) ?: "KSH"
            val amount = amountMatcher.group(2)?.replace(",", "")?.toDoubleOrNull() ?: continue
            if (amount <= 0.0) continue
            val txMatcher = TX_CODE_PATTERN.matcher(body)
            val txCode = if (txMatcher.find()) txMatcher.group(1)?.uppercase(Locale.ROOT) ?: "TX-${msg.id}" else "TX-${msg.id}"
            val direction = when {
                lower.contains("you have received") ||
                    lower.contains("received ksh") ||
                    lower.contains("received kes") ||
                    lower.contains("credited") ||
                    lower.contains("deposit") -> Direction.IN
                lower.contains("sent to") ||
                    lower.contains("paid to") ||
                    lower.contains("debited") ||
                    lower.contains("withdraw") ||
                    lower.contains("bought") ||
                    lower.contains("airtime") ||
                    lower.contains("fuliza") -> Direction.OUT
                else -> Direction.INFO
            }
            var balance: Double? = null
            val balMatcher = BALANCE_PATTERN.matcher(body)
            if (balMatcher.find()) {
                balance = balMatcher.group(2)?.replace(",", "")?.toDoubleOrNull()
            }
            val cpMatcher = COUNTERPARTY_PATTERN.matcher(body)
            val counterparty = if (cpMatcher.find()) {
                cpMatcher.group(1)?.trim()?.take(32) ?: msg.senderName.ifEmpty { "Mobile Money" }
            } else {
                msg.senderName.ifEmpty { "Mobile Money" }
            }
            entries.add(
                LedgerEntry(
                    messageId = msg.id,
                    threadId = msg.threadId,
                    txCode = txCode,
                    direction = direction,
                    amount = amount,
                    currency = currency,
                    counterparty = counterparty,
                    balance = balance,
                    timestampSeconds = msg.date,
                    rawBody = body
                )
            )
        }
        val thirtyDaysAgo = (System.currentTimeMillis() / 1000L) - (30L * 24L * 3600L)
        val recentEntries = entries.filter { it.timestampSeconds >= thirtyDaysAgo }
        val activePool = if (recentEntries.isNotEmpty()) recentEntries else entries
        val totalIn = activePool.filter { it.direction == Direction.IN }.sumOf { it.amount }
        val totalOut = activePool.filter { it.direction == Direction.OUT }.sumOf { it.amount }
        val latestBalance = entries.firstOrNull { it.balance != null }?.balance
        val primaryCurrency = entries.firstOrNull()?.currency ?: "KSH"
        return LedgerSummary(
            totalIn = totalIn,
            totalOut = totalOut,
            latestBalance = latestBalance,
            currency = primaryCurrency,
            entries = entries.take(60)
        )
    }
    fun formatMoney(amount: Double, currency: String = "KSH"): String {
        val nf = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        return "$currency ${nf.format(amount)}"
    }
    fun exportToCsv(context: Context, summary: LedgerSummary): String? {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            val sb = StringBuilder()
            sb.append("Date,TxCode,Direction,Currency,Amount,Counterparty,Balance\n")
            for (e in summary.entries) {
                val dateStr = sdf.format(Date(e.timestampSeconds * 1000L))
                val safeCp = e.counterparty.replace("\"", "\"\"")
                val balStr = e.balance?.toString() ?: ""
                sb.append("\"$dateStr\",\"${e.txCode}\",\"${e.direction}\",\"${e.currency}\",${e.amount},\"$safeCp\",$balStr\n")
            }
            val fileName = "GhostLedger_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.csv"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(sb.toString().toByteArray(Charsets.UTF_8))
                    }
                    "Downloads/$fileName"
                } else null
            } else {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, fileName)
                file.writeText(sb.toString(), Charsets.UTF_8)
                file.absolutePath
            }
        } catch (_: Exception) {
            null
        }
    }
}