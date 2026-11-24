package com.repforge.app.wear

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import org.json.JSONObject

data class WearSummaryExercise(val name: String, val sets: Int, val volumeKg: Double?)
data class WearSummaryOverview(val name: String, val sets: Int, val volumeKg: Double?, val exercises: List<WearSummaryExercise>)

/** Follow the phone's completed-set volume convention. Timed sets are not repetitions. */
internal fun wearSummaryOverview(workout: WearWorkout): WearSummaryOverview {
    val exercises = workout.exercises.mapNotNull { exercise ->
        val sets = exercise.sets.filter { it.done }
        if (sets.isEmpty()) return@mapNotNull null
        val timed = exercise.id == "side-plank" || normalizeUnit(exercise.repUnit).startsWith("mp")
        val volumes = if (timed) emptyList() else sets.mapNotNull { set ->
            val weight = set.weight.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }
            val reps = set.reps.replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }?.roundToInt()
            if (weight != null && reps != null && reps > 0) (weight * reps).takeIf { it.isFinite() } else null
        }
        WearSummaryExercise(exercise.name, sets.size, volumes.takeIf { it.isNotEmpty() }?.sum())
    }
    val volumes = exercises.mapNotNull { it.volumeKg }
    return WearSummaryOverview(workout.dayName.ifBlank { workout.programName }, exercises.sumOf { it.sets },
        volumes.takeIf { it.isNotEmpty() }?.sum(), exercises)
}

internal fun wearSummaryText(key: String, language: String = java.util.Locale.getDefault().language): String {
    val labels = when (key) {
        "volume" -> listOf("Összvolumen", "Total volume", "Gesamtvolumen", "Volum total")
        "sets" -> listOf("sorozat", "sets", "Sätze", "serii")
        "preview" -> listOf("Edzésösszegzés", "Workout summary", "Trainingsübersicht", "Rezumat antrenament")
        "save" -> listOf("Mentés", "Save", "Speichern", "Salvează")
        "pending" -> listOf("Mentésre vár", "Not yet saved", "Noch nicht gespeichert", "Nesalvat încă")
        "pulse" -> listOf("Átlagpulzus", "Average heart rate", "Durchschnittspuls", "Puls mediu")
        else -> error("Unknown summary label")
    }
    return labels[when (language) { "hu" -> 0; "de" -> 2; "ro" -> 3; else -> 1 }]
}

internal fun wearSummaryVolume(value: Double?): String = value?.takeIf { it.isFinite() }
    ?.let { trimWearNumber(it) + " kg" } ?: "–"

@Composable
internal fun WearFinishPreview(workout: WearWorkout, health: JSONObject?, onBack: () -> Unit, onSave: () -> Unit) {
    val overview = remember(workout) { wearSummaryOverview(workout) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(workout.workoutId) { while (true) { now = System.currentTimeMillis(); delay(1000L) } }
    val measured = health?.takeIf { it.optString("workoutId") == workout.workoutId && it.optString("source") == "wear_health_services" }
    fun metric(key: String, min: Double, max: Double): String? =
        measured?.takeIf { it.has(key) && !it.isNull(key) }?.optDouble(key)
            ?.takeIf { it.isFinite() && it in min..max }?.let(::trimWearNumber)
    val canSave = workout.completedSets > 0 && !WearUndo188.blocked(workout) && !WearOwnership193.pending(workout)
    RoundList(wearSummaryText("preview")) {
        centerItem { Title(overview.name, 17) }
        centerItem { Label("${formatSeconds(workout.elapsedSeconds(now))} · ${overview.sets} ${wearSummaryText("sets")}", size = 12) }
        centerItem { Label(wearSummaryText("volume") + ": " + wearSummaryVolume(overview.volumeKg), size = 12) }
        centerItem { Label("♥ " + (metric("averageHeartRate", 20.0, 250.0) ?: "–") + " bpm · " + (metric("totalCalories", 0.0, 100000.0) ?: "–") + " kcal") }
        centerItem { Pill(wearSummaryText("save"), modifier = Modifier.testTag("wear-summary-save"), enabled = canSave, onClick = onSave) }
        centerItem { Label(wearSummaryText("pending"), size = 10) }
        overview.exercises.forEach { exercise ->
            centerItem {
                Label(exercise.name, color = White, lines = 2)
                Label("${exercise.sets} ${wearSummaryText("sets")} · ${wearSummaryVolume(exercise.volumeKg)}")
            }
        }
        centerItem { Pill(WearMenu216.text("back"), tone = Secondary, onClick = onBack) }
    }
}
