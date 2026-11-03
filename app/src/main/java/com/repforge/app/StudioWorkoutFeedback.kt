package com.repforge.app

import org.json.JSONObject

/** The six Studio choices use the same machine values as the canonical Coach. */
internal object StudioWorkoutFeedback {
    val efforts = listOf("easy", "light", "right", "challenging", "hard", "pain")

    /** Only explicit edits touch metadata; absence never implies a rating or RIR. */
    fun editExercise(exercise: JSONObject, effort: String?, note: String, locked: Boolean, now: Long) {
        val previous = exercise.opt("effort") as? String
        if (effort != previous) {
            require(effort == null || effort in efforts)
            check(!locked && exercise.optJSONObject("feedback")?.optBoolean("applied") != true) {
                "Feedback already applied to a program"
            }
            if (effort == null) exercise.remove("effort") else exercise.put("effort", effort)
            // Keep existing applied flags and unrelated metadata; remove stale duplicated answers.
            exercise.optJSONObject("feedback")?.let { feedback ->
                listOf("rating", "effort", "effortScore", "suggestion").forEach { feedback.remove(it) }
                feedback.put("updatedAt", now)
            }
        }
        val oldNote = (exercise.optJSONObject("feedback")?.opt("note") as? String).orEmpty()
        if (note != oldNote) {
            require(note.length <= 3000 && note.none { it.isISOControl() && it != '\n' })
            val feedback = exercise.optJSONObject("feedback") ?: JSONObject()
            exercise.put("feedback", feedback.put("note", note).put("updatedAt", now))
        }
    }

    private const val PENDING = "studioExerciseFeedback"

    fun attachExerciseEdits(workout: JSONObject, ownerId: String, choices: Map<String, Int>, notes: Map<String, String>) {
        if (ownerId.isBlank() || ownerId != workout.optString("workoutId") || (choices.isEmpty() && notes.isEmpty())) return
        workout.put(PENDING, JSONObject().put("workoutId", ownerId)
            .put("choices", JSONObject().also { json -> choices.forEach { (id, index) -> require(index in -1..5); json.put(id, index) } })
            .put("notes", JSONObject().also { json -> notes.forEach { (id, note) -> json.put(id, note) } }))
    }

    /** Merge annotations onto the latest canonical draft; never replace its sets with a UI projection. */
    fun mergePendingExerciseEdits(input: JSONObject, draft: JSONObject, now: Long) {
        val pending = input.optJSONObject(PENDING) ?: return
        check(input.optString("workoutId") == draft.optString("workoutId") &&
            pending.optString("workoutId") == draft.optString("workoutId"))
        for (key in listOf("generation", "ownerEpoch193")) {
            if (input.has(key) && draft.has(key)) check(input.getLong(key) == draft.getLong(key)) { "Workout changed" }
        }
        val choices = pending.getJSONObject("choices")
        val notes = pending.getJSONObject("notes")
        val choiceMap = choices.keys().asSequence().associateWith { choices.getInt(it).also { index -> require(index in -1..5) } }
        val noteMap = notes.keys().asSequence().associateWith { notes.getString(it) }
        applyExercises(draft, draft.getString("workoutId"), choiceMap, noteMap, now)
        draft.remove(PENDING)
    }

    fun applyExercises(workout: JSONObject, ownerId: String, efforts: Map<String, Int>, notes: Map<String, String>, now: Long) {
        if (ownerId.isBlank() || workout.optString("workoutId") != ownerId) return
        require(efforts.values.all { it in -1..5 })
        val exercises = workout.optJSONArray("exercises") ?: return
        for (i in 0 until exercises.length()) {
            val ex = exercises.getJSONObject(i)
            val id = ex.optString("id")
            if (id !in efforts && id !in notes) continue
            editExercise(ex, if (id in efforts) this.efforts.getOrNull(efforts.getValue(id)) else ex.opt("effort") as? String,
                notes[id] ?: (ex.optJSONObject("feedback")?.opt("note") as? String).orEmpty(),
                workout.optJSONObject("feedback")?.optBoolean("applied") == true, now)
        }
    }

    fun chosen(index: Int, note: String, now: Long): JSONObject? {
        val rating = efforts.getOrNull(index)
        if (rating == null && note.isBlank()) return null
        return JSONObject().put("note", note).put("updatedAt", now).also {
            if (rating != null) it.put("rating", rating).put("effort", rating)
        }
    }
}
