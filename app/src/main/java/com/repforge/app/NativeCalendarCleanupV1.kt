package com.repforge.app

import androidx.room.withTransaction
import com.repforge.app.data.db.KeyValueDocumentEntity
import com.repforge.app.data.db.TrainPilotDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** One-time repair policy for the 2026 calendar duplication incident.
 * The marker is portable so stale Drive snapshots cannot resurrect removed schedule rows.
 */
internal object NativeCalendarCleanupV1 {
    const val JSON_KEY = "calendarCleanupV1"
    const val DOC_KEY = "native:calendarCleanupV1"
    private val cutoff = LocalDate.parse("2026-11-01")

    data class Report(val before: Int, val after: Int, val removed: Int)

    fun marker(now: Long = System.currentTimeMillis()): JSONObject =
        JSONObject()
            .put("version", 1)
            .put("cutoffInclusive", cutoff.toString())
            .put("onePerDayFrom", cutoff.plusDays(1).toString())
            .put("updatedAt", now)

    fun newestMarker(vararg roots: JSONObject?): JSONObject? {
        var best: JSONObject? = null
        var bestStamp = Long.MIN_VALUE
        for (root in roots) {
            val candidate = root?.optJSONObject(JSON_KEY) ?: continue
            if (candidate.optInt("version") != 1) continue
            val stamp = candidate.optLong("updatedAt", 0L)
            if (best == null || stamp > bestStamp) {
                best = JSONObject(candidate.toString())
                bestStamp = stamp
            }
        }
        return best
    }

    fun filterScheduled(input: JSONArray?, marker: JSONObject?): JSONArray {
        if (input == null || marker?.optInt("version") != 1) return input?.let { JSONArray(it.toString()) } ?: JSONArray()
        val cutoffDate = runCatching { LocalDate.parse(marker.getString("cutoffInclusive")) }.getOrNull() ?: return JSONArray(input.toString())
        val byDay = LinkedHashMap<String, JSONObject>()
        for (i in 0 until input.length()) {
            val row = input.optJSONObject(i) ?: continue
            val day = rowDay(row) ?: continue
            val date = runCatching { LocalDate.parse(day) }.getOrNull() ?: continue
            if (!date.isAfter(cutoffDate)) continue
            val current = byDay[day]
            if (current == null || rank(row) > rank(current) ||
                rank(row) == rank(current) && row.optLong("updatedAt", 0L) > current.optLong("updatedAt", 0L)) {
                byDay[day] = JSONObject(row.toString())
            }
        }
        val out = JSONArray()
        byDay.toSortedMap().values.forEach { out.put(it) }
        return out
    }

    suspend fun applyLocal(context: android.content.Context): Report {
        val db = TrainPilotDatabase.getInstance(context.applicationContext)
        return NativeDataReplacement.mutate(context.applicationContext) {
            db.withTransaction {
                val before = db.calendarScheduleDao().getAllEntries()
                val now = System.currentTimeMillis()
                val policy = marker(now)
                val source = JSONArray()
                before.forEach { row ->
                    source.put((runCatching { JSONObject(row.rawJson) }.getOrNull() ?: JSONObject())
                        .put("id", row.id)
                        .put("scheduleId", row.id)
                        .put("googleEventId", row.googleEventId)
                        .put("date", row.date)
                        .put("start", row.startIso)
                        .put("plannedStart", row.startIso)
                        .put("end", row.endIso)
                        .put("programId", row.programId)
                        .put("programName", row.programName)
                        .put("dayId", row.dayId)
                        .put("dayName", row.dayName)
                        .put("status", row.status)
                        .put("updatedAt", row.updatedAt))
                }
                val keptJson = filterScheduled(source, policy)
                val keptIds = (0 until keptJson.length()).mapNotNull { keptJson.optJSONObject(it)?.optString("id") }.toSet()
                val keptRows = before.filter { it.id in keptIds }

                db.calendarScheduleDao().deleteAllEntries()
                if (keptRows.isNotEmpty()) db.calendarScheduleDao().upsertEntries(keptRows)

                val stateDoc = db.keyValueDocumentDao().getDocument("native:calendarState")
                if (stateDoc != null) {
                    val state = runCatching { JSONObject(stateDoc.json) }.getOrDefault(JSONObject())
                    val filteredEvents = filterScheduled(state.optJSONArray("events"), policy)
                    state.put("events", filteredEvents)
                    state.put("undoStack", JSONArray())
                    state.put("updatedAt", now)
                    db.keyValueDocumentDao().putDocument(KeyValueDocumentEntity("native:calendarState", state.toString(), now))
                }
                db.keyValueDocumentDao().putDocument(KeyValueDocumentEntity(DOC_KEY, policy.toString(), now))
                Report(before.size, keptRows.size, before.size - keptRows.size)
            }
        }.also {
            context.applicationContext.getSharedPreferences("repforge-google", android.content.Context.MODE_PRIVATE)
                .edit().remove("calendarConfirmedFingerprints").remove("lastCalendarSyncIso").remove("lastCalendarSyncCount").commit()
        }
    }

    private fun rowDay(row: JSONObject): String? {
        for (key in listOf("date", "dateIso", "start", "plannedStart")) {
            val raw = row.optString(key).trim()
            if (raw.length >= 10) {
                val candidate = raw.substring(0, 10)
                if (runCatching { LocalDate.parse(candidate) }.isSuccess) return candidate
            }
        }
        return null
    }

    private fun rank(row: JSONObject): Int {
        val status = row.optString("status").lowercase()
        var score = when (status) {
            "completed" -> 400
            "planned" -> 300
            "skipped", "cancelled", "missed" -> 0
            else -> 100
        }
        if (row.optString("programName").isNotBlank()) score += 30
        if (row.optString("dayName").isNotBlank()) score += 20
        if (row.optString("googleEventId").matches(Regex("rf[0-9a-f]{64}"))) score += 10
        return score
    }
}
