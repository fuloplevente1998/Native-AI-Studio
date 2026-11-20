package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.model.LoggedWorkoutEntry
import com.example.ui.components.coachTargetText
import com.example.ui.screens.todayTileJournalType
import com.example.ui.theme.TrainPilotTheme
import com.example.ui.viewmodel.TrainPilotUiState
import com.repforge.app.ui.coach.NativeCoachExerciseAdvice
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-xxhdpi")
class Collector2783RegressionTest {
    @get:Rule val compose = createComposeRule()

    private fun pr(name: String) = """[{"exerciseId":"$name","exerciseName":"$name","kind":"maxWeight","value":"12 kg","previous":"10 kg"}]"""

    @Test fun prSheetListsOnlyTheRecordsOfTheSelectedPeriod() {
        val today = LocalDate.now()
        val workouts = listOf(
            LoggedWorkoutEntry(id = "new", titleHu = "A", savedStartIso = today.minusDays(3).atTime(9, 0).toString(), prRecordsJson = pr("Evezés")),
            LoggedWorkoutEntry(id = "old", titleHu = "B", savedStartIso = today.minusDays(60).atTime(9, 0).toString(), prRecordsJson = pr("Guggolás")))
        compose.setContent { TrainPilotTheme {
            com.example.ui.screens.MainProgressVolumeAndMuscleSection(
                TrainPilotUiState(loggedWorkouts = workouts, selectedProgressRangeIndex = 2, expandedStatsCardIndex = 0),
                onSelectRange = {}, onSelectVolumeBar = {}, onSelectExpandedStatsCard = {}, onOpenWorkoutDetail = {})
        } }
        compose.onAllNodesWithText("Evezés", substring = true, useUnmergedTree = true).assertCountEquals(1)
        compose.onAllNodesWithText("Guggolás", substring = true, useUnmergedTree = true).assertCountEquals(0)
    }

    @Test fun todayTilesWithDataOpenTheirJournalTypeAndEmptyTilesKeepTheSheet() {
        val withData = TrainPilotUiState(sleepHoursDecimal = "4.2 h", dailyHealthAvailableFields = setOf("steps", "activeCalories"))
        assertEquals("SleepSessionRecord", todayTileJournalType("sleep", withData))
        assertEquals("StepsRecord", todayTileJournalType("steps", withData))
        assertEquals("ActiveCaloriesBurnedRecord", todayTileJournalType("calories", withData))
        assertNull(todayTileJournalType("pulse", withData)) // pulse keeps its own trend view
        assertNull(todayTileJournalType("workout", withData))
        assertNull(todayTileJournalType("recovery", withData))
        assertNull(todayTileJournalType("sleep", TrainPilotUiState(sleepHoursDecimal = "—")))
        assertNull(todayTileJournalType("steps", TrainPilotUiState()))
    }

    @Test fun perArmTargetNamesTheRepWord() {
        fun t(text: String, kind: String = "reps") = NativeCoachExerciseAdvice.Target(kind, text = text)
        assertEquals("10 ism./kar", coachTargetText(t("10 /kar"), "ism./kar", "hu"))
        assertEquals("25 mp/oldal", coachTargetText(t("25/oldal", "time"), "", "hu"))
        assertEquals("2 RIR", coachTargetText(t("2 RIR", "rir"), "ism.", "hu"))
    }

    @Test fun exerciseCardsGetAMiniTrendFromSavedSets() {
        fun w(day: String, kg: Float, reps: Int) = LoggedWorkoutEntry(id = day, titleHu = "A", savedStartIso = "${day}T09:00",
            exerciseDetails = listOf(com.example.model.LoggedExerciseDetail("Goblet guggolás", "",
                listOf(com.example.model.ExerciseSetItem(1, kg, reps, 2, true), com.example.model.ExerciseSetItem(2, kg + 5f, reps, 2, false)))))
        val record = com.repforge.app.SavedExerciseBest("goblet", "Goblet guggolás", "Goblet squat", "ism.", "reps", 12.5, 12, null, null)
        val points = com.example.ui.screens.exerciseTrend(record, listOf(w("2026-11-10", 12.5f, 12), w("2026-11-01", 10f, 12)))
        assertEquals(listOf(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-10")), points.map { it.date }) // oldest first
        assertEquals(10.0 * (1 + 12 / 30.0), points.first().value, 1e-6) // uncompleted heavier set ignored
        val kind = com.example.ui.screens.exerciseTrendKind(record)
        assertEquals("▲ +3 kg", com.example.ui.screens.exerciseTrendChange(points, kind, "hu"))
        assertNull(com.example.ui.screens.exerciseTrendChange(points.take(1), kind, "hu"))
        val bodyweight = record.copy(weightKg = null)
        assertEquals("± 0", com.example.ui.screens.exerciseTrendChange(
            com.example.ui.screens.exerciseTrend(bodyweight, listOf(w("2026-11-10", 0f, 12), w("2026-11-01", 0f, 12))),
            com.example.ui.screens.exerciseTrendKind(bodyweight), "hu"))
        assertTrue(com.example.ui.screens.exerciseTrend(record.copy(distanceMeters = 5000.0), listOf(w("2026-11-10", 12.5f, 12))).isEmpty())
    }
}
