package com.example

import android.os.Looper
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.localization.TrainPilotStrings
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.screens.MainProgressVolumeAndMuscleSection
import com.example.ui.theme.TrainPilotTheme
import com.example.ui.viewmodel.TrainPilotUiState
import com.repforge.app.HealthJournalStore
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w480dp-h1200dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class StudioProgressUiRegressionTest {
    @get:Rule val compose = createComposeRule()
    private fun row(days: Long, kg: Int) = LoggedWorkoutEntry("$days", "", "Workout", 20, "", "", 1, kg, "",
        savedStartIso = LocalDate.now().minusDays(days).atTime(0, 10).atZone(ZoneId.systemDefault()).toInstant().toString())

    @Test fun periodButtonsChangeTheActualTotalAndDailyVersusWeeklyGraph() {
        var state by mutableStateOf(TrainPilotUiState(loggedWorkouts = listOf(row(0, 100), row(3, 200), row(20, 400), row(60, 800))))
        compose.setContent {
            TrainPilotTheme {
                Column(Modifier.width(360.dp).verticalScroll(rememberScrollState())) {
                    MainProgressVolumeAndMuscleSection(state,
                        onSelectRange = { state = state.copy(selectedProgressRangeIndex = it) },
                        onSelectVolumeBar = {}, onSelectExpandedStatsCard = {}, onOpenWorkoutDetail = {})
                }
            }
        }
        for ((index, total) in listOf(100, 300, 700, 1500).withIndex()) {
            compose.onNodeWithTag("progress_range_$index").performClick()
            compose.onNodeWithTag("progress_volume_total", useUnmergedTree = true).assertTextEquals("${if (total == 1500) "1 500" else total} kg")
        }
        compose.onNodeWithTag("progress_volume_expand").performClick()
        compose.onNodeWithTag("progress_volume_bucket_unit").assertTextEquals("Heti bontás")
        compose.onNodeWithTag("progress_range_0").performClick()
        compose.onNodeWithTag("progress_volume_bucket_unit").assertTextEquals("Napi bontás")
        compose.onNodeWithTag("progress_volume_total", useUnmergedTree = true).assertTextEquals("100 kg")
    }
    @Test fun bodyLayersFollowWorkoutLoadsAndSmallMeasuredLoadsAreNotRoundedAway() {
        var loads by mutableStateOf(listOf(MuscleLoadItem("chest", "Mell", "1 sorozat", 0, 0.005f, "Kisebb", emptyList(), "", setsCount = 1)))
        compose.setContent { TrainPilotTheme { AnatomicalMuscleHeatmapCard(realMuscleLoads = loads, periodDays = null) } }
        compose.waitUntil(30_000) {
            shadowOf(Looper.getMainLooper()).idle()
            compose.onAllNodesWithTag("progress_anatomy_layer_chest", useUnmergedTree = true).fetchSemanticsNodes().size == 1
        }
        compose.onNodeWithTag("muscle_heatmap_period_pill").assertDoesNotExist()
        compose.runOnIdle { loads = listOf(MuscleLoadItem("legs", "Láb", "3 sorozat", 100, 1f, "Nagyobb", emptyList(), "", setsCount = 3)) }
        compose.onNodeWithTag("progress_anatomy_layer_chest", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("progress_anatomy_layer_legs", useUnmergedTree = true).assertExists()
    }
    @Test fun dailyStatusUsesActualProviderAndDistinguishesUnknownFromZeroAndFailure() {
        var requested = ""
        var state by mutableStateOf(TrainPilotUiState(primaryHealthSourceSamsung = true, dailyHealthDate = LocalDate.now().toString(),
            dailyHealthSourceLabel = "Samsung Health", dailyHealthAvailableFields = setOf("steps", "activeCalories", "totalCalories"),
            dailySteps = 7000, activeCaloriesKcal = 1000, totalCaloriesKcal = 1500))
        compose.setContent {
            CompositionLocalProvider(LocalSyncDailyHealth provides { requested = it }) {
                TrainPilotTheme { StudioDailyHealthStatus(state) }
            }
        }
        compose.onNodeWithTag("daily_health_values").assertTextEquals("Napi lépés: 7000 • Aktív energia: 1000 kcal")
        compose.onNodeWithText("Összes energia (nyugalmival): 1500 kcal").assertExists()
        compose.onNodeWithTag("daily_health_sync").performClick()
        assertEquals(HealthJournalStore.SAMSUNG, requested)
        compose.runOnIdle { state = state.copy(dailyHealthAvailableFields = emptySet(), healthSyncFailed = true, healthSyncMessage = "Engedély visszavonva") }
        compose.onNodeWithTag("daily_health_values").assertTextContains("még nincs lekérve", substring = true)
        compose.onNodeWithTag("daily_health_sync_status").assertTextEquals("Engedély visszavonva")
        compose.runOnIdle { state = state.copy(dailyHealthDate = LocalDate.now().minusDays(1).toString(),
            dailyHealthAvailableFields = setOf("steps"), dailySteps = 7000) }
        compose.onNodeWithTag("daily_health_values").assertTextContains("még nincs lekérve", substring = true)
    }
    @Test fun stickyHandoverUsesRealDirectionAndDisablesPendingCommands() {
        var calls = 0
        var state by mutableStateOf(TrainPilotUiState(isWorkoutSessionActive = true, watchAppAvailable = true))
        compose.setContent { TrainPilotTheme { StudioWorkoutHandoverBar(state, TrainPilotStrings.forLanguage(AppLanguage.HU)) { calls++ } } }
        compose.onNodeWithTag("workout_handover_action").performClick()
        assertEquals(1, calls)
        compose.runOnIdle { state = state.copy(watchControlPending = true) }
        compose.onNodeWithTag("workout_handover_action").assertIsNotEnabled()
        compose.runOnIdle { state = state.copy(watchControlPending = false, phoneControlsWorkout = false, watchAppAvailable = false) }
        compose.onNodeWithTag("workout_handover_action").assertIsEnabled().performClick()
        assertEquals(2, calls)
    }
}
