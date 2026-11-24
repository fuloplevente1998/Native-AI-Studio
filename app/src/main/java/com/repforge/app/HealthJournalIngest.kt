package com.repforge.app

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.TreeSet
import com.repforge.app.HealthJournalStore.Companion.HC
import com.repforge.app.HealthJournalStore.Companion.BLE
import com.repforge.app.HealthJournalStore.Companion.LEGACY
import com.repforge.app.HealthJournalStore.Companion.SAMSUNG
import com.repforge.app.HealthJournalStore.Companion.LOCK
import com.repforge.app.HealthJournalStore.Companion.day
import com.repforge.app.HealthJournalStore.Companion.iso
import com.repforge.app.HealthJournalStore.Companion.validDay
import com.repforge.app.HealthJournalStore.Companion.time
import com.repforge.app.HealthJournalStore.Companion.text

@Throws(Exception::class)
internal fun upsert(db: SQLiteDatabase, r: JSONObject) {
    val channel = text(r, "channel", 32)
    val source = text(r, "source", 255)
    val type = text(r, "type", 80)
    val id = text(r, "recordId", 200)
    if (channel != HC && channel != BLE && channel != SAMSUNG) {
        throw IllegalArgumentException("Invalid health channel")
    }
    val modified = time(r, "lastModifiedMs")
    val start = time(r, "startMs")
    val end = time(r, "endMs")
    if (end < start || !validDay(r.getString("day")) || r.toString().length > 2 * 1024 * 1024) {
        throw IllegalArgumentException("Invalid health record")
    }
    r.getJSONObject("data")
    if (channel == HC) {
        db.rawQuery("SELECT deleted_ms FROM deletions WHERE record_id=?", arrayOf(id)).use { c ->
            if (c.moveToFirst() && c.getLong(0) >= modified) return
        }
    }
    val v = ContentValues().apply {
        put("channel", channel)
        put("source", source)
        put("type", type)
        put("record_id", id)
        put("sort_ms", if ("SleepSessionRecord" == type) end else start)
        put("modified_ms", modified)
        put("start_ms", start)
        put("end_ms", end)
        put("day", r.getString("day"))
        put("json", r.toString())
    }
    db.insertWithOnConflict("records", null, v, SQLiteDatabase.CONFLICT_IGNORE)
    db.update(
        "records",
        v,
        "channel=? AND type=? AND record_id=? AND modified_ms<=?",
        arrayOf(channel, type, id, modified.toString())
    )
}

@Throws(Exception::class)
internal fun HealthJournalStore.importRecords(records: JSONArray, generation: Long) {
    transaction { db ->
        writable(db, generation)
        for (i in 0 until records.length()) {
            upsert(db, records.getJSONObject(i))
        }
        null
    }
}

@Throws(Exception::class)
internal fun HealthJournalStore.reconcileRecords(
    records: JSONArray,
    type: String,
    start: Long,
    end: Long,
    readAt: Long,
    generation: Long
) {
    transaction { db ->
        writable(db, generation)
        val seen = HashSet<String>()
        for (i in 0 until records.length()) {
            val r = records.getJSONObject(i)
            seen.add(r.getString("recordId"))
            upsert(db, r)
        }
        if (start >= readAt - 28L * 86_400_000L) {
            val removed = ArrayList<Array<String>>()
            db.rawQuery(
                "SELECT record_id,day FROM records WHERE channel=? AND type=? AND start_ms>=? AND start_ms<? AND end_ms<=? AND modified_ms<=?",
                arrayOf(HC, type, start.toString(), end.toString(), end.toString(), readAt.toString())
            ).use { c ->
                while (c.moveToNext()) {
                    if (!seen.contains(c.getString(0))) {
                        removed.add(arrayOf(c.getString(0), c.getString(1)))
                    }
                }
            }
            for (row in removed) {
                db.delete("days", "day=? AND channel=? AND source<>'@aggregate'", arrayOf(row[1], HC))
                db.delete("records", "channel=? AND type=? AND record_id=?", arrayOf(HC, type, row[0]))
                val vDel = ContentValues().apply {
                    put("record_id", row[0])
                    put("deleted_ms", readAt)
                }
                db.insertWithOnConflict("deletions", null, vDel, SQLiteDatabase.CONFLICT_REPLACE)
                val vDirty = ContentValues().apply { put("day", row[1]) }
                db.insertWithOnConflict("dirty_days", null, vDirty, SQLiteDatabase.CONFLICT_IGNORE)
            }
        }
        null
    }
}

