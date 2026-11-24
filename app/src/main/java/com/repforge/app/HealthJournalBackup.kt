package com.repforge.app

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import com.repforge.app.HealthJournalStore.Companion.HC
import com.repforge.app.HealthJournalStore.Companion.BLE
import com.repforge.app.HealthJournalStore.Companion.LEGACY
import com.repforge.app.HealthJournalStore.Companion.SAMSUNG
import com.repforge.app.HealthJournalStore.Companion.LOCK
import com.repforge.app.HealthJournalStore.Companion.time
import com.repforge.app.HealthJournalStore.Companion.text

@Throws(Exception::class)
internal fun HealthJournalStore.exportSnapshot(): JSONObject {
    return transaction { db ->
        val out = JSONObject().put("schemaVersion", 1)
        val records = JSONArray()
        val days = JSONArray()
        val deleted = JSONArray()
        var size = 0
        db.rawQuery("SELECT json FROM records ORDER BY row_id", null).use { c ->
            while (c.moveToNext()) {
                val json = c.getString(0)
                size += json.length
                if (size > 18 * 1024 * 1024) {
                    throw IllegalStateException("Health export exceeds 18 MB; choose a shorter period in a future export")
                }
                records.put(JSONObject(json))
            }
        }
        db.rawQuery("SELECT day,channel,source,json FROM days ORDER BY day", null).use { c ->
            while (c.moveToNext()) {
                days.put(
                    JSONObject()
                        .put("day", c.getString(0))
                        .put("channel", c.getString(1))
                        .put("source", c.getString(2))
                        .put("data", JSONObject(c.getString(3)))
                )
            }
        }
        db.rawQuery("SELECT record_id,deleted_ms FROM deletions", null).use { c ->
            while (c.moveToNext()) {
                deleted.put(JSONObject().put("recordId", c.getString(0)).put("deletedMs", c.getLong(1)))
            }
        }
        val sync = JSONObject()
        for (key in arrayOf("lastSyncAt", "lastFullSyncAt", "lastAttemptAt", "lastSyncAt_$HC", "lastSyncAt_$SAMSUNG")) {
            val value = meta(db, key, "")
            if (value.isNotEmpty()) sync.put(key, value)
        }
        out.put("records", records).put("days", days).put("deletions", deleted).put("syncMeta", sync)
        BackupJsonIO.requireWithinCharacters(out, 18 * 1024 * 1024)
        out
    }
}

@Throws(Exception::class)
private fun HealthJournalStore.replace(db: SQLiteDatabase, snapshot: JSONObject) {
    if (snapshot.getInt("schemaVersion") != 1) {
        throw IllegalArgumentException("Unsupported health backup")
    }
    val records = snapshot.getJSONArray("records")
    val days = snapshot.getJSONArray("days")
    val deleted = snapshot.optJSONArray("deletions")
    if (records.length() > 100_000 || days.length() > 20_000) {
        throw IllegalArgumentException("Health backup too large")
    }
    BackupJsonIO.requireWithinCharacters(snapshot, 18 * 1024 * 1024)
    db.delete("records", null, null)
    db.delete("days", null, null)
    db.delete("deletions", null, null)
    db.delete("dirty_days", null, null)
    if (deleted != null) {
        for (i in 0 until deleted.length()) {
            val d = deleted.getJSONObject(i)
            val v = ContentValues().apply {
                put("record_id", text(d, "recordId", 200))
                put("deleted_ms", time(d, "deletedMs"))
            }
            db.insertOrThrow("deletions", null, v)
        }
    }
    for (i in 0 until records.length()) {
        upsert(db, records.getJSONObject(i))
    }
    for (i in 0 until days.length()) {
        val d = days.getJSONObject(i)
        val channel = d.getString("channel")
        val source = d.getString("source")
        if (channel !in listOf(HC, BLE, LEGACY, SAMSUNG) || source.isEmpty() || source.length > 255) {
            throw IllegalArgumentException("Invalid day source")
        }
        saveDay(db, d.getString("day"), channel, source, d.getJSONObject("data"))
    }
    val sync = snapshot.optJSONObject("syncMeta")
    for (key in arrayOf("lastSyncAt", "lastFullSyncAt", "lastAttemptAt", "lastSyncAt_$HC", "lastSyncAt_$SAMSUNG")) {
        if (sync != null && !sync.isNull(key)) {
            putMeta(db, key, sync.getString(key))
        } else {
            db.delete("meta", "key=?", arrayOf(key))
        }
    }
    putMeta(db, "migrated_v1", "true")
}

@Throws(Exception::class)
internal fun HealthJournalStore.prepareRestore(snapshot: JSONObject): String {
    return transaction { db ->
        if (meta(db, "restore_pending", "").isNotEmpty()) {
            throw IllegalStateException("Restore already pending")
        }
        val old = exportSnapshot()
        db.execSQL("SAVEPOINT validate_health")
        try {
            replace(db, snapshot)
        } finally {
            db.execSQL("ROLLBACK TO validate_health")
            db.execSQL("RELEASE validate_health")
        }
        val token = UUID.randomUUID().toString()
        putSnapshot(db, "restore_old", old)
        putSnapshot(db, "restore_next", snapshot)
        putMeta(db, "restore_pending", token)
        token
    }
}

@Throws(Exception::class)
internal fun HealthJournalStore.installRestore(token: String) {
    transaction { db ->
        if (token != meta(db, "restore_pending", "")) {
            throw IllegalArgumentException("Invalid restore token")
        }
        replace(db, readSnapshot(db, "restore_next"))
        putMeta(db, "generation", (generation() + 1L).toString())
        null
    }
}

@Throws(Exception::class)
internal fun HealthJournalStore.finishRestore(token: String, commit: Boolean) {
    transaction { db ->
        if (token != meta(db, "restore_pending", "")) {
            throw IllegalArgumentException("Invalid restore token")
        }
        if (!commit) {
            replace(db, readSnapshot(db, "restore_old"))
        }
        db.delete("meta_parts", "key IN ('restore_old','restore_next')", null)
        db.delete("meta", "key IN ('restore_pending','restore_old','restore_next')", null)
        putMeta(db, "generation", (generation() + 1L).toString())
        null
    }
}

internal fun HealthJournalStore.pendingRestore(): String {
    synchronized(LOCK) {
        return meta(readableDatabase, "restore_pending", "")
    }
}

@Throws(Exception::class)
internal fun HealthJournalStore.erase() {
    transaction { db ->
        val epoch = generation() + 1L
        db.delete("records", null, null)
        db.delete("days", null, null)
        db.delete("meta", null, null)
        db.delete("deletions", null, null)
        db.delete("dirty_days", null, null)
        putMeta(db, "generation", epoch.toString())
        putMeta(db, "migrated_v1", "true")
        null
    }
}
