package org.fossify.messages.helpers
import android.content.Context
import android.text.InputType
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
object GhostVaultManager {
    private const val PREFS_NAME = "ghost_vault_prefs"
    private const val KEY_VAULTED_THREADS = "vaulted_thread_ids"
    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    fun getVaultedThreadIds(context: Context): Set<Long> {
        val raw = prefs(context).getStringSet(KEY_VAULTED_THREADS, emptySet()) ?: emptySet()
        return raw.mapNotNull { it.toLongOrNull() }.toSet()
    }
    fun isThreadVaulted(context: Context, threadId: Long): Boolean {
        return getVaultedThreadIds(context).contains(threadId)
    }
    fun setThreadVaulted(context: Context, threadId: Long, vaulted: Boolean) {
        val current = (prefs(context).getStringSet(KEY_VAULTED_THREADS, emptySet()) ?: emptySet()).toMutableSet()
        if (vaulted) {
            current.add(threadId.toString())
        } else {
            current.remove(threadId.toString())
        }
        prefs(context).edit().putStringSet(KEY_VAULTED_THREADS, current).apply()
    }
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Unlock Ghost Vault",
        subtitle: String = "Verify fingerprint, face, or device PIN",
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        val decoyPin = GhostPrivacyManager.getDecoyPin(activity)
        if (decoyPin != null) {
            val input = EditText(activity).apply {
                hint = "Enter Vault PIN or tap Biometric"
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
                setPadding(48, 36, 48, 36)
            }
            AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage("Enter PIN or use device biometrics")
                .setView(input)
                .setPositiveButton("Unlock") { _, _ ->
                    val entered = input.text.toString().trim()
                    if (entered == decoyPin) {
                        GhostPrivacyManager.setPanicModeActive(activity, true)
                        onSuccess()
                    } else {
                        launchSystemBiometric(activity, title, subtitle, {
                            GhostPrivacyManager.setPanicModeActive(activity, false)
                            onSuccess()
                        }, onError)
                    }
                }
                .setNeutralButton("Use Biometric") { _, _ ->
                    launchSystemBiometric(activity, title, subtitle, {
                        GhostPrivacyManager.setPanicModeActive(activity, false)
                        onSuccess()
                    }, onError)
                }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }
        launchSystemBiometric(activity, title, subtitle, onSuccess, onError)
    }
    private fun launchSystemBiometric(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val canAuth = BiometricManager.from(activity).canAuthenticate(authenticators)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            onSuccess()
            return
        }
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }
            }
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(authenticators)
            .build()
        prompt.authenticate(promptInfo)
    }
}