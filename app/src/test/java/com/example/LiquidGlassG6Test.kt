package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.example.model.SubScreen
import com.example.ui.theme.*
import com.repforge.app.ui.theme.NativeThemeTokens
import org.junit.Assert.*
import org.junit.Test

/** Final cross-role regression. No claims about device frame time or rendered pixels. */
class LiquidGlassG6Test {
    @Test fun allIntegerClarityStepsKeepEveryRoleInsideTheContrastEnvelope() {
        for (role in GlassMaterialRole.values()) {
            var previous: GlassOptics? = null
            for (step in 0..100) {
                val m = GlassOptics.forRole(step, role)
                val label = "$role/$step"
                assertSame(label, m, GlassOptics.forRole(step, role))
                assertTrue(label, m.coat in 0f..1f && m.specular in 0f..0.60f)
                assertTrue(label, m.backdropCap in 0f..GlassOptics.SURFACE_LUMA_MAX)
                assertTrue(label, m.blurOnlyScale in 0f..1f)
                assertTrue(label, GlassContrast.worstComposite(m.coat, m.backdropCap) <= GlassOptics.SURFACE_LUMA_MAX)
                assertTrue(label, GlassContrast.blurOnlyWorst(m.coat, m.saturation, m.blurOnlyScale) <= GlassOptics.SURFACE_LUMA_MAX)
                previous?.let { last ->
                    assertTrue(label, m.coat <= last.coat && last.coat - m.coat <= .005f)
                    assertTrue(label, m.blurDp <= last.blurDp && last.blurDp - m.blurDp <= .25f)
                    assertTrue(label, m.bend >= last.bend && m.bend - last.bend <= .003f)
                }
                if (role == GlassMaterialRole.Performance) {
                    assertEquals(label, 0f, m.bend, 0f)
                    assertEquals(label, 0f, m.dispersion, 0f)
                }
                previous = m
            }
        }
    }

    @Test fun glassTilesAndLabelsStayReadableOnTheBoundedPageAndPanel() {
        val underlays = listOf(GlassOptics.lensWorstCase, GlassContrast.grey(GlassScene.lumaCap))
        for (seed in LiquidGlassPalettes.all) for (step in listOf(0, 50, 65, 100)) {
            val p = seed.withGlassClarity(step)
            for (under in underlays) for (elevated in listOf(false, true)) {
                val tile = p.glassTile(p.surfaceCard, elevated).compositeOver(under)
                val label = "${p.key}/$step/$elevated"
                assertTrue(label, tile.luminance() <= GlassOptics.SURFACE_LUMA_MAX + .003f)
                for (ink in listOf(p.textPrimary, p.textSecondary, p.primary, p.success, p.danger, p.warning))
                    assertTrue(label, NativeThemeTokens.contrast(ink, tile) >= 4.5f)
                assertTrue(label, NativeThemeTokens.contrast(p.cardBorder, tile) >= 3f)
            }
        }
    }

    @Test fun measurementRoutesNeverEnableTheExpensiveGlassPath() {
        val measurement = setOf(SubScreen.ACTIVE_STRENGTH, SubScreen.ACTIVE_TIMED,
            SubScreen.ACTIVE_BILATERAL, SubScreen.ACTIVE_OUTDOOR_GPS)
        for (route in measurement) for (p in LiquidGlassPalettes.all) {
            assertFalse("$route", glassOpticsEnabledFor(route))
            assertFalse(p.glassEffectsActive(glassOpticsEnabledFor(route)))
            assertTrue(p.glassRimActive())
            for (photo in listOf(false, true)) {
                val mode = GlassScene.mode(p.isLiquidGlass, glassOpticsEnabledFor(route), true)
                assertFalse(GlassScene.ticks(mode, visible = true, hasPhoto = photo))
            }
        }
        assertTrue(glassOpticsEnabledFor(SubScreen.NONE))
    }

    @Test fun originalAndCustomPalettesNeverEnterTheGlassPath() {
        val custom = CustomThemeDraft.fromStyle(CustomThemeStyle.CONTRAST, "G6 custom").let {
            requireNotNull(resolveCustomTheme(it, newCustomThemeKey())).palette
        }
        for (p in TrainPilotPalettes.allPalettes + custom) {
            assertFalse(p.isLiquidGlass)
            assertFalse(p.glassEffectsActive(true))
            assertFalse(p.glassRimActive())
            for (step in listOf(0, 50, 65, 100)) assertSame(p, p.withGlassClarity(step))
            for (paint in listOf(p.surface, p.surfaceCard, p.surfaceElevated, p.primary, p.danger, Color.Transparent)) {
                assertEquals(paint, p.glassCardFill(paint))
                assertEquals(paint, p.glassTile(paint, false))
                assertEquals(paint, p.glassTile(paint, true))
            }
        }
    }
}
