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

    @Test fun liquidCloseHasAnElongatedFrameAndRetainsItsTapTarget() {
        var closed = 0
        compose.setContent {
            TrainPilotTheme(palette = TrainPilotPalettes.getById(40)) {
                TrainPilotRedCloseButton(onClick = { closed++ }, testTag = "liquid_close_fixture")
            }
        }
        val close = compose.onNodeWithTag("liquid_close_fixture")
        close.assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(48.dp)
        compose.onNodeWithTag("liquid_close_fixture_visual", useUnmergedTree = true)
            .assertWidthIsEqualTo(46.dp).assertHeightIsEqualTo(38.dp)
        close.performClick()
        compose.runOnIdle { assertEquals(1, closed) }
    }

    @Test fun settingsThemesStartClosedThenShareAGroupAndKeepPaletteSelectionWorking() {
        var selected by mutableIntStateOf(1)
        compose.setContent {
            TrainPilotTheme(palette = TrainPilotPalettes.getById(selected)) {
                SettingsMainScreen(uiState = TrainPilotUiState(selectedPaletteId = selected),
                    strings = TrainPilotStrings.forLanguage(AppLanguage.HU), onBack = {}, onOpenPalettePicker = {},
                    onSelectPalette = { selected = it }, onSelectLanguage = {}, onCycleRestTimer = {},
                    onToggleSoundAlert = {}, onToggleProgressionModal = {}, onToggleVibration = {},
                    onNavigateToSubScreen = {}, onShowBanner = {})
            }
        }
        compose.onNodeWithTag("settings_themes_group").assertExists()
        val list = compose.onNodeWithTag("settings_main_screen")
        list.performScrollToNode(hasTestTag("settings_open_palettes_row"))
        val collapsedFrame = compose.onNodeWithTag("settings_themes_group").getUnclippedBoundsInRoot()
        val header = compose.onNodeWithTag("settings_open_palettes_row").getUnclippedBoundsInRoot()
        assertTrue(header.left >= collapsedFrame.left && header.right <= collapsedFrame.right)
        assertTrue(header.top >= collapsedFrame.top && header.bottom <= collapsedFrame.bottom)
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
        list.performScrollToNode(hasTestTag("settings_palette_choice_9"))
        compose.onNodeWithTag("settings_palette_choice_9").assertContentDescriptionEquals(choice).performClick()
        compose.runOnIdle { assertEquals(9, selected) }
        list.performScrollToNode(hasTestTag("settings_open_palettes_row"))
        compose.onNodeWithTag("settings_open_palettes_row").performClick()
        compose.onNodeWithTag("settings_themes_group").assertExists()
        compose.runOnIdle { assertEquals(9, selected) }
    }

    @Test fun switchingBetweenLiquidAndOtherThemesKeepsTheSelectedCategoryAndTileVisible() {
        var selected by mutableIntStateOf(1)
        var backCalls = 0
        compose.setContent {
            val palette = TrainPilotPalettes.getById(selected)
            TrainPilotTheme(palette = palette) {
                // The JVM fixture exercises actual selection, inset changes and scrolling;
                // Kyant's Android-only bytecode is covered by the release R8/device gate.
                CompositionLocalProvider(
                    com.example.ui.theme.LocalGlassOpticsEnabled provides false,
                    com.example.ui.theme.LocalGlassTopInset provides
                        (when (selected) { 40 -> 132.dp; 35 -> 108.dp; else -> 0.dp })
                ) {
                    SettingsMainScreen(uiState = TrainPilotUiState(selectedPaletteId = selected),
                        strings = TrainPilotStrings.forLanguage(AppLanguage.HU),
                        onBack = { backCalls++ }, onOpenPalettePicker = {},
                        onSelectPalette = { selected = it }, onSelectLanguage = {}, onCycleRestTimer = {},
                        onToggleSoundAlert = {}, onToggleProgressionModal = {}, onToggleVibration = {},
                        onNavigateToSubScreen = {}, onShowBanner = {})
                }
            }
        }
        val list = compose.onNodeWithTag("settings_main_screen")
        list.performScrollToNode(hasTestTag("settings_open_palettes_row"))
        compose.onNodeWithTag("settings_open_palettes_row").performClick()
        list.performScrollToNode(hasTestTag("settings_theme_category_toggle_4"))
        compose.onNodeWithTag("settings_theme_category_toggle_4").performClick()
        list.performScrollToNode(hasTestTag("settings_palette_choice_35"))
        compose.onNodeWithTag("settings_palette_choice_35").performClick()
        compose.onNodeWithTag("settings_palette_choice_35").assertIsSelected().assertIsDisplayed()
        compose.onNodeWithTag("liquid_glass_clarity").assertExists()
        compose.runOnIdle { assertEquals(35, selected); assertEquals(0, backCalls) }
        list.performScrollToNode(hasTestTag("settings_palette_choice_40"))
        compose.onNodeWithTag("settings_palette_choice_40").performClick()
        compose.onNodeWithTag("settings_palette_choice_40").assertIsSelected().assertIsDisplayed()
        compose.runOnIdle { assertEquals(40, selected); assertEquals(0, backCalls) }
        list.performScrollToNode(hasTestTag("settings_theme_category_toggle_1"))
        compose.onNodeWithTag("settings_theme_category_toggle_1").performClick()
        list.performScrollToNode(hasTestTag("settings_palette_choice_9"))
        compose.onNodeWithTag("settings_palette_choice_9").performClick()
        compose.onNodeWithTag("settings_palette_choice_9").assertIsSelected().assertIsDisplayed()
        compose.runOnIdle { assertEquals(9, selected); assertEquals(0, backCalls) }
    }

    @Test fun settingsExposesGoogleCloudLoginAndDriveCalendarControls() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        context.getSharedPreferences("repforge-google", android.content.Context.MODE_PRIVATE).edit().clear().commit()
        compose.setContent {
            TrainPilotTheme {
                SettingsMainScreen(
                    uiState = TrainPilotUiState(),
                    strings = TrainPilotStrings.forLanguage(AppLanguage.HU),
                    onBack = {},
                    onOpenPalettePicker = {},
                    onSelectLanguage = {},
                    onCycleRestTimer = {},
                    onToggleSoundAlert = {},
                    onToggleProgressionModal = {},
                    onToggleVibration = {},
                    onNavigateToSubScreen = {},
                    onShowBanner = {}
                )
            }
        }
        compose.onNodeWithTag("studio_google_cloud_card").assertExists()
        compose.onNodeWithTag("settings_google_login_btn").assertDoesNotExist()
        compose.onNodeWithTag("settings_google_expand").performScrollTo().performClick()
        compose.onNodeWithTag("settings_google_login_btn").assertExists()
        compose.onNodeWithTag("settings_google_expand").performClick()
        compose.onNodeWithTag("settings_google_login_btn").assertDoesNotExist()
    }

    @Test fun weeklyGoalSectionsShowMissingStepDataWithoutOpeningHistory() {
        compose.setContent {
            TrainPilotTheme {
                com.example.ui.screens.HealthTabScreen(
                    uiState = TrainPilotUiState(expandedHealthWeeklyGoals = true),
                    strings = TrainPilotStrings.forLanguage(AppLanguage.HU),
                    onToggleHealthSection = {}, onSelectWeightRange = {}, onToggleAddWeightDialog = {}, onSaveNewWeight = { _, _ -> },
                    onTogglePrimaryHealthSource = {}, onTriggerHealthSync = {}, onNavigateToSubScreen = {},
                    onOpenPalettePicker = {}, onOpenGoalsDialog = {})
            }
        }
        val list = compose.onNodeWithTag("health_tab_screen")
        list.performScrollToNode(hasTestTag("health_weekly_workouts_section"))
        compose.onNodeWithTag("health_weekly_summary_toggle").assertDoesNotExist()
        compose.onNodeWithTag("health_weekly_steps_card").assertDoesNotExist()
        compose.onNodeWithTag("health_weekly_minutes_card").assertDoesNotExist()
        val workouts = compose.onNodeWithTag("health_weekly_workouts_section").getUnclippedBoundsInRoot()
        list.performScrollToNode(hasTestTag("health_weekly_steps_section"))
        val steps = compose.onNodeWithTag("health_weekly_steps_section").getUnclippedBoundsInRoot()
        assertEquals(workouts.left, steps.left)
        assertEquals(workouts.right, steps.right)
        compose.onNodeWithTag("health_weekly_steps_section_summary", useUnmergedTree = true).assertTextEquals("—")
        compose.onNodeWithTag("health_weekly_steps_empty").assertDoesNotExist()
        list.performScrollToNode(hasTestTag("health_weekly_steps_section_toggle"))
        compose.onNodeWithTag("health_weekly_steps_section_toggle").performClick()
        compose.onNodeWithTag("health_weekly_steps_empty", useUnmergedTree = true)
            .assertTextContains("nincs adat", substring = true)
        compose.onNodeWithTag("health_weekly_steps_history").assertDoesNotExist()
    }

    @Test fun privacyPolicyAccuratelyDescribesHealthSourcesAndOpensFromStudioSettingsInAllFourLanguages() {
        for (lang in listOf("hu", "en", "de", "ro")) {
            val policy = com.repforge.app.PrivacyPolicy.text(lang)
            assertTrue("Missing Samsung Health in $lang", policy.contains("Samsung Health"))
            assertTrue("Missing Health Connect in $lang", policy.contains("Health Connect"))
            assertTrue("Missing Wear OS in $lang", policy.contains("Wear OS") || policy.contains("Wear-OS"))
            assertFalse("Stale denial of direct Samsung Health read in $lang",
                policy.contains("nem olvas közvetlenül a Samsung Health alkalmazásból") ||
                    policy.contains("does not read directly from Samsung Health") ||
                    policy.contains("liest nicht direkt aus Samsung Health") ||
                    policy.contains("nu citește direct din aplicația Samsung Health")
            )
        }

        var currentLang by mutableStateOf(AppLanguage.RO)
        compose.setContent {
            TrainPilotTheme {
                SettingsMainScreen(
                    uiState = TrainPilotUiState(language = currentLang),
                    strings = TrainPilotStrings.forLanguage(currentLang),
                    onBack = {},
                    onOpenPalettePicker = {},
                    onSelectLanguage = { currentLang = it },
                    onCycleRestTimer = {},
                    onToggleSoundAlert = {},
                    onToggleProgressionModal = {},
                    onToggleVibration = {},
                    onNavigateToSubScreen = {},
                    onShowBanner = {}
                )
            }
        }
        val list = compose.onNodeWithTag("settings_main_screen")
        list.performScrollToNode(hasTestTag("settings_open_privacy_row"))
        compose.onNodeWithTag("settings_open_privacy_row").performClick()
        compose.onNodeWithTag("settings_privacy_dialog").assertExists()
        compose.onNodeWithText("Samsung Health SDK", substring = true).assertExists()
        compose.onNodeWithText(TrainPilotStrings.forLanguage(currentLang).closeAction).performClick()
        compose.onNodeWithTag("settings_privacy_dialog").assertDoesNotExist()
    }
}
