package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.example.ui.theme.*
import com.repforge.app.ui.theme.NativeThemeTokens
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class KyantK2IntegrationTest {
    @Test fun semanticStatePaintPreservesContrastAndLegacy() {
        for (seed in LiquidGlassPalettes.all) for (step in 0..100) {
            val p = seed.withGlassClarity(step)
            for (accent in listOf(p.primary, p.success, p.danger, p.warning)) for (strong in listOf(false, true)) {
                val paint = p.glassStateTile(accent, Color.Black, strong)
                assertTrue(paint.alpha in 0.01f..0.45f)
                val composite = paint.compositeOver(GlassOptics.lensWorstCase)
                assertTrue(composite.luminance() <= GlassOptics.SURFACE_LUMA_MAX + .003f)
                assertTrue(NativeThemeTokens.contrast(p.textPrimary, composite) >= 4.5f)
            }
        }
        for (p in TrainPilotPalettes.allPalettes) {
            assertEquals(p.surface, p.glassStateTile(p.primary, p.surface))
        }
    }

    @Test fun windowOverlayPaintIsOpaqueBoundedAndLegacyUnchanged() {
        for (seed in LiquidGlassPalettes.all) for (step in 0..100) {
            val p = seed.withGlassClarity(step)
            val fill = p.glassOverlayFill(Color.Black)
            assertEquals(1f, fill.alpha, .001f)
            assertTrue(fill.luminance() <= GlassOptics.SURFACE_LUMA_MAX + .003f)
            assertTrue(NativeThemeTokens.contrast(p.textPrimary, fill) >= 4.5f)
            assertTrue(NativeThemeTokens.contrast(p.textSecondary, fill) >= 4.5f)
        }
        for (p in TrainPilotPalettes.allPalettes) {
            assertEquals(p.surface, p.glassOverlayFill(p.surface))
        }
    }

    @Test fun photoViewerHasAtMostFivePercentTransparencyAndKeepsForegroundContrast() {
        for (seed in LiquidGlassPalettes.all) for (step in 0..100) {
            val p = seed.withGlassClarity(step)
            val fill = p.photoViewerBackground(Color.Black)
            val requested = subtleGlassWindowOpacity(step)
            val expectedAlpha8 = 255 - step * 255 / 2000
            assertEquals(expectedAlpha8 / 255f, fill.alpha, .0001f)
            assertTrue(fill.alpha >= requested - .0001f)
            assertTrue(fill.alpha - requested < 1f / 255f + .0001f)
            assertTrue(fill.alpha >= .95f)
            val brightest = fill.compositeOver(Color.White)
            assertTrue(brightest.luminance() <= GlassOptics.SURFACE_LUMA_MAX + .003f)
            assertTrue(NativeThemeTokens.contrast(p.textPrimary, brightest) >= 4.5f)
            assertTrue(NativeThemeTokens.contrast(p.textSecondary, brightest) >= 4.5f)
        }
        for (p in TrainPilotPalettes.allPalettes) {
            assertEquals(p.surface.copy(alpha = 1f), p.photoViewerBackground(p.surface))
        }
        val viewer = File("src/main/java/com/repforge/app/ui/progress/NativeProgressPhotoGallery.kt").readText()
            .substringAfter("if (selected != null)")
        assertTrue(viewer.contains(".background(palette.photoViewerBackground(colors.bg))"))
        assertTrue(viewer.contains("if (palette.isLiquidGlass) Modifier else Modifier.trainPilotScreenBackdrop(palette)"))
    }

    @Test fun coachDividerAndProgramDayPaintRespectThemeAndSelection() {
        assertEquals(GlassMaterialRole.Button, programDayGlassRole(false))
        assertEquals(GlassMaterialRole.Selection, programDayGlassRole(true))
        for (seed in LiquidGlassPalettes.all) for (clarity in listOf(0, 50, 65, 100)) {
            val p = seed.withGlassClarity(clarity)
            assertEquals(p.textPrimary.copy(alpha = .16f), p.coachAdviceDividerColor())
            val idle = p.programDayFill(false)
            val active = p.programDayFill(true)
            assertEquals(p.glassControlFill(p.surface, GlassMaterialRole.Button), idle)
            assertEquals(p.glassControlFill(p.primary, GlassMaterialRole.Selection).alpha, active.alpha, .01f)
            assertTrue(active.alpha > idle.alpha)
            assertTrue(active.blue > active.red)
            assertTrue(active.blue > active.green)
            for (selected in listOf(false, true)) {
                val fill = p.programDayFill(selected)
                assertTrue(fill.alpha in 0.01f..0.45f)
                val composite = fill.compositeOver(GlassOptics.lensWorstCase)
                assertTrue(NativeThemeTokens.contrast(p.programDayTextColor(selected), composite) >= 4.5f)
            }
        }
        for (p in TrainPilotPalettes.allPalettes) {
            assertEquals(p.cardBorder, p.coachAdviceDividerColor())
            assertEquals(p.surface, p.programDayFill(false))
            assertEquals(p.primary, p.programDayFill(true))
            assertEquals(p.textPrimary, p.programDayTextColor(false))
            assertEquals(p.onPrimary, p.programDayTextColor(true))
        }
    }

    @Test fun navigationUsesSingleHeaderLensAndNestedLightControls() {
        fun source(path: String) = File("src/main/java/com/example/$path").readText()
        val header = source("ui/components/common/TopHeaderAndNav.kt")
        val pill = source("ui/components/common/GlassSelectionPill.kt")
        assertTrue(header.contains("glassHeaderControl("))
        assertFalse(pill.contains(".glassHeaderMaterial("))
        assertTrue(pill.contains(".glassHeaderControl("))
        assertTrue(source("ui/theme/GlassAwareScaffold.kt").contains("content(bodyPadding, capture,"))
    }
}
