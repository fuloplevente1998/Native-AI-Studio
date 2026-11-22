package com.example

import com.repforge.app.ui.health.HourlyStepReading
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
