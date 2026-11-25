package com.example.ui.theme

import androidx.compose.material3.ButtonColors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.repforge.app.ui.theme.NativeThemeTokens

/**
 * Liquid Glass 2.0 / G3 (2811): shared fills of the common components (buttons, cells, tiles, state chips)
 * in the glass family. One contrast rule for all of them: a fill is a colour whose luminance is at most the
 * neutral tile tint's ([GLASS_TILE_LUMA] < [GlassOptics.SURFACE_LUMA_MAX]) at a low alpha, so drawn over any
 * glass composite (an sRGB mix, convex in luminance) it never lifts it above the ceiling and the palette text
 * stays >= 4.5:1. Legacy palettes and custom themes always get exactly the colour they passed in.
 */
internal val GLASS_TILE_LUMA: Float = GlassContrast.luminance(GLASS_TILE.red, GLASS_TILE.green, GLASS_TILE.blue)

/** Strength of a state tint relative to the neutral tile (planned/selected/semantic states). */
internal const val GLASS_STATE_STRENGTH = 1.7f
internal const val GLASS_STATE_STRONG = 2.4f

/**
 * A semantic state on glass (planned day, selected, save/delete, success/warning): the [accent] hue at the
 * tile luminance and a low alpha. The state also keeps its semantic outline and foreground, never colour alone.
 */
internal fun TrainPilotPalette.glassStateTile(accent: Color, legacy: Color, strong: Boolean = false): Color {
    if (!isLiquidGlass) return legacy
    val limited = GlassContrast.limit(accent.copy(alpha = 1f), GLASS_TILE_LUMA)
    val alpha = glassTileAlpha(glassClarity) * if (strong) GLASS_STATE_STRONG else GLASS_STATE_STRENGTH
    return limited.copy(alpha = alpha.coerceIn(0f, .45f))
}

/** A foreground lifted (towards white) until it reads >= 4.5:1 on the brightest glass composite. */
internal fun glassReadable(color: Color, minimum: Float = 4.55f): Color {
    if (color.alpha < .99f) return color
    for (step in 0..100) {
        val candidate = lerp(color, Color.White, step / 100f)
        if (NativeThemeTokens.contrast(candidate, GlassOptics.lensWorstCase) >= minimum) return candidate
    }
    return Color.White
}

/** Saturated (accent-like) rather than a neutral grey/white surface. */
private fun Color.isChromatic(): Boolean = maxOf(red, green, blue) - minOf(red, green, blue) > .18f

/**
 * Glass version of a button's colours: a solid fill (accent, white/grey surface, save green, delete red)
 * becomes a translucent state tint of its own hue (neutral fills: the plain tile tint), transparent stays
 * transparent, and the label is lifted to stay readable on glass. Legacy / custom: the same instance.
 */
internal fun TrainPilotPalette.glassButtonColors(colors: ButtonColors): ButtonColors {
    if (!isLiquidGlass) return colors
    fun fill(c: Color): Color = when {
        c.alpha < .05f -> c
        c.isChromatic() -> glassStateTile(c, c, strong = true)
        else -> glassTilePaint(glassClarity, elevated = true)
    }
    return colors.copy(containerColor = fill(colors.containerColor), contentColor = glassReadable(colors.contentColor),
        disabledContainerColor = fill(colors.disabledContainerColor).let { it.copy(alpha = it.alpha * .6f) },
        disabledContentColor = colors.disabledContentColor)
}
