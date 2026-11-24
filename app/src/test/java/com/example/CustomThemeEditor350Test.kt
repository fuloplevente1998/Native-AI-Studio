package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.model.AppLanguage
import com.example.ui.components.CustomThemeEditorSheet
import com.example.ui.theme.*
import org.junit.Assert.*
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
class CustomThemeEditor350Test {
    @get:Rule val compose = createComposeRule()

    @Test fun previewDoesNotSaveAndNameIsOnlyRequestedAfterSave() {
        val key = newCustomThemeKey()
        val initial = requireNotNull(resolveCustomTheme(CustomThemeDraft.fromStyle(CustomThemeStyle.MATTE), key)).suggestedDraft
        val saved = mutableListOf<CustomThemeDefinition>()
        compose.setContent {
            TrainPilotTheme {
                CustomThemeEditorSheet(initial, key, AppLanguage.HU, onSave = { saved.add(it) }, onDismiss = {})
            }
        }
        compose.onNodeWithTag("custom_theme_name").assertDoesNotExist()
        compose.onNodeWithTag("custom_theme_preview_dialog").assertDoesNotExist()
        compose.onNodeWithTag("custom_theme_open_preview").performClick()
        compose.onNodeWithTag("custom_theme_preview_dialog").assertExists()
        compose.onNodeWithTag("custom_theme_preview_workout").performClick()
        compose.onNodeWithText("Fekvenyomás").assertExists()
        compose.runOnIdle { assertTrue(saved.isEmpty()) }
        compose.onNodeWithTag("custom_theme_close_preview").performClick()
        compose.onNodeWithTag("custom_theme_preview_dialog").assertDoesNotExist()
        compose.onNodeWithTag("custom_theme_name").assertDoesNotExist()
        compose.onNodeWithTag("custom_theme_save").assertIsEnabled().performClick()
        compose.onNodeWithTag("custom_theme_name_dialog").assertExists()
        compose.onNodeWithTag("custom_theme_confirm_save").assertIsNotEnabled()
        compose.onNodeWithTag("custom_theme_name").performTextInput("Teszt szín")
        compose.onNodeWithTag("custom_theme_confirm_save").assertIsEnabled().performClick()
        compose.runOnIdle {
            assertEquals(1, saved.size)
            assertEquals("Teszt szín", saved.single().draft.name)
            assertEquals(key, saved.single().key)
            assertEquals(initial.colors, saved.single().draft.colors)
        }
    }
}
