package com.repforge.app

import com.repforge.app.data.db.WorkoutHistoryDao
import com.repforge.app.data.db.WorkoutHistoryEntity
import org.json.JSONArray
import org.json.JSONObject

/** User-confirmed identity association; never adds source metrics to existing workout totals. */
internal object ExternalWorkoutAssociation {
    private const val IDS = "externalLinkedIds359"
    private const val SOURCES = "externalLinkedSources359"
    private val idPattern = Regex("tp359-[a-f0-9]{64}")
    fun linkedIds(raw: JSONObject): List<String> {
        val ids = raw.optJSONArray(IDS) ?: return emptyList()
        return (0 until ids.length()).map { ids.optString(it) }.filter { idPattern.matches(it) }.distinct()
    }

    fun snapshots(raw: JSONObject): List<JSONObject> {
        val sources = raw.optJSONObject(SOURCES) ?: return emptyList()
        return linkedIds(raw).mapNotNull { sources.optJSONObject(it) }
    }

    suspend fun attach(dao: WorkoutHistoryDao, row: WorkoutHistoryEntity, id: String, workout: ExternalWorkout,
                       metrics: JSONObject?, guard: () -> Unit): ExternalWorkoutImport.Outcome {
        val raw = JSONObject(row.rawJson)
        val ids = linkedIds(raw)
        if (ids.size >= 64 && id !in ids) return ExternalWorkoutImport.Outcome.REVIEW_OVERLAP
        val sources = raw.optJSONObject(SOURCES) ?: JSONObject()
        val previous = sources.optJSONObject(id)
        if (previous != null && previous.optLong("lastModifiedMs") > workout.modifiedMs)
            return ExternalWorkoutImport.Outcome.ALREADY_IMPORTED
        val next = workout.summary()
        // A linked session whose source window moved must not silently appear as the
        // same measurement of the host workout. Keep the explicit link but flag review.
        val changedWindow = previous != null &&
            (previous.optString("start") != next.optString("start") || previous.optString("end") != next.optString("end"))
        val outsideHost = row.startedEpochMs >= workout.endMs || row.finishedEpochMs <= workout.startMs
        if (changedWindow || outsideHost || previous?.optBoolean("windowChanged") == true)
            next.put("windowChanged", true)
        else if (metrics != null) next.put("metrics", metrics)
        if (id in ids && previous?.toString() == next.toString()) return ExternalWorkoutImport.Outcome.ALREADY_IMPORTED
        sources.put(id, next)
        raw.put(IDS, JSONArray((ids + id).distinct())).put(SOURCES, sources)
        check(dao.updateJournalPhotoJson(row.id, row.rawJson, raw.toString()) == 1)
        guard()
        return ExternalWorkoutImport.Outcome.UPDATED
    }
}
