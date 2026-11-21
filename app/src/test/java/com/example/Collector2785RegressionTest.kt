package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.localization.TrainPilotStrings
import com.example.model.AppLanguage
import com.example.model.BodyWeightLogItem
import com.example.ui.components.TrainPilotFilterChip
import com.example.ui.components.TrainPilotPrimaryButton
import com.example.ui.components.TrainPilotSecondaryButton
import com.example.ui.components.TrainPilotTextField
import com.example.ui.components.chartNearestByFraction
import com.example.ui.components.chartNearestIndex
import com.example.ui.screens.HealthTabScreen
import com.example.ui.screens.StudioStatisticsContent
import com.example.ui.screens.WeightMeasurementEntry
import com.example.ui.screens.pulseTimeFractions
import com.example.ui.screens.todayTileWeekLabel
import com.example.ui.screens.weightMeasurementsInRange
import com.example.ui.theme.TrainPilotPalette
import com.example.ui.theme.TrainPilotPalettes
import com.example.ui.theme.TrainPilotTheme
import com.example.ui.viewmodel.TrainPilotUiState
import com.example.ui.viewmodel.WeeklyHealthDaySummary
import com.repforge.app.NativeDailyHealthDetails
import com.repforge.app.SavedExerciseBest
import com.repforge.app.ui.navigation.NativePlanningShortcuts
import com.repforge.app.ui.progress.Exercise1RmPointUiModel
import com.repforge.app.ui.progress.ProgressUiState
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

