package com.example

import com.example.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.Duration

class StudioProgressWindowTest {
    private val today = LocalDate.of(2026, 11, 2)
    private val zone = ZoneId.of("Europe/Budapest")
    private fun row(days: Int, kg: Int) = LoggedWorkoutEntry("$days", "", "Workout", 20, "", "", 1, kg, "",
        savedStartIso = today.minusDays(days.toLong()).atTime(12, 0).atZone(zone).toInstant().toString())
    private val rows = listOf(row(0, 100), row(3, 200), row(20, 400), row(60, 800), row(120, 5000), row(-1, 9000))

    @Test fun eachRangeFiltersTotalsAndBucketsFromTheSameDates() {
        val expected = listOf(100, 300, 700, 1500)
        for (index in 0..3) {
            val period = StudioProgressWindow(progressPeriodDays(index), today)
            assertEquals(expected[index], period.workouts(rows, zone).sumOf { it.totalVolumeKg })
            assertEquals(expected[index], period.volumes(rows, zone).sumOf { it.second })
            assertEquals(listOf(1, 7, 5, 13)[index], period.volumes(rows, zone).size)
        }
    }
    @Test fun oldestIncludedDayAndLastPartialWeekAreNeitherLostNorDuplicated() {
        val period = StudioProgressWindow(90, today)
        val edge = listOf(row(89, 111), row(90, 999), row(0, 222))
        assertEquals(333, period.volumes(edge, zone).sumOf { it.second })
        assertEquals(111, period.volumes(edge, zone).first().second)
        assertEquals(222, period.volumes(edge, zone).last().second)
    }
    @Test fun monthlyWeeklyBucketsKeepBoundaryDaysAndAxisUsesReadableTicks() {
        val month = StudioProgressWindow(30, today)
        val values = month.volumes(listOf(row(29, 111), row(23, 222), row(22, 333), row(0, 444), row(30, 999)), zone)
        assertEquals(5, values.size)
        assertEquals(333, values.first().second)
        assertEquals(333, values[1].second)
        assertEquals(444, values.last().second)
        assertEquals(1110, values.sumOf { it.second })
        assertEquals(2000L, volumeAxisMaximum(1254))
        assertEquals(4000L, volumeAxisMaximum(3187))
        assertTrue(volumeAxisMaximum(0) > 0)
    }
    @Test fun localDayQueryBoundsFollowDstAndIncludeTheEntireEndDay() {
        val day = StudioProgressWindow(1, LocalDate.of(2026, 10, 25))
        assertEquals(Duration.ofHours(25).toMillis(), day.toEpoch(zone) - day.fromEpoch(zone) + 1)
    }
}
