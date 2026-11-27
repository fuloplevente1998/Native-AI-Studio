package com.example.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

internal val LocalPinnedGlassChrome = staticCompositionLocalOf<KyantScene?> { null }

/** Measured fixed bars and a separate scroll source. Only the list is captured, never its lenses. */
@Composable
internal fun GlassScrollChrome(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    rootInset: Dp = LocalGlassTopInset.current,
    localWindow: Boolean = false,
    top: (@Composable () -> Unit)? = null,
    bottom: (@Composable () -> Unit)? = null,
    content: @Composable (Modifier, Dp, Dp) -> Unit
) {
    // Only a real window-local scene layer can be combined with the list capture.
    // A combined Backdrop is not itself a LayerBackdrop; retain the padded fallback in that case.
    val sceneLayer = LocalKyantScene.current.forThisWindow()?.backdrop as? LayerBackdrop
    // Opt-in for nested lists / sheets: capture only this window's scrolling body,
    // never an inherited parent-window layer. Static sheet paint stays behind the lens.
    val powerSave = LocalGlassPowerSave.current
    val localLive = localWindow && KyantGlass.active(LocalTrainPilotPalette.current) &&
        LocalGlassOpticsEnabled.current && !powerSave
    // New opt-in list chrome also keeps its padded fallback in battery saver;
    // preserve the existing root/measurement routes' original policy.
    val live = enabled && (!localWindow || !powerSave) &&
        ((LocalGlassChromeUnderlap.current && sceneLayer != null) || localLive)
    if (!live) {
        Column(modifier) {
            if (rootInset > 0.dp) Spacer(Modifier.height(rootInset))
            top?.invoke()
            Box(Modifier.weight(1f).fillMaxWidth()) { content(Modifier.fillMaxSize(), 0.dp, 0.dp) }
            bottom?.invoke()
        }
        return
    }

    val layer = rememberLayerBackdrop()
    val chrome = if (sceneLayer != null) rememberKyantHeaderScene(sceneLayer, layer)
        else rememberKyantScene(layer, samplesContent = true)
    MeasuredScrollChrome(
        modifier = modifier,
        rootInset = rootInset,
        capture = Modifier.layerBackdrop(layer),
        top = { CompositionLocalProvider(LocalKyantHeader provides chrome, LocalPinnedGlassChrome provides chrome) { top?.invoke() } },
        bottom = { CompositionLocalProvider(LocalKyantHeader provides chrome, LocalPinnedGlassChrome provides chrome) { bottom?.invoke() } },
        content = content
    )
}
