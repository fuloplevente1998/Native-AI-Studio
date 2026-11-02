package com.repforge.app

import android.content.Context
import android.net.Uri
import com.repforge.app.data.db.HistoryTombstoneEntity
import com.repforge.shared.GpsRoutePacket193
import com.repforge.shared.WorkoutOwnership193
import org.json.JSONArray
import com.repforge.app.data.db.TrainPilotDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** A prior durable deletion may be acknowledged without clearing the current phone workout. */
internal object NativeWatchDiscardAdmission {
    enum class Decision { APPLY, ALREADY_DISCARDED, RECORD_ORPHAN_DISCARD, REJECT }

    fun decide(command: JSONObject, current: JSONObject?, execution: JSONObject?, ownershipAllowed: Boolean,
        tombstoned: Boolean, saved: Boolean, restoreBlocked: Boolean = false, orphanAuthorized: Boolean = false): Decision {
        val id = command.optString("workoutId")
        if (command.optString("action") != "discardWorkout" || id.isBlank() || restoreBlocked) return Decision.REJECT
        val activeDraft = current?.optBoolean("active") == true && current.optString("workoutId") == id
        val activeExecution = execution?.optBoolean("active") == true
        val sameExecution = activeExecution && execution?.optString("workoutId") == id
        if (tombstoned && !activeDraft && !sameExecution) return Decision.ALREADY_DISCARDED
        if (!activeDraft && !sameExecution && !saved && orphanAuthorized) return Decision.RECORD_ORPHAN_DISCARD
        if (!activeDraft || !ownershipAllowed || saved || (current?.optBoolean("finalizationPending") == true && !current.optBoolean("discardPending")) ||
            activeExecution && !sameExecution) return Decision.REJECT
        return Decision.APPLY
    }

    /** The listener supplies the real DataItem URI. This authorizes only the watch's
     * explicit orphan discard, never editing an active draft or deleting saved history. */
    internal fun orphanProof(command: JSONObject): Boolean {
        val frame = command.optJSONObject("finalSnapshot") ?: return false
        val uri = runCatching { Uri.parse(command.optString("_uri")) }.getOrNull() ?: return false
        return command.optInt("schema") == 1 && command.optBoolean("confirmed") &&
            command.optString("commandId").length in 1..128 && command.optString("workoutId").length in 1..96 &&
            command.optLong("sequence") > 0 && uri.scheme == "wear" && GpsRoutePacket193.id(uri.host) &&
            uri.path == WearCommandListenerService.COMMAND_PATH_PREFIX + command.optString("commandId") &&
            frame.optString("workoutId") == command.optString("workoutId") &&
            frame.optInt("ownershipProtocol193") == 1 && frame.optString("timerOwner") in setOf("phone", "watch") &&
            WorkoutOwnership193.owns(frame, frame.optString("timerOwner")) &&
            frame.optLong("ownerEpoch193", -1L) >= 0L &&
            command.optLong("ownerEpoch193", -1L) == frame.optLong("ownerEpoch193")
    }

    suspend fun rejectsResurrection(db: TrainPilotDatabase, command: JSONObject): Boolean =
        command.optString("action") in setOf("startWorkout", "startQuickWorkout", "addQuickExercise", "watchAutomationState", "resumeWorkout") &&
            db.workoutHistoryDao().isTombstoned(command.optString("workoutId"), JSONObject.quote(command.optString("workoutId")))

    /** Called under the existing replacement lease; only the targeted deletion marker/journal can change. */
    suspend fun reconcile(context: Context, db: TrainPilotDatabase, command: JSONObject): Decision {
        val decision = resolve(context, db, command)
        if (decision == Decision.RECORD_ORPHAN_DISCARD) {
            val id = command.getString("workoutId")
            db.workoutHistoryDao().upsertTombstone(HistoryTombstoneEntity(id, id,
                JSONArray().put(id).toString(), System.currentTimeMillis()))
        }
        if (decision in setOf(Decision.RECORD_ORPHAN_DISCARD, Decision.ALREADY_DISCARDED)) {
            val journal = PhoneOwnership193.journal(context)
            val state = journal.read()
            if (state.optString("stage") !in setOf("requested", "offered", "incoming", "accepted", "committed", "installed") &&
                !WorkoutOwnership193.pending(state.optJSONObject("snapshot"))) journal.close(command.getString("workoutId"))
        }
        return decision
    }

    /** Caller owns the replacement lease; the native execution store stays on Main. */
    suspend fun resolve(context: Context, db: TrainPilotDatabase, command: JSONObject): Decision {
        val id = command.optString("workoutId")
        val current = ActiveWorkoutStore.current(context)
        val (execution, blocked) = withContext(Dispatchers.Main.immediate) {
            val store = PhoneWorkoutExecution.store(context)
            store.snapshot() to store.restoreBlocked()
        }
        val journal = PhoneOwnership193.journal(context).read()
        val journalTargetsThis = journal.optJSONObject("snapshot")?.optString("workoutId") == id ||
            journal.optJSONObject("request")?.optString("workoutId") == id
        val pending = journalTargetsThis && (journal.optString("stage") in setOf("requested", "offered", "incoming", "accepted", "committed", "installed") ||
            WorkoutOwnership193.pending(journal.optJSONObject("snapshot")))
        return decide(command, current, execution, !pending && PhoneOwnership193.commandAllowed(context, command),
            db.workoutHistoryDao().isTombstoned(id, JSONObject.quote(id)),
            db.workoutHistoryDao().hasSavedWorkoutId(id), blocked, orphanAuthorized = !pending && orphanProof(command))
    }
}
