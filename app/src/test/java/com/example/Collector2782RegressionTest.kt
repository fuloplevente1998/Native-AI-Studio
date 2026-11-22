package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.model.ExerciseSetItem
import com.example.model.LoggedExerciseDetail
import com.example.model.LoggedWorkoutEntry
import com.example.ui.screens.loggedPrRecords
import com.example.ui.theme.TrainPilotTheme
import com.repforge.app.ui.progress.CardioModelResult
import com.repforge.app.ui.progress.CardioTotals
import com.repforge.app.ui.progress.CardioWeekStats
import com.repforge.app.ui.progress.ProgressUiState
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xxhdpi")
class Collector2782RegressionTest {
    @get:Rule val compose = createComposeRule()

    private fun w(id: String, day: String, kg: Float, reps: Int, stored: String = "") = LoggedWorkoutEntry(id = id, titleHu = "Otthoni A/B",
        savedStartIso = "${day}T09:00", prRecordsJson = stored,
        exerciseDetails = listOf(LoggedExerciseDetail("Bolgár guggolás", "", listOf(ExerciseSetItem(1, kg, reps, 2, true)))))

    @Test fun workoutsWithoutStoredPrsGetThemFromTheirSavedSets() {
        val records = loggedPrRecords(listOf(w("a", "2026-11-01", 5f, 10), w("b", "2026-11-05", 5f, 10),
            w("c", "2026-11-09", 5.5f, 10), w("d", "2026-11-12", 5.5f, 10)))
        // First session is no record, an equal weight is no record, 5 → 5,5 kg is one.
        assertEquals(listOf("c:Bolgár guggolás:maxWeight"), records.map { it.id })
        assertEquals("5 kg", records.single().previousLabel)
        assertEquals("2026-11-09", records.single().dateLabel)
        // A stored list still wins for its own workout.
        val stored = """[{"exerciseId":"x","exerciseName":"Evezés","kind":"reps","value":"12","previous":"10"}]"""
        assertEquals(listOf("Evezés"), loggedPrRecords(listOf(w("a", "2026-11-01", 5f, 10), w("c", "2026-11-09", 5.5f, 10, stored)))
            .map { it.exerciseNameHu })
    }

    @Test fun aDuplicateSheetDismissDoesNotFallThroughToHome() {
        val vm = com.example.ui.viewmodel.TrainPilotViewModel()
        vm.selectTab(com.example.model.MainTab.PROGRESS)
        vm.navigateToSubScreen(com.example.model.SubScreen.LOG_WORKOUT_DETAIL)
        val depth = vm.uiState.value.backStack.size
        repeat(2) { vm.dismissSheetRoute(com.example.model.SubScreen.LOG_WORKOUT_DETAIL, depth) }
        assertEquals(com.example.model.MainTab.PROGRESS, vm.uiState.value.currentTab)
        assertEquals(com.example.model.SubScreen.NONE, vm.uiState.value.currentSubScreen)
    }

    private fun week(ago: Int, km: Double, n: Int): CardioWeekStats {
        val start = LocalDate.now().minusWeeks(ago.toLong())
        return CardioWeekStats(start, start.toString(), 0L, 0L, ago, km * 1000, km * 360, if (km > 0) 360.0 else null, 0.0, km * 1000, km * 360, n, n)
    }

