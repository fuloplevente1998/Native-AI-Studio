package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.model.ExerciseItem
import com.example.model.ExerciseSetItem
import com.example.model.LoggedWorkoutEntry
import com.example.ui.theme.LocalTrainPilotPalette
import com.repforge.app.ui.theme.trainPilotClickable
import com.example.ui.viewmodel.TrainPilotUiState
import java.text.NumberFormat
import java.util.Locale

private fun topTr(lang: AppLanguage, hu: String, en: String, de: String, ro: String) = when (lang) {
    AppLanguage.HU -> hu; AppLanguage.EN -> en; AppLanguage.DE -> de; AppLanguage.RO -> ro
}

/**
 * 2775: one header for every active measurement screen (strength, timed, bilateral):
 * "Workout • n / N exercises", day badge + compact Coach chip, horizontal exercise strip.
 */
@Composable
internal fun StudioActiveWorkoutTop(
    uiState: TrainPilotUiState,
    exercises: List<ExerciseItem>,
    currentIndex: Int,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSwitchExercise: (ExerciseItem) -> Unit,
    coachOwner: androidx.lifecycle.ViewModelStoreOwner
) {
    val lang = uiState.language
    val title = LoggedWorkoutEntry(id = "", titleHu = uiState.activeWorkoutTitleHu)
        .localizedTitle(lang, uiState.programs, uiState.exercises)
    val total = exercises.size.coerceAtLeast(1)
    val position = currentIndex.coerceIn(0, total - 1) + 1
    StudioActiveWorkoutHeaderSequence(
        state = uiState,
        header = {
            SubScreenTopBar(
                title = topTr(lang, "$title • $position / $total gyakorlat", "$title • $position / $total exercises",
                    "$title • $position / $total Übungen", "$title • $position / $total exerciții"),
                onBack = onBack,
                onOpenSettings = onOpenSettings,
                rightExtraContent = { StudioWorkoutHealthAction() }
            )
        },
        exerciseSequence = {
            StudioExerciseSequenceStrip(state = uiState, exercises = exercises,
                currentIndex = currentIndex.coerceIn(0, total - 1), onSelect = onSwitchExercise)
        },
        sessionTrailing = { StudioWorkoutCoachChip(uiState.selectedExerciseId, coachOwner) }
    )
}

/**
 * 2775: shared exercise title row: name ⌄ (opens the library in swap mode), one subtitle line,
 * video button and the exercise-kind icon with an "i" badge (opens the details).
 */
@Composable
internal fun StudioActiveExerciseTitleRow(
    language: AppLanguage,
    name: String,
    subtitle: String,
    exercise: ExerciseItem?,
    exerciseId: String?,
    onOpenLibrary: () -> Unit,
    onOpenDetail: (String) -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    Row(Modifier.fillMaxWidth().testTag("active_exercise_title_row"),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).trainPilotClickable(RoundedCornerShape(12.dp)) { onOpenLibrary() }
            .padding(vertical = 2.dp).testTag("active_exercise_title")) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(name, modifier = Modifier.weight(1f, fill = false), style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, color = palette.textPrimary)
                Icon(Icons.Default.KeyboardArrowDown,
                    contentDescription = topTr(language, "Gyakorlat váltása", "Switch exercise", "Übung wechseln", "Schimbă exercițiul"),
                    tint = palette.primary, modifier = Modifier.size(22.dp))
            }
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(subtitle, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall, color = palette.textSecondary,
                    modifier = Modifier.testTag("active_exercise_subtitle"))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SmallExerciseVideoButton(exerciseName = name, exerciseId = exerciseId)
            Surface(shape = RoundedCornerShape(10.dp), color = palette.surfaceElevated,
                modifier = Modifier.trainPilotClickable(RoundedCornerShape(10.dp)) { exerciseId?.let(onOpenDetail) }) {
                Box(Modifier.size(48.dp).testTag("active_exercise_kind_icon"), contentAlignment = Alignment.Center) {
                    Icon(exerciseKindIcon(exercise),
                        contentDescription = topTr(language, "Gyakorlat részletei", "Exercise details", "Übungsdetails", "Detalii exercițiu"),
                        tint = palette.textPrimary, modifier = Modifier.size(22.dp))
                    Box(Modifier.align(Alignment.BottomEnd).padding(3.dp).size(15.dp).background(palette.primary, CircleShape),
                        contentAlignment = Alignment.Center) {
                        Text("i", color = palette.onPrimary, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                    }
                }
            }
        }
    }
}

/**
 * 2775: sets of the most recent *saved* workout that contains this exercise (never the active one).
 * Empty when there is no saved history: the UI then shows nothing (no fabricated values).
 */
internal fun lastLoggedSets(logged: List<LoggedWorkoutEntry>, exercise: ExerciseItem?, activeWorkoutId: String): List<ExerciseSetItem> {
    if (exercise == null) return emptyList()
    val names = listOf(exercise.nameHu, exercise.nameEn).map { it.trim() }.filter { it.isNotEmpty() }
    if (names.isEmpty()) return emptyList()
    return logged.asSequence()
        .filter { it.id != activeWorkoutId && !it.isOutdoorRun }
        .sortedByDescending { it.savedStartIso.ifBlank { it.dateLabelHu } }
        .mapNotNull { entry -> entry.exerciseDetails.firstOrNull { d -> names.any { it.equals(d.nameHu.trim(), ignoreCase = true) } } }
        .map { detail -> detail.sets.filter { it.completed } }
        .firstOrNull { it.isNotEmpty() }
        .orEmpty()
}

/** "legutóbb: 5,5 × 12" (weight omitted when none was logged). */
internal fun lastSetLabel(set: ExerciseSetItem, language: AppLanguage): String {
    val number = NumberFormat.getNumberInstance(Locale.forLanguageTag(language.name.lowercase())).apply {
        maximumFractionDigits = 2; isGroupingUsed = false
    }
    val prefix = topTr(language, "legutóbb", "last time", "letztes Mal", "ultima dată")
    val value = if (set.weightKg > 0f) "${number.format(set.weightKg.toDouble())} × ${set.reps}" else "× ${set.reps}"
    return "$prefix: $value"
}
