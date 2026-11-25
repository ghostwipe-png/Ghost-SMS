package org.fossify.messages.helpers
import android.content.Context
import android.graphics.Color
import org.fossify.commons.extensions.checkAppIconColor
import org.fossify.commons.models.RadioItem
import org.fossify.messages.extensions.config
object GhostThemeManager {
    data class ThemePreset(
        val id: Int,
        val name: String,
        val textColor: Int,
        val backgroundColor: Int,
        val primaryColor: Int,
        val accentColor: Int,
        val appIconColor: Int
    )
    val presets = listOf(
        ThemePreset(
            id = 1,
            name = "Midnight Ghost (Obsidian & Crimson)",
            textColor = Color.parseColor("#F2F4F8"),
            backgroundColor = Color.parseColor("#121318"),
            primaryColor = Color.parseColor("#B71C1C"),
            accentColor = Color.parseColor("#FF5252"),
            appIconColor = Color.parseColor("#212121")
        ),
        ThemePreset(
            id = 2,
            name = "Executive Slate (Charcoal & Steel)",
            textColor = Color.parseColor("#ECEFF4"),
            backgroundColor = Color.parseColor("#181C22"),
            primaryColor = Color.parseColor("#37474F"),
            accentColor = Color.parseColor("#64B5F6"),
            appIconColor = Color.parseColor("#455A64")
        ),
        ThemePreset(
            id = 3,
            name = "Nordic Frost (Crisp Light & Navy)",
            textColor = Color.parseColor("#0F172A"),
            backgroundColor = Color.parseColor("#F8FAFC"),
            primaryColor = Color.parseColor("#1E3A8A"),
            accentColor = Color.parseColor("#2563EB"),
            appIconColor = Color.parseColor("#303F9F")
        ),
        ThemePreset(
            id = 4,
            name = "Emerald Noir (Deep Forest & Mint)",
            textColor = Color.parseColor("#E6F4EA"),
            backgroundColor = Color.parseColor("#0E1714"),
            primaryColor = Color.parseColor("#1B5E20"),
            accentColor = Color.parseColor("#00E676"),
            appIconColor = Color.parseColor("#388E3C")
        ),
        ThemePreset(
            id = 5,
            name = "Royal Amethyst (Velvet & Gold)",
            textColor = Color.parseColor("#F3E8FF"),
            backgroundColor = Color.parseColor("#161224"),
            primaryColor = Color.parseColor("#4A148C"),
            accentColor = Color.parseColor("#FFD54F"),
            appIconColor = Color.parseColor("#512DA8")
        ),
        ThemePreset(
            id = 6,
            name = "Thick WhatsApp Emerald (Dark & Bold)",
            textColor = Color.parseColor("#E9EDEF"),
            backgroundColor = Color.parseColor("#0B141A"),
            primaryColor = Color.parseColor("#005C4B"),
            accentColor = Color.parseColor("#25D366"),
            appIconColor = Color.parseColor("#388E3C")
        )
    )
    fun getRadioItems(): ArrayList<RadioItem> {
        return ArrayList(presets.map { RadioItem(it.id, it.name) })
    }
    fun getCurrentThemeId(context: Context): Int {
        val cfg = context.config
        return presets.firstOrNull {
            it.backgroundColor == cfg.backgroundColor && it.primaryColor == cfg.primaryColor
        }?.id ?: -1
    }
    fun getCurrentThemeName(context: Context): String {
        val id = getCurrentThemeId(context)
        return presets.firstOrNull { it.id == id }?.name ?: "Custom / Default"
    }
    fun applyThemePreset(context: Context, presetId: Int) {
        val preset = presets.firstOrNull { it.id == presetId } ?: return
        val cfg = context.config
        cfg.isSystemThemeEnabled = false
        cfg.textColor = preset.textColor
        cfg.backgroundColor = preset.backgroundColor
        cfg.primaryColor = preset.primaryColor
        cfg.accentColor = preset.accentColor
        if (cfg.appIconColor != preset.appIconColor) {
            cfg.appIconColor = preset.appIconColor
            context.checkAppIconColor()
        }
    }
}