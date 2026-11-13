package com.example

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.theme.TrainPilotTheme
import com.repforge.app.ui.journal.JournalSetValueDropdown
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class CompactJournalSetRegressionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun presetCustomAndEmptyValuesReachCallback() {
        var value by mutableStateOf("")
        compose.setContent { TrainPilotTheme {
            JournalSetValueDropdown(value, "RIR", "rir", true, listOf("", "0", "1", "2"), "Custom") { value = it }
        } }
        compose.onNodeWithTag("rir").performClick()
        compose.onNodeWithTag("rir_option_2").performClick()
        compose.runOnIdle { assertEquals("2", value) }
        compose.onNodeWithTag("rir").performClick()
        compose.onNodeWithTag("rir_option_empty").performClick()
        compose.runOnIdle { assertEquals("", value) }
        compose.onNodeWithTag("rir").performClick()
        compose.onNodeWithTag("rir_custom").performClick()
        compose.onNodeWithTag("rir_custom_input").performTextReplacement("1.25")
        compose.runOnIdle { assertEquals("1.25", value) }
    }
}
