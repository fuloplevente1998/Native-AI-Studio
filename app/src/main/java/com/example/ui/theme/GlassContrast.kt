package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 2810/6 contrast model of the white-ish Liquid Glass tint (own TrainPilot math, shared by the AGSL lens,
 * the Compose path and the tests). Text sits on the tint (alpha `coat`, colour [GlassOptics.TINT]) over a
 * backdrop. Readability is NOT bought with a darker tint: the backdrop's luminance under the glass is
 * limited ([backdropCap]) so the composite never exceeds [GlassOptics.SURFACE_LUMA_MAX], and the palette
 * text colours are lifted against that single ceiling (>= 4.5:1, outlines >= 3:1).
 */
internal object GlassContrast {
    private const val LEVELS = 7
    private const val ITERATIONS = 16
    /** The modelled composite stays a little under the ceiling; the shader clamp is the last safety net. */
    private const val MARGIN = .985f
    private val rec709 = floatArrayOf(.2126f, .7152f, .0722f)
    /** Android `ColorMatrix.setSaturation` weights (API 31–32 colour-matrix path). */
    private val matrixWeights = floatArrayOf(.213f, .715f, .072f)

    val target: Float get() = GlassOptics.SURFACE_LUMA_MAX * MARGIN

    fun luminance(r: Float, g: Float, b: Float): Float =
        rec709[0] * GlassScene.linear(r) + rec709[1] * GlassScene.linear(g) + rec709[2] * GlassScene.linear(b)

    /** Neutral grey of a given relative luminance: the worst case for light text of every hue. */
    fun grey(luminance: Float): Color = GlassScene.srgb(luminance.coerceIn(0f, 1f)).let { Color(it, it, it) }

    /** Luminance of tint (alpha [coat]) over backdrop channels r, g, b (sRGB alpha mix, as drawn). */
    fun composite(coat: Float, r: Float, g: Float, b: Float): Float {
        val w = GlassOptics.TINT
        return luminance(coat * w.red + (1f - coat) * r, coat * w.green + (1f - coat) * g, coat * w.blue + (1f - coat) * b)
    }

    /**
     * Brightest composite over every backdrop whose luminance is at most [cap]. The composite is convex in
     * the backdrop, so its maximum lies on the colour cube corners and the cap boundary; a 7-level grid
     * scaled down to the cap (in linear light, like the guard) covers both.
     */
    fun worstComposite(coat: Float, cap: Float): Float {
        var worst = 0f
        for (ri in 0 until LEVELS) for (gi in 0 until LEVELS) for (bi in 0 until LEVELS) {
            val r = ri / (LEVELS - 1f)
            val g = gi / (LEVELS - 1f)
            val b = bi / (LEVELS - 1f)
            val l = luminance(r, g, b)
            val k = if (l <= cap || l <= 0f) 1f else cap / l
            val value = if (k >= 1f) composite(coat, r, g, b) else composite(coat,
                GlassScene.srgb(GlassScene.linear(r) * k), GlassScene.srgb(GlassScene.linear(g) * k),
                GlassScene.srgb(GlassScene.linear(b) * k))
            if (value > worst) worst = value
        }
        return worst
    }

    /** Largest backdrop luminance that keeps tint + backdrop under the ceiling (0 when the tint alone reaches it). */
    fun backdropCap(coat: Float): Float {
        if (worstComposite(coat, 0f) > target) return 0f
        var lo = 0f
        var hi = target
        repeat(ITERATIONS) {
            val mid = (lo + hi) / 2f
            if (worstComposite(coat, mid) <= target) lo = mid else hi = mid
        }
        return lo
    }

    /** Exact mirror of the AGSL `limitLuma`: above [cap], linear-light scale down (hue kept). */
    fun limit(c: Color, cap: Float): Color {
        val r = c.red.coerceIn(0f, 1f); val g = c.green.coerceIn(0f, 1f); val b = c.blue.coerceIn(0f, 1f)
        val l = luminance(r, g, b)
        if (l <= cap) return Color(r, g, b)
        val k = cap.coerceAtLeast(0f) / l
        return Color(GlassScene.srgb(GlassScene.linear(r) * k), GlassScene.srgb(GlassScene.linear(g) * k),
            GlassScene.srgb(GlassScene.linear(b) * k))
    }

    /**
     * 2811/1: Kotlin mirror of the lens centre (no rim zone) over one backdrop pixel, in shader order:
     * vibrancy -> backdrop limit -> white-ish tint -> safety ceiling. Used to prove the header has no step.
     */
    fun lensCentre(backdrop: Color, optics: GlassOptics): Color {
        val luma = rec709[0] * backdrop.red + rec709[1] * backdrop.green + rec709[2] * backdrop.blue
        fun sat(v: Float) = (luma + (v - luma) * optics.saturation).coerceIn(0f, 1f)
        val vivid = limit(Color(sat(backdrop.red), sat(backdrop.green), sat(backdrop.blue)), optics.backdropCap)
        val w = GlassOptics.TINT
        val a = optics.coat
        val tinted = Color(a * w.red + (1f - a) * vivid.red, a * w.green + (1f - a) * vivid.green, a * w.blue + (1f - a) * vivid.blue)
        return limit(tinted, GlassOptics.SURFACE_LUMA_MAX)
    }

    /** Largest channel gain of the saturation matrix: the own-channel coefficient with the others at 0. */
    fun saturationGain(saturation: Float, channel: Int): Float =
        matrixWeights[channel] * (1f - saturation) + saturation

    /**
     * API 31–32 (no shader): blur -> colour matrix (saturation, then a uniform scale) -> tint. No channel
     * exceeds scale x gain, whatever the content (white text included), so the scale is chosen for that.
     */
    fun blurOnlyWorst(coat: Float, saturation: Float, scale: Float): Float = composite(coat,
        scale * saturationGain(saturation, 0), scale * saturationGain(saturation, 1), scale * saturationGain(saturation, 2))

    fun blurOnlyScale(coat: Float, saturation: Float): Float {
        if (blurOnlyWorst(coat, saturation, 0f) > target) return 0f
        var lo = 0f
        var hi = 1f
        repeat(ITERATIONS) {
            val mid = (lo + hi) / 2f
            if (blurOnlyWorst(coat, saturation, mid) <= target) lo = mid else hi = mid
        }
        return lo
    }
}
