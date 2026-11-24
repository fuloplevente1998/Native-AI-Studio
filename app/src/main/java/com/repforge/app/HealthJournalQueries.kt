package com.repforge.app

import android.database.sqlite.SQLiteDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import kotlin.math.max
import kotlin.math.min
import com.repforge.app.HealthJournalStore.Companion.HC
import com.repforge.app.HealthJournalStore.Companion.BLE
import com.repforge.app.HealthJournalStore.Companion.LEGACY
import com.repforge.app.HealthJournalStore.Companion.SAMSUNG
import com.repforge.app.HealthJournalStore.Companion.LOCK
import com.repforge.app.HealthJournalStore.Companion.day
import com.repforge.app.HealthJournalStore.Companion.iso
import com.repforge.app.HealthJournalStore.Companion.validDay

@Throws(Exception::class)
internal fun HealthJournalStore.projection(): JSONObject {
    synchronized(LOCK) {
        val db = readableDatabase
        val days = JSONObject()
        val primary = meta(db, "primary", HC)
        val keys = ArrayList<String>()
        val allowBle = if (meta(db, "logWatchSteps", "false").toBoolean()) "1" else "0"
        db.rawQuery(
            "SELECT DISTINCT day FROM days WHERE channel<>'ble_watch' OR $allowBle ORDER BY day DESC LIMIT 30",
            null
        ).use { c ->
            while (c.moveToNext()) keys.add(c.getString(0))
        }
        for (dayStr in keys) {
            var hc: JSONObject? = null
            var old: JSONObject? = null
            var ble: JSONObject? = null
            var samsung: JSONObject? = null
            var watchSource: String? = null
            db.rawQuery("SELECT channel,source,json FROM days WHERE day=?", arrayOf(dayStr)).use { c ->
                while (c.moveToNext()) {
                    val channel = c.getString(0)
                    val value = JSONObject(c.getString(2))
                    when {
                        channel == HC && "@aggregate" == c.getString(1) -> hc = value
                        channel == SAMSUNG && "@aggregate" == c.getString(1) -> samsung = value
                        channel == LEGACY -> old = value
                        channel == BLE && (ble == null || value.optString("readAt") > ble!!.optString("readAt")) -> {
                            ble = value
                            watchSource = c.getString(1)
                        }
                    }
                }
            }
            if (hc != null) hc!!.put("sleepSessions", sleepSessions(db, dayStr, ""))
            val preferredProvider = meta(db, "provider", HC)
            val hasSelectedProvider = if (preferredProvider == SAMSUNG) samsung != null
                else hc != null || old != null
            val explicitWatchSteps = ble != null && meta(db, "logWatchSteps", "false").toBoolean()
            if (!hasSelectedProvider && !explicitWatchSteps) continue
            val out = HealthProviderProjection.day(hc, samsung, old, preferredProvider)
            if (ble != null && meta(db, "logWatchSteps", "false").toBoolean() && (primary == BLE || out.isNull("steps"))) {
                out.put("steps", ble!!.get("steps"))
                out.put("stepsSource", BLE)
                out.put("stepsObservedAt", ble!!.optString("readAt"))
                out.put("stepsDeviceId", watchSource)
            }
            db.rawQuery("SELECT day FROM dirty_days WHERE day=?", arrayOf(dayStr)).use { dirty ->
                if (dirty.moveToFirst()) out.put("partial", true)
            }
            out.put("day", dayStr)
            days.put(dayStr, out)
        }
        val out = JSONObject()
            .put("version", 1)
            .put("days", days)
            .put("preferences", preferences())
            .put("nativeJournal", true)
            .put("warnings", JSONArray())
        for (key in arrayOf("lastSyncAt", "lastFullSyncAt", "lastAttemptAt", "lastSyncAt_$HC", "lastSyncAt_$SAMSUNG")) {
            val value = meta(db, key, "")
            out.put(key, if (value.isEmpty()) JSONObject.NULL else value)
        }
        return out
    }
}

