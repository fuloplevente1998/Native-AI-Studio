package com.example

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.example.model.MainTab
import com.example.model.SubScreen
import com.example.ui.components.isActiveMeasurementScreen
import com.example.ui.theme.*
import com.example.ui.viewmodel.NavRoute
import com.example.ui.viewmodel.TrainPilotUiState
import org.junit.Assert.*
import org.junit.Test

/** 2808/10–12: glass gate during a running session, touch bubble rules, more transparency at Clear. */
class LiquidGlass2808Test {
    private val glass = LiquidGlassPalettes.all.first()

    // ---- 10 ----

    @Test fun aRunningSessionKeepsGlassOnEveryOtherScreen() {
        for (tab in MainTab.values()) {
            val home = TrainPilotUiState(currentTab = tab, currentSubScreen = SubScreen.NONE, isWorkoutSessionActive = true)
            assertTrue("$tab must keep glass while a workout runs", glass.glassEffectsActive(glassOpticsEnabledFor(home)))
        }
        for (screen in SubScreen.values()) {
            val state = TrainPilotUiState(currentTab = MainTab.WORKOUT, currentSubScreen = screen, isWorkoutSessionActive = true)
            assertEquals("$screen", !isActiveMeasurementScreen(screen), glassOpticsEnabledFor(state))
        }
    }

    @Test fun measurementScreenStaysCheapAlsoUnderAnOpenSheet() {
        val active = TrainPilotUiState(currentTab = MainTab.WORKOUT, currentSubScreen = SubScreen.ACTIVE_STRENGTH,
            isWorkoutSessionActive = true)
        assertFalse(glassOpticsEnabledFor(active))
        // The exercise library sheet over the strength screen: the screen underneath stays opaque.
        val sheet = TrainPilotUiState(currentTab = MainTab.WORKOUT, currentSubScreen = SubScreen.EXERCISE_LIBRARY,
            isWorkoutSessionActive = true, backStack = listOf(NavRoute(MainTab.WORKOUT, SubScreen.ACTIVE_STRENGTH)))
        assertFalse(glassOpticsEnabledFor(sheet))
        // Light glass (vector rim) remains there; legacy palettes never get it.
        assertTrue(glass.glassRimActive())
        assertFalse(TrainPilotPalettes.getById(1).glassRimActive())
    }

    @Test fun glassGateNoLongerReadsTheSessionFlag() {
        val source = java.io.File("src/main/java/com/repforge/app/MainActivity.kt").readText()
        assertTrue(source.contains("LocalGlassOpticsEnabled provides glassOpticsEnabledFor(uiState)"))
        assertFalse(source.contains("glassOpticsEnabledFor(uiState.isWorkoutSessionActive)"))
    }

    // ---- 11 ----

    @Test fun touchBubbleOnlyForGlassWithMotionOnAnEnabledControl() {
        assertTrue(GlassTouchBubble.enabled(glassActive = true, motionEnabled = true, controlEnabled = true))
        assertFalse("reduced motion", GlassTouchBubble.enabled(true, GlassMotion.enabled(0f), true))
        assertFalse("disabled control", GlassTouchBubble.enabled(true, true, false))
        assertFalse("legacy palette", GlassTouchBubble.enabled(
            TrainPilotPalettes.getById(1).glassEffectsActive(true), true, true))
        assertFalse("active measurement screen", GlassTouchBubble.enabled(
            glass.glassEffectsActive(glassOpticsEnabledFor(SubScreen.ACTIVE_STRENGTH)), true, true))
        assertTrue(GlassTouchBubble.enabled(glass.glassEffectsActive(glassOpticsEnabledFor(SubScreen.NONE)), true, true))
    }

