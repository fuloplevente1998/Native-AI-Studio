package com.repforge.app

import android.content.Context
import androidx.room.withTransaction
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.time.Instant

/** Explicit import foundation. Does not start an SDK read, own an active session, or run a worker.
 * The caller must have chosen an import candidate; no automatic association from time overlap.
 */
internal object ExternalWorkoutImport {
    enum class Outcome { IMPORTED, ALREADY_IMPORTED, LOCALLY_DELETED, REVIEW_OVERLAP, UNAVAILABLE, CHANGED }

    // Provider + package + record ID, length-framed to avoid delimiter collisions.
    fun localId(identity: ExternalWorkoutIdentity): String {
        val key = listOf(identity.provider, identity.source, identity.recordId)
            .joinToString("") { "${it.length}:$it" }
        return "tp359-" + MessageDigest.getInstance("SHA-256")
            .digest(key.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 255) }
    }

    /** The elapsed interval is real, but active time, weight, sets and other metrics are not inferred.
     * Private source metadata is covered by NativeBackupPrivacy, while the opaque ID survives backup.
     */
    internal fun raw(workout: ExternalWorkout): JSONObject {
        val id = localId(workout.identity)
        return JSONObject().put("id", id).put("key", id).put("syncId", id)
            .put("started", Instant.ofEpochMilli(workout.startMs).toString())
            .put("finished", Instant.ofEpochMilli(workout.endMs).toString())
            .put("durationSeconds", (workout.elapsedMs / 1000).coerceAtMost(Int.MAX_VALUE.toLong()))
            .put("programName", "External workout").put("dayName", "")
            .put("originDevice", "external").put("exercises", JSONArray())
            .put("externalWorkoutImported359", true).put("externalWorkout359", workout.summary())
    }

    /** Resolve and verify the current journal record under the existing replacement fence. */
    suspend fun importSelected(
        context: Context, identity: ExternalWorkoutIdentity, expectedModifiedMs: Long
    ): Outcome = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val ticket = NativeDataReplacement.continuationForCurrentData(app)
        val journal = HealthJournalStore.get(app)
        val generation = journal.generation()
        // Live provider grants, not only a cached health dashboard status.
        val allowed = when (identity.provider) {
            HealthJournalStore.HC -> NativePrivacyControls.enabled(app, NativePrivacyControls.HEALTH_CONNECT) &&
                "READ_EXERCISE" in HealthConnectRepository.currentlyGrantedReadKeys(app)
            HealthJournalStore.SAMSUNG -> NativePrivacyControls.enabled(app, NativePrivacyControls.SAMSUNG_HEALTH) &&
                "READ_EXERCISE" in SamsungHealthRepository.readGrantedKeys(app) { ticket.requireCurrent(app) }
            else -> false
        }
        if (!allowed) return@withContext Outcome.UNAVAILABLE
        val db = TrainPilotDatabase.getInstance(app)
        NativeDataReplacement.mutate(app) {
            ticket.requireCurrent(app)
            check(NativeHealthBackupGate.available()) { "Backup is in progress" }
            val workout = journal.externalWorkoutRecord(identity, generation)
                ?: return@mutate Outcome.UNAVAILABLE
            if (workout.modifiedMs != expectedModifiedMs) return@mutate Outcome.CHANGED
            persist(app, db, workout) {
                ticket.requireCurrent(app)
                NativePrivacyControls.requireEnabled(app, identity.provider)
                if (identity.provider == HealthJournalStore.HC)
                    check(HealthConnectRepository.isAllowed(app, "READ_EXERCISE"))
                val current = journal.externalWorkoutRecord(identity, generation)
                check(current != null && current.summary().toString() == workout.summary().toString()) {
                    "External workout changed; refresh and retry"
                }
                check(NativeHealthBackupGate.available())
            }
        }
    }

    /** Internal transactional boundary, also exercised with an isolated Room DB in tests.
     * Do not overwrite existing local edits, photos, or an existing same-source import.
     */
    internal suspend fun persist(
        context: Context, db: TrainPilotDatabase, workout: ExternalWorkout, guard: () -> Unit
    ): Outcome {
        if (workout.isOwnExport(context.packageName) || workout.elapsedMs <= 0 ||
            workout.endMs > System.currentTimeMillis()) return Outcome.UNAVAILABLE
        val id = localId(workout.identity)
        val dao = db.workoutHistoryDao()
        return NativeDataReplacement.mutate(context) {
            db.withTransaction {
                guard()
                if (dao.isTombstoned(id, JSONObject.quote(id))) return@withTransaction Outcome.LOCALLY_DELETED
                if (dao.getWorkoutWithDetailsById(id) != null) return@withTransaction Outcome.ALREADY_IMPORTED
                if (dao.hasExternalImportOverlap(id, workout.startMs, workout.endMs))
                    return@withTransaction Outcome.REVIEW_OVERLAP
                LegacyWebViewMigrator(context, db, TrainPilotSettingsStore(context))
                    .saveWorkoutJsonToRoom(raw(workout))
                check(dao.getWorkoutWithDetailsById(id) != null) { "External workout was not stored" }
                guard()
                Outcome.IMPORTED
            }
        }
    }
}
