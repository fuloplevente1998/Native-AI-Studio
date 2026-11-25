package com.example

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.example.model.SubScreen
import com.example.ui.theme.*
import com.repforge.app.GlassBackdropImages
import com.repforge.app.ui.theme.NativeThemeTokens
import com.repforge.app.ui.theme.TrainPilotActionAppearance
import com.repforge.app.ui.theme.TrainPilotFrames
import com.repforge.app.ui.theme.liquidGlassDropsFrame
import com.repforge.app.ui.theme.liquidGlassDropsStroke
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

/** 2809/1–3: living backdrop rules and guard, the single frame gate, inner tile tint. */
class LiquidGlass2809Test {
    private val glass = LiquidGlassPalettes.all
    private val legacy = TrainPilotPalettes.allPalettes

    // ---- 1: backdrop enable/disable ----

    @Test fun backdropOnlyForGlassAndOffOnTheMeasurementScreens() {
        for (p in legacy) for (motion in listOf(true, false)) {
            assertEquals(GlassBackdropMode.OFF, GlassScene.mode(p.isLiquidGlass, glassOpticsEnabledFor(SubScreen.NONE), motion))
            assertFalse(GlassScene.usesImage(p.isLiquidGlass, "glass_backdrop/backdrop_" + "a".repeat(32) + ".jpg"))
        }
        for (p in glass) {
            assertEquals(GlassBackdropMode.LIVING, GlassScene.mode(p.isLiquidGlass, glassOpticsEnabledFor(SubScreen.NONE), true))
            for (measuring in listOf(SubScreen.ACTIVE_STRENGTH, SubScreen.ACTIVE_TIMED, SubScreen.ACTIVE_BILATERAL,
                SubScreen.ACTIVE_OUTDOOR_GPS)) {
                assertEquals("$measuring", GlassBackdropMode.OFF,
                    GlassScene.mode(p.isLiquidGlass, glassOpticsEnabledFor(measuring), true))
            }
            assertTrue(GlassScene.usesImage(p.isLiquidGlass, "x"))
            assertFalse("no image chosen", GlassScene.usesImage(p.isLiquidGlass, ""))
        }
    }

    @Test fun reducedMotionFreezesTheBackdropAndAHiddenAppDoesNotTick() {
        val reduced = GlassScene.mode(true, true, GlassMotion.enabled(0f))
        assertEquals(GlassBackdropMode.STATIC, reduced)
        assertFalse(GlassScene.ticks(reduced, visible = true))
        assertTrue(GlassScene.ticks(GlassBackdropMode.LIVING, visible = true))
        assertFalse("static photo needs no clock", GlassScene.ticks(GlassBackdropMode.LIVING, visible = true, hasPhoto = true))
        assertTrue("fallback after photo removal", GlassScene.ticks(GlassBackdropMode.LIVING, visible = true, hasPhoto = false))
        assertFalse("paused when not visible", GlassScene.ticks(GlassBackdropMode.LIVING, visible = false))
        assertFalse(GlassScene.ticks(GlassBackdropMode.OFF, visible = true))
        assertTrue("cheap clock", GlassScene.FRAME_MS >= 50L)
    }

    @Test fun blobsDriftSlowlyAndStayNearTheScreen() {
        val w = 1080f
        val h = 2340f
        val start = GlassScene.blobs(w, h, 0f)
        assertEquals(GlassScene.blobCount, start.size)
        assertEquals("static frame is deterministic", start, GlassScene.blobs(w, h, 0f))
        val later = GlassScene.blobs(w, h, 20f)
        assertTrue("living backdrop moves", start.zip(later).any { (a, b) -> abs(a.x - b.x) + abs(a.y - b.y) > 10f })
        for (t in 0..600 step 7) for (b in GlassScene.blobs(w, h, t.toFloat())) {
            assertTrue(b.x in -GlassScene.DRIFT * w..(1 + GlassScene.DRIFT) * w)
            assertTrue(b.y in -GlassScene.DRIFT * h..(1 + GlassScene.DRIFT) * h)
            assertTrue(b.radius > w * .3f && b.strength in 0f..1f)
        }
        // One 83 ms tick moves a blob by well under a pixel-scale jump.
        val a = GlassScene.blobs(w, h, 10f)
        val b = GlassScene.blobs(w, h, 10f + GlassScene.FRAME_MS / 1000f)
        assertTrue(a.zip(b).all { (p, q) -> abs(p.x - q.x) < 4f && abs(p.y - q.y) < 4f })
        assertEquals(1.5f, GlassScene.advance(1f, .5f), 1e-6f)
        assertEquals("a long pause never jumps the drift", 2f, GlassScene.advance(1f, 3600f), 1e-6f)
    }

