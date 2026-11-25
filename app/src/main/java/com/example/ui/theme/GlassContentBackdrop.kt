package com.example.ui.theme

import android.graphics.RenderEffect
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Explicit, local backdrop capture. The caller identifies the source content and the lens.
 * The captured layer excludes the lens and all foreground labels, preventing a feedback loop.
 * No screenshots, window capture, bitmaps, readback, or external rendering library.
 */
@Stable
internal class GlassBackdrop internal constructor(internal val layer: GraphicsLayer) {
    internal var origin by mutableStateOf(Offset.Zero)
}

internal val LocalGlassHeaderBackdrop = staticCompositionLocalOf<GlassBackdrop?> { null }
internal val LocalGlassTopInset = compositionLocalOf { 0.dp }

/**
 * Header, nav and selection glass. 2811/1 device round 3: every route draws the SAME material as the calendar
 * header (the page scene through [trainPilotGlassMaterial], proven continuous with the page on device); routes
 * whose content scrolls under the header add only that content, blurred and luminance-bounded, on top
 * ([glassContentOverlay]). The former full content lens (recorded page + AGSL `CONTENT_LENS`) rendered as a dark
 * flat band on device and is no longer used for the header.
 */
@Composable
internal fun Modifier.glassHeaderMaterial(palette: TrainPilotPalette, shape: Shape,
    role: GlassRole = GlassRole.Navigation): Modifier {
    val backdrop = LocalGlassHeaderBackdrop.current
    val material = trainPilotGlassMaterial(palette, shape, role)
    return if (backdrop != null) material.glassContentOverlay(backdrop, palette, shape) else material
}

/**
 * Channel scale of the scrolled content shown through the header: even white content stays at the page
 * ceiling (the Navigation role's API 31–32 bound: no tint, no vibrancy), so glyphs on the header keep >= 4.5:1.
 * Where nothing is under the header the recorded layer is transparent and the header equals the page.
 */
internal fun glassOverlayScale(clarity: Int): Float = GlassOptics.from(clarity, GlassRole.Navigation).blurOnlyScale

/**
 * 2811/1: only the recorded content (the capture sits inside the page scene, see [GlassAwareScaffold]), platform
 * blur + uniform channel scale, clipped to [shape]. No fill, tint or luminance clamp of its own over the page.
 */
@Composable
internal fun Modifier.glassContentOverlay(backdrop: GlassBackdrop, palette: TrainPilotPalette, shape: Shape): Modifier {
    if (!palette.isLiquidGlass || Build.VERSION.SDK_INT < 31) return this
    val enabled = LocalGlassOpticsEnabled.current
    val overlay = rememberGraphicsLayer()
    val origin = remember { mutableStateOf(Offset.Zero) }
    val optics = remember(palette.glassClarity) { GlassOptics.from(palette.glassClarity, GlassRole.Navigation) }
    return onGloballyPositioned { origin.value = it.positionInRoot() }
        .drawWithCache {
            val path = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }
            val dimensions = IntSize(size.width.roundToInt().coerceAtLeast(1), size.height.roundToInt().coerceAtLeast(1))
            val effect = if (!enabled || Build.VERSION.SDK_INT < 31) null else runCatching {
                createContentBlur31(optics.blurDp.dp.toPx(), glassOverlayScale(palette.glassClarity))
            }.getOrNull()
            overlay.renderEffect = effect
            onDrawWithContent {
                if (effect != null && backdrop.layer.size.width > 0 && backdrop.layer.size.height > 0) clipPath(path) {
                    runCatching {
                        val relative = origin.value - backdrop.origin
                        overlay.record(this, layoutDirection, dimensions) {
                            translate(left = -relative.x, top = -relative.y) { drawLayer(backdrop.layer) }
                        }
                        drawLayer(overlay)
                    }
                }
                drawContent()
            }
        }
}

@Composable
internal fun rememberGlassBackdrop(): GlassBackdrop {
    val layer = rememberGraphicsLayer()
    return remember(layer) { GlassBackdrop(layer) }
}

@Composable
internal fun Modifier.captureGlassBackdrop(backdrop: GlassBackdrop): Modifier =
    onGloballyPositioned { backdrop.origin = it.positionInRoot() }
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
internal fun Modifier.liveGlassLens(backdrop: GlassBackdrop, palette: TrainPilotPalette, shape: Shape,
    requested: GlassRole = GlassRole.Panel): Modifier {
    if (!palette.isLiquidGlass) return this
    val enabled = LocalGlassOpticsEnabled.current
    val lensLayer = rememberGraphicsLayer()
    // Read only while drawing: moving the lens (scroll, sliding pill) re-records, never recompiles.
    val origin = remember { mutableStateOf(Offset.Zero) }
    val role = GlassRole.resolve(requested, enabled)
    val optics = remember(palette.glassClarity, role) { GlassOptics.from(palette.glassClarity, role) }
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
private fun createContentBlur31(radius: Float, optics: GlassOptics): androidx.compose.ui.graphics.RenderEffect =
    createContentBlur31(radius, optics.blurOnlyScale, optics.saturation, Shader.TileMode.CLAMP)

/**
 * Blur, then saturation + uniform scale. 2811/1: the header overlay uses saturation 1 (no vibrancy) and DECAL
 * edges (outside the recorded content is transparent, so the header edge never smears content in).
 */
@RequiresApi(31)
private fun createContentBlur31(radius: Float, scale: Float, saturation: Float = 1f,
    tile: Shader.TileMode = Shader.TileMode.DECAL): androidx.compose.ui.graphics.RenderEffect {
    val blur = RenderEffect.createBlurEffect(radius.coerceAtLeast(.5f), radius.coerceAtLeast(.5f), tile)
    val filter = android.graphics.ColorMatrixColorFilter(glassVibrancyMatrix(saturation, scale))
    // Blur first, then vibrancy + contrast scale (the effect input is applied before the filter).
    return RenderEffect.createColorFilterEffect(filter, blur).asComposeRenderEffect()
}

/** Saturation boost, then every channel scaled by [scale] (the API 31–32 contrast bound). */
internal fun glassVibrancyMatrix(saturation: Float, scale: Float): FloatArray {
    val matrix = android.graphics.ColorMatrix().apply { setSaturation(saturation) }
    matrix.postConcat(android.graphics.ColorMatrix().apply { setScale(scale, scale, scale, 1f) })
    return matrix.array
}
