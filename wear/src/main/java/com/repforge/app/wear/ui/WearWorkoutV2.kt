package com.repforge.app.wear

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text

internal fun wearV2Text(key: String, language: String = java.util.Locale.getDefault().language): String {
    val words = when (key) {
        "last" -> listOf("Legutóbb", "Last time", "Zuletzt", "Ultima dată")
        "next" -> listOf("Következő", "Next", "Nächster Satz", "Următorul")
        "restAdjust" -> listOf("Időállítás még nem elérhető", "Rest adjustment unavailable", "Pausenanpassung nicht verfügbar", "Ajustarea pauzei indisponibilă")
        "phoneControl" -> listOf("Vezérlés telefonon", "Control on phone", "Am Telefon steuern", "Controlează pe telefon")
        "pauseWorkout" -> listOf("Edzés szüneteltetése", "Pause workout", "Training pausieren", "Pauză antrenament")
        "undo" -> listOf("Visszavonás", "Undo", "Rückgängig", "Anulează")
        else -> error("Unknown V2 label")
    }
    return words[when (language) { "hu" -> 0; "de" -> 2; "ro" -> 3; else -> 1 }]
}

/** The journal projection has names, not exercise IDs. Ambiguous names never supply a previous value. */
internal fun wearPreviousSetV2(items: List<MiniJournalWorkout>, currentId: String, exerciseName: String, setNumber: Int): String? {
    if (exerciseName.isBlank() || setNumber <= 0) return null
    for (item in items.filter { it.workoutId != currentId }.sortedByDescending {
        runCatching { java.time.Instant.parse(it.started).toEpochMilli() }.getOrDefault(Long.MIN_VALUE)
    }) {
        val matches = item.exercises.filter { it.name == exerciseName }
        if (matches.size != 1) continue
        val set = matches.single().sets.singleOrNull { it.number == setNumber } ?: continue
        val reps = set.reps.takeIf { it.isNotBlank() } ?: continue
        val weight = set.weight?.takeIf { it.isFinite() && it >= 0.0 }
        return if (weight == null) reps else "${weight.toString().removeSuffix(".0")} kg × $reps"
    }
    return null
}

@Composable
internal fun WearV2Value(value: String, unit: String, modifier: Modifier, enabled: Boolean, onEdit: () -> Unit) {
    Column(modifier.heightIn(min = 48.dp).clickable(enabled = enabled, role = Role.Button, onClick = onEdit),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(value, color = White, fontSize = 25.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Label(unit, size = 10)
    }
}

@Composable
internal fun WearRestV2(workout: WearWorkout, remaining: Int, onSkip: () -> Unit, onMenu: () -> Unit,
    onAdjust: ((Int) -> Unit)? = null, onPhoneControl: (() -> Unit)? = null) {
    val next = if (!workout.exerciseComplete) workout.currentSet?.takeUnless { it.done }
        else workout.exercises.drop(workout.currentExercise + 1).firstOrNull()?.sets?.firstOrNull { !it.done }
    val nextName = if (!workout.exerciseComplete) workout.exercise?.name
        else workout.exercises.getOrNull(workout.currentExercise + 1)?.name
    val undo = LocalUndo.current
    val blocked = WearUndo188.blocked(workout) || WearOwnership193.pending(workout)
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp).testTag("wear-rest-v2"),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Label(wearText("Pihenő"), size = 12)
        Text(formatSeconds(remaining), color = White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Label(wearV2Text("next") + ": " + (next?.let {
            listOfNotNull(nextName, it.weight.takeIf { w -> w.isNotBlank() }?.let { w -> "$w kg" },
                it.reps.takeIf { reps -> reps.isNotBlank() }?.let { reps -> "× $reps" }).joinToString(" ")
        } ?: "–"), size = 10, lines = 2)
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            RoundControl("−15", enabled = onAdjust != null && !blocked && remaining > 0, description = "−15 s", onClick = { onAdjust?.invoke(-WEAR_REST_STEP367) })
            RoundControl("»", Gold, enabled = !blocked, caption = wearText("Kihagyás"),
                description = wearText("Pihenő kihagyása"), onClick = onSkip)
            RoundControl("+15", enabled = onAdjust != null && !blocked && remaining in 1 until WEAR_REST_MAX367, description = "+15 s", onClick = { onAdjust?.invoke(WEAR_REST_STEP367) })
        }
        if (onAdjust == null) Label(wearV2Text("restAdjust"), size = 9)
        if (undo != null && WearUndo188.available(workout))
            Pill(wearV2Text("undo"), tone = Secondary, enabled = !blocked, size = 10) {
                WearUndo188.record(workout)?.optString("recordId")?.let(undo)
            }
        else if (onPhoneControl != null && onAdjust == null)
            Pill(wearV2Text("phoneControl"), tone = Secondary, size = 10, onClick = onPhoneControl)
    }
}
