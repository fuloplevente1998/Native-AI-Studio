package com.example

import com.example.ui.components.hourlyStepBarFractions
import com.repforge.app.ui.health.HourlyStepReading
import com.repforge.app.ui.health.HourlySteps
import com.repforge.app.ui.health.elapsedStepHourWindows
import com.repforge.app.ui.health.measuredStepHours
import com.repforge.app.ui.health.hourlySteps
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.*
import org.junit.Test

class HourlyStepsRegressionTest {
    private fun reading(start: String, end: String, count: Long) =
        HourlyStepReading(Instant.parse(start), Instant.parse(end), count)

    @Test fun countsWholeRecordsAndDistinguishesMeasuredZeroUnknownAndFutureHours() {
        val first = reading("2026-11-21T08:10:00Z", "2026-11-21T08:13:00Z", 30)
        val rows = listOf(first, first, reading("2026-11-21T08:20:00Z", "2026-11-21T09:00:00Z", 17),
            reading("2026-11-21T09:00:00Z", "2026-11-21T09:10:00Z", 0),
            reading("2026-11-21T10:59:00Z", "2026-11-21T11:01:00Z", 20),
            reading("2026-11-21T11:04:00Z", "2026-11-21T11:05:00Z", 5))
        val result = hourlySteps(LocalDate.parse("2026-11-21"), ZoneOffset.UTC, Instant.parse("2026-11-21T11:30:00Z"), rows)
        assertEquals(24, result.size)
        assertEquals(47L, result[8].count)
        assertEquals(0L, result[9].count)
        assertNull(result[10].count)
        assertNull(result[11].count)
        assertFalse(result[11].future)
        assertTrue(result[12].future)
    }

    @Test fun individualWindowsKeepDistinctCountsAndOnlyCoverElapsedTime() {
        val day = LocalDate.parse("2026-11-22")
        val now = Instant.parse("2026-11-22T04:35:00Z")
        val windows = elapsedStepHourWindows(day, ZoneOffset.UTC, now)
        val counts = listOf(0L, 45L, 312L, 20L, 241L)
        assertEquals(counts.size, windows.size)
        assertEquals(now, windows.last().second)
        val measured = measuredStepHours(windows, counts, ZoneOffset.UTC)
        assertEquals(counts, (0..4).map { measured[it] })
        val values = (0..5).map { HourlySteps(it, measured[it], future = it == 5) }
        val bars = hourlyStepBarFractions(values)
        assertEquals(0f, requireNotNull(bars[0]), 0f)
        assertEquals(45f / 312f, requireNotNull(bars[1]), 0.0001f)
        assertEquals(1f, requireNotNull(bars[2]), 0f)
        assertEquals(20f / 312f, requireNotNull(bars[3]), 0.0001f)
        assertEquals(241f / 312f, requireNotNull(bars[4]), 0.0001f)
        assertNull(bars[5])
    }

    @Test fun independentWindowsRequireBothOccurrencesOfRepeatedHour() {
        val zone = ZoneId.of("Europe/Budapest")
        val day = LocalDate.parse("2026-10-25")
        val now = Instant.parse("2026-10-25T04:12:00Z")
        val windows = elapsedStepHourWindows(day, zone, now)
        val repeated = windows.indices.filter { windows[it].first.atZone(zone).hour == 2 }
        assertEquals(2, repeated.size)
        val counts = windows.indices.map { i -> if (i == repeated[0]) 21L else if (i == repeated[1]) 4L else null }
        assertEquals(mapOf(2 to 25L), measuredStepHours(windows, counts, zone))
        assertEquals(emptyMap<Int, Long>(), measuredStepHours(windows, counts.map { it.takeUnless { n -> n == 4L } }, zone))
    }

    @Test fun repeatedDstHourCountsDisjointRecordsAndOverlappingRowsRemainUnknown() {
        val result = hourlySteps(LocalDate.parse("2026-10-25"), ZoneId.of("Europe/Budapest"), Instant.parse("2026-10-25T04:30:00Z"),
            listOf(reading("2026-10-25T00:10:00Z", "2026-10-25T00:20:00Z", 9),
                reading("2026-10-25T01:10:00Z", "2026-10-25T01:20:00Z", 11),
                reading("2026-10-25T02:10:00Z", "2026-10-25T02:40:00Z", 31),
                reading("2026-10-25T02:30:00Z", "2026-10-25T02:45:00Z", 19)))
        assertEquals(20L, result[2].count)
        assertNull(result[3].count)
        val spring = hourlySteps(LocalDate.parse("2026-03-29"), ZoneId.of("Europe/Budapest"),
            Instant.parse("2026-03-29T05:00:00Z"), emptyList())
        assertNull(spring[2].count)
        assertFalse(spring[2].future)
    }
}
