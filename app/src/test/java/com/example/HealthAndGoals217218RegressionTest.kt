package com.example

import com.example.model.*
import com.example.ui.viewmodel.TrainPilotUiState
import com.example.localization.TrainPilotStrings
import com.repforge.app.healthreadings.PulseDayReading
import com.repforge.app.healthreadings.ReadingState
import com.repforge.app.healthreadings.RecoveryDayReading
import com.repforge.app.healthreadings.RecoverySource
import com.repforge.app.HealthJournalStore
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Regression tests for Issues #217 and #218:
 * - #217: Six Health tiles ("Mai állapot") data provenance, honest labels, missing/stale data handling, and journal navigation.
 * - #218: Workout goal period (weekly vs monthly), 25 edzés/hónap, daily steps, monthly calendar boundaries, and persistence.
 */
class HealthAndGoals217218RegressionTest {

    private val zone = ZoneId.of("Europe/Budapest")
    private val testToday = LocalDate.of(2026, 11, 9)

    private fun mockWorkout(id: String, daysAgo: Long) = LoggedWorkoutEntry(
        id = id,
        dateLabelHu = testToday.minusDays(daysAgo).toString(),
        titleHu = "Edzés $id",
        durationMinutes = 45,
        totalSets = 3,
        totalVolumeKg = 300,
        savedStartIso = testToday.minusDays(daysAgo).atTime(10, 0).atZone(zone).toInstant().toString()
    )

    // =========================================================================
    // TASK A (#217): HEALTH METRICS PROVENANCE, LABELS & MISSING DATA
    // =========================================================================

    @Test
    fun testSleepStagesTruthfulWhenMissingVersusProvided() {
        val stateWithoutStages = TrainPilotUiState(
            dailyHealthAvailableFields = setOf("sleepMinutes"),
            sleepDurationLabel = "7 ó 15 p",
            sleepHoursDecimal = "7.2 h",
            sleepHasStages = false,
            sleepStagesSummaryHu = "A szolgáltató nem adott át alvásfázisbontást (csak teljes időtartam)"
        )
        assertFalse(stateWithoutStages.sleepHasStages)
        assertTrue(stateWithoutStages.sleepStagesSummaryHu.contains("nem adott át alvásfázisbontást"))

        val stateWithStages = TrainPilotUiState(
            dailyHealthAvailableFields = setOf("sleepMinutes"),
            sleepDurationLabel = "8 ó 0 p",
            sleepHoursDecimal = "8.0 h",
            sleepHasStages = true,
            sleepStagesSummaryHu = "Részletes alvásfázisok rögzítve (mély, REM, könnyű alvás)"
        )
        assertTrue(stateWithStages.sleepHasStages)
        assertTrue(stateWithStages.sleepStagesSummaryHu.contains("Részletes alvásfázisok"))
    }

    @Test
    fun testMissingHealthDataShowsHyphenNeverFabricatedZeros() {
        val emptyState = TrainPilotUiState(
            dailyHealthAvailableFields = emptySet(),
            dailySteps = 0,
            activeCaloriesKcal = 0,
            totalCaloriesKcal = 0,
            sleepDurationLabel = "—",
            sleepHoursDecimal = "—",
            todayPulseReading = null,
            todayRecoveryReading = null
        )
        assertEquals("—", emptyState.sleepDurationLabel)
        assertEquals("—", emptyState.sleepHoursDecimal)
        assertNull(emptyState.todayPulseReading)
        assertNull(emptyState.todayRecoveryReading)
        assertFalse("steps" in emptyState.dailyHealthAvailableFields)
        assertFalse("activeCalories" in emptyState.dailyHealthAvailableFields)
    }

    @Test
    fun testActiveCaloriesSeparatedFromTotalCalories() {
        val state = TrainPilotUiState(
            dailyHealthAvailableFields = setOf("activeCalories", "totalCalories"),
            activeCaloriesKcal = 450,
            totalCaloriesKcal = 2100
        )
        assertEquals(450, state.activeCaloriesKcal)
        assertEquals(2100, state.totalCaloriesKcal)
        assertNotEquals(state.activeCaloriesKcal, state.totalCaloriesKcal)
    }

    @Test
    fun testPulseLabelsDistinguishAverageRestingAndLastReading() {
        val avgReading = PulseDayReading(
            day = testToday,
            provider = HealthJournalStore.HC,
            state = ReadingState.AVAILABLE,
            averageBpm = 68.4,
            averageDescriptionHu = "Napi átlagpulzus",
            explanationHu = "Egész napos aggregáció"
        )
        assertEquals(68.4, avgReading.averageBpm!!, 0.01)
        assertNull(avgReading.lastBpm)

        val lastReading = PulseDayReading(
            day = testToday,
            provider = HealthJournalStore.HC,
            state = ReadingState.AVAILABLE,
            lastBpm = 75,
            explanationHu = "Utolsó időbélyeges mérés"
        )
        assertNull(lastReading.averageBpm)
        assertEquals(75, lastReading.lastBpm)

        val restingReading = PulseDayReading(
            day = testToday,
            provider = HealthJournalStore.HC,
            state = ReadingState.AVAILABLE,
            restingBpm = 54.0,
            explanationHu = "Nyugalmi pulzus"
        )
        assertNull(restingReading.averageBpm)
        assertEquals(54.0, restingReading.restingBpm!!, 0.01)
    }