@Throws(Exception::class)
private fun sleepSessions(db: SQLiteDatabase, day: String, source: String): JSONArray {
    val result = JSONArray()
    db.rawQuery(
        "SELECT json FROM records WHERE channel=? AND type='SleepSessionRecord' AND day=? AND (?='' OR source=?) ORDER BY sort_ms DESC LIMIT 100",
        arrayOf(HC, day, source, source)
    ).use { c ->
        while (c.moveToNext()) {
            val r = JSONObject(c.getString(0))
            val data = r.getJSONObject("data")
            val stages = JSONArray()
            val raw = data.optJSONArray("stages")
            if (raw != null) {
                for (i in 0 until raw.length()) {
                    val s = raw.getJSONObject(i)
                    stages.put(
                        JSONObject()
                            .put("start", iso(s.getLong("startMs")))
                            .put("end", iso(s.getLong("endMs")))
                            .put("type", s.getInt("type"))
                            .put("isAsleep", s.getBoolean("isAsleep"))
                    )
                }
            }
            result.put(
                JSONObject()
                    .put("id", r.getString("recordId"))
                    .put("source", r.getString("source"))
                    .put("start", iso(r.getLong("startMs")))
                    .put("end", iso(r.getLong("endMs")))
                    .put("durationMinutes", (r.getLong("endMs") - r.getLong("startMs")) / 60_000.0)
                    .put("stages", stages)
            )
        }
    }
    return result
}

internal fun HealthJournalStore.dirtyDays(): JSONArray {
    synchronized(LOCK) {
        val days = JSONArray()
        readableDatabase.rawQuery("SELECT day FROM dirty_days ORDER BY day LIMIT 500", null).use { c ->
            while (c.moveToNext()) days.put(c.getString(0))
        }
        return days
    }
}

@Throws(Exception::class)
internal fun HealthJournalStore.needsSourceDay(date: String, source: String): Boolean {
    synchronized(LOCK) {
        val db = readableDatabase
        val read: Long
        db.rawQuery(
            "SELECT json FROM days WHERE day=? AND channel=? AND source=?",
            arrayOf(date, HC, source)
        ).use { c ->
            if (!c.moveToFirst()) return true
            read = try {
                Instant.parse(JSONObject(c.getString(0)).optString("readAt")).toEpochMilli()
            } catch (_: Exception) {
                return true
            }
        }
        if (date == day(System.currentTimeMillis()) && System.currentTimeMillis() - read > 5 * 60_000L) {
            return true
        }
        db.rawQuery(
            "SELECT MAX(modified_ms) FROM records WHERE day=? AND channel=? AND source=?",
            arrayOf(date, HC, source)
        ).use { c ->
            if (c.moveToFirst() && c.getLong(0) > read) return true
        }
        db.rawQuery("SELECT day FROM dirty_days WHERE day=?", arrayOf(date)).use { c ->
            return c.moveToFirst()
        }
    }
}

@Throws(Exception::class)
internal fun HealthJournalStore.sourceDaySeed(date: String, source: String): JSONObject {
    synchronized(LOCK) {
        val db = readableDatabase
        val out = JSONObject()
        val seen = HashSet<String>()
        db.rawQuery(
            "SELECT type,json FROM records WHERE day=? AND channel=? AND source=? ORDER BY sort_ms DESC",
            arrayOf(date, HC, source)
        ).use { c ->
            while (c.moveToNext()) {
                val type = c.getString(0)
                if (!seen.add(type)) continue
                val record = JSONObject(c.getString(1))
                val data = record.getJSONObject("data")
                var key: String? = null
                when (type) {
                    "WeightRecord" -> key = "weightKg"
                    "BodyFatRecord" -> key = "bodyFatPercent"
                    "OxygenSaturationRecord" -> key = "oxygenSaturationPercent"
                    "Vo2MaxRecord" -> key = "vo2Max"
                    "HeartRateVariabilityRmssdRecord" -> key = "hrvRmssdMs"
                    "RespiratoryRateRecord" -> key = "respiratoryRate"
                    "BloodGlucoseRecord" -> key = "bloodGlucoseMmolL"
                    "BloodPressureRecord" -> {
                        out.put("bloodPressureSystolic", data.opt("systolic"))
                            .put("bloodPressureDiastolic", data.opt("diastolic"))
                    }
                }
                if (key != null) out.put(key, data.opt("value"))
            }
        }
        return out
    }
}

