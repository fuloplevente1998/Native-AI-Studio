package com.example.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Assign timestamped workouts to the user's local day, never to a display-string UTC day. */
internal fun LoggedWorkoutEntry.localWorkoutDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate? {
    if (savedStartIso.isNotBlank()) {
        return runCatching { Instant.parse(savedStartIso).atZone(zone).toLocalDate() }.getOrNull()
            ?: runCatching { LocalDateTime.parse(savedStartIso).toLocalDate() }.getOrNull()
            ?: runCatching { LocalDate.parse(savedStartIso) }.getOrNull()
    }
    return runCatching { LocalDate.parse(dateLabelHu.take(10)) }.getOrNull()
}

/** Inclusive rolling seven-day window; previous windows end before the next starts. */
internal fun List<LoggedWorkoutEntry>.workoutsInWeekEnding(
    end: LocalDate,
    zone: ZoneId = ZoneId.systemDefault()
): List<LoggedWorkoutEntry> = filter { workout ->
    val day = workout.localWorkoutDate(zone)
    day != null && !day.isBefore(end.minusDays(6)) && !day.isAfter(end)
}

internal fun List<LoggedWorkoutEntry>.workoutsOn(
    date: LocalDate, zone: ZoneId = ZoneId.systemDefault()
): List<LoggedWorkoutEntry> = filter { it.localWorkoutDate(zone) == date }

/** Completed history in the current local calendar week, Monday through today; never lifetime totals. */
internal fun List<LoggedWorkoutEntry>.workoutsInCalendarWeekToDate(
    today: LocalDate, zone: ZoneId = ZoneId.systemDefault()
): List<LoggedWorkoutEntry> {
    val monday = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
    return filter { row -> row.localWorkoutDate(zone)?.let { !it.isBefore(monday) && !it.isAfter(today) } == true }
}
