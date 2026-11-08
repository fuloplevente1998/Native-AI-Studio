package com.repforge.app

import androidx.room.withTransaction
import com.repforge.app.data.db.CalendarScheduleEntity
import com.repforge.app.data.db.KeyValueDocumentEntity
import com.repforge.app.data.db.TrainPilotDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

/**
 * One-time repair for the calendar duplication incident that affected the user's
 * September-November 2026 TrainPilot/RepForge schedule.
 *
 * Scope is intentionally frozen:
 * - remove every scheduled row through 2026-11-01 inclusive;
 * - from 2026-11-02 through 2026-11-30 keep at most one row per day;
 * - never impose an "one workout per day" rule on December 2026 or later.
 *
 * The same pure filter is reused for backup/Drive merges so an old cloud snapshot
 * cannot resurrect rows removed by this repair.
 */
internal object NativeCalendarCleanup202611 {
    private const val MARKER = "native:calendarCleanup202611"
    private val deleteThrough = LocalDate.parse("2026-11-01")
    private val dedupeThrough = LocalDate.parse("2026-11-30")

    data class Report(val before: Int, val after: Int, val removed: Int)

    fun cleanRows(rows: List<CalendarScheduleEntity>): List<CalendarScheduleEntity> {
        val outside = ArrayList<CalendarScheduleEntity>()
        val byDay = LinkedHashMap<LocalDate, MutableList<CalendarScheduleEntity>>()
        for (row in rows) {
            val date = runCatching { LocalDate.parse(row.date) }.getOrNull()
            when {
                date == null -> outside.add(row)
                !date.isAfter(deleteThrough) -> Unit
                date.isAfter(dedupeThrough) -> outside.add(row)
                else -> byDay.getOrPut(date) { ArrayList() }.add(row)
            }
        }
        val kept = byDay.values.map { group ->
            group.maxWithOrNull(compareBy<CalendarScheduleEntity>({ rowScore(it) }, { it.updatedAt }, { it.id }))
                ?: group.first()
        }
        return (outside + kept).sortedWith(compareBy({ it.date }, { it.startIso }, { it.id }))
    }

    fun cleanScheduledJson(rows: JSONArray?): JSONArray {
        if (rows == null) return JSONArray()
        data class Item(val raw: JSONObject, val date: LocalDate?)
        val outside = ArrayList<Item>()
        val byDay = LinkedHashMap<LocalDate, MutableList<Item>>()
        for (i in 0 until rows.length()) {
            val src = rows.optJSONObject(i) ?: continue
            val copy = JSONObject(src.toString())
            val start = copy.optString("start", copy.optString("plannedStart", "")).trim()
            val dateText = copy.optString("date", if (start.length >= 10) start.substring(0, 10) else "").trim()
            val date = runCatching { LocalDate.parse(dateText) }.getOrNull()
            when {
                date == null -> outside.add(Item(copy, null))
                !date.isAfter(deleteThrough) -> Unit
                date.isAfter(dedupeThrough) -> outside.add(Item(copy, date))
                else -> byDay.getOrPut(date) { ArrayList() }.add(Item(copy, date))
            }
        }
        val selected = ArrayList<Item>()
        selected.addAll(outside)
        for (group in byDay.values) {
            selected.add(group.maxWithOrNull(compareBy<Item>(
                { jsonScore(it.raw) },
                { it.raw.optLong("updatedAt", 0L) },
                { it.raw.optString("id", it.raw.optString("scheduleId", "")) }
            )) ?: group.first())
        }
        selected.sortWith(compareBy(
            { it.date?.toString() ?: it.raw.optString("date", "") },
            { it.raw.optString("start", it.raw.optString("plannedStart", "")) },
            { it.raw.optString("id", it.raw.optString("scheduleId", "")) }
        ))
        return JSONArray().also { out -> selected.forEach { out.put(it.raw) } }
    }