    @Test fun touchBubbleGeometryIsBoundedAndSpringsAway() {
        val density = 3f
        val chip = GlassTouchBubble.radiusPx(Size(240f, 120f), density)
        val wide = GlassTouchBubble.radiusPx(Size(1000f, 144f), density)
        val tiny = GlassTouchBubble.radiusPx(Size(30f, 30f), density)
        for (r in listOf(chip, wide, tiny)) {
            assertTrue(r >= GlassTouchBubble.MIN_RADIUS_DP * density - 1e-3f)
            assertTrue(r <= GlassTouchBubble.MAX_RADIUS_DP * density + 1e-3f)
        }
        assertTrue("slight magnification only", GlassTouchBubble.MAGNIFY in .05f..0.15f)
        assertEquals(0f, GlassTouchBubble.alpha(0f), 0f)
        assertEquals(0f, GlassTouchBubble.alpha(-.3f), 0f)
        assertEquals(1f, GlassTouchBubble.alpha(1.2f), 0f)
        assertTrue(GlassTouchBubble.radiusFactor(0f) < GlassTouchBubble.radiusFactor(.5f))
        assertTrue(GlassTouchBubble.radiusFactor(.5f) < GlassTouchBubble.radiusFactor(1f))
        assertEquals(GlassTouchBubble.radiusFactor(0f), GlassTouchBubble.radiusFactor(-.4f), 0f)
        assertEquals(Offset(100f, 0f), GlassTouchBubble.clampToBounds(Offset(140f, -8f), Size(100f, 48f)))
        assertEquals(Offset(20f, 30f), GlassTouchBubble.clampToBounds(Offset(20f, 30f), Size(100f, 48f)))
    }

    @Test fun touchBubbleShaderIsOwnSourceAndPremultipliedSafe() {
        val src = GlassTouchBubble.LENS_SOURCE
        val uniforms = Regex("""uniform\s+(?:shader|float|float2)\s+(\w+);""").findAll(src).map { it.groupValues[1] }.toSet()
        assertEquals(setOf("content", "center", "radius", "strength", "magnify"), uniforms)
        assertTrue(src.contains("half4 main(float2 pos)"))
        assertTrue("lift towards alpha, never above it", src.contains("half3(c.a)"))
        assertEquals(src.count { it == '{' }, src.count { it == '}' })
        assertEquals(src.count { it == '(' }, src.count { it == ')' })
    }

    @Test fun pressPathCarriesTheBubbleForButtonsChipsAndNav() {
        val press = java.io.File("src/main/java/com/repforge/app/ui/theme/TrainPilotPressFeedback.kt").readText()
        assertTrue(press.contains("}.glassTouchBubble(source, enabled, shape)"))
        for (file in listOf("TrainPilotOutlinedButton.kt", "TrainPilotButton.kt", "TrainPilotActionButtons.kt")) {
            val text = java.io.File("src/main/java/com/repforge/app/ui/theme/$file").readText()
            assertTrue(file, text.contains(".glassTouchBubble(source, enabled, shape)"))
        }
    }

    // ---- 12 ----

    @Test fun clearEndLetsMoreBackdropThrough() {
        val clear = GlassOptics.from(100)
        // Previous V4 values at 100: coat .10, veil .90, blurDp 1, in-shader diffusion .0015, card paint .24.
        assertTrue(clear.coat < .10f && clear.coat > 0f)
        assertTrue(clear.veil < .90f)
        assertTrue(clear.blurDp < 1f && clear.blurDp > 0f)
        assertTrue(clear.blur < .0015f)
        val p = glass.withGlassClarity(100)
        assertTrue(checkNotNull(p.customCardPaint).alpha < .24f)
        // Proportionally across the range: every step lets through at least as much as V4 did.
        for (step in 0..100) {
            val t = step / 100f
            val optics = GlassOptics.from(step)
            assertTrue(optics.coat <= .62f - .52f * t + 1e-6f)
            assertTrue(optics.veil <= .94f - .04f * t + 1e-6f)
            assertTrue(checkNotNull(glass.withGlassClarity(step).customCardPaint).alpha <= .82f - .58f * t + 1e-6f)
        }
        // Frosted end unchanged.
        val frost = GlassOptics.from(0)
        assertEquals(.62f, frost.coat, 1e-6f)
        assertEquals(.94f, frost.veil, 1e-6f)
        assertEquals(GlassOptics.FROSTED_BLUR_DP, frost.blurDp, 1e-4f)
    }
}