    @Test fun backdropIsColourfulButGuardedForText() {
        val grid = (0..10).flatMap { r -> (0..10).flatMap { g -> (0..10).map { b -> Color(r / 10f, g / 10f, b / 10f) } } }
        for (c in grid) {
            val guarded = GlassScene.guard(c)
            // Compose Color stores 8-bit sRGB channels: allow one quantum, still below the lens worst case.
            assertTrue("$c", guarded.luminance() <= GlassScene.lumaCap + .002f)
            assertTrue(guarded.luminance() < GlassOptics.lensWorstCase.luminance())
            if (c.luminance() <= GlassScene.lumaCap * GlassScene.KNEE - 1e-4f)
                assertEquals("dark colours pass unchanged", c.red, guarded.red, 1e-3f)
        }
        // Compose path (API 31–32): the linear image dim keeps even a white pixel at the guard.
        val dim = GlassScene.imageScale
        assertTrue(Color(dim, dim, dim).luminance() <= GlassScene.lumaCap + .002f)
        for (seed in glass) {
            val tones = GlassScene.tones(seed)
            val hues = tones.map { GlassScene.hueOf(it) }.sorted()
            assertTrue("${seed.key} hues spread", hues.last() - hues.first() > 90f)
            for (tone in GlassScene.guardedTones(seed)) {
                assertTrue("${seed.key} stays colourful", maxOf(tone.red, tone.green, tone.blue) -
                    minOf(tone.red, tone.green, tone.blue) > .08f)
            }
            for (clarity in 0..100 step 10) {
                val p = seed.withGlassClarity(clarity)
                val surfaces = GlassScene.guardedTones(p) + GlassScene.base(p, 0f) + GlassScene.base(p, 1f) +
                    grid.filterIndexed { i, _ -> i % 37 == 0 }.map(GlassScene::guard) + GlassScene.guard(Color.White)
                for (surface in surfaces) {
                    for (text in listOf(p.textPrimary, p.textSecondary, p.primary, p.success, p.warning, p.danger))
                        assertTrue("${p.key} $clarity $text on $surface", NativeThemeTokens.contrast(text, surface) >= 4.5f)
                    assertTrue(NativeThemeTokens.contrast(p.cardBorder, surface) >= 3f)
                }
            }
        }
    }

    @Test fun imageCropCoversTheSceneAndPathsAreValidated() {
        val (scale, at) = GlassScene.crop(1440, 960, 1080f, 2340f)
        assertTrue(1440 * scale >= 1080f - 1e-3f && 960 * scale >= 2340f - 1e-3f)
        assertEquals(0f, at.y, 1e-3f)
        val (w, h) = GlassBackdropImages.storedSize(4000, 3000)
        assertEquals(1440, w)
        assertEquals(1080, h)
        val valid = "glass_backdrop/backdrop_" + "0123456789abcdef".repeat(2) + ".jpg"
        assertTrue(GlassBackdropImages.isValidPath(valid))
        for (bad in listOf("", "../databases/trainpilot.db", "/data/user/0/x.jpg", "glass_backdrop/../x.jpg",
            "workout_photos/" + "a".repeat(32) + ".jpg", valid + "x")) {
            assertFalse(bad, GlassBackdropImages.isValidPath(bad))
            assertEquals("", GlassBackdropImages.portable(bad))
        }
        assertEquals(valid, GlassBackdropImages.portable(valid))
    }

    // ---- 2: single frame gate ----

    @Test fun glassDropsNeutralFramesAndKeepsSemanticOnes() {
        for (p in glass) {
            val selection = p.selectionColors()
            for (neutral in listOf(BorderStroke(1.dp, p.cardBorder), BorderStroke(1.dp, p.sectionBorder),
                BorderStroke(.5.dp, p.fieldOutline()),
                TrainPilotFrames.page(p.cardBorder, p.primary), TrainPilotFrames.control(p.cardBorder),
                TrainPilotFrames.stroke(1.dp, p.textPrimary),
                BorderStroke(1.dp, Brush.linearGradient(listOf(p.primary.copy(alpha = .55f), p.cardBorder))))) {
                assertTrue("${p.key} drops $neutral", liquidGlassDropsStroke(p, neutral))
            }
            for (semantic in listOf(p.primary, p.danger, p.success, p.warning,
                TrainPilotActionAppearance.DARK_SAVE_OUTLINE, TrainPilotActionAppearance.DARK_NEGATIVE_OUTLINE)) {
                for (alpha in listOf(1f, .7f, .45f)) {
                    assertFalse("${p.key} keeps $semantic", liquidGlassDropsStroke(p, BorderStroke(1.dp, semantic.copy(alpha = alpha))))
                    assertFalse(liquidGlassDropsFrame(p, semantic.copy(alpha = alpha)))
                }
                assertFalse(liquidGlassDropsStroke(p, TrainPilotFrames.control(semantic, selected = true)))
            }
            // FIXES_2809 #2: selection is semantic in glass (accent-derived) and keeps its frame.
            assertFalse("${p.key} keeps selection", liquidGlassDropsStroke(p, BorderStroke(1.dp, selection.border)))
            assertFalse("no stroke, nothing to drop", liquidGlassDropsStroke(p, null))
        }
    }

