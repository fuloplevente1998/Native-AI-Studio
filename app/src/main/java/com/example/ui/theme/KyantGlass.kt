package com.example.ui.theme

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Build
import android.view.View
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.rememberGraphicsLayer
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
import com.kyant.backdrop.shadow.Shadow

/*
 * [Kyant test build only – branch test/2810-kyant-glass, NEVER merged into main]
 * Liquid Glass (palettes 33–40) rendered with the Kyant Backdrop engine
 * (io.github.kyant0:backdrop 1.0.6, Apache-2.0, see assets/third-party-licenses/). Only its public
 * API is used (rememberLayerBackdrop / layerBackdrop / drawBackdrop + blur, lens, colorFilter,
 * highlight, shadow); no Kyant source is copied. Legacy palettes and custom themes never reach it.
 *
 * Layers: the page backdrop (living blobs or the user image, [drawGlassSceneCompose]) is drawn once
 * at the root and recorded as one [LayerBackdrop]; every card, tile, chip and sheet panel samples
 * THAT layer, so the glass reads the same everywhere. The header / nav pill samples the scene plus
 * the scrolled content. API < 31: the previous opaque fallback (Kyant needs RenderEffect).
 */
internal object KyantGlass {
    val supported: Boolean get() = Build.VERSION.SDK_INT >= 31

    fun active(palette: TrainPilotPalette): Boolean = palette.isLiquidGlass && supported

    /**
     * Contrast guard after the vibrancy boost. The page backdrop is luminance-guarded to
     * [GlassScene.lumaCap]; a saturation boost s can lift a guarded colour's luminance (up to
     * ~1.56x at s = 1.30), so the same colour matrix scales the channels back down. Numerically
     * checked on the full guarded sRGB set: k = 1 - .75 (s - 1) keeps it at or below the cap.
     */
    fun sceneDim(saturation: Float): Float = (1f - .75f * (saturation - 1f)).coerceIn(.5f, 1f)

    /** Content-sampling glass (header, nav pill, preview) may see white text: cap it at the lens peak. */
    const val CONTENT_DIM = GlassOptics.LENS_PEAK

    /** Rim zone of the lens (dp) – grows towards Clear, like the own engine's bevel. */
    fun refractionHeightDp(clarity: Float): Float = 6f + 18f * clarity.coerceIn(0f, 1f)

    /** Refraction strength (dp) – stronger towards Clear. */
    fun refractionAmountDp(clarity: Float): Float = 10f + 38f * clarity.coerceIn(0f, 1f)

    fun highlightAlpha(clarity: Float): Float = .35f + .55f * clarity.coerceIn(0f, 1f)

    /** Vibrancy + guard in one matrix: saturation s, then every channel scaled by [dim]. */
    fun vibrancyMatrix(saturation: Float, dim: Float): FloatArray {
        val matrix = ColorMatrix().apply { setSaturation(saturation) }
        matrix.postConcat(ColorMatrix().apply { setScale(dim, dim, dim, 1f) })
        return matrix.array
    }
}

