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
            val path = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache)) }
            val relative = origin - backdrop.origin
            val dimensions = IntSize(size.width.roundToInt().coerceAtLeast(1), size.height.roundToInt().coerceAtLeast(1))
            val optics = GlassOptics.from(palette.glassClarity)
            val effect = if (!enabled) null else if (Build.VERSION.SDK_INT >= 33) runCatching {
                createContentLensEffect33(dimensions, optics, palette.surface, 1.dp.toPx())
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
                        lensLayer.record(this, layoutDirection, dimensions) {
                            drawRect(palette.surface)
                            translate(left = -relative.x, top = -relative.y) { drawLayer(backdrop.layer) }
                        }
                        drawLayer(lensLayer)
                        // Older platform blur has no shader luminance guard.
                        if (Build.VERSION.SDK_INT < 33) drawRect(palette.surface.copy(alpha = .84f))
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
private fun createContentLensEffect33(size: IntSize, optics: GlassOptics, tint: Color, density: Float): androidx.compose.ui.graphics.RenderEffect {
    val shader = RuntimeShader(CONTENT_LENS)
    shader.setFloatUniform("extent", size.width.toFloat(), size.height.toFloat())
    shader.setFloatUniform("spread", (1f + (1f - optics.clarity) * 15f) * density)
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
    layout(color) uniform half4 tint;

    half3 readScene(float2 p) {
        return underlay.eval(clamp(p, float2(0.5), extent - float2(0.5))).rgb;
    }

    half4 main(float2 pos) {
        float2 n = pos / extent * 2.0 - 1.0;
        float edge = pow(clamp(max(abs(n.x), abs(n.y)), 0.0, 1.0), 4.0);
        float2 at = pos - n * edge * bend * min(extent.x, extent.y);
        float2 d = float2(spread, spread);
        half3 c = readScene(at) * 0.24;
        c += (readScene(at + float2(d.x, 0)) + readScene(at - float2(d.x, 0))
           + readScene(at + float2(0, d.y)) + readScene(at - float2(0, d.y))) * 0.12;
        c += (readScene(at + d) + readScene(at - d)
           + readScene(at + float2(d.x, -d.y)) + readScene(at + float2(-d.x, d.y))) * 0.07;
        c = mix(c, tint.rgb, half(coat));
        // Keep opaque white foreground readable even over bright source content.
        half ceiling = max(max(c.r, c.g), c.b);
        c *= min(half(1.0), half(0.40) / max(ceiling, half(0.001)));
        return half4(c, 1.0);
    }
"""
