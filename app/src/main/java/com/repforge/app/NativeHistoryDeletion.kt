package com.repforge.app

import android.content.Context
import androidx.room.withTransaction
import com.repforge.app.data.db.*
import com.repforge.shared.GpsRouteLedger193
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** The journal's canonical deletion, with a durable, metadata-only file-cleanup receipt. */
internal object NativeHistoryDeletion {
    private const val PREFIX = "native:historyCleanup:"
    private fun strings(array: JSONArray) = (0 until array.length()).map { array.getString(it) }
    private fun sorted(d: WorkoutHistoryWithDetails) = d.copy(
        exercises = d.exercises.sortedBy { it.exercise.rowId }.map { it.copy(sets = it.sets.sortedBy { s -> s.rowId }) },
        photos = d.photos.sortedBy { it.id })

    suspend fun delete(context: Context, db: TrainPilotDatabase, target: WorkoutHistoryWithDetails,
        ticket: NativeDataReplacement.Continuation) = NativeDataReplacement.mutate(context) {
        ticket.requireCurrent(context)
        val w = target.workout
        val aliases = listOf(w.id, w.syncId, w.workoutKey).filter(String::isNotBlank).distinct()
        NativeJournalEditor.requireNotActive(context, aliases.toSet())
        val key = PREFIX + w.id
        db.withTransaction {
            val current = db.workoutHistoryDao().getWorkoutWithDetailsById(w.id)
            if (current != null) {
                check(sorted(current) == sorted(target)) { "Az edzés megváltozott. Nyisd meg újra." }
                val now = System.currentTimeMillis()
                val routes = current.exercises.flatMap { it.sets }.flatMap {
                    listOfNotNull(it.gpsRouteKey163, it.gpsRouteKey193)
                }.filter(String::isNotBlank).distinct()
                val rawPhotos = JSONObject(current.workout.rawJson).optJSONArray("photos") ?: JSONArray()
                val photoIds = (current.photos.map { it.id } + (0 until rawPhotos.length()).mapNotNull {
                    rawPhotos.optJSONObject(it)?.optString("id")?.takeIf(String::isNotBlank)
                }).distinct()
                val receipt = JSONObject().put("id", w.id).put("deletedAt", now)
                    .put("aliases", JSONArray(aliases)).put("photos", JSONArray(photoIds))
                    .put("routes", JSONArray(routes)).put("ledger", w.ownershipProtocol193 == 1)
                db.keyValueDocumentDao().putDocument(KeyValueDocumentEntity(key, receipt.toString(), now))
                db.workoutHistoryDao().upsertTombstone(HistoryTombstoneEntity(w.id, w.workoutKey, JSONArray(aliases).toString(), now))
                // Exact primary key: a coincident workoutKey on another row is not authorization to delete it.
                db.workoutHistoryDao().deleteWorkoutById(w.id)
            } else check(db.keyValueDocumentDao().getDocument(key) != null) { "Az edzés már nem található." }
        }
        cleanup(context, db, key)
    }

    suspend fun recover(context: Context, db: TrainPilotDatabase) = NativeDataReplacement.mutate(context) {
        for (doc in db.keyValueDocumentDao().getHistoryCleanupDocuments()) cleanup(context, db, doc.key)
    }

    private suspend fun cleanup(context: Context, db: TrainPilotDatabase, key: String) {
        val dao = db.workoutHistoryDao()
        val doc = db.keyValueDocumentDao().getDocument(key) ?: return
        val receipt = JSONObject(doc.json)
        val id = receipt.getString("id")
        val tombstone = dao.getHistoryTombstone(id)
        // A restore/replacement invalidates a pending receipt; never delete newly restored assets.
        if (dao.getWorkoutWithDetailsById(id) != null || tombstone?.deletedAt != receipt.getLong("deletedAt")) {
            db.keyValueDocumentDao().deleteDocument(key)
            return
        }
        NativeJournalEditor.requireNotActive(context, strings(receipt.getJSONArray("aliases")).toSet())
        val active = withContext(Dispatchers.Main.immediate) {
            listOfNotNull(ActiveWorkoutStore.current(context), PhoneWorkoutExecution.store(context).snapshot(),
                PhoneOwnership193.journal(context).read().optJSONObject("snapshot"))
                .filter { it.optBoolean("active") || it.optBoolean("finalizationPending") || it.optBoolean("discardPending") }
                .joinToString("\n")
        }
        suspend fun referenced(value: String) = active.contains(JSONObject.quote(value)) ||
            dao.hasHistoryAssetReference(value, JSONObject.quote(value))
        val photos = WorkoutPhotoRepository(context)
        for (photo in strings(receipt.getJSONArray("photos"))) {
            if (!referenced(photo) && photos.exists(photo)) check(photos.delete(photo)) {
                "A fotófájl törlése nem sikerült. Próbáld újra." }
        }
        val routes = strings(receipt.getJSONArray("routes"))
        var sharedRoute = false
        for (route in routes) {
            if (referenced(route)) { sharedRoute = true; continue }
            if (route.startsWith("wear193:")) GpsRouteLedger193.open(context.noBackupFilesDir).delete(route)
            else check(GpsRouteStore(context.noBackupFilesDir).delete(route)) {
                "Az útvonalfájl törlése nem sikerült. Próbáld újra." }
        }
        if (receipt.optBoolean("ledger") && !sharedRoute && !referenced(id))
            GpsRouteLedger193.open(context.noBackupFilesDir).deleteWorkout(id)
        // A failure leaves the receipt for an explicit retry or the next journal opening.
        db.keyValueDocumentDao().deleteDocument(key)
    }
}
