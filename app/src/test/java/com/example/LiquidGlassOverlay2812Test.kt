package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.ui.components.TrainPilotScreenSheet
import com.example.ui.components.trainPilotAnimatedExpansion
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.example.ui.theme.*
import com.repforge.app.ui.theme.NativeThemeTokens
import org.junit.Assert.*
import org.junit.Test

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-mdpi")
class LiquidGlassOverlay2812Test {
    @get:Rule val compose = createComposeRule()

    @Test fun sheetSceneDoesNotFollowRootClock() {
        val root = GlassSceneState().apply { seconds = 12f }
        val frozen = freezeSheetScene(root)
        root.seconds = 48f
        assertNotSame(root, frozen)
        assertEquals(12f, frozen.seconds, 0f)
        assertEquals(0f, freezeSheetScene(null).seconds, 0f)
    }

    @Test fun sheetHeightChangesWithoutAnIntermediateContentAnimation() {
        val tall = mutableStateOf(false)
        val opened = mutableStateOf(true)
        val measuredHeights = mutableListOf<Int>()
        compose.setContent {
            TrainPilotTheme {
                if (opened.value) TrainPilotScreenSheet(onDismiss = { opened.value = false }, testTag = "motion_sheet") {
                    Box(Modifier.testTag("motion_body").fillMaxWidth()
                        .onSizeChanged { measuredHeights += it.height }.trainPilotAnimatedExpansion()
                        .height(if (tall.value) 360.dp else 120.dp))
                }
            }
        }
        val body = compose.onNodeWithTag("motion_body")
        fun height() = body.getUnclippedBoundsInRoot().let { it.bottom - it.top }
        assertEquals(120.dp, height())
        val shortPx = compose.runOnIdle { measuredHeights.last() }
        compose.runOnIdle { measuredHeights.clear(); tall.value = true }
        compose.waitForIdle()
        assertEquals(360.dp, height())
        val tallPx = compose.runOnIdle {
            assertEquals("No animated intermediate heights while growing", 1, measuredHeights.distinct().size)
            measuredHeights.last()
        }
        assertEquals(shortPx * 3, tallPx)
        compose.runOnIdle { measuredHeights.clear(); tall.value = false }
        compose.waitForIdle()
        assertEquals(120.dp, height())
        compose.runOnIdle {
            assertEquals("No animated intermediate heights while shrinking", listOf(shortPx), measuredHeights.distinct())
        }
        compose.onNodeWithTag("motion_sheet").performTouchInput { swipeDown() }
        compose.waitForIdle()
        body.assertDoesNotExist()
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

    @Test fun slidingSheetPaintUsesRealWindowTransparencyWithBoundedContrast() {
        for (seed in LiquidGlassPalettes.all) for (step in 0..100) {
            val p = seed.withGlassClarity(step)
            val fill = p.slidingSheetBackground(Color.Black)
            val requested = subtleGlassWindowOpacity(step)
            val expectedAlpha8 = 255 - step * 255 / 2000
            assertEquals(expectedAlpha8 / 255f, fill.alpha, .0001f)
            assertTrue(fill.alpha >= requested - .0001f)
            assertTrue(fill.alpha - requested < 1f / 255f + .0001f)
            assertTrue(fill.alpha >= .95f)
            assertEquals(p.photoViewerBackground(Color.Black).alpha, fill.alpha, .001f)
            val brightest = fill.compositeOver(Color.White)
            assertTrue(brightest.luminance() <= GlassOptics.SURFACE_LUMA_MAX + .003f)
            assertTrue(NativeThemeTokens.contrast(p.textPrimary, brightest) >= 4.5f)
            assertTrue(NativeThemeTokens.contrast(p.textSecondary, brightest) >= 4.5f)
            assertEquals(1f, p.glassOverlayFill(Color.Black).alpha, .001f)
        }
        for (p in TrainPilotPalettes.allPalettes) {
            assertEquals(p.surface, p.slidingSheetBackground(p.surface))
        }
        assertEquals(1f, subtleGlassWindowOpacity(-1), .001f)
        assertEquals(.95f, subtleGlassWindowOpacity(101), .001f)
        val source = java.io.File("src/main/java/com/example/ui/components/TrainPilotModalBottomSheet.kt").readText()
        assertTrue(source.contains("palette.slidingSheetBackground(containerColor)"))
        assertTrue(source.contains("containerColor = fill"))
        assertFalse(source.contains("drawGlassSceneCompose"))
        assertFalse(source.contains("freezeSheetScene("))
    }

    @Test fun slidingSheetsRetainPaletteHueInsteadOfNeutralGrey() {
        for (step in 0..100) {
            fun fill(key: String) = LiquidGlassPalettes.all.first { it.key == key }
                .withGlassClarity(step).slidingSheetBackground(Color.Black)
            val ice = fill("glass_ice")
            val amber = fill("glass_amber")
            val emerald = fill("glass_emerald")
            val rose = fill("glass_rose")
            assertTrue("Ice loses its blue hue at $step: $ice", ice.blue - ice.red > .08f)
            assertTrue("Gold loses its yellow hue at $step: $amber", amber.red - amber.blue > .08f)
            assertTrue("Emerald loses its green hue at $step: $emerald", emerald.green - emerald.red > .08f)
            assertTrue("Rose loses its pink hue at $step: $rose", rose.red - rose.green > .08f)
            assertEquals(ice.alpha, amber.alpha, .0001f)
            assertTrue(ice.copy(alpha = 1f).luminance() >= .065f)
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
