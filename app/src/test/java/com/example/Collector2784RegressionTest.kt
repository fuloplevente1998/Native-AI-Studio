package com.example

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import com.example.model.AppLanguage
import com.example.ui.screens.StudioStatisticsContent
import com.example.ui.theme.TrainPilotTheme
import com.example.ui.viewmodel.TrainPilotUiState
import com.example.ui.viewmodel.WeeklyHealthDaySummary
import com.repforge.app.ui.progress.ProgressUiState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h780dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Collector2784RegressionTest {
    @get:Rule val compose = createComposeRule()

    private fun assertWhole(tag: String, expected: String) {
        val node = compose.onNodeWithTag(tag, useUnmergedTree = true)
        node.assertTextEquals(expected)
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val layout = layouts.single()
        assertEquals("$tag is cut", expected.length, layout.getLineEnd(layout.lineCount - 1, visibleEnd = true))
        assertFalse("$tag overflows", layout.didOverflowWidth || layout.didOverflowHeight)
    }

    @Test fun sideBySideLabelsShowTheWholeWordInEveryLanguage() {
        var lang by mutableStateOf(AppLanguage.HU)
        compose.setContent { TrainPilotTheme {
            StudioStatisticsContent(TrainPilotUiState(language = lang), ProgressUiState(), {}, {}, {}, {}, {}, {})
        } }
        val names = mapOf(AppLanguage.HU to "Gyakorlatok", AppLanguage.EN to "Exercises", AppLanguage.DE to "Übungen", AppLanguage.RO to "Exerciții")
        names.forEach { (l, word) ->
            compose.runOnIdle { lang = l }
            assertWhole("statistics_subtab_label_0", word)
        }
    }

    @Test fun workoutTileTitlesAreNotCutEvenInGerman() {
        var lang by mutableStateOf(AppLanguage.DE)
        compose.setContent { TrainPilotTheme {
            com.example.ui.screens.WorkoutTabScreen(state = TrainPilotUiState(language = lang), onOpenSettings = {}, onOpenQuickWorkout = {},
                onOpenExerciseLibrary = {}, onOpenOutdoorWorkout = {}, onOpenProgramsList = {}, onOpenProgramEditor = { _, _ -> },
                onStartWorkoutDay = {}, onOpenCalendar = {}, onOpenCoach = {}, onOpenProgressionModal = {})
        } }
        assertWhole("workout_tab_exercise_picker_title", "Wiederholungen")
        compose.runOnIdle { lang = AppLanguage.HU }
        assertWhole("workout_tab_exercise_picker_title", "Ismétléses")
    }

    @Test fun aSelectedStepDayFiltersTheListBelow() {
        val today = LocalDate.now()
        fun day(back: Long, steps: Int) = WeeklyHealthDaySummary(today.minusDays(back).toString(), "", steps, 0, "", 0, 0, "HC",
            stepsAvailable = true, stepsSourceLabelHu = "Samsung Health")
        compose.setContent { TrainPilotTheme {
            StudioStatisticsContent(TrainPilotUiState(language = AppLanguage.HU, statsSubTab = 1, cardioFilterStepsMode = true,
                dailyActivityDays = listOf(day(0, 3352), day(1, 11620), day(4, 13944))), ProgressUiState(), {}, {}, {}, {}, {}, {})
        } }
        compose.onNodeWithTag("statistics_steps_row_$today").assertExists()
        compose.onNodeWithTag("statistics_steps_column_9").performClick() // 4 days ago
        compose.onNodeWithTag("statistics_steps_row_${today.minusDays(4)}").assertExists()
        compose.onNodeWithTag("statistics_steps_row_$today").assertDoesNotExist()
        compose.onNodeWithTag("statistics_steps_row_${today.minusDays(1)}").assertDoesNotExist()
        compose.onNodeWithTag("statistics_steps_list_toggle").performClick()
        compose.onNodeWithTag("statistics_steps_row_${today.minusDays(1)}").assertExists()
    }
}
