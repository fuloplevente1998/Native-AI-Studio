package com.example.ui.theme

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.toArgb
import kotlin.math.min

/** Corner radii (top-left, top-right, bottom-right, bottom-left) of a drawn outline, in px. */
internal fun Outline.glassCornerRadii(): FloatArray = when (this) {
    is Outline.Rounded -> roundRect.run {
        floatArrayOf(topLeftCornerRadius.x, topRightCornerRadius.x, bottomRightCornerRadius.x, bottomLeftCornerRadius.x)
    }
    else -> FloatArray(4)
}

/**
 * Compiled once per process and reused by every glass surface. Only uniforms change while
 * scrolling or dragging the slider. Each draw call / RenderEffect snapshots the uniforms, so one
 * shared instance can serve many surfaces on the UI thread. A compile failure disables the
 * programs for good (opaque fallback) instead of retrying every frame.
 */
@RequiresApi(33)
internal object GlassPrograms33 {
    private var failed = false
    private var sceneProgram: RuntimeShader? = null
    private var sceneLensProgram: RuntimeShader? = null
    private var contentLensProgram: RuntimeShader? = null

    private fun compile(source: String): RuntimeShader? {
        if (failed) return null
        return runCatching { RuntimeShader(source) }.onFailure { failed = true }.getOrNull()
    }
    fun scene(): RuntimeShader? = sceneProgram ?: compile(GlassShaderSource.SCENE)?.also { sceneProgram = it }
    fun sceneLens(): RuntimeShader? = sceneLensProgram ?: compile(GlassShaderSource.SCENE_LENS)?.also { sceneLensProgram = it }
    fun contentLens(): RuntimeShader? = contentLensProgram ?: compile(GlassShaderSource.CONTENT_LENS)?.also { contentLensProgram = it }

    fun bindScene(shader: RuntimeShader, sceneWidth: Float, sceneHeight: Float, originX: Float, originY: Float,
        palette: TrainPilotPalette) {
        shader.setFloatUniform("screen", sceneWidth.coerceAtLeast(1f), sceneHeight.coerceAtLeast(1f))
        shader.setFloatUniform("origin", originX, originY)
        shader.setFloatUniform("accentMax", GlassOptics.SCENE_ACCENT_MAX)
        shader.setColorUniform("base", palette.background.toArgb())
        shader.setColorUniform("accent", palette.primary.toArgb())
    }

    fun bindMaterial(shader: RuntimeShader, width: Float, height: Float, radii: FloatArray, optics: GlassOptics,
        tint: Color, density: Float, frostPx: Float, highlight: Float) {
        shader.setFloatUniform("extent", width.coerceAtLeast(1f), height.coerceAtLeast(1f))
        shader.setFloatUniform("radii", radii[0], radii[1], radii[2], radii[3])
        shader.setFloatUniform("px", density.coerceAtLeast(.5f))
        shader.setFloatUniform("bend", optics.bend)
        shader.setFloatUniform("bevel", optics.bevel)
        shader.setFloatUniform("dispersion", optics.dispersion)
        shader.setFloatUniform("frost", frostPx)
        shader.setFloatUniform("coat", optics.coat)
        shader.setFloatUniform("saturation", optics.saturation)
        shader.setFloatUniform("specular", (optics.specular + highlight).coerceIn(0f, 1f))
        shader.setFloatUniform("shade", optics.shadow)
        shader.setFloatUniform("peak", GlassOptics.LENS_PEAK)
        shader.setFloatUniform("lightDir", LIGHT_X, LIGHT_Y)
        shader.setColorUniform("tint", tint.toArgb())
    }