/** What a glass surface samples and how it is tinted. */
internal enum class KyantSurface {
    /** Card / panel / chip over the page backdrop: palette card paint as tint, soft shadow. */
    CARD,
    /** Inner tile: the caller's translucent tile colour is the tint; blur only, no lens/shadow. */
    TILE,
    /** Header, nav pill, preview lens: no shadow; samples the header backdrop (scene + content). */
    BAR,
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
internal fun rememberKyantSceneBackdrop(palette: TrainPilotPalette): LayerBackdrop {
    val scene = LocalGlassScene.current
    val onDraw: ContentDrawScope.() -> Unit = remember(palette, scene) { { drawKyantScene(palette, scene) } }
    return rememberLayerBackdrop(rememberGraphicsLayer(), onDraw)
}

private fun DrawScope.drawKyantScene(palette: TrainPilotPalette, scene: GlassSceneState?) =
    drawGlassSceneCompose(palette, scene, Offset.Zero, size.width.coerceAtLeast(1f), size.height.coerceAtLeast(1f))

/** Draws the page backdrop visibly and records it as [backdrop] (children are not recorded). */
@Composable
internal fun Modifier.kyantSceneLayer(palette: TrainPilotPalette, backdrop: LayerBackdrop): Modifier {
    val scene = LocalGlassScene.current
    return drawBehind { drawKyantScene(palette, scene) }.layerBackdrop(backdrop)
}

/**
 * One glass surface sampling [scene] through Kyant: vibrancy + guard → blur (0–100 slider:
 * 24 dp Frosted … 0.5 dp Clear) → rim lens (API 33+, rounded shapes) → palette tint → highlight.
 */
@Composable
internal fun Modifier.kyantGlass(scene: KyantScene, palette: TrainPilotPalette, shape: Shape, kind: KyantSurface): Modifier {
    val density = LocalDensity.current
    val clarity = palette.glassClarity
    val tint = if (kind == KyantSurface.TILE) null else palette.cardPaint
    val content = scene.samplesContent
    val effects = remember(clarity, kind, density, shape, content) {
        val optics = GlassOptics.from(clarity)
        val t = optics.clarity
        val dim = if (content) KyantGlass.CONTENT_DIM else KyantGlass.sceneDim(optics.saturation)
        val filter = ColorMatrixColorFilter(KyantGlass.vibrancyMatrix(optics.saturation, dim))
        val blurPx = with(density) { optics.blurDp.dp.toPx() }
        val lensHeight = with(density) { KyantGlass.refractionHeightDp(t).dp.toPx() }
        val lensAmount = with(density) { KyantGlass.refractionAmountDp(t).dp.toPx() }
        val withLens = kind != KyantSurface.TILE
        val block: com.kyant.backdrop.BackdropEffectScope.() -> Unit = {
            colorFilter(filter)
            blur(blurPx)
            // Kyant's lens needs a rounded-rect SDF; other shapes keep blur + tint only.
            if (withLens && this.shape is CornerBasedShape && size.minDimension > 1f) {
                lens(lensHeight.coerceAtMost(size.minDimension / 2f), lensAmount,
                    depthEffect = t > .5f, chromaticAberration = t > .6f)
            }
        }
        block
    }
    val highlight = remember(clarity) {
        val t = GlassOptics.from(clarity).clarity
        val value = Highlight(width = (.5f + .8f * t).dp, alpha = KyantGlass.highlightAlpha(t))
        val provider: () -> Highlight? = { value }
        provider
    }
    val shadow: (() -> Shadow?)? = remember(kind) {
        if (kind != KyantSurface.CARD) null else {
            val value = Shadow(radius = 12.dp, color = Color.Black.copy(alpha = .14f))
            val provider: () -> Shadow? = { value }
            provider
        }
    }
    val surface: (DrawScope.() -> Unit)? = remember(tint) {
        if (tint == null) null else {
            val paint: DrawScope.() -> Unit = { drawRect(tint) }
            paint
        }
    }
    val shapeProvider = remember(shape) { { shape } }
    return drawBackdrop(
        backdrop = scene.backdrop,
        shape = shapeProvider,
        effects = effects,
        highlight = highlight,
        shadow = shadow,
        onDrawSurface = surface,
    )
}

/** Card-level entry used by [trainPilotGlassMaterial]; null when Kyant cannot serve this window. */
@Composable
internal fun Modifier.kyantGlassOrNull(palette: TrainPilotPalette, shape: Shape, kind: KyantSurface): Modifier? {
    if (!KyantGlass.active(palette)) return null
    val scene = (if (kind == KyantSurface.BAR) LocalKyantHeader.current else LocalKyantScene.current).forThisWindow()
        ?: return null
    return kyantGlass(scene, palette, shape, kind)
}

/**
 * Inner glass tile (2809/3 tiles, Kyant build): the tile's own translucent colour stays the tint,
 * Kyant adds blur of the page backdrop under it. Legacy palettes / API < 31: unchanged.
 */
@Composable
internal fun Modifier.glassTileBackdrop(palette: TrainPilotPalette, shape: Shape): Modifier =
    kyantGlassOrNull(palette, shape, KyantSurface.TILE) ?: this

/** Composite of [backdrop] + [content] for content-sampling glass (header, nav pill). */
@Composable
internal fun rememberKyantHeaderScene(scene: LayerBackdrop, content: LayerBackdrop?): KyantScene {
    val view = LocalView.current
    val combined = if (content == null) scene
        else com.kyant.backdrop.backdrops.rememberCombinedBackdrop(scene, content)
    return remember(combined, view) { KyantScene(combined, view, samplesContent = content != null) }
}

@Composable
internal fun rememberKyantScene(backdrop: LayerBackdrop, samplesContent: Boolean = false): KyantScene {
    val view = LocalView.current
    return remember(backdrop, view, samplesContent) { KyantScene(backdrop, view, samplesContent) }
}
