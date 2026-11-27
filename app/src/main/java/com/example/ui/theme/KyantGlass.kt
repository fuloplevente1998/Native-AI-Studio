package com.example.ui.theme

import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorMatrixColorFilter
import android.os.Build
import android.view.View
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorFilter
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.highlight.HighlightStyle
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow

/**
 * Kyant Backdrop 2.0.1 (Apache-2.0). Only public APIs; one guarded backdrop per window,
 * with a separate combined source for scrolling header content. Legacy palettes never use it.
 */
internal object KyantGlass {
    val supported: Boolean get() = Build.VERSION.SDK_INT >= 31
    fun active(palette: TrainPilotPalette): Boolean = palette.isLiquidGlass && supported
}

/**
 * A Kyant backdrop bound to the window (view) that records it; other windows fall back.
 * [samplesContent]: the backdrop also holds app content (white text possible) → stronger guard.
 */
@Stable
internal class KyantScene(val backdrop: Backdrop, val view: View, val samplesContent: Boolean)

/** Page backdrop of the current window (root scaffold or a sheet panel). */
internal val LocalKyantScene = staticCompositionLocalOf<KyantScene?> { null }

/** Header / nav backdrop: page backdrop plus the content scrolled under the header. */
internal val LocalKyantHeader = staticCompositionLocalOf<KyantScene?> { null }

/** The scene local only if it belongs to this window (sheets and dialogs are separate windows). */
@Composable
internal fun KyantScene?.forThisWindow(): KyantScene? {
    val view = LocalView.current
    return this?.takeIf { it.view === view }
}

/**
 * The page backdrop layer: the guarded living scene (or user image) in this node's own
 * coordinates. Only the scene is recorded, never the children, so no glass samples itself.
 */
@Composable
internal fun rememberKyantSceneBackdrop(
    palette: TrainPilotPalette,
    scene: GlassSceneState? = LocalGlassScene.current
): LayerBackdrop {
    val onDraw: ContentDrawScope.() -> Unit = remember(palette, scene) { { drawKyantScene(palette, scene) } }
    return rememberLayerBackdrop(rememberGraphicsLayer(), onDraw)
}

private fun DrawScope.drawKyantScene(palette: TrainPilotPalette, scene: GlassSceneState?) =
    drawGlassSceneCompose(palette, scene, Offset.Zero, size.width.coerceAtLeast(1f), size.height.coerceAtLeast(1f))

/**
 * One root scene, drawn as an empty sibling behind the scaffold, never around its content.
 * Backdrop 2.0.1 LayerBackdropNode.draw calls drawContent then records its custom onDraw.
 * Our outer draw modifier runs that recording first, then displays the very same GraphicsLayer.
 * No second procedural/photo draw, no additional graphics layer or bitmap, and no child capture.
 */
@Composable
internal fun KyantSceneBackground(backdrop: LayerBackdrop, modifier: Modifier = Modifier) {
    Box(modifier.drawWithContent {
        drawContent()
        drawLayer(backdrop.graphicsLayer)
    }.layerBackdrop(backdrop))
}

