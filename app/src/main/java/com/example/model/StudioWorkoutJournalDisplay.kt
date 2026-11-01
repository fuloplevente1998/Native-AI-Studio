package com.example.model

import java.util.Locale

/** Journal labels must use the native set's measurement unit, not assume all sets use kilograms. */
internal object StudioWorkoutJournalDisplay {
    fun setLabel(row: ExerciseSetItem, measurementType: String, repUnit: String, outdoor: Boolean): String {
        val type = measurementType.lowercase(Locale.ROOT)
        val unit = repUnit.lowercase(Locale.ROOT)
        if (outdoor || type in setOf("distance", "gps", "cardio") || unit in setOf("km", "m", "meter", "meters")) {
            val meters = row.distanceMeters
            return if (meters != null && meters.isFinite() && meters > 0.0) {
                String.format(Locale.forLanguageTag("hu-HU"), "%.2f km", meters / 1000.0)
            } else "Nincs rögzített távolság"
        }
        if (type in setOf("duration", "time", "timed", "seconds") || unit in setOf("sec", "s", "mp", "seconds")) {
            val seconds = row.reps.coerceAtLeast(0)
            return if (seconds > 0) "$seconds mp" else "Nincs rögzített idő"
        }
        if (row.weightKg > 0f && row.reps > 0) {
            val weight = if (row.weightKg % 1f == 0f) row.weightKg.toInt().toString()
                else String.format(Locale.forLanguageTag("hu-HU"), "%.1f", row.weightKg)
            return "$weight kg × ${row.reps}"
        }
        if (row.reps > 0) return "${row.reps} ismétlés (saját testsúly)"
        return "Nincs rögzített sorozatadat"
    }
}
