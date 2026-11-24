package com.repforge.app.wear

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal fun recommendedMeta(rec: WatchHomeRecommendation): String {
    val count = "${rec.day.exercises.size} gyakorlat"
    return try {
        if (rec.plannedStart.isBlank()) count
        else
            "$count · ${Instant.parse(rec.plannedStart).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))}"
    } catch (_: Exception) {
        count
    }
}

internal fun normalizeUnit(unit: String): String = unit.replace("\\s+".toRegex(), "").lowercase()

internal fun formatSeconds(seconds: Int): String =
    "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"

internal fun numberText(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString().replace('.', ',')

internal fun fieldValue(set: WearSet, field: String): Double =
    when (field) {
        "weight" -> set.weight.toDoubleOrNull() ?: 0.0
        "leftSeconds" -> set.leftSeconds.toDouble()
        "rightSeconds" -> set.rightSeconds.toDouble()
        else -> set.reps.toDoubleOrNull() ?: 0.0
    }


/** 2777: "A nap" -> "A", "Felsőtest – Erő" -> "Fe"; a short token wins over the word "nap". */
internal fun wearDayInitial(name: String): String {
    val tokens = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return (tokens.firstOrNull { it.length <= 2 && it.all(Char::isLetterOrDigit) } ?: tokens.firstOrNull() ?: "?").take(2).replaceFirstChar { it.uppercase() }
}
