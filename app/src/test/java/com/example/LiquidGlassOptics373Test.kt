package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.example.ui.theme.*
import com.repforge.app.ui.theme.NativeThemeTokens
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

/**
 * PR #373 Liquid Glass finish. Robolectric cannot run AGSL, so these protect the parameter and
 * selection logic the renderer consumes: continuous slider mapping, readability against the
 * rendered worst cases, the glass gate (family + measurement screen) and reduced-motion rules.
 */
class LiquidGlassOptics373Test {
    private fun GlassOptics.values() = listOf(blur, blurDp, bend, bevel, dispersion, coat, saturation, specular, shadow, veil)
    private val decreasing = setOf(0, 1, 5, 9) // blur, blurDp, coat, veil fall towards Clear

    @Test fun sliderMapsEveryValueMonotonicallyAndContinuously() {
        val all = (0..100).map { GlassOptics.from(it) }
        val frost = all.first().values()
        val clear = all.last().values()
        for (field in frost.indices) {
            val range = abs(clear[field] - frost[field])
            assertTrue("field $field must differ between Frosted and Clear", range > 0f)
            for (step in 1..100) {
                val before = all[step - 1].values()[field]
                val after = all[step].values()[field]
                if (field in decreasing) assertTrue("field $field at $step", after < before)
                else assertTrue("field $field at $step", after > before)
                // Continuity: no single 1% step jumps by more than 5% of the whole range.
                assertTrue("field $field jumps at $step", abs(after - before) <= range * .05f)
            }
        }
        val (f0, f37, f50, f100) = listOf(0, 37, 50, 100).map { GlassOptics.from(it) }
        assertTrue(f0.blurDp > f37.blurDp && f37.blurDp > f50.blurDp && f50.blurDp > f100.blurDp)
        assertTrue(f0.bend < f37.bend && f37.bend < f50.bend && f50.bend < f100.bend)
        assertTrue(f0.specular < f37.specular && f37.specular < f50.specular && f50.specular < f100.specular)
        assertEquals(GlassOptics.FROSTED_BLUR_DP, f0.blurDp, 1e-4f)
        assertEquals(GlassOptics.CLEAR_BLUR_DP, f100.blurDp, 1e-4f)
        // In-between drag positions interpolate between the neighbouring steps.
        val half = GlassOptics.from(36.5f)
        assertTrue(half.blurDp < GlassOptics.from(36).blurDp && half.blurDp > f37.blurDp)
        assertEquals(f0, GlassOptics.from(Float.NaN))
        assertEquals(f100, GlassOptics.from(250f))
    }

    @Test fun sliderPositionRoundsToStoredStep() {
        assertEquals(0, glassClarityStep(0f))
        assertEquals(37, glassClarityStep(37.4f))
        assertEquals(38, glassClarityStep(37.6f))
        assertEquals(50, glassClarityStep(50f))
        assertEquals(100, glassClarityStep(100f))
        assertEquals(0, glassClarityStep(-4f))
        assertEquals(100, glassClarityStep(180f))
        assertEquals(0, glassClarityStep(Float.NaN))
    }

    @Test fun textStaysReadableOnEveryRenderedGlassWorstCase() {
        val peak = Color(GlassOptics.LENS_PEAK, GlassOptics.LENS_PEAK, GlassOptics.LENS_PEAK)
        assertEquals(peak, GlassOptics.lensWorstCase)
        for (seed in LiquidGlassPalettes.all) for (clarity in 0..100 step 5) {
            val p = seed.withGlassClarity(clarity)
            val optics = GlassOptics.from(clarity)
            // sRGB mix, as the AGSL scene does: base towards white by at most SCENE_ACCENT_MAX.
            val a = GlassOptics.SCENE_ACCENT_MAX
            val scene = Color(p.background.red + (1 - p.background.red) * a,
                p.background.green + (1 - p.background.green) * a, p.background.blue + (1 - p.background.blue) * a)
            val rendered = listOf(peak, scene,
                p.surface.copy(alpha = optics.veil).compositeOver(Color.White))
            val selection = p.selectionColors()
            for (surface in rendered) {
                for (text in listOf(p.textPrimary, p.textSecondary, p.primary, p.secondary, p.success,
                    p.warning, p.danger, selection.foreground)) {
                    assertTrue("${p.key} $clarity $text on $surface",
                        NativeThemeTokens.contrast(text, surface) >= 4.5f)
                }
                assertTrue(NativeThemeTokens.contrast(p.cardBorder, surface) >= 3f)
                assertTrue(NativeThemeTokens.contrast(selection.border, surface) >= 3f)
            }
        }
    }

