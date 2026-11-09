package com.repforge.app

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/** AI Studio presentation adapter to the existing native execution/editing APIs. Main thread only. */
internal object StudioWorkoutBridge {
    /** A library choice edits its original native session, never starts a replacement session. */
    fun switchExercise(context: android.content.Context, target: NativeWorkoutTarget, replacement: JSONObject): JSONObject {
        val frame = NativeWorkoutEditing.project(context)
        val original = target.resolve(frame)
        check(original.exerciseIndex == frame.optInt("currentExercise")) { "Az aktuális gyakorlat megváltozott." }
        check(!NativeGpsMeasurement.busy(frame)) { "Előbb fejezd be a GPS-mérést." }
        val exercises = frame.getJSONArray("exercises")
        val existing = (0 until exercises.length()).firstOrNull {
            exercises.getJSONObject(it).optString("id") == replacement.getString("id")
        }
        if (existing != null) {
            val exercise = exercises.getJSONObject(existing)
            return NativeWorkoutEditing.selectExercise(context, NativeWorkoutTarget(target.workoutId, target.generation,
                exercise.getString("id"), exercise.getJSONArray("sets").getJSONObject(0).getString("setId")))
        }
        NativeSetStopwatch.sealPause(frame)
        NativeSetStopwatch.pause(frame, trackPause = false)
        val clock = frame.optJSONObject(NativeSetStopwatch.FIELD)
        val rows = original.exercise.getJSONArray("sets")
        val hasRecordedValues = (0 until rows.length()).any { index ->
            val set = rows.getJSONObject(index)
            set.optBoolean("done") || set.optLong("leftSeconds") > 0 || set.optLong("rightSeconds") > 0 ||
                set.optBoolean("leftRecorded") || set.optBoolean("rightRecorded") || set.optDouble("distanceMeters") > 0
        }
        // Keep a partially measured exercise addressable under its original IDs. A new
        // exercise gets fresh native sets and cannot inherit its stopwatch or side values.
        val keepOriginal = hasRecordedValues || (clock?.optString("exerciseId") == target.exerciseId && NativeSetStopwatch.elapsed(clock) > 0)
        val index = if (keepOriginal) exercises.length() else original.exerciseIndex
        if (!keepOriginal && clock?.optString("exerciseId") == target.exerciseId) frame.remove(NativeSetStopwatch.FIELD)
        exercises.put(index, JSONObject(replacement.toString()))
        frame.put("currentExercise", index).put("selectionRevision", frame.optLong("selectionRevision") + 1L)
        return NativeWorkoutEditing.commitEdit(context, frame, clockEdit = true)
    }

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
                    // Float display roundtrips must not turn untouched #228 values into manual edits.
                    val originalWeight = old.optDouble("weight", Double.NaN)
                    val displayedWeight = update.optDouble("weight", Double.NaN)
                    if (originalWeight.isFinite() && displayedWeight.isFinite() &&
                        originalWeight.toFloat() == displayedWeight.toFloat()) merged.put("weight", old.get("weight"))
                    if (!update.optBoolean("done") && update.optString("reps") == "0" && old.optString("reps").isEmpty()) {
                        merged.put("reps", "")
                    }
                    if (update.optBoolean("leftRecorded") && update.optInt("leftSec") > 0 && !update.has("leftSeconds")) {
                        merged.put("leftSeconds", update.optInt("leftSec"))
                    }
                    if (update.optBoolean("rightRecorded") && update.optInt("rightSec") > 0 && !update.has("rightSeconds")) {
                        merged.put("rightSeconds", update.optInt("rightSec"))
                    }
                    for (key in listOf("distanceMeters", "distanceSource", "manualDistanceMeters", "gpsMeasuredMeters", "gpsRouteKey163")) {
                        if (old.has(key) && !update.has(key)) merged.put(key, old.get(key))
                    }
                    if (!update.has("actualRir")) merged.remove("actualRir")
                    sets.put(j, merged)
                }
            }
        }
        for (key in listOf("studioGpsKey", "studioIndoor", "restEndAt")) if (candidate.has(key)) edit.put(key, candidate.get(key))
        val nextExerciseIndex = candidate.getInt("currentExercise")
        val exerciseChanged = nextExerciseIndex != frame.optInt("currentExercise")
        if (exerciseChanged) {
            NativeSetStopwatch.sealPause(edit)
            NativeSetStopwatch.pause(edit, trackPause = false)
            edit.put("selectionRevision", frame.optLong("selectionRevision") + 1L)
        }
        edit.put("exercises", exercises).put("currentExercise", nextExerciseIndex)
        return NativeWorkoutEditing.commitEdit(context, edit, clockEdit = exerciseChanged)
    }

    fun startIndoor(context: android.content.Context): JSONObject {
        var frame = NativeWorkoutEditing.project(context)
        if (frame.optBoolean("workoutPaused")) { pause(context, false); frame = NativeWorkoutEditing.project(context) }
        frame.remove("studioGpsKey")
        frame.put("studioIndoor", true)
        val exercise = frame.getJSONArray("exercises").getJSONObject(frame.optInt("currentExercise"))
        val sets = exercise.getJSONArray("sets")
        val set = (0 until sets.length()).map { sets.getJSONObject(it) }.first { !it.optBoolean("done") }
        val target = NativeWorkoutTarget(frame.getString("workoutId"), frame.getLong("generation"), exercise.getString("id"), set.getString("setId"))
        if (frame.optJSONObject(NativeSetStopwatch.FIELD)?.let { NativeSetStopwatch.running(it) } != true) {
            NativeSetStopwatch.start(frame, target, "single")
        }
        val committed = NativeWorkoutEditing.commitEdit(context, frame, clockEdit = true)
        runCatching {
            if (!IntervalTimerService.armed(committed.optString("workoutId"), committed.optLong("generation"))) {
                androidx.core.content.ContextCompat.startForegroundService(
                    context,
                    android.content.Intent(context, IntervalTimerService::class.java).apply {
                        action = IntervalTimerService.ARM
                        putExtra("workoutId", committed.getString("workoutId"))
                        putExtra("generation", committed.getLong("generation"))
                        putExtra("language", committed.optString("language", "hu"))
                    }
                )
            }
            IntervalTimerService.trackSetStopwatch(target.workoutId, target.generation)
        }
        return committed
    }

    /**
     * Validates a user-entered manual distance in kilometers (accepts '.' or ',' decimal separator).
     * Valid range: > 0.0 km and <= 2000.0 km (2,000,000 meters).
     * Returns positive distance in meters, or null if invalid.
     */
    fun validateManualDistanceKm(rawInput: String): Double? {
        val normalized = rawInput.trim().replace(',', '.')
        if (normalized.isEmpty()) return null
        val km = normalized.toDoubleOrNull() ?: return null
        if (!km.isFinite() || km <= 0.0 || km > 2000.0) return null
        val meters = kotlin.math.round(km * 10000.0) / 10.0
        return if (meters > 0.0 && meters <= 2_000_000.0) meters else null
    }

    fun readOutdoorManualDistanceMeters(frame: JSONObject?): Double? {
        val set = frame?.optJSONArray("exercises")?.optJSONObject(0)?.optJSONArray("sets")?.optJSONObject(0) ?: return null
        val manual = set.optDouble("manualDistanceMeters", Double.NaN)
        if (manual.isFinite() && manual > 0.0) return manual
        if (set.optString("distanceSource") == "manual") {
            val d = set.optDouble("distanceMeters", Double.NaN)
            if (d.isFinite() && d > 0.0) return d
        }
        return null
    }

    fun readOutdoorEffectiveDistanceMeters(frame: JSONObject?, liveGpsMeters: Double = 0.0): Double {
        val manual = readOutdoorManualDistanceMeters(frame)
        if (manual != null && manual > 0.0) return manual
        val set = frame?.optJSONArray("exercises")?.optJSONObject(0)?.optJSONArray("sets")?.optJSONObject(0)
        val setDist = set?.optDouble("distanceMeters", Double.NaN)?.takeIf { it.isFinite() && it > 0.0 } ?: 0.0
        return maxOf(setDist, if (liveGpsMeters.isFinite() && liveGpsMeters > 0.0) liveGpsMeters else 0.0)
    }

    /**
     * Persists or clears the manual outdoor distance in both the native execution store and ActiveWorkoutStore.
     */
    fun setOutdoorManualDistance(
        context: android.content.Context,
        manualMeters: Double?,
        markDoneIfPositive: Boolean = false
    ): JSONObject? {
        val current = ActiveWorkoutStore.current(context) ?: return null
        if (!current.optBoolean("active") || current.optString("timerOwner", "phone") != "phone") return null
        val workoutId = current.optString("workoutId")
        val generation = current.optLong("generation")
        if (workoutId.isBlank()) return null
        val updatedExec = PhoneWorkoutExecution.store(context)
            .setOutdoorManualDistance(workoutId, generation, manualMeters, markDoneIfPositive)
            ?: return null
        val nextDraft = JSONObject(current.toString())
        val draftEx = nextDraft.optJSONArray("exercises")?.optJSONObject(0)
        val draftSet = draftEx?.optJSONArray("sets")?.optJSONObject(0)
        val execSet = updatedExec.optJSONArray("exercises")?.optJSONObject(0)?.optJSONArray("sets")?.optJSONObject(0)
        if (draftSet != null && execSet != null) {
            for (key in listOf("distanceMeters", "distanceSource", "manualDistanceMeters", "gpsMeasuredMeters", "done")) {
                if (execSet.has(key)) draftSet.put(key, execSet.get(key)) else draftSet.remove(key)
            }
        }
        updatedExec.optJSONObject(NativeGpsMeasurement.FIELD)?.let { nextDraft.put(NativeGpsMeasurement.FIELD, JSONObject(it.toString())) }
        ActiveWorkoutStore.saveOwned(context, nextDraft)
        PhoneWorkoutExecution.publish(context)
        return ActiveWorkoutStore.current(context)
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
