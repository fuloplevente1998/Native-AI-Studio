package com.repforge.app.wear

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.json.JSONObject
import kotlin.math.roundToInt

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
                value!!.optString("key").let { key ->
                    if (Regex("custom:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}").matches(key)) key
                    else key.take(32)
                },
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
    val onAccentColor: Color get() = color(onAccent, if (accentColor.luminance() > 0.179f) Color.Black else Color.White)

    val accentColor: Color
        get() = Color(0xFF000000L or accent.substring(1).toLong(16))

    val secondaryColor: Color
        get() = Color(0xFF000000L or accent2.substring(1).toLong(16))

    val frame: Color
        get() = color(outline, mix(Color(0xFF2C3440), accentColor, if (family == "vivid") .76f else .66f))

    val button: Color
        get() = mix(elevatedColor, accentColor, if (family == "vivid") .24f else .16f)

    /** Material controls use the same palette as our custom Wear cards, including light surfaces. */
    fun materialColors(base: androidx.wear.compose.material3.ColorScheme = androidx.wear.compose.material3.ColorScheme()): androidx.wear.compose.material3.ColorScheme {
        val onSecondary = if (secondaryColor.luminance() > 0.179f) Color.Black else Color.White
        return base.copy(
            primary = accentColor, primaryDim = accentColor, primaryContainer = button,
            onPrimary = onAccentColor, onPrimaryContainer = textColor,
            secondary = secondaryColor, secondaryDim = secondaryColor, secondaryContainer = elevatedColor,
            onSecondary = onSecondary, onSecondaryContainer = textColor,
            tertiary = secondaryColor, tertiaryDim = secondaryColor, tertiaryContainer = elevatedColor,
            onTertiary = onSecondary, onTertiaryContainer = textColor,
            surfaceContainerLow = surfaceColor, surfaceContainer = surfaceColor, surfaceContainerHigh = elevatedColor,
            onSurface = textColor, onSurfaceVariant = mutedColor,
            outline = outlineColor, outlineVariant = outlineColor.copy(alpha = .6f),
            background = backgroundColor, onBackground = textColor
        )
    }

    /**
     * 2808: the display palette of the watch. The stored phone palette stays untouched (it is
     * persisted and round-tripped as received); only rendering uses this copy. A light or
     * custom phone background becomes the dark watch base, accents are kept, and every colour
     * is lightened only as far as needed for text >= 4.5:1 and controls >= 3:1 on all surfaces.
     */
    fun forWatch(): WearHomeTheme {
        val darkBase = backgroundColor.luminance() > WEAR_LIGHT_SURFACE
        fun surface(value: Color, dark: Color, tint: Float) =
            if (!darkBase && value.luminance() <= WEAR_LIGHT_SURFACE) value else mix(dark, accentColor, tint)
        val bg = wearOpaque(surface(backgroundColor, Color(0xFF0E1015), 0f))
        val card = wearOpaque(surface(surfaceColor, Color(0xFF1A1E25), .06f))
        val raised = wearOpaque(surface(elevatedColor, Color(0xFF232933), .10f))
        val all = listOf(bg, card, raised)
        fun readable(value: Color, fallback: Color) =
            wearEnsureContrast(if (value.luminance() < .18f) fallback else value, all, WEAR_TEXT_CONTRAST)
        val accentOut = wearEnsureContrast(
            wearEnsureContrast(accentColor, listOf(bg, card), WEAR_TEXT_CONTRAST), listOf(raised), WEAR_CONTROL_CONTRAST)
        val onAccentOut = color(onAccent, Color.Black).takeIf { wearContrast(it, accentOut) >= WEAR_TEXT_CONTRAST }
            ?: listOf(Color.Black, Color.White).maxBy { wearContrast(it, accentOut) }
        return copy(
            accent = wearHex(accentOut),
            accent2 = wearHex(wearEnsureContrast(secondaryColor, all, WEAR_TEXT_CONTRAST)),
            background = wearHex(bg), surface = wearHex(card), elevated = wearHex(raised),
            text = wearHex(readable(textColor, Color(0xFFF6F8FB))),
            muted = wearHex(wearEnsureContrast(if (darkBase) Color(0xFF9AA5B6) else mutedColor, all, WEAR_TEXT_CONTRAST)),
            outline = wearHex(wearEnsureContrast(frame, listOf(bg, card), WEAR_CONTROL_CONTRAST)),
            onAccent = wearHex(onAccentOut),
        )
    }

    // Same gamma-encoded sRGB interpolation as the phone's CSS color-mix(in srgb).
    private fun mix(a: Color, b: Color, amount: Float): Color =
        Color(
            red = a.red * (1f - amount) + b.red * amount,
            green = a.green * (1f - amount) + b.green * amount,
            blue = a.blue * (1f - amount) + b.blue * amount,
        )
}

internal const val WEAR_TEXT_CONTRAST = 4.5f
internal const val WEAR_CONTROL_CONTRAST = 3f
private const val WEAR_LIGHT_SURFACE = .18f

/** WCAG 2.x contrast ratio of two opaque colours. */
internal fun wearContrast(a: Color, b: Color): Float {
    val la = a.luminance()
    val lb = b.luminance()
    return (maxOf(la, lb) + .05f) / (minOf(la, lb) + .05f)
}

internal fun wearHex(color: Color): String =
    "#%02x%02x%02x".format(java.util.Locale.ROOT,
        (color.red * 255f).roundToInt().coerceIn(0, 255),
        (color.green * 255f).roundToInt().coerceIn(0, 255),
        (color.blue * 255f).roundToInt().coerceIn(0, 255),
    )

private fun wearOpaque(color: Color): Color = Color(0xFF000000L or wearHex(color).substring(1).toLong(16))

/** Moves a colour toward white (dark surfaces) or black just far enough; checked after sRGB rounding. */
internal fun wearEnsureContrast(color: Color, surfaces: List<Color>, minimum: Float): Color {
    val start = wearOpaque(color)
    if (surfaces.all { wearContrast(start, it) >= minimum }) return start
    val target = if (surfaces.map { it.luminance() }.average() < .5) Color.White else Color.Black
    for (step in 1..40) {
        val amount = step / 40f
        val candidate = wearOpaque(Color(
            red = start.red + (target.red - start.red) * amount,
            green = start.green + (target.green - start.green) * amount,
            blue = start.blue + (target.blue - start.blue) * amount,
        ))
        if (surfaces.all { wearContrast(candidate, it) >= minimum }) return candidate
    }
    return target
}

internal val LocalWearHomeTheme = staticCompositionLocalOf { WearHomeTheme.DEFAULT }