    @Test fun glassOnlyForGlassFamilyAndNeverOnTheActiveMeasurementScreen() {
        for (legacy in TrainPilotPalettes.allPalettes) {
            assertFalse(legacy.glassEffectsActive(true))
            assertFalse(legacy.glassRimActive())
            assertSame(legacy, legacy.withGlassClarity(37))
        }
        val glass = LiquidGlassPalettes.all
        assertEquals((33..40).toList(), glass.map { it.id })
        // 2808/10: the expensive optics are off only on the measurement screen itself.
        for (p in glass) {
            assertTrue(p.glassEffectsActive(glassOpticsEnabledFor(com.example.model.SubScreen.NONE)))
            for (measuring in listOf(com.example.model.SubScreen.ACTIVE_STRENGTH, com.example.model.SubScreen.ACTIVE_TIMED,
                com.example.model.SubScreen.ACTIVE_BILATERAL, com.example.model.SubScreen.ACTIVE_OUTDOOR_GPS)) {
                assertFalse(p.glassEffectsActive(glassOpticsEnabledFor(measuring)))
            }
            assertTrue("cheap rim stays (light glass)", p.glassRimActive())
        }
    }

    @Test fun reducedMotionDisablesPillTravelAndPressScaling() {
        assertFalse(GlassMotion.enabled(0f))
        assertFalse(GlassMotion.enabled(Float.NaN))
        assertTrue(GlassMotion.enabled(.5f))
        assertTrue(GlassMotion.enabled(1f))
        assertFalse(GlassMotion.animateTravel(motionEnabled = false, alreadyPlaced = true))
        assertFalse(GlassMotion.animateTravel(motionEnabled = true, alreadyPlaced = false))
        assertTrue(GlassMotion.animateTravel(motionEnabled = true, alreadyPlaced = true))
        assertEquals(1f, GlassMotion.pressScale(0f), 0f)
        assertEquals(GlassMotion.PRESS_SCALE, GlassMotion.pressScale(1f), 1e-6f)
        assertTrue(GlassMotion.PRESS_SCALE in 0.94f..0.98f)
        assertTrue("release overshoot stays subtle", GlassMotion.pressScale(-5f) <= 1.03f)
        assertTrue(GlassMotion.edgeStiffness(leading = true) > GlassMotion.edgeStiffness(leading = false))
        assertEquals(1f, GlassMotion.squash(120f, 120f), 0f)
        var last = 1f
        for (width in 120..480 step 20) {
            val squash = GlassMotion.squash(width.toFloat(), 120f)
            assertTrue(squash <= last && squash >= GlassMotion.MIN_SQUASH)
            last = squash
        }
        assertEquals(1f, GlassMotion.squash(50f, 0f), 0f)
    }

    @Test fun shaderUniformsMatchTheBinder() {
        fun uniforms(source: String) = Regex("""uniform\s+(?:shader|float|float2|float4|half4)\s+(\w+);""")
            .findAll(source).map { it.groupValues[1] }.toSet()
        val material = setOf("extent", "radii", "px", "bend", "bevel", "dispersion", "frost", "coat",
            "saturation", "specular", "shade", "peak", "lightDir", "tint")
        // 2809/1: living backdrop (four drifting blobs) and the optional user image share one guard.
        val sceneCommon = setOf("screen", "origin", "lumaCap", "knee", "guard", "base")
        assertEquals(sceneCommon + (0 until GlassScene.blobCount).flatMap { listOf("blob$it", "tone$it") },
            uniforms(GlassShaderSource.SCENE))
        assertEquals(sceneCommon + "photo", uniforms(GlassShaderSource.SCENE_PHOTO))
        assertEquals(material + "scene", uniforms(GlassShaderSource.SCENE_LENS))
        assertEquals(material + "underlay", uniforms(GlassShaderSource.CONTENT_LENS))
        for (source in listOf(GlassShaderSource.SCENE, GlassShaderSource.SCENE_PHOTO, GlassShaderSource.SCENE_LENS,
            GlassShaderSource.CONTENT_LENS)) {
            assertFalse("unexpanded template", source.contains('$'))
            assertEquals(source.count { it == '{' }, source.count { it == '}' })
            assertEquals(source.count { it == '(' }, source.count { it == ')' })
            assertTrue(source.contains("half4 main(float2 pos)"))
        }
    }
}
