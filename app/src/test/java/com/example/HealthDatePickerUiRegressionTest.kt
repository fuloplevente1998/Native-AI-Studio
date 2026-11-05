package com.example

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.ui.components.CompactDatePickerDialog
import com.example.ui.theme.TrainPilotPalettes
import com.example.ui.theme.TrainPilotTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w320dp-h640dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class HealthDatePickerUiRegressionTest {
    @get:Rule val compose = createComposeRule()

    private fun exercisePicker(paletteId: Int, language: String) {
        val initial = LocalDate.parse("2026-11-02")
        var selected by mutableStateOf(initial)
        var opened by mutableStateOf(true)
        var confirmed = 0
        compose.setContent {
            TrainPilotTheme(palette = TrainPilotPalettes.allPalettes.first { it.id == paletteId }, fontScaleMultiplier = 2f) {
                if (opened) CompactDatePickerDialog(selected, language, onDismiss = { opened = false }, onConfirm = {
                    selected = it; confirmed++; opened = false
                }) else TextButton(onClick = { opened = true }, modifier = Modifier.testTag("reopen_picker")) { Text("Open") }
            }
        }
        compose.onNodeWithTag("health_date_day_2").performScrollTo().assertIsSelected()
        compose.onNodeWithTag("health_date_next_month").performScrollTo().performClick()
        compose.onNodeWithTag("health_date_day_3").performScrollTo().performClick()
        compose.onNodeWithTag("health_date_cancel").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(initial, selected); assertEquals(0, confirmed) }
        compose.onNodeWithTag("reopen_picker").performClick()
        // Cancellation also restores the original month and day on reopening.
        compose.onNodeWithTag("health_date_day_2").performScrollTo().assertIsSelected().assertContentDescriptionEquals("2026-11-02")
        compose.onNodeWithTag("health_date_next_year").performScrollTo().performClick()
        compose.onNodeWithTag("health_date_previous_year").performScrollTo().performClick()
        compose.onNodeWithTag("health_date_next_month").performScrollTo().performClick()
        compose.onNodeWithTag("health_date_day_3").performScrollTo().performClick()
        val cell = compose.onNodeWithTag("health_date_day_3").getUnclippedBoundsInRoot()
        assertTrue("Day touch target keeps its height with large text", cell.bottom - cell.top >= 48.dp)
        compose.onNodeWithTag("health_date_day_3").assertIsSelected().assertContentDescriptionEquals("2026-12-03")
        compose.onNodeWithTag("health_date_confirm").performScrollTo().assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(LocalDate.parse("2026-12-03"), selected); assertEquals(1, confirmed) }
        compose.onNodeWithTag("health_date_picker").assertDoesNotExist()
    }

    @Test fun matteHungarianCancelAndConfirmationAtLargeFont() = exercisePicker(1, "hu")
    @Test fun contrastEnglishCancelAndConfirmationAtLargeFont() = exercisePicker(9, "en")
    @Test fun glowGermanCancelAndConfirmationAtLargeFont() = exercisePicker(17, "de")
    @Test fun lightRomanianCancelAndConfirmationAtLargeFont() = exercisePicker(25, "ro")
}
