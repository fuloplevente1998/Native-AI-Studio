package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.example.ui.theme.*
import com.repforge.app.ui.theme.NativeThemeTokens
import org.junit.Assert.*
import org.junit.Test

class LiquidGlassOverlay2812Test {
    @Test fun everySheetColorFollowsPaletteNotColor() {
        for (p in LiquidGlassPalettes.all + TrainPilotPalettes.allPalettes) {
            for (color in listOf(p.background, p.surface, p.surfaceCard, p.surfaceElevated,
                p.primary, Color.Transparent, Color.Unspecified, Color.Magenta)) {
                assertEquals(p.isLiquidGlass, sheetUsesGlass(p, color))
            }
        }
    }

    @Test fun overlayRolesStayDistinctAndReadable() {
        for (step in 0..100) {
            val tile = glassOverlayRolePaint(step, GlassMaterialRole.Tile)
            val button = glassOverlayRolePaint(step, GlassMaterialRole.Button)
            val selected = glassOverlayRolePaint(step, GlassMaterialRole.Selection)
            assertTrue(tile.alpha < button.alpha)
            assertTrue(button.alpha < selected.alpha)
            assertTrue(selected.blue > selected.red)
            for (paint in listOf(tile, button, selected)) {
                val composite = paint.compositeOver(GlassOptics.lensWorstCase)
                assertTrue(composite.luminance() <= GlassOptics.SURFACE_LUMA_MAX + .003f)
                assertTrue(NativeThemeTokens.contrast(Color.White, composite) >= 4.5f)
            }
            assertFalse(KyantMaterialSpec.forRole(GlassMaterialRole.Tile, step).usesBackdrop)
            assertTrue(KyantMaterialSpec.forRole(GlassMaterialRole.Overlay, step).shadow)
            assertTrue(KyantMaterialSpec.forRole(GlassMaterialRole.Overlay, step).blurDp >
                KyantMaterialSpec.forRole(GlassMaterialRole.Panel, step).blurDp)
        }
    }
}