    @Test fun cardioWeeksAreTappableAndAnEmptyWeekIsNotZero() {
        val weeks = (7 downTo 0).map { ago -> week(ago, if (ago == 3) 0.0 else 5.0 + ago, if (ago == 3) 0 else 2) }
        val totals = CardioTotals(60000.0, 21600.0, 360.0, 0.0, 60000.0, 21600.0)
        val model = CardioModelResult(null, totals, weeks.last(), weeks[weeks.size - 2], weeks, emptyList(), emptyList(), 10, 10)
        compose.setContent { TrainPilotTheme {
            com.repforge.app.ui.progress.NativeCardioProgressPanel(ProgressUiState(cardio = model, cardioLoading = false), {})
        } }
        compose.onNodeWithTag("progress_cardio_selected_week_value", useUnmergedTree = true).assertTextContains("km", substring = true)
        val latest = compose.onNodeWithTag("progress_cardio_week_7").assertIsSelected()
        val empty = compose.onNodeWithTag("progress_cardio_week_4").assertIsNotSelected()
        empty.performTouchInput { click() }.assertIsSelected()
        latest.assertIsNotSelected()
        compose.onNodeWithTag("progress_cardio_selected_week_value", useUnmergedTree = true).assertTextEquals("nincs edzés")
        latest.performTouchInput { click() }.assertIsSelected()
        empty.assertIsNotSelected()
        compose.onNodeWithTag("progress_cardio_selected_week_value", useUnmergedTree = true).assertTextContains("5", substring = true)
    }
    @Test fun cardioOverviewHasNoInnerActivityChoicesButKeepsThreeTotalsAndTypedRecords() {
        fun record(activity: String, meters: Int, seconds: Double) =
            com.repforge.app.ui.progress.CardioRecordEntry(
                exerciseId = activity, workoutId = "workout-$activity", setId = "set-$activity",
                setRowId = 1L, setNumber = 1, epochMs = 1_795_000_000_000L,
                meters = meters.toDouble(), roundedDistanceMeters = meters,
                seconds = seconds, pace = seconds / (meters / 1000.0),
                source = "manual", distanceSource = null,
                gpsRouteKey163 = null, gpsRouteKey193 = null
            )
        val weeks = (7 downTo 0).map { ago -> week(ago, 2.0 + ago, 1) }
        val totals = CardioTotals(36_000.0, 12_960.0, 360.0, 0.0, 36_000.0, 12_960.0)
        val records = listOf(
            com.repforge.app.ui.progress.CardioDistanceRecordGroup(
                "cycling", 5000, 2, record("cycling", 5000, 1000.0), record("cycling", 5000, 1100.0)),
            com.repforge.app.ui.progress.CardioDistanceRecordGroup(
                "running", 5000, 1, record("running", 5000, 1500.0), null),
            com.repforge.app.ui.progress.CardioDistanceRecordGroup(
                "walking", 2000, 1, record("walking", 2000, 1800.0), null)
        )
        val activities = listOf(
            com.repforge.app.ui.progress.CardioActivityOption(
                "cycling", "Kerékpározás", "Cycling", "Radfahren", "Ciclism", 2, 1),
            com.repforge.app.ui.progress.CardioActivityOption(
                "running", "Futás", "Running", "Laufen", "Alergare", 1, 1),
            com.repforge.app.ui.progress.CardioActivityOption(
                "walking", "Gyaloglás", "Walking", "Gehen", "Mers", 1, 1)
        )
        val model = CardioModelResult(null, totals, weeks.last(), weeks[6], weeks,
            records, activities, 4, 3)
        compose.setContent { TrainPilotTheme {
            com.repforge.app.ui.progress.NativeCardioProgressPanel(
                ProgressUiState(cardio = model, cardioLoading = false), {})
        } }
        compose.onNodeWithTag("progress_cardio_choices").assertDoesNotExist()
        compose.onNodeWithTag("progress_cardio_all").assertDoesNotExist()
        compose.onNodeWithTag("progress_cardio_activity_cycling").assertDoesNotExist()
        compose.onNodeWithTag("progress_cardio_week_distance").assertExists()
        compose.onNodeWithTag("progress_cardio_previous_week_distance").assertExists()
        compose.onNodeWithTag("progress_cardio_total_distance").assertExists()
        compose.onNodeWithTag("progress_cardio_chart").assertExists()
        compose.onNodeWithTag("progress_cardio_record_cycling_5000").assertExists()
        compose.onNodeWithTag("progress_cardio_record_running_5000").assertExists()
        compose.onNodeWithText("Kerékpározás", substring = true).assertExists()
        compose.onNodeWithText("Futás", substring = true).assertExists()
        compose.onNodeWithTag("progress_cardio_record_walking_2000").assertDoesNotExist()
        compose.onNodeWithTag("progress_cardio_week_5").performTouchInput { click() }.assertIsSelected()
        compose.onNodeWithTag("progress_cardio_expand").performClick()
        compose.onNodeWithTag("progress_cardio_record_walking_2000").assertExists()
        compose.onNodeWithText("Gyaloglás", substring = true).assertExists()
        compose.onNodeWithTag("progress_cardio_week_5").assertIsSelected()
    }

    @Test fun unfilteredCardioModelCombinesWeeksButNeverMergesEqualDistanceRecordsAcrossActivities() {
        val zone = java.time.ZoneId.of("Europe/Budapest")
        fun set(activity: String, workoutId: String, date: LocalDate, km: Double, seconds: Double) =
            com.repforge.app.ui.progress.CardioSetRow(
                workoutId = workoutId, exerciseId = activity,
                exerciseNameHu = if (activity == "cycling") "Kerékpározás" else "Futás",
                finishedEpochMs = date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli(),
                distanceMeters = km * 1000, durationSeconds = seconds
            )
        val rows = listOf(
            set("cycling", "ride-current", LocalDate.of(2026, 11, 20), 5.0, 900.0),
            set("running", "run-current", LocalDate.of(2026, 11, 21), 5.0, 1500.0),
            set("cycling", "ride-previous", LocalDate.of(2026, 11, 11), 5.0, 850.0),
            set("running", "run-previous", LocalDate.of(2026, 11, 10), 2.0, 720.0)
        )
        val now = LocalDate.of(2026, 11, 22).atTime(20, 0).atZone(zone).toInstant().toEpochMilli()
        val model = com.repforge.app.ui.progress.NativeCardioModel.compute(
            rows, now, zone, com.repforge.app.ui.progress.NativeCardioModel.ALL_ACTIVITIES)
        assertNull(model.selectedExerciseId)
        assertEquals(8, model.weeks.size)
        assertEquals(10_000.0, model.currentWeek.meters, 0.001)
        assertEquals(7_000.0, model.previousWeek.meters, 0.001)
        assertEquals(17_000.0, model.totals.meters, 0.001)
        assertEquals(2, model.currentWeek.workoutCount)
        assertEquals(2, model.previousWeek.workoutCount)
        val byTypeAndDistance = model.records.associateBy { it.exerciseId to it.roundedDistanceMeters }
        assertEquals(setOf("cycling" to 5000, "running" to 5000, "running" to 2000),
            byTypeAndDistance.keys)
        assertEquals(2, byTypeAndDistance.getValue("cycling" to 5000).count)
        assertEquals(1, byTypeAndDistance.getValue("running" to 5000).count)
        assertEquals("ride-previous", byTypeAndDistance.getValue("cycling" to 5000).best.workoutId)
        assertEquals(setOf("cycling", "running"), model.activities.map { it.exerciseId }.toSet())
    }

}
