package com.example.ui.theme

import android.graphics.RenderEffect
import androidx.compose.foundation.background
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalView
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Explicit, local backdrop capture. The caller identifies the source content and the lens.
 * The captured layer excludes the lens and all foreground labels, preventing a feedback loop.
 * No screenshots, window capture, bitmaps, readback, or external rendering library.
 */
@Stable
internal class GlassBackdrop internal constructor(internal val layer: GraphicsLayer,
    internal val kyant: LayerBackdrop?, internal val view: android.view.View) {
    internal var origin by mutableStateOf(Offset.Zero)
}

internal val LocalGlassHeaderBackdrop = staticCompositionLocalOf<GlassBackdrop?> { null }
internal val LocalGlassTopInset = compositionLocalOf { 0.dp }

@Composable
internal fun Modifier.glassHeaderMaterial(palette: TrainPilotPalette, shape: Shape,
    role: GlassMaterialRole = GlassMaterialRole.Navigation): Modifier {
    if (LocalGlassOverlay.current) return trainPilotGlassMaterial(palette, shape, role)
    // A chrome-only lens is allowed on scrolling measurement screens, without enabling
    // per-card optics, animated selection, or press effects in the workout content.
    if (LocalGlassChromeUnderlap.current && palette.isLiquidGlass) {
        val chrome = LocalKyantHeader.current.forThisWindow()
        if (chrome != null) return kyantGlass(chrome, palette, shape, role)
    }
    kyantGlassOrNull(palette, shape, role)?.let { return it }
    val backdrop = LocalGlassHeaderBackdrop.current
    return if (backdrop != null) liveGlassLens(backdrop, palette, shape)
    else trainPilotGlassMaterial(palette, shape, role)
}

@Composable
internal fun rememberGlassBackdrop(): GlassBackdrop {
    val layer = rememberGraphicsLayer()
    val kyant = if (KyantGlass.supported && LocalGlassOpticsEnabled.current) rememberLayerBackdrop(layer) else null
    val view = LocalView.current
    return remember(layer, kyant, view) { GlassBackdrop(layer, kyant, view) }
}

@Composable
internal fun Modifier.captureGlassBackdrop(backdrop: GlassBackdrop): Modifier =
    if (backdrop.kyant != null && backdrop.view === LocalView.current) layerBackdrop(backdrop.kyant)
    else onGloballyPositioned { backdrop.origin = it.positionInRoot() }
        .drawWithContent {
            var hardware = false
            drawIntoCanvas { hardware = it.nativeCanvas.isHardwareAccelerated }
            if (!hardware) { drawContent(); return@drawWithContent }
            val parent = this
            backdrop.layer.record(this, layoutDirection, IntSize(
                size.width.roundToInt().coerceAtLeast(1), size.height.roundToInt().coerceAtLeast(1))) {
                parent.drawContent()
            }
            drawLayer(backdrop.layer)
        }

/**
 * Render actual captured Compose content inside an independently positioned glass surface.
 * API 33+: platform blur chained into the TrainPilot lens shader (cached program, uniforms only).
 * API 31–32 (2810/6): platform blur + colour matrix (vibrancy, then the contrast scale
 * [GlassOptics.blurOnlyScale]) + white-ish tint + vector rim. Older / software: opaque paint.
 */
