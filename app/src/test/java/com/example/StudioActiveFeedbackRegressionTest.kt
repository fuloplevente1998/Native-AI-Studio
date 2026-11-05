package com.example

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.model.ExerciseItem
import com.example.model.SubScreen
import com.example.ui.components.rememberExerciseFeedbackNext
import com.example.ui.components.rememberStudioWorkoutCoachOwner
import com.example.ui.theme.TrainPilotTheme
import com.example.ui.viewmodel.TrainPilotUiState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w480dp-h960dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StudioActiveFeedbackRegressionTest {
    @get:Rule val compose = createComposeRule()
    private fun initial() = TrainPilotUiState(isWorkoutSessionActive = true,
        activeWorkoutId = "session", activeWorkoutGeneration = 7,
        currentSubScreen = SubScreen.ACTIVE_STRENGTH,
        activeWorkoutExercises = listOf(ExerciseItem("press", "Nyomás"), ExerciseItem("row", "Evezés")))

    @Test fun coachViewModelsSurviveItemRemovalAndClearOnlyWhenExerciseScreenCloses() {
        val screen = mutableStateOf(true)
        val item = mutableStateOf(true)
        var cleared = 0
        var created = 0
        var first: androidx.lifecycle.ViewModel? = null
        var observed: androidx.lifecycle.ViewModel? = null
        compose.setContent {
            if (screen.value) {
                val owner = rememberStudioWorkoutCoachOwner()
                if (item.value) {
                    val model = remember(owner) {
                        androidx.lifecycle.ViewModelProvider(owner, object : androidx.lifecycle.ViewModelProvider.Factory {
                            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                created++
                                @Suppress("UNCHECKED_CAST")
                                return object : androidx.lifecycle.ViewModel() {
                                    override fun onCleared() { cleared++ }
                                } as T
                            }
                        })["advice", androidx.lifecycle.ViewModel::class.java]
                    }
                    SideEffect { observed = model }
                }
            }
        }
        compose.runOnIdle { first = observed; item.value = false }
        compose.runOnIdle { assertEquals(0, cleared); item.value = true }
        compose.runOnIdle { assertSame(first, observed); assertEquals(1, created); screen.value = false }
        compose.runOnIdle { assertEquals(1, cleared) }
    }

    @Test fun nextOffersOptionalFeedbackAndSkipDoesNotInventAnAnswer() {
        var next = 0
        var answers = 0
        compose.setContent { TrainPilotTheme {
            val action = rememberExerciseFeedbackNext(initial(), { next++ }, { _, _ -> answers++ }, { _, _ -> answers++ })
            Button(action, Modifier.testTag("next_trigger")) { Text("Next") }
        } }
        compose.onNodeWithTag("next_trigger").performClick()
        compose.onNodeWithTag("active_feedback_prompt").assertExists()
        compose.runOnIdle { assertEquals(0, next); assertEquals(0, answers) }
        compose.onNodeWithTag("active_feedback_skip").performClick()
        compose.runOnIdle { assertEquals(1, next); assertEquals(0, answers) }
        compose.onNodeWithTag("active_feedback_prompt").assertDoesNotExist()
    }

    @Test fun answerBelongsToTheDepartingExerciseAndRemoteSelectionDismissesOldPrompt() {
        val state = mutableStateOf(initial())
        var answer: Pair<String, Int>? = null
        var next = 0
        compose.setContent { TrainPilotTheme {
            val action = rememberExerciseFeedbackNext(state.value, { next++ }, { id, effort -> answer = id to effort }, { _, _ -> })
            Button(action, Modifier.testTag("next_trigger")) { Text("Next") }
        } }
        compose.onNodeWithTag("next_trigger").performClick()
        compose.onNodeWithTag("active_feedback_session_press_selector").performClick()
        compose.onNodeWithTag("active_feedback_session_press_hard").performClick()
        compose.runOnIdle { assertEquals("press", answer?.first); assertEquals(4, answer?.second); assertEquals(0, next) }
        compose.onNodeWithTag("active_feedback_next").performClick()
        compose.runOnIdle { assertEquals(1, next) }
        compose.onNodeWithTag("next_trigger").performClick()
        compose.runOnIdle { state.value = state.value.copy(activeExerciseIndex = 1) }
        compose.onNodeWithTag("active_feedback_prompt").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, next); assertEquals("press", answer?.first) }
    }
}
