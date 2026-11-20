package com.example.ui.screens

import com.example.model.LoggedWorkoutEntry
import com.example.model.PrRecordEntry
import org.json.JSONArray

/**
 * 2779: PR records come from the saved workouts themselves (Room `prRecordsJson226`), newest first.
 * Before, the Progress page read a list that was never filled, so it always showed 0.
 */
internal fun loggedPrRecords(workouts: List<LoggedWorkoutEntry>): List<PrRecordEntry> {
    // 2782: older / watch-finalized workouts can have an empty stored PR list. For those the same
    // rule as the save-time calculator is replayed over the saved sets (beat the best of all earlier
    // workouts; the first session of an exercise is not a record). Saved data is never changed.
    val replayed = replayedPrRecords(workouts)
    return workouts.sortedByDescending { it.savedStartIso.ifBlank { it.dateLabelHu } }.flatMap { workout ->
        val array = runCatching { JSONArray(workout.prRecordsJson.ifBlank { "[]" }) }.getOrDefault(JSONArray())
        if (array.length() == 0) return@flatMap replayed[workout.id].orEmpty()
        (0 until array.length()).mapNotNull { i ->
            val o = array.optJSONObject(i) ?: return@mapNotNull null
            val name = o.optString("exerciseName").ifBlank { return@mapNotNull null }
            PrRecordEntry(
                id = "${workout.id}:${o.optString("exerciseId")}:${o.optString("kind")}",
                exerciseNameHu = name,
                typeLabelHu = prKindLabel(o.optString("kind")),
                dateLabel = (workout.savedStartIso.ifBlank { workout.dateLabelHu }).take(10),
                previousLabel = o.optString("previous").ifBlank { "—" },
                newLabel = o.optString("value")
            )
        }
    }
}

private fun prKindLabel(kind: String) = when (kind) {
    "maxWeight" -> "Súlyrekord"
    "e1rm" -> "Becsült 1RM rekord"
    "reps" -> "Ismétlésrekord"
    "time" -> "Idő / ismétlés rekord"
    "distance" -> "Távolságrekord"
    else -> "Rekord"
}

private fun kg(v: Float) = v.toString().removeSuffix(".0").replace('.', ',')

/** workout id → records replayed from completed sets, oldest workout first. */
internal fun replayedPrRecords(workouts: List<LoggedWorkoutEntry>): Map<String, List<PrRecordEntry>> {
    val bestWeight = HashMap<String, Float>()
    val bestReps = HashMap<String, Int>()
    val out = HashMap<String, List<PrRecordEntry>>()
    for (w in workouts.sortedBy { it.savedStartIso.ifBlank { it.dateLabelHu } }) {
        val found = mutableListOf<PrRecordEntry>()
        val sessionWeight = HashMap<String, Pair<Float, Int>>()
        val sessionReps = HashMap<String, Int>()
        for (ex in w.exerciseDetails) {
            for (set in ex.sets) {
                if (!set.completed) continue
                if (set.weightKg > 0f && set.reps > 0) {
                    val cur = sessionWeight[ex.nameHu]
                    if (cur == null || set.weightKg > cur.first || set.weightKg == cur.first && set.reps > cur.second)
                        sessionWeight[ex.nameHu] = set.weightKg to set.reps
                } else if (set.weightKg <= 0f && set.reps > 0) {
                    sessionReps[ex.nameHu] = maxOf(sessionReps[ex.nameHu] ?: 0, set.reps)
                }
            }
        }
        val date = w.savedStartIso.ifBlank { w.dateLabelHu }.take(10)
        sessionWeight.forEach { (name, best) ->
            val prev = bestWeight[name]
            if (prev != null && best.first > prev) found += PrRecordEntry("${w.id}:$name:maxWeight", name,
                prKindLabel("maxWeight"), date, "${kg(prev)} kg", "${kg(best.first)} kg (${kg(best.first)} kg × ${best.second})")
            bestWeight[name] = maxOf(prev ?: 0f, best.first)
        }
        sessionReps.forEach { (name, best) ->
            val prev = bestReps[name]
            if (prev != null && best > prev) found += PrRecordEntry("${w.id}:$name:reps", name,
                prKindLabel("reps"), date, "$prev", "$best")
            bestReps[name] = maxOf(prev ?: 0, best)
        }
        if (found.isNotEmpty()) out[w.id] = found
    }
    return out
}
