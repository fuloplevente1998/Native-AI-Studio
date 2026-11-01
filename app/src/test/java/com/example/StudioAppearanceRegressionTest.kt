package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.localization.TrainPilotStrings
import com.example.model.AppLanguage
import com.example.ui.components.SubScreenTopBar
import com.example.ui.components.TrainPilotRedCloseButton
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
class StudioAppearanceRegressionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun smallerCloseKeepsAccessibleHitTargetAndCompactHeading() {
        var closed = 0
        compose.setContent {
            TrainPilotTheme {
                Column(Modifier.width(360.dp)) {
                    SubScreenTopBar("Beállítások", {}, {})
                    Box { TrainPilotRedCloseButton({ closed++ }, testTag = "close_fixture") }
                }
            }
        }
        compose.onNodeWithTag("close_fixture").assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(48.dp).performClick()
        compose.onNodeWithTag("close_fixture_visual", useUnmergedTree = true)
            .assertWidthIsEqualTo(38.dp).assertHeightIsEqualTo(38.dp)
        assertEquals(1, closed)
        val heading = compose.onNodeWithText("Beállítások").getUnclippedBoundsInRoot()
        assertTrue(heading.bottom - heading.top <= 24.dp)
    }

    @Test fun settingsThemesStartClosedThenShareAGroupAndKeepPaletteSelectionWorking() {
        var selected by mutableIntStateOf(1)
        compose.setContent {
            TrainPilotTheme(palette = TrainPilotPalettes.getById(selected)) {
                SettingsMainScreen(uiState = TrainPilotUiState(selectedPaletteId = selected),
                    strings = TrainPilotStrings.forLanguage(AppLanguage.HU), onBack = {}, onOpenPalettePicker = {},
                    onSelectPalette = { selected = it }, onSelectLanguage = {}, onCycleRestTimer = {},
                    onToggleSoundAlert = {}, onToggleProgressionModal = {}, onSaveProgression = { _, _, _ -> },
                    onNavigateToSubScreen = {}, onShowBanner = {})
            }
        }
        compose.onNodeWithTag("settings_themes_group").assertDoesNotExist()
        val list = compose.onNodeWithTag("settings_main_screen")
        list.performScrollToNode(hasTestTag("settings_open_palettes_row"))
        compose.onNodeWithTag("settings_open_palettes_row").performClick()
        list.performScrollToNode(hasTestTag("settings_themes_group"))
        val frame = compose.onNodeWithTag("settings_themes_group").getUnclippedBoundsInRoot()
        repeat(4) { index ->
            val category = compose.onNodeWithTag("settings_theme_category_$index").getUnclippedBoundsInRoot()
            assertTrue(category.left >= frame.left && category.right <= frame.right)
            assertTrue(category.top >= frame.top && category.bottom <= frame.bottom)
        }
        compose.onNodeWithTag("settings_theme_category_toggle_1").performClick()
        val choice = TrainPilotPalettes.getById(9).localizedName(AppLanguage.HU)
        list.performScrollToNode(hasText(choice))
        compose.onNodeWithText(choice).performClick()
        compose.runOnIdle { assertEquals(9, selected) }
        list.performScrollToNode(hasTestTag("settings_open_palettes_row"))
        compose.onNodeWithTag("settings_open_palettes_row").performClick()
        compose.onNodeWithTag("settings_themes_group").assertDoesNotExist()
        compose.runOnIdle { assertEquals(9, selected) }
    }
}
