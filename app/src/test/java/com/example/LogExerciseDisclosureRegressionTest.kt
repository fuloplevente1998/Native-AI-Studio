package com.example

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.unit.dp
import com.example.localization.TrainPilotStrings
import com.example.model.AppLanguage
import com.example.model.ExerciseItem
import com.example.model.ExerciseSetItem
import com.example.model.LoggedExerciseDetail
import com.example.model.LoggedWorkoutEntry
import com.example.model.SubScreen
import com.example.ui.components.LocalOpenExerciseVideo
import com.example.ui.screens.LogWorkoutDetailScreen
import com.example.ui.theme.TrainPilotTheme
import com.example.ui.viewmodel.TrainPilotViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w393dp-h850dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class LogExerciseDisclosureRegressionTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun workoutDetailsStartCollapsedAndKeepVideoIndependentFromExerciseDisclosure() {
        val model = TrainPilotViewModel()
        val squat = ExerciseItem("squat", "Guggolás")
        val firstSet = ExerciseSetItem(setNumber = 1, weightKg = 60f, reps = 8, rir = 2, completed = true)
        val firstExercise = LoggedExerciseDetail(squat.nameHu, "1/1 sorozat", listOf(firstSet))
        val secondExercise = LoggedExerciseDetail(
            "Fekvőtámasz", "1/2 sorozat", listOf(firstSet, firstSet.copy(setNumber = 2, completed = false))
        )
        val singleExercise = LoggedWorkoutEntry("single", titleHu = "Egy gyakorlat", exerciseDetails = listOf(firstExercise))
        val twoExercises = LoggedWorkoutEntry("two", titleHu = "Két gyakorlat", exerciseDetails = listOf(firstExercise, secondExercise))
        var video: Pair<String, String?>? = null

        assertEquals(-1, model.uiState.value.expandedLogExerciseIndex)
        model.openLogWorkoutDetail(twoExercises.id)
        compose.setContent {
            val state by model.uiState.collectAsState()
            TrainPilotTheme {
                CompositionLocalProvider(LocalOpenExerciseVideo provides { name, id -> video = name to id }) {
                    if (state.currentSubScreen == SubScreen.LOG_WORKOUT_DETAIL) {
                        LogWorkoutDetailScreen(
                            uiState = state.copy(
                                language = AppLanguage.HU,
                                loggedWorkouts = listOf(singleExercise, twoExercises),
                                exercises = listOf(squat)
                            ),
                            strings = TrainPilotStrings.forLanguage(AppLanguage.HU),
                            onBack = { model.navigateBack() },
                            onOpenSettings = {},
                            onSelectDetailTab = model::selectLogDetailTab,
                            onToggleExpandedExercise = model::toggleExpandedLogExercise,
                            onOpenPhotos = {},
                            onOpenRouteMap = {},
                            onOpenWatchSync = {},
                            onOpenHealthTab = {}
                        )
                    }
                }
            }
        }

        val collapsed = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Összecsukva")
        val expanded = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Kinyitva")
        val firstCard = compose.onNodeWithTag("log_exercise_card_0")
        val secondCard = compose.onNodeWithTag("log_exercise_card_1")
        firstCard.assert(collapsed).assertHeightIsAtLeast(48.dp)
        secondCard.assert(collapsed)
        compose.onNodeWithText("1/1 sorozat", useUnmergedTree = true).assertIsDisplayed()
        val firstBounds = firstCard.getUnclippedBoundsInRoot()
        val secondBounds = secondCard.getUnclippedBoundsInRoot()
        assertTrue(firstBounds.bottom - firstBounds.top <= 60.dp)
        assertEquals(4.dp, secondBounds.top - firstBounds.bottom)
        compose.onNodeWithText("1. sorozat", useUnmergedTree = true).assertDoesNotExist()

        compose.onNodeWithTag("exercise_video_btn_${squat.nameHu.hashCode()}", useUnmergedTree = true)
            .assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
            .performTouchInput { click(center) }
        compose.runOnIdle { assertEquals(squat.nameHu to squat.id, video) }
        firstCard.assert(collapsed)

        firstCard.performClick().assert(expanded)
        compose.onNodeWithContentDescription("Teljesített mérés", useUnmergedTree = true).assertIsDisplayed()
        firstCard.performClick().assert(collapsed)
        firstCard.performClick().assert(expanded)
        secondCard.performClick().assert(expanded)
        firstCard.assert(collapsed)
        compose.onNodeWithText("Nem teljesített", useUnmergedTree = true).assertIsDisplayed()

        compose.runOnIdle { model.openLogWorkoutDetail(singleExercise.id) }
        firstCard.assert(collapsed)
        secondCard.assertDoesNotExist()
        firstCard.performClick().assert(expanded)
        compose.onNodeWithTag("subscreen_back_button").performClick()
        secondCard.assert(expanded)

        compose.onNodeWithTag("subscreen_back_button").performClick()
        compose.runOnIdle { model.openLogWorkoutDetail(twoExercises.id) }
        firstCard.assert(collapsed)
        secondCard.assert(collapsed)
        compose.onNodeWithText("1. sorozat", useUnmergedTree = true).assertDoesNotExist()
    }
}
