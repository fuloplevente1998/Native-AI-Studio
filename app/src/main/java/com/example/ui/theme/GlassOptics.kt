package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.ui.components.sheetHostState
import kotlin.math.pow

/**
 * False only on the active measurement screens (2808/10): there the expensive passes (AGSL lens,
 * platform blur, live capture) fall back to the opaque paint with the cheap vector rim. Every other
 * screen keeps full glass, also while a workout session runs in the background.
 */
internal val LocalGlassOpticsEnabled = androidx.compose.runtime.staticCompositionLocalOf { true }

/** Semantic material roles, all using the same renderer and luminance ceiling. */
internal enum class GlassMaterialRole(
    val coatScale: Float, val bendScale: Float, val rimScale: Float, val depthScale: Float
) {
    Navigation(.55f, .85f, .70f, .55f),
    Panel(1f, 1f, 1f, 1f),
    Tile(1f, .55f, .60f, .55f),
    Button(1f, 1.12f, 1.10f, 1.10f),
    Selection(.80f, 1.20f, 1.25f, 1.15f),
    Input(1f, .55f, .70f, .65f),
    Overlay(1f, .80f, .85f, .80f),
    Performance(1f, .0f, .70f, .55f),
}

/**
 * TrainPilot Liquid Glass material as ONE continuous function of the family slider
 * (0 = Frosted, 100 = Clear Liquid). Every field is monotonic in the slider and moves by a
 * small, bounded amount per step, so a drag never jumps between looks. Independently authored
 * TrainPilot values. 2810/6: iOS-like layering (technique as in public Apache-2.0 backdrop engines,
 * own code): strong blur -> rim refraction -> vibrancy -> backdrop luminance limit -> thin white-ish
 * tint -> specular rim; readability comes from the backdrop limit, not from a dark tint.
 */
