package com.example

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.model.AppLanguage
import com.example.ui.screens.StudioWorkoutModesDialog
import com.example.ui.theme.TrainPilotTheme
import com.example.ui.viewmodel.TrainPilotUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w480dp-h960dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class StudioWorkoutModesLanguageTest {
    @get:Rule val compose = createComposeRule()

    @Test fun sharedEditorFollowsSelectedLanguageAndUsesCanonicalValues() {
        var language by mutableStateOf(AppLanguage.HU)
        var previewStyle = ""
        var previewRule = ""
        var inherited = true
        compose.setContent {
            TrainPilotTheme {
                StudioWorkoutModesDialog(
                    state = TrainPilotUiState(language = language),
                    scope = "global",
                    onDismiss = {}, onCycleRest = {}, onToggleSound = {},
                    onToggleVibration = {},
                    onPreview = { style, rule, inherit ->
                        previewStyle = style
                        previewRule = rule
                        inherited = inherit
                    },
                    onApprove = {}, onEditPreview = {}, onUndo = {}
                )
            }
        }
        val titles = listOf(
            AppLanguage.HU to "Edzés beállításai és progresszió",
            AppLanguage.EN to "Workout settings & progression",
            AppLanguage.DE to "Trainingseinstellungen & Progression",
            AppLanguage.RO to "Setări antrenament și progresie"
        )
        for ((next, title) in titles) {
            compose.runOnIdle { language = next }
            compose.onNodeWithText(title).assertExists()
            compose.onNodeWithTag("studio_modes_style_normal").assertExists()
            compose.onNodeWithTag("studio_modes_style_hit").assertExists()
            compose.onNodeWithTag("studio_modes_progression_double").assertExists()
            compose.onNodeWithTag("studio_modes_progression_linear").assertExists()
            compose.onNodeWithTag("studio_modes_progression_rir").assertExists()
            compose.onNodeWithTag("studio_modes_vibration").assertExists()
        }
        compose.onNodeWithTag("studio_modes_style_hit").performScrollTo().performClick()
        compose.onNodeWithTag("studio_modes_progression_rir").performScrollTo().performClick()
        compose.onNodeWithTag("studio_modes_preview").performClick()
        compose.runOnIdle {
            assertEquals("hit", previewStyle)
            assertEquals("rir", previewRule)
            assertFalse(inherited)
        }
    }
}
