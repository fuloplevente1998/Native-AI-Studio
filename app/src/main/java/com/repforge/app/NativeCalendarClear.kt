package com.repforge.app

import android.content.Context
import androidx.room.withTransaction
import com.repforge.app.data.db.CalendarScheduleEntity
import com.repforge.app.data.db.KeyValueDocumentEntity
import com.repforge.app.data.db.TrainPilotDatabase
import org.json.JSONArray
import org.json.JSONObject

/** A confirmed deletion retains the existing cancellation/Google-delete contract. */
internal object NativeCalendarClear {
    data class Preview(val rows: List<CalendarScheduleEntity>, val document: KeyValueDocumentEntity?,
        val planner: KeyValueDocumentEntity?)

    suspend fun preview(db: TrainPilotDatabase): Preview = db.withTransaction {
        Preview(db.calendarScheduleDao().getAllEntries(), db.keyValueDocumentDao().getDocument("native:calendarState"),
            db.keyValueDocumentDao().getDocument("plannerSettings"))
    }

    data class Result(val ids: Set<String>, val count: Int)

    suspend fun commit(context: Context, db: TrainPilotDatabase, expected: Preview,
        ticket: NativeDataReplacement.Continuation): Result = NativeDataReplacement.mutate(context) {
        ticket.requireCurrent(context)
        db.withTransaction {
            check(preview(db) == expected) { "calendarChanged" }
            NativeProgramEditor.assertNoActiveWorkout(context)
            val existingEvents = JSONObject(expected.document?.json ?: "{}").optJSONArray("events") ?: JSONArray()
            val completedIds = (0 until existingEvents.length()).mapNotNull { i ->
                existingEvents.optJSONObject(i)?.takeIf { it.optString("status").equals("completed", true) }?.optString("id")
            }.toSet()
            val removable = expected.rows.filter { row ->
                row.status in setOf("planned", "skipped") && row.id !in completedIds &&
                    !db.workoutHistoryDao().hasScheduleHistory(row.id)
            }
            val stamp = maxOf(System.currentTimeMillis(),
                (expected.rows.maxOfOrNull { it.updatedAt } ?: 0L) + 1,
                (expected.document?.updatedAt ?: 0L) + 1, (expected.planner?.updatedAt ?: 0L) + 1)
            val ids = removable.map { it.id }.toSet()
            val cancelled = removable.filterNot { com.repforge.app.ui.calendar.NativeCalendarRepository.cancelled(it) }.map { row -> row.copy(status = "skipped", updatedAt = stamp,
                rawJson = JSONObject(row.rawJson).put("status", "skipped").put("cancelled", true)
                    .put("updatedAt", stamp).toString()) }
            val doc = JSONObject(expected.document?.json ?: "{}")
            val kept = JSONArray()
            val events = doc.optJSONArray("events") ?: JSONArray()
            for (i in 0 until events.length()) {
                val event = events.getJSONObject(i)
                if (event.optString("id") !in ids) kept.put(event)
            }
            doc.put("events", kept).put("undoStack", JSONArray()).put("updatedAt", stamp)
            ticket.requireCurrent(context)
            NativeProgramEditor.assertNoActiveWorkout(context)
            db.calendarScheduleDao().upsertEntries(cancelled)
            db.keyValueDocumentDao().putDocument(KeyValueDocumentEntity("native:calendarState", doc.toString(), stamp))
            expected.planner?.let { planner ->
                val raw = JSONObject(planner.json).apply {
                    remove("nativePlannerReceipt"); remove("nativePlannerIds"); put("updatedAt", stamp)
                }
                db.keyValueDocumentDao().putDocument(planner.copy(json = raw.toString(), updatedAt = stamp))
            }
            Result(ids, cancelled.size)
        }
    }
}
