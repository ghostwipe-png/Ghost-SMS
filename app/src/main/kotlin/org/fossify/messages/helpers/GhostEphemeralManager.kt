package org.fossify.messages.helpers
import android.content.Context
import org.fossify.messages.extensions.deleteMessage
import org.fossify.messages.extensions.messagesDB
object GhostEphemeralManager {
    private const val PREFS_NAME = "ghost_ephemeral_prefs"
    private const val PREFIX_TTL = "ttl_seconds_"
    val TIMER_OPTIONS = listOf(
        0L to "Off",
        3600L to "1 Hour",
        86400L to "24 Hours",
        604800L to "7 Days"
    )
    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    fun getThreadTtlSeconds(context: Context, threadId: Long): Long {
        if (threadId <= 0L) return 0L
        return prefs(context).getLong("$PREFIX_TTL$threadId", 0L)
    }
    fun setThreadTtlSeconds(context: Context, threadId: Long, ttlSeconds: Long) {
        if (threadId <= 0L) return
        prefs(context).edit().putLong("$PREFIX_TTL$threadId", ttlSeconds).apply()
    }
    fun formatTtlLabel(ttlSeconds: Long): String {
        return TIMER_OPTIONS.firstOrNull { it.first == ttlSeconds }?.second ?: "Off"
    }
    fun purgeExpiredMessages(context: Context, threadId: Long): Int {
        if (threadId <= 0L) return 0
        val ttlSeconds = getThreadTtlSeconds(context, threadId)
        if (ttlSeconds <= 0L) return 0
        val nowSeconds = System.currentTimeMillis() / 1000L
        val cutoffSeconds = nowSeconds - ttlSeconds
        val threadMessages = try {
            context.messagesDB.getThreadMessages(threadId)
        } catch (_: Exception) {
            return 0
        }
        var purged = 0
        for (msg in threadMessages) {
            val msgSeconds = if (msg.date > 10_000_000_000L) msg.date / 1000L else msg.date.toLong()
            if (!msg.isScheduled && msgSeconds in 1L until cutoffSeconds) {
                try {
                    context.deleteMessage(msg.id, msg.isMMS)
                    purged++
                } catch (_: Exception) {
                }
            }
        }
        return purged
    }
    fun purgeAllThreads(context: Context): Int {
        val allKeys = prefs(context).all.keys.filter { it.startsWith(PREFIX_TTL) }
        var totalPurged = 0
        for (key in allKeys) {
            val threadId = key.removePrefix(PREFIX_TTL).toLongOrNull() ?: continue
            totalPurged += purgeExpiredMessages(context, threadId)
        }
        return totalPurged
    }
}