    @Test fun legacyPalettesAndCustomThemesKeepEveryFrame() {
        val custom = CustomThemeDraft.fromStyle(CustomThemeStyle.CONTRAST, "x").let {
            requireNotNull(resolveCustomTheme(it, newCustomThemeKey())).palette }
        for (p in legacy + custom) {
            for (stroke in listOf(BorderStroke(1.dp, p.cardBorder), TrainPilotFrames.page(p.sectionBorder, p.primary),
                BorderStroke(1.dp, Brush.linearGradient(listOf(p.sectionBorder, p.cardBorder))), TrainPilotFrames.control(p.textPrimary)))
                assertFalse("${p.key}", liquidGlassDropsStroke(p, stroke))
        }
    }

    @Test fun framesGoThroughTheGateAtTheReportedPlaces() {
        fun src(path: String) = java.io.File("src/main/java/$path").readText()
        assertTrue(src("com/example/MainActivity.kt").contains(".trainPilotFrame(TrainPilotFrames.page(palette.cardBorder, palette.primary)"))
        assertFalse(src("com/example/MainActivity.kt").contains(".border(TrainPilotFrames.page"))
        assertFalse("nav pill uses rim light", src("com/example/ui/components/common/GlassSelectionPill.kt").contains(".border("))
        assertTrue(src("com/example/ui/components/TrainPilotControls.kt").contains(
            "trainPilotFrameStroke(BorderStroke(1.dp, if (selected) selection.border else p.cardBorder))"))
        assertTrue("search row", src("com/example/ui/screens/log/LogTabScreen.kt").contains(".trainPilotFrame(1.dp, palette.cardBorder, shape)"))
    }

    // ---- 3: inner tiles ----

    @Test fun tileTintFallsContinuouslyWithClarityAndCardsToo() {
        var lastTile = Float.MAX_VALUE
        var lastCard = Float.MAX_VALUE
        var lastCoat = Float.MAX_VALUE
        val cards = (0..100).map { checkNotNull(glass.first().withGlassClarity(it).customCardPaint).alpha }
        for (step in 0..100) {
            val tile = glassTileAlpha(step)
            val card = cards[step]
            assertTrue("tile $step", tile < lastTile)
            // 2810/6: the card tint spans .40 -> .06 (.0034 per step, under one 8-bit alpha quantum), so the
            // stored paint may repeat for one step: the exact tint falls strictly, the paint never rises and
            // strictly falls over every two steps.
            val coat = GlassOptics.from(step).coat
            assertTrue("coat $step", coat < lastCoat)
            assertTrue("card $step", card <= lastCard)
            if (step >= 2) assertTrue("card $step over two steps", card < cards[step - 2])
            lastCoat = coat
            if (step > 0) assertTrue(lastTile - tile <= .01f)
            lastTile = tile
            lastCard = card
        }
        assertTrue("low-opacity at the Clear end", glassTileAlpha(100) <= .15f)
        // 2810/6: the tile tint is a neutral below the glass ceiling (no longer the dark paint at .66), so a
        // tile can never lift a glass composite above it; it stays clearly visible at the Frosted end.
        assertTrue(glassTileAlpha(0) >= .14f && glassTileAlpha(0) > glassTileAlpha(100))
        for (elevated in listOf(false, true))
            assertTrue(glassTilePaint(50, elevated).copy(alpha = 1f).luminance() < GlassOptics.SURFACE_LUMA_MAX)
        assertEquals(glassTileAlpha(0), glassTileAlpha(-5), 0f)
        assertEquals(glassTileAlpha(100), glassTileAlpha(180), 0f)
        for (p in glass) for (step in listOf(0, 50, 100)) {
            val tinted = p.withGlassClarity(step)
            assertEquals(glassTileAlpha(step), tinted.glassTile(tinted.surfaceCard).alpha, 1f / 255f)
            assertEquals(glassTileAlpha(step), tinted.glassTile(tinted.surfaceElevated, elevated = true).alpha, 1f / 255f)
        }
    }

    @Test fun legacyTilesAreUnchangedAndGlassTextStaysReadableOnTiles() {
        for (p in legacy) {
            assertEquals(p.surfaceCard, p.glassTile(p.surfaceCard))
            assertEquals(p.surfaceElevated, p.glassTile(p.surfaceElevated, elevated = true))
            assertEquals(p.surface, p.glassTile(p.surface))
        }
        for (seed in glass) for (step in 0..100 step 10) {
            val p = seed.withGlassClarity(step)
            val optics = GlassOptics.from(step)
            val under = listOf(GlassOptics.lensWorstCase, GlassOptics.sceneWorstCase(p.background), p.surface, p.background,
                GlassScene.guard(Color.White), GlassOptics.blurOnlyWorstCase(step),
                GlassContrast.grey(GlassContrast.worstComposite(optics.coat, optics.backdropCap)))
            for (u in under) for (elevated in listOf(false, true)) {
                val tile = p.glassTile(p.surfaceCard, elevated).compositeOver(u)
                for (text in listOf(p.textPrimary, p.textSecondary, p.primary, p.danger, p.success, p.warning))
                    assertTrue("${p.key} $step $text", NativeThemeTokens.contrast(text, tile) >= 4.5f)
            }
        }
    }
}
