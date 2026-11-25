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
import kotlin.math.pow

/**
 * 2810/2–6: iOS-like own glass. Behaviour, not UI nodes: the white-ish tint follows the slider within
 * the approved range, the backdrop limit keeps every composite under the ceiling (checked here with an
 * independent, finer grid), text stays >= 4.5:1, surfaces go through the glass path only in glass
 * palettes, and every top-nav tab is recorded under the glass header.
 */
class LiquidGlass2810Test {
    private val glass = LiquidGlassPalettes.all
    private val legacy = TrainPilotPalettes.allPalettes

    // Independent mirror of the sRGB/luminance maths (not the production helpers).
    private fun lin(c: Float) = if (c <= .04045f) c / 12.92f else ((c + .055f) / 1.055f).pow(2.4f)
    private fun srgb(l: Float) = if (l <= .0031308f) l * 12.92f else 1.055f * l.pow(1f / 2.4f) - .055f
    private fun luma(r: Float, g: Float, b: Float) = .2126f * lin(r) + .7152f * lin(g) + .0722f * lin(b)

    @Test fun themeCategoryTitleIsReadableOnAllEightGlassPalettes() {
        assertEquals((33..40).toList(), glass.map { it.id })
        for (seed in glass) for (step in listOf(0, 65, 100)) {
            val p = seed.withGlassClarity(step)
            val color = com.example.ui.components.paletteCategoryTitleColor(p)
            for (elevated in listOf(false, true)) {
                val tile = p.glassTile(p.surfaceElevated, elevated).compositeOver(GlassOptics.lensWorstCase)
                assertTrue("${p.key} $step $elevated", NativeThemeTokens.contrast(color, tile) >= 4.5f)
            }
        }
        for (p in legacy.filterNot { it.isLiquidGlass })
            assertEquals(Color.Unspecified, com.example.ui.components.paletteCategoryTitleColor(p))
    }

    @Test fun tintAlphaFollowsClarityMonotonicallyWithinTheApprovedRange() {
        var last = Float.MAX_VALUE
        for (step in 0..100) {
            val optics = GlassOptics.from(step)
            assertTrue("strictly thinner towards Clear at $step", optics.coat < last)
            if (step > 0) assertTrue("continuous at $step", last - optics.coat <= .005f)
            last = optics.coat
            for (p in glass) assertEquals(optics.coat, checkNotNull(p.withGlassClarity(step).customCardPaint).alpha, 1f / 255f)
        }
        assertEquals(.40f, GlassOptics.from(0).coat, 1e-6f)
        assertEquals(.06f, GlassOptics.from(100).coat, 1e-6f)
        val tint = GlassOptics.TINT
        assertTrue("white-ish tint", minOf(tint.red, tint.green, tint.blue) >= .9f)
        // Strong blur + vibrancy at Clear (approved: ~18–24 dp, ~1.8x), top rim ~45 % white.
        val clear = GlassOptics.from(100)
        assertTrue(clear.blurDp in 18f..24f)
        assertEquals(1.8f, clear.saturation, .05f)
        assertEquals(.45f, clear.specular, .02f)
        assertTrue(GlassOptics.from(0).blurDp > clear.blurDp)
    }

