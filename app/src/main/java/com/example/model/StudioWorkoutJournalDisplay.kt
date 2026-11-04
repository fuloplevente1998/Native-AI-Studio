package com.example.model

import java.util.Locale

/** Journal labels must use the native set's measurement unit, not assume all sets use kilograms. */
internal object StudioWorkoutJournalDisplay {
    fun setLabel(
        row: ExerciseSetItem,
        measurementType: String,
        repUnit: String,
        outdoor: Boolean,
        lang: AppLanguage = AppLanguage.HU
    ): String {
        val type = measurementType.lowercase(Locale.ROOT)
        val unit = repUnit.lowercase(Locale.ROOT)
        val localeTag = when (lang) {
            AppLanguage.HU -> "hu-HU"
            AppLanguage.EN -> "en-US"
            AppLanguage.DE -> "de-DE"
            AppLanguage.RO -> "ro-RO"
        }
        if (outdoor || type in setOf("distance", "gps", "cardio") || unit in setOf("km", "m", "meter", "meters")) {
            val meters = row.distanceMeters
            return if (meters != null && meters.isFinite() && meters > 0.0) {
                String.format(Locale.forLanguageTag(localeTag), "%.2f km", meters / 1000.0)
            } else when (lang) {
                AppLanguage.HU -> "Nincs rögzített távolság"
                AppLanguage.EN -> "No distance recorded"
                AppLanguage.DE -> "Keine Distanz aufgezeichnet"
                AppLanguage.RO -> "Nicio distanță înregistrată"
            }
        }
        if (type in setOf("duration", "time", "timed", "seconds") || unit in setOf("sec", "s", "mp", "seconds")) {
            val seconds = row.reps.coerceAtLeast(0)
            val secUnit = when (lang) {
                AppLanguage.HU -> "mp"
                AppLanguage.EN -> "sec"
                AppLanguage.DE -> "Sek."
                AppLanguage.RO -> "sec"
            }
            return if (seconds > 0) "$seconds $secUnit" else when (lang) {
                AppLanguage.HU -> "Nincs rögzített idő"
                AppLanguage.EN -> "No time recorded"
                AppLanguage.DE -> "Keine Zeit aufgezeichnet"
                AppLanguage.RO -> "Niciun timp înregistrat"
            }
        }
        if (row.weightKg > 0f && row.reps > 0) {
            val weight = if (row.weightKg % 1f == 0f) row.weightKg.toInt().toString()
                else String.format(Locale.forLanguageTag(localeTag), "%.1f", row.weightKg)
            return "$weight kg × ${row.reps}"
        }
        if (row.reps > 0) return when (lang) {
            AppLanguage.HU -> "${row.reps} ismétlés (saját testsúly)"
            AppLanguage.EN -> "${row.reps} reps (bodyweight)"
            AppLanguage.DE -> "${row.reps} Wdh. (Eigengewicht)"
            AppLanguage.RO -> "${row.reps} repetări (greutatea corpului)"
        }
        return when (lang) {
            AppLanguage.HU -> "Nincs rögzített sorozatadat"
            AppLanguage.EN -> "No set data recorded"
            AppLanguage.DE -> "Keine Satzdaten aufgezeichnet"
            AppLanguage.RO -> "Nicio dată de serie înregistrată"
        }
    }
}
