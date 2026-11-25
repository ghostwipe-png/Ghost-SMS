package org.fossify.messages.helpers
import android.app.Activity
import android.content.Context
import android.view.WindowManager
object GhostPrivacyManager {
    private const val PREFS_NAME = "ghost_privacy_prefs"
    private const val KEY_STEALTH_SCREEN = "stealth_flag_secure"
    private const val KEY_LINK_GUARD = "phishing_link_guard"
    private const val KEY_DECOY_PIN = "decoy_vault_pin"
    private const val KEY_PANIC_ACTIVE = "panic_mode_active"
    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    fun isStealthScreenEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_STEALTH_SCREEN, true)
    fun setStealthScreenEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_STEALTH_SCREEN, enabled).apply()
    }
    fun applyWindowSecurity(activity: Activity) {
        if (isStealthScreenEnabled(activity)) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
    fun isLinkGuardEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_LINK_GUARD, true)
    fun setLinkGuardEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_LINK_GUARD, enabled).apply()
    }
    fun getDecoyPin(context: Context): String? =
        prefs(context).getString(KEY_DECOY_PIN, null)?.takeIf { it.isNotEmpty() }
    fun setDecoyPin(context: Context, pin: String?) {
        prefs(context).edit().putString(KEY_DECOY_PIN, pin?.trim().orEmpty()).apply()
    }
    fun isPanicModeActive(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PANIC_ACTIVE, false)
    fun setPanicModeActive(context: Context, active: Boolean) {
        prefs(context).edit().putBoolean(KEY_PANIC_ACTIVE, active).apply()
    }
}