package com.example

import com.example.model.ExerciseCategory
import com.example.ui.viewmodel.TrainPilotViewModel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuditCustomLibraryViewModelRegressionTest {
    @Test fun unavailableDatabaseDoesNotPublishExerciseOrProgramSuccess() {
        val vm = TrainPilotViewModel()
        vm.toggleAddCustomExerciseDialog(true)
        val before = vm.uiState.value
        vm.addCustomExercise("Offline exercise", ExerciseCategory.CORE, "bodyweight", "Instructions", addToCurrentProgramDay = false)
        val afterExercise = vm.uiState.value
        assertEquals(before.exercises, afterExercise.exercises)
        assertEquals(before.programs, afterExercise.programs)
        assertTrue(afterExercise.showAddCustomExerciseDialog)
        assertEquals(before.statusBannerMessage, afterExercise.statusBannerMessage)

        vm.createNewCustomProgram("Offline program", "Description")
        val afterProgram = vm.uiState.value
        assertEquals(before.programs, afterProgram.programs)
        assertEquals(before.editingProgramId, afterProgram.editingProgramId)
        assertEquals(before.currentSubScreen, afterProgram.currentSubScreen)
        assertEquals(before.statusBannerMessage, afterProgram.statusBannerMessage)
    }
}
