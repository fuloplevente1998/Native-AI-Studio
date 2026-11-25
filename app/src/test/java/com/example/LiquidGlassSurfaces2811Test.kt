package com.example

import androidx.compose.material3.ButtonColors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.example.ui.theme.*
import com.repforge.app.ui.theme.NativeThemeTokens
import org.junit.Assert.*
import org.junit.Test

/**
 * 2811 G2–G4: the shared glass fills of buttons, cells, chips, selection and native cards. Solid blocks
 * become translucent state tints in the glass family only; every tint keeps glass composites under the
 * ceiling (text >= 4.5:1, outlines >= 3:1); legacy palettes and custom themes keep their exact colours.
 */
class LiquidGlassSurfaces2811Test {
    private val glass = LiquidGlassPalettes.all
    private val custom = CustomThemeDraft.fromStyle(CustomThemeStyle.CONTRAST, "x").let {
        requireNotNull(resolveCustomTheme(it, newCustomThemeKey())).palette }
    private val legacy = TrainPilotPalettes.allPalettes + custom
    private val ceiling = GlassOptics.SURFACE_LUMA_MAX

    private fun colors(container: Color, content: Color) = ButtonColors(container, content, container.copy(alpha = .5f), content.copy(alpha = .38f))

    @Test fun stateTintsStayUnderTheCeilingAndKeepTextReadable() {
        assertTrue(GLASS_TILE_LUMA < ceiling)
        for (seed in glass) for (step in listOf(0, 50, 65, 100)) {
            val p = seed.withGlassClarity(step)
            val under = listOf(GlassOptics.lensWorstCase, GlassContrast.grey(GlassScene.lumaCap), p.surface, p.background)
            for (accent in listOf(p.primary, p.danger, p.success, p.warning, Color.White, Color(0xFFFFD000)))
                for (strong in listOf(false, true)) {
                    val tint = p.glassStateTile(accent, Color.Magenta, strong)
                    assertTrue(tint.alpha in .05f..(.45f))
                    for (u in under) {
                        val surface = tint.compositeOver(u)
                        assertTrue("${p.key} $step $accent", surface.luminance() <= ceiling + .003f)
                        for (text in listOf(p.textPrimary, p.textSecondary, p.primary, p.selectionColors().foreground))
                            assertTrue("${p.key} $step $text on $surface", NativeThemeTokens.contrast(text, surface) >= 4.5f)
                        assertTrue(NativeThemeTokens.contrast(p.selectionColors().border, surface) >= 3f)
                    }
                }
        }
    }

    @Test fun solidButtonsBecomeGlassOnlyInTheGlassFamily() {
        for (p in legacy) {
            val c = colors(p.primary, p.onPrimary)
            assertSame(c, p.glassButtonColors(c))
            assertEquals(p.primary.copy(alpha = .12f), p.glassStateTile(p.primary, p.primary.copy(alpha = .12f)))
        }
        for (seed in glass) {
            val p = seed.withGlassClarity(65)
            // Accent (save), white/grey (secondary), dark red (delete) and transparent (text) buttons.
            for ((container, content) in listOf(p.primary to p.onPrimary, Color.White to Color.Black,
                Color(0xFF9E9E9E) to Color.Black, Color(0xFF3E2024) to Color(0xFFFFB4B4), Color.Transparent to p.primary)) {
                val g = p.glassButtonColors(colors(container, content))
                if (container.alpha < .05f) assertEquals(container, g.containerColor)
                else {
                    assertTrue("$container translucent", g.containerColor.alpha <= .45f)
                    assertTrue(g.containerColor.compositeOver(GlassOptics.lensWorstCase).luminance() <= ceiling + .003f)
                }
                assertTrue("$content readable", NativeThemeTokens.contrast(g.contentColor,
                    g.containerColor.compositeOver(GlassOptics.lensWorstCase)) >= 4.5f)
            }
        }
    }

