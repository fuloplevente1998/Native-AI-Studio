package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import com.example.localization.TrainPilotStrings
import com.example.model.AppLanguage
import com.example.model.BodyWeightLogItem
import com.example.ui.screens.HealthTabScreen
import com.example.ui.screens.WeightMeasurementEntry
import com.example.ui.screens.recentWeightTrend
import com.example.ui.components.StudioPrivatePhotos
import com.example.ui.screens.calorieDayLabel
import com.example.ui.screens.todayTileWeekLabel
import com.example.ui.theme.TrainPilotTheme
import com.example.ui.viewmodel.TrainPilotUiState
import com.example.ui.viewmodel.WeeklyHealthDaySummary
import com.repforge.app.ui.progress.ProgressViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

/** 2788: total calories per day, weight edit keeps its ID, today vs weekly split, wording, whole tile titles. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h900dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class Collector2788RegressionTest {
    @get:Rule val compose = createComposeRule()
    private val today = LocalDate.now()
    private val hu = TrainPilotStrings.forLanguage(AppLanguage.HU)
    private val dot = DateTimeFormatter.ofPattern("yyyy. MM. dd.")

    private fun kcalDay(back: Long, active: Int?, total: Int?) = today.minusDays(back).toString().let {
        WeeklyHealthDaySummary(dateIso = it, dayLabelHu = it, steps = 0, activeCaloriesKcal = active ?: 0, sleepLabel = "—",
            workoutMinutes = 0, workoutCount = 0, sourceLabelHu = "", activeCaloriesMeasured = active, totalCaloriesKcal = total)
    }

    private fun plain(s: String) = s.replace(' ', ' ').replace(' ', ' ')

    @Test fun calorieLabelNeverAddsActiveToTotalAndNeverInventsAValue() {
        val d = LocalDate.parse("2026-11-20")
        assertTrue(plain(calorieDayLabel(d, 2180, 177, "hu")).endsWith("teljes 2 180 kcal · ebből aktív 177"))
        assertTrue(plain(calorieDayLabel(d, 2180, null, "hu")).endsWith("teljes 2 180 kcal · aktív: nincs adat"))
        assertTrue(plain(calorieDayLabel(d, null, 177, "en")).endsWith("active 177 kcal · total: no data"))
        assertTrue(calorieDayLabel(d, null, null, "hu").endsWith("nincs adat"))
        assertFalse(plain(calorieDayLabel(d, 2180, 177, "hu")).contains("2 357"))
    }

    @Test fun aDayWithoutWorkoutIsNoWorkoutNotNoData() {
        val d = LocalDate.parse("2026-11-20")
        assertTrue(todayTileWeekLabel("workout", d, null, "hu").endsWith("nincs edzés"))
        assertTrue(todayTileWeekLabel("workout", d, null, "en").endsWith("no workout"))
        assertTrue(todayTileWeekLabel("steps", d, null, "hu").endsWith("nincs adat"))
    }

    @Test fun calorieSheetShowsTotalWithActivePartPerDay() {
        val days = listOf(kcalDay(0, 177, 2180), kcalDay(1, 412, null))
        compose.setContent { TrainPilotTheme {
            HealthTabScreen(TrainPilotUiState(weeklyHealthDays = days), hu, {}, {}, {}, { _, _ -> }, {}, {}, {}, {})
        } }
        compose.onNodeWithTag("health_today_calories").performClick()
        val info = compose.onNodeWithTag("health_today_tile_week_info_calories", useUnmergedTree = true)
        info.assertTextContains("ebből aktív 177", substring = true)
        val yesterday = compose.onNodeWithTag("health_today_tile_week_calories_5")
        val todayBar = compose.onNodeWithTag("health_today_tile_week_calories_6")
        todayBar.assertIsSelected()
        yesterday.assertIsNotSelected().performClick().assertIsSelected()
        todayBar.assertIsNotSelected()
        info.assertTextContains("teljes: nincs adat", substring = true)
    }

    @Test fun allTodayTileCloseButtonsDismissAndTheTilesCanReopen() {
        compose.setContent { TrainPilotTheme {
            HealthTabScreen(TrainPilotUiState(), hu, {}, {}, {}, { _, _ -> }, {}, {}, {}, {})
        } }
        listOf("sleep", "calories", "pulse", "steps", "workout", "recovery").forEach { key ->
            val tile = compose.onNodeWithTag("health_today_$key")
            tile.performClick()
            val sheet = compose.onNodeWithTag("health_today_tile_sheet")
            sheet.assertExists()
            compose.onNodeWithTag("health_today_tile_close").performClick()
            sheet.assertDoesNotExist()
        }
        compose.onNodeWithTag("health_today_sleep").performClick()
        compose.onNodeWithTag("health_today_tile_sheet").assertExists()
    }

    @Test fun todayTileHandsOffToTheJournalAndItsProviderSelectionSurvivesReopening() {
        var state by mutableStateOf(TrainPilotUiState(primaryHealthSourceSamsung = false))
        compose.setContent { TrainPilotTheme {
            HealthTabScreen(state, hu,
                { section -> if (section == "journal") state = state.copy(expandedHealthJournal = !state.expandedHealthJournal) },
                {}, {}, { _, _ -> }, {}, {}, {}, {})
        } }
        val tile = compose.onNodeWithTag("health_today_sleep")
        val tileSheet = compose.onNodeWithTag("health_today_tile_sheet")
        val journalSheet = compose.onNodeWithTag("health_journal_sheet")
        val provider = compose.onNodeWithTag("journal_provider_samsung_health")
        tile.performClick()
        compose.onNodeWithTag("health_expanded_action_button").performScrollTo().performClick()
        tileSheet.assertDoesNotExist()
        journalSheet.assertExists()
        provider.assertIsNotSelected().performClick().assertIsSelected()
        compose.onNodeWithTag("health_journal_close").performClick()
        journalSheet.assertDoesNotExist()
        tileSheet.assertDoesNotExist()
        tile.performClick()
        compose.onNodeWithTag("health_expanded_action_button").performScrollTo().performClick()
        provider.assertIsSelected()
    }

    @Test fun measurementSummaryHidesTheTechnicalOriginButKeepsEnergyAndSteps() {
        val origin = "Health Connect (com.example.provider.internal.long_origin)"
        compose.setContent { TrainPilotTheme {
            HealthTabScreen(TrainPilotUiState(
                dailyHealthDate = today.toString(),
                dailyHealthAvailableFields = setOf("activeCalories", "totalCalories", "steps"),
                dailyHealthSourceLabel = origin,
                activeCaloriesKcal = 177,
                totalCaloriesKcal = 2180,
                dailySteps = 6261
            ), hu, {}, {}, {}, { _, _ -> }, {}, {}, {}, {})
        } }
        compose.onNodeWithTag("health_today_calories").performClick()
        compose.onNodeWithText("Teljes napi energia (nyugalmival): 2180 kcal", substring = true).assertExists()
        compose.onNodeWithText(origin, substring = true).assertDoesNotExist()
        compose.onNodeWithTag("health_today_tile_close").performClick()
        compose.onNodeWithTag("health_today_steps").performClick()
        compose.onNodeWithText("Becsült táv", substring = true).assertExists()
        compose.onNodeWithText(origin, substring = true).assertDoesNotExist()
    }

    @Test fun compactWeightTrendUsesSevenNewestRealMeasurementsInTimeOrder() {
        val entries = (1..9).map { id ->
            val day = today.minusDays(80 - id.toLong())
            WeightMeasurementEntry("$id", "80 kg", 80f, day.format(dot), day)
        }
        assertTrue(recentWeightTrend(emptyList()).isEmpty())
        assertEquals(entries.take(1), recentWeightTrend(entries.take(1)))
        assertEquals(entries.take(2), recentWeightTrend(entries.take(2).reversed()))
        assertEquals(entries.takeLast(7), recentWeightTrend(entries.reversed()))
    }

    @Test fun oldWeightMeasurementsShowInTheCompactChartIndependentlyOfTheExpandedRange() {
        val days = listOf(today.minusDays(80), today.minusDays(50))
        var state by mutableStateOf(TrainPilotUiState(
            selectedWeightRangeIndex = 0,
            bodyWeightLogs = days.mapIndexed { i, d -> BodyWeightLogItem("w$i", 80f + i, d.toString(), d.format(dot)) }
        ))
        compose.setContent { TrainPilotTheme {
            HealthTabScreen(state, hu,
                { section -> if (section == "weight") state = state.copy(expandedHealthWeightLog = !state.expandedHealthWeightLog) },
                { range -> state = state.copy(selectedWeightRangeIndex = range) }, {}, { _, _ -> }, {}, {}, {}, {})
        } }
        val list = compose.onNodeWithTag("health_tab_screen")
        val compact = compose.onNodeWithTag("health_weight_chart_compact_canvas", useUnmergedTree = true)
        list.performScrollToNode(hasTestTag("health_weight_chart_compact"))
        compact.assertExists()
        compose.onNodeWithTag("health_weight_log_header").performClick()
        list.performScrollToNode(hasTestTag("health_weight_range_empty"))
        compose.onNode(
            hasAnyAncestor(hasTestTag("health_weight_range_empty")) and
                hasText("Nincs mérés ebben az időszakban", substring = true),
            useUnmergedTree = true
        ).assertIsDisplayed()
        list.performScrollToNode(hasTestTag("health_weight_log_header"))
        compose.onNodeWithTag("health_weight_log_header").performClick()
        compact.assertExists()
        compose.runOnIdle { state = state.copy(bodyWeightLogs = state.bodyWeightLogs.takeLast(1)) }
        compact.assertExists()
        compose.runOnIdle { state = state.copy(bodyWeightLogs = emptyList(), currentBodyWeightKg = 95f) }
        compact.assertDoesNotExist()
        compose.onNodeWithText("Még nincs rögzített testsúlymérés.").assertExists()
    }

    @Test fun stepsSheetIsAboutTodayAndLinksToTheWeeklyHistory() {
        val toggled = mutableListOf<String>()
        compose.setContent { TrainPilotTheme {
            HealthTabScreen(TrainPilotUiState(dailySteps = 6261, dailyHealthAvailableFields = setOf("steps"),
                dailyHealthDate = today.toString(), dailyHealthSourceLabel = "Samsung Health"),
                hu, { toggled += it }, {}, {}, { _, _ -> }, {}, {}, {}, {})
        } }
        compose.onNodeWithTag("health_today_steps").performClick()
        compose.onNodeWithTag("health_today_tile_week_steps").assertDoesNotExist()
        compose.onNodeWithText("deduplikált", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Mai rekordok").assertDoesNotExist()
        compose.onNodeWithText("1. oldal", substring = true).assertDoesNotExist()
        compose.onNodeWithTag("health_today_tile_open_weekly_steps").performClick()
        compose.runOnIdle { assertEquals(listOf("weekly"), toggled) }
    }

    @Test fun workoutSheetHasNoTechnicalWording() {
        compose.setContent { TrainPilotTheme {
            HealthTabScreen(TrainPilotUiState(), hu, {}, {}, {}, { _, _ -> }, {}, {}, {}, {})
        } }
        compose.onNodeWithTag("health_today_workout").performClick()
        compose.onNodeWithText("Room", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Ma még nincs elmentett edzés.").assertExists()
    }

    @Test fun editingAWeightKeepsItsIdInsteadOfAddingANewEntry() {
        val d = today.minusDays(3)
        val saved = mutableListOf<Pair<Float, String>>()
        val updated = mutableListOf<Triple<String, Float, String>>()
        compose.setContent { TrainPilotTheme {
            HealthTabScreen(TrainPilotUiState(expandedHealthWeightLog = true,
                bodyWeightLogs = listOf(BodyWeightLogItem("w-original", 82f, d.toString(), d.format(dot)))),
                hu, {}, {}, {}, { kg, label -> saved += kg to label }, {}, {}, {}, {},
                onUpdateWeight = { id, kg, label -> updated += Triple(id, kg, label) })
        } }
        val list = compose.onNodeWithTag("health_tab_screen")
        list.performScrollToNode(hasTestTag("health_weight_menu_w-original"))
        compose.onNodeWithTag("health_weight_menu_w-original").performClick()
        compose.onNodeWithTag("health_weight_edit_w-original").performClick()
        compose.onNodeWithTag("health_weight_date_preset_1").performClick() // Tegnap
        compose.onNodeWithTag("health_weight_save").performClick()
        compose.runOnIdle {
            assertTrue("an edit must not create a new entry", saved.isEmpty())
            assertEquals(listOf(Triple("w-original", 82f, today.minusDays(1).format(dot))), updated)
        }
    }

    @Test fun weightMeasurementMenuOffersPhotoAttachment() {
        val d = today.minusDays(1)
        compose.setContent { TrainPilotTheme {
            HealthTabScreen(TrainPilotUiState(expandedHealthWeightLog = true,
                bodyWeightLogs = listOf(BodyWeightLogItem("w-photo", 82f, d.toString(), d.format(dot)))),
                hu, {}, {}, {}, { _, _ -> }, {}, {}, {}, {})
        } }
        val list = compose.onNodeWithTag("health_tab_screen")
        list.performScrollToNode(hasTestTag("health_weight_menu_w-photo"))
        compose.onNodeWithTag("health_weight_menu_w-photo").performClick()
        compose.onNodeWithTag("health_weight_photo_w-photo").performClick()
        compose.onNodeWithTag("health_weight_photo_camera").assertExists()
        compose.onNodeWithTag("health_weight_photo_gallery").assertExists()
        compose.onNodeWithTag("health_weight_photo_close").performClick()
    }

    @Test fun globalPhotoGalleryOffersAnUnattachedOtherPhotoInTheCurrentTheme() {
        val model = ProgressViewModel(ApplicationProvider.getApplicationContext<Application>())
        compose.setContent { TrainPilotTheme {
            StudioPrivatePhotos(TrainPilotUiState(), hu, {}, {}, model)
        } }
        compose.onNodeWithTag("other_photo_attach_card").assertExists()
        compose.onNodeWithTag("other_camera_button").assertExists()
        compose.onNodeWithTag("other_gallery_button").assertExists()
        compose.onNodeWithText("Nem edzéshez vagy méréshez kötött fotó", substring = true).assertExists()
    }

    @Test fun todayTileTitlesAreWholeWords() {
        compose.setContent { TrainPilotTheme {
            HealthTabScreen(TrainPilotUiState(), hu, {}, {}, {}, { _, _ -> }, {}, {}, {}, {})
        } }
        for (key in listOf("calories", "pulse", "recovery", "workout")) {
            val node = compose.onNodeWithTag("health_today_${key}_title", useUnmergedTree = true)
            val layouts = mutableListOf<TextLayoutResult>()
            node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            val layout = layouts.single()
            assertFalse("$key title is cut", layout.didOverflowWidth || layout.hasVisualOverflow)
            assertEquals("$key title has every character", layout.layoutInput.text.length,
                layout.getLineEnd(layout.lineCount - 1, visibleEnd = true))
        }
    }
}
