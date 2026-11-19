package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material.icons.filled.Timer
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.model.ExerciseItem

/** Icon family of an exercise (2774): one rule covers the whole library, custom exercises included. */
enum class ExerciseKind { TIMED, CARDIO, MACHINE, BODYWEIGHT, FREE_WEIGHT }

fun exerciseKind(exercise: ExerciseItem?): ExerciseKind {
    if (exercise == null) return ExerciseKind.FREE_WEIGHT
    val type = exercise.measurementType.lowercase()
    val unit = exercise.repUnit.trim().lowercase()
    val equipment = exercise.equipmentHu.lowercase()
    return when {
        type in setOf("distance", "gps") || "gps" in equipment -> ExerciseKind.CARDIO
        exercise.isTimed || type == "time" || unit in setOf("mp", "sec", "s", "second") -> ExerciseKind.TIMED
        exercise.loadType.lowercase() in setOf("machine", "cable") -> ExerciseKind.MACHINE
        exercise.loadType.lowercase() == "bodyweight" -> ExerciseKind.BODYWEIGHT
        exercise.loadType.lowercase() in setOf("dumbbell", "barbell", "weight") -> ExerciseKind.FREE_WEIGHT
        listOf("csiga", "gép", "kábel", "cable", "machine").any { it in equipment } -> ExerciseKind.MACHINE
        listOf("súlyzó", "rúd", "kettlebell", "tárcsa", "dumbbell", "barbell").any { it in equipment } -> ExerciseKind.FREE_WEIGHT
        exercise.loadType.lowercase() == "bodyweight" || "testsúly" in equipment || "gumiszalag" in equipment -> ExerciseKind.BODYWEIGHT
        else -> ExerciseKind.FREE_WEIGHT
    }
}

fun exerciseKindIcon(exercise: ExerciseItem?): ImageVector = when (exerciseKind(exercise)) {
    ExerciseKind.TIMED -> Icons.Default.Timer
    ExerciseKind.CARDIO -> Icons.AutoMirrored.Filled.DirectionsRun
    ExerciseKind.MACHINE -> Icons.Default.PrecisionManufacturing
    ExerciseKind.BODYWEIGHT -> Icons.Default.SportsGymnastics
    ExerciseKind.FREE_WEIGHT -> Icons.Default.FitnessCenter
}
