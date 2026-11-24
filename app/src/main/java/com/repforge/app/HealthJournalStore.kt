package com.repforge.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.max
import kotlin.math.min

/** The primary health store. The UI only receives bounded projections/pages. */
internal class HealthJournalStore(context: Context) :
    SQLiteOpenHelper(context, "health-journal.db", null, 2) {

    companion object {
        const val HC: String = "health_connect"
        const val BLE: String = "ble_watch"
        const val LEGACY: String = "legacy"
        const val SAMSUNG: String = "samsung_health"
        internal val LOCK = Any()
        private const val META_CHUNK_CHARS: Int = 32768
        private const val META_CHUNKS: String = "\u0001health-meta-chunks-v1"
        private val DAY_REGEX = Regex("\\d{4}-\\d{2}-\\d{2}")
        private val DAILY_SOURCE_REGEX = Regex("[A-Za-z0-9_.-]{1,255}")

        // Kept as @JvmStatic private var instance so Robolectric tests reflecting on "instance" succeed!
        @JvmStatic
        private var instance: HealthJournalStore? = null

        @JvmStatic
        private var createdInstance: HealthJournalStore? = null

        @JvmStatic
        private var createdContext: Context? = null

        @JvmStatic
        fun get(context: Context): HealthJournalStore {
            synchronized(LOCK) {
                val app = context.applicationContext ?: context
                val current = instance
                if (current == null || (current === createdInstance && createdContext !== app)) {
                    runCatching { current?.close() }
                    val fresh = HealthJournalStore(app)
                    createdInstance = fresh
                    createdContext = app
                    instance = fresh
                }
                return instance!!
            }
        }

        private fun createMetaParts(db: SQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE meta_parts (key TEXT NOT NULL, part INTEGER NOT NULL, value TEXT NOT NULL, " +
                    "PRIMARY KEY(key,part), FOREIGN KEY(key) REFERENCES meta(key) ON DELETE CASCADE)"
            )
        }

        @JvmStatic
        fun day(time: Long): String {
            return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(time))
        }

        @JvmStatic
        fun iso(time: Long): String {
            val f = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            f.timeZone = TimeZone.getTimeZone("UTC")
            return f.format(Date(time))
        }

        @JvmStatic
        fun validDay(day: String?): Boolean {
            if (day == null || !DAY_REGEX.matches(day)) return false
            return try {
                val f = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                f.isLenient = false
                val parsed = f.parse(day) ?: return false
                f.format(parsed) == day
            } catch (_: Exception) {
                false
            }
        }

        @Throws(Exception::class)
        internal fun time(r: JSONObject, key: String): Long {
            val value = r.getLong(key)
            if (value < 0L || value > 4_102_444_800_000L) {
                throw IllegalArgumentException("Invalid health time")
            }
            return value
        }

        @Throws(Exception::class)
        internal fun text(r: JSONObject, key: String, max: Int): String {
            val value = r.getString(key)
            if (value.isEmpty() || value.length > max || value.indexOf('\u0000') >= 0) {
                throw IllegalArgumentException("Invalid health identity")
            }
            return value
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
        db.rawQuery("PRAGMA secure_delete=ON", null).use { it.moveToFirst() }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE records (row_id INTEGER PRIMARY KEY, channel TEXT NOT NULL, source TEXT NOT NULL, " +
                "type TEXT NOT NULL, record_id TEXT NOT NULL, sort_ms INTEGER NOT NULL, modified_ms INTEGER NOT NULL, " +
                "start_ms INTEGER NOT NULL, end_ms INTEGER NOT NULL, day TEXT NOT NULL, json TEXT NOT NULL, " +
                "UNIQUE(channel,type,record_id))"
        )
        db.execSQL("CREATE INDEX records_page ON records(day DESC,sort_ms DESC,row_id DESC)")
        db.execSQL("CREATE INDEX records_source ON records(channel,source,day DESC,row_id DESC)")
        db.execSQL(
            "CREATE TABLE days (day TEXT NOT NULL, channel TEXT NOT NULL, source TEXT NOT NULL, " +
                "json TEXT NOT NULL, PRIMARY KEY(day,channel,source))"
        )
        db.execSQL("CREATE TABLE meta (key TEXT PRIMARY KEY, value TEXT NOT NULL)")
        createMetaParts(db)
        db.execSQL("CREATE TABLE deletions (record_id TEXT PRIMARY KEY, deleted_ms INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE dirty_days (day TEXT PRIMARY KEY)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion == 1 && newVersion == 2) {
            createMetaParts(db)
        } else {
            throw IllegalStateException("Unsupported health schema")
        }
    }

    internal fun interface Work<T> {
        @Throws(Exception::class)
        fun run(db: SQLiteDatabase): T
    }

    @Throws(Exception::class)
    internal fun <T> transaction(work: Work<T>): T {
        synchronized(LOCK) {
            val db = writableDatabase
            db.beginTransaction()
            try {
                val value = work.run(db)
                db.setTransactionSuccessful()
                return value
            } finally {
                db.endTransaction()
            }
        }
    }

    internal fun meta(db: SQLiteDatabase, key: String, fallback: String): String {
        val first: String
        val length: Int
        db.rawQuery(
            "SELECT substr(value,1,$META_CHUNK_CHARS),length(value) FROM meta WHERE key=?",
            arrayOf(key)
        ).use { c ->
            if (!c.moveToFirst()) return fallback
            first = c.getString(0)
            length = c.getInt(1)
        }
        val value = StringBuilder(first)
        if (META_CHUNKS == first) {
            value.setLength(0)
            var expected = 0
            db.rawQuery(
                "SELECT part,value FROM meta_parts WHERE key=? ORDER BY part",
                arrayOf(key)
            ).use { c ->
                while (c.moveToNext()) {
                    if (c.getInt(0) != expected++) {
                        throw IllegalStateException("Incomplete health restore snapshot")
                    }
                    value.append(c.getString(1))
                }
            }
            if (expected == 0) throw IllegalStateException("Missing health restore snapshot")
        } else {
            var start = META_CHUNK_CHARS + 1
            while (start <= length) {
                db.rawQuery(
                    "SELECT substr(value,?,$META_CHUNK_CHARS) FROM meta WHERE key=?",
                    arrayOf(start.toString(), key)
                ).use { c ->
                    if (!c.moveToFirst()) throw IllegalStateException("Missing health restore snapshot")
                    value.append(c.getString(0))
                }
                start += META_CHUNK_CHARS
            }
        }
        return value.toString()
    }

    internal fun putMeta(db: SQLiteDatabase, key: String, value: String) {
        val chunked = value.length > META_CHUNK_CHARS || META_CHUNKS == value
        val v = ContentValues().apply {
            put("key", key)
            put("value", if (chunked) META_CHUNKS else value)
        }
        if (db.insertWithOnConflict("meta", null, v, SQLiteDatabase.CONFLICT_REPLACE) == -1L) {
            throw IllegalStateException("Cannot persist health restore metadata")
        }
        if (chunked) {
            var part = 0
            var start = 0
            while (start < value.length) {
                var end = min(start + META_CHUNK_CHARS, value.length)
                if (end < value.length &&
                    Character.isHighSurrogate(value[end - 1]) &&
                    Character.isLowSurrogate(value[end])
                ) {
                    end--
                }
                val chunk = ContentValues().apply {
                    put("key", key)
                    put("part", part++)
                    put("value", value.substring(start, end))
                }
                db.insertOrThrow("meta_parts", null, chunk)
                start = end
            }
        }
    }

    /** Persist restore JSON in bounded SQLite rows without a document-sized string. */
    internal fun putSnapshot(db: SQLiteDatabase, key: String, snapshot: JSONObject) {
        putMeta(db, key, META_CHUNKS)
        db.delete("meta_parts", "key=?", arrayOf(key))
        var part = 0
        val writer = object : java.io.Writer() {
            val buffer = StringBuilder(META_CHUNK_CHARS + 1)
            fun emit(final: Boolean) {
                var length = buffer.length
                if (!final && length > 0 && Character.isHighSurrogate(buffer[length - 1])) length--
                if (length == 0) return
                val row = ContentValues().apply {
                    put("key", key); put("part", part++); put("value", buffer.substring(0, length))
                }
                db.insertOrThrow("meta_parts", null, row)
                buffer.delete(0, length)
            }
            override fun write(chars: CharArray, offset: Int, length: Int) {
                var start = offset
                val end = offset + length
                while (start < end) {
                    val count = min(end - start, META_CHUNK_CHARS - buffer.length)
                    buffer.append(chars, start, count); start += count
                    if (buffer.length >= META_CHUNK_CHARS) emit(false)
                }
            }
            override fun write(text: String, offset: Int, length: Int) {
                var start = offset
                val end = offset + length
                while (start < end) {
                    val count = min(end - start, META_CHUNK_CHARS - buffer.length)
                    buffer.append(text, start, start + count); start += count
                    if (buffer.length >= META_CHUNK_CHARS) emit(false)
                }
            }
            override fun flush() { emit(false) }
            override fun close() { emit(true) }
        }
        writer.use { BackupJsonIO.write(snapshot, it) }
    }

    /** Also accepts pending snapshots written by older versions as one TEXT value. */
    internal fun readSnapshot(db: SQLiteDatabase, key: String): JSONObject {
        val header = db.rawQuery("SELECT substr(value,1,$META_CHUNK_CHARS) FROM meta WHERE key=?", arrayOf(key)).use {
            if (!it.moveToFirst()) throw IllegalStateException("Missing health restore snapshot")
            it.getString(0)
        }
        if (header != META_CHUNKS) return BackupJsonIO.read(java.io.StringReader(meta(db, key, ""))) as JSONObject
        val reader = object : java.io.Reader() {
            var part = 0
            var chunk = ""
            var offset = 0
            var ended = false
            override fun read(chars: CharArray, start: Int, length: Int): Int {
                if (length == 0) return 0
                if (offset == chunk.length) {
                    if (ended) return -1
                    db.rawQuery("SELECT value FROM meta_parts WHERE key=? AND part=?", arrayOf(key, part.toString())).use {
                        if (!it.moveToFirst()) {
                            val remaining = db.rawQuery("SELECT COUNT(*) FROM meta_parts WHERE key=? AND part>?", arrayOf(key, part.toString())).use { c -> c.moveToFirst(); c.getInt(0) }
                            if (part == 0 || remaining != 0) throw IllegalStateException("Incomplete health restore snapshot")
                            ended = true
                            return -1
                        }
                        chunk = it.getString(0); offset = 0; part++
                    }
                }
                val count = min(length, chunk.length - offset)
                chunk.toCharArray(chars, start, offset, offset + count)
                offset += count
                return count
            }
            override fun close() = Unit
        }
        return BackupJsonIO.read(reader) as JSONObject
    }

    fun generation(): Long {
        synchronized(LOCK) {
            return meta(readableDatabase, "generation", "0").toLong()
        }
    }

    internal fun writable(db: SQLiteDatabase, generation: Long) {
        if (meta(db, "generation", "0").toLong() != generation || meta(db, "restore_pending", "").isNotEmpty()) {
            throw IllegalStateException("Health data changed; retry after restore/reopen")
        }
    }

    @Throws(Exception::class)
    fun preferences(): JSONObject {
        synchronized(LOCK) {
            val db = readableDatabase
            return JSONObject()
                .put("provider", meta(db, "provider", HC))
                .put("primary", meta(db, "primary", HC))
                .put("autoOnOpen", meta(db, "autoOnOpen", "true").toBoolean())
                .put("logWatchSteps", meta(db, "logWatchSteps", "false").toBoolean())
                .put("hcDailySource", meta(db, "hcDailySource", "auto"))
                .put("bleBackground", meta(db, "bleBackground", "false").toBoolean())
                .put("hcBackground", meta(db, "hcBackground", "false").toBoolean())
        }
    }

    @Throws(Exception::class)
    fun setPreferences(preferences: JSONObject) {
        transaction { db ->
            writable(db, generation())
            var sourceChanged = false
            var accessChanged = false
            for (key in arrayOf("provider", "primary", "autoOnOpen", "logWatchSteps", "hcDailySource", "bleBackground", "hcBackground")) {
                if (preferences.has(key)) {
                    val value = if (key in arrayOf("provider", "primary", "hcDailySource")) {
                        preferences.getString(key)
                    } else {
                        preferences.getBoolean(key).toString()
                    }
                    if ("provider" == key && value != HC && value != SAMSUNG) {
                        throw IllegalArgumentException("Invalid health provider")
                    }
                    if ("provider" == key && value != meta(db, key, HC)) accessChanged = true
                    if ("primary" == key && value != HC && value != BLE) {
                        throw IllegalArgumentException("Invalid primary source")
                    }
                    if ("hcDailySource" == key && !DAILY_SOURCE_REGEX.matches(value)) {
                        throw IllegalArgumentException("Invalid daily source")
                    }
                    if ("hcDailySource" == key && value != meta(db, key, "auto")) sourceChanged = true
                    if (key in arrayOf("logWatchSteps", "bleBackground", "hcBackground") && value != meta(db, key, "false")) {
                        accessChanged = true
                    }
                    putMeta(db, key, value)
                }
            }
            if (!meta(db, "logWatchSteps", "false").toBoolean()) {
                putMeta(db, "bleBackground", "false")
            }
            if (sourceChanged || accessChanged) {
                putMeta(db, "generation", (generation() + 1L).toString())
            }
            if (sourceChanged) {
                db.execSQL("INSERT OR IGNORE INTO dirty_days SELECT day FROM days WHERE channel='health_connect' AND source='@aggregate'")
            }
            null
        }
    }

    internal fun nativeSyncDirtyDays(): Set<java.time.LocalDate> = synchronized(LOCK) {
        val days = sortedSetOf<java.time.LocalDate>()
        readableDatabase.rawQuery("SELECT day FROM dirty_days ORDER BY day LIMIT 531", null).use { c ->
            while (c.moveToNext()) days.add(java.time.LocalDate.parse(c.getString(0)))
        }
        check(days.size <= 530) { "Health dirty days exceed foreground budget" }
        days
    }

    internal fun nativeSyncLastSuccess(provider: String? = null): String = synchronized(LOCK) {
        if (provider == null) meta(readableDatabase, "lastSyncAt", "—")
        else { require(provider in setOf(HC, SAMSUNG)); meta(readableDatabase, "lastSyncAt_$provider", "—") }
    }

    /** Only this private key contains SDK tokens. Export/projection use explicit whitelists. */
    private fun nativeSyncKey(provider: String): String {
        require(provider == HC || provider == SAMSUNG)
        return "native_sync_private_" + provider
    }

    internal fun nativeSyncState(provider: String, expectedGeneration: Long): NativeHealthSync.Checkpoint = synchronized(LOCK) {
        writable(readableDatabase, expectedGeneration)
        val value = JSONObject(meta(readableDatabase, nativeSyncKey(provider), "{}"))
        if (value.optLong("epoch", -1) != expectedGeneration) NativeHealthSync.Checkpoint()
        else NativeHealthSync.Checkpoint(value.optString("scope"), value.optString("token"),
            value.optBoolean("initialized"), value.optLong("revision"))
    }

    /** Generation AND revision CAS: an old SDK response cannot resurrect a restore's token. */
    internal fun nativeSyncCheckpoint(
        provider: String, expectedGeneration: Long, expectedRevision: Long,
        checkpoint: NativeHealthSync.Checkpoint, full: Boolean, at: String
    ) {
        transaction { db ->
            writable(db, expectedGeneration)
            val current = nativeSyncState(provider, expectedGeneration)
            check(current.revision == expectedRevision) { "Obsolete Health sync checkpoint" }
            putMeta(db, nativeSyncKey(provider), JSONObject().put("epoch", expectedGeneration)
                .put("revision", expectedRevision + 1).put("scope", checkpoint.scope)
                .put("token", checkpoint.token).put("initialized", true).toString())
            putMeta(db, "lastSyncAt", at)
            putMeta(db, "lastSyncAt_$provider", at)
            if (full) putMeta(db, "lastFullSyncAt", at)
            null
        }
    }

    /** 2774: a complete day read without a change token still counts as the provider's last sync. */
    internal fun nativeSyncDataOnly(provider: String, expectedGeneration: Long, at: String) {
        transaction { db ->
            writable(db, expectedGeneration)
            putMeta(db, "lastSyncAt", at)
            putMeta(db, "lastSyncAt_$provider", at)
            null
        }
    }

    internal fun nativeSyncAttempt(expectedGeneration: Long, at: String) {
        transaction { db ->
            writable(db, expectedGeneration)
            putMeta(db, "lastAttemptAt", at)
            null
        }
    }

    @Throws(Exception::class)
    fun syncMeta(meta: JSONObject) {
        transaction { db ->
            writable(db, generation())
            for (key in arrayOf("lastSyncAt", "lastFullSyncAt", "lastAttemptAt", "lastSyncAt_$HC", "lastSyncAt_$SAMSUNG")) {
                if (meta.has(key) && !meta.isNull(key)) {
                    putMeta(db, key, meta.getString(key))
                }
            }
            null
        }
    }
}
