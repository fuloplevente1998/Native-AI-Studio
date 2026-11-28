package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.localization.TrainPilotStrings
import com.example.model.AppLanguage
import com.example.model.LoggedWorkoutEntry
import com.example.ui.components.QuickWorkoutHubSheet
import com.example.ui.screens.LogWorkoutDetailScreen
import com.example.ui.screens.WeightLogBody
import com.example.ui.theme.*
import com.example.ui.viewmodel.TrainPilotUiState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w393dp-h900dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class DeviceSheetFeedbackRegressionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun realWeightLogUsesOneDropAndUpdatesSelectedRangeOnlyInLiquid() {
        var state by mutableStateOf(TrainPilotUiState(selectedWeightRangeIndex = 0))
        var liquid by mutableStateOf(true)
        compose.setContent {
            TrainPilotTheme(TrainPilotPalettes.getById(if (liquid) 34 else 1)) {
                Column {
                    WeightLogBody(state, true, TrainPilotStrings.forLanguage(AppLanguage.HU),
                        emptyList(), emptyList(), java.time.format.DateTimeFormatter.ISO_LOCAL_DATE,
                        {}, { state = state.copy(selectedWeightRangeIndex = it) }, {}, {}, {}, {})
                }
            }
        }
        compose.onNodeWithTag("health_weight_range_0").assertIsSelected()
        compose.onNodeWithTag("health_weight_range_2").performScrollTo().performClick().assertIsSelected()
        compose.onAllNodesWithTag("glass_selection_pill", useUnmergedTree = true).assertCountEquals(1)
        compose.runOnIdle { assertEquals(2, state.selectedWeightRangeIndex); liquid = false }
        compose.onNodeWithTag("health_weight_range_track").assertDoesNotExist()
        compose.onNodeWithTag("health_weight_range_1").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, state.selectedWeightRangeIndex) }
    }

    @Test fun actualJournalDetailTabsMoveOneSelectionAndKeepLegacyLayout() {
        val workout = LoggedWorkoutEntry("device-test", "2026-11-25 22:57", "Otthoni A/B", 45,
            "44:29", "", 0, 0, "")
        var state by mutableStateOf(TrainPilotUiState(loggedWorkouts = listOf(workout),
            selectedLogWorkoutId = workout.id, logDetailTabIndex = 0))
        var liquid by mutableStateOf(true)
        compose.setContent {
            TrainPilotTheme(TrainPilotPalettes.getById(if (liquid) 34 else 1)) {
                CompositionLocalProvider(LocalGlassOverlay provides liquid) {
                    LogWorkoutDetailScreen(state, TrainPilotStrings.forLanguage(AppLanguage.HU),
                        {}, {}, { state = state.copy(logDetailTabIndex = it) }, {}, {}, {}, {}, {})
                }
            }
        }
        compose.onNodeWithTag("log_detail_tab_0").assertIsSelected()
        compose.onNodeWithTag("log_detail_tab_2").performClick().assertIsSelected()
        compose.onNodeWithTag("log_detail_tab_0").assertIsNotSelected()
        compose.onAllNodesWithTag("glass_selection_pill", useUnmergedTree = true).assertCountEquals(1)
        compose.runOnIdle { assertEquals(2, state.logDetailTabIndex); liquid = false }
        compose.onNodeWithTag("log_detail_tab_track").assertDoesNotExist()
        compose.onNodeWithTag("log_detail_tab_1").performClick()
        compose.runOnIdle { assertEquals(1, state.logDetailTabIndex) }
    }

    @Test fun quickWorkoutSheetKeepsSameViewportWhenSwitchingAllThreeModes() {
        var tab by mutableIntStateOf(0)
        var started: String? = null
        compose.setContent {
            TrainPilotTheme(TrainPilotPalettes.getById(34)) {
                QuickWorkoutHubSheet(AppLanguage.HU, tab, { tab = it }, {}, { started = it },
                    strengthContent = { Box(Modifier.fillMaxWidth().height(100.dp)) },
                    timedContent = { Box(Modifier.fillMaxWidth().height(240.dp)) })
            }
        }
        val original = compose.onNodeWithTag("quick_workout_body").getUnclippedBoundsInRoot()
        assertTrue(original.bottom - original.top > 300.dp)
        for (index in listOf(1, 2, 0, 2)) {
            compose.onNodeWithTag("quick_tab_$index").performClick().assertIsSelected()
            val bounds = compose.onNodeWithTag("quick_workout_body").getUnclippedBoundsInRoot()
            assertEquals(original.top, bounds.top)
            assertEquals(original.bottom, bounds.bottom)
        }
        compose.onNodeWithTag("quick_outdoor_start").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("Kültéri futás", started) }
    }
}
