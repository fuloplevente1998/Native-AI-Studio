package com.example.ui.theme

import android.graphics.Paint
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** Independently authored material parameters. Blur is in normalized scene coordinates. */
internal data class GlassOptics(val clarity: Float, val blur: Float, val bend: Float, val coat: Float) {
    companion object {
        fun from(value: Int): GlassOptics {
            val t = value.coerceIn(0, 100) / 100f
            return GlassOptics(t, .085f * (1f - t) + .002f, .035f * t, .74f - .50f * t)
        }
    }
}

/**
 * Controlled backdrop: the same procedural scene is sampled by the lens shader.
 * This deliberately does not read pixels from other windows, protected content or
 * arbitrary Compose siblings. Content-underlay capture is a separate follow-up.
 * Only used by Liquid Glass; all other palettes keep their existing paint.
 */
@Composable
internal fun Modifier.trainPilotGlassScene(palette: TrainPilotPalette): Modifier =
    if (palette.isLiquidGlass) glassSceneModifier(palette, null) else this

@Composable
internal fun Modifier.trainPilotGlassMaterial(palette: TrainPilotPalette, shape: Shape): Modifier =
    if (palette.isLiquidGlass) glassSceneModifier(palette, shape) else this

@Composable
private fun Modifier.glassSceneModifier(palette: TrainPilotPalette, shape: Shape?): Modifier {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val sceneWidth = with(density) { configuration.screenWidthDp.dp.toPx() }.coerceAtLeast(1f)
    val sceneHeight = with(density) { configuration.screenHeightDp.dp.toPx() }.coerceAtLeast(1f)
    var origin by remember { mutableStateOf(Offset.Zero) }
    return onGloballyPositioned { coordinates -> origin = coordinates.positionInRoot() }
        .drawWithCache {
            val path = shape?.let { Path().apply { addOutline(it.createOutline(size, layoutDirection, this@drawWithCache)) } }
            val optics = GlassOptics.from(palette.glassClarity)
            val painter = if (Build.VERSION.SDK_INT >= 33) runCatching {
                GlassShader33(palette, optics, origin, sceneWidth, sceneHeight, size.width, size.height, shape != null)
            }.getOrNull() else null
            val fallback = Brush.verticalGradient(listOf(palette.background, palette.customPageEnd ?: palette.background))
            val rim = Brush.verticalGradient(listOf(Color.White.copy(alpha = .7f),
                palette.cardBorder.copy(alpha = .32f), palette.primary.copy(alpha = .55f)))
            fun DrawScope.paint() {
                val painted = painter != null && Build.VERSION.SDK_INT >= 33 && runCatching {
                    drawIntoCanvas { canvas ->
                        check(canvas.nativeCanvas.isHardwareAccelerated)
                        painter.draw(canvas.nativeCanvas)
                    }
                }.isSuccess
                if (!painted) {
                    drawRect(fallback)
                    if (shape != null) drawRect(palette.cardPaint)
                }
            }
            onDrawWithContent {
                if (path == null) paint()
                else clipPath(path) { paint() }
                drawContent()
                if (path != null) clipPath(path) { drawPath(path, rim, style = Stroke(1.5.dp.toPx())) }
            }
        }
}

/** Public Android APIs only. AGSL strings are original TrainPilot code. */
@RequiresApi(33)
private class GlassShader33(
    palette: TrainPilotPalette, optics: GlassOptics, origin: Offset,
    sceneWidth: Float, sceneHeight: Float, width: Float, height: Float, lens: Boolean
) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val w = width
    private val h = height
    init {
        val scene = RuntimeShader(SCENE)
        scene.setFloatUniform("screen", sceneWidth, sceneHeight)
        scene.setFloatUniform("origin", origin.x, origin.y)
        scene.setColorUniform("base", palette.background.toArgb())
        scene.setColorUniform("accent", palette.primary.toArgb())
        paint.shader = if (!lens) scene else RuntimeShader(LENS).apply {
            setInputShader("scene", scene)
            setFloatUniform("extent", width.coerceAtLeast(1f), height.coerceAtLeast(1f))
            setFloatUniform("screen", sceneWidth, sceneHeight)
            setFloatUniform("spread", optics.blur)
            setFloatUniform("bend", optics.bend)
            setFloatUniform("coat", optics.coat)
            setFloatUniform("clarity", optics.clarity)
            setColorUniform("tint", palette.surface.toArgb())
        }
    }
    fun draw(canvas: android.graphics.Canvas) { canvas.drawRect(0f, 0f, w, h, paint) }

    private companion object {
        const val SCENE = """
            uniform float2 screen;
            uniform float2 origin;
            layout(color) uniform half4 base;
            layout(color) uniform half4 accent;

            half4 main(float2 pos) {
                float2 uv = clamp((pos + origin) / screen, 0.0, 1.0);
                float ribbon = 0.5 + 0.5 * sin(uv.y * 12.0 + sin(uv.x * 5.0) * 1.7);
                float pool = exp(-5.0 * dot(uv - float2(0.72, 0.24), uv - float2(0.72, 0.24)));
                float amount = 0.045 * ribbon + 0.045 * pool;
                half3 color = mix(base.rgb * half(0.70 + 0.30 * (1.0 - uv.y)), accent.rgb, half(amount));
                return half4(color, 1.0);
            }
        """
        const val LENS = """
            uniform shader scene;
            uniform float2 extent;
            uniform float2 screen;
            uniform float spread;
            uniform float bend;
            uniform float coat;
            uniform float clarity;
            layout(color) uniform half4 tint;

            half4 main(float2 pos) {
                float2 uv = pos / extent;
                float2 centered = uv * 2.0 - 1.0;
                float edge = pow(clamp(max(abs(centered.x), abs(centered.y)), 0.0, 1.0), 4.0);
                float2 samplePos = pos - centered * edge * bend * min(extent.x, extent.y);
                float2 d = min(screen * spread, extent * 0.45);
                half3 color = scene.eval(samplePos).rgb * 0.24;
                color += (scene.eval(samplePos + float2(d.x, 0)).rgb +
                          scene.eval(samplePos - float2(d.x, 0)).rgb +
                          scene.eval(samplePos + float2(0, d.y)).rgb +
                          scene.eval(samplePos - float2(0, d.y)).rgb) * 0.12;
                color += (scene.eval(samplePos + d).rgb + scene.eval(samplePos - d).rgb +
                          scene.eval(samplePos + float2(d.x, -d.y)).rgb +
                          scene.eval(samplePos + float2(-d.x, d.y)).rgb) * 0.07;
                color = mix(color, tint.rgb, half(coat));
                float glint = pow(1.0 - clamp(uv.y, 0.0, 1.0), 10.0) * (0.03 + clarity * 0.06);
                return half4(mix(color, half3(1.0), half(glint)), 1.0);
            }
        """
    }
}
