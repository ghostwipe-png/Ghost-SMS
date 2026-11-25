package org.fossify.messages.helpers
import android.content.Context
import android.os.Environment
import android.util.Base64
import org.fossify.messages.extensions.conversationsDB
import org.fossify.messages.extensions.messagesDB
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
object GhostEncryptedBackupManager {
    private const val MAGIC_HEADER = "GHOST_SMS_V1"
    private const val PBKDF2_ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val GCM_TAG_BITS = 128
    data class BackupSummary(
        val file: File,
        val conversationCount: Int,
        val messageCount: Int,
        val vaultedThreadCount: Int
    )
    data class RestoreSummary(
        val conversationCount: Int,
        val messageCount: Int,
        val vaultedRestored: Int
    )
    fun getBackupDirectory(context: Context): File {
        val docs = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val dir = File(docs, "GhostSMS")
        if (!dir.exists() && !dir.mkdirs()) {
            return File(context.filesDir, "ghost_backups").apply { mkdirs() }
        }
        return dir
    }
    fun listBackupFiles(context: Context): List<File> {
        val primary = getBackupDirectory(context).listFiles { f -> f.isFile && f.name.endsWith(".ghost") }?.toList().orEmpty()
        val internal = File(context.filesDir, "ghost_backups").listFiles { f -> f.isFile && f.name.endsWith(".ghost") }?.toList().orEmpty()
        return (primary + internal).distinctBy { it.absolutePath }.sortedByDescending { it.lastModified() }
    }
    fun createEncryptedBackup(context: Context, passphrase: String): BackupSummary {
        require(passphrase.length >= 6) { "Passphrase must be at least 6 characters" }
        val conversations = context.conversationsDB.getNonArchived() + context.conversationsDB.getAllArchived()
        val messages = context.messagesDB.getAll()
        val vaultedIds = GhostVaultManager.getVaultedThreadIds(context)
        val rootJson = JSONObject().apply {
            put("app", "Ghost SMS")
            put("version", 1)
            put("exportedAt", System.currentTimeMillis())
            put("vaultedThreadIds", JSONArray(vaultedIds.toList()))
            val convArray = JSONArray()
            for (c in conversations) {
                convArray.put(JSONObject().apply {
                    put("threadId", c.threadId)
                    put("snippet", c.snippet)
                    put("date", c.date)
                    put("read", c.read)
                    put("title", c.title)
                    put("photoUri", c.photoUri)
                    put("isGroupConversation", c.isGroupConversation)
                    put("phoneNumber", c.phoneNumber)
                    put("isArchived", c.isArchived)
                })
            }
            put("conversations", convArray)
            val msgArray = JSONArray()
            for (m in messages) {
                msgArray.put(JSONObject().apply {
                    put("id", m.id)
                    put("body", m.body)
                    put("type", m.type)
                    put("status", m.status)
                    put("date", m.date)
                    put("read", m.read)
                    put("threadId", m.threadId)
                    put("isMMS", m.isMMS)
                    put("subscriptionId", m.subscriptionId)
                    put("senderName", m.senderName)
                    put("senderPhoneNumber", m.senderPhoneNumber)
                })
            }
            put("messages", msgArray)
        }
        val plaintext = rootJson.toString().toByteArray(StandardCharsets.UTF_8)
        val rng = SecureRandom()
        val salt = ByteArray(SALT_BYTES).also { rng.nextBytes(it) }
        val iv = ByteArray(IV_BYTES).also { rng.nextBytes(it) }
        val key = deriveKey(passphrase, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext)
        val envelope = JSONObject().apply {
            put("magic", MAGIC_HEADER)
            put("kdf", "PBKDF2WithHmacSHA256")
            put("iter", PBKDF2_ITERATIONS)
            put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
            put("payload", Base64.encodeToString(ciphertext, Base64.NO_WRAP))
        }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val outFile = File(getBackupDirectory(context), "ghost_vault_$stamp.ghost")
        outFile.writeText(envelope.toString(), StandardCharsets.UTF_8)
        return BackupSummary(
            file = outFile,
            conversationCount = conversations.size,
            messageCount = messages.size,
            vaultedThreadCount = vaultedIds.size
        )
    }
    fun restoreEncryptedBackup(context: Context, file: File, passphrase: String): RestoreSummary {
        val raw = file.readText(StandardCharsets.UTF_8)
        val envelope = JSONObject(raw)
        require(envelope.optString("magic") == MAGIC_HEADER) { "Invalid .ghost archive header" }
        val iter = envelope.optInt("iter", PBKDF2_ITERATIONS)
        val salt = Base64.decode(envelope.getString("salt"), Base64.NO_WRAP)
        val iv = Base64.decode(envelope.getString("iv"), Base64.NO_WRAP)
        val ciphertext = Base64.decode(envelope.getString("payload"), Base64.NO_WRAP)
        val key = deriveKey(passphrase, salt, iter)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val plaintext = cipher.doFinal(ciphertext)
        val rootJson = JSONObject(String(plaintext, StandardCharsets.UTF_8))
        val vaultedArr = rootJson.optJSONArray("vaultedThreadIds") ?: JSONArray()
        var vaultedRestored = 0
        for (i in 0 until vaultedArr.length()) {
            val tid = vaultedArr.optLong(i, 0L)
            if (tid != 0L && !GhostVaultManager.isThreadVaulted(context, tid)) {
                GhostVaultManager.setThreadVaulted(context, tid, true)
                vaultedRestored++
            }
        }
        val convCount = rootJson.optJSONArray("conversations")?.length() ?: 0
        val msgCount = rootJson.optJSONArray("messages")?.length() ?: 0
        return RestoreSummary(
            conversationCount = convCount,
            messageCount = msgCount,
            vaultedRestored = vaultedRestored
        )
    }
    private fun deriveKey(passphrase: String, salt: ByteArray, iterations: Int = PBKDF2_ITERATIONS): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, iterations, KEY_LENGTH_BITS)
        val bytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(bytes, "AES")
    }
}