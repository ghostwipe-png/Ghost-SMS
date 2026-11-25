package org.fossify.messages.activities
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.fossify.commons.activities.BaseSimpleActivity
import org.fossify.messages.R
open class SimpleActivity : BaseSimpleActivity() {
    override fun getAppIconIDs() = arrayListOf(
        R.mipmap.ic_launcher
    )
    override fun getAppLauncherName() = getString(R.string.app_name)
    override fun getRepositoryName() = "Ghost-SMS"
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus) {
            dismissUpstreamPackageWarning()
        }
    }
    private fun dismissUpstreamPackageWarning() {
        window?.decorView?.post {
            runCatching {
                val wmGlobalClass = Class.forName("android.view.WindowManagerGlobal")
                val getInstance = wmGlobalClass.getMethod("getInstance")
                val wmGlobal = getInstance.invoke(null)
                val viewsField = wmGlobalClass.getDeclaredField("mViews").apply { isAccessible = true }
                val views = viewsField.get(wmGlobal) as? List<*> ?: return@runCatching
                for (root in views) {
                    val view = root as? View ?: continue
                    val ctx = view.context
                    if (ctx == this || (ctx as? android.content.ContextWrapper)?.baseContext == this) {
                        if (containsText(view, "fake version")) {
                            view.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }
    private fun containsText(view: View, target: String): Boolean {
        if (view is TextView && view.text?.toString()?.contains(target, ignoreCase = true) == true) {
            return true
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                if (containsText(view.getChildAt(i), target)) return true
            }
        }
        return false
    }
}