package com.example.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape

/**
 * Android-only window material. One local scene is recorded before the Overlay lens;
 * descendants use bounded paint, never another blur. No parent-window pixel sampling.
 * The caller gates this path on palette/API/optics support.
 */
@Composable
internal fun KyantWindowOverlay(
    palette: TrainPilotPalette,
    shape: Shape,
    content: @Composable (Modifier) -> Unit
) {
    val layer = rememberKyantSceneBackdrop(palette)
    val scene = rememberKyantScene(layer)
    Box {
        KyantSceneBackground(layer, Modifier.matchParentSize().clip(shape))
        val overlay = Modifier.kyantGlass(scene, palette, shape, GlassMaterialRole.Overlay)
        CompositionLocalProvider(LocalKyantScene provides scene, LocalKyantHeader provides null,
            LocalGlassOverlay provides true) {
            content(overlay)
        }
    }
}
