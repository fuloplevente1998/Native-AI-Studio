package com.repforge.app

import android.content.Context
import androidx.room.withTransaction
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.repforge.app.ui.health.NativeHealthForegroundAdmission
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.time.Instant

/** Explicit import foundation. Does not start an SDK read, own an active session, or run a worker.
 * The caller must have chosen an import candidate; no automatic association from time overlap.
 */
internal object ExternalWorkoutImport {
    enum class Outcome { IMPORTED, UPDATED, ALREADY_IMPORTED, LOCALLY_DELETED, REVIEW_OVERLAP, UNAVAILABLE, CHANGED }

    private val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val imported = changes.asSharedFlow()

    internal fun notifyImported() { changes.tryEmit(Unit) }

    internal suspend fun hasReadGrant(app: Context, provider: String, guard: () -> Unit): Boolean =
        when (provider) {
            HealthJournalStore.HC -> NativePrivacyControls.enabled(app, NativePrivacyControls.HEALTH_CONNECT) &&
                "READ_EXERCISE" in HealthConnectRepository.currentlyGrantedReadKeys(app)
            HealthJournalStore.SAMSUNG -> NativePrivacyControls.enabled(app, NativePrivacyControls.SAMSUNG_HEALTH) &&
                "READ_EXERCISE" in SamsungHealthRepository.readGrantedKeys(app, guard)
            else -> false
        }

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
    internal fun raw(workout: ExternalWorkout, metrics: JSONObject? = null): JSONObject {
        val id = localId(workout.identity)
        return JSONObject().put("id", id).put("key", id).put("syncId", id)
            .put("started", Instant.ofEpochMilli(workout.startMs).toString())
            .put("finished", Instant.ofEpochMilli(workout.endMs).toString())
            .put("durationSeconds", (workout.elapsedMs / 1000).coerceAtMost(Int.MAX_VALUE.toLong()))
            .put("programName", "External workout").put("dayName", "")
            .put("originDevice", "external").put("exercises", JSONArray())
            .put("externalWorkoutImported359", true).put("externalWorkout359", sourceSnapshot(workout, metrics))
    }

    private fun sourceSnapshot(workout: ExternalWorkout, metrics: JSONObject?): JSONObject =
        workout.summary().apply { if (metrics != null) put("metrics", metrics) }

