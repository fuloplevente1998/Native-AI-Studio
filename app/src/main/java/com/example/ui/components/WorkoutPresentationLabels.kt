package com.example.ui.components

import com.example.model.AppLanguage
import com.example.model.ExerciseItem
import com.example.model.ExerciseSetItem

/** Display-only labels; no load, repetitions, side count or volume conversion. */
internal data class StrengthColumnLabels(val weight: String, val repetitions: String)

/** Visiting an exercise, or completing just one set, is not completion. */
internal fun exerciseFullyCompleted(sets: List<ExerciseSetItem>?): Boolean =
    !sets.isNullOrEmpty() && sets.all { it.completed }

internal fun strengthColumnLabels(exercise: ExerciseItem?, language: AppLanguage): StrengthColumnLabels {
    fun word(hu: String, en: String, de: String, ro: String) = when (language) {
        AppLanguage.HU -> hu; AppLanguage.EN -> en; AppLanguage.DE -> de; AppLanguage.RO -> ro
    }
    val weight = when (exercise?.loadType) {
        "per_hand" -> word("kg/kar", "kg/hand", "kg/Hand", "kg/braț")
        "bodyweight" -> word("Saját súly", "Bodyweight", "Körpergewicht", "Greutate proprie")
        "assisted" -> word("kg segítség", "kg assist", "kg Hilfe", "kg asist.")
        else -> "kg" // total and single_dumbbell are the actual entered load, not a derived sum.
    }
    val timed = exercise?.measurementType == "time" || exercise?.repUnit?.trim()?.startsWith("mp") == true
    return StrengthColumnLabels(weight, if (timed) word("mp", "sec", "Sek.", "sec")
        else word("ism.", "reps", "Wdh.", "rep."))
}

/** Keep the canonical active snapshot's units through restore, even if the catalog differs. */
internal fun withNativeStrengthUnits(catalog: ExerciseItem, loadType: String?, repUnit: String?): ExerciseItem =
    catalog.copy(loadType = loadType?.takeIf { it.isNotBlank() } ?: catalog.loadType,
        repUnit = repUnit ?: catalog.repUnit)

/** Suppress only a repeated day code; a user's distinct title remains visible. */
internal fun secondaryWorkoutDayTitle(code: String, title: String): String? {
    fun normalized(value: String) = value.trim().lowercase(java.util.Locale.ROOT)
        .replace(Regex("\\b(nap|day|tag|ziua|zi)\\b"), "")
        .replace(Regex("[\\s.·:–—-]+"), "")
    return title.takeIf { it.isNotBlank() && normalized(it) != normalized(code) }
}