@Throws(Exception::class)
@JvmOverloads
internal fun HealthJournalStore.page(
    from: String,
    to: String,
    channel: String,
    source: String,
    before: Long,
    beforeTime: Long,
    limit: Int,
    recordType: String = "",
    includeSummaries: Boolean = true
): JSONObject {
    if (!validDay(from) || !validDay(to) || from > to) {
        throw IllegalArgumentException("Invalid health period")
    }
    if (channel.isNotEmpty() && channel !in listOf(HC, BLE, LEGACY, SAMSUNG, "journal")) {
        throw IllegalArgumentException("Invalid channel")
    }
    require(recordType.length <= 80) { "Invalid health record type" }
    synchronized(LOCK) {
        val db = readableDatabase
        val boundedLimit = max(1, min(50, limit))
        val args = ArrayList<String>(
            listOf(
                from,
                to,
                (if (beforeTime > 0L) beforeTime else Long.MAX_VALUE).toString(),
                (if (beforeTime > 0L) beforeTime else Long.MAX_VALUE).toString(),
                (if (before > 0L) before else Long.MAX_VALUE).toString()
            )
        )
        var where = "day>=? AND day<=? AND (sort_ms<? OR (sort_ms=? AND row_id<?))"
        if ("journal" == channel) {
            where += " AND channel<>'ble_watch'"
        } else if (channel.isNotEmpty()) {
            where += " AND channel=?"
            args.add(channel)
        }
        if (source.isNotEmpty()) {
            where += " AND source=?"
            args.add(source)
        }
        if (recordType.isNotEmpty()) {
            where += " AND type=?"
            args.add(recordType)
        }
        val rows = JSONArray()
        var next = 0L
        var nextTime = 0L
        db.rawQuery(
            "SELECT row_id,json,sort_ms FROM records WHERE $where ORDER BY sort_ms DESC,row_id DESC LIMIT ${boundedLimit + 1}",
            args.toTypedArray()
        ).use { c ->
            while (c.moveToNext()) {
                if (rows.length() == boundedLimit) {
                    val lastObj = rows.getJSONObject(rows.length() - 1)
                    next = lastObj.getLong("cursor")
                    nextTime = lastObj.getLong("cursorTime")
                    break
                }
                val row = JSONObject(c.getString(1))
                row.put("cursor", c.getLong(0))
                row.put("cursorTime", c.getLong(2))
                rows.put(row)
            }
        }
        val origins = JSONArray()
        val originArgs = arrayListOf<String>()
        var originWhere = ""
        if (!includeSummaries) {
            originWhere = " WHERE day>=? AND day<=?"
            originArgs.add(from); originArgs.add(to)
            if (channel == "journal") originWhere += " AND channel<>'ble_watch'"
            else if (channel.isNotEmpty()) { originWhere += " AND channel=?"; originArgs.add(channel) }
            if (recordType.isNotEmpty()) { originWhere += " AND type=?"; originArgs.add(recordType) }
        }
        db.rawQuery(
            "SELECT channel,source,MAX(row_id),json FROM records$originWhere GROUP BY channel,source ORDER BY channel,source LIMIT 200",
            originArgs.toTypedArray()
        ).use { c ->
            while (c.moveToNext()) {
                origins.put(
                    JSONObject()
                        .put("channel", c.getString(0))
                        .put("source", c.getString(1))
                        .put("name", JSONObject(c.getString(3)).getJSONObject("data").optString("deviceName", ""))
                )
            }
        }
        var summaries = JSONArray()
        if (includeSummaries) {
            val dayArgs = ArrayList<String>(listOf(from, to))
            var dayWhere = "day>=? AND day<=?"
            if ("journal" == channel) {
                dayWhere += " AND (channel='health_connect' AND source='@aggregate' OR channel='legacy' AND NOT EXISTS (SELECT 1 FROM days AS hc WHERE hc.day=days.day AND hc.channel='health_connect' AND hc.source='@aggregate'))"
            } else if (channel.isNotEmpty()) {
                dayWhere += " AND channel=?"
                dayArgs.add(channel)
            }
            if (source.isNotEmpty()) {
                dayWhere += " AND source=?"
                dayArgs.add(source)
            } else {
                dayWhere += " AND (channel<>'health_connect' OR source='@aggregate')"
            }
            db.rawQuery(
                "SELECT day,channel,source,json FROM days WHERE $dayWhere ORDER BY day DESC,channel,source LIMIT 31",
                dayArgs.toTypedArray()
            ).use { c ->
                while (c.moveToNext()) {
                    val d = JSONObject(c.getString(3))
                    if (c.getString(1) == HC) {
                        d.put("sleepSessions", sleepSessions(db, c.getString(0), if ("@aggregate" == c.getString(2)) "" else c.getString(2)))
                    }
                    db.rawQuery("SELECT day FROM dirty_days WHERE day=?", arrayOf(c.getString(0))).use { dirty ->
                        if (dirty.moveToFirst()) d.put("partial", true)
                    }
                    d.put("channel", c.getString(1))
                    d.put("source", c.getString(2))
                    d.put("day", c.getString(0))
                    summaries.put(d)
                }
            }
            if ("journal" == channel) {
                summaries = JSONArray()
                val preferred = meta(db, "provider", HC)
                db.rawQuery(
                    "SELECT DISTINCT day FROM days WHERE day>=? AND day<=? AND channel<>'ble_watch' ORDER BY day DESC LIMIT 31",
                    arrayOf(from, to)
                ).use { dates ->
                    while (dates.moveToNext()) {
                        val date = dates.getString(0)
                        var hc: JSONObject? = null
                        var samsung: JSONObject? = null
                        var legacy: JSONObject? = null
                        db.rawQuery("SELECT channel,source,json FROM days WHERE day=?", arrayOf(date)).use { c ->
                            while (c.moveToNext()) {
                                when {
                                    c.getString(0) == HC && "@aggregate" == c.getString(1) -> hc = JSONObject(c.getString(2))
                                    c.getString(0) == SAMSUNG && "@aggregate" == c.getString(1) -> samsung = JSONObject(c.getString(2))
                                    c.getString(0) == LEGACY -> legacy = JSONObject(c.getString(2))
                                }
                            }
                        }
                        // Journal days must reflect the selected provider; other
                        // channels retain their own historical rows for explicit inspection.
                        if ((preferred == SAMSUNG && samsung == null) ||
                            (preferred != SAMSUNG && hc == null && legacy == null)) continue
                        if (hc != null) hc!!.put("sleepSessions", sleepSessions(db, date, ""))
                        val selected = HealthProviderProjection.day(hc, samsung, legacy, preferred).put("day", date)
                        if (!selected.has("source")) {
                            selected.put("source", if (hc != null || samsung != null) "@aggregate" else "unknown")
                        }
                        db.rawQuery("SELECT day FROM dirty_days WHERE day=?", arrayOf(date)).use { dirty ->
                            if (dirty.moveToFirst() && selected.optString("channel") == HC) {
                                selected.put("partial", true)
                            }
                        }
                        summaries.put(selected)
                    }
                }
            }
        }
        return JSONObject()
            .put("records", rows)
            .put("nextCursor", next)
            .put("nextTime", nextTime)
            .put("sources", origins)
            .put("days", summaries)
            .put("preferences", preferences())
    }
}