    @Test fun backdropLimitKeepsEveryCompositeUnderTheCeilingOnAFinerGrid() {
        val levels = (0..12).map { it / 12f }
        val tint = GlassOptics.TINT
        for (step in 0..100 step 5) {
            val optics = GlassOptics.from(step)
            val cap = optics.backdropCap
            assertTrue(cap in 0f..GlassOptics.SURFACE_LUMA_MAX)
            var worst = 0f
            for (r in levels) for (g in levels) for (b in levels) {
                val l = luma(r, g, b)
                // The shader limit: linear-light scale down to the cap (hue kept).
                val k = if (l <= cap || l == 0f) 1f else cap / l
                val br = if (k < 1f) srgb(lin(r) * k) else r
                val bg = if (k < 1f) srgb(lin(g) * k) else g
                val bb = if (k < 1f) srgb(lin(b) * k) else b
                val a = optics.coat
                worst = maxOf(worst, luma(a * tint.red + (1 - a) * br, a * tint.green + (1 - a) * bg, a * tint.blue + (1 - a) * bb))
            }
            assertTrue("$step: $worst", worst <= GlassOptics.SURFACE_LUMA_MAX)
            // API 31–32: every channel <= scale x saturation gain (Android weights), then the same tint.
            val s = optics.saturation
            val gains = listOf(.213f, .715f, .072f).map { it * (1 - s) + s * 1f }
            val k = optics.blurOnlyScale
            val a = optics.coat
            val blurOnly = luma(a * tint.red + (1 - a) * k * gains[0], a * tint.green + (1 - a) * k * gains[1],
                a * tint.blue + (1 - a) * k * gains[2])
            assertTrue("$step blur-only $blurOnly", blurOnly <= GlassOptics.SURFACE_LUMA_MAX)
            // Text on the brightest of these composites (contrast depends on luminance only).
            val brightest = Color(srgb(maxOf(worst, blurOnly)), srgb(maxOf(worst, blurOnly)), srgb(maxOf(worst, blurOnly)))
            for (seed in glass) {
                val p = seed.withGlassClarity(step)
                for (text in listOf(p.textPrimary, p.textSecondary, p.primary, p.success, p.warning, p.danger,
                    p.heartAccent, p.sleepAccent, p.selectionColors().foreground))
                    assertTrue("${p.key} $step $text", NativeThemeTokens.contrast(text, brightest) >= 4.5f)
                assertTrue(NativeThemeTokens.contrast(p.cardBorder, brightest) >= 3f)
                // Inner tiles over the brightest composite stay under the ceiling (a mix with a darker neutral).
                for (elevated in listOf(false, true)) {
                    val tile = glassTilePaint(step, elevated).compositeOver(brightest)
                    assertTrue(tile.luminance() <= GlassOptics.SURFACE_LUMA_MAX + .003f)
                }
            }
        }
        // The page itself is guarded below the ceiling, and the tint is not darkened to get there.
        assertTrue(GlassScene.lumaCap < GlassOptics.SURFACE_LUMA_MAX)
        assertTrue(GlassScene.lumaCap > .084f) // brighter than the 2809 page (lens peak .32 -> luminance .084)
    }

    @Test fun backdropIsBrighterAndLarger() {
        assertTrue(GlassScene.BLOB_STRENGTH >= .9f)
        val blobs = GlassScene.blobs(1080f, 2340f, 0f)
        assertTrue(blobs.all { it.radius >= 1080f * .7f })
        for (p in glass) for (tone in GlassScene.guardedTones(p)) {
            assertTrue(tone.luminance() <= GlassScene.lumaCap + .002f)
            assertTrue("${p.key} vivid", maxOf(tone.red, tone.green, tone.blue) - minOf(tone.red, tone.green, tone.blue) > .12f)
        }
    }

    @Test fun surfacesUseTheGlassPathOnlyInGlassPalettes() {
        val custom = CustomThemeDraft.fromStyle(CustomThemeStyle.CONTRAST, "x").let {
            requireNotNull(resolveCustomTheme(it, newCustomThemeKey())).palette }
        for (p in legacy + custom) {
            for (c in listOf(p.surface, p.surfaceCard, p.surfaceElevated, p.background, p.cardPaint)) {
                assertEquals(c, p.glassCardFill(c))
                assertEquals(c, p.glassTile(c))
            }
            assertSame(p, p.withGlassClarity(10))
        }
        for (p in glass) {
            assertEquals(Color.Transparent, p.glassCardFill(p.surface))
            assertNotEquals(p.surface, p.glassTile(p.surface))
        }
        // The reported surfaces are routed through the shared glass card path.
        fun src(path: String) = java.io.File("src/main/java/com/example/$path").readText()
        for (path in listOf("ui/screens/settings/SettingsMainScreen.kt", "ui/screens/workout/WorkoutTabScreen.kt",
            "ui/screens/WorkoutTabDayCards.kt", "ui/screens/workout/CalendarPlannerScreen.kt", "ui/components/WeeklyGoals.kt",
            "ui/components/StudioStrengthSetCard.kt", "ui/screens/measure/ActiveStrengthWorkoutScreen.kt",
            "ui/screens/measure/ActiveTimedWorkoutScreen.kt", "ui/screens/measure/ActiveBilateralWorkoutScreen.kt",
            "ui/screens/workout/ProgramsListScreen.kt", "ui/screens/progress/ProgressVolumeAndMuscle.kt",
            "ui/components/HomeCoachAdviceCard.kt", "ui/components/QuickWorkoutHubSheet.kt", "ui/screens/StudioStatisticsScreen.kt")) {
            val text = src(path)
            assertTrue(path, text.contains("glassCardFill(") && text.contains("trainPilotGlassMaterial("))
        }
        // Dialog windows stay opaque: the replan dialog keeps its plain card colour.
        val calendar = src("ui/screens/workout/CalendarPlannerScreen.kt")
        val dialog = calendar.substring(calendar.indexOf("androidx.compose.ui.window.Dialog("))
        assertTrue(dialog.substringBefore("Column(").contains("color = palette.surfaceCard,"))
    }

    @Test fun previousOpaquePathStaysForOldDevices() {
        for (step in listOf(0, 37, 100)) {
            val t = step / 100f
            assertEquals(GLASS_PAINT.copy(alpha = .82f - .64f * t), glassFallbackPaint(step))
        }
    }

