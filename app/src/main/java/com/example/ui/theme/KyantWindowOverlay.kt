package com.example.ui.theme
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.remember
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

/** The sheet container already paints this color behind its handle and body. Capture only that
 * known opaque fill, not the children and not a second animated scene. */
@Composable
internal fun KyantStaticSheetBackdrop(fill: Color, content: @Composable (Modifier) -> Unit) {
    val draw: ContentDrawScope.() -> Unit = remember(fill) { { drawRect(fill) } }
    val layer = rememberLayerBackdrop(rememberGraphicsLayer(), draw)
    val scene = rememberKyantScene(layer)
    CompositionLocalProvider(LocalKyantScene provides scene, LocalKyantHeader provides scene) {
        content(Modifier.layerBackdrop(layer))
    }
}
