package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Liquid Glass 2.0 / G1 (2811): semantic material roles of the ONE TrainPilot glass engine. A role is a
 * set of multipliers over the slider material ([GlassOptics.from]); it never adds a second engine, blur
 * pass or shader. Every role keeps the single contrast rule (tint + limited backdrop <= the ceiling), so the
 * palette text colours stay valid for all of them. Only the Liquid Glass family (33–40) reads roles.
 *
 * Multipliers: [tint] white-ish coat alpha (<= 1, so the tint alone never reaches the ceiling), [blurScale]
 * backdrop blur, [rim] specular edge, [depth] inner shade + thickness, [elevation] soft outer shadow,
 * [refraction] rim bend, [chroma] edge-only colour split, [vibrancy] saturation boost (s - 1).
 */
internal enum class GlassRole(
    val tint: Float,
    val blurScale: Float,
    val rim: Float,
    val depth: Float,
    val elevation: Float,
    val refraction: Float,
    val chroma: Float,
    val vibrancy: Float,
) {
    /**
     * Header band (2811/1, device round 2): continuous with the page – no tint, vibrancy, rim, inner shade or
     * shadow, and the page's own luminance guard, so at scroll top it equals the page pixel for pixel (no
     * step, no hard line). Content scrolled under it is blurred and edge-refracted, and limited to the same
     * page ceiling, so glyphs on it stay >= 4.5:1. Quick buttons and the nav capsule are glass tiles on it.
     */
    Navigation(0f, 1.10f, 0f, 0f, 0f, .80f, .70f, 0f),
    /** Outer card / section: the reference material (identical to the pre-role engine). */
    Panel(1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f),
    /** Inner tile on a panel: lighter, flatter, no outer shadow – always weaker than [Panel]. */
    Tile(.55f, .75f, .55f, .50f, 0f, .55f, .50f, 1f),
    /** Chips, segmented controls, buttons. */
    Button(.80f, .85f, .85f, .80f, .40f, .90f, .80f, 1f),
    /** The moving selection lens (nav pill, selected day). */
    Selection(.60f, .90f, 1.10f, .60f, .30f, 1.10f, .90f, 1.10f),
    /** Search fields, inputs: inset, little refraction. */
    Input(.90f, .80f, .55f, .90f, 0f, .50f, .40f, 1f),
    /** Sheets / modal panels: a little more frost and lift. */
    Overlay(1f, 1.15f, 1f, 1.10f, 1.25f, .90f, .70f, 1f),
    /** Active measurement screens: no blur, refraction or shadow; cheap vector rim only. */
    Performance(1f, 0f, .75f, .50f, 0f, 0f, 0f, 1f);

    /** Visual weight (edge light + depth + lift): orders the hierarchy, e.g. Panel > Tile. */
    val prominence: Float get() = rim + depth + elevation

    /** No own tint or vibrancy: the surface reproduces the guarded page exactly where nothing is under it. */
    val matchesPage: Boolean get() = tint == 0f && vibrancy == 0f

    companion object {
        /**
         * The role actually drawn: where the expensive optics are off (active measurement screen) every
         * surface uses the light [Performance] material; elsewhere the requested role.
         */
        fun resolve(requested: GlassRole, opticsEnabled: Boolean): GlassRole =
            if (opticsEnabled || requested.matchesPage) requested else Performance
    }
}

/**
 * Directional specular edge shared by the AGSL rim and its vector mirror ([drawGlassHighlights],
 * [trainPilotGlassRim]): top strongest, sides weaker, bottom weakest, a little extra towards the light
 * (upper left). Kept apart from Compose so the shape of the light is unit-testable.
 */
internal object GlassRimLight {
    const val BOTTOM = .27f
    const val TOP_GAIN = .73f
    const val SIDE_GAIN = .13f
    const val LIGHT_GAIN = .08f
    /** Light from the upper left (y grows downwards); the shader uses the same uniform. */
    const val LIGHT_X = -.48f
    const val LIGHT_Y = -.877f
    /** The shine never goes past this white mix: no neon line. */
    const val MAX_SHINE = .60f

    val top: Float get() = BOTTOM + TOP_GAIN
    val side: Float get() = BOTTOM + SIDE_GAIN

    /** Rim line alpha for an outward normal (nx, ny); mirrors the AGSL `rimAlpha`. */
    fun edgeAlpha(nx: Float, ny: Float, specular: Float): Float {
        val topness = (-ny).coerceIn(0f, 1f)
        val sideness = 1f - kotlin.math.abs(ny)
        val facing = (nx * LIGHT_X + ny * LIGHT_Y).coerceAtLeast(0f)
        return specular * (BOTTOM + TOP_GAIN * topness + SIDE_GAIN * sideness) + LIGHT_GAIN * specular * facing
    }

    /** Vertical vector rim (top, middle, bottom) for paths without AGSL. */
    fun gradient(strength: Float): List<Pair<Float, Color>> = listOf(
        0f to Color.White.copy(alpha = strength * top.coerceAtMost(1f)),
        .5f to Color.White.copy(alpha = strength * side),
        1f to Color.White.copy(alpha = strength * BOTTOM))
}
