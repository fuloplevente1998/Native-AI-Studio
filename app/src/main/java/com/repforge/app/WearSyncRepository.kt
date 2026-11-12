package com.repforge.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.repforge.app.data.db.HistoryTombstoneEntity
import com.repforge.app.data.db.KeyValueDocumentEntity
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.TrainPilotSettingsStore
import com.repforge.shared.GpsRouteLedger193
import com.repforge.shared.OwnershipTransport193
import com.repforge.shared.SetEditorRules212
import com.repforge.shared.WearDataLayerPaths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object WearSyncRepository {
    const val ACTIVE_WORKOUT_PATH: String = WearDataLayerPaths.ACTIVE_WORKOUT_PATH
    const val WATCH_HOME_PATH: String = WearDataLayerPaths.WATCH_HOME_PATH
    const val AUTOMATIC_COMMAND_PREFIX: String = WearDataLayerPaths.AUTOMATIC_COMMAND_PREFIX

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** The watch performs and durably publishes its own resume; the phone never changes owner or pause optimistically. */
    fun requestWatchWorkoutContinuation(context: Context, onFailure: (String) -> Unit) {
        check(!NativeDataReplacement.blocked(context)) { "Adat-visszaállítás vagy törlés van folyamatban." }
        val current = ActiveWorkoutStore.current(context) ?: error("Nincs aktív edzés.")
        if (!current.optBoolean("workoutPaused")) return
        val command = com.repforge.shared.WorkoutContinuation.request(current, UUID.randomUUID().toString(), System.currentTimeMillis())
        Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { peers ->
            val latest = ActiveWorkoutStore.current(context)
            if (NativeDataReplacement.blocked(context) || latest == null ||
                !com.repforge.shared.WorkoutContinuation.accepts(latest, command, System.currentTimeMillis())) {
                onFailure("Az órás edzés állapota megváltozott. Nyisd meg újra a folytatást.")
                return@addOnSuccessListener
            }
            if (peers.size != 1) {
                onFailure("Az órás folytatáshoz csatlakoztasd az órát, majd próbáld újra.")
                return@addOnSuccessListener
            }
            val map = PutDataMapRequest.create(com.repforge.shared.WorkoutContinuation.PATH_PREFIX + command.getString("commandId"))
            map.dataMap.putString("command", command.toString())
            Wearable.getDataClient(context).putDataItem(map.asPutDataRequest().setUrgent())
                .addOnFailureListener { onFailure("Az órás folytatáskérés küldése nem sikerült. Próbáld újra.") }
        }.addOnFailureListener { onFailure("Az órakapcsolat nem ellenőrizhető. Próbáld újra.") }
    }

    fun refreshWatchHomeAsync(context: Context) {
        val app = context.applicationContext
        syncScope.launch { runCatching { publishWatchHomeSnapshot(app) } }
    }

    /**
     * Automatically prepares and offers ownership to the watch when the watch requests
     * takeover ("incoming" stage) and the phone workout is already paused or can be safely paused.
     */
    fun autoPrepareIncomingOwnershipIfEligible(context: Context, localNodeId: String) {
        val appContext = context.applicationContext
        Handler(Looper.getMainLooper()).post {
            try {
                val journal = PhoneOwnership193.journal(appContext)
                val state = journal.read()
                if ("incoming" != state.optString("stage")) return@post
                val request = state.optJSONObject("request") ?: return@post
                val current = ActiveWorkoutStore.current(appContext) ?: return@post
                if (!current.optBoolean("active", true) ||
                    "phone" != current.optString("timerOwner", "phone") ||
                    current.optString("workoutId") != request.optString("workoutId")
                ) {
                    return@post
                }
                if (DistanceTrackingService.snapshot(appContext).optBoolean("active")) {
                    return@post
                }
                // Ensure workout is paused before preparing the offer
                if (!current.optBoolean("workoutPaused")) {
                    val now = System.currentTimeMillis()
                    val pausedFrame = JSONObject(current.toString())
                        .put("workoutPaused", true)
                        .put("pauseRevision", max(current.optLong("pauseRevision") + 1L, now))
                        .put("pausedRestMillis", max(current.optLong("pausedRestMillis"), max(0L, current.optLong("restEndAt") - now)))
                        .put("restEndAt", 0)
                    val intervals = pausedFrame.optJSONArray("activeIntervals")
                    if (intervals != null) {
                        for (i in 0 until intervals.length()) {
                            val row = intervals.optJSONObject(i)
                            if (row != null && (row.isNull("end") || row.optString("end").isEmpty())) {
                                row.put("end", Instant.ofEpochMilli(now).toString())
                            }
                        }
                    }
                    PhoneWorkoutExecution.setWorkoutPause(appContext, pausedFrame)
                }
                val frozen = PhoneWorkoutExecution.store(appContext).snapshot() ?: ActiveWorkoutStore.current(appContext) ?: return@post
                PhoneOwnership193.prepare(appContext, frozen, request.getString("id"), localNodeId)
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Executes any watch command natively in the background (without requiring a WebView!),
     * commits the result to Room / ActiveWorkoutStore / PhoneWorkoutExecutionStore,
     * and publishes the durable receipt to `/trainpilot/command-receipt/<commandId>`.
     */
    fun processIncomingWatchCommandAsync(context: Context, command: JSONObject, uri: Uri?) {
        val appContext = context.applicationContext
        syncScope.launch {
            try {
                // Explicit Unit bounds the K2 inference across the many command branches
                // and nested Main dispatcher receipts; result is intentionally discarded.
                NativeDataReplacement.mutate<Unit>(appContext) {
                    check(!NativeDataReplacement.blocked(appContext)) { "Adat-visszaállítás vagy törlés van folyamatban." }
                    val action = command.optString("action")
                    val db = TrainPilotDatabase.getInstance(appContext)
                    if (action == "finishWorkout") {
                        val id = command.optString("workoutId")
                        if (db.workoutHistoryDao().isTombstoned(id, JSONObject.quote(id))) {
                            WearCommandReceipts.complete(appContext, command, "superseded", uri)
                            return@mutate
                        }
                        val stored = db.workoutHistoryDao().getWorkoutWithDetails(id)
                        if (stored != null) {
                            val (current, execution) = withContext(Dispatchers.Main.immediate) {
                                ActiveWorkoutStore.current(appContext) to PhoneWorkoutExecution.store(appContext).snapshot()
                            }
                            if (NativeWatchFinishAdmission.isSavedRetry(command, JSONObject(stored.workout.rawJson), current, execution)) {
                                check(WearCommandReceipts.complete(appContext, command, "saved", uri)) { "A mentés nyugtája nem tárolható." }
                                return@mutate
                            }
                            // Do not overwrite an already saved/edited journal entry with a stale retry.
                            if (current == null || current.optString("workoutId") != id || !current.optBoolean("finalizationPending")) {
                                WearCommandReceipts.complete(appContext, command, "superseded", uri)
                                return@mutate
                            }
                        }
                        if (!PhoneOwnership193.commandAllowed(appContext, command)) {
                            WearCommandReceipts.complete(appContext, command, "superseded", uri)
                            return@mutate
                        }
                    }
                    if (NativeWatchDiscardAdmission.rejectsResurrection(db, command)) {
                        WearCommandReceipts.complete(appContext, command, "superseded", uri)
                        return@mutate
                    }
                    if (action == "discardWorkout") {
                        when (NativeWatchDiscardAdmission.reconcile(appContext, db, command)) {
                            NativeWatchDiscardAdmission.Decision.ALREADY_DISCARDED,
                            NativeWatchDiscardAdmission.Decision.RECORD_ORPHAN_DISCARD -> {
                                check(WearCommandReceipts.complete(appContext, command, "discarded", uri)) {
                                    "Az elvetés visszaigazolása nem menthető."
                                }
                                publishWatchHomeSnapshot(appContext, JSONObject().put("commandId", command.optString("commandId"))
                                    .put("workoutId", command.optString("workoutId")).put("status", "discarded"))
                                return@mutate
                            }
                            NativeWatchDiscardAdmission.Decision.REJECT -> {
                                WearCommandReceipts.complete(appContext, command, "superseded", uri)
                                return@mutate
                            }
                            NativeWatchDiscardAdmission.Decision.APPLY -> Unit
                        }
                    }
                    val pending = ActiveWorkoutStore.current(appContext)?.optBoolean("finalizationPending") == true
                    check(!pending || action in setOf("finishWorkout", "saveWorkoutFeedback", "healthSummary") ||
                        action == "discardWorkout" && ActiveWorkoutStore.current(appContext)?.optBoolean("discardPending") == true) {
                        "Workout finalization is in progress"
                    }
                    val settingsStore = TrainPilotSettingsStore(appContext)
                    val migrator = LegacyWebViewMigrator(appContext, db, settingsStore)

                    when (action) {
                        "startWorkout", "startQuickWorkout" -> {
                            val finalSnapshot = command.optJSONObject("finalSnapshot")
                            if (finalSnapshot != null) {
                                val origin = PhoneGpsRoute193.startOrigin(command)
                                val toSave = JSONObject(finalSnapshot.toString())
                                    .put("active", true)
                                    .put("timerOwner", "watch")
                                    .put("ownershipProtocol193", finalSnapshot.optInt("ownershipProtocol193", 1))
                                if (origin.isNotEmpty()) {
                                    toSave.put("watchSourceNode193", origin)
                                }
                                withContext(Dispatchers.Main) {
                                    val admitted = NativeWatchSnapshotAdmission.snapshot(ActiveWorkoutStore.current(appContext),
                                        JSONObject(command.toString()).put("finalSnapshot", toSave))
                                    if (admitted == null || !PhoneOwnership193.commandAllowed(appContext, command)) {
                                        WearCommandReceipts.complete(appContext, command, "superseded", uri)
                                        return@withContext
                                    }
                                    val saved = ActiveWorkoutStore.saveOwned(appContext, admitted)
                                    PhoneGpsRoute193.authorizeOwned(appContext, saved)
                                    publishActiveWorkoutSnapshot(appContext, saved)
                                    WearCommandReceipts.complete(appContext, command, "applied", uri)
                                }
                            } else {
                                withContext(Dispatchers.Main) {
                                    WearCommandReceipts.complete(appContext, command, "error", uri)
                                }
                            }
                        }

                        "addQuickExercise", "watchAutomationState", "resumeWorkout" -> {
                            withContext(Dispatchers.Main) {
                                val current = ActiveWorkoutStore.current(appContext)
                                val next = NativeWatchSnapshotAdmission.snapshot(current, command)
                                if (next == null || !PhoneOwnership193.commandAllowed(appContext, command)) {
                                    WearCommandReceipts.complete(appContext, command, "superseded", uri)
                                    return@withContext
                                }
                                // Recovery without a local draft is reserved for explicit start.
                                if (current == null || !current.optBoolean("active", true)) {
                                    WearCommandReceipts.complete(appContext, command, "superseded", uri)
                                    return@withContext
                                }
                                val saved = ActiveWorkoutStore.saveOwned(appContext, next)
                                PhoneGpsRoute193.authorizeOwned(appContext, saved)
                                publishActiveWorkoutSnapshot(appContext, saved)
                                WearCommandReceipts.complete(appContext, command, "applied", uri)
                            }
                        }

                        "updateSet", "completeSet", "selectExercise", "nextExercise", "prevExercise", "skipRest", "toggleAutomatic" -> {
                            withContext(Dispatchers.Main) {
                                val current = ActiveWorkoutStore.current(appContext)
                                if (current == null || !current.optBoolean("active") ||
                                    current.optString("workoutId") != command.optString("workoutId")
                                ) {
                                    WearCommandReceipts.complete(appContext, command, "superseded", uri)
                                    return@withContext
                                }
                                val saved = commitWatchEditorMutation(appContext, current, command)
                                publishActiveWorkoutSnapshot(appContext, saved)
                                WearCommandReceipts.complete(appContext, command, "applied", uri)
                            }
                        }

                        "finishWorkout" -> {
                            val workoutId = command.optString("workoutId")
                            val phoneOwnedRequest = withContext(Dispatchers.Main.immediate) {
                                NativeWatchFinishAdmission.isPhoneOwnedRequest(command, ActiveWorkoutStore.current(appContext)) &&
                                    PhoneOwnership193.commandAllowed(appContext, command)
                            }
                            // In mirror mode the watch requests a phone-owned finish. Use only
                            // the phone's canonical measurements, never the mirrored payload.
                            val finalSnapshot = if (phoneOwnedRequest) withContext(Dispatchers.Main.immediate) {
                                val current = requireNotNull(ActiveWorkoutStore.current(appContext))
                                if (current.optBoolean("finalizationPending")) current else NativeWorkoutEditing.project(appContext)
                            } else command.optJSONObject("finalSnapshot")
                                ?: ActiveWorkoutStore.current(appContext)?.takeIf { it.optString("workoutId") == workoutId }
                            if (finalSnapshot == null) {
                                withContext(Dispatchers.Main) {
                                    WearCommandReceipts.complete(appContext, command, "error", uri)
                                }
                                return@mutate
                            }
                            val origin = PhoneGpsRoute193.startOrigin(command)
                            if (!phoneOwnedRequest && origin.isNotEmpty()) {
                                PhoneGpsRoute193.authorizeOwned(
                                    appContext,
                                    JSONObject(finalSnapshot.toString()).put("watchSourceNode193", origin)
                                )
                            }
                            val finishedIso = if (phoneOwnedRequest) Instant.now().toString()
                                else command.optString("finishedAt").ifBlank { Instant.now().toString() }
                            val historyObj = JSONObject(finalSnapshot.toString())
                                .put("id", workoutId)
                                .put("syncId", workoutId)
                                .put("workoutId", workoutId)
                                .put("finished", finishedIso)
                                .put("originDevice", if (phoneOwnedRequest) "phone" else "watch")

                            // Attach any watch GPS route bindings (#193) to the matching sets
                            if (!phoneOwnedRequest) attachWatchRouteBindings193(appContext, workoutId, historyObj)

                            // Attach watch Health Services summary if present
                            val healthSummary = command.optJSONObject("healthSummary")
                            if (healthSummary != null) {
                                historyObj.put("wear", healthSummary)
                            }

                            val feedback = command.optJSONObject("feedback")
                            if (feedback != null) historyObj.put("feedback", JSONObject(feedback.toString()).put("updatedAt", System.currentTimeMillis()))
                            val saved = TrainPilotWorkoutFinalization(appContext, db).finishWorkout(historyObj, fromWatch = !phoneOwnedRequest,
                                phoneOwnedWatchRequest = command.takeIf { phoneOwnedRequest })
                            if (feedback != null) settingsStore.setPendingFeedback(null, null)
                            else settingsStore.setPendingFeedback(workoutId, saved.history.optString("started", workoutId))
                            withContext(Dispatchers.Main) {
                                saved.closedSnapshot?.let { publishActiveWorkoutSnapshot(appContext, it) }
                                // A saved receipt is possible only after native close and draft cleanup.
                                check(WearCommandReceipts.complete(appContext, command, "saved", uri)) {
                                    "A mentés visszaigazolása nem menthető."
                                }
                            }

                            val workoutResult = JSONObject()
                                .put("commandId", command.optString("commandId"))
                                .put("workoutId", workoutId)
                                .put("status", "saved")
                                .put("feedbackAnswered", feedback != null)
                            publishWatchHomeSnapshot(appContext, workoutResult)
                        }

                        "discardWorkout" -> {
                            val workoutId = command.optString("workoutId")
                            withContext(Dispatchers.Main.immediate) {
                                val current = requireNotNull(ActiveWorkoutStore.current(appContext))
                                check(current.optBoolean("active") && current.optString("workoutId") == workoutId)
                                ActiveWorkoutStore.saveFinalizing(appContext, JSONObject(current.toString())
                                    .put("finalizationPending", true).put("discardPending", true)
                                    .put("workoutPaused", true).put("restEndAt", 0L))
                                val execution = PhoneWorkoutExecution.store(appContext)
                                if (execution.snapshot()?.optString("workoutId") == workoutId) execution.suspendCommands(true)
                                IntervalTimerService.stopForOwnership(appContext, workoutId)
                                DistanceTrackingService.stop(appContext)
                            }
                            try {
                                GpsRouteLedger193.open(appContext.noBackupFilesDir).deleteWorkout(workoutId)
                            } catch (_: Exception) {
                                // Route cleanup is best effort; tombstone remains mandatory.
                            }
                            db.workoutHistoryDao().upsertTombstone(
                            HistoryTombstoneEntity(
                                primaryId = workoutId,
                                workoutKey = workoutId,
                                idsJson = JSONArray().put(workoutId).toString(),
                                deletedAt = System.currentTimeMillis()
                            )
                        )
                        withContext(Dispatchers.Main) {
                            val current = ActiveWorkoutStore.current(appContext)
                            check(current?.optBoolean("active") == true && current.optString("workoutId") == workoutId) {
                                "Az elvetendő edzés megváltozott."
                            }
                            val execDb = PhoneWorkoutExecution.store(appContext)
                            val frame = execDb.snapshot()
                            if (frame != null && frame.optString("workoutId") == workoutId) {
                                if (frame.optBoolean("active")) check(execDb.discardExecution(workoutId,
                                    frame.optLong("generation"), frame.optLong("executionRevision"))) {
                                    "A natív edzés elvetése nem sikerült."
                                }
                                IntervalTimerService.closeWorkout(workoutId, frame.optLong("generation"))
                                execDb.suspendCommands(false)
                            }
                            val closed = ActiveWorkoutStore.clear(appContext)
                            publishActiveWorkoutSnapshot(appContext, closed)
                            check(WearCommandReceipts.complete(appContext, command, "discarded", uri)) {
                                "Az elvetés visszaigazolása nem menthető."
                            }
                        }
                        val workoutResult = JSONObject()
                            .put("commandId", command.optString("commandId"))
                            .put("workoutId", workoutId)
                            .put("status", "discarded")
                        publishWatchHomeSnapshot(appContext, workoutResult)
                    }

                    "saveWorkoutFeedback" -> {
                        val workoutId = command.optString("workoutId")
                        val feedback = command.optJSONObject("feedback")
                        if (workoutId.isNotBlank() && feedback != null) {
                            val existing = db.workoutHistoryDao().getWorkoutWithDetails(workoutId)
                            if (existing != null) {
                                val raw = JSONObject(existing.workout.rawJson)
                                raw.put("feedback", feedback.put("updatedAt", System.currentTimeMillis()))
                                migrator.saveWorkoutJsonToRoom(raw)
                            }
                            if (settingsStore.current().pendingFeedbackWorkoutId == workoutId) {
                                settingsStore.setPendingFeedback(null, null)
                            }
                        }
                        withContext(Dispatchers.Main) {
                            WearCommandReceipts.complete(appContext, command, "applied", uri)
                        }
                        publishWatchHomeSnapshot(appContext)
                    }

                    "healthSummary" -> {
                        val workoutId = command.optString("workoutId")
                        val summary = command.optJSONObject("healthSummary")
                        if (workoutId.isNotBlank() && summary != null) {
                            val existing = db.workoutHistoryDao().getWorkoutWithDetails(workoutId)
                            if (existing != null) {
                                val raw = JSONObject(existing.workout.rawJson)
                                raw.put("wear", summary)
                                migrator.saveWorkoutJsonToRoom(raw)
                            }
                        }
                        withContext(Dispatchers.Main) {
                            WearCommandReceipts.complete(appContext, command, "applied", uri)
                        }
                        publishWatchHomeSnapshot(appContext)
                    }

                    "requestHome" -> {
                        publishWatchHomeSnapshot(appContext)
                        withContext(Dispatchers.Main) {
                            WearCommandReceipts.complete(appContext, command, "processed", uri)
                        }
                    }

                    else -> {
                        withContext(Dispatchers.Main) {
                            WearCommandReceipts.complete(appContext, command, "processed", uri)
                        }
                    }
                }
                Unit // Keep the metadata lease operation's result invariant.
                }
            } catch (_: Exception) {
                if (uri != null) {
                    try {
                        WearCommandQueueStore.enqueue(appContext, command, uri.toString())
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    private fun attachWatchRouteBindings193(context: Context, workoutId: String, workoutJson: JSONObject) {
        try {
            val bindings = GpsRouteLedger193.open(context.noBackupFilesDir).bindings(workoutId)
            if (bindings.length() == 0) return
            val mapBySetId = HashMap<String, String>()
            for (i in 0 until bindings.length()) {
                val b = bindings.optJSONObject(i) ?: continue
                val setId = b.optString("setId")
                val routeKey = b.optString("routeKey")
                if (setId.isNotBlank() && routeKey.isNotBlank()) {
                    mapBySetId[setId] = routeKey
                }
            }
            val exercises = workoutJson.optJSONArray("exercises") ?: return
            for (i in 0 until exercises.length()) {
                val sets = exercises.optJSONObject(i)?.optJSONArray("sets") ?: continue
                for (j in 0 until sets.length()) {
                    val s = sets.optJSONObject(j) ?: continue
                    val setId = s.optString("setId")
                    val routeKey = mapBySetId[setId]
                    if (routeKey != null) {
                        s.put("gpsRouteKey193", routeKey)
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    /** Phone-owned native edits must reach its durable engine before acknowledging the watch. */
    internal fun commitWatchEditorMutation(context: Context, current: JSONObject, command: JSONObject): JSONObject {
        check(PhoneOwnership193.commandAllowed(context, command)) { "Workout ownership changed" }
        val action = command.optString("action")
        val phoneEditor = current.optInt("nativeEditorProjection") == 1 && current.optString("timerOwner") == "phone"
        if (!phoneEditor || action !in setOf("updateSet", "selectExercise", "nextExercise", "prevExercise", "completeSet", "skipRest")) {
            return ActiveWorkoutStore.saveOwned(context, applyWatchMutationToSnapshot(current, command))
        }
        val frame = NativeWorkoutEditing.project(context)
        check(frame.optString("workoutId") == command.optString("workoutId") &&
            !frame.optBoolean("workoutPaused") && frame.optLong("pauseRevision") == command.optLong("pauseRevision")) {
            "Workout pause or identity changed"
        }
        val exercises = frame.getJSONArray("exercises")
        if (action in setOf("updateSet", "selectExercise", "completeSet")) {
            val exercise = exercises.getJSONObject(command.getInt("exerciseIndex"))
            check(exercise.getString("id") == command.getString("exerciseId")) { "Exercise identity changed" }
            if (action in setOf("updateSet", "completeSet")) {
                val set = exercise.getJSONArray("sets").getJSONObject(command.getInt("setIndex"))
                check(set.getString("setId") == command.getString("setId")) {
                    "Set identity changed"
                }
                if (action == "updateSet") {
                    check(!set.optBoolean("done")) { "Set identity changed" }
                    check(command.optString("field") in setOf("weight", "reps", "leftSeconds", "rightSeconds")) {
                        "Unsupported set field"
                    }
                } else if (set.optBoolean("done")) {
                    return frame
                }
            }
        }
        val next = applyWatchMutationToSnapshot(frame, command)
        if (next.optInt("currentExercise") != frame.optInt("currentExercise")) {
            NativeSetStopwatch.sealPause(next)
            NativeSetStopwatch.pause(next, trackPause = false)
            next.put("selectionRevision", frame.optLong("selectionRevision") + 1L)
        }
        return NativeWorkoutEditing.commitEdit(context, next, clockEdit = action != "updateSet")
    }

    private fun applyWatchMutationToSnapshot(current: JSONObject, command: JSONObject): JSONObject {
        val next = JSONObject(current.toString())
        val action = command.optString("action")
        val exercises = next.optJSONArray("exercises") ?: JSONArray()
        next.put("localSequence", max(next.optLong("localSequence"), command.optLong("sequence")))

        when (action) {
            "selectExercise" -> {
                val idx = command.optInt("exerciseIndex", next.optInt("currentExercise"))
                    .coerceIn(0, (exercises.length() - 1).coerceAtLeast(0))
                next.put("currentExercise", idx)
            }
            "nextExercise" -> {
                val idx = (next.optInt("currentExercise") + 1)
                    .coerceIn(0, (exercises.length() - 1).coerceAtLeast(0))
                next.put("currentExercise", idx)
            }
            "prevExercise" -> {
                val idx = (next.optInt("currentExercise") - 1)
                    .coerceIn(0, (exercises.length() - 1).coerceAtLeast(0))
                next.put("currentExercise", idx)
            }
            "skipRest" -> {
                next.put("restEndAt", 0L)
            }
            "toggleAutomatic" -> {
                val enabled = command.optBoolean("value", false)
                next.put("automaticEnabled", enabled)
                val auto = next.optJSONObject("automation") ?: JSONObject().put("owner", next.optString("timerOwner", "watch"))
                auto.put("enabled", enabled)
                next.put("automation", auto)
            }
            "updateSet" -> {
                val eIdx = command.optInt("exerciseIndex", next.optInt("currentExercise"))
                val sIdx = command.optInt("setIndex", 0)
                val field = command.optString("field")
                val ex = exercises.optJSONObject(eIdx)
                val set = ex?.optJSONArray("sets")?.optJSONObject(sIdx)
                if (set != null && !set.optBoolean("done")) {
                    when (field) {
                        "weight" -> set.put("weight", command.opt("value")?.toString().orEmpty())
                        "reps" -> set.put("reps", command.opt("value")?.toString().orEmpty())
                        "leftSeconds" -> {
                            val left = command.optInt("value", 0).coerceAtLeast(0)
                            set.put("leftSeconds", left)
                            val right = set.optInt("rightSeconds", 0)
                            if (left > 0 && right > 0) set.put("reps", min(left, right).toString())
                        }
                        "rightSeconds" -> {
                            val right = command.optInt("value", 0).coerceAtLeast(0)
                            set.put("rightSeconds", right)
                            val left = set.optInt("leftSeconds", 0)
                            if (left > 0 && right > 0) set.put("reps", min(left, right).toString())
                        }
                    }
                }
            }
            "completeSet" -> {
                val eIdx = command.optInt("exerciseIndex", next.optInt("currentExercise"))
                val sIdx = command.optInt("setIndex", 0)
                val ex = exercises.optJSONObject(eIdx)
                val set = ex?.optJSONArray("sets")?.optJSONObject(sIdx)
                if (ex != null && set != null && !set.optBoolean("done")) {
                    val before = JSONObject(next.toString())
                    if (command.has("reps")) set.put("reps", command.optString("reps"))
                    if (command.has("weight")) set.put("weight", command.optString("weight"))
                    if (command.has("leftSeconds")) set.put("leftSeconds", command.optInt("leftSeconds"))
                    if (command.has("rightSeconds")) set.put("rightSeconds", command.optInt("rightSeconds"))
                    set.put("done", true)
                    next.put("currentExercise", eIdx)
                    val restSec = next.optInt("restSeconds", 90)
                    next.put("restEndAt", System.currentTimeMillis() + restSec * 1000L)
                    try {
                        val target = JSONObject().put("exerciseId", ex.getString("id")).put("setId", set.getString("setId"))
                        next.put("lastRecord188", WorkoutUndo188.capture(before, next, target, command.optString("commandId")))
                    } catch (_: Exception) {
                    }
                }
            }
        }
        command.optJSONObject("automation")?.let { next.put("automation", it) }
        return next
    }

    fun publishActiveWorkoutSnapshot(context: Context, snapshot: JSONObject) {
        val mapRequest = PutDataMapRequest.create(ACTIVE_WORKOUT_PATH)
        mapRequest.dataMap.putBoolean("active", snapshot.optBoolean("active", true))
        mapRequest.dataMap.putLong("revision", snapshot.optLong("revision", 0L))
        mapRequest.dataMap.putString("snapshot", snapshot.toString())
        mapRequest.dataMap.putLong("refreshedAt", System.currentTimeMillis())
        Wearable.getDataClient(context).putDataItem(mapRequest.asPutDataRequest().setUrgent())
    }

    internal suspend fun watchExerciseForStart(db: TrainPilotDatabase,
        lib: com.repforge.app.data.db.ExerciseLibraryEntity,
        settings: com.repforge.app.data.store.TrainPilotUserSettings, overrideSets: JSONArray? = null): JSONObject {
        val setsArr = JSONArray()
        if (overrideSets != null && overrideSets.length() > 0) {
            for (j in 0 until overrideSets.length()) {
                val s = overrideSets.optJSONObject(j) ?: continue
                setsArr.put(
                    JSONObject()
                        .put("set", s.optInt("set", j + 1))
                        .put("weight", s.opt("weight")?.toString().orEmpty())
                        .put("reps", s.optString("reps", lib.defaultReps))
                )
            }
        } else {
            for (j in 1..lib.defaultSets.coerceAtLeast(1)) {
                setsArr.put(
                    JSONObject()
                        .put("set", j)
                        .put("weight", if (lib.defaultWeight > 0.0) lib.defaultWeight.toString() else "0")
                        .put("reps", lib.defaultReps)
                )
            }
        }
        val exercise = JSONObject()
            .put("id", lib.id)
            .put("name", if (settings.language == "en") lib.nameEn else lib.nameHu)
            .put("loadType", lib.loadType)
            .put("repUnit", lib.repUnit)
            .put("measurementType", lib.measurementType)
            .put("targetReps", lib.defaultReps)
            .put("sets", setsArr)
        NativeWorkoutSession.prefillFromHistory(db, exercise, setsArr)
        return exercise
    }

    /**
     * Builds the canonical WatchHomeSnapshot from Room + DataStore and publishes it
     * to `/trainpilot/watch-home` so the watch has up-to-date programs, calendar,
     * quick exercises, Mini Journal (recentWorkouts), theme, and pendingFeedback.
     */
    suspend fun publishWatchHomeSnapshot(context: Context, workoutResult: JSONObject? = null) =
        withContext(Dispatchers.IO) {
            val appContext = context.applicationContext
            val db = TrainPilotDatabase.getInstance(appContext)
            val settingsStore = TrainPilotSettingsStore(appContext)
            TrainPilotDatabase.ensureBuiltinCatalogSeeded(db, appContext)

            val settings = settingsStore.current()
            val allExercises = db.exerciseLibraryDao().getAllExercises().associateBy { it.id }
            val programsWithDays = db.programDao().getProgramsWithDays()
            val activeProgram = programsWithDays.firstOrNull { it.program.id == settings.activeProgramId }
                ?: programsWithDays.firstOrNull()

            suspend fun buildWatchExerciseJson(exId: String, overrideSets: JSONArray? = null): JSONObject? {
                val lib = allExercises[exId] ?: return null
                return watchExerciseForStart(db, lib, settings, overrideSets)
            }

            suspend fun buildWatchDayJson(progId: String, progName: String, dayId: String, dayName: String, exercisesJson: String): JSONObject? {
                val rawArr = try {
                    JSONArray(exercisesJson)
                } catch (_: Exception) {
                    JSONArray()
                }
                val exList = JSONArray()
                for (i in 0 until rawArr.length()) {
                    val item = rawArr.opt(i)
                    when (item) {
                        is String -> buildWatchExerciseJson(item)?.let { exList.put(it) }
                        is JSONObject -> {
                            val id = item.optString("id")
                            buildWatchExerciseJson(id, item.optJSONArray("sets"))?.let { exList.put(it) }
                        }
                    }
                }
                if (exList.length() == 0) return null
                return JSONObject()
                    .put("id", dayId)
                    .put("name", dayName)
                    .put("programId", progId)
                    .put("programName", progName)
                    .put("exercises", exList)
            }

            val activeDaysArray = JSONArray()
            val allCalendarDaysArray = JSONArray()
            for (pWithDays in programsWithDays) {
                val p = pWithDays.program
                val pName = if (settings.language == "en") p.nameEn else p.nameHu
                for (d in pWithDays.days.sortedBy { it.sortOrder }) {
                    val dName = if (settings.language == "en") d.nameEn else d.nameHu
                    val built = buildWatchDayJson(p.id, pName, d.dayId, dName, d.exercisesJson) ?: continue
                    allCalendarDaysArray.put(built)
                    if (p.id == activeProgram?.program?.id) {
                        activeDaysArray.put(built)
                    }
                }
            }

            // Calendar entries (up to 28 around today)
            val today = LocalDate.now()
            val fromDate = today.minusDays(7).toString()
            val toDate = today.plusDays(21).toString()
            val scheduleRows = db.calendarScheduleDao().getEntriesInRange(fromDate, toDate).take(28)
            val calendarArray = JSONArray()
            var recommendedJson: JSONObject? = null

            for (entry in scheduleRows) {
                calendarArray.put(
                    JSONObject()
                        .put("date", entry.date)
                        .put("scheduleId", entry.id)
                        .put("plannedStart", entry.startIso)
                        .put("programId", entry.programId)
                        .put("dayId", entry.dayId)
                        .put("status", entry.status)
                )
                if (recommendedJson == null && entry.status == "planned" && entry.date >= today.toString()) {
                    for (i in 0 until allCalendarDaysArray.length()) {
                        val dayCandidate = allCalendarDaysArray.getJSONObject(i)
                        if (dayCandidate.optString("programId") == entry.programId &&
                            dayCandidate.optString("id") == entry.dayId
                        ) {
                            recommendedJson = JSONObject()
                                .put("scheduleId", entry.id)
                                .put("plannedStart", entry.startIso)
                                .put("day", dayCandidate)
                            break
                        }
                    }
                }
            }
            if (recommendedJson == null && activeDaysArray.length() > 0) {
                recommendedJson = JSONObject()
                    .put("scheduleId", "")
                    .put("plannedStart", "")
                    .put("day", activeDaysArray.getJSONObject(0))
            }

            // Quick exercises catalog
            val quickArray = JSONArray()
            for (ex in allExercises.values.sortedBy { it.nameHu }) {
                buildWatchExerciseJson(ex.id)?.let { quickArray.put(it) }
            }

            // Recent workouts (Mini Journal max 8)
            val recentWorkoutsArray = JSONArray()
            val recentHistory = db.workoutHistoryDao().getWearMiniJournal()
            for (h in recentHistory) {
                val exArr = JSONArray()
                for (exWithSets in h.exercises.sortedBy { it.exercise.orderIndex }.take(14)) {
                    val sArr = JSONArray()
                    for (s in exWithSets.sets.filter { it.done }.sortedBy { it.setNumber }.take(12)) {
                        val sObj = JSONObject()
                            .put("number", s.setNumber)
                            .put("reps", s.reps)
                        if (s.weight != null && s.weight > 0.0) sObj.put("weight", s.weight)
                        if (s.leftSeconds != null && s.leftSeconds > 0) sObj.put("leftSeconds", s.leftSeconds)
                        if (s.rightSeconds != null && s.rightSeconds > 0) sObj.put("rightSeconds", s.rightSeconds)
                        if (s.distanceMeters != null && s.distanceMeters > 0.0) sObj.put("distanceMeters", s.distanceMeters)
                        sArr.put(sObj)
                    }
                    if (sArr.length() > 0) {
                        exArr.put(
                            JSONObject()
                                .put("name", exWithSets.exercise.nameHu)
                                .put("sets", sArr)
                        )
                    }
                }
                val itemObj = JSONObject()
                    .put("workoutId", h.workout.id)
                    .put("started", h.workout.started)
                    .put("finished", h.workout.finished)
                    .put("dayName", h.workout.dayName.ifBlank { "Edzés" })
                    .put("quickWorkout", h.workout.quickWorkout)
                    .put("exercises", exArr)
                NativeSavedWearHealth.read(h.workout)?.let {
                    itemObj.put("wear", it)
                }
                h.workout.health240Json?.let {
                    runCatching { itemObj.put("healthConnect", JSONObject(it)) }
                }
                recentWorkoutsArray.put(itemObj)
            }

            val draftDoc = db.keyValueDocumentDao().getDocument("draft")
            val activeWorkout = ActiveWorkoutStore.current(appContext)
            val hasDraft = (activeWorkout != null && activeWorkout.optBoolean("active", false)) ||
                (draftDoc != null && draftDoc.json.isNotBlank() && draftDoc.json != "null" && draftDoc.json != "{}")

            val now = System.currentTimeMillis()
            val homeSnapshot = JSONObject()
                .put("schema", 1)
                .put("activeProgramId", activeProgram?.program?.id.orEmpty())
                .put("activeProgramName", activeProgram?.program?.nameHu ?: "TrainPilot")
                .put("restSeconds", settings.restSeconds)
                .put("hasDraft", hasDraft)
                .put("days", activeDaysArray)
                .put("calendarDays", allCalendarDaysArray)
                .put("calendar", calendarArray)
                .put("quickExercises", quickArray)
                .put("recentWorkouts", recentWorkoutsArray)
                .put("journalSchema", 1)
                .put("phoneVersion", BuildConfig.VERSION_NAME)
                .put("publishedAt", now)
                .put("theme", settingsStore.current().themeJson())
                .put("automaticProtocol", 1)
                .put("ownershipProtocol193", 1)

            if (recommendedJson != null) {
                homeSnapshot.put("recommended", recommendedJson)
            }
            if (settings.pendingFeedbackWorkoutId.isNotBlank() && settings.pendingFeedbackStarted.isNotBlank()) {
                homeSnapshot.put(
                    "pendingFeedback",
                    JSONObject()
                        .put("workoutId", settings.pendingFeedbackWorkoutId)
                        .put("started", settings.pendingFeedbackStarted)
                )
            }
            if (workoutResult != null) {
                homeSnapshot.put("workoutResult", workoutResult)
            }

            val mapRequest = PutDataMapRequest.create(WATCH_HOME_PATH)
            mapRequest.dataMap.putString("snapshot", homeSnapshot.toString())
            mapRequest.dataMap.putLong("publishedAt", now)
            Wearable.getDataClient(appContext).putDataItem(mapRequest.asPutDataRequest().setUrgent())
        }

    /**
     * Opens the watch app remotely to start sensor measurement for the active phone workout.
     */
    suspend fun startWatchMeasurementRemotely(activity: Activity, workoutId: String, requestOwnership: Boolean = false): Result<Boolean> =
        withContext(Dispatchers.Main) {
            val requestedAt = SystemClock.elapsedRealtime()
            try {
                val nodes = Wearable.getNodeClient(activity).connectedNodes.await()
                val current = ActiveWorkoutStore.current(activity)
                if (SystemClock.elapsedRealtime() - requestedAt > 15_000L) {
                    return@withContext Result.failure(IllegalStateException("The watch start request expired."))
                }
                if (!activity.hasWindowFocus()) {
                    return@withContext Result.failure(IllegalStateException("The phone workout must be visible to open the watch."))
                }
                if (current == null || workoutId.isEmpty() || !current.optBoolean("active", false) ||
                    workoutId != current.optString("workoutId")
                ) {
                    return@withContext Result.failure(IllegalStateException("The requested workout is no longer active."))
                }
                if (nodes.size != 1) {
                    return@withContext Result.failure(
                        IllegalStateException(if (nodes.isEmpty()) "No watch is connected." else "More than one watch is connected.")
                    )
                }
                val uri = Uri.Builder()
                    .scheme("trainpilot")
                    .authority("wear")
                    .path("/start-measurement")
                    .appendQueryParameter("workoutId", workoutId)
                    .appendQueryParameter("revision", current.optLong("revision", 0L).toString())
                    .apply { if (requestOwnership) appendQueryParameter("ownership", "watch") }
                    .build()
                val intent = Intent(Intent.ACTION_VIEW).setData(uri).addCategory(Intent.CATEGORY_BROWSABLE)
                val task = RemoteActivityHelper(activity, ContextCompat.getMainExecutor(activity))
                    .startRemoteActivity(intent, nodes[0].id)
                kotlinx.coroutines.suspendCancellableCoroutine { cont ->
                    task.addListener(
                        {
                            try {
                                task.get()
                                cont.resume(Result.success(true), null)
                            } catch (e: Exception) {
                                cont.resume(Result.failure(e), null)
                            }
                        },
                        ContextCompat.getMainExecutor(activity)
                    )
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
