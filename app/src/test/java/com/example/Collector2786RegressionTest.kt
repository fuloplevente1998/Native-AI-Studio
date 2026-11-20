package com.example

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.localization.TrainPilotStrings
import com.example.model.AppLanguage
import com.example.model.WorkoutGoalPeriod
import com.example.ui.components.WeeklyGoalsCompact
import com.example.ui.components.weeklyGoalSummary
import com.example.ui.screens.HealthTabScreen
import com.example.ui.theme.TrainPilotTheme
import com.example.ui.viewmodel.TrainPilotUiState
import com.example.ui.viewmodel.WeeklyHealthDaySummary
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

/** 2786: one shared weekly goals block (Home compact, Health big), weight card, one sources/watch/sync card. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h900dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class Collector2786RegressionTest {
    @get:Rule val compose = createComposeRule()
    private val today = LocalDate.now()

    private fun day(back: Long, steps: Int, available: Boolean = true) = today.minusDays(back).toString().let {
        WeeklyHealthDaySummary(dateIso = it, dayLabelHu = it, steps = steps, activeCaloriesKcal = 0, sleepLabel = "—",
            workoutMinutes = 0, workoutCount = 0, sourceLabelHu = "", stepsAvailable = available)
    }

    @Test fun summaryUsesOnlyMeasuredDaysOfTheLastWeekAndNeverInventsSteps() {
        val none = weeklyGoalSummary(TrainPilotUiState(stepsGoal = 10000, workoutGoalTarget = 3, workoutGoalCompleted = 2), today)
        assertNull(none.steps)
        assertEquals(0f, none.stepFraction)
        assertEquals(2, none.workoutsDone); assertEquals(3, none.workoutsTarget)
        val s = weeklyGoalSummary(TrainPilotUiState(stepsGoal = 10000, workoutGoalPeriod = WorkoutGoalPeriod.MONTHLY,
            weeklyHealthDays = listOf(day(0, 7000), day(3, 5000), day(5, 9999, available = false), day(9, 20000))), today)
        assertEquals(12000, s.steps)
        assertEquals(2, s.stepDays)
        assertEquals(70000, s.stepsTarget)
        assertTrue(s.monthly)
    }

    @Test fun weeklyStepPercentCanExceedHundredWhileBarRemainsCapped() {
        val summary = weeklyGoalSummary(TrainPilotUiState(stepsGoal = 10000,
            weeklyHealthDays = listOf(day(0, 83273))), today)
        assertEquals(83273, summary.steps)
        assertEquals(119, summary.stepPercent)
        assertEquals(1f, summary.stepFraction)

        compose.setContent { TrainPilotTheme {
            WeeklyGoalsCompact(TrainPilotUiState(stepsGoal = 10000,
                weeklyHealthDays = listOf(day(0, 83273))), framed = true,
                onOpen = {}, onGoals = null)
        } }
        compose.onNodeWithText("119%").assertExists()
    }

    @Test fun compactBlockOpensTheBigViewAndItsGoalsButtonOpensTheGoalsDialog() {
        var opened = 0; var goals = 0
        compose.setContent { TrainPilotTheme {
            WeeklyGoalsCompact(TrainPilotUiState(workoutGoalTarget = 3, workoutGoalCompleted = 1), framed = true,
                onOpen = { opened++ }, onGoals = { goals++ })
        } }
        compose.onNodeWithTag("weekly_goals_workouts", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("1 / 3", substring = true).performClick()
        compose.onNodeWithTag("weekly_goals_settings").performClick()
        compose.runOnIdle { assertEquals(1, opened); assertEquals(1, goals) }
    }

    @Test fun healthWeeklyGoalsLiveInTodayAndOpenBigWithTappableDays() {
        var state by mutableStateOf(TrainPilotUiState(stepsGoal = 8000, weeklyHealthDays = listOf(day(0, 7699), day(2, 4321))))
        compose.setContent { TrainPilotTheme {
            HealthTabScreen(state, TrainPilotStrings.forLanguage(AppLanguage.HU),
                { if (it == "weekly") state = state.copy(expandedHealthWeeklyGoals = !state.expandedHealthWeeklyGoals)
                  if (it == "weekly_steps") state = state.copy(expandedWeeklySteps = !state.expandedWeeklySteps) },
                {}, {}, { _, _ -> }, {}, {}, {}, {})
        } }
        compose.onNodeWithTag("health_weekly_status_card").assertDoesNotExist()
        compose.onNodeWithTag("health_weekly_steps_card").assertDoesNotExist()
        compose.onNodeWithTag("health_weekly_goals_compact").performClick()
        compose.runOnIdle { assertTrue(state.expandedHealthWeeklyGoals) }
        val list = compose.onNodeWithTag("health_tab_screen")
        list.performScrollToNode(hasTestTag("health_weekly_steps_card"))
        compose.onNodeWithTag("health_weekly_steps_card").performClick()
        list.performScrollToNode(hasTestTag("health_weekly_steps_bars_info"))
        val info = compose.onNodeWithTag("health_weekly_steps_bars_info", useUnmergedTree = true)
        info.assertTextContains("lépés", substring = true)
        compose.onNodeWithTag("health_weekly_steps_bars_4").performClick()
        info.assertTextContains("4", substring = true).assertTextContains("321 lépés", substring = true)
        compose.onNodeWithTag("health_weekly_steps_bars_0").performClick()
        info.assertTextContains("nincs adat", substring = true)
        list.performScrollToNode(hasTestTag("health_weight_card"))
        compose.onNodeWithTag("health_weight_card").assertExists()
    }

    @Test fun sourcesWatchAndSyncAreOneCard() {
        var state by mutableStateOf(TrainPilotUiState(expandedHealthConnect = false))
        compose.setContent { TrainPilotTheme {
            HealthTabScreen(state, TrainPilotStrings.forLanguage(AppLanguage.HU),
                { if (it == "connect") state = state.copy(expandedHealthConnect = !state.expandedHealthConnect) },
                {}, {}, { _, _ -> }, {}, {}, {}, {})
        } }
        val list = compose.onNodeWithTag("health_tab_screen")
        list.performScrollToNode(hasText("Források, óra és szinkron"))
        compose.onNodeWithTag("health_sync_button").assertDoesNotExist()
        compose.onNodeWithTag("health_open_watch_sync_card").assertDoesNotExist()
        compose.onNodeWithText("Források, óra és szinkron").performClick()
        list.performScrollToNode(hasTestTag("health_open_watch_sync_card"))
        list.performScrollToNode(hasTestTag("health_sync_button"))
        compose.onNodeWithTag("health_sync_button").assertExists()
    }
}