/** 2785: tappable charts, period-only weight data, app-style themed controls, weight form, wiring. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w400dp-h900dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class Collector2785RegressionTest {
    @get:Rule val compose = createComposeRule()
    private val today = LocalDate.now()
    private val dot = DateTimeFormatter.ofPattern("yyyy. MM. dd.")

    // ---- 1. charts: nearest-point selection and info labels ----

    @Test fun chartTapPicksTheNearestPoint() {
        assertEquals(0, chartNearestIndex(0f, 300f, 5))
        assertEquals(4, chartNearestIndex(300f, 300f, 5))
        assertEquals(2, chartNearestIndex(160f, 300f, 5))
        assertEquals(4, chartNearestIndex(999f, 300f, 5))
        assertEquals(0, chartNearestIndex(50f, 300f, 1))
        assertEquals(1, chartNearestByFraction(100f, 300f, listOf(0f, 0.3f, 1f)))
        assertEquals(2, chartNearestByFraction(290f, 300f, listOf(0f, 0.3f, 1f)))
        val readings = listOf("2026-11-18T00:00:00Z", "2026-11-18T06:00:00Z", "2026-11-19T00:00:00Z")
            .map { NativeDailyHealthDetails.PulsePoint(it, 60, "hc") }
        assertEquals(listOf(0f, 0.25f, 1f), pulseTimeFractions(readings))
    }

    @Test fun weekBarLabelNamesDayValueAndUnitAndNeverInventsZero() {
        val day = LocalDate.parse("2026-11-18")
        val steps = todayTileWeekLabel("steps", day, 8432.0, "hu").replace(' ', ' ').replace(' ', ' ')
        assertTrue(steps, steps.endsWith("8 432 lépés"))
        assertTrue(todayTileWeekLabel("workout", day, 45.0, "en").endsWith("45 min"))
        assertTrue(todayTileWeekLabel("pulse", day, 61.6, "de").endsWith("62 bpm"))
        val missing = todayTileWeekLabel("calories", day, null, "hu")
        assertTrue(missing, missing.endsWith("nincs adat") && "0" !in missing.substringAfter("·"))
    }

    @Test fun todayTileBarsAreTappableAndTheInfoLineFollowsTheSelection() {
        // 2788: the 7-day workout history lives in Weekly goals (with day names); the tile sheet links there.
        val days = listOf(6 to 30, 0 to 45).map { (back, minutes) ->
            val d = today.minusDays(back.toLong()).toString()
            WeeklyHealthDaySummary(dateIso = d, dayLabelHu = d, steps = 0, activeCaloriesKcal = 0, sleepLabel = "—",
                workoutMinutes = minutes, workoutCount = 1, sourceLabelHu = "")
        }
        val toggled = mutableListOf<String>()
        compose.setContent { TrainPilotTheme {
            HealthTabScreen(TrainPilotUiState(weeklyHealthDays = days), TrainPilotStrings.forLanguage(AppLanguage.HU),
                { toggled += it }, {}, {}, { _, _ -> }, {}, {}, {}, {})
        } }
        compose.onNodeWithTag("health_today_workout").performClick()
        compose.onNodeWithTag("health_today_tile_week_workout").assertDoesNotExist()
        compose.onNodeWithTag("health_today_tile_open_weekly_workout").performClick()
        compose.onNodeWithTag("health_today_tile_sheet").assertDoesNotExist()
        compose.runOnIdle { assertEquals(listOf("weekly"), toggled) }
    }

    @Test fun exerciseChartTapSelectsAnEarlierPoint() {
        compose.setContent { TrainPilotTheme {
            StudioStatisticsContent(
                state = TrainPilotUiState(savedRecordsLoading = false, savedExerciseBests = listOf(
                    SavedExerciseBest("pullover", "Pullover", "Pullover", "reps", "weight", 5.5, 15, null, null))),
                native = ProgressUiState(selectedExerciseId = "pullover", strengthLoading = false,
                    exercise1RmTrend = listOf(
                        Exercise1RmPointUiModel("2026-11-01", 5.0, 10, 6.67),
                        Exercise1RmPointUiModel("2026-11-03", 5.5, 15, 8.25))),
                onSelectTab = {}, onStepsMode = {}, onExercise = {}, onActivity = {},
                onRetry = {}, onRefreshRecords = {})
        } }
        compose.onNodeWithText("Pullover").performClick()
        val info = compose.onNodeWithTag("statistics_record_chart_info_pullover", useUnmergedTree = true)
        val chart = compose.onNodeWithTag("statistics_record_chart_pullover", useUnmergedTree = true)
        val stateDescription = androidx.compose.ui.semantics.SemanticsProperties.StateDescription
        info.performScrollTo().assertTextContains("2026-11-03", substring = true)
        chart.assert(SemanticsMatcher("latest selection") { it.config[stateDescription].startsWith("2026-11-03") })
        chart.performTouchInput { click(Offset(2f, centerY)) }
        info.assertTextContains("2026-11-01", substring = true)
        chart.assert(SemanticsMatcher("earlier selection") { it.config[stateDescription].startsWith("2026-11-01") })
        chart.performTouchInput { click(Offset(right - 2f, centerY)) }
        info.assertTextContains("2026-11-03", substring = true)
    }

    // ---- 2. weight periods show only their own measurements ----

    private fun entry(id: String, daysBack: Long, kg: Float) = today.minusDays(daysBack).let {
        WeightMeasurementEntry(id, "$kg kg", kg, it.format(dot), it)
    }

    @Test fun weightPeriodsNeverFallBackToTheFullList() {
        val all = listOf(entry("a", 40, 82f), entry("b", 200, 85f), entry("c", 2, 80f))
        assertEquals(listOf("c"), weightMeasurementsInRange(all, 0, today).map { it.id })
        assertEquals(listOf("c"), weightMeasurementsInRange(all, 1, today).map { it.id })
        assertEquals(listOf("a", "c"), weightMeasurementsInRange(all, 2, today).map { it.id })
        assertEquals(listOf("b", "a", "c"), weightMeasurementsInRange(all, 4, today).map { it.id })
        assertTrue(weightMeasurementsInRange(listOf(entry("old", 50, 82f)), 0, today).isEmpty())
    }

    private fun weightState(range: Int) = TrainPilotUiState(expandedHealthWeightLog = true, selectedWeightRangeIndex = range,
        bodyWeightLogs = listOf(40L to 82f, 60L to 84f).mapIndexed { i, (back, kg) ->
            val d = today.minusDays(back)
            BodyWeightLogItem("w$i", kg, d.toString(), d.format(dot))
        })

    @Test fun emptyWeightPeriodSaysSoAndNamesTheLatestEntry() {
        var state by mutableStateOf(weightState(0))
        compose.setContent { TrainPilotTheme {
            HealthTabScreen(state, TrainPilotStrings.forLanguage(AppLanguage.HU), {}, {}, {}, { _, _ -> }, {}, {}, {}, {})
        } }
        val list = compose.onNodeWithTag("health_tab_screen", useUnmergedTree = true)
        list.performScrollToNode(hasTestTag("health_weight_range_empty"))
        compose.onNodeWithText("Nincs mérés ebben az időszakban · utolsó: ${today.minusDays(40).format(dot)}", substring = true).assertExists()
        compose.onNodeWithTag("health_weight_chart_info", useUnmergedTree = true).assertDoesNotExist()
        compose.runOnIdle { state = weightState(4) }
        list.performScrollToNode(hasTestTag("health_weight_chart_info"))
        compose.onNodeWithTag("health_weight_range_empty", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("health_weight_chart_info", useUnmergedTree = true).assertTextContains(today.minusDays(40).format(dot), substring = true)
        compose.onNodeWithTag("health_weight_chart_canvas", useUnmergedTree = true).performTouchInput { click(Offset(1f, centerY)) }
        compose.onNodeWithTag("health_weight_chart_info", useUnmergedTree = true).assertTextContains(today.minusDays(60).format(dot), substring = true)
    }

    // ---- 4. weight entry form: calendar, quick days, Cancel/Save ----

    @Test fun weightFormQuickDayAndCalendarSaveTheChosenDate() {
        val saved = mutableListOf<Pair<Float, String>>()
        compose.setContent { TrainPilotTheme {
            HealthTabScreen(TrainPilotUiState(showAddWeightDialog = true, currentBodyWeightKg = 80f),
                TrainPilotStrings.forLanguage(AppLanguage.HU), {}, {}, {}, { kg, label -> saved += kg to label }, {}, {}, {}, {})
        } }
        compose.onNodeWithTag("health_weight_date_preset_0").assertIsSelected()
        compose.onNodeWithTag("health_weight_date_preset_1").performClick().assertIsSelected()
        compose.onNodeWithTag("health_weight_date_field").performClick()
        compose.onNodeWithTag("health_date_picker").assertExists()
        compose.onNodeWithTag("health_date_cancel").performScrollTo().performClick()
        compose.onNodeWithTag("health_weight_save").performClick()
        compose.runOnIdle { assertEquals(listOf(80f to today.minusDays(1).format(dot)), saved) }
    }

    // ---- 6. new controls follow the theme (dark, light, contrast) ----

    private fun close(expected: Color, actual: Color) =
        listOf(expected.red to actual.red, expected.green to actual.green, expected.blue to actual.blue)
            .all { (e, a) -> abs(e - a) < 0.03f }

    private fun paletteCheck(palette: TrainPilotPalette) {
        var host: View? = null
        compose.setContent {
            TrainPilotTheme(palette = palette) {
                val view = LocalView.current
                SideEffect { host = view }
                Column(Modifier.fillMaxSize().background(palette.background).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TrainPilotFilterChip(selected = false, onClick = {}, label = { Spacer(Modifier.width(80.dp)) },
                        modifier = Modifier.testTag("theme_chip"))
                    TrainPilotTextField(value = "", onValueChange = {}, modifier = Modifier.width(220.dp).testTag("theme_field"))
                    TrainPilotPrimaryButton("", onClick = {}, modifier = Modifier.width(160.dp).testTag("theme_primary"))
                    TrainPilotSecondaryButton("", onClick = {}, modifier = Modifier.width(160.dp).testTag("theme_secondary"))
                }
            }
        }
        val pixels = compose.runOnIdle {
            val view = requireNotNull(host)
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            bitmap.asImageBitmap().toPixelMap()
        }
        fun center(tag: String): Color {
            val b = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
            return pixels[b.center.x.roundToInt(), b.center.y.roundToInt()]
        }
        val name = palette.key
        assertTrue("$name chip uses the card colour", close(palette.surfaceCard, center("theme_chip")))
        assertTrue("$name field uses the card colour", close(palette.surfaceCard, center("theme_field")))
        assertTrue("$name save uses the accent", close(palette.primary, center("theme_primary")))
        assertTrue("$name cancel uses the elevated surface", close(palette.surfaceElevated, center("theme_secondary")))
        if (palette.isDark) assertFalse("$name: no white control on a dark theme", close(Color.White, center("theme_chip")))
    }

    @Test fun darkThemeControls() = paletteCheck(TrainPilotPalettes.allPalettes.first { it.id == 1 })
    @Test fun contrastThemeControls() = paletteCheck(TrainPilotPalettes.allPalettes.first { it.id == 9 })
    @Test fun lightThemeControls() = paletteCheck(TrainPilotPalettes.allPalettes.first { !it.isDark })

    // ---- 5. wiring of entry points ----

    @Test fun planningShortcutsReachTheirDestinations() {
        val opened = mutableListOf<String>()
        compose.setContent { TrainPilotTheme {
            NativePlanningShortcuts("hu", enabled = true, onPrograms = { opened += "programs" },
                onCalendar = { opened += "calendar" }, onCoach = { opened += "coach" })
        } }
        listOf("programs", "calendar", "coach").forEach { compose.onNodeWithTag("shortcut_$it").performClick() }
        compose.runOnIdle { assertEquals(listOf("programs", "calendar", "coach"), opened) }
    }

    @Test fun noVisibleControlIsBoundToAnEmptyAction() {
        val root = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
            .map { File(it, "app/src/main/java") }.firstOrNull { it.isDirectory } ?: error("Phone sources required")
        val empty = Regex("""\b(onClick|on[A-Z]\w*)\s*=\s*\{\s*}""")
        // Not buttons: a read-only field's value callback, a hidden top-bar slot, the onboarding back guard.
        val allowed = setOf("onValueChange", "onOpenSettings", "onBack")
        val found = root.walkTopDown().filter { it.extension == "kt" }.flatMap { file ->
            file.readLines().mapIndexedNotNull { i, line ->
                empty.findAll(line).map { it.groupValues[1] }.firstOrNull { it !in allowed }
                    ?.let { "${file.relativeTo(root).invariantSeparatorsPath}:${i + 1} $it" }
            }
        }.toList()
        assertTrue("Wire every visible control to a real action: $found", found.isEmpty())
    }
}
