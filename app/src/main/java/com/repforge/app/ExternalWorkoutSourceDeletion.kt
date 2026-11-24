package com.repforge.app

import android.content.Context
import androidx.room.withTransaction
import com.repforge.app.data.db.HistoryTombstoneEntity
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.db.WorkoutHistoryWithDetails
import com.repforge.app.ui.health.NativeHealthForegroundAdmission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** HC tombstones, not absence, authorize deletion. Never erase annotated local history. */
internal object ExternalWorkoutSourceDeletion {
    data class Report(val removed: Int = 0, val retained: Int = 0, val failed: Boolean = false) {
        fun json() = JSONObject().put("removed", removed).put("retained", retained).put("failed", failed)
    }

    private val pureKeys = setOf("id", "key", "syncId", "started", "finished", "durationSeconds",
        "programName", "dayName", "programId", "dayId", "workout", "originDevice", "exercises",
        "externalWorkoutImported359", "externalWorkout359")

    internal fun pureCopy(detail: WorkoutHistoryWithDetails, raw: JSONObject): Boolean {
        val w = detail.workout
        return w.originDevice == "external" && raw.optBoolean("externalWorkoutImported359") &&
            w.programId.isBlank() && w.dayId.isBlank() && w.scheduleId.isBlank() &&
            w.editedAt == null && w.effortFeedback == null && !w.feedbackSkipped &&
            w.health240Json == null && w.wearMetricsJson == null &&
            detail.exercises.isEmpty() && detail.photos.isEmpty() &&
            raw.keys().asSequence().all { it in pureKeys }
    }

    /** Retain only the minimal identity and timing evidence required for a retained local row.
     * Do not leave provider measurements in a source-deleted snapshot or in its backup.
     */
    internal fun markDeleted(snapshot: JSONObject): JSONObject {
        snapshot.put("sourceDeleted", true)
        for (key in listOf("metrics", "series", "segments", "laps", "sessionEvidence", "sessionCalories", "device"))
            snapshot.remove(key)
        return snapshot
    }

    internal fun HealthJournalStore.confirmedDeletion(identity: ExternalWorkoutIdentity, modified: Long,
                                                       generation: Long): Long? = synchronized(HealthJournalStore.LOCK) {
        if (identity.provider != HealthJournalStore.HC) return@synchronized null
        val db = readableDatabase
        writable(db, generation)
        val exists = db.rawQuery("SELECT 1 FROM records WHERE channel=? AND type='ExerciseSessionRecord' AND record_id=? LIMIT 1",
            arrayOf(identity.provider, identity.recordId)).use { it.moveToFirst() }
        if (exists) return@synchronized null
        db.rawQuery("SELECT deleted_ms FROM deletions WHERE record_id=? AND deleted_ms>=?",
            arrayOf(identity.recordId, modified.toString())).use { if (it.moveToFirst()) it.getLong(0) else null }
    }

    suspend fun reconcile(context: Context, generation: Long, syncGuard: () -> Unit): Report = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val ticket = NativeDataReplacement.continuationForCurrentData(app)
        val journal = HealthJournalStore.get(app)
        val dao = TrainPilotDatabase.getInstance(app).workoutHistoryDao()
        val db = TrainPilotDatabase.getInstance(app)
        var removed = 0
        var retained = 0
        var after = ""
        val guard = {
            syncGuard()
            ticket.requireCurrent(app)
            check(NativePrivacyControls.enabled(app, NativePrivacyControls.HEALTH_CONNECT) &&
                HealthConnectRepository.isAllowed(app, "READ_EXERCISE"))
            check(NativeHealthForegroundAdmission.safeOffMain(app) && NativeHealthBackupGate.available())
            check(journal.generation() == generation)
        }
        try {
            // Keyset scan, never materialize the entire history or infer deletions from missing records.
            while (true) {
                guard()
                NativeHealthForegroundAdmission.requireMainAdmission(app)
                val ids = dao.pageExternalWorkoutIds(after, 64)
                if (ids.isEmpty()) break
                for (id in ids) {
                    val outcome = NativeDataReplacement.mutate(app) {
                        db.withTransaction {
                            guard()
                            val detail = dao.getWorkoutWithDetailsById(id) ?: return@withTransaction 0
                            val raw = JSONObject(detail.workout.rawJson)
                            var linkedChanged = false
                            val links = raw.optJSONObject("externalLinkedSources359")
                            for (linkedId in ExternalWorkoutAssociation.linkedIds(raw)) {
                                val linked = links?.optJSONObject(linkedId) ?: continue
                                if (linked.optBoolean("sourceDeleted") || linked.optString("provider") != HealthJournalStore.HC) continue
                                val identity = ExternalWorkoutIdentity(HealthJournalStore.HC, linked.optString("source"), linked.optString("id"))
                                if (ExternalWorkoutImport.localId(identity) != linkedId) continue
                                if (journal.confirmedDeletion(identity, linked.optLong("lastModifiedMs"), generation) != null) {
                                    markDeleted(linked)
                                    linkedChanged = true
                                }
                            }
                            val source = raw.optJSONObject("externalWorkout359")
                            val at = source?.takeIf { it.optString("provider") == HealthJournalStore.HC }?.let {
                                val identity = ExternalWorkoutIdentity(HealthJournalStore.HC, it.optString("source"), it.optString("id"))
                                if (ExternalWorkoutImport.localId(identity) != id) null
                                else journal.confirmedDeletion(identity, it.optLong("lastModifiedMs"), generation)
                            }
                            if (at == null) {
                                if (!linkedChanged) return@withTransaction 0
                                check(dao.updateJournalPhotoJson(id, detail.workout.rawJson, raw.toString()) == 1)
                                guard()
                                return@withTransaction 2
                            }
                            if (pureCopy(detail, raw)) {
                                val w = detail.workout
                                dao.upsertTombstone(HistoryTombstoneEntity(w.id, w.workoutKey,
                                    JSONArray(listOf(w.id, w.syncId, w.workoutKey).filter(String::isNotBlank).distinct()).toString(), at))
                                dao.deleteWorkoutById(id)
                                guard()
                                1
                            } else {
                                val primary = requireNotNull(source)
                                if (primary.optBoolean("sourceDeleted") && !linkedChanged) return@withTransaction 0
                                markDeleted(primary)
                                check(dao.updateExternalWorkoutJson(id, detail.workout.rawJson, raw.toString()) == 1)
                                guard()
                                2
                            }
                        }
                    }
                    if (outcome == 1) removed++ else if (outcome == 2) retained++
                }
                after = ids.last()
            }
            NativeDataReplacement.mutate(app) {
                guard()
                ExternalRouteStore(app.noBackupFilesDir).prune(dao)
            }
            Report(removed, retained)
        } finally {
            if (removed + retained > 0) ExternalWorkoutImport.notifyImported()
        }
    }
}
