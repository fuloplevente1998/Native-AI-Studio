package com.example

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.example.localization.TrainPilotStrings
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

    @Test fun progressOverviewStatisticsSwitchKeepsIconsAndCompactTouchTargets() {
        var active by mutableStateOf(0)
        compose.setContent { TrainPilotTheme {
            com.example.ui.screens.ProgressTabScreen(
                uiState = TrainPilotUiState(progressMainSection = active),
                strings = TrainPilotStrings.forLanguage(AppLanguage.HU),
                onSelectMainSection = { active = it },
                onSelectExercise = {}, onSelectRange = {}, onSelectVolumeBar = {},
                onSelectExpandedStatsCard = {}, onSelectStatsSubTab = {},
                onToggleCardioFilterStepsMode = {}, onOpenWorkoutDetail = {},
                onOpenSettings = {}, onOpenPalettePicker = {}
            )
        } }
        compose.onNodeWithTag("progress_section_tab_0").assertHeightIsEqualTo(48.dp)
        compose.onNodeWithTag("progress_section_tab_2").assertHeightIsEqualTo(48.dp)
        compose.onNodeWithTag("progress_section_visual_0").assertHeightIsEqualTo(42.dp)
        compose.onNodeWithTag("progress_section_visual_2").assertHeightIsEqualTo(42.dp)
        compose.onNodeWithTag("progress_section_icon_0", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("progress_section_icon_2", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("progress_section_label_0", useUnmergedTree = true).assertTextEquals("Fejlődés")
        compose.onNodeWithTag("progress_section_tab_2").performClick()
        compose.runOnIdle { assertEquals(2, active) }
        compose.onNodeWithTag("progress_section_icon_0", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("progress_section_icon_2", useUnmergedTree = true).assertExists()
    }

    @Test fun selectedStepDayIsShownOnceAndAllHistorySlidesUpFromFixedButton() {
        val today = LocalDate.now()
        fun day(back: Long, steps: Int) = WeeklyHealthDaySummary(today.minusDays(back).toString(), "", steps, 0, "", 0, 0, "HC",
            stepsAvailable = true, stepsSourceLabelHu = "Samsung Health")
        compose.setContent { TrainPilotTheme {
            StudioStatisticsContent(TrainPilotUiState(language = AppLanguage.HU, statsSubTab = 1, cardioFilterStepsMode = true,
                dailyActivityDays = listOf(day(0, 3352), day(1, 11620), day(4, 13944))), ProgressUiState(), {}, {}, {}, {}, {}, {})
        } }
        // The inline selected-day row is the only numeric detail, below the chart.
        compose.onNodeWithTag("statistics_steps_selected_value", useUnmergedTree = true)
            .assertTextContains("352", substring = true)
        compose.onNodeWithTag("statistics_steps_row_$today").assertDoesNotExist()
        compose.onNodeWithTag("statistics_steps_column_9").performClick() // four days ago
        compose.onNodeWithTag("statistics_steps_selected_value", useUnmergedTree = true)
            .assertTextContains("944", substring = true)
        val toggle = compose.onNodeWithTag("statistics_steps_list_toggle")
        toggle.assertExists().performClick()
        compose.onNodeWithTag("statistics_steps_daily_sheet").assertExists()
        val sheetHeader = compose.onNodeWithTag("statistics_steps_sheet_header").assertExists()
        val sheetList = compose.onNodeWithTag("statistics_steps_sheet_list").assertExists()
        val headerBounds = sheetHeader.getUnclippedBoundsInRoot()
        val listBounds = sheetList.getUnclippedBoundsInRoot()
        assertTrue("The close control must stay above the scrolling history", headerBounds.bottom <= listBounds.top)
        compose.onNodeWithTag("statistics_steps_row_${today.minusDays(4)}").assertExists()
        compose.onNodeWithTag("statistics_steps_row_$today").assertExists()
        compose.onNodeWithTag("statistics_steps_row_${today.minusDays(1)}").assertExists()
        compose.onNodeWithTag("statistics_steps_sheet_close").performClick()
        compose.onNodeWithTag("statistics_steps_daily_sheet").assertDoesNotExist()
        toggle.assertExists()
        compose.onNodeWithTag("statistics_steps_row_$today").assertDoesNotExist()
        compose.onNodeWithTag("statistics_steps_selected_value", useUnmergedTree = true)
            .assertTextContains("944", substring = true)
    }
}
