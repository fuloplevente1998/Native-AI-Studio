package com.example

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.example.ui.components.StudioNativeEditorTheme
import com.example.ui.theme.TrainPilotTheme
import com.repforge.app.NativeJournalEditor
import com.repforge.app.data.db.WorkoutHistoryEntity
import com.repforge.app.data.db.WorkoutHistoryExerciseEntity
import com.repforge.app.data.db.WorkoutHistoryExerciseWithSets
import com.repforge.app.data.db.WorkoutHistorySetEntity
import com.repforge.app.data.db.WorkoutHistoryWithDetails
import com.repforge.app.ui.journal.JournalEditState
import com.repforge.app.ui.journal.JournalEditorDialog
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class FullHeightJournalEditorRegressionTest {
    @get:Rule val compose = createComposeRule()

    private fun fixture(): JournalEditState {
        val start = "2026-11-13T17:30:00Z"
        val end = "2026-11-13T18:25:00Z"
        val sets = JSONArray()
        repeat(10) { i -> sets.put(JSONObject().put("setId", "set-$i").put("weight", 5.5)
            .put("reps", "15").put("done", true).put("actualRir", 2)) }
        val raw = JSONObject().put("id", "edit-workout").put("started", start).put("finished", end)
            .put("exercises", JSONArray().put(JSONObject().put("id", "goblet").put("hu", "Goblet guggolás")
                .put("loadType", "total").put("repUnit", "ism.").put("measurementType", "reps").put("sets", sets)))
        val workout = WorkoutHistoryEntity(id = "edit-workout", workoutKey = "edit-workout", syncId = "edit-workout",
            started = start, finished = end, startedEpochMs = java.time.Instant.parse(start).toEpochMilli(),
            finishedEpochMs = java.time.Instant.parse(end).toEpochMilli(), durationSeconds = 3300,
            programId = "ab", programName = "Otthoni A/B", dayId = "a", dayName = "A", scheduleId = "",
            quickWorkout = false, originDevice = "phone", ownershipProtocol193 = 0, ownerEpoch193 = 0,
            effortFeedback = null, feedbackSkipped = false, feedbackUpdatedAt = 0, totalVolumeKg = 825.0,
            completedSetsCount = 10, totalSetsCount = 10, totalRepsCount = 150, hasPr226 = false,
            prRecordsJson226 = "[]", wearMetricsJson = null, health240Json = null, timeLogs162Json = null,
            activeIntervalsJson = null, editedAt = null, rawJson = raw.toString())
        val exercise = WorkoutHistoryExerciseEntity(rowId = 1, workoutHistoryId = workout.id,
            exerciseId = "goblet", nameHu = "Goblet guggolás", nameEn = "Goblet squat", loadType = "total",
            repUnit = "ism.", measurementType = "reps", orderIndex = 0, actualRir = null, setRir229 = true)
        val storedSets = (0 until 10).map { i -> WorkoutHistorySetEntity(rowId = i + 1L, setId = "set-$i",
            workoutHistoryId = workout.id, historyExerciseRowId = 1, exerciseId = "goblet", setNumber = i + 1,
            weight = 5.5, reps = "15", leftSeconds = null, rightSeconds = null, distanceMeters = null,
            done = true, actualRir = 2, gpsRouteKey163 = null, gpsRouteKey193 = null) }
        val detail = WorkoutHistoryWithDetails(workout, listOf(WorkoutHistoryExerciseWithSets(exercise, storedSets)), emptyList())
        val context = ApplicationProvider.getApplicationContext<Context>()
        return JournalEditState(NativeJournalEditor.open(context, detail), NativeJournalEditor.from(detail), emptyList())
    }

    @Test fun fullHeightEditorPinsSaveAndPreservesDatesAndOtherFieldsWhenEditing() {
        var state by mutableStateOf(fixture())
        var saved: NativeJournalEditor.Draft? = null
        compose.setContent { TrainPilotTheme { StudioNativeEditorTheme {
            JournalEditorDialog(state, "hu", { state = state.copy(draft = it) }, { saved = it }, {})
        } } }
        val sheet = compose.onNodeWithTag("journal_editor_sheet").getUnclippedBoundsInRoot()
        assertTrue(sheet.bottom - sheet.top > 650.dp)
        val list = compose.onNodeWithTag("journal_editor_list")
        list.performScrollToNode(hasTestTag("journal_editor_weight_0_0"))
        val weightBounds = compose.onNodeWithTag("journal_editor_weight_0_0").getUnclippedBoundsInRoot()
        val repsBounds = compose.onNodeWithTag("journal_editor_reps_0_0").getUnclippedBoundsInRoot()
        val rirBounds = compose.onNodeWithTag("journal_editor_rir_0_0").getUnclippedBoundsInRoot()
        assertEquals(weightBounds.top, repsBounds.top)
        assertEquals(weightBounds.top, rirBounds.top)
        val cardBounds = compose.onNodeWithTag("journal_editor_set_0_0").getUnclippedBoundsInRoot()
        assertTrue(cardBounds.bottom - cardBounds.top <= 140.dp)
        compose.onNodeWithTag("journal_editor_weight_0_0").performClick()
        compose.onNodeWithTag("journal_editor_weight_0_0_custom").performClick()
        compose.onNodeWithTag("journal_editor_weight_0_0_custom_input").performTextReplacement("6.25")
        compose.onNodeWithTag("journal_editor_save").assertIsDisplayed().performClick()
        compose.runOnIdle {
            val result = requireNotNull(saved)
            assertEquals(state.opened.detail.workout.started, result.started)
            assertEquals(state.opened.detail.workout.finished, result.finished)
            assertEquals("6.25", result.exercises.first().sets.first().weight)
            assertEquals("15", result.exercises.first().sets.first().reps)
            assertEquals("2", result.exercises.first().sets.first().rir)
            assertEquals(10, result.exercises.first().sets.size)
        }
        list.performScrollToNode(hasTestTag("journal_editor_add_set_0"))
        compose.onNodeWithTag("journal_editor_save").assertIsDisplayed()
        compose.onNodeWithTag("journal_editor_cancel_footer").assertIsDisplayed()
    }

    @Test fun choosingSetValuesChangesDraftWithoutCompletingTheSet() {
        var state by mutableStateOf(fixture())
        compose.setContent { TrainPilotTheme { StudioNativeEditorTheme {
            JournalEditorDialog(state, "hu", { state = state.copy(draft = it) }, {}, {})
        } } }
        compose.onNodeWithTag("journal_editor_list").performScrollToNode(hasTestTag("journal_editor_done_0_0"))
        val weight = compose.onNodeWithTag("journal_editor_weight_0_0").getUnclippedBoundsInRoot()
        val reps = compose.onNodeWithTag("journal_editor_reps_0_0").getUnclippedBoundsInRoot()
        val rir = compose.onNodeWithTag("journal_editor_rir_0_0").getUnclippedBoundsInRoot()
        assertEquals(weight.top, reps.top)
        assertEquals(reps.top, rir.top)
        compose.onNodeWithTag("journal_editor_done_0_0").performClick()
        compose.onNodeWithTag("journal_editor_reps_0_0").performClick()
        compose.onNodeWithTag("journal_editor_reps_0_0_option_1").performClick()
        compose.onNodeWithTag("journal_editor_rir_0_0").performClick()
        compose.onNodeWithTag("journal_editor_rir_0_0_option_empty").performClick()
        compose.runOnIdle {
            val set = state.draft.exercises.first().sets.first()
            assertEquals("1", set.reps)
            assertEquals("", set.rir)
            assertFalse(set.done)
            assertEquals("5.5", set.weight)
        }
    }

    @Test fun swipeCannotSilentlyDiscardEditsAndKeepingTheDraftPreservesTheNote() {
        var state by mutableStateOf(fixture())
        var cancelled = 0
        compose.setContent { TrainPilotTheme { StudioNativeEditorTheme {
            JournalEditorDialog(state, "hu", { state = state.copy(draft = it) }, {}, { cancelled++ })
        } } }
        compose.onNodeWithTag("journal_editor_list").performScrollToNode(hasTestTag("journal_editor_note"))
        compose.onNodeWithTag("journal_editor_note").performTextReplacement("Megőrzendő megjegyzés")
        compose.onNodeWithTag("journal_editor_sheet").performTouchInput {
            swipeDown(startY = 20f, endY = height - 20f, durationMillis = 250)
        }
        compose.onNodeWithTag("journal_editor_discard_confirmation").assertExists()
        compose.runOnIdle { assertEquals(0, cancelled) }
        compose.onNodeWithTag("journal_editor_discard_keep").performClick()
        compose.runOnIdle { assertEquals("Megőrzendő megjegyzés", state.draft.note); assertEquals(0, cancelled) }
        compose.onNodeWithTag("journal_editor_note").assertTextContains("Megőrzendő megjegyzés")
    }

    @Test fun savingBlocksCancelAndSaveUntilTheOperationCompletes() {
        var state by mutableStateOf(fixture().copy(saving = true))
        var cancelled = 0
        var saved = 0
        compose.setContent { TrainPilotTheme { StudioNativeEditorTheme {
            JournalEditorDialog(state, "hu", {}, { saved++ }, { cancelled++ })
        } } }
        compose.onNodeWithTag("journal_editor_save").assertIsNotEnabled()
        compose.onNodeWithTag("journal_editor_cancel_footer").assertIsNotEnabled()
        compose.onNodeWithTag("journal_editor_cancel").performClick()
        compose.runOnIdle { assertEquals(0, cancelled); assertEquals(0, saved); state = state.copy(saving = false) }
        compose.onNodeWithTag("journal_editor_cancel_footer").performClick()
        compose.runOnIdle { assertEquals(1, cancelled); assertEquals(0, saved) }
    }
}
