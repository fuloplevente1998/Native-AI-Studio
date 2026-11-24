package com.repforge.app

import android.util.AtomicFile
import com.repforge.app.data.db.WorkoutHistoryDao
import com.repforge.app.data.db.WorkoutHistoryEntity
import com.repforge.app.ui.journal.RouteCoordinatesUiModel
import org.json.JSONObject
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.time.Instant

/** Private imported-route cache. Unlike live GPS logs the provider may omit accuracy,
 * so never invent accuracy or append to the live tracking ledger. No backup references.
 */
internal class ExternalRouteStore(private val root: File) {
    data class Fingerprint(val id: String, val version: Long, val start: Long, val end: Long)
    companion object {
        const val DIRECTORY = "external_routes359"
        private const val MAGIC = 0x54503359
        private const val MAX_POINTS = 50_000
        private const val MAX_FILES = 512
        private const val MAX_BYTES = 1_000_000L
        private val idPattern = Regex("tp359-[a-f0-9]{64}")

        fun fingerprint(row: WorkoutHistoryEntity): Fingerprint? = runCatching {
            if (row.originDevice != "external" || !idPattern.matches(row.id)) return null
            val raw = JSONObject(row.rawJson)
            if (!raw.optBoolean("externalWorkoutImported359")) return null
            val source = raw.getJSONObject("externalWorkout359")
            if (source.optString("provider") != HealthJournalStore.HC || source.optBoolean("sourceDeleted") ||
                source.optBoolean("windowChanged")) return null
            val identity = ExternalWorkoutIdentity(HealthJournalStore.HC, source.getString("source"), source.getString("id"))
            if (ExternalWorkoutImport.localId(identity) != row.id) return null
            val start = Instant.parse(source.getString("start")).toEpochMilli()
            val end = Instant.parse(source.getString("end")).toEpochMilli()
            val version = source.getLong("lastModifiedMs")
            if (start != row.startedEpochMs || end != row.finishedEpochMs || end <= start || version < 0) return null
            Fingerprint(row.id, version, start, end)
        }.getOrNull()
    }

    private val directory = File(root, DIRECTORY)
    private fun file(id: String): AtomicFile {
        require(idPattern.matches(id))
        return AtomicFile(File(directory, id + ".route"))
    }
    private fun valid(lat: Double, lon: Double) = lat.isFinite() && lon.isFinite() &&
        lat in -90.0..90.0 && lon in -180.0..180.0

    private fun readHeader(input: DataInputStream, id: String): Fingerprint {
        check(input.readInt() == MAGIC)
        return Fingerprint(id, input.readLong(), input.readLong(), input.readLong())
    }

    fun contains(fingerprint: Fingerprint): Boolean = read(fingerprint) != null

    fun read(fingerprint: Fingerprint): RouteCoordinatesUiModel? = runCatching {
        val f = file(fingerprint.id)
        f.openRead().use { stream ->
            check(stream.channel.size() <= MAX_BYTES)
            DataInputStream(stream).use { input ->
                if (readHeader(input, fingerprint.id) != fingerprint) return null
                val count = input.readInt()
                check(count in 1..MAX_POINTS)
                val points = List(count) {
                    val lat = input.readDouble(); val lon = input.readDouble()
                    check(valid(lat, lon))
                    lat to lon
                }
                check(input.read() == -1)
                RouteCoordinatesUiModel(fingerprint.id, listOf(points), count, 0.0, null)
            }
        }
    }.getOrNull()

    fun save(fingerprint: Fingerprint, route: RouteCoordinatesUiModel) {
        require(route.routeKey == fingerprint.id && route.segments.size == 1)
        val points = route.segments.single()
        require(points.size in 1..MAX_POINTS && points.size == route.pointCount &&
            points.all { valid(it.first, it.second) })
        check(directory.isDirectory || directory.mkdirs())
        val f = file(fingerprint.id)
        if (!f.baseFile.exists()) check((directory.listFiles()?.count { it.name.endsWith(".route") } ?: MAX_FILES) < MAX_FILES)
        val stream = f.startWrite()
        try {
            val out = DataOutputStream(stream)
            out.writeInt(MAGIC)
            out.writeLong(fingerprint.version); out.writeLong(fingerprint.start); out.writeLong(fingerprint.end)
            out.writeInt(points.size)
            for ((lat, lon) in points) { out.writeDouble(lat); out.writeDouble(lon) }
            out.flush()
            f.finishWrite(stream)
        } catch (failure: Exception) {
            f.failWrite(stream)
            throw failure
        }
        check(contains(fingerprint))
    }

    fun delete(id: String): Boolean {
        if (!idPattern.matches(id)) return true
        val f = file(id)
        f.delete()
        return listOf("", ".bak", ".new").none { File(f.baseFile.path + it).exists() }
    }

    /** Under existing replacement/write lease. Crash-safe orphan cleanup: no source
     * metadata in the restored row means no authority to retain private coordinates.
     */
    suspend fun prune(dao: WorkoutHistoryDao) {
        val ids = directory.listFiles()?.mapNotNull {
            val id = it.name.substringBefore(".route")
            id.takeIf { idPattern.matches(id) }
        }?.distinct().orEmpty()
        for (id in ids) {
            val row = dao.getWorkoutWithDetailsById(id)?.workout
            val expected = row?.let(::fingerprint)
            if (expected == null || !contains(expected)) check(delete(id))
        }
    }
}
