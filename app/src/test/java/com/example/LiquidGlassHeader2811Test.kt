package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.example.model.MainTab
import com.example.model.SubScreen
import com.example.ui.theme.*
import com.repforge.app.ui.theme.NativeThemeTokens
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

/**
 * 2811/1 device round 2 ("Fejléc nem átlátszó"): the header band must be the same continuous surface as the
 * page at scroll top (no luminance or colour step at its bottom edge), scrolled content must be recorded under
 * it on every top-level screen, and glyphs on it stay readable.
 */
class LiquidGlassHeader2811Test {
    private val glass = LiquidGlassPalettes.all
    private val clarities = listOf(0, 50, 65, 100)

    private fun pagePixels(p: TrainPilotPalette): List<Color> {
        val grid = (0..6).flatMap { r -> (0..6).flatMap { g -> (0..6).map { b -> Color(r / 6f, g / 6f, b / 6f) } } }
        return GlassScene.guardedTones(p) + listOf(0f, .03f, .07f, 1f).map { GlassScene.base(p, it) } +
            grid.map { GlassScene.guard(it) } + GlassScene.guard(Color.White)
    }

    @Test fun headerBandHasNoStepAgainstThePageBackdrop() {
        for (seed in glass) for (clarity in clarities) {
            val p = seed.withGlassClarity(clarity)
            val nav = GlassOptics.from(clarity, GlassRole.Navigation)
            assertTrue(nav.role.matchesPage && nav.coat == 0f && nav.specular == 0f && nav.shadow == 0f && nav.elevation == 0f)
            assertTrue("the header limit never cuts the guarded page", nav.backdropCap >= GlassScene.lumaCap)
            for (page in pagePixels(p)) {
                val header = GlassContrast.lensCentre(page, nav)
                assertEquals("${p.key} $clarity luminance step on $page", page.luminance(), header.luminance(), .002f)
                for ((a, b) in listOf(page.red to header.red, page.green to header.green, page.blue to header.blue))
                    assertTrue("${p.key} $clarity colour step on $page", abs(a - b) <= 2f / 255f)
            }
        }
        // The pre-fix cause, kept as evidence: the card material over the same page is visibly darker/greyer.
        val p = glass[2].withGlassClarity(65)
        val panel = GlassOptics.from(65, GlassRole.Panel)
        assertTrue(pagePixels(p).any { abs(GlassContrast.lensCentre(it, panel).luminance() - it.luminance()) > .01f })
    }

    @Test fun glyphsOnTheHeaderStayReadable() {
        for (seed in glass) for (clarity in clarities) {
            val p = seed.withGlassClarity(clarity)
            val nav = GlassOptics.from(clarity, GlassRole.Navigation)
            // Brightest header pixel over ANY content scrolled under it (limited like the shader).
            val worst = GlassContrast.worstComposite(nav.coat, nav.backdropCap)
            assertTrue(worst <= GlassOptics.SURFACE_LUMA_MAX)
            val surfaces = listOf(GlassContrast.grey(worst), GlassContrast.grey(GlassScene.lumaCap),
                GlassOptics.blurOnlyWorstCase(clarity)) + listOf(false, true).map { p.glassTile(p.surface, it).compositeOver(GlassContrast.grey(worst)) }
            val selection = p.selectionColors()
            for (s in surfaces) {
                for (text in listOf(p.textPrimary, p.textSecondary, p.primary, selection.foreground))
                    assertTrue("${p.key} $clarity $text on $s", NativeThemeTokens.contrast(text, s) >= 4.5f)
                assertTrue("icon outline", NativeThemeTokens.contrast(selection.border, s) >= 3f)
            }
        }
    }

    @Test fun scrolledContentIsRecordedUnderTheHeaderOnEveryTopLevelScreen() {
        for (tab in listOf(MainTab.HOME, MainTab.PROGRAMS, MainTab.WORKOUT, MainTab.PROGRESS, MainTab.HEALTH, MainTab.LOG, MainTab.COACH))
            assertTrue("$tab", glassUnderlapRoute(tab, SubScreen.NONE))
        assertTrue(glassUnderlapRoute(MainTab.HOME, SubScreen.SETTINGS_MAIN))
        for (measuring in listOf(SubScreen.ACTIVE_STRENGTH, SubScreen.ACTIVE_TIMED, SubScreen.ACTIVE_BILATERAL, SubScreen.ACTIVE_OUTDOOR_GPS))
            assertFalse(glassUnderlapRoute(MainTab.WORKOUT, measuring))
        fun src(path: String) = java.io.File("src/main/java/com/example/$path").readText()
        for (path in listOf("ui/screens/log/LogTabScreen.kt", "ui/screens/health/CoachDetailScreen.kt"))
            assertTrue(path, src(path).contains("LocalGlassTopInset.current"))
        // The scene lens of a page-matched surface samples the guarded page, not the raw scene.
        assertTrue(src("ui/theme/GlassShaders.kt").contains("guard = optics.role.matchesPage"))
        assertTrue(src("ui/theme/TrainPilotGlassEngine.kt").contains("if (role.matchesPage) GlassScene.lumaCap else optics.backdropCap"))
    }
}
