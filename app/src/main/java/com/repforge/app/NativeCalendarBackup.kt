package com.repforge.app

import androidx.room.withTransaction
import com.repforge.app.data.db.CalendarScheduleEntity
import com.repforge.app.data.db.KeyValueDocumentEntity
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.shared.LegacySyncIdentity18
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale
import java.util.UUID

/** The portable calendar uses one row per ID. Studio's display document is rebuilt on import. */
internal object NativeCalendarBackup {
    const val STATE_KEY = "native:calendarState"
    const val STUDIO_SETTINGS_KEY = "native:studioPlannerSettings"
    const val VERSION_KEY = "calendarScheduleVersion"
    const val VERSION = 1
    private val googleEventIdPattern = Regex("rf[0-9a-f]{64}")

    fun validate(data: JSONObject) {
        if (data.optInt(VERSION_KEY) == VERSION) {
            val scheduled = requireNotNull(data.optJSONArray("scheduled")) { "Missing backup calendar schedule." }
            for (index in 0 until scheduled.length()) {
                val row = scheduled.getJSONObject(index)
                require(runCatching { LocalDate.parse(scheduleDate(row)) }.isSuccess) {
                    "Invalid backup calendar entry."
                }
            }
        }
    }

    suspend fun exportInto(database: TrainPilotDatabase, out: JSONObject) {
        val rows = database.withTransaction { persistedRows(database) }.sortedWith(compareBy({ it.startIso }, { it.id }))
        out.put(VERSION_KEY, VERSION).put("scheduled", JSONArray(rows.map { row ->
            runCatching { JSONObject(row.rawJson) }.getOrElse { JSONObject() }
                .put("id", row.id).put("scheduleId", row.id).put("googleEventId", row.googleEventId)
                .put("date", row.date).put("start", row.startIso).put("plannedStart", row.startIso)
                .put("end", row.endIso).put("programId", row.programId).put("programName", row.programName)
                .put("dayId", row.dayId).put("dayName", row.dayName).put("status", row.status)
                .put("updatedAt", row.updatedAt)
        }))
    }

    /** Caller holds the same Room transaction used to install the backup. */
    suspend fun restore(database: TrainPilotDatabase, data: JSONObject, replaceAll: Boolean) {
        val scheduled = data.optJSONArray("scheduled") ?: return
        val fullSnapshot = replaceAll && data.optInt(VERSION_KEY) == VERSION
        val stamp = System.currentTimeMillis()
        val cleaned = NativeCalendarCleanup202611.cleanScheduledJson(scheduled, preserveCancellations = true)
        val incoming = (0 until cleaned.length()).mapNotNull { readScheduleRow(cleaned.getJSONObject(it), stamp) }
            .sortedBy { it.updatedAt }.associateBy { it.id }.values
        if (!fullSnapshot && incoming.isEmpty()) return
        val dao = database.calendarScheduleDao()
        val rows = if (fullSnapshot) {
            dao.deleteAllEntries()
            incoming.toList()
        } else {
            val local = persistedRows(database).associateBy { it.id }
            // Old exports lacked complete coverage. Keep local IDs that aren't in the file,
            // and keep a newer local change (including an explicit cancellation) for a shared ID.
            val merged = local.toMutableMap()
            val installedSlots = local.values.filterNot(::cancelled).groupBy { row ->
                NativeCalendarCleanup202611.slotKey(row.date, row.startIso)
            }
            for (row in incoming) {
                val previous = local[row.id]
                val slot = NativeCalendarCleanup202611.slotKey(row.date, row.startIso)
                if (!cancelled(row) && slot != null && installedSlots[slot].orEmpty().any { it.id != row.id }) continue
                if (previous == null || previous.updatedAt <= row.updatedAt) merged[row.id] = row
            }
            merged.values.toList()
        }
        dao.upsertEntries(rows)
        val events = rows.sortedWith(compareBy({ it.startIso }, { it.id }))
            .filterNot(::cancelled)
            .map(NativeCalendarCleanup202611::toCalendarStateEvent)
        val state = JSONObject().put("events", JSONArray(events)).put("updatedAt", stamp)
        // Undo belongs to the previous editor session and could reintroduce replaced rows.
        database.keyValueDocumentDao().putDocument(KeyValueDocumentEntity(STATE_KEY, state.toString(), stamp))
    }

    private fun cancelled(row: CalendarScheduleEntity): Boolean =
        row.status.equals("cancelled", true) ||
            runCatching { JSONObject(row.rawJson).optBoolean("cancelled") }.getOrDefault(false)

