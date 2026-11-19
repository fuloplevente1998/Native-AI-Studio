package com.example

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.ui.components.ActiveWorkoutLiveBar
import com.example.ui.theme.TrainPilotTheme
import com.example.ui.viewmodel.TrainPilotUiState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 2774: the bar over every page shows the time and wires Continue / Finish like the workout screen. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-mdpi")
class ActiveWorkoutLiveBarRegressionTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun barShowsElapsedTimeAndWiresContinueFinishAndConfirmedDiscard() {
        var cont = 0; var finish = 0; var discard = 0
        val state = TrainPilotUiState(isWorkoutSessionActive = true,
            workoutStartedEpochMs = System.currentTimeMillis() - 65_000L)
        compose.setContent { TrainPilotTheme {
            ActiveWorkoutLiveBar(state, onContinue = { cont++ }, onFinish = { finish++ }, onDiscard = { discard++ })
        } }
        compose.onNodeWithTag("live_bar_elapsed", useUnmergedTree = true).assertTextContains("01:0", substring = true)
        compose.onNodeWithTag("live_bar_continue").performClick()
        compose.runOnIdle { assertEquals(1, cont) }
        compose.onNodeWithTag("live_bar_finish").performClick()
        compose.onNodeWithTag("live_bar_finish_sheet").assertExists()
        compose.onNodeWithTag("live_bar_finish_discard").performClick()
        compose.runOnIdle { assertEquals(0, discard) }
        compose.onNodeWithTag("live_bar_discard_confirm").performClick()
        compose.runOnIdle { assertEquals(1, discard) }
        compose.onNodeWithTag("live_bar_finish").performClick()
        compose.onNodeWithTag("live_bar_finish_save").performClick()
        compose.runOnIdle { assertEquals(1, finish) }
    }
}
