package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.components.LiquidSegmentedTabs
import com.example.ui.theme.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-mdpi")
class LiquidGlassSegmentedTabsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun oneDropMovesBetweenTabsAndDisabledTrackCannotChangeSelection() {
        var selected by mutableIntStateOf(0)
        var enabled by mutableStateOf(true)
        compose.setContent {
            TrainPilotTheme(TrainPilotPalettes.getById(33)) {
                Column {
                    LiquidSegmentedTabs(listOf("Exercises", "Cardio", "Steps"), selected,
                        { selected = it }, "test_track", enabled = enabled) { _, text, _ -> Text(text) }
                }
            }
        }
        compose.onNodeWithTag("test_track_0").assertIsSelected()
        compose.onAllNodesWithTag("glass_selection_pill", useUnmergedTree = true).assertCountEquals(1)
        compose.onNodeWithTag("test_track_2").performClick().assertIsSelected()
        compose.onNodeWithTag("test_track_0").assertIsNotSelected()
        compose.onAllNodesWithTag("glass_selection_pill", useUnmergedTree = true).assertCountEquals(1)
        compose.runOnIdle { enabled = false }
        compose.onNodeWithTag("test_track_1").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(2, selected) }
    }

    @Test fun scrollableTrackRetainsOneSelectionAndAllChoices() {
        var selected by mutableIntStateOf(0)
        val labels = List(8) { "Category $it" }
        compose.setContent {
            TrainPilotTheme(TrainPilotPalettes.getById(40)) {
                LiquidSegmentedTabs(labels, selected, { selected = it }, "scroll_track",
                    scrollable = true) { _, text, _ -> Text(text) }
            }
        }
        compose.onNodeWithTag("scroll_track_0").assertIsSelected()
        compose.onNodeWithTag("scroll_track_7").performScrollTo().performClick().assertIsSelected()
        compose.onAllNodesWithTag("glass_selection_pill", useUnmergedTree = true).assertCountEquals(1)
    }

    @Test fun sharedTrackRetainsSpringTravelAcrossHeaderRecomposition() {
        val track = com.example.ui.components.GlassPillTrack()
        var selected by mutableIntStateOf(0)
        var generation by mutableIntStateOf(0)
        compose.setContent {
            TrainPilotTheme(TrainPilotPalettes.getById(34)) {
                com.example.ui.components.GlassPillTrackMotion(track, selected)
                key(generation) {
                    LiquidSegmentedTabs(listOf("Exercises", "Cardio", "Steps"), selected,
                        { selected = it }, "recreated_track", sharedTrack = track) { _, text, _ -> Text(text) }
                }
            }
        }
        compose.waitForIdle()
        val before = compose.runOnIdle {
            assertTrue("The initial slot must be placed", track.placed)
            assertEquals(requireNotNull(track.bounds[0]).first, track.left.value, 1f)
            track.left.value
        }
        val destination = compose.runOnIdle { requireNotNull(track.bounds[2]).first }
        assertTrue("The destination must be to the right of the initial slot", destination > before)
        compose.mainClock.autoAdvance = false
        try {
            compose.runOnIdle { selected = 2; generation++ }
            // Flush placement/recomposition between manual frames; a single bulk advance can
            // precede the Android layout and the effect's first frame on Robolectric.
            var moving = before
            repeat(8) {
                if (moving <= before) {
                    compose.mainClock.advanceTimeByFrame()
                    moving = compose.runOnIdle { track.left.value }
                }
            }
            assertTrue("The retained pill should not snap to the new slot", moving < destination)
            assertTrue("The retained spring should advance", moving > before)
            compose.runOnIdle { generation++ }
            compose.mainClock.advanceTimeByFrame()
            val afterRecreation = compose.runOnIdle { track.left.value }
            assertTrue("Header recreation must not reset or snap the travelling pill",
                afterRecreation >= moving && afterRecreation < destination)
        } finally { compose.mainClock.autoAdvance = true }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(requireNotNull(track.bounds[2]).first, track.left.value, 1f) }
        compose.onNodeWithTag("recreated_track_2").assertIsSelected()
        compose.onAllNodesWithTag("glass_selection_pill", useUnmergedTree = true).assertCountEquals(1)
    }
}