/** Keep a successfully read day retryable if its external Room publication failed.
 * The existing full/delta sync already consumes dirty_days; no extra scheduler or token.
 */
internal fun HealthJournalStore.markExternalImportDirtyDay(day: String, generation: Long) {
    require(validDay(day))
    transaction { db ->
        writable(db, generation)
        db.insertWithOnConflict("dirty_days", null, ContentValues().apply { put("day", day) },
            SQLiteDatabase.CONFLICT_IGNORE)
        null
    }
}

internal fun HealthJournalStore.nativeChangeRevision(expectedGeneration: Long): Long = synchronized(LOCK) {
    writable(readableDatabase, expectedGeneration)
    meta(readableDatabase, "hc_change_revision", "0").toLong()
}

/** Atomic local epoch check + local write. No provider IPC is allowed in writer. */
internal fun HealthJournalStore.nativePublishAtChangeRevision(expectedGeneration: Long, revision: Long, writer: () -> Unit) {
    synchronized(LOCK) {
        check(nativeChangeRevision(expectedGeneration) == revision) { "Health changes superseded this daily read" }
        writer()
    }
}

private fun affectedSyncDays(days: MutableSet<String>, start: Long, end: Long, day: String) {
    days.add(day)
    val zone = java.time.ZoneId.systemDefault()
    var cursor = Instant.ofEpochMilli(start).atZone(zone).toLocalDate()
    val last = Instant.ofEpochMilli(end).atZone(zone).toLocalDate()
    check(java.time.temporal.ChronoUnit.DAYS.between(cursor, last) <= 530) { "Health interval exceeds sync budget" }
    while (cursor <= last) { days.add(cursor.toString()); cursor = cursor.plusDays(1) }
}

@Throws(Exception::class)
internal fun HealthJournalStore.applyChanges(records: JSONArray, deleted: JSONArray, generation: Long): JSONArray {
    return transaction { db ->
        writable(db, generation)
        val dirty = TreeSet<String>()
        for (i in 0 until deleted.length()) {
            val d = deleted.getJSONObject(i)
            val id = text(d, "recordId", 200)
            val at = time(d, "deletedMs")
            db.rawQuery("SELECT day,start_ms,end_ms FROM records WHERE channel=? AND record_id=?", arrayOf(HC, id)).use { c ->
                while (c.moveToNext()) affectedSyncDays(dirty, c.getLong(1), c.getLong(2), c.getString(0))
            }
            val v = ContentValues().apply {
                put("record_id", id)
                put("deleted_ms", at)
            }
            db.insertWithOnConflict("deletions", null, v, SQLiteDatabase.CONFLICT_IGNORE)
            db.update("deletions", v, "record_id=? AND deleted_ms<=?", arrayOf(id, at.toString()))
            db.delete("records", "channel=? AND record_id=? AND modified_ms<=?", arrayOf(HC, id, at.toString()))
        }
        for (i in 0 until records.length()) {
            val r = records.getJSONObject(i)
            db.rawQuery(
                "SELECT day,start_ms,end_ms FROM records WHERE channel=? AND type=? AND record_id=?",
                arrayOf(HC, r.getString("type"), r.getString("recordId"))
            ).use { c ->
                if (c.moveToFirst()) affectedSyncDays(dirty, c.getLong(1), c.getLong(2), c.getString(0))
            }
            upsert(db, r)
            affectedSyncDays(dirty, r.getLong("startMs"), r.getLong("endMs"), r.getString("day"))
        }
        for (dayStr in dirty) {
            db.delete("days", "day=? AND channel=? AND source<>'@aggregate'", arrayOf(dayStr, HC))
            val v = ContentValues().apply { put("day", dayStr) }
            db.insertWithOnConflict("dirty_days", null, v, SQLiteDatabase.CONFLICT_IGNORE)
        }
        if (records.length() > 0 || deleted.length() > 0) {
            putMeta(db, "hc_change_revision", (meta(db, "hc_change_revision", "0").toLong() + 1).toString())
        }
        JSONArray(dirty)
    }
}

