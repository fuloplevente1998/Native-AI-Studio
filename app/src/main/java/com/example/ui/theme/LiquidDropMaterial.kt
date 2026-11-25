package com.example.ui.theme

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

/** Only opt-in controls use a live lens while measuring. Dense rows, separate-window children,
 * battery saver and older APIs retain a bounded, dimensional paint. No new capture layer. */
internal fun liquidDropUsesLens(glass: Boolean, supported: Boolean, powerSave: Boolean,
    overlay: Boolean, fullOptics: Boolean, measurementChrome: Boolean, allowMeasurement: Boolean,
    live: Boolean, role: GlassMaterialRole): Boolean =
    glass && supported && !powerSave && !overlay && live &&
        (fullOptics || (measurementChrome && allowMeasurement)) &&
        (role == GlassMaterialRole.Button || role == GlassMaterialRole.Selection || role == GlassMaterialRole.Input)

/** Three opaque RGB seeds bounded below the existing glass ceiling; alpha is kept low. */
internal fun liquidDropPaints(palette: TrainPilotPalette, role: GlassMaterialRole, accent: Color? = null): List<Color> {
    val base = GlassContrast.limit(accent ?: GLASS_TILE, GLASS_TILE_LUMA)
    val alpha = when (role) {
        GlassMaterialRole.Selection -> (.36f - .12f * palette.glassClarity / 100f)
        GlassMaterialRole.Panel -> .24f
        GlassMaterialRole.Input -> .20f
        else -> .20f
    }
    return listOf(base.copy(alpha = (alpha + .08f).coerceAtMost(.45f)),
        base.copy(alpha = alpha * .50f), base.copy(alpha = alpha))
}

/** The optical surface is a single Kyant pass over the window's existing scene. Never
 * sample the header's captured content from a body control, or a dialog's parent window.
 * The cheap material uses no backdrop and adds only the shared vector bevel. */
@Composable
internal fun Modifier.liquidDropMaterial(palette: TrainPilotPalette, shape: Shape, role: GlassMaterialRole,
    accent: Color? = null, live: Boolean = true, allowMeasurement: Boolean = false): Modifier {
    if (!palette.isLiquidGlass) return this
    val optical = liquidDropUsesLens(true, KyantGlass.supported, LocalGlassPowerSave.current,
        LocalGlassOverlay.current, LocalGlassOpticsEnabled.current, LocalGlassChromeUnderlap.current,
        allowMeasurement, live, role)
    if (optical) {
        val scene = LocalKyantScene.current.forThisWindow()
        if (scene != null) {
            val lens = kyantGlass(scene, palette, shape, role)
            return if (accent == null) lens else lens.background(
                GlassContrast.limit(accent, GLASS_TILE_LUMA).copy(alpha = .20f), shape)
        }
    }
    return background(Brush.verticalGradient(liquidDropPaints(palette, role, accent)), shape)
        .trainPilotGlassRim(palette, shape, role)
}