    suspend fun apply(database: TrainPilotDatabase): Report = database.withTransaction {
        val markerDao = database.keyValueDocumentDao()
        val beforeRows = database.calendarScheduleDao().getAllEntries()
        if (markerDao.getDocument(MARKER) != null) {
            return@withTransaction Report(beforeRows.size, beforeRows.size, 0)
        }

        val keptRows = cleanRows(beforeRows)
        val keepIds = keptRows.mapTo(HashSet()) { it.id }
        for (row in beforeRows) {
            if (row.id !in keepIds) database.calendarScheduleDao().deleteEntry(row.id)
        }

        // The Compose calendar reads native:calendarState directly. Keep it aligned
        // with Room and clear undo snapshots that could re-add a removed duplicate.
        val stateDoc = markerDao.getDocument("native:calendarState")
        if (stateDoc != null) {
            val root = runCatching { JSONObject(stateDoc.json) }.getOrNull()
            if (root != null) {
                val oldEvents = root.optJSONArray("events") ?: JSONArray()
                val keptById = keptRows.associateBy { it.id }
                val repaired = JSONArray()
                val seen = HashSet<String>()
                for (i in 0 until oldEvents.length()) {
                    val event = oldEvents.optJSONObject(i) ?: continue
                    val id = event.optString("id")
                    if (id.isNotEmpty() && id in keptById && seen.add(id)) repaired.put(JSONObject(event.toString()))
                }
                for (row in keptRows) {
                    if (seen.add(row.id)) repaired.put(toCalendarStateEvent(row))
                }
                val stamp = System.currentTimeMillis()
                root.put("events", repaired)
                    .put("undoStack", JSONArray())
                    .put("updatedAt", stamp)
                markerDao.putDocument(KeyValueDocumentEntity("native:calendarState", root.toString(), stamp))
            }
        }

        val stamp = System.currentTimeMillis()
        markerDao.putDocument(
            KeyValueDocumentEntity(
                MARKER,
                JSONObject()
                    .put("version", 1)
                    .put("deleteThrough", deleteThrough.toString())
                    .put("dedupeThrough", dedupeThrough.toString())
                    .put("before", beforeRows.size)
                    .put("after", keptRows.size)
                    .put("removed", beforeRows.size - keptRows.size)
                    .toString(),
                stamp
            )
        )
        Report(beforeRows.size, keptRows.size, beforeRows.size - keptRows.size)
    }

    private fun rowScore(row: CalendarScheduleEntity): Int {
        val raw = runCatching { JSONObject(row.rawJson) }.getOrNull()
        var score = statusScore(row.status)
        if (raw?.optBoolean("cancelled", false) == true) score -= 500
        if (row.programName.isNotBlank()) score += 120
        if (row.dayName.isNotBlank()) score += 80
        if (row.programId.isNotBlank()) score += 30
        if (row.dayId.isNotBlank()) score += 30
        if (raw?.optString("titleHu").orEmpty().isNotBlank()) score += 20
        return score
    }

    private fun jsonScore(row: JSONObject): Int {
        var score = statusScore(row.optString("status", "planned"))
        if (row.optBoolean("cancelled", false)) score -= 500
        if (row.optString("programName").isNotBlank()) score += 120
        if (row.optString("dayName").isNotBlank()) score += 80
        if (row.optString("programId").isNotBlank()) score += 30
        if (row.optString("dayId", row.optString("workout", "")).isNotBlank()) score += 30
        if (row.optString("titleHu").isNotBlank()) score += 20
        return score
    }

    private fun statusScore(status: String): Int = when (status.lowercase()) {
        "completed" -> 1_000
        "planned" -> 500
        "skipped", "cancelled", "missed" -> -200
        else -> 0
    }

    private fun toCalendarStateEvent(row: CalendarScheduleEntity): JSONObject {
        val raw = runCatching { JSONObject(row.rawJson) }.getOrElse { JSONObject() }
        val date = runCatching { LocalDate.parse(row.date) }.getOrNull()
        val startInstant = parseInstant(row.startIso)
        val endInstant = parseInstant(row.endIso)
        val timeLabel = raw.optString("timeLabel").ifBlank {
            row.startIso.substringAfter('T', "").take(5).takeIf { it.length == 5 } ?: "10:00"
        }
        val duration = raw.optInt("durationMinutes", 0).takeIf { it > 0 }
            ?: if (startInstant != null && endInstant != null && endInstant.isAfter(startInstant))
                Duration.between(startInstant, endInstant).toMinutes().toInt().coerceAtLeast(1)
            else 60
        val status = when (row.status.lowercase()) {
            "completed" -> "COMPLETED"
            "skipped", "cancelled", "missed" -> "MISSED"
            else -> "PLANNED"
        }
        return JSONObject(raw.toString())
            .put("id", row.id)
            .put("dayOfMonth", date?.dayOfMonth ?: raw.optInt("dayOfMonth", 1))
            .put("dateIso", row.date)
            .put("timeLabel", timeLabel)
            .put("titleHu", raw.optString("titleHu").ifBlank {
                row.dayName.ifBlank { row.programName.ifBlank { "Edzés" } }
            })
            .put("exerciseCount", raw.optInt("exerciseCount", 0))
            .put("durationMinutes", duration)
            .put("status", status)
            .put("splitBadge", raw.optString("splitBadge").ifBlank { row.dayId })
    }

    private fun parseInstant(value: String): Instant? {
        if (value.isBlank()) return null
        return runCatching { Instant.parse(value) }.getOrElse {
            runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()
        }
    }
}
