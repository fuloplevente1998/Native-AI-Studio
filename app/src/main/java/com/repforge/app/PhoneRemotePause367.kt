package com.repforge.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/** Only the canonical native phone engine may pause/resume a phone-owned workout. */
internal object PhoneRemotePause367 {
    fun proof(command: JSONObject): JSONObject = JSONObject()
        .put("id", command.optString("commandId"))
        .put("workoutId", command.optString("workoutId"))
        .put("generation", command.optLong("generation", -1))
        .put("epoch", command.optLong("ownerEpoch193", -1))
        .put("basePause", command.optLong("pauseRevision", -1))
        .put("paused", command.optBoolean("paused"))
        .put("requestedRevision", command.optLong("requestedRevision", -1))

    fun sameProof(frame: JSONObject, command: JSONObject): Boolean {
        val stamp = frame.optJSONObject("remotePause367") ?: return false
        val expected = proof(command)
        return expected.keys().asSequence().all { stamp.opt(it)?.toString() == expected.opt(it)?.toString() } &&
            frame.optLong("pauseRevision") == command.optLong("requestedRevision") &&
            frame.optBoolean("workoutPaused") == command.optBoolean("paused")
    }

    fun identity(frame: JSONObject?, command: JSONObject): Boolean =
        frame != null && command.optString("action") == "phonePause367" &&
            command.optString("commandId").length in 1..128 && command.opt("paused") is Boolean &&
            command.optLong("sequence") > 0 && frame.optBoolean("active") &&
            frame.optInt("nativeEditorProjection") == 1 && frame.optString("timerOwner") == "phone" &&
            frame.optString("workoutId") == command.optString("workoutId") &&
            frame.optLong("generation") > 0 && frame.optLong("generation") == command.optLong("generation", -1) &&
            frame.optLong("ownerEpoch193") == command.optLong("ownerEpoch193", -1) &&
            command.optLong("requestedRevision", -1) in 1L..9_007_199_254_740_991L &&
            command.optLong("requestedRevision") > command.optLong("pauseRevision", -1)

    /** Called on Main under the existing data replacement lease. No watch-side optimistic pause. */
    fun apply(context: Context, command: JSONObject): Boolean {
        val store = PhoneWorkoutExecution.store(context)
        val frame = store.snapshot() ?: return false
        val id = command.optString("workoutId")
        if (!identity(frame, command) || !PhoneOwnership193.commandAllowed(context, command) ||
            !PhoneOwnership193.phoneAllowed(context, id) || store.restoreBlocked() || store.commandsSuspended()) return false
        val active = ActiveWorkoutStore.current(context) ?: return false
        if (!active.optBoolean("active") || active.optString("workoutId") != id ||
            active.optBoolean("finalizationPending") || active.optString("timerOwner") != "phone" ||
            active.optLong("ownerEpoch193") != frame.optLong("ownerEpoch193")) return false
        // A crash after the canonical write but before the receipt replays the same state, not a toggle.
        if (sameProof(frame, command)) {
            PhoneWorkoutExecution.setWorkoutPause(context, frame)
            return true
        }
        if (frame.optLong("pauseRevision") != command.optLong("pauseRevision") ||
            frame.optBoolean("workoutPaused") == command.optBoolean("paused")) return false
        val next = prepare(frame, command, System.currentTimeMillis()) ?: return false
        PhoneWorkoutExecution.setWorkoutPause(context, next)
        return store.snapshot()?.let { sameProof(it, command) } == true
    }

    fun prepare(frame: JSONObject, command: JSONObject, now: Long): JSONObject? {
        if (!identity(frame, command) || frame.optLong("pauseRevision") != command.optLong("pauseRevision") ||
            frame.optBoolean("workoutPaused") == command.optBoolean("paused")) return null
        val paused = command.optBoolean("paused")
        val intervals = frame.optJSONArray("activeIntervals")?.let { JSONArray(it.toString()) }
            ?: JSONArray().put(JSONObject().put("start", frame.optString("started")).put("end", JSONObject.NULL))
        if (paused) {
            for (i in 0 until intervals.length()) {
                val row = intervals.optJSONObject(i) ?: continue
                if (row.isNull("end") || row.optString("end").isEmpty())
                    row.put("end", Instant.ofEpochMilli(now).toString())
            }
        } else {
            intervals.put(JSONObject().put("start", Instant.ofEpochMilli(now).toString()).put("end", JSONObject.NULL))
        }
        return JSONObject(frame.toString()).put("workoutPaused", paused)
            .put("pauseRevision", command.optLong("requestedRevision"))
            .put("activeIntervals", intervals).put("remotePause367", proof(command))
    }
}