@Composable
internal fun Modifier.liveGlassLens(backdrop: GlassBackdrop, palette: TrainPilotPalette, shape: Shape): Modifier {
    if (!palette.isLiquidGlass) return this
    if (backdrop.view !== LocalView.current) return trainPilotGlassMaterial(palette, shape, GlassMaterialRole.Selection)
    val enabled = LocalGlassOpticsEnabled.current
    if (enabled && backdrop.kyant != null && backdrop.view === LocalView.current) {
        return kyantGlass(rememberKyantScene(backdrop.kyant, samplesContent = true), palette, shape,
            GlassMaterialRole.Selection)
    }
    val lensLayer = rememberGraphicsLayer()
    // Read only while drawing: moving the lens (scroll, sliding pill) re-records, never recompiles.
    val origin = remember { mutableStateOf(Offset.Zero) }
    val optics = remember(palette.glassClarity) { GlassOptics.from(palette.glassClarity) }
    return onGloballyPositioned { origin.value = it.positionInRoot() }
        .drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            val path = Path().apply { addOutline(outline) }
            val radii = outline.glassCornerRadii()
            val dimensions = IntSize(size.width.roundToInt().coerceAtLeast(1), size.height.roundToInt().coerceAtLeast(1))
            val effect = if (!enabled) null else if (Build.VERSION.SDK_INT >= 33) runCatching {
                GlassPrograms33.contentEffect(dimensions.width, dimensions.height, radii, optics, density = density, highlight = 0f)
            }.getOrNull() else if (Build.VERSION.SDK_INT >= 31) runCatching {
                createContentBlur31(optics.blurDp.dp.toPx(), optics)
            }.getOrNull() else null
            lensLayer.renderEffect = effect
            val fallback = Brush.verticalGradient(listOf(palette.background, palette.customPageEnd ?: palette.background))
            onDrawWithContent {
                clipPath(path) {
                    var hardware = false
                    drawIntoCanvas { hardware = it.nativeCanvas.isHardwareAccelerated }
                    val live = effect != null && hardware && backdrop.layer.size.width > 0 && backdrop.layer.size.height > 0 &&
                        runCatching {
                            val relative = origin.value - backdrop.origin
                            lensLayer.record(this, layoutDirection, dimensions) {
                                drawRect(palette.surface)
                                translate(left = -relative.x, top = -relative.y) { drawLayer(backdrop.layer) }
                            }
                            drawLayer(lensLayer)
                        }.isSuccess
                    if (live && Build.VERSION.SDK_INT < 33) {
                        // The colour matrix bounds every channel; the thin tint stays under the ceiling.
                        drawRect(GlassOptics.TINT.copy(alpha = optics.coat))
                        drawGlassHighlights(path, palette, optics, 0f)
                    } else if (!live) {
                        drawRect(fallback)
                        drawRect(glassFallbackPaint(palette.glassClarity))
                        drawGlassHighlights(path, palette, optics, 0f)
                    }
                }
                drawContent()
            }
        }
}

@RequiresApi(31)
private fun createContentBlur31(radius: Float, optics: GlassOptics): androidx.compose.ui.graphics.RenderEffect {
    val blur = RenderEffect.createBlurEffect(radius.coerceAtLeast(.5f), radius.coerceAtLeast(.5f), Shader.TileMode.CLAMP)
    val filter = android.graphics.ColorMatrixColorFilter(glassVibrancyMatrix(optics.saturation, optics.blurOnlyScale))
    // Blur first, then vibrancy + contrast scale (the effect input is applied before the filter).
    return RenderEffect.createColorFilterEffect(filter, blur).asComposeRenderEffect()
}

/** Saturation boost, then every channel scaled by [scale] (the API 31–32 contrast bound). */
internal fun glassVibrancyMatrix(saturation: Float, scale: Float): FloatArray {
    val matrix = android.graphics.ColorMatrix().apply { setSaturation(saturation) }
    matrix.postConcat(android.graphics.ColorMatrix().apply { setScale(scale, scale, scale, 1f) })
    return matrix.array
}

/** Nested header controls composite over the one header lens, including active workout. */
internal fun glassHeaderControlPaint(clarity: Int, role: GlassMaterialRole): Color =
    glassControlPaint(clarity, role)

@Composable
internal fun Modifier.glassHeaderControl(palette: TrainPilotPalette, shape: Shape, role: GlassMaterialRole): Modifier =
    if (!palette.isLiquidGlass) this else liquidDropMaterial(palette, shape, role, live = false)
