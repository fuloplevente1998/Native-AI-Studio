package com.repforge.app

import com.repforge.app.data.db.CalendarScheduleEntity
import com.repforge.shared.LegacySyncIdentity18
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate

/**
 * One plan per persisted calendar row, including old cancellation records.
 * 2806: only planned workouts from today to [WINDOW_DAYS] ahead are kept in Google Calendar; past and far-future
 * TrainPilot events are removed from the managed calendar (the local plan and the journal are untouched).
 */
internal object NativeCalendarSyncPlan {
    const val WINDOW_DAYS = 30L
    data class Operation(val id: String, val delete: Boolean, val payload: JSONObject?)

    fun inWindow(date: String, today: LocalDate, windowDays: Long = WINDOW_DAYS): Boolean {
        val day = runCatching { LocalDate.parse(date.take(10)) }.getOrNull() ?: return true
        return !day.isBefore(today) && !day.isAfter(today.plusDays(windowDays))
    }

    fun operations(entries: List<CalendarScheduleEntity>, today: LocalDate = LocalDate.now()): Sequence<Operation> =
        NativeCalendarCleanup202611.dedupeSameStartSlot(entries).asSequence().map { entry ->
        val id = entry.googleEventId.takeIf { Regex("rf[0-9a-f]{64}").matches(it) }
            ?: LegacySyncIdentity18.calendarEventId(entry.id, entry.date, entry.programId, entry.dayId)
        if (entry.status == "skipped" || !inWindow(entry.date, today)) Operation(id, true, null)
        else {
            val start = entry.startIso.ifBlank { "${entry.date}T18:00:00Z" }
            val end = entry.endIso.ifBlank {
                runCatching { Instant.parse(start).plusSeconds(3600).toString() }.getOrDefault("${entry.date}T19:00:00Z")
            }
            Operation(id, false, JSONObject().put("id", id)
                .put("summary", "TrainPilot: ${entry.programName} – ${entry.dayName}".trim(' ', '–'))
                .put("description", "Státusz: ${entry.status}")
                .put("start", JSONObject().put("dateTime", start)).put("end", JSONObject().put("dateTime", end)))
        }
    }.groupBy { it.id }.values.asSequence().map { sameId ->
        // A live entry must not be deleted because a stale duplicate was skipped.
        sameId.filterNot { it.delete }.maxByOrNull { it.payload?.optJSONObject("start")?.optString("dateTime").orEmpty() }
            ?: sameId.first()
    }
}
