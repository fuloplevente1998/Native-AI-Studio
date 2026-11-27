package com.example

import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.example.ui.theme.*
import com.repforge.app.ui.theme.NativeThemeTokens
import org.junit.Assert.*
import org.junit.Test

class LiquidDrop2814Test {
    @Test fun onlyExplicitControlsCanUseMeasurementLens() {
        for (role in GlassMaterialRole.entries) {
            fun gate(glass: Boolean = true, supported: Boolean = true, saver: Boolean = false,
                overlay: Boolean = false, full: Boolean = true, chrome: Boolean = false,
                measurement: Boolean = false, live: Boolean = true) =
                liquidDropUsesLens(glass, supported, saver, overlay, full, chrome, measurement, live, role)
            val control = role in listOf(GlassMaterialRole.Button, GlassMaterialRole.Selection, GlassMaterialRole.Input)
            assertEquals(control, gate())
            assertFalse(gate(full = false, chrome = true))
            assertEquals(control, gate(full = false, chrome = true, measurement = true))
            assertFalse(gate(full = false, measurement = true))
            assertFalse(gate(saver = true))
            assertFalse(gate(overlay = true))
            assertFalse(gate(supported = false))
            assertFalse(gate(glass = false))
            assertFalse(gate(live = false))
        }
    }

    @Test fun paintAndAccentRemainBoundedAndClarityControlled() {
        for (seed in LiquidGlassPalettes.all) for (role in GlassMaterialRole.entries) {
            val frost = seed.withGlassClarity(0)
            val clear = seed.withGlassClarity(100)
            assertTrue(liquidDropPaints(frost, role).first().alpha > liquidDropPaints(clear, role).first().alpha)
            for (p in listOf(frost, seed.withGlassClarity(65), clear)) {
                for (accent in listOf(null, p.primary, p.success, p.danger)) {
                    val paints = liquidDropPaints(p, role, accent)
                    assertEquals(3, paints.size)
                    assertTrue(paints[0].alpha > paints[1].alpha)
                    for (paint in paints) {
                        val composite = paint.compositeOver(GlassOptics.lensWorstCase)
                        assertTrue(composite.luminance() <= GlassOptics.SURFACE_LUMA_MAX + .003f)
                        assertTrue(NativeThemeTokens.contrast(p.textPrimary, composite) >= 4.5f)
                    }
                }
            }
        }
    }

    @Test fun unaccentedControlsFollowTheirPaletteAtFullClarity() {
        val ice = LiquidGlassPalettes.all.first { it.key == "glass_ice" }.withGlassClarity(100)
        val gold = LiquidGlassPalettes.all.first { it.key == "glass_amber" }.withGlassClarity(100)
        for (role in listOf(GlassMaterialRole.Button, GlassMaterialRole.Selection)) {
            val icePaint = liquidDropPaints(ice, role).first()
            val goldPaint = liquidDropPaints(gold, role).first()
            assertTrue(icePaint.blue - icePaint.red > .08f)
            assertTrue(goldPaint.red - goldPaint.blue > .08f)
        }
    }

    @Test fun dropButtonsHaveStrongerLensThanNavigationAndKeepDenseTilesCheap() {
        for (step in 0..100) {
            val button = KyantMaterialSpec.forRole(GlassMaterialRole.Button, step)
            val selected = KyantMaterialSpec.forRole(GlassMaterialRole.Selection, step)
            val nav = KyantMaterialSpec.forRole(GlassMaterialRole.Navigation, step)
            assertTrue(button.lensAmountDp > nav.lensAmountDp)
            assertTrue(selected.lensAmountDp > button.lensAmountDp)
            assertTrue(button.shadow && selected.shadow)
            assertFalse(KyantMaterialSpec.forRole(GlassMaterialRole.Tile, step).usesBackdrop)
            assertFalse(KyantMaterialSpec.forRole(GlassMaterialRole.Performance, step).usesBackdrop)
        }
    }
}
