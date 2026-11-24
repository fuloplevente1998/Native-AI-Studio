package com.example.ui.theme

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
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

@Composable
internal fun Modifier.glassHeaderMaterial(palette: TrainPilotPalette, shape: Shape): Modifier {
    val backdrop = LocalGlassHeaderBackdrop.current
    return if (backdrop != null) liveGlassLens(backdrop, palette, shape)
    else trainPilotGlassMaterial(palette, shape)
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

/** Render actual captured Compose content inside an independently positioned glass surface. */
@Composable
internal fun Modifier.liveGlassLens(backdrop: GlassBackdrop, palette: TrainPilotPalette, shape: Shape): Modifier {
    if (!palette.isLiquidGlass) return this
    val enabled = LocalGlassOpticsEnabled.current
    val lensLayer = rememberGraphicsLayer()
    var origin by remember { mutableStateOf(Offset.Zero) }
    return onGloballyPositioned { origin = it.positionInRoot() }
        .drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            val path = Path().apply { addOutline(outline) }
            val corner = (outline as? Outline.Rounded)?.roundRect?.topLeftCornerRadius?.x ?: 0f
            val relative = origin - backdrop.origin
            val dimensions = IntSize(size.width.roundToInt().coerceAtLeast(1), size.height.roundToInt().coerceAtLeast(1))
            val optics = GlassOptics.from(palette.glassClarity)
            val effect = if (!enabled) null else if (Build.VERSION.SDK_INT >= 33) runCatching {
                createContentLensEffect33(dimensions, optics, palette.surface, 1.dp.toPx(), corner)
            }.getOrNull() else if (Build.VERSION.SDK_INT >= 31) runCatching {
                createContentBlur31((1f + (1f - optics.clarity) * 16f).dp.toPx())
            }.getOrNull() else null
            lensLayer.renderEffect = effect
            val edge = Brush.verticalGradient(listOf(Color.White.copy(alpha = .8f), palette.cardBorder.copy(alpha = .4f),
                palette.primary.copy(alpha = .6f)))
            onDrawWithContent {
                clipPath(path) {
                    drawRect(palette.surface)
                    var hardware = false
                    drawIntoCanvas { hardware = it.nativeCanvas.isHardwareAccelerated }
                    if (effect != null && hardware && backdrop.layer.size.width > 0 && backdrop.layer.size.height > 0) {
                        runCatching {
                            lensLayer.record(this, layoutDirection, dimensions) {
                                drawRect(palette.surface)
                                translate(left = -relative.x, top = -relative.y) { drawLayer(backdrop.layer) }
                            }
                            drawLayer(lensLayer)
                            // Older platform blur has no shader luminance guard.
                            if (Build.VERSION.SDK_INT < 33) drawRect(palette.surface.copy(alpha = .84f))
                        }.onFailure { drawRect(palette.surface) }
                    }
                }
                drawContent()
                clipPath(path) { drawPath(path, edge, style = Stroke(2.dp.toPx())) }
            }
        }
}

@RequiresApi(31)
private fun createContentBlur31(radius: Float): androidx.compose.ui.graphics.RenderEffect =
    RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP).asComposeRenderEffect()

@RequiresApi(33)
private fun createContentLensEffect33(size: IntSize, optics: GlassOptics, tint: Color, density: Float, corner: Float): androidx.compose.ui.graphics.RenderEffect {
    val shader = RuntimeShader(CONTENT_LENS)
    shader.setFloatUniform("extent", size.width.toFloat(), size.height.toFloat())
    shader.setFloatUniform("spread", (.35f + (1f - optics.clarity) * 20f) * density)
    shader.setFloatUniform("corner", corner)
    shader.setFloatUniform("clarity", optics.clarity)
    shader.setFloatUniform("bend", optics.bend)
    shader.setFloatUniform("coat", optics.coat)
    shader.setColorUniform("tint", tint.toArgb())
    return RenderEffect.createRuntimeShaderEffect(shader, "underlay").asComposeRenderEffect()
}

/** Original lens approximation, using normalized distances and bounded samples. */
private const val CONTENT_LENS = """
    uniform shader underlay;
    uniform float2 extent;
    uniform float spread;
    uniform float bend;
    uniform float coat;
    uniform float corner;
    uniform float clarity;
    layout(color) uniform half4 tint;

    half3 readScene(float2 p) {
        return underlay.eval(clamp(p, float2(0.5), extent - float2(0.5))).rgb;
    }

    half4 main(float2 pos) {
        // Signed distance to a rounded rectangle: refraction follows the actual curved edge.
        float2 h = extent * 0.5;
        float2 p = pos - h;
        float radius = clamp(corner, 0.0, min(h.x, h.y));
        float2 q = abs(p) - (h - float2(radius));
        float2 o = max(q, float2(0.0));
        float distanceToEdge = length(o) + min(max(q.x, q.y), 0.0) - radius;
        float2 n = length(o) > 0.001 ? normalize(o) * sign(p) :
            (q.x > q.y ? float2(sign(p.x), 0.0) : float2(0.0, sign(p.y)));
        float bevel = max(2.0, min(h.x, h.y) * 0.44);
        float edge = clamp(1.0 + distanceToEdge / bevel, 0.0, 1.0);
        float curve = edge * edge * (3.0 - 2.0 * edge);
        float2 offset = n * curve * bend * min(extent.x, extent.y);
        float2 at = pos - offset;
        float2 d = float2(spread, spread);
        half3 sharp = readScene(at);
        half3 c = sharp * 0.24;
        c += (readScene(at + float2(d.x, 0)) + readScene(at - float2(d.x, 0))
           + readScene(at + float2(0, d.y)) + readScene(at - float2(0, d.y))) * 0.12;
        c += (readScene(at + d) + readScene(at - d)
           + readScene(at + float2(d.x, -d.y)) + readScene(at + float2(-d.x, d.y))) * 0.07;
        // Clear glass keeps the center sharp; dispersion is restricted to the curved bevel.
        float2 split = offset * 0.11 * clarity;
        half3 spectral = half3(readScene(at + split).r, sharp.g, readScene(at - split).b);
        c = mix(c, spectral, half(clarity * clarity * 0.88));
        c = mix(c, tint.rgb, half(coat));
        // Preserve readable dark material without flattening all bright pixels to the same value.
        half peak = max(max(c.r, c.g), c.b);
        c *= half(0.42) / max(half(0.42), peak);
        float reflection = max(dot(n, normalize(float2(-0.65, -1.0))), 0.0);
        float opposite = max(dot(n, normalize(float2(0.65, 1.0))), 0.0);
        float gleam = pow(edge, 8.0) * (0.06 + clarity * 0.24) *
            (reflection + opposite * 0.36);
        return half4(mix(c, half3(1.0), half(gleam)), 1.0);
    }
"""
