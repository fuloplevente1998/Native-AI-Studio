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
