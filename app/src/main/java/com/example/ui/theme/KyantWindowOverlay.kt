package com.example.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
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
    staticSheet: Boolean = false,
    content: @Composable (Modifier) -> Unit
) {
    val parentScene = LocalGlassScene.current
    val sceneState = if (staticSheet) remember(parentScene) { freezeSheetScene(parentScene) } else parentScene
    val layer = rememberKyantSceneBackdrop(palette, sceneState)
    val scene = rememberKyantScene(layer)
    val overlay = Modifier.kyantGlass(scene, palette, shape, GlassMaterialRole.Overlay)
    Box {
        KyantSceneBackground(layer, Modifier.matchParentSize().clip(shape))
        // The lens has no content children: scroll and status updates cannot enter its capture.
        if (staticSheet) Box(Modifier.matchParentSize().then(overlay))
        CompositionLocalProvider(LocalKyantScene provides scene, LocalKyantHeader provides null,
            LocalGlassOverlay provides true) {
            content(if (staticSheet) Modifier else overlay)
        }
    }
}
