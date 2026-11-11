package com.repforge.app.wear

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import org.json.JSONObject

/** Optional phone palette metadata; old phone snapshots remain readable. */
data class WearHomeTheme(
    val key: String,
    val accent: String,
    val accent2: String,
    val family: String,
    val background: String? = null, val surface: String? = null,
    val elevated: String? = null, val text: String? = null,
    val muted: String? = null, val outline: String? = null, val onAccent: String? = null,
) {
    companion object {
        val DEFAULT = WearHomeTheme("classicBlue", "#4d8fbe", "#7cb4d6", "basic")

        fun parse(value: JSONObject?): WearHomeTheme {
            val accent = value?.optString("accent").orEmpty()
            val accent2 = value?.optString("accent2").orEmpty()
            val hex = Regex("#[0-9a-fA-F]{6}")
            if (!hex.matches(accent) || !hex.matches(accent2)) return DEFAULT
            return WearHomeTheme(
                value!!.optString("key").take(32),
                accent,
                accent2,
                value.optString("family").takeIf { it in setOf("basic", "vivid", "matte", "contrast", "glow", "light") } ?: "basic",
                background = value.optString("background").takeIf(hex::matches),
                surface = value.optString("surface").takeIf(hex::matches),
                elevated = value.optString("elevated").takeIf(hex::matches),
                text = value.optString("text").takeIf(hex::matches),
                muted = value.optString("muted").takeIf(hex::matches),
                outline = value.optString("outline").takeIf(hex::matches),
                onAccent = value.optString("onAccent").takeIf(hex::matches),
            )
        }
    }

    fun toJson(): JSONObject = JSONObject().put("key", key).put("accent", accent).put("accent2", accent2).put("family", family)
        .put("background", background).put("surface", surface).put("elevated", elevated)
        .put("text", text).put("muted", muted).put("outline", outline).put("onAccent", onAccent)

    private fun color(hex: String?, fallback: Color): Color = hex?.let { Color(0xFF000000L or it.substring(1).toLong(16)) } ?: fallback
    val backgroundColor: Color get() = color(background, if (family == "light") Color(0xFFF4F7FB) else Color(0xFF0E1015))
    val surfaceColor: Color get() = color(surface, if (family == "light") Color.White else Color(0xFF1A1E25))
    val elevatedColor: Color get() = color(elevated, if (family == "light") Color(0xFFE5EBF3) else Color(0xFF232933))
    val textColor: Color get() = color(text, if (family == "light") Color(0xFF151B26) else Color(0xFFF6F8FB))
    val mutedColor: Color get() = color(muted, if (family == "light") Color(0xFF48566C) else Color(0xFF9AA5B6))
    val outlineColor: Color get() = color(outline, frame)
    val onAccentColor: Color get() = color(onAccent, textColor)

    val accentColor: Color
        get() = Color(0xFF000000L or accent.substring(1).toLong(16))

    val secondaryColor: Color
        get() = Color(0xFF000000L or accent2.substring(1).toLong(16))

    val frame: Color
        get() = color(outline, mix(Color(0xFF2C3440), accentColor, if (family == "vivid") .76f else .66f))

    val button: Color
        get() = mix(elevatedColor, accentColor, if (family == "vivid") .24f else .16f)

    // Same gamma-encoded sRGB interpolation as the phone's CSS color-mix(in srgb).
    private fun mix(a: Color, b: Color, amount: Float): Color =
        Color(
            red = a.red * (1f - amount) + b.red * amount,
            green = a.green * (1f - amount) + b.green * amount,
            blue = a.blue * (1f - amount) + b.blue * amount,
        )
}

internal val LocalWearHomeTheme = staticCompositionLocalOf { WearHomeTheme.DEFAULT }
