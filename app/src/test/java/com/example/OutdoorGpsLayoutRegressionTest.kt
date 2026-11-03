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
    private var started = 0
    private var finished = 0
    private var dialogClosed = 0
    private var paused = 0
    private var selected = ""
    private var dialogContentView: android.view.View? = null
    private val state = OutdoorGpsDisplay("Kültéri futás", "Élő GPS-jel (2 pont • ±4 m)",
        true, true, false, "00:38", "0,00 km", "— /km", "— kcal", "♥ — bpm", 2, 1, "", "")
    private val actions get() = OutdoorGpsActions({}, {}, { selected = it }, {}, { started++ },
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
                            dialogContentView = androidx.compose.ui.platform.LocalView.current
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

    @Test fun shortPhoneScrollsSecondaryContentWhileActionsStayVisible() {
        show(360, 480)
        assertControlsVisible()
        val body = compose.onNodeWithTag("outdoor_dashboard_body").fetchSemanticsNode()
        assertTrue(body.config.contains(SemanticsActions.ScrollBy))
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
        assertTrue(bounds.bottom - bounds.top <= 300.dp)
        val map = compose.onNodeWithTag("outdoor_compact_map_viewport").getUnclippedBoundsInRoot()
        assertTrue("Actual map remains useful after the GPS row", map.bottom - map.top >= 180.dp)
        assertFalse(compose.onNodeWithTag("outdoor_dashboard_body").fetchSemanticsNode()
            .config.contains(SemanticsActions.ScrollBy))
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
        for (tag in listOf("gps_close_fullscreen", "gps_elapsed_timer_text",
            "gps_pause_resume_btn", "gps_lap_btn", "gps_finish_workout_btn")) {
            val rect = compose.onNodeWithTag(tag).assertIsDisplayed().getUnclippedBoundsInRoot()
            assertTrue("$tag fits the actual small Dialog", rect.top >= bounds.top &&
                rect.bottom <= bounds.bottom && rect.left >= bounds.left && rect.right <= bounds.right)
        }
        compose.onNodeWithTag("gps_finish_workout_btn").performClick()
        assertEquals(1, finished)
    }

    @Test fun realDisplayFrameConstrictsOversizedDialogRootForBothStartedStates() {
        val viewport = com.example.ui.screens.outdoorVisibleViewportPx(
            hostHeight = 1000, viewTopOnScreen = 60, visibleTopOnScreen = 30,
            visibleBottomOnScreen = 1010, navigationBarBottom = 30, bottomReserve = 12
        )
        assertEquals(0, viewport.topInset)
        assertEquals(938, viewport.height)
        assertTrue("Fullscreen content must end above the physical display bottom",
            60 + viewport.topInset + viewport.height <= 1010 - 12)
        val edgeToEdge = com.example.ui.screens.outdoorVisibleViewportPx(
            hostHeight = 1000, viewTopOnScreen = 0, visibleTopOnScreen = 30,
            visibleBottomOnScreen = 1010, navigationBarBottom = 40, bottomReserve = 12
        )
        assertEquals(30, edgeToEdge.topInset)
        assertTrue( edgeToEdge.topInset + edgeToEdge.height <= 1000 - 40 - 12 )
    }

    @Test fun realDialogStartControlIsInsideSafeViewport() {
        showInRealDialog(display = state.copy(started = false))
        val viewport = compose.onNodeWithTag("outdoor_dialog_visible_viewport").getUnclippedBoundsInRoot()
        val button = compose.onNodeWithTag("gps_start_workout_btn").getUnclippedBoundsInRoot()
        assertTrue("Start button must fit safely above dialog bottom", button.bottom <= viewport.bottom)
        assertTrue("Start button must be below dialog top", button.top >= viewport.top)
    }

    @Test fun readyDialogStartButtonFitsAndroidVisibleDisplayNotJustComposeRoot() {
        showInRealDialog(display = state.copy(started = false))
        val start = compose.onNodeWithTag("gps_start_workout_btn").assertIsDisplayed()
        val view = requireNotNull(dialogContentView)
        val visible = android.graphics.Rect()
        val origin = IntArray(2)
        compose.runOnIdle {
            view.getWindowVisibleDisplayFrame(visible)
            view.getLocationOnScreen(origin)
        }
        val bounds = start.fetchSemanticsNode().boundsInRoot
        assertTrue("Start fits Android visible display top", origin[1] + bounds.top >= visible.top)
        assertTrue("Start fits Android visible display bottom", origin[1] + bounds.bottom <= visible.bottom)
        start.performClick()
        assertEquals(1, started)
    }

    @Test fun activityChoicesRemainAvailableBeforeStarting() {
        show(360, 480, display = state.copy(started = false))
        compose.onNodeWithTag("gps_start_workout_btn").assertIsDisplayed()
        compose.onNodeWithTag("outdoor_activity_picker").performClick()
        compose.onNodeWithTag("outdoor_chip_kerékpározás").performClick()
        assertEquals("Kerékpározás", selected)
    }
}
