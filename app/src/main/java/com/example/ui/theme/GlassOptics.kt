package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

/** False while a workout session runs: every glass surface falls back to its opaque paint. */
internal val LocalGlassOpticsEnabled = androidx.compose.runtime.staticCompositionLocalOf { true }

/**
 * TrainPilot Liquid Glass material as ONE continuous function of the family slider
 * (0 = Frosted, 100 = Clear Liquid). Every field is monotonic in the slider and moves by a
 * small, bounded amount per step, so a drag never jumps between looks. Independently authored
 * TrainPilot values; no third-party parameters or shader code.
 */
internal data class GlassOptics(
    /** 0 Frosted .. 1 Clear. */
    val clarity: Float,
    /** In-shader diffusion of the procedural scene, normalized to the scene size. */
    val blur: Float,
    /** Platform blur radius (dp) applied to live captured content. */
    val blurDp: Float,
    /** Rim refraction, as a fraction of the shorter side. Zero on the flat center. */
    val bend: Float,
    /** Width of the curved rim zone, as a fraction of the shorter half side. */
    val bevel: Float,
    /** Chromatic split; only applied inside the rim zone. */
    val dispersion: Float,
    /** Palette tint mixed over the backdrop. */
    val coat: Float,
    /** Backdrop vibrancy (1 = unchanged saturation). */
    val saturation: Float,
    /** Specular rim highlight strength. */
    val specular: Float,
    /** Inner depth shade along the rim. */
    val shadow: Float,
    /** Opaque surface veil over blur-only content (API 31–32, which has no shader guard). */
    val veil: Float,
) {
    companion object {
        /** No lens output channel exceeds this sRGB value; palettes keep text readable against it. */
        const val LENS_PEAK = .32f
        /** Upper bound of the accent mixed into the procedural page scene. */
        const val SCENE_ACCENT_MAX = .16f
        const val FROSTED_BLUR_DP = 24f
        const val CLEAR_BLUR_DP = 1f

        fun from(value: Int): GlassOptics = from(value.toFloat())

        /** Brightest possible lens output (the shader caps every channel at [LENS_PEAK]). */
        val lensWorstCase: Color get() = Color(LENS_PEAK, LENS_PEAK, LENS_PEAK)

        /** Upper bound of the procedural page scene: the base mixed (sRGB, like AGSL) towards white. */
        fun sceneWorstCase(background: Color): Color = Color(
            background.red + (1f - background.red) * SCENE_ACCENT_MAX,
            background.green + (1f - background.green) * SCENE_ACCENT_MAX,
            background.blue + (1f - background.blue) * SCENE_ACCENT_MAX)

        fun from(value: Float): GlassOptics {
            val t = (if (value.isNaN()) 0f else value.coerceIn(0f, 100f)) / 100f
            return GlassOptics(
                clarity = t,
                blur = .095f * (1f - t) + .0015f,
                // Exponential: equal slider steps read as equal perceived blur steps.
                blurDp = FROSTED_BLUR_DP * (CLEAR_BLUR_DP / FROSTED_BLUR_DP).pow(t),
                bend = .018f + .112f * t,
                bevel = .30f + .25f * t,
                dispersion = .004f + .056f * t * t,
                coat = .62f - .52f * t,
                saturation = 1.08f + .22f * t,
                specular = .18f + .34f * t,
                shadow = .14f + .16f * t,
                veil = .94f - .04f * t,
            )
        }
    }
}

/** The single gate: glass only for the Liquid Glass family (33–40) and only while optics are allowed. */
internal fun TrainPilotPalette.glassEffectsActive(opticsEnabled: Boolean): Boolean = isLiquidGlass && opticsEnabled

/** Optics are off for the whole app while a workout session runs (timer screens stay opaque and cheap). */
internal fun glassOpticsEnabledFor(workoutSessionActive: Boolean): Boolean = !workoutSessionActive

/** Slider position (any float, including in-between drag positions) to the stored 0..100 step. */
internal fun glassClarityStep(position: Float): Int =
    if (position.isNaN()) 0 else kotlin.math.round(position.coerceIn(0f, 100f)).toInt()

/**
 * Pure motion rules for the glass family, kept apart from Compose so they are unit-testable.
 * Reduced motion = the system animator duration scale is 0 (Developer options or accessibility
 * "Remove animations"): then no travel, squash or press scaling is animated.
 */
internal object GlassMotion {
    const val PRESS_SCALE = .96f
    const val PRESS_HIGHLIGHT = .16f
    const val EDGE_DAMPING = .74f
    private const val LEADING_STIFFNESS = 820f
    private const val TRAILING_STIFFNESS = 300f
    const val MIN_SQUASH = .84f

    fun enabled(animatorDurationScale: Float): Boolean =
        !animatorDurationScale.isNaN() && animatorDurationScale > 0f

    /** The selection pill travels only when motion is allowed and it already had a position. */
    fun animateTravel(motionEnabled: Boolean, alreadyPlaced: Boolean): Boolean = motionEnabled && alreadyPlaced

    /** Progress may overshoot below 0 on the release spring: the control briefly grows past 1. */
    fun pressScale(progress: Float): Float = 1f - (1f - PRESS_SCALE) * progress.coerceIn(-.5f, 1f)

    /** The edge facing the travel direction is stiffer and leads; the other trails: a liquid stretch. */
    fun edgeStiffness(leading: Boolean): Float = if (leading) LEADING_STIFFNESS else TRAILING_STIFFNESS

    /** Vertical squash while the pill is stretched, so it reads as one volume of liquid. */
    fun squash(width: Float, restWidth: Float): Float {
        if (restWidth <= 0f || width.isNaN()) return 1f
        val stretch = (width / restWidth - 1f).coerceIn(0f, 3f)
        return (1f - .30f * stretch / (1f + stretch)).coerceIn(MIN_SQUASH, 1f)
    }
}
