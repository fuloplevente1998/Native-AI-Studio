package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.model.ExerciseCategory
import com.example.ui.components.PersonalPlannerModalDialog
import com.example.ui.screens.AddCustomExerciseModalDialog
import com.example.ui.theme.TrainPilotTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class FullHeightProfileCustomRegressionTest {
    @get:Rule val compose = createComposeRule()

    private fun assertFullHeightAndPinnedFooter() {
        val body = compose.onNodeWithTag("full_height_form_body").getUnclippedBoundsInRoot()
        val footer = compose.onNodeWithTag("full_height_form_footer").getUnclippedBoundsInRoot()
        assertTrue(body.bottom - body.top > 400.dp)
        assertTrue(footer.top >= body.bottom)
        compose.onNodeWithTag("full_height_form_footer").assertIsDisplayed()
    }

    @Test fun plannerKeepsFooterVisibleAndSavesSelectedValues() {
        var savedHeight = 0
        var savedEquipment = ""
        compose.setContent { TrainPilotTheme {
            PersonalPlannerModalDialog(initialEquipmentHu = "Saját testsúly",
                onSaveAndOpenPlan = { height, _, _, _, _, equipment, _, _, _, _ ->
                    savedHeight = height; savedEquipment = equipment
                }, onDismiss = {})
        } }
        assertFullHeightAndPinnedFooter()
        compose.onNodeWithTag("profile_height_box").performClick()
        compose.onNodeWithText("181 cm").performScrollTo().performClick()
        compose.onNodeWithTag("profile_equipment_box").performScrollTo()
        compose.onNodeWithText("Terv összeállítása és mentése").assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(181, savedHeight)
            assertEquals("Saját testsúly", savedEquipment)
        }
    }

    @Test fun plannerCloseCancelsWithoutSaving() {
        var saves = 0
        var dismissals = 0
        compose.setContent { TrainPilotTheme {
            PersonalPlannerModalDialog(onSaveAndOpenPlan = { _, _, _, _, _, _, _, _, _, _ -> saves++ },
                onDismiss = { dismissals++ })
        } }
        compose.onNodeWithTag("planner_skip_btn").performClick()
        compose.runOnIdle { assertEquals(0, saves); assertEquals(1, dismissals) }
    }

    @Test fun customExercisePreservesInputsAndDefaultsWhenSaved() {
        var result: List<Any>? = null
        compose.setContent { TrainPilotTheme {
            AddCustomExerciseModalDialog(onDismiss = {}, onSaveCustomExercise = { name, category, equipment, description, video, timed, add ->
                result = listOf(name, category, equipment, description, video, timed, add)
            })
        } }
        assertFullHeightAndPinnedFooter()
        compose.onNodeWithTag("custom_exercise_name_input").performTextInput("Saját gyakorlat")
        compose.onNodeWithTag("custom_exercise_description_input").performScrollTo().performTextInput("Lassan")
        compose.onNodeWithTag("custom_exercise_save_button").assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(listOf("Saját gyakorlat", ExerciseCategory.LEGS,
                "Saját testsúly / Kültéri (pl. Kör futás)", "Lassan", "", false, true), result)
        }
    }

    @Test fun customExercisePassesEmptyNameToExistingSaveHandler() {
        var savedName: String? = null
        compose.setContent { TrainPilotTheme {
            AddCustomExerciseModalDialog(onDismiss = {},
                onSaveCustomExercise = { name, _, _, _, _, _, _ -> savedName = name })
        } }
        compose.onNodeWithTag("custom_exercise_save_button").performClick()
        compose.runOnIdle { assertEquals("", savedName) }
    }

    @Test fun customExerciseCancelDoesNotSave() {
        var saves = 0
        var dismissals = 0
        compose.setContent { TrainPilotTheme {
            AddCustomExerciseModalDialog(onDismiss = { dismissals++ },
                onSaveCustomExercise = { _, _, _, _, _, _, _ -> saves++ })
        } }
        compose.onNodeWithText("Mégse").performClick()
        compose.runOnIdle { assertEquals(0, saves); assertEquals(1, dismissals) }
    }
}
