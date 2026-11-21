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
            com.repforge.app.ui.progress.NativeCardioProgressPanel(ProgressUiState(cardio = model, cardioLoading = false), {}, {})
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
}
