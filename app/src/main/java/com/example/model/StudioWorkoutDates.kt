package com.example.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Assign timestamped workouts to the user's local day, never to a display-string UTC day. */
internal fun LoggedWorkoutEntry.localWorkoutDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate? {
    if (savedStartIso.isNotBlank()) {
        val fromIso = runCatching { Instant.parse(savedStartIso).atZone(zone).toLocalDate() }.getOrNull()
            ?: runCatching { java.time.OffsetDateTime.parse(savedStartIso).atZoneSameInstant(zone).toLocalDate() }.getOrNull()
            ?: runCatching { LocalDateTime.parse(savedStartIso.replace(' ', 'T').take(19)).toLocalDate() }.getOrNull()
            ?: runCatching { LocalDate.parse(savedStartIso.take(10)) }.getOrNull()
        if (fromIso != null) return fromIso
    }
    return parseFlexibleWorkoutDate(dateLabelHu)
}

private fun parseFlexibleWorkoutDate(raw: String): LocalDate? {
    val text = raw.trim()
    if (text.isEmpty()) return null
    runCatching { LocalDate.parse(text.take(10)) }.getOrNull()?.let { return it }
    Regex("(\\d{4})-(\\d{1,2})-(\\d{1,2})").find(text)?.let { m ->
        val y = m.groupValues[1].toIntOrNull()
        val mo = m.groupValues[2].toIntOrNull()
        val d = m.groupValues[3].toIntOrNull()
        if (y != null && mo != null && d != null) {
            runCatching { LocalDate.of(y, mo, d) }.getOrNull()?.let { return it }
        }
    }
    Regex("(\\d{4})\\.\\s*(\\d{1,2})\\.\\s*(\\d{1,2})\\.?").find(text)?.let { m ->
        val y = m.groupValues[1].toIntOrNull()
        val mo = m.groupValues[2].toIntOrNull()
        val d = m.groupValues[3].toIntOrNull()
        if (y != null && mo != null && d != null) {
            runCatching { LocalDate.of(y, mo, d) }.getOrNull()?.let { return it }
        }
    }
    Regex("(\\d{4})\\.\\s*([\\p{L}]+)\\.?\\s*(\\d{1,2})\\.?", RegexOption.IGNORE_CASE).find(text)?.let { m ->
        val y = m.groupValues[1].toIntOrNull()
        val monthToken = m.groupValues[2].lowercase(java.util.Locale.ROOT)
        val d = m.groupValues[3].toIntOrNull()
        val mo = monthNumberFromToken(monthToken)
        if (y != null && mo != null && d != null) {
            runCatching { LocalDate.of(y, mo, d) }.getOrNull()?.let { return it }
        }
    }
    return null
}

private fun monthNumberFromToken(token: String): Int? = when {
    token.startsWith("jan") || token.startsWith("ian") -> 1
    token.startsWith("feb") -> 2
    token.startsWith("már") || token.startsWith("mar") || token.startsWith("mär") -> 3
    token.startsWith("ápr") || token.startsWith("apr") -> 4
    token.startsWith("máj") || token.startsWith("maj") || token.startsWith("may") || token.startsWith("mai") -> 5
    token.startsWith("jún") || token.startsWith("jun") || token.startsWith("iun") -> 6
    token.startsWith("júl") || token.startsWith("jul") || token.startsWith("iul") -> 7
    token.startsWith("aug") -> 8
    token.startsWith("szep") || token.startsWith("sep") -> 9
    token.startsWith("okt") || token.startsWith("oct") -> 10
    token.startsWith("nov") -> 11
    token.startsWith("dec") || token.startsWith("dez") -> 12
    else -> null
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
