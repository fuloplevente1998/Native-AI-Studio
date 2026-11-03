package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.localization.TrainPilotStrings
import com.example.model.AppLanguage
import com.example.ui.screens.OutdoorGpsActions
import com.example.ui.screens.OutdoorGpsDashboard
import com.example.ui.screens.OutdoorGpsFullscreenDialog
import com.example.ui.screens.OutdoorGpsDisplay
import com.example.ui.theme.TrainPilotTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w480dp-h960dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class OutdoorGpsLayoutRegressionTest {
    @get:Rule val compose = createComposeRule()
    private var finished = 0
    private var dialogClosed = 0
    private var paused = 0
    private var selected = ""
    private val state = OutdoorGpsDisplay("Kültéri futás", "Élő GPS-jel (2 pont • ±4 m)",
        true, true, false, "00:38", "0,00 km", "— /km", "— kcal", "♥ — bpm", 2, 1, "", "")
    private val actions get() = OutdoorGpsActions({}, {}, { selected = it }, {}, {},
        { paused++ }, {}, { finished++ }, {})

    private fun show(width: Int, height: Int, font: Float = 1f, fullscreen: Boolean = false,
                     display: OutdoorGpsDisplay = state) {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, font)) {
                TrainPilotTheme {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
                        Box(Modifier.size(width.dp, height.dp).clipToBounds().testTag("viewport")) {
                            OutdoorGpsDashboard(display, TrainPilotStrings.forLanguage(display.language), actions,
                                modifier = if (fullscreen) Modifier.padding(top = 28.dp, bottom = 32.dp) else Modifier,
                                fullscreen = fullscreen) { mapModifier ->
                                Box(mapModifier.background(Color.Black).testTag("map_fixture"))
                            }
                        }
                    }
                }
            }
        }
    }

    /** Uses an actual Android Dialog window, not an inset-simulating Box fixture. */
    private fun showInRealDialog(font: Float = 1f, display: OutdoorGpsDisplay = state) {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, font)) {
                TrainPilotTheme {
                    OutdoorGpsFullscreenDialog(
                        state = display,
                        strings = TrainPilotStrings.forLanguage(display.language),
                        actions = actions,
                        onDismissRequest = { dialogClosed++ },
                        mapContent = { mapModifier ->
                            Box(mapModifier.background(Color.Black).testTag("real_dialog_map_fixture"))
                        }
                    )
                }
            }
        }
    }

    private fun assertControlsVisible(bottomInset: Int = 0) {
        val viewport = compose.onNodeWithTag("viewport").getUnclippedBoundsInRoot()
        listOf("gps_pause_resume_btn", "gps_lap_btn", "gps_finish_workout_btn").forEach { tag ->
            val button = compose.onNodeWithTag(tag).assertIsDisplayed()
            val bounds = button.getUnclippedBoundsInRoot()
            assertTrue("$tag left edge", bounds.left >= viewport.left)
            assertTrue("$tag right edge", bounds.right <= viewport.right)
            assertTrue("$tag top edge", bounds.top >= viewport.top)
            assertTrue("$tag bottom edge", bounds.bottom <= viewport.bottom - bottomInset.dp)
            assertTrue("$tag touch height", bounds.bottom - bounds.top >= 48.dp)
        }
    }

    @Test fun compactPhoneKeepsAllActionsVisibleWithoutScrolling() {
        show(360, 480)
        assertControlsVisible()
        val body = compose.onNodeWithTag("outdoor_dashboard_body").fetchSemanticsNode()
        assertFalse(body.config.contains(SemanticsActions.ScrollBy))
        compose.onNodeWithTag("gps_pause_resume_btn").performClick()
        compose.onNodeWithTag("gps_finish_workout_btn").performClick()
        assertEquals(1, paused)
        assertEquals(1, finished)
    }

    @Test fun fullscreenKeepsFinishAboveSystemNavigationArea() {
        show(393, 700, fullscreen = true)
        assertControlsVisible(bottomInset = 32)
        compose.onNodeWithTag("gps_finish_workout_btn").performClick()
        assertEquals(1, finished)
    }

    @Test fun largeSystemFontKeepsActionsReachableInShortWindow() {
        show(320, 420, font = 2f, display = state.copy(language = AppLanguage.DE))
        assertControlsVisible()
        val body = compose.onNodeWithTag("outdoor_dashboard_body").fetchSemanticsNode()
        assertTrue(body.config.contains(SemanticsActions.ScrollBy))
        compose.onNodeWithTag("gps_finish_workout_btn").performClick()
        assertEquals(1, finished)
    }

    @Test fun compactMapLeavesRoomForDataInsteadOfGrowingWithTheScreen() {
        show(393, 780)
        val bounds = compose.onNodeWithTag("outdoor_compact_map_card").getUnclippedBoundsInRoot()
        assertTrue(bounds.bottom - bounds.top <= 180.dp)
        assertControlsVisible()
    }

    @Test fun fullscreenTimerCloseAndAllActionsAreInsideTheMap() {
        show(393, 700, fullscreen = true)
        val map = compose.onNodeWithTag("map_fixture").getUnclippedBoundsInRoot()
        for (tag in listOf("gps_close_fullscreen", "gps_elapsed_timer_text", "gps_pause_resume_btn", "gps_lap_btn", "gps_finish_workout_btn")) {
            val bounds = compose.onNodeWithTag(tag).assertIsDisplayed().getUnclippedBoundsInRoot()
            assertTrue(tag, bounds.top >= map.top && bounds.bottom <= map.bottom && bounds.left >= map.left && bounds.right <= map.right)
        }
    }

    @Test fun fullscreenAlsoKeepsActionsReachableWithLargeText() {
        show(320, 480, font = 2f, fullscreen = true)
        assertControlsVisible(bottomInset = 32)
        compose.onNodeWithTag("gps_close_fullscreen").assertIsDisplayed()
        compose.onNodeWithTag("gps_elapsed_timer_text").assertIsDisplayed()
    }

    @Test fun realDialogWindowContainsHeaderTimerMetricsAndPrimaryActions() {
        showInRealDialog()
        val map = compose.onNodeWithTag("outdoor_fullscreen_map_bounds").assertIsDisplayed()
            .getUnclippedBoundsInRoot()
        val dialog = compose.onNodeWithTag("outdoor_dialog_safe_host").assertIsDisplayed()
            .getUnclippedBoundsInRoot()
        assertTrue("Real Dialog must have a sizeable drawable viewport", map.bottom - map.top > 320.dp)
        assertTrue("Map stays inside the safe Dialog content", map.top >= dialog.top && map.bottom <= dialog.bottom)
        for (tag in listOf("gps_close_fullscreen", "gps_elapsed_timer_text",
            "gps_pause_resume_btn", "gps_lap_btn", "gps_finish_workout_btn")) {
            val bounds = compose.onNodeWithTag(tag).assertIsDisplayed().getUnclippedBoundsInRoot()
            assertTrue("$tag lies inside actual Dialog map", bounds.left >= map.left &&
                bounds.right <= map.right && bounds.top >= map.top && bounds.bottom <= map.bottom)
        }
        compose.onNodeWithTag("gps_close_fullscreen").performClick()
        assertEquals(1, dialogClosed)
        assertEquals("Closing the map must not finish the workout", 0, finished)
        assertEquals("Closing the map must not pause the workout", 0, paused)
    }

    @Test
    @Config(sdk = [34], qualifiers = "w320dp-h480dp-mdpi")
    fun realDialogKeepsFinishReachableOnSmallWindowWithLargeFont() {
        showInRealDialog(font = 1.8f)
        val bounds = compose.onNodeWithTag("outdoor_fullscreen_map_bounds").getUnclippedBoundsInRoot()
        for (tag in listOf("gps_close_fullscreen", "gps_start_workout_btn")) {
            // The started=false case is covered separately; live session keeps Finish visible.
            if (tag == "gps_start_workout_btn") continue
            val rect = compose.onNodeWithTag(tag).assertIsDisplayed().getUnclippedBoundsInRoot()
            assertTrue(rect.top >= bounds.top && rect.bottom <= bounds.bottom)
        }
        val finish = compose.onNodeWithTag("gps_finish_workout_btn").assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue(finish.bottom <= bounds.bottom)
        compose.onNodeWithTag("gps_finish_workout_btn").performClick()
        assertEquals(1, finished)
    }

    @Test fun activityChoicesRemainAvailableBeforeStarting() {
        show(360, 480, display = state.copy(started = false))
        compose.onNodeWithTag("gps_start_workout_btn").assertIsDisplayed()
        compose.onNodeWithTag("outdoor_activity_picker").performClick()
        compose.onNodeWithTag("outdoor_chip_kerékpározás").performClick()
        assertEquals("Kerékpározás", selected)
    }
}
