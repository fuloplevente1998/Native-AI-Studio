package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.components.StudioCoachRecommendations
import com.example.ui.components.StudioCoachNextTrainingCard
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

    @Test fun indicatedCoachDayIsTheDayTheStartCallbackReceives() {
        var started = ""
        compose.setContent { TrainPilotTheme {
            StudioCoachRecommendations(CoachUiState(loading = false,
                target = CoachTarget("actual-program", "b", "B nap")),
                { p, d, s -> started = "$p:$d:$s" }, {}, {}, {}, {}, {})
        } }
        compose.onNodeWithTag("studio_coach_start_target").performClick()
        assertEquals("actual-program:b:null", started)
    }

    @Test fun upcomingDayNavigatesToCalendarInsteadOfStartingItEarly() {
        var starts = 0
        var calendar = 0
        compose.setContent { TrainPilotTheme {
            StudioCoachRecommendations(CoachUiState(loading = false,
                target = CoachTarget("p", "b", "B", upcoming = true)),
                { _, _, _ -> starts++ }, { calendar++ }, {}, {}, {}, {})
        } }
        compose.onNodeWithTag("studio_coach_start_target").performClick()
        assertEquals(0, starts); assertEquals(1, calendar)
    }

    @Test fun nextDayCardHighlightsRealProgramAndCalendarGate() {
        var calendar = 0
        var started = 0
        val target = CoachTarget("p", "a", "A", upcoming = true, programTitle = "Otthoni A/B – Alap")
        compose.setContent { TrainPilotTheme {
            StudioCoachNextTrainingCard(CoachUiState(loading = false, target = target),
                { _, _, _ -> started++ }, { calendar++ })
        } }
        compose.onNodeWithTag("studio_coach_target").assertExists()
        compose.onNodeWithTag("studio_coach_program_name").assertTextEquals("Otthoni A/B – Alap")
        compose.onNodeWithTag("studio_coach_day_badge").assertExists()
        compose.onNodeWithText("A").assertExists()
        compose.onNodeWithTag("studio_coach_start_target").performClick()
        assertEquals(1, calendar)
        assertEquals(0, started)
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
}
