package com.example.ui.screens

import com.example.model.LoggedWorkoutEntry
import com.example.model.PrRecordEntry
import org.json.JSONArray

/**
 * 2779: PR records come from the saved workouts themselves (Room `prRecordsJson226`), newest first.
 * Before, the Progress page read a list that was never filled, so it always showed 0.
 */
internal fun loggedPrRecords(workouts: List<LoggedWorkoutEntry>): List<PrRecordEntry> =
    workouts.sortedByDescending { it.savedStartIso.ifBlank { it.dateLabelHu } }.flatMap { workout ->
        val array = runCatching { JSONArray(workout.prRecordsJson.ifBlank { "[]" }) }.getOrDefault(JSONArray())
        (0 until array.length()).mapNotNull { i ->
            val o = array.optJSONObject(i) ?: return@mapNotNull null
            val name = o.optString("exerciseName").ifBlank { return@mapNotNull null }
            PrRecordEntry(
                id = "${workout.id}:${o.optString("exerciseId")}:${o.optString("kind")}",
                exerciseNameHu = name,
                typeLabelHu = when (o.optString("kind")) {
                    "maxWeight" -> "Súlyrekord"
                    "e1rm" -> "Becsült 1RM rekord"
                    "reps" -> "Ismétlésrekord"
                    "time" -> "Idő / ismétlés rekord"
                    "distance" -> "Távolságrekord"
                    else -> "Rekord"
                },
                dateLabel = (workout.savedStartIso.ifBlank { workout.dateLabelHu }).take(10),
                previousLabel = o.optString("previous").ifBlank { "—" },
                newLabel = o.optString("value")
            )
        }
    }