@Throws(Exception::class)
internal fun saveDay(db: SQLiteDatabase, day: String, channel: String, source: String, data: JSONObject) {
    if (!validDay(day)) throw IllegalArgumentException("Invalid day")
    val v = ContentValues().apply {
        put("day", day)
        put("channel", channel)
        put("source", source)
        put("json", data.toString())
    }
    db.insertWithOnConflict("days", null, v, SQLiteDatabase.CONFLICT_REPLACE)
}

@Throws(Exception::class)
internal fun HealthJournalStore.healthDay(day: String, result: JSONObject, generation: Long) {
    transaction { db ->
        writable(db, generation)
        val out = JSONObject(result.toString())
        val source = out.optString("source", "@aggregate")
        if (source.isEmpty() || source.length > 255) {
            throw IllegalArgumentException("Invalid daily origin")
        }
        val partial = out.optJSONArray("warnings")?.let { it.length() > 0 } ?: false
        if (partial) {
            db.rawQuery(
                "SELECT json FROM days WHERE day=? AND channel=? AND source=?",
                arrayOf(day, HC, source)
            ).use { c ->
                if (c.moveToFirst()) {
                    val old = JSONObject(c.getString(0))
                    if (old.optString("activityOrigin") == out.optString("activityOrigin")) {
                        val keys = old.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val value = out.opt(key)
                            if (value == null || value === JSONObject.NULL || (value is JSONArray && value.length() == 0)) {
                                out.put(key, old.get(key))
                            }
                        }
                    }
                } else {
                    // First read with only some permissions: retain the metrics genuinely
                    // received, visibly marked partial. An absent metric stays absent, not
                    // an invented zero or a discarded measurement.
                    val dirty = ContentValues().apply { put("day", day) }
                    db.insertWithOnConflict("dirty_days", null, dirty, SQLiteDatabase.CONFLICT_IGNORE)
                }
            }
        }
        out.put("day", day)
        out.put("partial", partial)
        if (!out.has("aggregateOrigin")) {
            out.put("aggregateOrigin", if ("@aggregate" == source) "health_connect_priority" else "health_connect_source")
        }
        saveDay(db, day, HC, source, out)
        if (!partial && "@aggregate" == source) {
            db.delete("dirty_days", "day=?", arrayOf(day))
        }
        null
    }
}

@Throws(Exception::class)
internal fun HealthJournalStore.samsungDay(day: String, result: JSONObject, records: JSONArray, generation: Long) {
    transaction { db ->
        writable(db, generation)
        val out = JSONObject(result.toString())
        val failed = out.optJSONArray("failedFields")
        if (failed != null && failed.length() > 0) {
            db.rawQuery(
                "SELECT json FROM days WHERE day=? AND channel=? AND source='@aggregate'",
                arrayOf(day, SAMSUNG)
            ).use { c ->
                if (c.moveToFirst()) {
                    val previous = JSONObject(c.getString(0))
                    for (i in 0 until failed.length()) {
                        val key = failed.getString(i)
                        if (previous.has(key)) out.put(key, previous.get(key))
                    }
                }
            }
        }
        out.put("day", day)
            .put("source", "@aggregate")
            .put("channel", SAMSUNG)
            .put("partial", failed != null && failed.length() > 0)
        for (i in 0 until records.length()) {
            val record = records.getJSONObject(i)
            if (SAMSUNG != record.optString("channel")) {
                throw IllegalArgumentException("Invalid Samsung record")
            }
            upsert(db, record)
        }
        saveDay(db, day, SAMSUNG, "@aggregate", out)
        null
    }
}

