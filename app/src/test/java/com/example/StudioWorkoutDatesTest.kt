package com.example

import com.example.model.LoggedWorkoutEntry
import com.example.model.localWorkoutDate
import com.example.model.workoutsInWeekEnding
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class StudioWorkoutDatesTest {
    private val zone = ZoneId.of("Europe/Budapest")
    private val today = LocalDate.of(2026, 11, 2)
    private fun workout(id: String, started: String, km: Float = 1f) = LoggedWorkoutEntry(
        id, "", "Run", 20, "20:00", "0:00", 0, 100, "",
        distanceKm = km, savedStartIso = started)

    @Test fun weeklyDistanceIncludesSevenLocalDaysAndExcludesOlderFutureAndUndatedRows() {
        val rows = listOf(
            workout("today", "2026-11-02T12:00:00Z", 2f),
            workout("oldest", "2026-10-26T23:00:00Z", 3f), // Oct 27 locally
            workout("too-old", "2026-10-26T22:59:59Z", 50f),
            workout("tomorrow", "2026-11-02T23:00:00Z", 100f),
            workout("unknown", "", 200f),
            workout("invalid", "broken", 400f))
        val week = rows.workoutsInWeekEnding(today, zone)
        assertEquals(listOf("today", "oldest"), week.map { it.id })
        assertEquals(5.0, week.sumOf { it.distanceKm.toDouble() }, 0.001)
        assertEquals(200, week.sumOf { it.totalVolumeKg })
    }

    @Test fun adjacentWeeklyBucketsDoNotOverlapAndUseLocalTimeAcrossDst() {
        val rows = listOf(workout("sunday", "2026-10-25T00:30:00Z"),
            workout("monday", "2026-10-26T23:30:00Z"))
        assertEquals(listOf("monday"), rows.workoutsInWeekEnding(today, zone).map { it.id })
        assertEquals(listOf("sunday"), rows.workoutsInWeekEnding(today.minusWeeks(1), zone).map { it.id })
    }

    @Test fun datedLegacyRowsRemainVisibleButInvalidTimestampCannotBecomeCurrentWeek() {
        assertEquals(LocalDate.of(2026, 11, 2),
            workout("legacy", "").copy(dateLabelHu = "2026-11-02 10:20").localWorkoutDate(zone))
        assertEquals(today, workout("local", "2026-11-02T01:00:00").localWorkoutDate(zone))
        assertNull(workout("broken", "invalid").copy(dateLabelHu = "2026-11-02").localWorkoutDate(zone))
    }

    @Test fun malformedCanonicalTimeCannotBeRescuedByItsDatePrefixOrDisplayLabel() {
        val malformed = listOf("2026-11-02T25:00:00", "2026-11-02T01:00:00junk", "2026-11-02broken")
            .mapIndexed { index, value -> workout("invalid-$index", value).copy(dateLabelHu = "2026. nov. 2.") }
        malformed.forEach { assertNull(it.savedStartIso, it.localWorkoutDate(zone)) }
        assertTrue(malformed.workoutsInWeekEnding(today, zone).isEmpty())
        assertEquals(today, workout("space-time", "2026-11-02 01:00:00").localWorkoutDate(zone))
    }

}
