package com.example.model

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun progressPeriodDays(index: Int): Int = listOf(1, 7, 30, 90)[index.coerceIn(0, 3)]

internal data class StudioProgressWindow(val days: Int, val end: LocalDate = LocalDate.now()) {
    val start: LocalDate = end.minusDays((days - 1).toLong())
    fun contains(date: LocalDate?): Boolean = date != null && !date.isBefore(start) && !date.isAfter(end)
    fun fromEpoch(zone: ZoneId = ZoneId.systemDefault()): Long = start.atStartOfDay(zone).toInstant().toEpochMilli()
    fun toEpoch(zone: ZoneId = ZoneId.systemDefault()): Long = end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
    fun workouts(rows: List<LoggedWorkoutEntry>, zone: ZoneId = ZoneId.systemDefault()) =
        rows.filter { contains(it.localWorkoutDate(zone)) }
    fun volumes(rows: List<LoggedWorkoutEntry>, zone: ZoneId = ZoneId.systemDefault()): List<Pair<String, Int>> {
        val step = if (days == 90) 7 else 1
        val dated = workouts(rows, zone).groupBy { it.localWorkoutDate(zone) }
        val format = DateTimeFormatter.ofPattern("MM.dd.", Locale.ROOT)
        return (0 until days step step).map { offset ->
            val first = start.plusDays(offset.toLong())
            val last = minOf(first.plusDays((step - 1).toLong()), end)
            val kg = dated.filterKeys { it != null && !it.isBefore(first) && !it.isAfter(last) }
                .values.flatten().sumOf { it.totalVolumeKg }
            first.format(format) to kg
        }
    }
}
