package com.example.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop

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
    val source = remember(layer, staticSheet) {
        if (staticSheet) SheetLocalBackdrop(layer) else layer
    }
    val overlay = Modifier.kyantGlass(rememberKyantScene(source), palette, shape, GlassMaterialRole.Overlay)
    val scene = rememberKyantScene(layer)
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

/** The frozen scene and lens are equal-sized siblings at the same origin. Their window
 * position changes in lockstep as Material moves the sheet, so Kyant needs no coordinate observer.
 */
private class SheetLocalBackdrop(private val source: LayerBackdrop) : Backdrop {
    override val isCoordinatesDependent = false

    override fun DrawScope.drawBackdrop(
        density: Density,
        coordinates: LayoutCoordinates?,
        layerBlock: (GraphicsLayerScope.() -> Unit)?
    ) {
        drawLayer(source.graphicsLayer)
    }
}