    private suspend fun persistedRows(database: TrainPilotDatabase): List<CalendarScheduleEntity> {
        val rows = database.calendarScheduleDao().getAllEntries()
        val state = database.keyValueDocumentDao().getDocument(STATE_KEY) ?: return rows
        val events = runCatching { JSONObject(state.json).optJSONArray("events") }.getOrNull() ?: return rows
        val byId = rows.associateByTo(LinkedHashMap()) { it.id }
        val zone = ZoneId.systemDefault()
        val legacyMonth = YearMonth.from(Instant.ofEpochMilli(state.updatedAt).atZone(zone))
        val visibleIds = HashSet<String>()
        for (index in 0 until events.length()) {
            val event = events.optJSONObject(index) ?: continue
            val id = event.optString("id")
            if (id.isBlank() || !visibleIds.add(id)) continue
            // Old installs could have the Studio event without a corresponding scheduled row.
            if (id !in byId) stateOnlyRow(event, legacyMonth, zone, state.updatedAt)?.let { byId[id] = it }
        }
        return byId.values.map { row ->
            val raw = runCatching { JSONObject(row.rawJson) }.getOrElse { JSONObject() }
            // Older Studio deletes left a skipped row for Google Calendar but removed the event.
            if (row.status == "skipped" && row.id !in visibleIds && raw.optString("status") in setOf("PLANNED", "MISSED")) {
                raw.put("cancelled", true)
            }
            row.copy(rawJson = raw.toString())
        }
    }

    private fun stateOnlyRow(event: JSONObject, month: YearMonth, zone: ZoneId, stamp: Long): CalendarScheduleEntity? {
        val date = runCatching {
            val stored = event.optString("dateIso")
            if (stored.isNotBlank()) LocalDate.parse(stored)
            else month.atDay(event.getInt("dayOfMonth").coerceIn(1, month.lengthOfMonth()))
        }.getOrNull() ?: return null
        val time = runCatching { LocalTime.parse(event.getString("timeLabel")) }.getOrNull() ?: return null
        val duration = event.optInt("durationMinutes").takeIf { it > 0 } ?: return null
        val start = date.atTime(time).atZone(zone).toInstant()
        val status = when (event.optString("status").uppercase(Locale.ROOT)) {
            "COMPLETED" -> "completed"
            "MISSED", "SKIPPED" -> "skipped"
            else -> "planned"
        }
        val title = event.optString("titleHu")
        return CalendarScheduleEntity(
            id = event.getString("id"), googleEventId = "", date = date.toString(),
            startIso = start.toString(), endIso = start.plusSeconds(duration * 60L).toString(),
            programId = event.optString("programId"), programName = title,
            dayId = event.optString("dayId").ifBlank { event.optString("splitBadge") }, dayName = title,
            status = status, updatedAt = stamp, rawJson = event.toString()
        )
    }

    private fun scheduleDate(raw: JSONObject): String =
        raw.optString("date").ifBlank { raw.optString("start", raw.optString("plannedStart")).take(10) }.trim()

    private fun readScheduleRow(raw: JSONObject, stamp: Long): CalendarScheduleEntity? {
        val start = raw.optString("start", raw.optString("plannedStart", "")).trim()
        val date = scheduleDate(raw)
        if (runCatching { LocalDate.parse(date) }.isFailure) return null
        val id = raw.optString("id", raw.optString("scheduleId", "")).ifBlank {
            "legacy-schedule-" + UUID.nameUUIDFromBytes(raw.toString().toByteArray(Charsets.UTF_8))
        }
        if (raw.optString("status").equals("cancelled", true)) raw.put("cancelled", true)
        val programId = raw.optString("programId")
        val dayId = raw.optString("dayId", raw.optString("workout", ""))
        val storedGoogleId = raw.optString("googleEventId")
        val googleId = if (!raw.isNull("googleEventId") && storedGoogleId.isEmpty()) {
            ""
        } else {
            storedGoogleId.takeIf { it.matches(googleEventIdPattern) }
                ?: LegacySyncIdentity18.calendarEventId(id, date, programId, dayId)
        }
        return CalendarScheduleEntity(
            id = id, googleEventId = googleId, date = date, startIso = start, endIso = raw.optString("end"),
            programId = programId, programName = raw.optString("programName"),
            dayId = dayId, dayName = raw.optString("dayName", dayId),
            status = when (raw.optString("status").lowercase(Locale.ROOT)) {
                "completed" -> "completed"
                "skipped", "missed", "cancelled" -> "skipped"
                else -> "planned"
            }, updatedAt = raw.optLong("updatedAt", stamp),
            rawJson = raw.toString()
        )
    }
}
