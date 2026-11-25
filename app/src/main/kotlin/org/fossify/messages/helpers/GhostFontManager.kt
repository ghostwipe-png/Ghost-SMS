package org.fossify.messages.helpers
import android.content.Context
import org.fossify.commons.helpers.FONT_TYPE_CUSTOM
import org.fossify.commons.helpers.FONT_TYPE_MONOSPACE
import org.fossify.commons.helpers.FONT_TYPE_SYSTEM_DEFAULT
import org.fossify.commons.helpers.FontHelper
import org.fossify.commons.models.RadioItem
import org.fossify.messages.extensions.config
import java.io.File
object GhostFontManager {
    data class FontPreset(
        val id: Int,
        val name: String,
        val systemFontPath: String?,
        val targetFileName: String
    )
    val presets = listOf(
        FontPreset(0, "System Default", null, ""),
        FontPreset(1, "Source Sans Clean", "/system/fonts/SourceSansPro-Regular.ttf", "ghost_source_sans.ttf"),
        FontPreset(2, "Source Sans SemiBold", "/system/fonts/SourceSansPro-SemiBold.ttf", "ghost_source_semibold.ttf"),
        FontPreset(3, "Source Sans Bold", "/system/fonts/SourceSansPro-Bold.ttf", "ghost_source_bold.ttf"),
        FontPreset(4, "Source Sans Italic", "/system/fonts/SourceSansPro-Italic.ttf", "ghost_source_italic.ttf"),
        FontPreset(5, "Classic Serif", "/system/fonts/NotoSerif-Regular.ttf", "ghost_noto_serif.ttf"),
        FontPreset(6, "Editorial Serif Bold", "/system/fonts/NotoSerif-Bold.ttf", "ghost_noto_serif_bold.ttf"),
        FontPreset(7, "Editorial Serif Italic", "/system/fonts/NotoSerif-Italic.ttf", "ghost_noto_serif_italic.ttf"),
        FontPreset(8, "Typewriter Cutive", "/system/fonts/CutiveMono.ttf", "ghost_cutive_mono.ttf"),
        FontPreset(9, "Developer Mono", "/system/fonts/DroidSansMono.ttf", "ghost_droid_mono.ttf"),
        FontPreset(10, "Elegant Script", "/system/fonts/DancingScript-Regular.ttf", "ghost_dancing_script.ttf"),
        FontPreset(11, "Casual Comic", "/system/fonts/ComingSoon.ttf", "ghost_coming_soon.ttf"),
        FontPreset(12, "Small Caps Gothic", "/system/fonts/CarroisGothicSC-Regular.ttf", "ghost_carrois_gothic.ttf")
    )
    fun getRadioItems(): ArrayList<RadioItem> {
        return ArrayList(presets.map { RadioItem(it.id, it.name) })
    }
    fun getCurrentFontId(context: Context): Int {
        val cfg = context.config
        if (cfg.fontType == FONT_TYPE_SYSTEM_DEFAULT) return 0
        if (cfg.fontType == FONT_TYPE_MONOSPACE && cfg.fontName.isEmpty()) return 9
        return presets.firstOrNull { it.targetFileName == cfg.fontName }?.id ?: 0
    }
    fun getCurrentFontName(context: Context): String {
        val id = getCurrentFontId(context)
        return presets.firstOrNull { it.id == id }?.name ?: "System Default"
    }
    fun applyFontPreset(context: Context, presetId: Int) {
        val preset = presets.firstOrNull { it.id == presetId } ?: presets[0]
        val cfg = context.config
        if (preset.id == 0 || preset.systemFontPath == null) {
            cfg.fontType = FONT_TYPE_SYSTEM_DEFAULT
            cfg.fontName = ""
            FontHelper.clearCache()
            return
        }
        val sourceFile = File(preset.systemFontPath)
        if (sourceFile.exists() && sourceFile.canRead()) {
            val bytes = sourceFile.readBytes()
            val saved = FontHelper.saveFontData(context, bytes, preset.targetFileName)
            if (saved) {
                cfg.fontType = FONT_TYPE_CUSTOM
                cfg.fontName = preset.targetFileName
                FontHelper.clearCache()
                return
            }
        }
        // Fallback if a device lacks that specific system TTF
        if (preset.id == 8 || preset.id == 9) {
            cfg.fontType = FONT_TYPE_MONOSPACE
            cfg.fontName = ""
        } else {
            cfg.fontType = FONT_TYPE_SYSTEM_DEFAULT
            cfg.fontName = ""
        }
        FontHelper.clearCache()
    }
}