/** Render a single role using the shared source; tiles stay on the inexpensive paint/rim path. */
@Composable
internal fun Modifier.kyantGlass(
    scene: KyantScene, palette: TrainPilotPalette, shape: Shape, role: GlassMaterialRole,
    accent: Color? = null,
    pressSource: MutableInteractionSource? = null,
    minimumBlurDp: Float = 0f,
    showRim: Boolean = true,
    chromeCoat: Color? = null
): Modifier {
    val density = LocalDensity.current
    val spec = remember(role, palette.glassClarity) { KyantMaterialSpec.forRole(role, palette.glassClarity) }
    if (!spec.usesBackdrop) return if (showRim) trainPilotGlassRim(palette, shape) else this
    val clarity = palette.glassClarity
    val scale = if (scene.samplesContent) spec.colorScale else spec.sceneScale
    val effects = remember(spec, density, shape, clarity, scale, minimumBlurDp, showRim) {
        val filter = ColorMatrixColorFilter(ColorMatrix(glassVibrancyMatrix(spec.saturation, scale)))
        val blurPx = with(density) { maxOf(spec.blurDp, minimumBlurDp).dp.toPx() }
        val height = with(density) { spec.lensHeightDp.dp.toPx() }
        val amount = with(density) { spec.lensAmountDp.dp.toPx() }
        val block: com.kyant.backdrop.BackdropEffectScope.() -> Unit = {
            colorFilter(filter)
            blur(blurPx)
            if (showRim && Build.VERSION.SDK_INT >= 33 && spec.usesLens && this.shape is CornerBasedShape && size.minDimension > 1f) {
                lens(height.coerceAtMost(size.minDimension / 2f), amount,
                    depthEffect = role == GlassMaterialRole.Button || role == GlassMaterialRole.Selection || clarity > 50,
                    chromaticAberration = clarity > 60)
            }
        }
        block
    }
    val interactiveSurface = pressSource != null
    val prism = glassPrismaticRimRole(role) && LocalGlassOpticsEnabled.current && !LocalGlassPowerSave.current
    val highlight = remember(spec, interactiveSurface, prism, showRim) {
        val value = Highlight(
            width = (if (prism) 0.9f + 1.5f * spec.highlightAlpha else 0.5f + 0.8f * spec.highlightAlpha).dp,
            alpha = if (prism) (spec.highlightAlpha * 1.12f).coerceAtMost(1f) else spec.highlightAlpha,
            style = if (interactiveSurface || prism) HighlightStyle.Default(angle = 34f, falloff = 1.15f) else HighlightStyle.Default)
        val provider: () -> Highlight? = { value.takeIf { showRim } }
        provider
    }
    val shadow: (() -> Shadow?)? = remember(spec.shadow, role) {
        if (!spec.shadow) null else {
            val control = role == GlassMaterialRole.Button || role == GlassMaterialRole.Selection
            val value = Shadow(radius = if (control) 6.dp else 12.dp, color = Color.Black.copy(alpha = if (control) .22f else .14f))
            val provider: () -> Shadow? = { value }
            provider
        }
    }
    val surface: DrawScope.() -> Unit = remember(palette, role, accent, chromeCoat) {
        val coat = kyantSurfaceCoat(palette, role, accent, chromeCoat)
        val drawSurface: DrawScope.() -> Unit = { drawRect(coat) }
        drawSurface
    }
    val shapeProvider = remember(shape) { { shape } }
    val innerShadow = remember(interactiveSurface) {
        if (interactiveSurface) {
            val value = InnerShadow(radius = 4.dp, offset = androidx.compose.ui.unit.DpOffset(0.dp, 1.dp),
                color = Color.Black.copy(alpha = .15f))
            val style: () -> InnerShadow? = { value }
            style
        } else null
    }
    // The optical layer and its resting rim remain stable during a touch. The shared
    // control indication draws the local bubble; selection-pill motion remains separate.
    return drawBackdrop(backdrop = scene.backdrop, shape = shapeProvider, effects = effects,
        highlight = highlight, shadow = shadow, innerShadow = innerShadow, onDrawSurface = surface)
}

/** Shared entry: only the matching window may sample a recorded backdrop. */
@Composable
internal fun Modifier.kyantGlassOrNull(palette: TrainPilotPalette, shape: Shape, role: GlassMaterialRole,
    minimumBlurDp: Float = 0f, showRim: Boolean = true, chromeCoat: Color? = null): Modifier? {
    if (LocalGlassOverlay.current || !KyantGlass.active(palette) || !LocalGlassOpticsEnabled.current) return null
    val scene = (if (role == GlassMaterialRole.Navigation || role == GlassMaterialRole.Selection)
        LocalKyantHeader.current ?: LocalKyantScene.current else LocalKyantScene.current).forThisWindow()
        ?: return null
    return kyantGlass(scene, palette, shape, role, minimumBlurDp = minimumBlurDp, showRim = showRim, chromeCoat = chromeCoat)
}

/** Composite of [backdrop] + [content] for content-sampling glass (header, nav pill). */
@Composable
internal fun rememberKyantHeaderScene(scene: LayerBackdrop, content: LayerBackdrop?): KyantScene {
    val view = LocalView.current
    val combined = if (content == null) scene
        else com.kyant.backdrop.backdrops.rememberCombinedBackdrop(scene, content)
    // The same conservative header correction also applies on padded routes (Calendar).
    // Only the source geometry differs; the calendar must not get a brighter scene-only tint.
    return remember(combined, view) { KyantScene(combined, view, samplesContent = true) }
}

@Composable
internal fun rememberKyantScene(backdrop: Backdrop, samplesContent: Boolean = false): KyantScene {
    val view = LocalView.current
    return remember(backdrop, view, samplesContent) { KyantScene(backdrop, view, samplesContent) }
}