@Throws(Exception::class)
internal fun HealthJournalStore.watchSteps(
    deviceId: String,
    name: String,
    count: Long,
    requestAt: Long,
    observedAt: Long,
    generation: Long
) {
    if (count < 0L || count > 1_000_000L || day(requestAt) != day(observedAt)) {
        throw IllegalArgumentException("Watch reading crossed midnight or is invalid")
    }
    transaction { db ->
        writable(db, generation)
        if (!meta(db, "logWatchSteps", "false").toBoolean()) return@transaction null
        val data = JSONObject()
            .put("steps", count)
            .put("unit", "steps")
            .put("observedAt", iso(observedAt))
            .put("dateBasis", "phone_observation")
            .put("deviceName", name)
        val r = JSONObject()
            .put("channel", BLE)
            .put("source", deviceId)
            .put("type", "StepsSnapshot")
            .put("recordId", "$deviceId:${day(observedAt)}")
            .put("lastModifiedMs", observedAt)
            .put("startMs", observedAt)
            .put("endMs", observedAt)
            .put("day", day(observedAt))
            .put("data", data)
        upsert(db, r)
        saveDay(
            db,
            day(observedAt),
            BLE,
            deviceId,
            JSONObject(data.toString()).put("day", day(observedAt)).put("readAt", iso(observedAt))
        )
        null
    }
}

@Throws(Exception::class)
internal fun HealthJournalStore.initialize(legacy: JSONObject?): JSONObject {
    transaction { db ->
        if (meta(db, "restore_pending", "").isNotEmpty()) return@transaction null
        if (meta(db, "migrated_v1", "false") != "true") {
            val days = legacy?.optJSONObject("days")
            if (days != null) {
                val keys = days.keys()
                while (keys.hasNext()) {
                    val dayStr = keys.next()
                    if (!validDay(dayStr)) throw IllegalArgumentException("Invalid legacy day")
                    saveDay(db, dayStr, LEGACY, "unknown", days.getJSONObject(dayStr))
                }
            }
            val recovery = legacy?.optJSONArray("recoveryHistory")
            if (recovery != null) {
                for (i in 0 until recovery.length()) {
                    val r = recovery.getJSONObject(i)
                    val dayStr = r.optString("day")
                    if (!validDay(dayStr)) continue
                    val d = days?.optJSONObject(dayStr) ?: JSONObject()
                    d.put("legacyRecovery", r)
                    if (!d.has("hrvRmssdMs")) d.put("hrvRmssdMs", r.opt("hrvRmssdMs"))
                    saveDay(db, dayStr, LEGACY, "unknown", d)
                }
            }
            if (legacy != null) {
                for (key in arrayOf("lastSyncAt", "lastFullSyncAt", "lastAttemptAt", "lastSyncAt_$HC", "lastSyncAt_$SAMSUNG")) {
                    if (legacy.has(key) && !legacy.isNull(key)) {
                        putMeta(db, key, legacy.getString(key))
                    }
                }
            }
            putMeta(db, "migrated_v1", "true")
        }
        null
    }
    return projection()
}

@Throws(Exception::class)
internal fun HealthJournalStore.retireWatchUi() {
    if (pendingRestore().isNotEmpty()) return
    val prefs = preferences()
    val daily = prefs.optString("hcDailySource", "auto")
    if (HC == prefs.optString("primary") && !prefs.optBoolean("logWatchSteps") &&
        !prefs.optBoolean("bleBackground") && (daily == "auto" || daily == "priority")) return
    setPreferences(
        JSONObject()
            .put("primary", HC)
            .put("logWatchSteps", false)
            .put("bleBackground", false)
            .put("hcDailySource", if ("priority" == daily) "priority" else "auto")
    )
}
