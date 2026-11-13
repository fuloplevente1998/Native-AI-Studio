package com.example

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.model.AppLanguage
import com.example.model.ExerciseSetItem
import com.example.ui.components.StudioStrengthSetCard
import com.example.ui.theme.TrainPilotTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w480dp-h960dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class StudioStrengthSetCardRegressionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun dropdownsPreserveOtherValuesAndDoNotCompleteTheSet() {
        var item by mutableStateOf(ExerciseSetItem(1, 5.5f, 15, 2, false, id = "set-a"))
        var completions = 0
        compose.setContent {
            TrainPilotTheme {
                StudioStrengthSetCard(item, "workout/1/exercise/set-a", AppLanguage.HU, "kg", "ism.", true, true,
                    onValues = { kg, reps, rir -> item = item.copy(weightKg = kg, reps = reps,
                        rir = rir ?: item.rir, actualRirProvided = rir != null) },
                    onToggle = { completions++ }, onDelete = {})
            }
        }
        compose.onNodeWithTag("active_set_reps_1").performClick()
        compose.onNodeWithTag("active_set_reps_1_option_20").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(20, item.reps)
            assertEquals(5.5f, item.weightKg)
            assertFalse(item.actualRirProvided)
            assertFalse(item.completed)
            assertEquals(0, completions)
        }
        compose.onNodeWithTag("active_set_rir_1").performClick()
        compose.onNodeWithTag("active_set_rir_1_option_3").performScrollTo().performClick()
        compose.onNodeWithTag("active_set_weight_1").performClick()
        compose.onNodeWithTag("active_set_weight_1_custom").performClick()
        compose.onNodeWithTag("active_set_weight_1_custom_input").performTextReplacement("6,25")
        compose.onNodeWithTag("active_set_weight_1_custom_apply").performClick()
        compose.runOnIdle {
            assertEquals(6.25f, item.weightKg)
            assertEquals(20, item.reps)
            assertEquals(3, item.rir)
            assertTrue(item.actualRirProvided)
            assertFalse(item.completed)
            assertEquals(0, completions)
        }
        compose.onNodeWithTag("toggle_set_1").performClick()
        compose.runOnIdle { assertEquals(1, completions) }
    }

    @Test fun decimalWeightUsesItsCanonicalTextWithoutFloatPrecisionNoise() {
        compose.setContent { TrainPilotTheme {
            StudioStrengthSetCard(ExerciseSetItem(1, 19.7f, 15, 2, false, id = "set-a"),
                "workout/1/exercise/set-a", AppLanguage.HU, "kg", "ism.", true, true,
                onValues = { _, _, _ -> }, onToggle = {}, onDelete = {})
        } }
        compose.onNodeWithTag("active_set_weight_1").assertTextContains("19,7")
    }

    @Test fun ownershipLockDisablesValuesCompletionAndDeletionAndClosesAnOpenMenu() {
        var enabled by mutableStateOf(true)
        var changes = 0
        compose.setContent { TrainPilotTheme {
            StudioStrengthSetCard(ExerciseSetItem(1, 5.5f, 15, 2, false, id = "set-a"),
                "workout/1/exercise/set-a", AppLanguage.HU, "kg", "ism.", enabled, true,
                onValues = { _, _, _ -> changes++ }, onToggle = { changes++ }, onDelete = { changes++ })
        } }
        compose.onNodeWithTag("active_set_reps_1").performClick()
        compose.onNodeWithTag("active_set_reps_1_menu").assertExists()
        compose.runOnIdle { enabled = false }
        compose.onNodeWithTag("active_set_reps_1_menu").assertDoesNotExist()
        for (tag in listOf("active_set_weight_1", "active_set_reps_1", "active_set_rir_1", "toggle_set_1", "delete_set_1")) {
            compose.onNodeWithTag(tag).assertIsNotEnabled()
        }
        compose.runOnIdle { assertEquals(0, changes) }
    }
}
