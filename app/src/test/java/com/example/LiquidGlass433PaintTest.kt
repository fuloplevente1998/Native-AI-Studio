package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.example.ui.theme.*
import com.repforge.app.ui.theme.NativeThemeTokens
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class LiquidGlass433PaintTest {
    private fun hueDistance(a: Color, b: Color): Float {
        val delta = abs(GlassScene.hueOf(a) - GlassScene.hueOf(b))
        return minOf(delta, 360f - delta)
    }

    @Test fun allLiquidStepsUseTheirOwnHueAndKeepReadableDistinctPaints() {
        for (seed in LiquidGlassPalettes.all) {
            var previousTile = 1f
            var previousSelection = 1f
            var previousFallback = 1f
            for (step in 0..100) {
                val p = seed.withGlassClarity(step)
                val tile = p.glassTile(Color.Magenta)
                val elevated = p.glassTile(Color.Magenta, elevated = true)
                val selection = p.glassControlFill(Color.Magenta, GlassMaterialRole.Selection)
                val nav = p.glassControlFill(Color.Magenta, GlassMaterialRole.Navigation)
                val fallback = glassFallbackPaint(p, step)
                val popup = p.glassOverlayFill(Color.Magenta)
                val overlay = GlassMaterialRole.entries.map { glassOverlayRolePaint(p, it) }
                val paints = listOf(tile, elevated, selection, nav, fallback, popup, p.surface, p.surfaceElevated, p.selectionColors().surface) + overlay
                for (paint in paints) {
                    assertTrue("${p.key} step=$step hue=$paint", hueDistance(p.background, paint) <= 6f)
                    assertTrue("${p.key} step=$step bounded=$paint",
                        paint.copy(alpha = 1f).luminance() <= GlassOptics.SURFACE_LUMA_MAX)
                    val composited = paint.compositeOver(GlassOptics.lensWorstCase)
                    for (text in listOf(p.textPrimary, p.textSecondary, p.primary, p.success, p.danger, p.warning))
                        assertTrue("${p.key} step=$step text=$text paint=$paint",
                            NativeThemeTokens.contrast(text, composited) >= 4.5f)
                }
                assertNotEquals(tile, elevated)
                assertTrue(nav.alpha < tile.alpha && tile.alpha < selection.alpha)
                assertTrue(tile.alpha <= previousTile && selection.alpha <= previousSelection &&
                    fallback.alpha <= previousFallback)
                assertEquals(glassTileAlpha(step), tile.alpha, 1f / 255f)
                assertEquals(.36f - .12f * step / 100f, selection.alpha, 1f / 255f)
                assertEquals(.82f - .64f * step / 100f, fallback.alpha, 1f / 255f)
                assertEquals(1f, popup.alpha, 0f)
                for (role in listOf(GlassMaterialRole.Tile, GlassMaterialRole.Performance))
                    assertFalse(KyantMaterialSpec.forRole(role, step).usesBackdrop)
                previousTile = tile.alpha
                previousSelection = selection.alpha
                previousFallback = fallback.alpha
            }
        }
        // Opposite palette families must not collapse to the same neutral RGB.
        for (step in 0..100) {
            val gold = LiquidGlassPalettes.all.single { it.id == 40 }.withGlassClarity(step)
            val emerald = LiquidGlassPalettes.all.single { it.id == 37 }.withGlassClarity(step)
            assertNotEquals(gold.glassTile(Color.Black), emerald.glassTile(Color.Black))
            assertNotEquals(gold.glassOverlayFill(Color.Black), emerald.glassOverlayFill(Color.Black))
        }
    }

    @Test fun standardAndCustomPaintsArePassedThroughUnchanged() {
        val custom = requireNotNull(resolveCustomTheme(
            CustomThemeDraft.fromStyle(CustomThemeStyle.CONTRAST, "433"), newCustomThemeKey())).palette
        assertEquals(32, TrainPilotPalettes.allPalettes.size)
        for (p in TrainPilotPalettes.allPalettes + custom) {
            for (step in 0..100) assertEquals(p, p.withGlassClarity(step))
            for (legacy in listOf(p.surface, p.primary, p.danger, Color.Transparent)) {
                assertEquals(legacy, p.glassTile(legacy))
                assertEquals(legacy, p.glassTile(legacy, elevated = true))
                assertEquals(legacy, p.glassOverlayFill(legacy))
                for (role in GlassMaterialRole.entries) assertEquals(legacy, p.glassControlFill(legacy, role))
            }
        }
    }
}
