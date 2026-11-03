package com.repforge.app

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/** AI Studio presentation adapter to the existing native execution/editing APIs. Main thread only. */
internal object StudioWorkoutBridge {
    fun persist(context: android.content.Context, candidate: JSONObject): JSONObject {
        val existing = ActiveWorkoutStore.current(context)?.takeIf { it.optBoolean("active") }
            ?: return NativeWorkoutSession.installPrepared(context, candidate)
        check(existing.optString("workoutId") == candidate.optString("workoutId")) { "Másik edzés aktív." }
        val frame = PhoneWorkoutExecution.store(context).snapshot() ?: error("A natív edzés hiányzik.")
        check(frame.optString("workoutId") == existing.optString("workoutId"))
        val edit = JSONObject(frame.toString())
        val originalExercises = frame.getJSONArray("exercises")
        val exercises = candidate.getJSONArray("exercises")
        for (i in 0 until exercises.length()) {
            val exercise = exercises.getJSONObject(i)
            val oldExercise = (0 until originalExercises.length()).map { originalExercises.getJSONObject(it) }
                .firstOrNull { it.optString("id") == exercise.optString("id") }
            if (oldExercise != null) {
                for (key in oldExercise.keys()) if (!exercise.has(key)) exercise.put(key, oldExercise.get(key))
                // These come from the durable exercise, not Studio's old display guesses.
                for (key in listOf("loadType", "repUnit", "measurementType", "prescription", "targetReps"))
                    if (oldExercise.has(key)) exercise.put(key, oldExercise.get(key))
            }
            val sets = exercise.getJSONArray("sets")
            for (j in 0 until sets.length()) {
                val update = sets.getJSONObject(j)
                val oldSets = oldExercise?.optJSONArray("sets") ?: JSONArray()
                val old = (0 until oldSets.length()).map { oldSets.getJSONObject(it) }
                    .firstOrNull { it.optString("setId") == update.optString("setId") }
                if (old != null) {
                    val merged = JSONObject(old.toString())
                    update.keys().forEach { merged.put(it, update.get(it)) }
                    if (!update.has("actualRir")) merged.remove("actualRir")
                    sets.put(j, merged)
                }
            }
        }
        for (key in listOf("studioGpsKey", "studioIndoor")) if (candidate.has(key)) edit.put(key, candidate.get(key))
        edit.put("exercises", exercises).put("currentExercise", candidate.getInt("currentExercise"))
        return NativeWorkoutEditing.commitEdit(context, edit)
    }

    fun startIndoor(context: android.content.Context): JSONObject {
        var frame = NativeWorkoutEditing.project(context)
        if (frame.optBoolean("workoutPaused")) { pause(context, false); frame = NativeWorkoutEditing.project(context) }
        val exercise = frame.getJSONArray("exercises").getJSONObject(frame.optInt("currentExercise"))
        val sets = exercise.getJSONArray("sets")
        val set = (0 until sets.length()).map { sets.getJSONObject(it) }.first { !it.optBoolean("done") }
        val target = NativeWorkoutTarget(frame.getString("workoutId"), frame.getLong("generation"), exercise.getString("id"), set.getString("setId"))
        NativeSetStopwatch.start(frame, target, "single")
        return NativeWorkoutEditing.commitEdit(context, frame, clockEdit = true)
    }

    /** A sticky handover does not navigate away first; close the real active interval before an offer. */
    fun pauseForHandover(context: android.content.Context, workoutId: String): JSONObject {
        val current = ActiveWorkoutStore.current(context) ?: error("Nincs aktív edzés.")
        check(current.optString("workoutId") == workoutId) { "Az edzés azonosítója megváltozott." }
        val paused = pause(context, true)
        check(paused.optString("workoutId") == workoutId && paused.optBoolean("workoutPaused") &&
            paused.optString("timerOwner", "phone") == "phone") { "Az edzés nem áll készen az órás átadásra." }
        return paused
    }

    /** Matches the original native editor's interval/rest/pause-revision contract. */
    fun pause(context: android.content.Context, paused: Boolean): JSONObject {
        val current = ActiveWorkoutStore.current(context) ?: error("Nincs aktív edzés.")
        check(current.optBoolean("active") && current.optString("timerOwner", "phone") == "phone" &&
            !current.optBoolean("finalizationPending") && !com.repforge.shared.WorkoutOwnership193.pending(current))
        if (current.optBoolean("workoutPaused") == paused) return current
        val now = System.currentTimeMillis()
        val next = transition(current, paused, now)
        PhoneWorkoutExecution.setWorkoutPause(context, next)
        return checkNotNull(ActiveWorkoutStore.current(context))
    }

    internal fun transition(current: JSONObject, paused: Boolean, now: Long): JSONObject {
        val next = JSONObject(current.toString())
        if (current.optBoolean("workoutPaused") == paused) return next
        next.put("workoutPaused", paused).put("pauseRevision", maxOf(now, current.optLong("pauseRevision") + 1))
        next.put("pausedRestMillis", if (paused) maxOf(0, current.optLong("restEndAt") - now) else 0L)
        next.put("restEndAt", if (!paused && current.optLong("pausedRestMillis") > 0) now + current.optLong("pausedRestMillis") else 0L)
        val intervals = next.optJSONArray("activeIntervals") ?: JSONArray()
        if (paused) {
            for (i in 0 until intervals.length()) intervals.getJSONObject(i).let {
                if (it.isNull("end") || it.optString("end").isBlank()) it.put("end", Instant.ofEpochMilli(now).toString())
            }
        } else intervals.put(JSONObject().put("start", Instant.ofEpochMilli(now).toString()).put("end", JSONObject.NULL))
        return next.put("activeIntervals", intervals)
    }
}
