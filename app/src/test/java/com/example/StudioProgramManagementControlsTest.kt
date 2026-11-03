package com.example

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
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
        var deleted = 0; var copies = 0
        val program = TrainingProgramModel("own", "Saját terv", "Own", "", "", emptyList(), true, false, emptyList())
        compose.setContent { TrainPilotTheme {
            StudioProgramManagementControls(program, false, false, { copies++ }, { deleted++ }, {}, {})
        } }
        compose.onNodeWithTag("studio_program_copy").performClick()
        assertEquals(1, copies); assertEquals(0, deleted)
        compose.onNodeWithTag("studio_program_delete").performClick()
        assertEquals(0, deleted)
        compose.onNodeWithTag("studio_program_confirm").performClick()
        assertEquals(1, deleted)
    }
}
