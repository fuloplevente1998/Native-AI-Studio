package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.example.ui.theme.*
import com.repforge.app.ui.theme.NativeThemeTokens
import org.junit.Assert.*
import org.junit.Test

class LiquidGlassOverlay2812Test {
    @Test fun sheetSceneDoesNotFollowRootClock() {
        val root = GlassSceneState().apply { seconds = 12f }
        val frozen = freezeSheetScene(root)
        root.seconds = 48f
        assertNotSame(root, frozen)
        assertEquals(12f, frozen.seconds, 0f)
        assertEquals(0f, freezeSheetScene(null).seconds, 0f)
    }

    @Test fun sheetHeightStaysPinnedDuringDragAndSettling() {
        val lock = com.example.ui.components.SheetHeightLock()
        assertNull(lock.heightFor(false, 0, 800))
        lock.measured(640)
        assertEquals(640, lock.heightFor(true, 0, 800))
        lock.measured(640)
        assertEquals(500, lock.heightFor(true, 0, 500))
        lock.measured(500)
        assertEquals(640, lock.heightFor(true, 0, 800))
        assertTrue("a new measured height must be applied after settling", lock.release())
        assertNull(lock.heightFor(false, 0, 800))
        lock.measured(420)
        assertEquals(420, lock.heightFor(true, 0, 800))
        assertFalse("unchanged size requires no new layout", lock.release())
        assertFalse(lock.hasPin)
    }

    @Test fun rootBackdropRemainsPausedUntilTheLastSheetCloses() {
        val root = GlassSceneState()
        assertFalse(root.coveredBySheet)
        root.sheetOpened()
        root.sheetOpened()
        root.sheetClosed()
        assertTrue(root.coveredBySheet)
        root.sheetClosed()
        assertFalse(root.coveredBySheet)
    }

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
