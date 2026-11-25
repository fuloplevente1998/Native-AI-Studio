package com.example.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/** Measured fixed bars and a separate scroll source. Only the list is captured, never its lenses. */
@Composable
internal fun GlassScrollChrome(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    rootInset: Dp = LocalGlassTopInset.current,
    top: (@Composable () -> Unit)? = null,
    bottom: (@Composable () -> Unit)? = null,
    content: @Composable (Modifier, Dp, Dp) -> Unit
) {
    // Only a real window-local scene layer can be combined with the list capture.
    // A combined Backdrop is not itself a LayerBackdrop; retain the padded fallback in that case.
    val sceneLayer = LocalKyantScene.current.forThisWindow()?.backdrop as? LayerBackdrop
    val live = enabled && LocalGlassChromeUnderlap.current && sceneLayer != null
    if (!live || sceneLayer == null) {
        Column(modifier) {
            if (rootInset > 0.dp) Spacer(Modifier.height(rootInset))
            top?.invoke()
            Box(Modifier.weight(1f).fillMaxWidth()) { content(Modifier.fillMaxSize(), 0.dp, 0.dp) }
            bottom?.invoke()
        }
        return
    }

    val layer = rememberLayerBackdrop()
    val chrome = rememberKyantHeaderScene(sceneLayer, layer)
    SubcomposeLayout(modifier) { constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val barConstraints = Constraints(minWidth = width, maxWidth = width, maxHeight = height)
        val inset = rootInset.roundToPx().coerceIn(0, height)
        val upper = subcompose("top") {
            CompositionLocalProvider(LocalKyantHeader provides chrome) { Box { top?.invoke() } }
        }.single().measure(barConstraints.copy(maxHeight = height - inset))
        val lower = subcompose("bottom") {
            CompositionLocalProvider(LocalKyantHeader provides chrome) { Box { bottom?.invoke() } }
        }.single().measure(barConstraints.copy(maxHeight = (height - inset - upper.height).coerceAtLeast(0)))
        val body = subcompose("body") {
            // The initial and terminal safe areas live INSIDE the scrolling viewport. The
            // measured heights are available in the same layout pass, including large font/rest dock.
            Box(Modifier.fillMaxSize().layerBackdrop(layer)) {
                content(Modifier.fillMaxSize(), (inset + upper.height).toDp(), lower.height.toDp())
            }
        }.single().measure(Constraints.fixed(width, height))
        layout(width, height) {
            body.placeRelative(0, 0)
            upper.placeRelative(0, inset)
            lower.placeRelative(0, height - lower.height)
        }
    }
}
