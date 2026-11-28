package com.example

import androidx.compose.ui.test.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.components.StudioCoachRecommendations
import com.example.ui.components.StudioCoachNextTrainingCard
import com.example.ui.components.StudioWorkoutCoachAdvice
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import com.example.ui.theme.TrainPilotTheme
import com.repforge.app.StudioWorkoutFeedback
import com.repforge.app.NativeProgramEditor
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

    @Test fun coachShowsOnlyFinalReadinessWhenSamsungSupplementIsOff() {
        val ui = mutableStateOf(com.example.ui.viewmodel.TrainPilotUiState())
        val result = NativeCoachReadiness.Result(74.0, 74, null, null, null, 420.0, null,
            null, null, 1, false)
        val readiness = mutableStateOf(result)
        compose.setContent { TrainPilotTheme {
            com.example.ui.screens.StudioCoachReadinessCard(ui.value, readiness.value)
        } }
        compose.onNodeWithTag("studio_coach_final_readiness").assertExists()
        compose.onNodeWithText("74").assertExists()
        compose.onNodeWithTag("studio_coach_score_breakdown").assertDoesNotExist()
        compose.onNodeWithText("TrainPilot", substring = false).assertDoesNotExist()
        compose.onNodeWithText("Samsung Energy").assertDoesNotExist()
        compose.runOnIdle {
            ui.value = ui.value.copy(permissionAccess = ui.value.permissionAccess.copy(samsungHealthEnabled = true))
            readiness.value = result.copy(score = 76.0, samsungScore = 84.0)
        }
        compose.onNodeWithTag("studio_coach_final_readiness").assertExists()
        compose.onNodeWithTag("studio_coach_score_breakdown").assertExists()
        compose.onNodeWithText("Samsung Energy").assertExists()
        compose.onNodeWithText("TrainPilot", substring = false).assertExists()
        compose.runOnIdle {
            ui.value = ui.value.copy(permissionAccess = ui.value.permissionAccess.copy(samsungHealthEnabled = false))
            readiness.value = result
        }
        compose.onNodeWithTag("studio_coach_final_readiness").assertExists()
        compose.onNodeWithTag("studio_coach_score_breakdown").assertDoesNotExist()
    }

    @Test fun onlyPersistableRecommendationsCanOpenTheActiveProgramPreview() {
        val stable = NativeCoachExerciseAdvice.Advice("increase", "stable", 22.5, 20.0,
            false, 3, NativeCoachExerciseAdvice.Target("reps", text = "8–12"))
        val press = CoachExerciseAdvice("press", "Nyomás", stable, "total", plannedWeight = 20.0)
        val recovery = press.copy(id = "row", name = "Evezés", advice = stable.copy(action = "easy", reason = "recovery"))
        val state = mutableStateOf(CoachUiState(loading = false, programEditable = true,
            target = CoachTarget("p", "a", "A nap"), exercises = listOf(press, recovery)))
        var selected: String? = null
        compose.setContent { TrainPilotTheme {
            StudioCoachRecommendations(state.value, { _, _, _ -> }, {}, {}, { selected = it }, {}, {}, showTarget = false)
        } }
        compose.onNodeWithTag("studio_coach_advice_press").performClick()
        compose.onNodeWithTag("studio_coach_apply_press").performClick()
        compose.runOnIdle { assertEquals("press", selected); state.value = state.value.copy(programEditable = false) }
        compose.onNodeWithTag("studio_coach_apply_press").assertDoesNotExist()
        compose.runOnIdle { state.value = state.value.copy(programEditable = true) }
        compose.onNodeWithTag("studio_coach_advice_row").performClick()
        compose.onNodeWithTag("studio_coach_apply_row").assertDoesNotExist()
    }

    @Test fun programWeightChangesOnlyTheApprovedExerciseInTheApprovedDay() {
        val first = NativeProgramEditor.DayDraft("a", "A", listOf(
            JSONObject().put("id", "press").put("weight", 20).put("reps", "8–12").put("customField", true).toString(),
            JSONObject.quote("row")))
        val second = NativeProgramEditor.DayDraft("b", "B", listOf(JSONObject.quote("press")))
        val draft = NativeProgramEditor.Draft("Program", "", listOf(first, second))
        val edited = NativeCoachVariation.withDayWeight(draft, "a", "press", 22.5)
        assertEquals(second, edited.days[1])
        assertEquals(first.entries[1], edited.days[0].entries[1])
        val entry = JSONObject(edited.days[0].entries[0])
        assertEquals(22.5, entry.getDouble("weight"), 0.0)
        assertEquals("8–12", entry.getString("reps"))
        assertTrue(entry.getBoolean("customField"))
        assertFalse(entry.getBoolean("trial"))
    }

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
                target = CoachTarget("actual-program", "b", "B nap", schedule =
                    com.repforge.app.data.db.CalendarScheduleEntity("actual-schedule", "", "2026-11-14",
                        "2026-11-14T10:00:00Z", "2026-11-14T11:00:00Z", "actual-program", "Program",
                        "b", "B", "planned", 1, "{}"))),
                { p, d, s -> started = "$p:$d:$s" }, { workoutPage++ }, {}, {}, {}, {})
        } }
        compose.onNodeWithTag("studio_coach_start_target").performClick()
        assertEquals("actual-program:b:actual-schedule", started)
        assertEquals(0, workoutPage)
    }

    @Test fun liquidCoachStartKeepsActualTargetAndDisabledAdmission() {
        var started = ""
        val state = mutableStateOf(CoachUiState(loading = false,
            target = CoachTarget("p", "b", "B", upcoming = true)))
        compose.setContent {
            TrainPilotTheme(com.example.ui.theme.TrainPilotPalettes.getById(40)) {
                StudioCoachNextTrainingCard(state.value, { p, d, _ -> started = "$p:$d" }, {})
            }
        }
        compose.onNodeWithTag("studio_coach_start_target").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals("p:b", started); state.value = state.value.copy(loading = true) }
        compose.onNodeWithTag("studio_coach_start_target").assertIsNotEnabled()
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
        compose.onNodeWithTag("studio_coach_day_name").assertDoesNotExist()
        compose.onNodeWithTag("studio_coach_start_target").performClick()
        assertEquals(0, workoutPage)
        assertEquals(1, started)
    }

    @Test fun longExerciseNamesHaveACompleteLayoutAboveTheirRecommendations() {
        val fixtures = listOf(
            Triple(320, 1.6f, "hu" to "Döntött törzsű evezés kétkezes rúddal"),
            Triple(360, 1.3f, "en" to "Single-arm bent-over dumbbell row with support"),
            Triple(360, 1f, "de" to "Vorgebeugtes Rudern mit der Langhantel im Obergriff"),
            Triple(480, 1.6f, "ro" to "Îndreptări românești cu gantere și sprijin unilateral")
        )
        val current = mutableStateOf(fixtures.first())
        compose.setContent {
            val (width, fontScale, translation) = current.value
            val (language, name) = translation
            val density = LocalDensity.current.density
            val advice = NativeCoachExerciseAdvice.Advice("increase", "stable", 22.5, 20.0,
                false, 3, NativeCoachExerciseAdvice.Target("reps", text = "12–15 / 8–12 / 6–8"))
            val state = CoachUiState(loading = false, language = language,
                exercises = listOf(CoachExerciseAdvice("long", name, advice, "total")))
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                TrainPilotTheme {
                    Box(Modifier.size(width.dp, 720.dp)) {
                        StudioCoachRecommendations(state, { _, _, _ -> }, {}, {}, {}, {}, {}, showTarget = false)
                    }
                }
            }
        }
        val row = compose.onNodeWithTag("studio_coach_advice_long")
        val nameNode = compose.onNodeWithTag("studio_coach_name_long", useUnmergedTree = true)
        val action = compose.onNodeWithTag("studio_coach_action_long", useUnmergedTree = true)
        val target = compose.onNodeWithTag("studio_coach_target_long", useUnmergedTree = true)
        fixtures.forEach { fixture ->
            compose.runOnIdle { current.value = fixture }
            val layouts = mutableListOf<TextLayoutResult>()
            nameNode.performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { it(layouts) }
            val layout = layouts.single()
            assertEquals(fixture.third.second, layout.layoutInput.text.text)
            assertFalse(layout.hasVisualOverflow)
            if (fixture.first == 320) assertTrue(layout.lineCount > 1)
            val rowBounds = row.getUnclippedBoundsInRoot()
            assertTrue(rowBounds.bottom - rowBounds.top >= 48.dp)
            assertTrue(nameNode.getUnclippedBoundsInRoot().bottom <= action.getUnclippedBoundsInRoot().top)
            target.assertIsDisplayed()
            row.performClick()
            compose.onNodeWithTag("studio_coach_details_long").assertIsDisplayed()
            row.performClick()
        }
    }

    @Test fun coachAdviceScrollsIndependentlyAndRetainsExpandedActions() {
        val advice = NativeCoachExerciseAdvice.Advice("increase", "stable", 22.5, 20.0,
            false, 3, NativeCoachExerciseAdvice.Target("reps", text = "8–12"))
        val exercises = (0 until 24).map { CoachExerciseAdvice("ex-$it", "Exercise $it", advice, "total") }
        val state = CoachUiState(loading = false, language = "hu", exercises = exercises)
        var opened = ""
        compose.setContent { TrainPilotTheme {
            com.example.ui.screens.CoachPageLayout(
                modifier = Modifier.size(360.dp, 600.dp),
                upper = { Box(Modifier.fillMaxWidth().height(160.dp).testTag("coach_test_upper")) },
                recommendations = { pinned, modifier ->
                    StudioCoachRecommendations(state, { _, _, _ -> }, {}, { opened = it }, {}, {}, {},
                        showTarget = false, modifier = modifier, pinnedList = pinned)
                })
        } }
        val upper = compose.onNodeWithTag("coach_test_upper")
        val header = compose.onNodeWithTag("studio_coach_fixed_advice_header")
        val panel = compose.onNodeWithTag("studio_coach_advice_list")
        val upperBounds = upper.getUnclippedBoundsInRoot()
        val headerBounds = header.getUnclippedBoundsInRoot()
        val panelBounds = panel.getUnclippedBoundsInRoot()
        compose.onNodeWithTag("studio_coach_advice_ex-23").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("Gyakorlat részletei").performScrollTo().performClick()
        assertEquals("ex-23", opened)
        assertEquals(upperBounds, upper.getUnclippedBoundsInRoot())
        assertEquals(headerBounds, header.getUnclippedBoundsInRoot())
        assertEquals(panelBounds, panel.getUnclippedBoundsInRoot())
    }

    @Test fun coachCompactDayKeepsDescriptiveNamesButNotDuplicateLetterLabels() {
        val target = mutableStateOf(CoachTarget("p", "a", "A nap", programTitle = "Otthoni A/B"))
        compose.setContent { TrainPilotTheme {
            StudioCoachNextTrainingCard(CoachUiState(loading = false, target = target.value),
                { _, _, _ -> }, {})
        } }
        compose.onNodeWithTag("studio_coach_day_badge").assertExists()
        compose.onNodeWithTag("studio_coach_day_name").assertDoesNotExist()
        compose.runOnIdle { target.value = target.value.copy(title = "Felsőtest") }
        compose.onNodeWithTag("studio_coach_day_name").assertTextEquals("Felsőtest")
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
        val sleep = JSONObject().put("day", "2026-11-03").put("sleepMinutes", 480)
        val result = NativeCoachReadiness.calculate(sleep, energy, listOf(workout), now, ZoneOffset.UTC)
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

    @Test fun painHoldSaysTodayWhenThereIsNoHistoryAndTheChipWarns() {
        // 2774: no "past pain" claim next to "matching history: 0".
        val pain = NativeCoachExerciseAdvice.Advice("hold", "pain", 0.0, 0.0, false, 0,
            NativeCoachExerciseAdvice.Target("reps", text = "15"))
        compose.setContent { TrainPilotTheme { androidx.compose.foundation.layout.Column {
            StudioWorkoutCoachAdvice(WorkoutCoachAdviceState("crunch", loading = false,
                row = CoachExerciseAdvice("crunch", "Hasprés", pain))) {}
        } } }
        compose.onNodeWithTag("studio_active_coach_reason").assertTextContains("Ma fájdalmat jeleztél", substring = true)
        assertEquals(com.example.ui.components.CoachChipTone.WARNING, com.example.ui.components.coachChipTone(pain))
        assertEquals(com.example.ui.components.CoachChipTone.PROPOSAL, com.example.ui.components.coachChipTone(
            NativeCoachExerciseAdvice.Advice("increase", "stable", 6.0, 5.5, false, 4, NativeCoachExerciseAdvice.Target("reps"))))
        assertEquals(com.example.ui.components.CoachChipTone.NEUTRAL, com.example.ui.components.coachChipTone(null))
    }
}
