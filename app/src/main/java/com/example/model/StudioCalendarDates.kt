package com.example.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

internal object StudioCalendarDates {
    fun window(event: CalendarDayEvent, zone: ZoneId): Pair<Instant, Instant> {
        require(event.durationMinutes in 1..1440)
        val start = LocalDate.parse(event.dateIso).atTime(LocalTime.parse(event.timeLabel)).atZone(zone).toInstant()
        return start to start.plusSeconds(event.durationMinutes * 60L)
    }
}
