package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.ui.components.PersonalPlannerModalDialog
import com.example.ui.screens.StudioStatisticsContent
import com.example.ui.theme.TrainPilotTheme
import com.example.ui.viewmodel.TrainPilotUiState
import com.repforge.app.SavedExerciseBest
import com.repforge.app.ui.progress.Exercise1RmPointUiModel
import com.repforge.app.ui.progress.ProgressUiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class ProfileAndRecordLayoutRegressionTest {
    @get:Rule val compose = createComposeRule()

    private fun profile() {
        compose.setContent { TrainPilotTheme {
            PersonalPlannerModalDialog(initialEquipmentHu = "Saját testsúly",
                onSaveAndOpenPlan = { _, _, _, _, _, _, _, _, _, _ -> }, onDismiss = {})
        } }
    }

    @Test fun profileOffersIntermediateCentimetersAndKilograms() {
        profile()
        compose.onNodeWithTag("profile_height_box").performClick()
        compose.onNodeWithText("181 cm").performScrollTo().performClick()
        compose.onNodeWithText("181 cm").assertExists()
        compose.onNodeWithTag("profile_weight_box").performClick()
        compose.onNodeWithText("66 kg").performScrollTo().performClick()
        compose.onNodeWithText("66 kg").assertExists()
    }

    @Test fun equipmentHasOnlyOneMultiSelectAndKeepsSelectionsWhileOpen() {
        profile()
        compose.onNodeWithTag("profile_equipment_chip_0").assertDoesNotExist()
        compose.onNodeWithTag("profile_equipment_box").performScrollTo().performClick()
        compose.onNodeWithTag("profile_equipment_option_1").performClick()
        compose.onNodeWithTag("profile_equipment_option_0").assertExists()
        compose.onNodeWithText("2 eszköz kiválasztva • A terv ezek alapján épül fel").assertExists()
        compose.onNodeWithTag("profile_equipment_option_0").performClick()
        compose.onNodeWithText("1 eszköz kiválasztva • A terv ezek alapján épül fel").assertExists()
    }

    @Test fun expandedRecordReservesFullChartHeightInsideItsCard() {
        compose.setContent { TrainPilotTheme {
            StudioStatisticsContent(
                state = TrainPilotUiState(savedRecordsLoading = false, savedExerciseBests = listOf(
                    SavedExerciseBest("pullover", "Pullover", "Pullover", "reps", "weight", 5.5, 15, null, null))),
                native = ProgressUiState(selectedExerciseId = "pullover", strengthLoading = false,
                    exercise1RmTrend = listOf(
                        Exercise1RmPointUiModel("2026-11-01", 5.0, 10, 6.67),
                        Exercise1RmPointUiModel("2026-11-03", 5.5, 15, 8.25))),
                onSelectTab = {}, onStepsMode = {}, onExercise = {}, onActivity = {},
                onRetry = {}, onRefreshRecords = {})
        } }
        val record = compose.onNodeWithTag("statistics_record_pullover")
        val initialBounds = record.getUnclippedBoundsInRoot()
        val collapsed = initialBounds.bottom - initialBounds.top
        compose.onNodeWithText("Pullover").performClick()
        val chart = compose.onNodeWithTag("statistics_record_chart_pullover", useUnmergedTree = true)
        chart.performScrollTo().assertIsDisplayed()
        val plot = chart.getUnclippedBoundsInRoot()
        val card = record.getUnclippedBoundsInRoot()
        assertTrue(plot.bottom - plot.top >= 160.dp)
        assertTrue(card.bottom - card.top >= collapsed + 160.dp)
        assertTrue(plot.top >= card.top && plot.bottom <= card.bottom)
    }
    @Test fun specificCardioFilterHidesDailyActivityUntilAllIsSelected() {
        val selected = androidx.compose.runtime.mutableStateOf("cycling")
        compose.setContent { TrainPilotTheme {
            StudioStatisticsContent(state = TrainPilotUiState(statsSubTab = 1, cardioFilterStepsMode = false),
                native = ProgressUiState(selectedCardioId = selected.value),
                onSelectTab = {}, onStepsMode = {}, onExercise = {},
                onActivity = { selected.value = it }, onRetry = {}, onRefreshRecords = {})
        } }
        compose.onNodeWithTag("statistics_daily_activity").assertDoesNotExist()
        compose.onNodeWithTag("progress_cardio_all").performClick()
        compose.onNodeWithTag("statistics_daily_activity").assertExists()
    }

    @Test fun libraryStarsUpdateFavoritesAndKeepSearchCompact() {
        val state = androidx.compose.runtime.mutableStateOf(TrainPilotUiState(exercises = listOf(
            com.example.model.ExerciseItem("squat", "Guggolás"),
            com.example.model.ExerciseItem("plank", "Plank"))))
        compose.setContent { TrainPilotTheme {
            com.example.ui.screens.ExerciseLibraryScreen(state.value, {}, {},
                onSearchQueryChange = { state.value = state.value.copy(exerciseSearchQuery = it) },
                onSelectCategory = {}, onSelectExercise = {},
                onToggleFavoritesOnly = { state.value = state.value.copy(favoriteExercisesOnly = !state.value.favoriteExercisesOnly) },
                onToggleFavorite = { id ->
                    val old = state.value.favoriteExerciseIds
                    state.value = state.value.copy(favoriteExerciseIds = if (id in old) old - id else old + id)
                })
        } }
        val search = compose.onNodeWithTag("log_search_input").getUnclippedBoundsInRoot()
        assertTrue(search.bottom - search.top == 48.dp)
        compose.onNodeWithTag("exercise_library_favorite_squat").performScrollTo().performClick().assertIsOn()
        compose.onNodeWithTag("studio_library_favorites_only").performScrollTo().performClick()
        compose.onNodeWithTag("exercise_library_favorite_plank").assertDoesNotExist()
        compose.onNodeWithTag("exercise_library_favorite_squat").performScrollTo().performClick()
        compose.onNodeWithTag("exercise_library_favorite_squat").assertDoesNotExist()
    }

}
