package com.example.ui.theme

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape

/**
 * Isolated Android/Kyant implementation. The caller's legacy path must not reference
 * LayerBackdrop directly: Kyant's JVM class format exceeds the Java 17 test runtime.
 * Invoke only after the existing palette/API/optics gates.
 */
@Composable
internal fun KyantWindowOverlay(
    palette: TrainPilotPalette,
    shape: Shape,
    content: @Composable (Modifier) -> Unit
) {
    val layer = rememberKyantSceneBackdrop(palette)
    val scene = rememberKyantScene(layer)
    val paint = GlassContrast.limit(GlassOptics.TINT, GlassOptics.SURFACE_LUMA_MAX)
        .copy(alpha = KyantMaterialSpec.forRole(GlassMaterialRole.Overlay, palette.glassClarity).tintAlpha)
    CompositionLocalProvider(LocalKyantScene provides scene, LocalKyantHeader provides null) {
        content(Modifier.clip(shape).kyantSceneLayer(palette, layer).background(paint, shape))
    }
}
