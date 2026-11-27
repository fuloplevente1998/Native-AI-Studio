package com.example.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.repforge.app.ui.theme.glassPressMotion

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
        GlassMaterialRole.Panel -> .28f - .10f * palette.glassClarity / 100f
        GlassMaterialRole.Input -> .24f - .08f * palette.glassClarity / 100f
        else -> .24f - .10f * palette.glassClarity / 100f
    }
    return listOf(base.copy(alpha = (alpha + .08f).coerceAtMost(.45f)),
        base.copy(alpha = alpha * .50f), base.copy(alpha = alpha))
}

/** The optical surface is a single Kyant pass over the window's existing scene. Never
 * sample the header's captured content from a body control, or a dialog's parent window.
 * The cheap material uses no backdrop and adds only the shared vector bevel. */
@Composable
internal fun Modifier.liquidDropMaterial(palette: TrainPilotPalette, shape: Shape, role: GlassMaterialRole,
    accent: Color? = null, live: Boolean = true, allowMeasurement: Boolean = false,
    pressSource: MutableInteractionSource? = null
): Modifier {
    if (!palette.isLiquidGlass) return this
    val fullOptics = LocalGlassOpticsEnabled.current
    val optical = liquidDropUsesLens(true, KyantGlass.supported, LocalGlassPowerSave.current,
        LocalGlassOverlay.current, fullOptics, LocalGlassChromeUnderlap.current,
        allowMeasurement, live, role)
    if (optical) {
        val scene = LocalKyantScene.current.forThisWindow()
        if (scene != null) return kyantGlass(scene, palette, shape, role, accent, pressSource)
    }
    val materialMotion = if (pressSource != null) Modifier.glassPressMotion(
        pressSource, enabled = true, shape = shape, stretchOnDrag = true, includeTouchBubble = false
    ) else Modifier
    return then(materialMotion)
        .background(Brush.verticalGradient(liquidDropPaints(palette, role, accent)), shape)
        .trainPilotGlassRim(palette, shape, role)
}
