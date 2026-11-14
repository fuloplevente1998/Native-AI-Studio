package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.localization.TrainPilotStrings
import com.example.model.*
import com.example.ui.components.TrainPilotGlobalTopHeaderAndNav
import com.example.ui.screens.*
import com.example.ui.theme.TrainPilotTheme
import com.example.ui.viewmodel.TrainPilotUiState
import com.repforge.app.ui.home.HomeWorkoutTarget
import com.repforge.app.ui.progress.NativeProgressPhoto
import com.repforge.app.ui.progress.NativeProgressPhotoGallery
import com.repforge.app.ui.progress.ProgressUiState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h900dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class DeviceFollowupUiRegressionTest {
    @get:Rule val compose = createComposeRule()
    private val strings = TrainPilotStrings.forLanguage(AppLanguage.HU)
    private val exercise = ExerciseItem("goblet-squat", "Goblet guggolás")
    private val program = TrainingProgramModel("home-basic", "Otthoni A/B – Alap", "Home A/B", "", "",
        emptyList(), false, true, listOf(
            ProgramDayModel("home-basic:a", "A nap", "A", "A", 60, listOf(exercise)),
            ProgramDayModel("home-basic:b", "B nap", "B", "B", 60, listOf(exercise))))
    private fun state() = TrainPilotUiState(programs = listOf(program), exercises = listOf(exercise),
        selectedExerciseId = exercise.id, editingProgramId = program.id)

    @Test fun homeDayBadgeTracksTheRealTargetInsteadOfAlwaysShowingFirstDay() {
        var state by mutableStateOf(state().copy(homeWorkoutTarget = HomeWorkoutTarget("home-basic", "a"),
            calendarEvents = listOf(CalendarDayEvent("plan-b", 14, "18:00", "B nap", 1, 120,
                CalendarEventStatus.PLANNED, "B", "2026-11-14", "home-basic", "b"))))
        compose.setContent { TrainPilotTheme {
            HomeScreen(state, strings, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
        } }
        compose.onNodeWithTag("home_planned_day", useUnmergedTree = true).assertTextEquals("A nap")
        compose.runOnIdle { state = state.copy(homeWorkoutTarget = HomeWorkoutTarget("home-basic", "b", "plan-b")) }
        compose.onNodeWithTag("home_planned_day", useUnmergedTree = true).assertTextEquals("B nap")
        compose.onNodeWithText("1 gyakorlat • kb. 120 perc", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Aktív program • Otthoni A/B", useUnmergedTree = true).assertExists()
    }

    @Test fun privatePhotoChoicesAreNewestFirstAndVerticalAndOpenTheCorrectPhoto() {
        var selected = ""
        val photos = listOf(
            NativeProgressPhoto("old", "before", 1_700_000_000_000, "Program", "A", "phone"),
            NativeProgressPhoto("new", "after", 1_710_000_000_000, "Program", "B", "phone"))
        compose.setContent { TrainPilotTheme {
            Box(Modifier.width(355.dp)) {
                NativeProgressPhotoGallery(ProgressUiState(progressPhotos = photos, photosLoading = false),
                    { selected = it }, {}, vertical = true)
            }
        } }
        val newest = compose.onNodeWithTag("progress_photo_new").getUnclippedBoundsInRoot()
        val oldest = compose.onNodeWithTag("progress_photo_old").getUnclippedBoundsInRoot()
        assertEquals(newest.left, oldest.left)
        assertTrue(newest.bottom <= oldest.top)
        compose.onNodeWithTag("progress_photo_new").performClick()
        assertEquals("new", selected)
    }

    @Test fun editorAndExerciseDetailHeadersRemainBelowGlobalNavWhileTheirBodyScrolls() {
        var detail by mutableStateOf(false)
        compose.setContent { TrainPilotTheme {
            Scaffold(modifier = Modifier.height(600.dp), topBar = { TrainPilotGlobalTopHeaderAndNav(MainTab.PROGRAMS, SubScreen.PROGRAM_EDITOR,
                AppLanguage.HU, 1, strings, {}, { _, _ -> }, {}, {}, {}) }) { padding ->
                Box(Modifier.fillMaxSize().padding(padding).testTag("followup_content")) {
                    if (detail) ExerciseDetailScreen(state(), {}, {}, {}, {})
                    else ProgramEditorScreen(state(), {}, {}, {}, {}, { _, _, _, _, _ -> }, {}, {})
                }
            }
        } }
        fun assertHeader(title: String, body: String) {
            val before = compose.onNodeWithText(title).getUnclippedBoundsInRoot()
            val content = compose.onNodeWithTag("followup_content").getUnclippedBoundsInRoot()
            assertTrue(before.top >= content.top)
            assertTrue(before.bottom <= content.bottom)
            compose.onNodeWithTag(body).performTouchInput { swipeUp() }
            val range = compose.onNodeWithTag(body).fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
            assertTrue("Body must actually scroll", range.value() > 0f)
            assertEquals(before, compose.onNodeWithText(title).getUnclippedBoundsInRoot())
        }
        assertHeader(strings.editProgramTitle, "program_editor_body")
        compose.runOnIdle { detail = true }
        assertHeader(strings.detailsAction, "exercise_detail_body")
    }
}
