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
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/** Measured fixed bars and a separate scroll source. Only the list is captured, never its lenses. */
@Composable
internal fun GlassScrollChrome(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    top: (@Composable () -> Unit)? = null,
    bottom: (@Composable () -> Unit)? = null,
    content: @Composable (Modifier, Dp, Dp) -> Unit
) {
    val rootInset = LocalGlassTopInset.current
    val scene = LocalKyantScene.current.forThisWindow()
    val live = enabled && LocalGlassChromeUnderlap.current && scene != null
    if (!live || scene == null) {
        Column(modifier) {
            if (rootInset > 0.dp) Spacer(Modifier.height(rootInset))
            top?.invoke()
            Box(Modifier.weight(1f).fillMaxWidth()) { content(Modifier.fillMaxSize(), 0.dp, 0.dp) }
            bottom?.invoke()
        }
        return
    }

    val layer = rememberLayerBackdrop()
    val chrome = rememberKyantScene(rememberCombinedBackdrop(scene.backdrop, layer), samplesContent = true)
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
            content(Modifier.fillMaxSize().layerBackdrop(layer), (inset + upper.height).toDp(), lower.height.toDp())
        }.single().measure(Constraints.fixed(width, height))
        layout(width, height) {
            body.placeRelative(0, 0)
            upper.placeRelative(0, inset)
            lower.placeRelative(0, height - lower.height)
        }
    }
}
