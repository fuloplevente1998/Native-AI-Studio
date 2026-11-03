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
    fun previous(): StudioProgressWindow = StudioProgressWindow(days, start.minusDays(1))
    fun volumeChangePercent(rows: List<LoggedWorkoutEntry>, zone: ZoneId = ZoneId.systemDefault()): Long? {
        val previous = previous().workouts(rows, zone).sumOf { it.totalVolumeKg.toLong() }
        if (previous <= 0L) return null
        val current = workouts(rows, zone).sumOf { it.totalVolumeKg.toLong() }
        return Math.round((current - previous) * 100.0 / previous)
    }
    fun volumes(rows: List<LoggedWorkoutEntry>, zone: ZoneId = ZoneId.systemDefault()): List<Pair<String, Int>> {
        val step = if (days >= 30) 7 else 1
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

/** Four readable ticks above the real maximum; never changes measured bar values. */
internal fun volumeAxisMaximum(maximum: Int): Long {
    val raw = maximum.coerceAtLeast(1) / 4.0
    val magnitude = Math.pow(10.0, kotlin.math.floor(kotlin.math.log10(raw)))
    val step = listOf(1.0, 2.0, 2.5, 5.0, 10.0).first { it * magnitude >= raw }
    return kotlin.math.ceil(step * magnitude).toLong().coerceAtLeast(1) * 4L
}