    @Test fun everyTopNavTabIsRecordedUnderTheGlassHeader() {
        assertEquals(setOf(MainTab.HOME, MainTab.PROGRAMS, MainTab.WORKOUT, MainTab.PROGRESS, MainTab.HEALTH,
            MainTab.COACH, MainTab.LOG), GlassUnderlapTabs)
        for (tab in GlassUnderlapTabs) assertTrue("$tab", glassUnderlapRoute(tab, SubScreen.NONE))
        assertTrue(glassUnderlapRoute(MainTab.HOME, SubScreen.SETTINGS_MAIN))
        assertFalse(glassUnderlapRoute(MainTab.CALENDAR, SubScreen.NONE))
        assertFalse(glassUnderlapRoute(MainTab.CALENDAR, SubScreen.CALENDAR_PLANNER))
        for (tab in MainTab.entries) assertTrue("Coach detail from $tab", glassUnderlapRoute(tab, SubScreen.COACH_DETAIL))
        for (measuring in listOf(SubScreen.ACTIVE_STRENGTH, SubScreen.ACTIVE_TIMED, SubScreen.ACTIVE_BILATERAL, SubScreen.ACTIVE_OUTDOOR_GPS))
            assertFalse("$measuring", glassUnderlapRoute(MainTab.WORKOUT, measuring))
        // Each underlapped screen starts its scroll content with the safe-area spacer (else content would hide).
        fun src(path: String) = java.io.File("src/main/java/com/example/$path").readText()
        for (path in listOf("ui/screens/HomeScreen.kt", "ui/screens/workout/ProgramsListScreen.kt",
            "ui/screens/workout/WorkoutTabScreen.kt", "ui/screens/progress/ProgressTabScreen.kt",
            "ui/screens/health/HealthTabScreen.kt", "ui/screens/settings/SettingsMainScreen.kt",
            "ui/screens/health/CoachDetailScreen.kt", "ui/screens/log/LogTabScreen.kt"))
            assertTrue(path, src(path).contains("LocalGlassTopInset.current"))
        // LazyColumn content padding is the in-scroll spacer, not an outer Modifier.padding.
        val coach = src("ui/screens/health/CoachDetailScreen.kt").substringAfter("LazyColumn(").substringBefore("if (uiState.currentSubScreen")
        val journal = src("ui/screens/log/LogTabScreen.kt").substringAfter("LazyColumn(").substringBefore("CompactJournalSearchField(")
        assertTrue(coach.contains("contentPadding = PaddingValues(") && coach.contains("top = 16.dp + LocalGlassTopInset.current, bottom = 16.dp"))
        assertTrue(journal.contains("contentPadding = PaddingValues(") && journal.contains("top = 8.dp + LocalGlassTopInset.current, bottom = 24.dp"))
        // The route host (every tab) is composed inside the box that records the header's backdrop.
        val root = src("MainActivity.kt")
        val capture = root.indexOf(".then(glassCapture)")
        val host = root.indexOf("TrainPilotRouteContent(routeHostState)")
        assertTrue(capture in 0 until host)
        assertTrue(root.substring(capture, host).contains("CompositionLocalProvider(LocalGlassTopInset provides glassTopInset)"))
        val scaffold = src("ui/theme/GlassAwareScaffold.kt")
        assertTrue(scaffold.contains("glassUnderlapRoute(glassHost.currentTab, glassHost.currentSubScreen)"))
        assertTrue(scaffold.contains("bottom = innerPadding.calculateBottomPadding()"))
        assertFalse("no per-tab allow-list left", scaffold.contains("setOf(MainTab.HOME, MainTab.HEALTH)"))
    }

    @Test fun lightTextIsTheOnlyRiskAndTheCeilingCoversIt() {
        // Contrast against a surface depends only on its luminance: the grey worst case equals any hue.
        val grey = GlassOptics.lensWorstCase
        // Hues brighter than the ceiling (red, yellow, cyan, magenta), scaled down as the limit does.
        for (hue in listOf(0f, 60f, 180f, 300f)) {
            val c = Color.hsv(hue, 1f, 1f)
            val l = c.luminance()
            assertTrue(l > GlassOptics.SURFACE_LUMA_MAX)
            val k = GlassOptics.SURFACE_LUMA_MAX / l
            val scaled = Color(srgb(lin(c.red) * k), srgb(lin(c.green) * k), srgb(lin(c.blue) * k))
            assertEquals(grey.luminance(), scaled.luminance(), .004f)
            assertTrue(abs(NativeThemeTokens.contrast(Color.White, grey) - NativeThemeTokens.contrast(Color.White, scaled)) < .1f)
        }
    }
}
