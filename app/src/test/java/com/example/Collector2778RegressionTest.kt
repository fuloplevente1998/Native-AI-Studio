package com.example

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.components.StudioRestTimerDock
import com.example.ui.theme.TrainPilotTheme
import com.example.ui.viewmodel.TrainPilotUiState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class Collector2778RegressionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun sharedRestDockExpandsToTheLargeCardAndKeepsItsActions() {
        // 2778: the bilateral/timed screens use the same dock as strength (not a hint line).
        val calls = mutableListOf<String>()
        var large by mutableStateOf(false)
        compose.setContent { TrainPilotTheme {
            StudioRestTimerDock(TrainPilotUiState(restTimerRemainingSec = 90, restTimerRunning = true,
                phoneControlsWorkout = true), large, onLarge = { large = it },
                onAdjust = { calls += "adjust$it" }, onToggle = { calls += "toggle" }, onSkip = { calls += "skip" },
                skipLabel = "Kihagyás")
        } }
        compose.onNodeWithTag("sticky_rest_timer_compact").assertExists()
        compose.onNodeWithTag("compact_rest_expand", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("sticky_rest_timer_large").assertExists()
        compose.onNodeWithText("01:30").assertExists()
        compose.onNodeWithText("+15 mp").performClick()
        compose.onNodeWithText("Kihagyás").performClick()
        compose.runOnIdle { assertEquals(listOf("adjust15", "skip"), calls) }
        compose.onNodeWithText("Lapos mód").performClick()
        compose.onNodeWithTag("sticky_rest_timer_compact").assertExists()
    }

    @Test fun noRestNoDock() {
        compose.setContent { TrainPilotTheme {
            StudioRestTimerDock(TrainPilotUiState(restTimerRemainingSec = 0), false, {}, {}, {}, {}, "Kihagyás")
        } }
        compose.onNodeWithTag("sticky_rest_timer_compact").assertDoesNotExist()
        compose.onNodeWithTag("sticky_rest_timer_large").assertDoesNotExist()
    }
}