    /** Live content lens: smooth platform blur first, then refraction/rim/tint on the blurred copy. */
    fun contentEffect(width: Int, height: Int, radii: FloatArray, optics: GlassOptics, tint: Color,
        density: Float, highlight: Float): androidx.compose.ui.graphics.RenderEffect? {
        val shader = contentLens() ?: return null
        bindMaterial(shader, width.toFloat(), height.toFloat(), radii, optics, tint, density, 0f, highlight)
        val lens = RenderEffect.createRuntimeShaderEffect(shader, "underlay")
        val radius = optics.blurDp * density
        val chained = if (radius < .5f) lens else RenderEffect.createChainEffect(lens,
            RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP))
        return chained.asComposeRenderEffect()
    }

    /** Procedural scene seen through the lens (cards, nav, sheets): returns the paint shader. */
    fun sceneMaterial(width: Float, height: Float, radii: FloatArray, sceneWidth: Float, sceneHeight: Float,
        originX: Float, originY: Float, palette: TrainPilotPalette, optics: GlassOptics, density: Float,
        highlight: Float): Shader? {
        val scene = scene() ?: return null
        val lens = sceneLens() ?: return null
        bindScene(scene, sceneWidth, sceneHeight, originX, originY, palette)
        // Rebind after the child's uniforms changed: the lens snapshots the child here.
        lens.setInputShader("scene", scene)
        val frost = min(optics.blur * min(sceneWidth, sceneHeight), min(width, height) * .45f)
        bindMaterial(lens, width, height, radii, optics, palette.surface, density, frost, highlight)
        return lens
    }

    // Light from the upper left (y grows downwards).
    private const val LIGHT_X = -.48f
    private const val LIGHT_Y = -.877f
}

/**
 * Original TrainPilot AGSL. General optics only: a rounded-rectangle distance field gives the
 * surface normal; the rim refracts (stronger at the edge, flat clear center), splits color only
 * inside the rim, gets an inner depth shade and a light-facing specular line. Output channels are
 * capped at [GlassOptics.LENS_PEAK] so palette text keeps its tested contrast.
 */
internal object GlassShaderSource {
    private const val MATERIAL_UNIFORMS = """
        uniform float2 extent;
        uniform float4 radii;
        uniform float px;
        uniform float bend;
        uniform float bevel;
        uniform float dispersion;
        uniform float frost;
        uniform float coat;
        uniform float saturation;
        uniform float specular;
        uniform float shade;
        uniform float peak;
        uniform float2 lightDir;
        layout(color) uniform half4 tint;
    """

