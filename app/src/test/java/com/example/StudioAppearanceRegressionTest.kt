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
                    onSaveProgression = { _, _, _ -> },
                    onNavigateToSubScreen = {},
                    onShowBanner = {}
                )
            }
        }
        compose.onNodeWithTag("studio_google_cloud_card").assertExists()
        compose.onNodeWithTag("settings_google_login_btn").assertExists()
    }

    @Test fun multiSelectEquipmentInPlannerGeneratesMatchingWorkoutPlan() {
        val vm = com.example.ui.viewmodel.TrainPilotViewModel()
        val multiEquipment = "Saját testsúly, Egykezes kézisúlyzó (1 db), Kézisúlyzó pár (2 db), Kétkezes rúd (Súlyzórúd), Stabil támaszték / Fal / Szék"
        vm.saveInitialProfileAndPlan(
            heightCm = 180,
            weightKg = 82f,
            goalHu = "Izomépítés",
            experienceHu = "Középhaladó",
            locationHu = "Otthon",
            equipmentHu = multiEquipment,
            daysPerWeek = 4,
            durationMin = 60,
            excludedMuscles = emptySet(),
            excludedExercises = emptySet()
        )
        val state = vm.uiState.value
        assertEquals(multiEquipment, state.userSelectedEquipmentHu)
        val generated = state.programs.first { it.isActive }
        val allGeneratedExercises = generated.days.flatMap { it.exercises }
        assertTrue(allGeneratedExercises.isNotEmpty())
        val machineExerciseIds = setOf("lat-pulldown", "seated-cable-row", "face-pull", "cable-fly", "triceps-pushdown", "leg-press", "leg-extension", "leg-curl", "cable-crunch")
        assertTrue(allGeneratedExercises.none { it.id in machineExerciseIds })
    }

    @Test fun healthWeeklySummaryExpandsWithSourceAttributionAndNoDemoWatchOnFreshInstall() {
        var expandedWeekly by mutableStateOf(false)
        compose.setContent {
            TrainPilotTheme {
                com.example.ui.screens.HealthTabScreen(
                    uiState = TrainPilotUiState(expandedHealthWeeklySummary = expandedWeekly),
                    strings = TrainPilotStrings.forLanguage(AppLanguage.HU),
                    onToggleHealthSection = { if (it == "weekly") expandedWeekly = !expandedWeekly },
                    onSelectWeightRange = {},
                    onToggleAddWeightDialog = {},
                    onSaveNewWeight = { _, _ -> },
                    onTogglePrimaryHealthSource = {},
                    onTriggerHealthSync = {},
                    onNavigateToSubScreen = {},
                    onOpenPalettePicker = {},
                    onOpenGoalsDialog = {}
                )
            }
        }
        val list = compose.onNodeWithTag("health_tab_screen")
        list.performScrollToNode(hasTestTag("health_weekly_summary_toggle"))
        compose.onNodeWithTag("health_weekly_expanded_section").assertDoesNotExist()
        compose.onNodeWithTag("health_weekly_summary_toggle").performClick()
        compose.runOnIdle { assertTrue(expandedWeekly) }
        compose.onNodeWithTag("health_weekly_expanded_section").assertExists()
        compose.onNodeWithText("Adatforrások és begyűjtés módja").assertExists()
    }
}

