package com.repforge.app

import android.content.Context
import org.json.JSONObject

/** A phone-owned rest is changed only through its existing active native timer. */
internal object PhoneRemoteRest367 {
    fun proof(c: JSONObject) = JSONObject().put("id",c.optString("commandId")).put("key",c.optString("key"))
        .put("workoutId",c.optString("workoutId")).put("generation",c.optLong("generation"))
        .put("epoch",c.optLong("ownerEpoch193")).put("pauseRevision",c.optLong("pauseRevision"))
        .put("phaseNumber",c.optLong("phaseNumber")).put("duration",c.optInt("duration")).put("delta",c.optInt("delta"))
        .apply { if(c.has("phaseDurationMs")) put("phaseDurationMs",c.optLong("phaseDurationMs")).put("restRevision",c.optLong("restRevision")) }

    fun sameProof(frame: JSONObject, c: JSONObject): Boolean {
        val saved = frame.optJSONObject("remoteRest367") ?: return false
        val expected = proof(c)
        return expected.keys().asSequence().all { saved.opt(it)?.toString() == expected.opt(it)?.toString() }
    }

    fun matches(frame: JSONObject, timer: JSONObject, c: JSONObject): Boolean =
        c.optString("action") == "phoneRest367" && c.optString("commandId").length in 1..128 &&
        c.optLong("sequence") > 0 && frame.optBoolean("active") && frame.optInt("nativeEditorProjection") == 1 &&
        frame.optString("timerOwner") == "phone" && !frame.optBoolean("workoutPaused") &&
        frame.optString("workoutId") == c.optString("workoutId") && frame.optLong("generation") > 0 &&
        frame.optLong("generation") == c.optLong("generation",-1) && frame.optLong("ownerEpoch193") == c.optLong("ownerEpoch193",-1) &&
        frame.optLong("pauseRevision") == c.optLong("pauseRevision",-1) && c.optInt("delta") in listOf(-15,15) &&
        c.optString("key").isNotBlank() && timer.optString("key") == c.optString("key") &&
        timer.optString("workoutId") == c.optString("workoutId") && timer.optLong("generation") == c.optLong("generation") &&
        timer.optLong("phaseNumber") == c.optLong("phaseNumber",-1) &&
        timer.optBoolean("running") && !timer.optBoolean("interrupted") && !timer.optBoolean("stopped") &&
        timer.optString("phase") == "rest" && if (c.has("phaseDurationMs")) {
            c.optLong("phaseDurationMs") > 0 && timer.optLong("restPhaseDurationMs367") == c.optLong("phaseDurationMs") &&
                timer.optLong("restChangeRevision367") == c.optLong("restRevision",-1)
        } else {
            timer.optBoolean("restOnly") && timer.optInt("initialRest") == 0 &&
                timer.optJSONArray("steps")?.length() == 1 &&
                timer.optJSONArray("steps")?.optJSONObject(0)?.optInt("rest") == c.optInt("duration",-1) &&
                !timer.has("restOverridesMs367")
        }

    fun apply(context: Context,c: JSONObject): Boolean {
        val store = PhoneWorkoutExecution.store(context)
        val frame = store.snapshot() ?: return false
        val id = c.optString("workoutId")
        val active = ActiveWorkoutStore.current(context) ?: return false
        if (!active.optBoolean("active") || active.optBoolean("finalizationPending") ||
            active.optString("workoutId") != id || active.optString("timerOwner") != "phone" ||
            frame.optString("workoutId") != id || frame.optString("timerOwner") != "phone" ||
            frame.optLong("generation") != c.optLong("generation",-1) ||
            frame.optLong("ownerEpoch193") != c.optLong("ownerEpoch193",-1) ||
            !PhoneOwnership193.commandAllowed(context,c) || !PhoneOwnership193.phoneAllowed(context,id) ||
            store.restoreBlocked() || store.commandsSuspended()) return false
        if (sameProof(frame,c)) { PhoneWorkoutExecution.publish(context); return true }
        val clock = IntervalTimerService.snapshot(context)
        if (!matches(frame,clock,c)) return false
        try {
            val target = NativeTimerTarget(id,frame.optLong("generation"),c.optString("key"))
            if(c.has("phaseDurationMs")) IntervalTimerService.adjustPhaseRest367(context,target,c.optInt("delta"),proof(c))
            else IntervalTimerService.adjustRest(context,target,c.optInt("delta"),proof(c))
        } catch (e: Exception) {
            // A durable write may already exist even if a later publication fails.
            if (store.snapshot()?.let { sameProof(it,c) } != true) throw e
        }
        if (store.snapshot()?.let { sameProof(it,c) } != true) return false
        PhoneWorkoutExecution.publish(context)
        return true
    }
}
