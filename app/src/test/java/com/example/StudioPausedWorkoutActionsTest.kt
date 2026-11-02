package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.ui.components.StudioPausedWorkoutActions
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
class StudioPausedWorkoutActionsTest {
    @get:Rule val compose = createComposeRule()
    @Test fun discardRequiresConfirmationAndCancelPreservesTheWorkout() {
        val deleted = mutableListOf<String>()
        var resumed = 0
        compose.setContent { TrainPilotTheme { Box(Modifier.width(320.dp)) {
            StudioPausedWorkoutActions("current", true, false, "Folytatás", { resumed++ }) { deleted += it }
        } } }
        compose.onNodeWithTag("home_continue_workout_button").performClick()
        compose.runOnIdle { assertEquals(1, resumed) }
        compose.onNodeWithTag("home_discard_workout_button").performClick()
        compose.runOnIdle { assertTrue(deleted.isEmpty()) }
        compose.onNodeWithTag("home_cancel_discard").performClick()
        compose.runOnIdle { assertTrue(deleted.isEmpty()) }
        compose.onNodeWithTag("home_discard_workout_button").performClick()
        compose.onNodeWithTag("home_confirm_discard").performClick()
        compose.runOnIdle { assertEquals(listOf("current"), deleted) }
        compose.onNodeWithTag("home_confirm_discard").assertDoesNotExist()
    }
    @Test fun changingTheWorkoutCannotApplyAnOldConfirmationToTheNewOne() {
        var id by mutableStateOf("first")
        val deleted = mutableListOf<String>()
        compose.setContent { TrainPilotTheme {
            StudioPausedWorkoutActions(id, true, false, "Folytatás", {}) { deleted += it }
        } }
        compose.onNodeWithTag("home_discard_workout_button").performClick()
        compose.runOnIdle { id = "second" }
        compose.onNodeWithTag("home_confirm_discard").assertDoesNotExist()
        compose.runOnIdle { assertTrue(deleted.isEmpty()) }
        compose.onNodeWithTag("home_discard_workout_button").performClick()
        compose.onNodeWithTag("home_confirm_discard").performClick()
        compose.runOnIdle { assertEquals(listOf("second"), deleted) }
    }
    @Test fun foreignOwnershipAndPendingStorageDisableDiscard() {
        var phone by mutableStateOf(false)
        var busy by mutableStateOf(false)
        compose.setContent { TrainPilotTheme {
            StudioPausedWorkoutActions("current", phone, busy, "Folytatás", {}) { fail("Disabled discard invoked") }
        } }
        compose.onNodeWithTag("home_discard_workout_button").assertIsNotEnabled()
        compose.runOnIdle { phone = true; busy = true }
        compose.onNodeWithTag("home_discard_workout_button").assertIsNotEnabled()
        compose.onNodeWithTag("home_continue_workout_button").assertIsNotEnabled()
    }
}
