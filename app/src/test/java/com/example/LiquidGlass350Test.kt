package com.example

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.example.ui.components.ColorWheelGeometry
import com.example.ui.theme.*
import com.repforge.app.ui.theme.NativeThemeTokens
import org.junit.Assert.*
import org.junit.Test

class LiquidGlass350Test {
    @Test fun familyIsIndependentAndOldThemesAreNotMutated() {
        assertEquals((1..32).toList(), TrainPilotPalettes.allPalettes.map { it.id })
        val all = TrainPilotPalettes.selectablePalettes
        assertEquals((1..40).toList(), all.map { it.id })
        assertEquals(40, all.map { it.key }.distinct().size)
        for (p in TrainPilotPalettes.allPalettes) {
            assertSame(p, p.withGlassClarity(0))
            assertSame(p, p.withGlassClarity(100))
            assertFalse(p.isLiquidGlass)
        }
        for (p in all.takeLast(8)) {
            assertTrue(p.isLiquidGlass)
            assertEquals(PaletteCategory.LIQUID_GLASS, p.subGroup)
            assertEquals(p, TrainPilotPalettes.getByKey(p.key))
            assertEquals(p, TrainPilotPalettes.getById(p.id))
        }
    }

    @Test fun frostAndClearChangeActualOpticalParameters() {
        val frost = GlassOptics.from(0)
        val middle = GlassOptics.from(50)
        val clear = GlassOptics.from(100)
        assertTrue(frost.blur > middle.blur && middle.blur > clear.blur)
        assertTrue(frost.coat > middle.coat && middle.coat > clear.coat)
        assertTrue(frost.bend < middle.bend && middle.bend < clear.bend)
        assertEquals(frost, GlassOptics.from(-2))
        assertEquals(clear, GlassOptics.from(120))
        for (p in LiquidGlassPalettes.all) {
            val frosted = p.withGlassClarity(0)
            val clearP = p.withGlassClarity(100)
            assertTrue(frosted.cardPaint.alpha > clearP.cardPaint.alpha)
            assertNotEquals(frosted.cardPaint, clearP.cardPaint)
            assertEquals(p.key, clearP.key)
            for (clarity in 0..100 step 10) {
                val c = p.withGlassClarity(clarity)
                val surfaces = listOf(c.background, c.surface, c.surfaceElevated,
                    c.cardPaint.compositeOver(requireNotNull(c.customPageEnd)))
                for (surface in surfaces) {
                    for (text in listOf(c.textPrimary, c.textSecondary, c.primary, c.secondary,
                        c.success, c.warning, c.danger, c.heartAccent, c.sleepAccent)) {
                        assertTrue("${p.key} clarity $clarity text $text", NativeThemeTokens.contrast(text, surface) >= 4.5f)
                    }
                    assertTrue(NativeThemeTokens.contrast(c.cardBorder, surface) >= 3f)
                }
                assertTrue(NativeThemeTokens.contrast(c.onPrimary, c.primary) >= 4.5f)
            }
        }
    }

    @Test fun ringAndFieldCoordinatesStayIndependent() {
        val g = ColorWheelGeometry(320f)
        for (hue in listOf(0f, 90f, 180f, 270f)) {
            val point = g.hueMarker(hue)
            assertTrue(g.ringHit(point))
            assertFalse(g.fieldHit(point))
            assertEquals(hue, g.hueAt(point), .01f)
        }
        for (s in listOf(0f, .5f, 1f)) for (v in listOf(0f, .5f, 1f)) {
            val point = g.fieldMarker(s, v)
            assertTrue(g.fieldHit(point))
            assertEquals(s, g.saturationAt(point), .0001f)
            assertEquals(v, g.valueAt(point), .0001f)
        }
        assertEquals(1f, g.saturationAt(Offset(1000f, 0f)))
        assertEquals(0f, g.valueAt(Offset(0f, 1000f)))
    }
}
