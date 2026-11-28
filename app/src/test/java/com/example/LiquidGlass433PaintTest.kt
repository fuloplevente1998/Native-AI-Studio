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
                val paints = listOf(tile, elevated, selection, nav, fallback, popup, p.surface, p.surfaceElevated, p.selectionColors().surface, liquidChromeTint(p), p.slidingSheetBackground(Color.Magenta), p.photoViewerBackground(Color.Magenta)) + overlay
                for (paint in paints) {
                    assertTrue("${p.key} step=$step hue=$paint", hueDistance(if (p.id == 33) Color.hsv(307f, .55f, 1f) else p.background, paint) <= 6f)
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

    @Test fun allEightMaterialFamiliesMatchTheirLivingScene() {
        // Independent expected hue bands; do not derive expected colors from the
        // material helper under test. The multicolor graphite scene uses violet.
        val expected = mapOf(33 to 307f, 34 to 202f, 35 to 222f, 36 to 190f,
            37 to 163f, 38 to 265f, 39 to 313f, 40 to 45f)
        assertEquals(expected.keys, LiquidGlassPalettes.all.map { it.id }.toSet())
        for (p in LiquidGlassPalettes.all) {
            val seed = Color.hsv(expected.getValue(p.id), .55f, 1f)
            val actual = Color.hsv(GlassScene.materialHue(p), .55f, 1f)
            assertTrue("${p.key} material family", hueDistance(seed, actual) <= 8f)
            assertTrue("${p.key} material must match at least one scene light",
                GlassScene.tones(p).any { hueDistance(it, actual) <= 24f })
            if (p.id != 33) {
                assertEquals(GlassScene.hueOf(p.background), GlassScene.materialHue(p), .01f)
            }
        }
    }

    @Test fun multicolorGraphiteChromeFollowsVioletSceneWithoutChangingGoldOrPage() {
        val graphite = LiquidGlassPalettes.all.single { it.id == 33 }
        val gold = LiquidGlassPalettes.all.single { it.id == 40 }
        assertEquals("glass_graphite", graphite.key)
        assertEquals(Color(0xFF172333), graphite.background)
        assertEquals(Color(0xFF382E08), gold.background)
        assertEquals(307f, GlassScene.materialHue(graphite), .01f)
        assertEquals(GlassScene.hueOf(gold.background), GlassScene.materialHue(gold), .01f)
        assertTrue(GlassScene.tones(graphite).any {
            hueDistance(it, Color.hsv(307f, .55f, 1f)) <= 2f
        })
        for (step in 0..100) {
            val p = graphite.withGlassClarity(step)
            val amber = gold.withGlassClarity(step)
            for (paint in listOf(p.photoViewerBackground(Color.Black),
                p.slidingSheetBackground(Color.Black), liquidChromeTint(p),
                p.surface, p.surfaceElevated, p.selectionColors().surface)) {
                assertTrue("Graphite violet material at $step: $paint", paint.red > paint.green && paint.blue > paint.green)
                assertTrue(hueDistance(Color.hsv(307f, .55f, 1f), paint) <= 6f)
            }
            for (paint in listOf(amber.photoViewerBackground(Color.Black),
                amber.slidingSheetBackground(Color.Black), liquidChromeTint(amber))) {
                assertTrue("Gold material at $step: $paint", paint.red > paint.blue)
                assertTrue(hueDistance(amber.background, paint) <= 6f)
            }
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
