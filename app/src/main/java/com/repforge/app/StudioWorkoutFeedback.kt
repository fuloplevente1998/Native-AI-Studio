package com.repforge.app

import com.repforge.shared.SetEditorRules212
import org.json.JSONObject
import java.util.Locale

/** The six Studio choices use the same machine values as the canonical Coach. */
internal object StudioWorkoutFeedback {
    val efforts = listOf("easy", "light", "right", "challenging", "hard", "pain")
    private val scoreToEffort = mapOf(
        1 to "easy",
        2 to "light",
        3 to "good",
        4 to "challenging",
        5 to "hard"
    )
    private val effortToScore = mapOf(
        "easy" to 1,
        "light" to 2,
        "good" to 3,
        "right" to 3,
        "challenging" to 4,
        "hard" to 5
    )
    private val knownAliases = mapOf(
        "easy" to "easy",
        "könnyű" to "easy",
        "konnyu" to "easy",
        "leicht" to "easy",
        "ușor" to "easy",
        "usor" to "easy",
        "light" to "light",
        "enyhe" to "light",
        "locker" to "light",
        "lejer" to "light",
        "good" to "good",
        "right" to "right",
        "just right" to "good",
        "pont jó" to "good",
        "pont jo" to "good",
        "gut" to "good",
        "genau richtig" to "good",
        "bun" to "good",
        "potrivit" to "good",
        "challenging" to "challenging",
        "kihívás" to "challenging",
        "kihivas" to "challenging",
        "fordernd" to "challenging",
        "solicitant" to "challenging",
        "hard" to "hard",
        "nehéz" to "hard",
        "nehez" to "hard",
        "schwer" to "hard",
        "greu" to "hard",
        "pain" to "pain",
        "fájdalom" to "pain",
        "fajdalom" to "pain",
        "schmerz" to "pain",
        "durere" to "pain"
    )

