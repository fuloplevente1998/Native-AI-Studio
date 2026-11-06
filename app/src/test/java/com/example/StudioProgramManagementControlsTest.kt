package com.example

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.*
import com.example.ui.components.StudioProgramManagementControls
import com.example.ui.theme.TrainPilotTheme
import com.example.model.TrainingProgramModel
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h960dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class StudioProgramManagementControlsTest {
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()
    @Test fun deleteRequiresExplicitConfirmationAndCopyHasItsOwnAction() {
        var deleted = 0; var copies = 0; var saved = 0
        val program = TrainingProgramModel("own", "Saját terv", "Own", "", "", emptyList(), true, false, emptyList())
        compose.setContent { TrainPilotTheme {
            StudioProgramManagementControls(program, false, false, { copies++ }, { deleted++ }, {}, {}, { saved++ })
        } }
        val saveBounds = compose.onNodeWithTag("studio_program_save").getUnclippedBoundsInRoot()
        val deleteBounds = compose.onNodeWithTag("studio_program_delete").getUnclippedBoundsInRoot()
        assertTrue(saveBounds.left < deleteBounds.left)
        compose.onNodeWithTag("studio_program_save").performClick()
        assertEquals(1, saved); assertEquals(0, deleted)
        compose.onNodeWithTag("studio_program_copy").performClick()
        assertEquals(1, copies); assertEquals(0, deleted)
        compose.onNodeWithTag("studio_program_delete").performClick()
        assertEquals(0, deleted)
        compose.onNodeWithTag("studio_program_confirm").performClick()
        assertEquals(1, deleted)
    }

    @Test fun programSaveCapturesNameAndUnsavedExerciseFieldsWithoutTheIndividualSaveButton() {
        val exercise = com.example.model.ExerciseItem("press", "Nyomás", defaultSets = 3)
        val day = com.example.model.ProgramDayModel("own:a", "A", "Alap A", "Base A", 45, listOf(exercise))
        val program = TrainingProgramModel("own", "Eredeti", "Original", "", "", emptyList(), true, false, listOf(day))
        val state = com.example.ui.viewmodel.TrainPilotUiState(programs = listOf(program), exercises = listOf(exercise),
            editingProgramId = "own", expandedEditorExerciseId = "press")
        var captured: Pair<String, List<com.example.model.StudioProgramPrescriptionDraft>>? = null
        var individualSaves = 0
        compose.setContent { TrainPilotTheme {
            com.example.ui.screens.ProgramEditorScreen(state, {}, {}, {}, {},
                { _, _, _, _, _ -> individualSaves++ }, {}, {},
                onSaveProgram = { name, fields -> captured = name to fields })
        } }
        compose.onNodeWithTag("editor_add_custom_exercise_button").assertDoesNotExist()
        compose.onNodeWithTag("studio_program_name").performTextReplacement("Mentett terv")
        val plus = hasTestTag("editor_stepper_plus") and hasAnyAncestor(hasTestTag("editor_sets_press"))
        compose.onNode(plus).performScrollTo().performClick()
        compose.onNode(plus).performClick()
        compose.onNodeWithTag("studio_program_save").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(0, individualSaves)
            assertEquals("Mentett terv", captured?.first)
            assertEquals(5, captured?.second?.single()?.sets)
            assertEquals("own:a", captured?.second?.single()?.dayKey)
        }
    }
}
