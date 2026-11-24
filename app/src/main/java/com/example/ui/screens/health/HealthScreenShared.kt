package com.example.ui.screens

import androidx.compose.foundation.layout.size
import com.example.model.AppLanguage

/**
 * 5. EGÉSZSÉG (Mockup 3/4 #5 + teljes régi main Egészség elrendezés)
 */
data class WeightMeasurementEntry(
    val id: String,
    val weightLabel: String,
    val weightValue: Float,
    val dateLabel: String,
    val parsedDate: java.time.LocalDate = java.time.LocalDate.now(),
    val photoCount: Int = 0
)

internal fun localizeTrainingStyle(rawHu: String, lang: AppLanguage): String = when (rawHu.trim()) {
    "Normál hipertrófia" -> tr(lang, "Normál hipertrófia", "Standard Hypertrophy", "Normale Hypertrophie", "Hipertrofie standard")
    "HIT-inspirált" -> tr(lang, "HIT-inspirált", "HIT-inspired", "HIT-inspiriert", "Inspirat de HIT")
    else -> rawHu
}

internal fun localizeProgressionRule(rawHu: String, lang: AppLanguage): String = when (rawHu.trim()) {
    "Dupla progresszió" -> tr(lang, "Dupla progresszió", "Double Progression", "Doppelte Progression", "Progresie dublă")
    "Lineáris progresszió" -> tr(lang, "Lineáris progresszió", "Linear Progression", "Lineare Progression", "Progresie liniară")
    "RIR/Coach" -> "RIR / Coach"
    else -> rawHu
}

internal fun parseWeightEntryLocalDate(rawDate: String, fallback: java.time.LocalDate = java.time.LocalDate.now()): java.time.LocalDate {
    val cleaned = rawDate.trim()
    if (cleaned.isBlank()) return fallback
    val nums = Regex("\\d+").findAll(cleaned).mapNotNull { it.value.toIntOrNull() }.toList()
    if (nums.size >= 3) {
        val y = if (nums[0] in 1970..2100) nums[0] else fallback.year
        val m = nums[1].coerceIn(1, 12)
        val d = nums[2].coerceIn(1, 28)
        return runCatching { java.time.LocalDate.of(y, m, nums[2].coerceIn(1, 31)) }
            .getOrElse { java.time.LocalDate.of(y, m, d) }
    }
    return fallback
}
