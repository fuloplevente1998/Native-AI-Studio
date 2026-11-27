package com.example.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
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


/** One opacity rule for the photo viewer and sliding sheets. The real parent window
 * may show through via ordinary window compositing, never through pixel capture.
 */
internal fun subtleGlassWindowOpacity(clarity: Int): Float =
    1f - .05f * (clarity.coerceIn(0, 100) / 100f)

private fun boundedSubtleWindowFill(base: Color, clarity: Int): Color {
    // Compose sRGB Color stores alpha in 8 bits. Round transparency DOWN so
    // the encoded colour never exceeds the requested 5% maximum (or any slider step).
    val opacity = (255 - clarity.coerceIn(0, 100) * 255 / 2000) / 255f
    // Convex sRGB mixing: even a white parent cannot lift the composite above the ceiling.
    val cap = (GlassOptics.SURFACE_LUMA_MAX - (1f - opacity)) / opacity
    return GlassContrast.limit(base, cap).copy(alpha = opacity)
}

internal fun TrainPilotPalette.photoViewerBackground(legacy: Color): Color {
    if (!isLiquidGlass) return legacy.copy(alpha = 1f)
    return boundedSubtleWindowFill(glassOverlayFill(legacy), glassClarity)
}

internal fun TrainPilotPalette.glassBackgroundTint(cap: Float, saturation: Float = .42f): Color =
    GlassContrast.limit(Color.hsv(GlassScene.hueOf(background), saturation, 1f), cap)

internal fun TrainPilotPalette.glassTintedTile(legacy: Color): Color =
    if (isLiquidGlass) glassBackgroundTint(GLASS_TILE_LUMA).copy(alpha = glassTileAlpha(glassClarity)) else legacy

/** A sliding panel uses the active Liquid palette hue, not a second procedural
 * scene or a neutral grey fill. Preserve the existing quantized 0–5% transparency.
 * Only its background is translucent. Centered alerts and popup menus stay opaque.
 */
internal fun TrainPilotPalette.slidingSheetBackground(legacy: Color): Color {
    if (!isLiquidGlass) return legacy
    // The brightened foreground accent can be nearly white, especially in the Ice palette.
    return boundedSubtleWindowFill(glassBackgroundTint(liquidChromeTint(this).luminance(), saturation = .55f), glassClarity)
}

/** A window is the sole optical surface; descendants use paint and vector rims. */
internal val LocalGlassOverlay = androidx.compose.runtime.staticCompositionLocalOf { false }

/** Window material selection is independent of the caller's legacy fill. JVM-safe. */
internal fun sheetUsesGlass(palette: TrainPilotPalette, @Suppress("UNUSED_PARAMETER") containerColor: Color): Boolean =
    palette.isLiquidGlass

/** Contrasting, bounded paint for nested window roles; capped for text contrast. */
internal fun glassOverlayRolePaint(clarity: Int, role: GlassMaterialRole): Color {
    val tile = KyantMaterialSpec.forRole(GlassMaterialRole.Tile, clarity).tintAlpha
    val neutral = GlassContrast.grey(GlassContrast.target)
    return when (role) {
        GlassMaterialRole.Selection -> GlassContrast.limit(Color(0xFF579BFF), GlassContrast.target)
            .copy(alpha = (tile * GLASS_STATE_STRONG).coerceIn(.28f, .45f))
        GlassMaterialRole.Button -> neutral.copy(alpha = tile + .04f)
        else -> neutral.copy(alpha = tile)
    }
}

@androidx.compose.runtime.Composable
internal fun TrainPilotPalette.overlayRoleFill(legacy: Color, role: GlassMaterialRole): Color =
    if (isLiquidGlass && LocalGlassOverlay.current) glassOverlayRolePaint(glassClarity, role) else legacy

/** Vector-only top highlight and blue selection edge/glow; no backdrop capture. */
@androidx.compose.runtime.Composable
internal fun androidx.compose.ui.Modifier.overlayRoleRim(
    palette: TrainPilotPalette, shape: androidx.compose.ui.graphics.Shape, role: GlassMaterialRole
): androidx.compose.ui.Modifier {
    if (!palette.isLiquidGlass || !LocalGlassOverlay.current) return this
    val strength = KyantMaterialSpec.forRole(role, palette.glassClarity).highlightAlpha
    return this.drawWithCache {
        val path = androidx.compose.ui.graphics.Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }
        val selected = role == GlassMaterialRole.Selection
        val alpha = strength * when (role) {
            GlassMaterialRole.Selection -> 1f
            GlassMaterialRole.Button -> .8f
            else -> .55f
        }
        val edge = if (selected) Color(0xFFB6D8FF) else Color.White
        val rim = androidx.compose.ui.graphics.Brush.verticalGradient(
            0f to edge.copy(alpha = alpha), 1f to edge.copy(alpha = alpha * .4f))
        onDrawWithContent {
            drawContent()
            clipPath(path) {
                if (selected) drawPath(path, edge.copy(alpha = .12f), style = Stroke(5.dp.toPx()))
                drawPath(path, rim, style = Stroke(2.dp.toPx()))
            }
        }
    }
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
    return GlassContrast.limit(programDaySelectionAccent, GLASS_TILE_LUMA).copy(alpha = paint.alpha)
}

internal fun TrainPilotPalette.programDayTextColor(selected: Boolean): Color =
    if (selected && !isLiquidGlass) onPrimary else textPrimary

/** Use the existing Sapphire accent for the blue selection specified by mockup 2812/3. */
internal val programDaySelectionAccent: Color
    get() = LiquidGlassPalettes.all.first { it.key == "glass_sapphire" }.primary

/** A few cached vector strokes outside the selected control, not a blur or sampled backdrop. */
internal fun Modifier.programDaySelectionGlow(palette: TrainPilotPalette, selected: Boolean, shape: Shape): Modifier =
    if (!palette.isLiquidGlass || !selected) this else drawWithCache {
        val edge = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }
        val accent = programDaySelectionAccent
        onDrawBehind {
            clipPath(edge, ClipOp.Difference) {
                drawPath(edge, accent.copy(alpha = .05f), style = Stroke(6.dp.toPx()))
                drawPath(edge, accent.copy(alpha = .09f), style = Stroke(4.dp.toPx()))
                drawPath(edge, accent.copy(alpha = .22f), style = Stroke(2.dp.toPx()))
            }
        }
    }