    @Test fun selectionTintedAndNativeCardsAreTranslucentInGlassAndExactElsewhere() {
        for (p in TrainPilotPalettes.allPalettes) {
            assertEquals(1f, p.selectionColors().surface.alpha)
            assertEquals(p.surfaceCard, nativeStudioColors(p).card)
            assertEquals(p.surfaceElevated, nativeStudioColors(p).card2)
        }
        for (seed in glass) for (step in listOf(0, 65, 100)) {
            val p = seed.withGlassClarity(step)
            for (fill in listOf(p.selectionColors().surface, p.tintedSurface(.12f), p.tintedSurface(.22f),
                nativeStudioColors(p).card, nativeStudioColors(p).card2)) {
                assertTrue("${p.key} $step $fill", fill.alpha < .5f)
                assertTrue(fill.compositeOver(GlassOptics.lensWorstCase).luminance() <= ceiling + .003f)
            }
        }
    }

    @Test fun reportedSolidSurfacesGoThroughTheGlassPath() {
        fun src(path: String) = java.io.File("src/main/java/com/example/$path").readText()
        // G2 calendar: cells via the shared cell look, buttons via the shared (glass-aware) button.
        val calendar = src("ui/screens/workout/CalendarPlannerScreen.kt")
        assertTrue(calendar.contains("glassCalendarCell(palette, isSelected,"))
        assertFalse(calendar.contains("plannedEv != null -> palette.primary.copy(alpha = 0.12f)"))
        assertTrue(src("ui/screens/workout/PlannerControls.kt").contains("GlassRole.Selection"))
        // G3: every shared button passes through the glass colour mapping.
        assertTrue(java.io.File("src/main/java/com/repforge/app/ui/theme/TrainPilotPressFeedback.kt").readText()
            .contains("glassButtonColors(colors)"))
        // G4: active workout screens sit on the shared backdrop, not an opaque panel.
        for (screen in listOf("ActiveStrengthWorkoutScreen", "ActiveTimedWorkoutScreen", "ActiveBilateralWorkoutScreen")) {
            val text = src("ui/screens/measure/$screen.kt")
            assertTrue(screen, text.contains(".trainPilotScreenBackdrop(palette)"))
            assertFalse(screen, text.contains("            .background(palette.background)\n"))
        }
        for (path in listOf("ui/screens/settings/GoogleCloudSyncCard.kt", "ui/components/StudioRestTimerDock.kt",
            "ui/components/StudioWorkoutHandoverBar.kt", "ui/screens/settings/WatchAndSyncScreen.kt",
            "ui/screens/log/LogWorkoutDetailScreen.kt", "ui/screens/health/HealthTabScreen.kt"))
            assertTrue(path, src(path).contains("trainPilotGlassMaterial(palette, "))
    }

    /**
     * 2811 device round 3 sweep: no opaque palette fill (surface / surfaceCard / surfaceElevated / cardPaint) is
     * left on the live screens except the intentional opaque ones: separate dialog windows, map overlays (over
     * map tiles), dropdown menus, the custom-theme preview, the scroll-to-top button over content, the legacy
     * sheet branch and the replan dialog.
     */
    @Test fun noAccidentalOpaqueFillIsLeftOnTheLiveScreens() {
        val allowed = setOf("CompactCalendarDialog.kt", "ExerciseVideoDialog.kt", "StudioStrengthSetCard.kt",
            "TrainPilotModalBottomSheet.kt", "CustomThemePreview.kt", "PersonalPlannerModal.kt", "ScrollHelpers.kt",
            "MapAppearanceSelector.kt", "WorkoutRouteMapBox.kt", "StudioWorkoutModesDialog.kt", "LogRouteMapScreen.kt",
            "AddCustomExerciseDialog.kt", "CalendarPlannerScreen.kt")
        val opaque = Regex("""\bcolor = (palette|p)\.(surface|surfaceCard|surfaceElevated|cardPaint)\b|\.background\((palette|p)\.(surface|surfaceCard|surfaceElevated|cardPaint)\)""")
        val offenders = java.io.File("src/main/java/com/example/ui").walkTopDown()
            .filter { it.isFile && it.extension == "kt" && "/theme/" !in it.invariantSeparatorsPath && it.name !in allowed }
            .flatMap { f -> f.readLines().mapIndexedNotNull { i, line -> if (opaque.containsMatchIn(line)) "${f.name}:${i + 1}" else null } }
            .toList()
        assertTrue(offenders.joinToString("\n"), offenders.isEmpty())
        // Only the replan dialog stays opaque in the calendar (its window is a dialog).
        assertEquals(1, opaque.findAll(java.io.File("src/main/java/com/example/ui/screens/workout/CalendarPlannerScreen.kt").readText()).count())
    }
}
