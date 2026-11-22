package com.repforge.app.ui.health

import com.repforge.app.HealthJournalStore
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.json.JSONObject

internal data class HourlyStepReading(val start: Instant, val end: Instant, val count: Long)
internal data class HourlySteps(val hour: Int, val count: Long?, val future: Boolean)

/** A record is usable only when its whole interval belongs to one elapsed wall-clock hour. */
internal fun hourlySteps(
    day: LocalDate,
    zone: ZoneId,
    now: Instant,
    raw: List<HourlyStepReading>
): List<HourlySteps> {
    val start = day.atStartOfDay(zone).toInstant()
    val end = day.plusDays(1).atStartOfDay(zone).toInstant()
    val readings = raw.filter { it.count >= 0 && it.start <= it.end && it.end <= now &&
        it.start < end && (it.end > start || it.start >= start) }.distinct()
    val segments = stepHourWindows(day, zone).groupBy { (a, _) -> a.atZone(zone).hour }
    return (0..23).map { hour ->
        val windows = segments[hour].orEmpty()
        val future = windows.isNotEmpty() && windows.all { it.first >= now }
        val relevant = readings.filter { row ->
            windows.any { (a, b) -> row.start < b && (row.end > a || row.start >= a) }
        }.sortedWith(compareBy(HourlyStepReading::start, HourlyStepReading::end))
        val complete = relevant.isNotEmpty() && relevant.all { row ->
            windows.any { (a, b) -> row.start >= a && row.start < b && row.end <= b }
        }
        val overlaps = relevant.zipWithNext().any { (a, b) ->
            a.end > b.start || a.start == b.start
        }
        val total = if (complete && !overlaps) {
            relevant.fold(0L) { sum, row -> if (Long.MAX_VALUE - sum >= row.count) sum + row.count else Long.MAX_VALUE }
        } else null
        HourlySteps(hour, total, future)
    }
}

private fun stepHourWindows(day: LocalDate, zone: ZoneId): List<Pair<Instant, Instant>> {
    val start = day.atStartOfDay(zone).toInstant()
    val end = day.plusDays(1).atStartOfDay(zone).toInstant()
    val hourStarts = (0..23).map { hour ->
        day.atTime(hour, 0).let { local -> zone.rules.getValidOffsets(local).map(local::toInstant) }
    }
    // Real offsets account for skipped and repeated hours. Transitions at a partial hour are
    // additional boundaries so a record can never straddle a change in the wall-clock hour.
    val boundaries = (hourStarts.flatten() + listOf(start, end) +
        listOfNotNull(zone.rules.nextTransition(start.minusNanos(1))?.instant?.takeIf { it in start..end }))
        .distinct().sorted()
    return boundaries.zipWithNext()
}

/** Use one source of observed intervals, without prorating multi-hour totals or combining apps. */
internal fun observedHealthConnectStepHours(
    day: LocalDate, zone: ZoneId, now: Instant, bySource: Map<String, List<HourlyStepReading>>
): List<HourlySteps> {
    val windows = stepHourWindows(day, zone)
    return bySource.toSortedMap().values.map { rows ->
        hourlySteps(day, zone, now, rows.filter { reading ->
            windows.any { (from, to) -> reading.start >= from && reading.start < to && reading.end <= to }
        })
    }.maxByOrNull { hours -> hours.count { it.count != null } }
        ?: hourlySteps(day, zone, now, emptyList())
}

internal fun readHourlySteps(
    records: NativeHealthRecords,
    day: LocalDate,
    provider: String,
    origin: String,
    zone: ZoneId,
    now: Instant
): List<HourlySteps> {
    // Include records beginning before midnight, so their unresolved portion is not counted twice.
    var query = HealthRecordQuery(day.minusDays(1), provider, origin, "StepsRecord", endDay = day)
    val valuesBySource = linkedMapOf<String, MutableList<HourlyStepReading>>()
    val start = day.atStartOfDay(zone).toInstant()
    repeat(200) {
        val page = records.load(query)
        for (row in page.rows) {
            if (row.startMs <= 0 || row.endMs < row.startMs) continue
            val reading = HourlyStepReading(Instant.ofEpochMilli(row.startMs), Instant.ofEpochMilli(row.endMs),
                NativeHealthRecords.number(JSONObject(row.data), "value")
                    ?.takeIf { it >= 0 && it < Long.MAX_VALUE.toDouble() && it % 1.0 == 0.0 }?.toLong() ?: continue)
            if (reading.end < start || (reading.end == start && reading.start < start)) continue
            valuesBySource.getOrPut(row.source) { mutableListOf() }.add(reading)
        }
        val next = page.next
        if (next == null) {
            if (provider == HealthJournalStore.HC)
                return observedHealthConnectStepHours(day, zone, now, valuesBySource)
            return hourlySteps(day, zone, now, valuesBySource.values.singleOrNull().orEmpty())
        }
        query = query.copy(cursor = next, generation = page.generation)
    }
    // The first 5,000 records alone cannot prove there are no overlapping older rows.
    return hourlySteps(day, zone, now, emptyList())
}
