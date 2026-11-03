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
        compose.onNodeWithTag("progress_range_0").performClick()
        val bar = compose.onNodeWithTag("progress_compact_bar_0", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val card = compose.onNodeWithTag("progress_total_volume_card").getUnclippedBoundsInRoot()
        assertTrue((bar.right - bar.left) < (card.right - card.left) / 4)
        for ((index, total) in listOf(100, 300, 700, 1500).withIndex()) {
            compose.onNodeWithTag("progress_range_$index").performClick()
            compose.onNodeWithTag("progress_volume_total", useUnmergedTree = true).assertTextEquals("${if (total == 1500) "1 500" else total} kg")
        }
        compose.onNodeWithTag("progress_volume_expand").performClick()
        compose.onNodeWithTag("progress_volume_bucket_unit").assertTextEquals("Heti bontás")
        compose.onNodeWithTag("progress_range_0").performClick()
        compose.onNodeWithTag("progress_volume_bucket_unit").assertTextEquals("Napi bontás")
        compose.onNodeWithTag("progress_volume_total", useUnmergedTree = true).assertTextEquals("100 kg")
        compose.onNodeWithTag("progress_range_2").performClick()
        compose.onNodeWithTag("progress_volume_bucket_unit").assertTextEquals("Heti bontás")
    }
    @Test fun threeMonthChartOpensAtLatestAndManualScrollIsPreservedOnSelection() {
        var selected by mutableStateOf(-1)
        val period = StudioProgressWindow(90, LocalDate.of(2026, 11, 2))
        val bars = period.volumes(listOf(row(0, 100))).mapIndexed { index, pair -> pair.first to (100 + index) }
        compose.setContent { TrainPilotTheme {
            Box(Modifier.width(300.dp)) {
                com.example.ui.screens.StudioVolumeChart(bars, period, selected, false) { selected = it }
            }
        } }
        fun scrollRange() = compose.onNodeWithTag("progress_volume_scroll_compact").fetchSemanticsNode()
            .config[androidx.compose.ui.semantics.SemanticsProperties.HorizontalScrollAxisRange]
        val range = scrollRange()
        compose.runOnIdle {
            assertTrue(range.maxValue() > 0f)
            assertEquals(range.maxValue(), range.value(), 1f)
        }
        compose.onNodeWithTag("progress_compact_bar_0", useUnmergedTree = true).performScrollTo()
        compose.onNodeWithTag("progress_compact_bar_0", useUnmergedTree = true).performTouchInput { click() }
        val afterSelection = scrollRange()
        compose.runOnIdle { assertEquals(0, selected); assertTrue(afterSelection.value() < afterSelection.maxValue()) }
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
    @Test fun sixMuscleLabelsRemainOneLineAndAdviceHasCompactRoom() {
        val labels = listOf(
            "chest" to "Mellizom", "back" to "Hátizom", "shoulders" to "Vállizom",
            "arms" to "Karizmok", "core" to "Törzs/Has", "legs" to "Lábizmok"
        )
        val loads = labels.map { (id, label) ->
            MuscleLoadItem(id, label, "3 sorozat", 60, 0.6f, "Közepes", emptyList(), "")
        }
        compose.setContent { TrainPilotTheme {
            Box(Modifier.width(360.dp)) { AnatomicalMuscleHeatmapCard(realMuscleLoads = loads) }
        } }
        val card = compose.onNodeWithTag("anatomical_muscle_heatmap_card").getUnclippedBoundsInRoot()
        assertTrue("Six labels must not inflate the progress card", card.bottom - card.top < 290.dp)
        val back = compose.onNodeWithText("Hátizom", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue("Hátizom should fit without wrapping", back.bottom - back.top < 24.dp)
        compose.onNodeWithTag("muscle_explanation_text").assertDoesNotExist()
        compose.onNodeWithTag("muscle_explanation_toggle").performClick()
        compose.onNodeWithTag("muscle_explanation_text").assertExists()
    }

    @Test fun musclePeriodVisualShrinks15PercentButKeeps48dpTouchTarget() {
        var selected = 90
        compose.setContent { TrainPilotTheme {
            Box(Modifier.width(360.dp)) {
                AnatomicalMuscleHeatmapCard(periodDays = selected, onPeriodSelected = { selected = it })
            }
        } }
        val hit = compose.onNodeWithTag("muscle_heatmap_period_pill").getUnclippedBoundsInRoot()
        val visual = compose.onNodeWithTag("muscle_heatmap_period_visual", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertEquals(48.dp, hit.bottom - hit.top)
        assertEquals(48.dp, hit.right - hit.left)
        assertEquals(41.dp, visual.bottom - visual.top)
        assertEquals(41.dp, visual.right - visual.left)
        compose.onNodeWithTag("muscle_heatmap_period_pill").performClick()
        compose.runOnIdle { assertEquals(30, selected) }
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
    @Test fun healthTabCanMoveRefreshAndTotalEnergyWithoutBreakingSharedHomeStatus() {
        val state = TrainPilotUiState(primaryHealthSourceSamsung = true,
            dailyHealthDate = LocalDate.now().toString(), dailyHealthSourceLabel = "Samsung Health",
            dailyHealthAvailableFields = setOf("steps", "activeCalories", "totalCalories"),
            dailySteps = 5000, activeCaloriesKcal = 220, totalCaloriesKcal = 1460)
        compose.setContent { TrainPilotTheme { StudioDailyHealthStatus(state,
            showRefreshButton = false, showTotalEnergy = false) } }
        compose.onNodeWithTag("daily_health_values").assertTextContains("Aktív energia: 220 kcal", substring = true)
        compose.onNodeWithTag("daily_health_sync").assertDoesNotExist()
        compose.onNodeWithText("Összes energia (nyugalmival): 1460 kcal").assertDoesNotExist()
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
    @Test fun statisticsReplaceStrengthAndAreNotAppendedToTheProgressPage() {
        var state by mutableStateOf(TrainPilotUiState(savedRecordsLoading = false,
            savedExerciseBests = listOf(com.repforge.app.SavedExerciseBest("row", "Evezés", "Row", "ism.", "reps", 12.5, null, null, null))))
        compose.setContent {
            TrainPilotTheme {
                com.example.ui.screens.ProgressTabScreen(state, TrainPilotStrings.forLanguage(AppLanguage.HU),
                    onSelectMainSection = { state = state.copy(progressMainSection = it) }, onSelectExercise = {},
                    onSelectRange = {}, onSelectVolumeBar = {}, onSelectExpandedStatsCard = {},
                    onSelectStatsSubTab = { state = state.copy(statsSubTab = it) }, onToggleCardioFilterStepsMode = {},
                    onOpenWorkoutDetail = {}, onOpenSettings = {}, onOpenPalettePicker = {})
            }
        }
        compose.onNodeWithTag("statistics_subtab_0").assertDoesNotExist()
        compose.onNodeWithText("📊 Statisztikák").performClick()
        compose.onNodeWithTag("statistics_records_title").assertDoesNotExist()
        compose.onNodeWithTag("statistics_subtab_0").assertExists()
        compose.onNodeWithTag("statistics_record_row").assertExists()
        compose.onNodeWithTag("statistics_subtab_1").performClick()
        compose.onNodeWithTag("statistics_daily_activity").assertExists()
        compose.onAllNodes(hasText(TrainPilotStrings.forLanguage(AppLanguage.HU).strength1RmTab, substring = true)).assertCountEquals(0)
    }
    @Test fun calendarDayActionsOpenAsAnOverlayAndKeepUndoAndPlanningAccessible() {
        val adds = mutableListOf<String>()
        var undos = 0
        compose.setContent {
            TrainPilotTheme {
                Box(Modifier.width(320.dp)) {
                    com.example.ui.screens.CalendarPlannerScreen(
                        state = TrainPilotUiState(selectedCalendarDay = 2, calendarEvents = emptyList(), calendarUndoStack = listOf(emptyList())),
                        onBack = {}, onOpenSettings = {}, onSelectDay = {}, onOpenReplanModal = {},
                        onApplyReplan = { _, _ -> }, onOpenWorkoutDetail = {},
                        onAddPlannedWorkoutOnDay = { day, split, _, _ -> assertEquals(5, day); adds += split },
                        onUndoCalendarAction = { undos++ })
                }
            }
        }
        compose.onNodeWithTag("calendar_day_dialog").assertDoesNotExist()
        compose.onNodeWithTag("calendar_day_actions").assertDoesNotExist()
        compose.onNodeWithTag("calendar_undo_action").performClick()
        assertEquals(1, undos)
        compose.onNodeWithTag("calendar_day_5").performClick()
        compose.onNodeWithTag("calendar_day_dialog").assertExists()
        compose.onNodeWithTag("calendar_empty_day").assertExists()
        val bounds = compose.onNodeWithTag("calendar_day_actions").getUnclippedBoundsInRoot()
        assertTrue((bounds.bottom - bounds.top) < 260.dp)
        compose.onAllNodesWithTag("calendar_undo_action").assertCountEquals(1)
        compose.onNodeWithTag("calendar_add_A").performClick()
        compose.onNodeWithTag("calendar_add_B").performClick()
        compose.onNodeWithTag("calendar_day_dialog_close").performClick()
        compose.onNodeWithTag("calendar_day_dialog").assertDoesNotExist()
        compose.onNodeWithTag("calendar_planning_open").performClick()
        compose.onNodeWithTag("calendar_planning_dialog").assertExists()
        compose.onNodeWithTag("calendar_planning_dialog_close").performClick()
        assertEquals(listOf("A", "B"), adds); assertEquals(1, undos)
    }

    @Test fun volumePercentageActuallyUpdatesForIncreaseAndDecrease() {
        var state by mutableStateOf(TrainPilotUiState(selectedProgressRangeIndex = 1,
            loggedWorkouts = listOf(row(0, 1500), row(7, 1000))))
        compose.setContent { TrainPilotTheme {
            Column(Modifier.width(360.dp).verticalScroll(rememberScrollState())) {
                MainProgressVolumeAndMuscleSection(state, {}, {}, {}, {})
            }
        } }
        compose.onNodeWithTag("progress_volume_change", useUnmergedTree = true).assertTextEquals("▲ +50%")
        compose.runOnIdle { state = state.copy(loggedWorkouts = listOf(row(0, 500), row(7, 1000))) }
        compose.onNodeWithTag("progress_volume_change", useUnmergedTree = true).assertTextEquals("▼ -50%")
    }

}
