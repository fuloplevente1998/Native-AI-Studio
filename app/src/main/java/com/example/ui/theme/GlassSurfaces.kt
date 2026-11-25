package com.example.ui.theme

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


/** Opaque, guarded popup/sheet paint: a separate window never samples the obscured parent.
 * Use one matte material across the complete surface, including the drag handle. */
internal fun TrainPilotPalette.glassOverlayFill(legacy: Color): Color {
    if (!isLiquidGlass) return legacy
    val base = GlassContrast.limit(surfaceCard.copy(alpha = 1f), GlassOptics.SURFACE_LUMA_MAX)
    val tint = GlassContrast.limit(primary.copy(alpha = 1f), GlassOptics.SURFACE_LUMA_MAX)
    val coat = KyantMaterialSpec.forRole(GlassMaterialRole.Overlay, glassClarity).tintAlpha
    return lerp(base, tint, coat)
}

/** 2812/3: keep the legacy advice divider; glass only needs a quiet, theme-derived line. */
internal fun TrainPilotPalette.coachAdviceDividerColor(): Color =
    if (isLiquidGlass) textPrimary.copy(alpha = .16f) else cardBorder

internal fun programDayGlassRole(selected: Boolean): GlassMaterialRole =
    if (selected) GlassMaterialRole.Selection else GlassMaterialRole.Button

/** Static day selector paint. Reuse role opacity and bounded accent instead of another lens. */
internal fun TrainPilotPalette.programDayFill(selected: Boolean): Color {
    val legacy = if (selected) primary else surface
    val role = programDayGlassRole(selected)
    val paint = glassControlFill(legacy, role)
    if (!isLiquidGlass || !selected) return paint
    return GlassContrast.limit(primary, GLASS_TILE_LUMA).copy(alpha = paint.alpha)
}

internal fun TrainPilotPalette.programDayTextColor(selected: Boolean): Color =
    if (selected && !isLiquidGlass) onPrimary else textPrimary