    private const val MATERIAL_BODY = """
        float cornerRadius(float2 p, float2 h) {
            float r = p.y < 0.0 ? (p.x < 0.0 ? radii.x : radii.y) : (p.x < 0.0 ? radii.w : radii.z);
            return clamp(r, 0.0, min(h.x, h.y));
        }

        float boxDistance(float2 p, float2 h) {
            float r = cornerRadius(p, h);
            float2 q = abs(p) - (h - float2(r));
            return length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - r;
        }

        half4 glassAt(float2 pos) {
            float2 h = extent * 0.5;
            float2 p = pos - h;
            float sd = boxDistance(p, h);
            float2 dx = float2(1.0, 0.0);
            float2 dy = float2(0.0, 1.0);
            float2 g = float2(boxDistance(p + dx, h) - boxDistance(p - dx, h),
                              boxDistance(p + dy, h) - boxDistance(p - dy, h));
            float2 n = length(g) > 0.0001 ? normalize(g) : float2(0.0, -1.0);

            float rim = max(2.0 * px, min(h.x, h.y) * bevel);
            float inside = clamp(-sd / rim, 0.0, 1.0);
            float slope = (1.0 - inside) * (1.0 - inside);
            float2 shift = -n * slope * bend * min(extent.x, extent.y);
            float2 at = pos + shift;

            float3 c = backdrop(at);
            if (frost > 0.5) {
                float2 d = float2(frost, frost * 0.6);
                c = c * 0.28
                  + (backdrop(at + float2(d.x, 0.0)) + backdrop(at - float2(d.x, 0.0))
                   + backdrop(at + float2(0.0, d.x)) + backdrop(at - float2(0.0, d.x))) * 0.11
                  + (backdrop(at + d) + backdrop(at - d)
                   + backdrop(at + float2(d.y, -d.x)) + backdrop(at + float2(-d.y, d.x))) * 0.07;
            }

            float2 split = -n * slope * dispersion * rim;
            float redShift = backdrop(at + split).r;
            float blueShift = backdrop(at - split).b;
            c.r = mix(c.r, redShift, slope);
            c.b = mix(c.b, blueShift, slope);

            float luma = dot(c, float3(0.2126, 0.7152, 0.0722));
            c = clamp(mix(float3(luma), c, saturation), 0.0, 1.0);
            c = mix(c, float3(tint.rgb), coat);
            float top = max(max(c.r, c.g), c.b);
            float knee = peak * 0.55;
            float mapped = top <= knee ? top : knee + (peak - knee) * (1.0 - exp((knee - top) / (peak - knee)));
            c = c * (mapped / max(top, 0.0001));

            float band = 1.0 - smoothstep(0.0, rim * 0.9, -sd);
            float away = 0.5 + 0.5 * dot(n, -lightDir);
            c = c * (1.0 - shade * band * band * (0.45 + 0.55 * away));

            float facing = max(dot(n, lightDir), 0.0);
            float echo = max(dot(n, -lightDir), 0.0) * 0.4;
            float edgeLine = 1.0 - smoothstep(0.0, 1.25 * px, -sd);
            float spill = 1.0 - smoothstep(0.0, rim * 0.6, -sd);
            float sheen = 1.0 - smoothstep(0.0, extent.y * 0.45, pos.y);
            float shine = specular * (edgeLine * (0.45 + 0.55 * (facing + echo))
                + spill * spill * facing * 0.45 + sheen * 0.14);
            c = mix(c, float3(1.0), clamp(shine, 0.0, 0.85));
            return half4(half3(c), 1.0);
        }
    """

    const val SCENE = """
        uniform float2 screen;
        uniform float2 origin;
        uniform float accentMax;
        layout(color) uniform half4 base;
        layout(color) uniform half4 accent;

        half4 main(float2 pos) {
            float2 uv = clamp((pos + origin) / screen, 0.0, 1.0);
            float aspect = screen.y / max(screen.x, 1.0);
            float2 d1 = (uv - float2(0.80, 0.16)) * float2(1.0, aspect);
            float2 d2 = (uv - float2(0.10, 0.58)) * float2(1.0, aspect);
            float2 d3 = (uv - float2(0.72, 0.94)) * float2(1.0, aspect);
            float ribbon = 0.5 + 0.5 * sin(uv.y * 11.0 + sin(uv.x * 4.6 + uv.y * 2.0) * 1.8);
            float bright = min(0.05 * ribbon + 0.10 * exp(-6.0 * dot(d1, d1)) + 0.07 * exp(-8.0 * dot(d3, d3)),
                accentMax * 0.7);
            float deep = min(0.16 * exp(-7.0 * dot(d2, d2)), accentMax * 0.3);
            float3 c = float3(base.rgb) * (0.72 + 0.28 * (1.0 - uv.y));
            c = mix(c, float3(accent.rgb), bright);
            c = mix(c, float3(accent.rgb) * 0.5, deep);
            return half4(half3(c), 1.0);
        }
    """

    const val SCENE_LENS = """
        uniform shader scene;
        $MATERIAL_UNIFORMS
        float3 backdrop(float2 p) { return float3(scene.eval(p).rgb); }
        $MATERIAL_BODY
        half4 main(float2 pos) { return glassAt(pos); }
    """

    const val CONTENT_LENS = """
        uniform shader underlay;
        $MATERIAL_UNIFORMS
        float3 backdrop(float2 p) {
            return float3(underlay.eval(clamp(p, float2(0.5), extent - float2(0.5))).rgb);
        }
        $MATERIAL_BODY
        half4 main(float2 pos) { return glassAt(pos); }
    """
}