internal data class GlassOptics(
    /** 0 Frosted .. 1 Clear. */
    val clarity: Float,
    /** In-shader diffusion of the procedural scene, normalized to a phone-width scene ([blurDp] / [SCENE_REFERENCE_DP]). */
    val blur: Float,
    /** Backdrop blur radius (dp): platform blur on live content, in-shader diffusion on the scene. */
    val blurDp: Float,
    /** Rim refraction, as a fraction of the shorter side. Zero on the flat center. */
    val bend: Float,
    /** Width of the curved rim zone, as a fraction of the shorter half side. */
    val bevel: Float,
    /** Chromatic split; only applied inside the rim zone. */
    val dispersion: Float,
    /** Alpha of the white-ish [TINT] over the backdrop (2810/6: .40 Frosted -> .06 Clear). */
    val coat: Float,
    /** Backdrop vibrancy (1 = unchanged saturation). */
    val saturation: Float,
    /** Specular rim highlight strength: alpha of the top edge line (sides/bottom are weaker). */
    val specular: Float,
    /** Inner depth shade along the rim (darkens only). */
    val shadow: Float,
    /** Largest backdrop luminance under the tint ([GlassContrast.backdropCap]): grows towards Clear. */
    val backdropCap: Float,
    /** API 31–32 colour-matrix scale after the saturation boost ([GlassContrast.blurOnlyScale]). */
    val blurOnlyScale: Float,
) {
    companion object {
        /**
         * 2810/6: luminance ceiling of every glass composite (tint over the limited backdrop, page,
         * tiles). White text keeps 5.8:1; palette text/accents are lifted against it ([lensWorstCase]).
         */
        const val SURFACE_LUMA_MAX = .13f
        /** V4 scene bound; kept as an extra contrast surface (2809 page backdrop: [GlassScene.lumaCap]). */
        const val SCENE_ACCENT_MAX = .16f
        const val FROSTED_BLUR_DP = 32f
        const val CLEAR_BLUR_DP = 20f
        const val SCENE_REFERENCE_DP = 411f
        const val FROSTED_COAT = .40f
        const val CLEAR_COAT = .06f
        /** The white-ish glass tint (cool white). */
        val TINT = Color(0xFFEEF3FB)

        private val cache = arrayOfNulls<GlassOptics>(101)
        private val roleCache = Array(GlassMaterialRole.values().size) { arrayOfNulls<GlassOptics>(101) }

        /** Recompute both contrast bounds when the material tint changes; never raise the ceiling. */
        fun forRole(value: Int, role: GlassMaterialRole): GlassOptics {
            val step = value.coerceIn(0, 100)
            val base = from(step)
            if (role == GlassMaterialRole.Panel) return base
            val cache = roleCache[role.ordinal]
            return cache[step] ?: run {
                val coat = base.coat * role.coatScale
                base.copy(
                    coat = coat,
                    bend = base.bend * role.bendScale,
                    dispersion = base.dispersion * role.bendScale,
                    specular = (base.specular * role.rimScale).coerceAtMost(.60f),
                    shadow = base.shadow * role.depthScale,
                    backdropCap = if (coat == base.coat) base.backdropCap else GlassContrast.backdropCap(coat),
                    blurOnlyScale = if (coat == base.coat) base.blurOnlyScale
                        else GlassContrast.blurOnlyScale(coat, base.saturation),
                ).also { cache[step] = it }
            }
        }


        fun from(value: Int): GlassOptics {
            val step = value.coerceIn(0, 100)
            return cache[step] ?: compute(step.toFloat()).also { cache[step] = it }
        }

        /** Brightest possible glass composite: a neutral grey at [SURFACE_LUMA_MAX] (worst for light text). */
        val lensWorstCase: Color get() = GlassContrast.grey(SURFACE_LUMA_MAX)

        /** Upper bound of the V4 procedural page scene: the base mixed (sRGB, like AGSL) towards white. */
        fun sceneWorstCase(background: Color): Color = Color(
            background.red + (1f - background.red) * SCENE_ACCENT_MAX,
            background.green + (1f - background.green) * SCENE_ACCENT_MAX,
            background.blue + (1f - background.blue) * SCENE_ACCENT_MAX)

        /** API 31–32 blur-only content lens: brightest composite of tint over scaled, saturated content. */
        fun blurOnlyWorstCase(clarity: Int): Color = from(clarity).let {
            GlassContrast.grey(GlassContrast.blurOnlyWorst(it.coat, it.saturation, it.blurOnlyScale))
        }

        fun from(value: Float): GlassOptics {
            val v = if (value.isNaN()) 0f else value.coerceIn(0f, 100f)
            return if (v == kotlin.math.floor(v)) from(v.toInt()) else compute(v)
        }

        private fun compute(value: Float): GlassOptics {
            val t = value / 100f
            val blurDp = FROSTED_BLUR_DP * (CLEAR_BLUR_DP / FROSTED_BLUR_DP).pow(t)
            val coat = FROSTED_COAT - (FROSTED_COAT - CLEAR_COAT) * t
            val saturation = 1.40f + .40f * t
            return GlassOptics(
                clarity = t,
                blur = blurDp / SCENE_REFERENCE_DP,
                // Exponential: equal slider steps read as equal perceived blur steps (32 -> 20 dp).
                blurDp = blurDp,
                bend = .018f + .112f * t,
                bevel = .30f + .25f * t,
                dispersion = .004f + .056f * t * t,
                coat = coat,
                saturation = saturation,
                specular = .30f + .15f * t,
                shadow = .10f + .08f * t,
                backdropCap = GlassContrast.backdropCap(coat),
                blurOnlyScale = GlassContrast.blurOnlyScale(coat, saturation),
            )
        }
    }
}

/** The single gate: glass only for the Liquid Glass family (33–40) and only while optics are allowed. */
internal fun TrainPilotPalette.glassEffectsActive(opticsEnabled: Boolean): Boolean = isLiquidGlass && opticsEnabled

/**
 * 2808/10: full optics everywhere except the active measurement screen itself (strength, timed,
 * bilateral, outdoor GPS) – also while a sheet is open over it. Home, Progress, Health, Settings
 * and the workout tab keep glass while a session runs in the background.
 */
internal fun glassOpticsEnabledFor(hostScreen: com.example.model.SubScreen): Boolean =
    !com.example.ui.components.isActiveMeasurementScreen(hostScreen)

internal fun glassOpticsEnabledFor(state: com.example.ui.viewmodel.TrainPilotUiState): Boolean =
    glassOpticsEnabledFor(state.sheetHostState().currentSubScreen)

/** Cheap vector rim/sheen (no shader, blur or capture): stays on the measurement screens too. */
internal fun TrainPilotPalette.glassRimActive(): Boolean = isLiquidGlass

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
