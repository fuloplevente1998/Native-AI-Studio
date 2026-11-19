package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.localization.TrainPilotStrings
import com.example.model.AppLanguage
import com.example.model.ExerciseItem
import com.example.ui.screens.ActiveBilateralWorkoutScreen
import com.example.ui.screens.ActiveStrengthWorkoutScreen
import com.example.ui.screens.ActiveTimedWorkoutScreen
import com.example.ui.theme.TrainPilotTheme
import com.example.ui.viewmodel.TrainPilotUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w800dp-h1000dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class AuditActiveExerciseNavigationUiTest {
    @get:Rule val compose = createComposeRule()
    private val strings = TrainPilotStrings.forLanguage(AppLanguage.HU)
    private val exercises = listOf(ExerciseItem("first", "Első gyakorlat"), ExerciseItem("second", "Második gyakorlat"))

    @Test fun strengthNavigationRespectsOwnershipPendingAndSaving() = checkNavigation("strength")
    @Test fun timedNavigationRespectsOwnershipPendingAndSaving() = checkNavigation("timed")
    @Test fun bilateralNavigationRespectsOwnershipPendingAndSaving() = checkNavigation("bilateral")

    private fun checkNavigation(mode: String) {
        var state by mutableStateOf(TrainPilotUiState(exercises = exercises, activeWorkoutExercises = exercises,
            activeWorkoutId = "workout", isWorkoutSessionActive = true, selectedExerciseId = exercises[0].id))
        var previous = 0
        var next = 0
        compose.setContent { TrainPilotTheme {
            when (mode) {
                "strength" -> ActiveStrengthWorkoutScreen(state, strings, {}, {}, {}, {}, { _, _, _, _ -> }, {},
                    {}, {}, {}, {}, onPreviousExercise = { previous++ }, onNextExercise = { next++ })
                "timed" -> ActiveTimedWorkoutScreen(state, strings, {}, {}, {},
                    onPreviousExercise = { previous++ }, onNextExercise = { next++ })
                else -> ActiveBilateralWorkoutScreen(state, strings, {}, {}, {},
                    onPreviousExercise = { previous++ }, onNextExercise = { next++ })
            }
        } }
        fun previousNode(): SemanticsNodeInteraction = when (mode) {
            "strength" -> compose.onNodeWithTag("active_prev_exercise_btn")
            "bilateral" -> compose.onNodeWithTag("bilateral_prev_exercise_btn")
            else -> compose.onNodeWithTag("timed_prev_exercise_btn")
        }
        fun nextNode(): SemanticsNodeInteraction = when (mode) {
            "strength" -> compose.onNodeWithTag("active_next_exercise_btn")
            "bilateral" -> compose.onNodeWithTag("bilateral_next_exercise_btn")
            else -> compose.onNodeWithTag("timed_next_exercise_btn")
        }
        val blocked = listOf(
            state.copy(phoneControlsWorkout = false),
            state.copy(watchControlPending = true),
            state.copy(isSavingWorkout = true)
        )
        blocked.forEach { blockedState ->
            compose.runOnIdle { state = blockedState }
            previousNode().assertIsNotEnabled().performTouchInput { click() }
            nextNode().assertIsNotEnabled().performTouchInput { click() }
            compose.runOnIdle { assertEquals(0, previous); assertEquals(0, next) }
        }
        compose.runOnIdle { state = state.copy(phoneControlsWorkout = true, watchControlPending = false, isSavingWorkout = false) }
        previousNode().assertIsEnabled().performClick()
        nextNode().assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, previous); assertEquals(1, next) }
    }
}
