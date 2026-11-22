package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.localization.TrainPilotStrings
import com.example.model.AppLanguage
import com.example.ui.screens.SettingsMainScreen
import com.example.ui.theme.TrainPilotPalettes
import com.example.ui.theme.TrainPilotTheme
import com.example.ui.viewmodel.TrainPilotUiState
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
class SettingsThemePressFeedbackRegressionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun allThemeCategoryHeadersRespondToATapInTheirSidePadding() {
        var selected by mutableIntStateOf(9)
        compose.setContent {
            TrainPilotTheme(palette = TrainPilotPalettes.getById(selected)) {
                SettingsMainScreen(uiState = TrainPilotUiState(selectedPaletteId = selected),
                    strings = TrainPilotStrings.forLanguage(AppLanguage.HU), onBack = {}, onOpenPalettePicker = {},
                    onSelectPalette = { selected = it }, onSelectLanguage = {}, onCycleRestTimer = {},
                    onToggleSoundAlert = {}, onToggleProgressionModal = {}, onToggleVibration = {},
                    onNavigateToSubScreen = {}, onShowBanner = {})
            }
        }
        val list = compose.onNodeWithTag("settings_main_screen")
        list.performScrollToNode(hasTestTag("settings_open_palettes_row"))
        compose.onNodeWithTag("settings_open_palettes_row").performClick()
        val initial = compose.onNodeWithTag("settings_palette_choice_9")
        initial.assertIsSelected()
        compose.onNodeWithTag("settings_theme_category_toggle_1").performClick()
        repeat(4) { index ->
            val tag = "settings_theme_category_toggle_$index"
            val choice = "settings_palette_choice_${index * 8 + 1}"
            list.performScrollToNode(hasTestTag(tag))
            val header = compose.onNodeWithTag(tag)
            val bounds = header.fetchSemanticsNode().boundsInRoot
            val card = compose.onNodeWithTag("settings_theme_category_$index").fetchSemanticsNode().boundsInRoot
            assertEquals("The collapsed header owns the full width", card.width, bounds.width, 1f)
            assertEquals("The collapsed header owns the full height", card.height, bounds.height, 1f)

            // The label starts 12 dp in. This hit is on the painted card margin.
            header.performTouchInput { click(Offset(5f, centerY)) }
            list.performScrollToNode(hasTestTag(choice))
            compose.onNodeWithTag(choice).assertIsDisplayed()
            val next = compose.onNodeWithTag("settings_palette_choice_${index * 8 + 2}")
            val first = compose.onNodeWithTag(choice).getUnclippedBoundsInRoot()
            assertEquals(first.top, next.getUnclippedBoundsInRoot().top)
            compose.onNodeWithTag("settings_palette_choice_${index * 8 + 5}").assertIsDisplayed()
            list.performScrollToNode(hasTestTag(tag))
            if (index < 3) {
                header.performTouchInput { click(Offset(width - 5f, centerY)) }
                compose.onNodeWithTag(choice).assertDoesNotExist()
            }
        }
        list.performScrollToNode(hasTestTag("settings_palette_choice_25"))
        compose.onNodeWithTag("settings_palette_choice_25").performClick()
        compose.runOnIdle { assertEquals(25, selected) }
    }
}
