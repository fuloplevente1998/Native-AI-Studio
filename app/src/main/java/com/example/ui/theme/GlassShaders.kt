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
    private var scenePhotoProgram: RuntimeShader? = null
    private var sceneLensProgram: RuntimeShader? = null
    private var contentLensProgram: RuntimeShader? = null

    private fun compile(source: String): RuntimeShader? {
        if (failed) return null
        return runCatching { RuntimeShader(source) }.onFailure { failed = true }.getOrNull()
    }
    fun scene(): RuntimeShader? = sceneProgram ?: compile(GlassShaderSource.SCENE)?.also { sceneProgram = it }
    fun scenePhoto(): RuntimeShader? = scenePhotoProgram ?: compile(GlassShaderSource.SCENE_PHOTO)?.also { scenePhotoProgram = it }
    fun sceneLens(): RuntimeShader? = sceneLensProgram ?: compile(GlassShaderSource.SCENE_LENS)?.also { sceneLensProgram = it }
    fun contentLens(): RuntimeShader? = contentLensProgram ?: compile(GlassShaderSource.CONTENT_LENS)?.also { contentLensProgram = it }

    /**
     * 2809/1: the living backdrop (blobs drifting with [seconds]) or, with [photo], the user image.
     * [guard] = the page itself (luminance guard on); the lens samples it unguarded and limits its own
     * backdrop at [GlassOptics.backdropCap] after the vibrancy boost (2810/6).
     */
    fun boundScene(sceneWidth: Float, sceneHeight: Float, originX: Float, originY: Float,
        palette: TrainPilotPalette, seconds: Float, photo: Shader?, guard: Boolean): RuntimeShader? {
        val shader = (if (photo != null) scenePhoto() else scene()) ?: return null
        shader.setFloatUniform("screen", sceneWidth.coerceAtLeast(1f), sceneHeight.coerceAtLeast(1f))
        shader.setFloatUniform("origin", originX, originY)
        shader.setFloatUniform("lumaCap", GlassScene.lumaCap)
        shader.setFloatUniform("knee", GlassScene.KNEE)
        shader.setFloatUniform("guard", if (guard) 1f else 0f)
        shader.setColorUniform("base", palette.background.toArgb())
        if (photo != null) {
            shader.setInputShader("photo", photo)
            return shader
        }
        val tones = GlassScene.tones(palette)
        GlassScene.blobs(sceneWidth, sceneHeight, seconds).forEachIndexed { i, blob ->
            shader.setFloatUniform("blob$i", blob.x, blob.y, blob.radius, blob.strength)
            shader.setColorUniform("tone$i", tones[i].toArgb())
        }
        return shader
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
        shader.setFloatUniform("backdropCap", optics.backdropCap)
        shader.setFloatUniform("ceiling", GlassOptics.SURFACE_LUMA_MAX)
        shader.setFloatUniform("lightDir", LIGHT_X, LIGHT_Y)
        shader.setColorUniform("tint", tint.toArgb())
    }

    /** Live content lens: smooth platform blur first, then refraction/rim/tint on the blurred copy. */
    fun contentEffect(width: Int, height: Int, radii: FloatArray, optics: GlassOptics, tint: Color = GlassOptics.TINT,
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
        highlight: Float, seconds: Float = 0f, photo: Shader? = null): Shader? {
        val lens = sceneLens() ?: return null
        val scene = boundScene(sceneWidth, sceneHeight, originX, originY, palette, seconds, photo, guard = false) ?: return null
        // Rebind after the child's uniforms changed: the lens snapshots the child here.
        lens.setInputShader("scene", scene)
        // 2810/6: strong diffusion (20–32 dp) of the soft scene, capped for small chips.
        val frost = min(optics.blur * min(sceneWidth, sceneHeight), min(width, height) * .45f)
        bindMaterial(lens, width, height, radii, optics, GlassOptics.TINT, density, frost, highlight)
        return lens
    }

    // Light from the upper left (y grows downwards).
    private const val LIGHT_X = -.48f
    private const val LIGHT_Y = -.877f
}

