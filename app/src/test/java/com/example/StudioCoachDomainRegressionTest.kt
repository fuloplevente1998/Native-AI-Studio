package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.components.StudioCoachRecommendations
import com.example.ui.components.StudioCoachNextTrainingCard
import com.example.ui.components.StudioWorkoutCoachAdvice
import androidx.compose.runtime.mutableStateOf
import com.example.ui.theme.TrainPilotTheme
import com.repforge.app.StudioWorkoutFeedback
import com.repforge.app.ui.coach.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import java.time.Instant
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w480dp-h960dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class StudioCoachDomainRegressionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun backgroundCoachReloadKeepsVisibleTargetPositionWithoutInitialLoadingPlaceholder() {
        val state = mutableStateOf(CoachUiState(loading = false, provider = "health_connect",
            target = CoachTarget("p", "a", "Alap A", programTitle = "Otthoni A/B")))
        compose.setContent { TrainPilotTheme {
            StudioCoachRecommendations(state.value, { _, _, _ -> }, {}, {}, {}, {}, {})
        } }
        val before = compose.onNodeWithTag("studio_coach_target").fetchSemanticsNode().boundsInRoot
        compose.runOnIdle { state.value = state.value.copy(loading = true) }
        compose.onNodeWithTag("studio_coach_initial_loading").assertDoesNotExist()
        compose.onNodeWithTag("studio_coach_program_name").assertTextEquals("Otthoni A/B")
        assertEquals(before, compose.onNodeWithTag("studio_coach_target").fetchSemanticsNode().boundsInRoot)
    }

    @Test fun currentCoachDayStartsItsActualProgramAndDay() {
        var started = ""
        var workoutPage = 0
        compose.setContent { TrainPilotTheme {
            StudioCoachRecommendations(CoachUiState(loading = false,
                target = CoachTarget("actual-program", "b", "B nap")),
                { p, d, s -> started = "$p:$d:$s" }, { workoutPage++ }, {}, {}, {}, {})
        } }
        compose.onNodeWithTag("studio_coach_start_target").performClick()
        assertEquals("actual-program:b:null", started)
        assertEquals(0, workoutPage)
    }

    @Test fun explicitUpcomingDayStartUsesCanonicalStartCallback() {
        var starts = 0
        var workoutPage = 0
        compose.setContent { TrainPilotTheme {
            StudioCoachRecommendations(CoachUiState(loading = false,
                target = CoachTarget("p", "b", "B", upcoming = true)),
                { _, _, _ -> starts++ }, { workoutPage++ }, {}, {}, {}, {})
        } }
        compose.onNodeWithTag("studio_coach_start_target").performClick()
        assertEquals(1, starts); assertEquals(0, workoutPage)
    }

    @Test fun nextDayCardHighlightsRealProgramAndStartsIt() {
        var workoutPage = 0
        var started = 0
        val target = CoachTarget("p", "a", "A", upcoming = true, programTitle = "Otthoni A/B – Alap")
        compose.setContent { TrainPilotTheme {
            StudioCoachNextTrainingCard(CoachUiState(loading = false, target = target),
                { _, _, _ -> started++ }, { workoutPage++ })
        } }
        compose.onNodeWithTag("studio_coach_target").assertExists()
        compose.onNodeWithTag("studio_coach_program_name").assertTextEquals("Otthoni A/B – Alap")
        compose.onNodeWithTag("studio_coach_day_badge").assertExists()
        compose.onNodeWithText("A").assertExists()
        compose.onNodeWithTag("studio_coach_start_target").performClick()
        assertEquals(0, workoutPage)
        assertEquals(1, started)
    }

    @Test fun laterExerciseAdviceDoesNotDuplicateHighlightedNextDay() {
        compose.setContent { TrainPilotTheme {
            StudioCoachRecommendations(CoachUiState(loading = false,
                target = CoachTarget("p", "a", "A", programTitle = "Alap")),
                { _, _, _ -> }, {}, {}, {}, {}, {}, showTarget = false)
        } }
        compose.onNodeWithTag("studio_coach_target").assertDoesNotExist()
    }

    @Test fun absentFeedbackIsAbsentAndPainUsesCanonicalMachineFields() {
        assertNull(StudioWorkoutFeedback.chosen(-1, "", 1L))
        val pain = requireNotNull(StudioWorkoutFeedback.chosen(5, "fáj", 1L))
        assertEquals("pain", pain.getString("rating")); assertEquals("pain", pain.getString("effort"))
        assertFalse(requireNotNull(StudioWorkoutFeedback.chosen(-1, "megjegyzés", 1L)).has("rating"))
        val now = Instant.parse("2026-11-03T10:00:00Z")
        val energy = JSONObject().put("energyScore", 90).put("energyScoreSource", "samsung_health")
            .put("energyScoreTime", "2026-11-03T09:00:00Z").put("day", "2026-11-03")
        val workout = JSONObject().put("started", "2026-11-03T07:00:00Z")
            .put("finished", "2026-11-03T08:00:00Z").put("feedback", pain)
            .put("exercises", JSONArray().put(JSONObject()))
        val result = NativeCoachReadiness.calculate(JSONObject(), energy, listOf(workout), now, ZoneOffset.UTC)
        assertTrue(result.pain); assertTrue(requireNotNull(result.score) <= 54.0)
        assertTrue(com.example.ui.components.studioCoachReadinessText(result).contains("Fájdalomjelzés"))
        assertFalse(com.example.ui.components.studioCoachReadinessText(result).contains("teljes értékű"))
    }

    @Test fun activeCoachDisplaysRealAdviceAndSeparatesLoadingNoHistoryAndReadFailure() {
        var refreshes = 0
        val advice = NativeCoachExerciseAdvice.Advice("increase", "stable", 22.5, 20.0,
            false, 3, NativeCoachExerciseAdvice.Target("reps", text = "8-12"))
        val state = mutableStateOf(WorkoutCoachAdviceState("press"))
        compose.setContent { TrainPilotTheme { androidx.compose.foundation.layout.Column {
            StudioWorkoutCoachAdvice(state.value) { refreshes++ }
        } } }
        compose.onNodeWithTag("studio_active_coach_loading").assertExists()
        compose.onNodeWithTag("studio_active_coach_refresh").assertIsNotEnabled()
        compose.runOnIdle { state.value = WorkoutCoachAdviceState("press", loading = false,
            row = CoachExerciseAdvice("press", "Press", advice, "total")) }
        compose.onNodeWithTag("studio_active_coach_weight").assertTextContains("22.5 kg", substring = true)
        compose.onNodeWithTag("studio_active_coach_no_history").assertDoesNotExist()
        compose.onNodeWithTag("studio_active_coach_refresh").performClick()
        assertEquals(1, refreshes)
        // Background invalidation keeps valid advice visible while it is recomputed.
        compose.runOnIdle { state.value = state.value.copy(loading = true) }
        compose.onNodeWithTag("studio_active_coach_loading").assertDoesNotExist()
        compose.onNodeWithTag("studio_active_coach_weight").assertTextContains("22.5 kg", substring = true)
        compose.onNodeWithTag("studio_active_coach_refresh").assertIsNotEnabled()
        compose.runOnIdle { state.value = WorkoutCoachAdviceState("press", loading = false,
            row = CoachExerciseAdvice("press", "Press", advice.copy(action = "start", reason = "start", historyCount = 0))) }
        compose.onNodeWithTag("studio_active_coach_no_history").assertExists()
        compose.onNodeWithTag("studio_active_coach_error").assertDoesNotExist()
        compose.runOnIdle { state.value = WorkoutCoachAdviceState("press", loading = false, error = true) }
        compose.onNodeWithTag("studio_active_coach_error").assertExists()
        compose.onNodeWithTag("studio_active_coach_no_history").assertDoesNotExist()
        compose.onNodeWithTag("studio_active_coach_weight").assertDoesNotExist()
    }
}
