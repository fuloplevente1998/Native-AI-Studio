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
        assertNull(todayTileJournalType("pulse", withData)) // no pulse reading today
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
}