/**
 * Original TrainPilot AGSL. General optics only: a rounded-rectangle distance field gives the
 * surface normal; the rim refracts (stronger at the edge, flat clear center), splits color only
 * inside the rim, gets an inner depth shade and a thin specular edge (strongest on top).
 * 2810/6 order (the layering public Apache-2.0 glass engines use, own code): blur -> refraction ->
 * vibrancy -> backdrop luminance limit ([GlassOptics.backdropCap]) -> white-ish tint -> safety
 * ceiling ([GlassOptics.SURFACE_LUMA_MAX], a no-op inside the model) -> rim light.
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
        uniform float backdropCap;
        uniform float ceiling;
        uniform float2 lightDir;
        layout(color) uniform half4 tint;
    """

    private const val MATERIAL_BODY = """
        float3 lensLinear(float3 c) {
            return mix(c / 12.92, pow((c + float3(0.055)) / 1.055, float3(2.4)), step(float3(0.04045), c));
        }

        float3 lensSrgb(float3 l) {
            return mix(l * 12.92, 1.055 * pow(max(l, float3(0.0)), float3(1.0 / 2.4)) - float3(0.055),
                step(float3(0.0031308), l));
        }

        // Hard luminance limit in linear light (hue and saturation kept), mirrored by GlassContrast.
        float3 limitLuma(float3 c, float cap) {
            float3 lin = lensLinear(clamp(c, 0.0, 1.0));
            float l = dot(lin, float3(0.2126, 0.7152, 0.0722));
            if (l <= cap) { return clamp(c, 0.0, 1.0); }
            return lensSrgb(lin * (max(cap, 0.0) / l));
        }

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
            c = limitLuma(c, backdropCap);
            c = mix(c, float3(tint.rgb), coat);
            c = limitLuma(c, ceiling);

            float band = 1.0 - smoothstep(0.0, rim * 0.9, -sd);
            float away = 0.5 + 0.5 * dot(n, -lightDir);
            c = c * (1.0 - shade * band * band * (0.45 + 0.55 * away));

            // Thin specular edge: ~1 px line plus a 3 px glow, top strongest, sides and bottom weaker.
            // It stays inside the padding, never under text.
            float edgeLine = 1.0 - smoothstep(0.0, 1.25 * px, -sd);
            float glow = 1.0 - smoothstep(0.0, 3.0 * px, -sd);
            float topness = clamp(-n.y, 0.0, 1.0);
            float side = 1.0 - abs(n.y);
            float rimAlpha = specular * (0.27 + 0.73 * topness + 0.13 * side)
                + 0.08 * specular * max(dot(n, lightDir), 0.0);
            float shine = edgeLine * rimAlpha + glow * glow * rimAlpha * 0.30 * topness;
            c = mix(c, float3(1.0), clamp(shine, 0.0, 0.60));
            return half4(half3(c), 1.0);
        }
    """

    /** 2809/1: exact sRGB luminance guard, mirrored by [GlassScene.guard]. */
    private const val SCENE_GUARD = """
        uniform float2 screen;
        uniform float2 origin;
        uniform float lumaCap;
        uniform float knee;
        uniform float guard;
        layout(color) uniform half4 base;

        float3 toLinear(float3 c) {
            return mix(c / 12.92, pow((c + float3(0.055)) / 1.055, float3(2.4)), step(float3(0.04045), c));
        }

        float3 toSrgb(float3 l) {
            return mix(l * 12.92, 1.055 * pow(max(l, float3(0.0)), float3(1.0 / 2.4)) - float3(0.055),
                step(float3(0.0031308), l));
        }

        float3 guardLuma(float3 c) {
            c = clamp(c, 0.0, 1.0);
            float3 lin = toLinear(c);
            float l = dot(lin, float3(0.2126, 0.7152, 0.0722));
            float k = lumaCap * knee;
            if (l <= k) { return c; }
            float mapped = k + (lumaCap - k) * (1.0 - exp((k - l) / (lumaCap - k)));
            return toSrgb(lin * (mapped / l));
        }

        float3 finish(float3 c) {
            if (guard > 0.5) { c = guardLuma(c); }
            return c;
        }
    """

    /** Living backdrop: a top-lit palette base and four soft palette blobs that drift slowly. */
    const val SCENE = """
        $SCENE_GUARD
        uniform float4 blob0;
        uniform float4 blob1;
        uniform float4 blob2;
        uniform float4 blob3;
        layout(color) uniform half4 tone0;
        layout(color) uniform half4 tone1;
        layout(color) uniform half4 tone2;
        layout(color) uniform half4 tone3;

        float3 addBlob(float3 c, float2 p, float4 b, half4 tone) {
            float2 d = (p - b.xy) / max(b.z, 1.0);
            float w = clamp(b.w * exp(-2.2 * dot(d, d)), 0.0, 1.0);
            return mix(c, float3(tone.rgb), w);
        }

        half4 main(float2 pos) {
            float2 p = pos + origin;
            float v = clamp(p.y / screen.y, 0.0, 1.0);
            float3 c = float3(base.rgb) * (0.80 + 0.20 * (1.0 - v));
            c = addBlob(c, p, blob0, tone0);
            c = addBlob(c, p, blob1, tone1);
            c = addBlob(c, p, blob2, tone2);
            c = addBlob(c, p, blob3, tone3);
            return half4(half3(finish(c)), 1.0);
        }
    """

    /** Optional user image (2809/1 B), centre-cropped in root pixels, guarded on the page. */
    const val SCENE_PHOTO = """
        uniform shader photo;
        $SCENE_GUARD

        half4 main(float2 pos) {
            float2 p = pos + origin;
            float v = clamp(p.y / screen.y, 0.0, 1.0);
            float3 c = mix(float3(photo.eval(p).rgb), float3(base.rgb), 0.16) * (0.88 + 0.12 * (1.0 - v));
            return half4(half3(finish(c)), 1.0);
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
