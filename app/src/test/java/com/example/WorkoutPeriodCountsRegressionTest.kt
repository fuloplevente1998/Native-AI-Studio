package com.example

import com.example.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class WorkoutPeriodCountsRegressionTest {
    private val today = LocalDate.of(2026, 11, 3)
    private val zone = ZoneId.of("Europe/Budapest")
    private fun row(days: Long) = LoggedWorkoutEntry("$days", "", "Workout", 20, "", "", 1, 100, "",
        savedStartIso = today.minusDays(days).atTime(12, 0).atZone(zone).toInstant().toString())
    @Test fun importingTwentyEightHistoryRowsDoesNotMakeThemAllTodaysOrThisWeeksWorkouts() {
        val history = (10L..35L).map(::row) + row(0) + row(1)
        assertEquals(28, history.size)
        assertEquals(1, history.workoutsOn(today, zone).size)
        assertEquals(2, history.workoutsInCalendarWeekToDate(today, zone).size)
    }
    @Test fun calendarWeekExcludesSundayAndFutureDates() {
        val history = listOf(row(0), row(1), row(2), row(-1))
        assertEquals(listOf("0", "1"), history.workoutsInCalendarWeekToDate(today, zone).map { it.id })
    }
    @Test fun localMidnightUsesThePhonesTimezoneAndInvalidDatesAreNotInventedAsToday() {
        val midnight = row(0).copy(savedStartIso = "2026-11-02T23:30:00Z")
        val invalid = row(5).copy(savedStartIso = "invalid", dateLabelHu = "")
        assertEquals(1, listOf(midnight, invalid).workoutsOn(today, zone).size)
        assertTrue(listOf(invalid).workoutsInCalendarWeekToDate(today, zone).isEmpty())
    }
}
