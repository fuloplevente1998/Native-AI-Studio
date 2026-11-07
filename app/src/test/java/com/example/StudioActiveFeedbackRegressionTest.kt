package com.example

import android.content.Context
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.model.ExerciseItem
import com.example.model.SubScreen
import com.example.ui.components.rememberExerciseFeedbackNext
import com.example.ui.components.StudioActiveWorkoutSessionInfo
import com.example.ui.components.rememberStudioWorkoutCoachOwner
import com.example.ui.components.StudioExerciseFeedbackReview
import com.example.ui.components.StudioWorkoutOverallFeedback
import com.example.ui.components.StudioActiveWorkoutHeaderSequence
import com.example.ui.theme.TrainPilotTheme
import com.example.ui.viewmodel.TrainPilotUiState
import com.repforge.app.ActiveWorkoutStore
import org.json.JSONObject
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

    @Test fun activeDayBadgeHasNoDuplicateProgramLabel() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("trainpilot_wear_sync", Context.MODE_PRIVATE).edit().clear().commit()
        ActiveWorkoutStore.saveOwned(context, JSONObject()
            .put("workoutId", "session")
            .put("generation", 7L)
            .put("programName", "Otthoni A/B – Alap")
            .put("dayName", "A"))

        compose.setContent {
            TrainPilotTheme {
                StudioActiveWorkoutSessionInfo(initial().copy(activeWorkoutTitleHu = "Otthoni A/B – Alap • A"))
            }
        }
        compose.waitForIdle()

        compose.onNodeWithTag("active_workout_program_label").assertDoesNotExist()
        compose.onNodeWithTag("active_workout_day").assertIsDisplayed()
        compose.onNodeWithText("A nap").assertIsDisplayed()
    }

    @Test fun activeDayBadgeIsBetweenLargeHeaderAndExerciseSequence() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("trainpilot_wear_sync", Context.MODE_PRIVATE).edit().clear().commit()
        ActiveWorkoutStore.saveOwned(context, JSONObject()
            .put("workoutId", "session")
            .put("generation", 7L)
            .put("programName", "Otthoni A/B – Alap")
            .put("dayName", "A"))
        val state = initial().copy(activeWorkoutTitleHu = "Otthoni A/B – Alap")

        compose.setContent { TrainPilotTheme {
            StudioActiveWorkoutHeaderSequence(
                state = state,
                header = { Text("Otthoni A/B – Alap • 1 / 2 gyakorlat") },
                exerciseSequence = { Text("1. Nyomás    2. Evezés") }
            )
        } }
        compose.waitForIdle()

        val header = compose.onNodeWithTag("active_workout_large_header").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val day = compose.onNodeWithTag("active_workout_day_between_header_and_exercises").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val exercises = compose.onNodeWithTag("active_workout_exercise_sequence_slot").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        compose.onNodeWithText("A nap").assertIsDisplayed()
        assertTrue("Day badge must be below the large workout header", day.top >= header.bottom)
        assertTrue("Day badge must be above the horizontal exercise sequence", day.bottom <= exercises.top)
        compose.onAllNodesWithText("Otthoni A/B – Alap", substring = true).assertCountEquals(1)
    }

    @Test fun summaryReviewsExistingExerciseAnswerAndOnlyExplicitEditOpensTheChoices() {
        val effort = mutableStateOf<String?>("hard")
        var edits = 0
        compose.setContent { TrainPilotTheme {
            StudioExerciseFeedbackReview(effort.value, "Már megadott jegyzet", "hu", "Nyomás", true,
                { effort.value = it; edits++ }, {}, "review_press")
        } }
        compose.onNodeWithTag("review_press_answer").assertTextContains("Túl nehéz", substring = true)
        compose.onNodeWithTag("review_press_editor_hard").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, edits) }
        compose.onNodeWithTag("review_press_edit").performClick()
        compose.onNodeWithTag("review_press_editor_hard").assertIsSelected()
        compose.onNodeWithTag("review_press_editor_pain").performClick()
        compose.onNodeWithTag("review_press_editor_pain").assertIsSelected()
        compose.onNodeWithTag("review_press_answer").assertTextContains("Fájdalom", substring = true)
        compose.runOnIdle { assertEquals(1, edits); assertEquals("pain", effort.value) }
    }

    @Test fun overallRatingStartsCollapsedAndItsAnswerLeavesTheExerciseReviewUntouched() {
        val overall = mutableStateOf<String?>(null)
        var exerciseEdits = 0
        compose.setContent { TrainPilotTheme { androidx.compose.foundation.layout.Column {
            StudioWorkoutOverallFeedback(overall.value, "", "hu", true, { overall.value = it }, {}, "overall")
            StudioExerciseFeedbackReview("hard", "", "hu", "Nyomás", true,
                { exerciseEdits++ }, {}, "review_press")
        } } }
        compose.onNodeWithText("Edzés összesített visszajelzése").assertIsDisplayed()
        compose.onNodeWithTag("overall_choices_pain").assertDoesNotExist()
        compose.onNodeWithTag("overall_toggle").performClick()
        compose.onNodeWithTag("overall_choices_pain").performClick()
        compose.onNodeWithTag("review_press_answer").assertTextContains("Túl nehéz", substring = true)
        compose.runOnIdle { assertEquals("pain", overall.value); assertEquals(0, exerciseEdits) }
    }

    @Test fun alreadyAnsweredExerciseContinuesWithoutAskingForTheSameFeedbackAgain() {
        var next = 0
        var answers = 0
        compose.setContent { TrainPilotTheme {
            val state = initial().copy(exerciseFeedbackWorkoutId = "session", exerciseFeedbackChoices = mapOf("press" to 2))
            val action = rememberExerciseFeedbackNext(state, { next++ }, { _, _ -> answers++ }, { _, _ -> answers++ })
            Button(action, Modifier.testTag("next_trigger")) { Text("Next") }
        } }
        compose.onNodeWithTag("next_trigger").performClick()
        compose.onNodeWithTag("active_feedback_prompt").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, next); assertEquals(0, answers) }
    }

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