    /** Resolve and verify the current journal record under the existing replacement fence. */
    suspend fun importSelected(
        context: Context, identity: ExternalWorkoutIdentity, expectedModifiedMs: Long,
        targetId: String? = null, keepSeparate: Boolean = false
    ): Outcome = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val ticket = NativeDataReplacement.continuationForCurrentData(app)
        val journal = HealthJournalStore.get(app)
        val generation = journal.generation()
        // Live provider grants, not only a cached health dashboard status.
        NativeHealthForegroundAdmission.requireMainAdmission(app)
        val grants = if (identity.provider == HealthJournalStore.HC &&
            NativePrivacyControls.enabled(app, NativePrivacyControls.HEALTH_CONNECT))
            HealthConnectRepository.currentlyGrantedReadKeys(app)
        else if (identity.provider == HealthJournalStore.SAMSUNG && NativePrivacyControls.enabled(app, NativePrivacyControls.SAMSUNG_HEALTH))
            SamsungHealthRepository.readGrantedKeys(app) { ticket.requireCurrent(app) }
        else emptySet()
        val allowed = "READ_EXERCISE" in grants
        if (!allowed) return@withContext Outcome.UNAVAILABLE
        val db = TrainPilotDatabase.getInstance(app)
        val outcome = NativeDataReplacement.mutate(app) {
            ticket.requireCurrent(app)
            check(NativeHealthForegroundAdmission.safeOffMain(app)) { "Health import is not admitted" }
            check(NativeHealthBackupGate.available()) { "Backup is in progress" }
            val workout = journal.externalWorkoutRecord(identity, generation)
                ?: return@mutate Outcome.UNAVAILABLE
            if (workout.modifiedMs != expectedModifiedMs) return@mutate Outcome.CHANGED
            val metrics = journal.externalWorkoutMetrics(workout, generation, grants)
            persist(app, db, workout, metrics, targetId, keepSeparate) {
                ticket.requireCurrent(app)
                NativePrivacyControls.requireEnabled(app, identity.provider)
                if (identity.provider == HealthJournalStore.HC)
                    check(grants.all { HealthConnectRepository.isAllowed(app, it) })
                val current = journal.externalWorkoutRecord(identity, generation)
                check(current != null && current.summary().toString() == workout.summary().toString()) {
                    "External workout changed; refresh and retry"
                }
                check(journal.externalWorkoutMetrics(workout, generation, grants)?.toString() == metrics?.toString()) {
                    "External workout measurements changed; refresh and retry"
                }
                check(NativeHealthBackupGate.available())
                check(NativeHealthForegroundAdmission.safeOffMain(app))
            }
        }
        if (outcome in setOf(Outcome.IMPORTED, Outcome.UPDATED)) notifyImported()
        outcome
    }

    /** Internal transactional boundary, also exercised with an isolated Room DB in tests.
     * Update only the provider snapshot of the same identity, never local edits or children.
     */
    internal suspend fun persist(
        context: Context, db: TrainPilotDatabase, workout: ExternalWorkout, metrics: JSONObject? = null,
        targetId: String? = null, keepSeparate: Boolean = false, allowCreate: Boolean = true, guard: () -> Unit
    ): Outcome {
        if (workout.isOwnExport(context.packageName) || workout.elapsedMs <= 0 ||
            workout.endMs > System.currentTimeMillis()) return Outcome.UNAVAILABLE
        val id = localId(workout.identity)
        val dao = db.workoutHistoryDao()
        return NativeDataReplacement.mutate(context) {
            db.withTransaction {
                guard()
                if (dao.isTombstoned(id, JSONObject.quote(id))) return@withTransaction Outcome.LOCALLY_DELETED
                val existing = dao.getWorkoutWithDetailsById(id)
                if (existing != null) {
                    val w = existing.workout
                    val raw = JSONObject(w.rawJson)
                    if (w.originDevice != "external" || !raw.optBoolean("externalWorkoutImported359"))
                        return@withTransaction Outcome.REVIEW_OVERLAP
                    val previous = raw.optJSONObject("externalWorkout359")
                    if (previous != null && previous.optLong("lastModifiedMs") > workout.modifiedMs)
                        return@withTransaction Outcome.ALREADY_IMPORTED
                    val next = sourceSnapshot(workout, metrics)
                    // Only a pristine imported copy may adopt a corrected provider interval.
                    // A user annotation, exercise, photo or explicit association is never overwritten.
                    val moved = w.startedEpochMs != workout.startMs || w.finishedEpochMs != workout.endMs
                    if (moved && ExternalWorkoutSourceDeletion.pureCopy(existing, raw) &&
                        !dao.hasExternalImportOverlap(id, workout.startMs, workout.endMs)) {
                        val start = Instant.ofEpochMilli(workout.startMs).toString()
                        val end = Instant.ofEpochMilli(workout.endMs).toString()
                        val duration = (workout.elapsedMs / 1000).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                        raw.put("started", start).put("finished", end).put("durationSeconds", duration)
                            .put("externalWorkout359", next)
                        check(dao.updateExternalWorkoutWindow(id, w.rawJson, raw.toString(), start, end,
                            workout.startMs, workout.endMs, duration) == 1)
                        guard()
                        return@withTransaction Outcome.UPDATED
                    }
                    if (moved) {
                        next.put("windowChanged", true)
                        next.remove("metrics")
                    }
                    if (previous?.toString() == next.toString()) return@withTransaction Outcome.ALREADY_IMPORTED
                    raw.put("externalWorkout359", next)
                    check(dao.updateExternalWorkoutJson(id, w.rawJson, raw.toString()) == 1)
                    guard()
                    return@withTransaction Outcome.UPDATED
                }
                val linked = dao.externalLinkCandidates(id, JSONObject.quote(id))
                    .filter { id in ExternalWorkoutAssociation.linkedIds(JSONObject(it.rawJson)) }
                if (linked.size > 1) return@withTransaction Outcome.REVIEW_OVERLAP
                if (linked.isNotEmpty()) {
                    val row = linked.single()
                    if (targetId != null && targetId != row.id) return@withTransaction Outcome.REVIEW_OVERLAP
                    return@withTransaction ExternalWorkoutAssociation.attach(dao, row, id, workout, metrics, guard)
                }
                // Syncing an existing association/import does not imply consent to create new history.
                if (!allowCreate) return@withTransaction Outcome.UNAVAILABLE
                if (targetId != null) {
                    val row = dao.getWorkoutWithDetailsById(targetId)?.workout ?: return@withTransaction Outcome.CHANGED
                    if (row.id == id || row.startedEpochMs >= workout.endMs || row.finishedEpochMs <= workout.startMs ||
                        dao.isTombstoned(row.id, JSONObject.quote(row.id))) return@withTransaction Outcome.CHANGED
                    return@withTransaction ExternalWorkoutAssociation.attach(dao, row, id, workout, metrics, guard)
                }
                if (!keepSeparate && dao.hasExternalImportOverlap(id, workout.startMs, workout.endMs))
                    return@withTransaction Outcome.REVIEW_OVERLAP
                LegacyWebViewMigrator(context, db, TrainPilotSettingsStore(context))
                    .saveWorkoutJsonToRoom(raw(workout, metrics))
                check(dao.getWorkoutWithDetailsById(id) != null) { "External workout was not stored" }
                guard()
                Outcome.IMPORTED
            }
        }
    }
}
