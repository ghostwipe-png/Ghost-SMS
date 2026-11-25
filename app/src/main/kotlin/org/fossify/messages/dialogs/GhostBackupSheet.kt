package org.fossify.messages.dialogs
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.FileProvider
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.commons.extensions.adjustAlpha
import org.fossify.commons.extensions.getContrastColor
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.toast
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.helpers.GhostEncryptedBackupManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
class GhostBackupSheet(
    private val activity: BaseSimpleActivity,
    private val onRestored: () -> Unit
) {
    fun show() {
        val dialog = BottomSheetDialog(activity)
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        val bgColor = activity.getProperBackgroundColor()
        val primaryColor = activity.getProperPrimaryColor()
        val textColor = activity.getProperTextColor()
        val density = activity.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()
        val root = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(24))
            background = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadii = floatArrayOf(dp(22).toFloat(), dp(22).toFloat(), dp(22).toFloat(), dp(22).toFloat(), 0f, 0f, 0f, 0f)
            }
        }
        root.addView(TextView(activity).apply {
            text = "Encrypted Vault Backup (.ghost)"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f)
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(textColor)
        })
        root.addView(TextView(activity).apply {
            text = "AES-256-GCM + PBKDF2 offline archive for conversations & Ghost Vault locks"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(textColor.adjustAlpha(0.68f))
            setPadding(0, dp(2), 0, dp(14))
        })
        val archiveList = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        fun refreshArchives() {
            archiveList.removeAllViews()
            val files = GhostEncryptedBackupManager.listBackupFiles(activity)
            val fmt = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US)
            if (files.isEmpty()) {
                archiveList.addView(TextView(activity).apply {
                    text = "No .ghost archives saved yet. Tap below to export your first encrypted archive."
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    setTextColor(textColor.adjustAlpha(0.68f))
                    gravity = Gravity.CENTER
                    setPadding(dp(12), dp(20), dp(12), dp(20))
                })
                return
            }
            for (file in files) {
                val kb = (file.length() / 1024L).coerceAtLeast(1L)
                val row = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(14), dp(12), dp(14), dp(12))
                    background = GradientDrawable().apply {
                        setColor(textColor.adjustAlpha(0.06f))
                        cornerRadius = dp(12).toFloat()
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = dp(8) }
                    setOnClickListener { promptRestore(file, dialog) }
                }
                val col = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                col.addView(TextView(activity).apply {
                    text = file.name
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f)
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(textColor)
                })
                col.addView(TextView(activity).apply {
                    text = "${fmt.format(Date(file.lastModified()))} • ${kb} KB • Tap to decrypt & restore"
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 11.5f)
                    setTextColor(textColor.adjustAlpha(0.65f))
                })
                row.addView(col)
                row.addView(TextView(activity).apply {
                    text = "Share"
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(primaryColor)
                    setPadding(dp(10), dp(6), dp(10), dp(6))
                    setOnClickListener { shareArchive(file) }
                })
                archiveList.addView(row)
            }
        }
        val exportBtn = TextView(activity).apply {
            text = "Create AES-256 .ghost Backup Now"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setTextColor(primaryColor.getContrastColor())
            setPadding(dp(16), dp(13), dp(16), dp(13))
            background = GradientDrawable().apply {
                setColor(primaryColor)
                cornerRadius = dp(14).toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(14) }
            setOnClickListener {
                promptCreateBackup { refreshArchives() }
            }
        }
        root.addView(exportBtn)
        val scroll = ScrollView(activity).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(240))
        }
        scroll.addView(archiveList)
        root.addView(scroll)
        refreshArchives()
        dialog.setContentView(root)
        dialog.show()
    }
    private fun promptCreateBackup(onDone: () -> Unit) {
        val input = EditText(activity).apply {
            hint = "Encryption passphrase (min 6 chars)"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        AlertDialog.Builder(activity)
            .setTitle("Encrypt .ghost Archive")
            .setMessage("Choose a strong passphrase. Without it, this archive cannot be decrypted.")
            .setView(input)
            .setPositiveButton("Encrypt & Save") { _, _ ->
                val pass = input.text?.toString().orEmpty()
                if (pass.length < 6) {
                    activity.toast("Passphrase must be at least 6 characters")
                    return@setPositiveButton
                }
                activity.toast("Encrypting archive...")
                ensureBackgroundThread {
                    try {
                        val summary = GhostEncryptedBackupManager.createEncryptedBackup(activity, pass)
                        activity.runOnUiThread {
                            activity.toast("Saved ${summary.file.name} (${summary.messageCount} msgs, ${summary.vaultedThreadCount} vaulted)")
                            onDone()
                        }
                    } catch (e: Exception) {
                        activity.runOnUiThread {
                            activity.toast("Backup failed: ${e.message}")
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun promptRestore(file: File, sheetDialog: BottomSheetDialog) {
        val input = EditText(activity).apply {
            hint = "Enter archive passphrase"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        AlertDialog.Builder(activity)
            .setTitle("Decrypt ${file.name}")
            .setView(input)
            .setPositiveButton("Decrypt & Restore") { _, _ ->
                val pass = input.text?.toString().orEmpty()
                ensureBackgroundThread {
                    try {
                        val res = GhostEncryptedBackupManager.restoreEncryptedBackup(activity, file, pass)
                        activity.runOnUiThread {
                            activity.toast("Verified & restored ${res.vaultedRestored} vault locks (${res.messageCount} msgs in archive)")
                            onRestored()
                            sheetDialog.dismiss()
                        }
                    } catch (_: Exception) {
                        activity.runOnUiThread {
                            activity.toast("Decryption failed: wrong passphrase or corrupted file")
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun shareArchive(file: File) {
        try {
            val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            activity.startActivity(Intent.createChooser(intent, "Share Encrypted .ghost Backup"))
        } catch (e: Exception) {
            activity.toast("Saved at: ${file.absolutePath}")
        }
    }
}