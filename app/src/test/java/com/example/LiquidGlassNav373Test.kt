package com.example

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.localization.TrainPilotStrings
import com.example.model.AppLanguage
import com.example.model.MainTab
import com.example.model.SubScreen
import com.example.ui.components.TrainPilotGlobalTopHeaderAndNav
import com.example.ui.components.pillReflection
import com.example.ui.theme.TrainPilotPalettes
import com.example.ui.theme.LocalGlassOpticsEnabled
import com.example.ui.theme.TrainPilotTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import kotlin.math.abs

/** #373: the shared glass pill follows the selected tab; it stretches only when motion is allowed. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w480dp-h960dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class LiquidGlassNav373Test {
    @get:Rule val compose = createComposeRule()

    /** Returns the widest pill seen while travelling Home -> Health, and the settled pill/tab bounds. */
    private fun travel(animatorScale: Float, paletteId: Int = 35): Triple<Float, Float, Pair<Float, Float>> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, animatorScale)
        var selected by mutableStateOf(MainTab.HOME)
        compose.setContent { TrainPilotTheme(palette = TrainPilotPalettes.getById(paletteId)) {
            Box(Modifier.width(420.dp)) {
                TrainPilotGlobalTopHeaderAndNav(selected, SubScreen.NONE, AppLanguage.HU,
                    paletteId, TrainPilotStrings.forLanguage(AppLanguage.HU), { selected = it }, { _, _ -> }, {}, {}, {})
            }
        } }
        compose.waitForIdle()
        val home = compose.onNodeWithTag("top_nav_home").fetchSemanticsNode().boundsInRoot
        val start = compose.onNodeWithTag("glass_selection_pill").fetchSemanticsNode().boundsInRoot
        assertEquals(home.left, start.left, 2f)
        assertEquals(home.right, start.right, 2f)
        val rest = home.width
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("top_nav_health").performClick()
        var widest = 0f
        repeat(90) {
            compose.mainClock.advanceTimeByFrame()
            widest = maxOf(widest, compose.onNodeWithTag("glass_selection_pill").fetchSemanticsNode().boundsInRoot.width)
        }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.onNodeWithTag("top_nav_health").assertIsSelected()
        val health = compose.onNodeWithTag("top_nav_health").fetchSemanticsNode().boundsInRoot
        val end = compose.onNodeWithTag("glass_selection_pill").fetchSemanticsNode().boundsInRoot
        assertEquals(health.left, end.left, 2f)
        assertEquals(health.right, end.right, 2f)
        return Triple(widest, rest, end.left to health.left)
    }

    @Test fun pillReflectionFollowsTravelAndDragWithinTheExistingMotionGate() {
        assertEquals(0f, pillReflection(0f, 0f, 80f, enabled = true), 0f)
        val towardsRight = pillReflection(12f, 0f, 80f, enabled = true)
        assertTrue(towardsRight > 0f && towardsRight < 1f)
        assertEquals(-towardsRight, pillReflection(-12f, 0f, 80f, enabled = true), .001f)
        assertTrue(pillReflection(0f, -3f, 80f, enabled = true) < 0f)
        assertEquals(1f, pillReflection(1000f, 30f, 80f, enabled = true), 0f)
        assertEquals(-1f, pillReflection(-1000f, -30f, 80f, enabled = true), 0f)
        assertEquals(0f, pillReflection(40f, 3f, 80f, enabled = false), 0f)
        assertEquals(0f, pillReflection(40f, 3f, 0f, enabled = true), 0f)
    }

    @Test fun pillSlidesToTheSelectedTabWithLiquidStretch() {
        val (widest, rest, _) = travel(animatorScale = 1f)
        assertTrue("pill should stretch while travelling: widest=$widest rest=$rest", widest > rest + 4f)
    }

    @Test fun reducedMotionMovesThePillWithoutTravelOrStretch() {
        val (widest, rest, _) = travel(animatorScale = 0f)
        assertTrue("no stretch under reduced motion: widest=$widest rest=$rest", widest <= rest + 1f)
    }

    @Test fun measurementNavigationKeepsSelectionAndClickWithoutAnimatedPill() {
        var clicked: MainTab? = null
        compose.setContent { TrainPilotTheme(palette = TrainPilotPalettes.getById(35)) {
            CompositionLocalProvider(LocalGlassOpticsEnabled provides false) {
                TrainPilotGlobalTopHeaderAndNav(MainTab.WORKOUT, SubScreen.ACTIVE_STRENGTH,
                    AppLanguage.HU, 35, TrainPilotStrings.forLanguage(AppLanguage.HU),
                    { clicked = it }, { _, _ -> }, {}, {}, {})
            }
        } }
        compose.onNodeWithTag("top_nav_workout").assertIsSelected()
        compose.onAllNodesWithTag("glass_selection_pill").assertCountEquals(0)
        compose.onNodeWithTag("top_nav_health").performClick()
        compose.runOnIdle { assertEquals(MainTab.HEALTH, clicked) }
    }

    @Test fun legacyPaletteHasNoGlassPill() {
        compose.setContent { TrainPilotTheme(palette = TrainPilotPalettes.getById(1)) {
            TrainPilotGlobalTopHeaderAndNav(MainTab.HOME, SubScreen.NONE, AppLanguage.HU, 1,
                TrainPilotStrings.forLanguage(AppLanguage.HU), {}, { _, _ -> }, {}, {}, {})
        } }
        compose.onNodeWithTag("top_nav_home").assertIsSelected()
        compose.onNodeWithTag("top_shortcut_track").assertDoesNotExist()
        compose.onAllNodesWithTag("glass_selection_pill").assertCountEquals(0)
    }

    @Test fun allNineNavigationButtonsKeepSharedHeaderGeometryAndRouteActions() {
        var tab by mutableStateOf(MainTab.HOME)
        var screen by mutableStateOf(SubScreen.NONE)
        val clicks = mutableListOf<String>()
        compose.setContent {
            TrainPilotTheme(palette = TrainPilotPalettes.getById(33)) {
                Box(Modifier.width(420.dp)) {
                    TrainPilotGlobalTopHeaderAndNav(tab, screen, AppLanguage.HU, 33,
                        TrainPilotStrings.forLanguage(AppLanguage.HU),
                        { tab = it; screen = SubScreen.NONE; clicks += it.name },
                        { _, _ -> }, {}, {}, { screen = SubScreen.SETTINGS_MAIN; clicks += "SETTINGS" })
                }
            }
        }
        val background = compose.onNodeWithTag("top_header_glass_background", useUnmergedTree = true)
        val initial = background.getUnclippedBoundsInRoot()
        val routes = listOf(
            "top_nav_programs" to MainTab.PROGRAMS,
            "top_nav_workout" to MainTab.WORKOUT,
            "top_nav_progress" to MainTab.PROGRESS,
            "top_nav_health" to MainTab.HEALTH,
            "top_bar_coach_button" to MainTab.COACH,
            "top_bar_journal_button" to MainTab.LOG,
            "top_bar_calendar_button" to MainTab.CALENDAR,
            "top_nav_home" to MainTab.HOME
        )
        for ((tag, expected) in routes) {
            compose.onNodeWithTag(tag).performClick()
            compose.runOnIdle { assertEquals(expected, tab); assertEquals(SubScreen.NONE, screen) }
            assertEquals(initial, background.getUnclippedBoundsInRoot())
            compose.onNodeWithTag(tag).assertIsSelected()
            compose.onAllNodesWithTag("glass_selection_pill", useUnmergedTree = true).assertCountEquals(1)
            val bounds = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
            val drop = compose.onNodeWithTag("glass_selection_pill", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertEquals(bounds.left, drop.left, 2f)
            assertEquals(bounds.right, drop.right, 2f)
            assertEquals(bounds.top, drop.top, 2f)
        }
        compose.onNodeWithTag("top_bar_settings_button").performClick().assertIsSelected()
        compose.onNodeWithTag("top_nav_home").assertIsNotSelected()
        compose.onNodeWithTag("top_shortcut_track").assertExists()
        compose.onAllNodesWithTag("glass_selection_pill", useUnmergedTree = true).assertCountEquals(1)
        compose.runOnIdle {
            assertEquals(SubScreen.SETTINGS_MAIN, screen)
            assertEquals(routes.map { it.second.name } + "SETTINGS", clicks)
        }
        assertEquals(initial, background.getUnclippedBoundsInRoot())
        compose.onNodeWithTag("top_nav_home").performClick().assertIsSelected()
        assertEquals(initial, background.getUnclippedBoundsInRoot())
    }
}
