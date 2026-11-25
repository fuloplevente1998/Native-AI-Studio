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
    MeasuredScrollChrome(
        modifier = modifier,
        rootInset = rootInset,
        capture = Modifier.layerBackdrop(layer),
        top = { CompositionLocalProvider(LocalKyantHeader provides chrome) { top?.invoke() } },
        bottom = { CompositionLocalProvider(LocalKyantHeader provides chrome) { bottom?.invoke() } },
        content = content
    )
}