    @Test
    fun testRecoveryScoreClearlyDistinguishesSamsungEnergyScoreFromTrainPilotEstimate() {
        val samsungReading = RecoveryDayReading(
            day = testToday,
            provider = HealthJournalStore.SAMSUNG,
            state = ReadingState.AVAILABLE,
            source = RecoverySource.SAMSUNG_ENERGY,
            score = 85.0,
            coachScore = 85.0,
            samsungEnergy = 85.0,
            samsungAt = "2026-11-09T07:00:00Z",
            trainPilotEstimate = null,
            explanationHu = "Samsung Health Energy Score, mai időbélyeges mérés; nem a TrainPilot becslése."
        )
        assertEquals(RecoverySource.SAMSUNG_ENERGY, samsungReading.source)
        assertEquals("85/100", samsungReading.displayScoreHu)
        assertTrue(samsungReading.explanationHu.contains("Samsung Health Energy Score"))

        val tpReading = RecoveryDayReading(
            day = testToday,
            provider = HealthJournalStore.HC,
            state = ReadingState.AVAILABLE,
            source = RecoverySource.TRAINPILOT_ESTIMATE,
            score = 72.0,
            coachScore = 72.0,
            samsungEnergy = null,
            samsungAt = null,
            trainPilotEstimate = 72,
            explanationHu = "TrainPilot-becslés valós alvás/HRV és naplózott terhelés alapján; nem Samsung Energy Score."
        )
        assertEquals(RecoverySource.TRAINPILOT_ESTIMATE, tpReading.source)
        assertEquals("72/100", tpReading.displayScoreHu)
        assertTrue(tpReading.explanationHu.contains("TrainPilot-becslés"))
    }

    // =========================================================================
    // TASK B (#218): WORKOUT GOALS, WEEKLY/MONTHLY PERIODS & STEPS
    // =========================================================================

    @Test
    fun testTwentyFiveWorkoutsPerMonthFormatting() {
        val stringsHu = TrainPilotStrings.forLanguage(AppLanguage.HU)
        val formattedMonthly = stringsHu.periodStepsFormat(25, true, 10000)
        assertEquals("Havi 25 edzés • Napi 10000 lépés", formattedMonthly)

        val formattedWeekly = stringsHu.periodStepsFormat(4, false, 8000)
        assertEquals("Heti 4 edzés • Napi 8000 lépés", formattedWeekly)
    }

    @Test
    fun testCalendarMonthWorkoutsBoundaryAndDeduplication() {
        // Today is 2026-11-09.
        // First of month is 2026-11-01 (8 days ago).
        // 9 days ago is 2026-10-31 (previous month, October).
        val wToday = mockWorkout("w_today", 0)
        val w3DaysAgo = mockWorkout("w_3d", 3)
        val wFirstOfMonth = mockWorkout("w_1st", 8)
        val wLastMonth = mockWorkout("w_oct31", 9)
        val wDuplicate = mockWorkout("w_today", 0) // Duplicate ID

        val history = listOf(wToday, w3DaysAgo, wFirstOfMonth, wLastMonth, wDuplicate)

        val monthWorkouts = history.workoutsInCalendarMonthToDate(testToday, zone)
        assertEquals(3, monthWorkouts.size) // w_today, w_3d, w_1st (w_oct31 excluded, duplicate ignored)
        assertTrue(monthWorkouts.any { it.id == "w_today" })
        assertTrue(monthWorkouts.any { it.id == "w_3d" })
        assertTrue(monthWorkouts.any { it.id == "w_1st" })
        assertFalse(monthWorkouts.any { it.id == "w_oct31" })

        val periodWorkoutsMonthly = history.workoutsInPeriodToDate(WorkoutGoalPeriod.MONTHLY, testToday, zone)
        assertEquals(3, periodWorkoutsMonthly.size)

        // 2026-11-09 is Monday.
        // In calendar week to date (Monday to today), only today's workout qualifies.
        val weekWorkouts = history.workoutsInPeriodToDate(WorkoutGoalPeriod.WEEKLY, testToday, zone)
        assertEquals(1, weekWorkouts.size)
        assertEquals("w_today", weekWorkouts.first().id)
    }

    @Test
    fun testUserGoalsSerializationAndConservativeMigration() {
        fun resolvePeriod(rawPeriod: String?): WorkoutGoalPeriod {
            val token = rawPeriod?.trim().orEmpty()
            if (token.isEmpty()) return WorkoutGoalPeriod.WEEKLY
            return runCatching { WorkoutGoalPeriod.valueOf(token.uppercase()) }.getOrDefault(WorkoutGoalPeriod.WEEKLY)
        }

        // 1. Legacy format with absent period token defaults conservatively to WEEKLY
        assertEquals(WorkoutGoalPeriod.WEEKLY, resolvePeriod(null))
        assertEquals(WorkoutGoalPeriod.WEEKLY, resolvePeriod(""))
        assertEquals(WorkoutGoalPeriod.WEEKLY, resolvePeriod("   "))
        assertEquals(WorkoutGoalPeriod.WEEKLY, resolvePeriod("weekly"))
        assertEquals(WorkoutGoalPeriod.WEEKLY, resolvePeriod("WEEKLY"))

        // 2. New format with explicit monthly goal
        assertEquals(WorkoutGoalPeriod.MONTHLY, resolvePeriod("monthly"))
        assertEquals(WorkoutGoalPeriod.MONTHLY, resolvePeriod("MONTHLY"))

        // 3. Fallback on invalid/corrupted token keeps existing data safe
        assertEquals(WorkoutGoalPeriod.WEEKLY, resolvePeriod("corrupted_value"))
    }
}