    /**
     * Maps known legacy effort strings, translated labels, or 1..5 effortScore values
     * to canonical effort tokens ("easy", "light", "good", "right", "challenging", "hard", "pain").
     * Returns null for unknown/missing values so callers never corrupt unrecognized legacy fields.
     */
    fun normalizeKnownEffort(raw: Any?): String? = when (raw) {
        null, JSONObject.NULL -> null
        is Number -> {
            val d = raw.toDouble()
            val i = raw.toInt()
            if (d == i.toDouble()) scoreToEffort[i] else null
        }
        is String -> {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) null
            else knownAliases[trimmed.lowercase(Locale.ROOT)]
                ?: trimmed.toIntOrNull()?.let { scoreToEffort[it] }
        }
        else -> null
    }

    /** Reads the exercise's own saved effort (preserving unknown non-blank strings if present). */
    fun readOwnExerciseEffort(exercise: JSONObject): String? {
        val direct = (exercise.opt("effort") as? String)?.trim()?.takeIf { it.isNotEmpty() }
        if (direct != null) return normalizeKnownEffort(direct) ?: direct
        normalizeKnownEffort(exercise.opt("effort"))?.let { return it }
        val fb = exercise.optJSONObject("feedback")
        if (fb != null) {
            val fbEffort = (fb.opt("effort") as? String)?.trim()?.takeIf { it.isNotEmpty() }
            if (fbEffort != null) return normalizeKnownEffort(fbEffort) ?: fbEffort
            normalizeKnownEffort(fb.opt("effort"))?.let { return it }
            val fbRating = (fb.opt("rating") as? String)?.trim()?.takeIf { it.isNotEmpty() }
            if (fbRating != null) return normalizeKnownEffort(fbRating) ?: fbRating
            normalizeKnownEffort(fb.opt("effortScore"))?.let { return it }
        }
        normalizeKnownEffort(exercise.opt("effortScore"))?.let { return it }
        return null
    }

    /** Reads the workout's saved effort across v1.3 rating, v2.3.2 feedback.effort/effortScore, and effortFeedback. */
    fun readWorkoutEffort(workout: JSONObject): String? {
        val fb = workout.optJSONObject("feedback")
        if (fb != null) {
            val fbEffort = (fb.opt("effort") as? String)?.trim()?.takeIf { it.isNotEmpty() }
            if (fbEffort != null) return normalizeKnownEffort(fbEffort) ?: fbEffort
            normalizeKnownEffort(fb.opt("effort"))?.let { return it }
            val fbRating = (fb.opt("rating") as? String)?.trim()?.takeIf { it.isNotEmpty() }
            if (fbRating != null) return normalizeKnownEffort(fbRating) ?: fbRating
            normalizeKnownEffort(fb.opt("effortScore"))?.let { return it }
        }
        val rootEffort = (workout.opt("effortFeedback") as? String)?.trim()?.takeIf { it.isNotEmpty() }
        if (rootEffort != null) return normalizeKnownEffort(rootEffort) ?: rootEffort
        normalizeKnownEffort(workout.opt("effortFeedback"))?.let { return it }
        return null
    }

    /** Reads canonical exercise effort for Coach, falling back to workout-level feedback. */
    fun readExerciseEffort(exercise: JSONObject, workout: JSONObject? = null): String {
        val own = normalizeKnownEffort(readOwnExerciseEffort(exercise))
        if (own != null) return if (own == "right") "good" else own
        val inherited = workout?.let { normalizeKnownEffort(readWorkoutEffort(it)) }
        if (inherited != null) return if (inherited == "right") "good" else inherited
        return ""
    }

    /** Reads exercise note across feedback.note, note, and userNote. */
    fun readExerciseNote(exercise: JSONObject): String =
        (exercise.optJSONObject("feedback")?.opt("note") as? String)
            ?: (exercise.opt("note") as? String)
            ?: (exercise.opt("userNote") as? String)
            ?: ""

    /** Reads workout note across feedback.note and root note. */
    fun readWorkoutNote(workout: JSONObject): String =
        (workout.optJSONObject("feedback")?.opt("note") as? String)
            ?: (workout.opt("note") as? String)
            ?: ""

    /** Reads legacy exercise-level RIR (0..10) from exercise.actualRir or exercise.feedback.rir without coercing missing/invalid values to 0. */
    fun readLegacyExerciseRir(exercise: JSONObject): Int? {
        SetEditorRules212.parseActualRir229(exercise)?.let { return it }
        fun parseStrictRir(raw: Any?): Int? = when (raw) {
            null, JSONObject.NULL -> null
            is Number -> {
                val d = raw.toDouble()
                val i = raw.toInt()
                if (d.isFinite() && d == i.toDouble() && i in 0..10) i else null
            }
            is String -> {
                val s = raw.trim()
                if (Regex("^(?:10|[0-9])$").matches(s)) s.toIntOrNull() else null
            }
            else -> null
        }
        return parseStrictRir(exercise.opt("actualRir"))
            ?: parseStrictRir(exercise.optJSONObject("feedback")?.opt("rir"))
    }

    /** Only explicit edits touch metadata; absence never implies a rating or RIR. */
    fun editExercise(exercise: JSONObject, effort: String?, note: String, locked: Boolean, now: Long) {
        val previous = readOwnExerciseEffort(exercise)
        val normalizedNew = normalizeKnownEffort(effort)
        val effectiveNew = if (effort == null) null else (normalizedNew ?: effort)
        val sameKnownThird = (previous == "good" && effectiveNew == "right") || (previous == "right" && effectiveNew == "good")
        if (effectiveNew != previous && !sameKnownThird) {
            require(effectiveNew == null || normalizedNew != null)
            check(!locked && exercise.optJSONObject("feedback")?.optBoolean("applied") != true) {
                "Feedback already applied to a program"
            }
            if (effectiveNew == null) {
                exercise.remove("effort")
                exercise.remove("effortScore")
            } else {
                exercise.put("effort", effectiveNew)
                if (exercise.has("effortScore")) {
                    val score = effortToScore[effectiveNew]
                    if (score != null) exercise.put("effortScore", score) else exercise.remove("effortScore")
                }
            }
            // Keep existing applied flags and unrelated metadata; remove stale duplicated answers.
            exercise.optJSONObject("feedback")?.let { feedback ->
                listOf("rating", "effort", "effortScore", "suggestion").forEach { feedback.remove(it) }
                feedback.put("updatedAt", now)
            }
        }
        val oldNote = readExerciseNote(exercise)
        if (note != oldNote) {
            require(note.length <= 3000 && note.none { it.isISOControl() && it != '\n' })
            val feedback = exercise.optJSONObject("feedback") ?: JSONObject()
            exercise.put("feedback", feedback.put("note", note).put("updatedAt", now))
            if (exercise.has("note")) exercise.put("note", note)
            if (exercise.has("userNote")) exercise.put("userNote", note)
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
            editExercise(ex, if (id in efforts) this.efforts.getOrNull(efforts.getValue(id)) else readOwnExerciseEffort(ex),
                notes[id] ?: readExerciseNote(ex),
                workout.optJSONObject("feedback")?.optBoolean("applied") == true, now)
        }
    }

    fun chosen(index: Int, note: String, now: Long): JSONObject? {
        val rating = efforts.getOrNull(index)
        if (rating == null && note.isBlank()) return null
        return JSONObject().put("note", note).put("updatedAt", now).also {
            if (rating != null) {
                val canonicalEffort = if (rating == "right") "good" else rating
                val legacyRating = when (canonicalEffort) {
                    "pain" -> "pain"
                    "challenging", "hard" -> "hard"
                    "good" -> "right"
                    else -> "easy"
                }
                it.put("rating", legacyRating)
                    .put("effort", canonicalEffort)
                    .put("effortScore", effortToScore[canonicalEffort] ?: JSONObject.NULL)
                    .put("source", "phone")
            }
        }
    }
}
