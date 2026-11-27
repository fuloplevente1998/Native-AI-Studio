package com.example.ui.components

import com.example.model.AppLanguage
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Status of the Home "planned workout" date line (2773). */
enum class HomePlanTiming { TODAY_UPCOMING, TODAY_PASSED, LATER, CYCLE }

data class HomePlanLine(val text: String, val timing: HomePlanTiming)

/**
 * Text of the extra date line on the Home planned-workout card. A plan dated today stays "today"
 * for the whole calendar day even after its time passed ("still doable today"); the selection
 * moves to the next plan only after local midnight. No calendar row = next day in the cycle.
 */
fun homePlannedDateLine(startIso: String?, now: ZonedDateTime, language: AppLanguage, compact: Boolean = false): HomePlanLine {
    fun t(hu: String, en: String, de: String, ro: String) = when (language) {
        AppLanguage.HU -> hu; AppLanguage.EN -> en; AppLanguage.DE -> de; AppLanguage.RO -> ro
    }
    val start = startIso?.let { runCatching { Instant.parse(it).atZone(now.zone) }.getOrNull() }
        ?: return HomePlanLine(t("↻ Következő a sorban", "↻ Next in sequence", "↻ Als Nächstes in der Reihe", "↻ Următorul din serie"), HomePlanTiming.CYCLE)
    val time = start.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
    val today = now.toLocalDate()
    val day = start.toLocalDate()
    return when {
        day == today && !start.isAfter(now) -> HomePlanLine(
            t("📅 Ma, $time · még ma elvégezhető", "📅 Today, $time · still doable today",
                "📅 Heute, $time · heute noch machbar", "📅 Azi, $time · încă se poate face azi"), HomePlanTiming.TODAY_PASSED)
        day == today -> HomePlanLine(t("📅 Ma, $time", "📅 Today, $time", "📅 Heute, $time", "📅 Azi, $time"), HomePlanTiming.TODAY_UPCOMING)
        day == today.plusDays(1) -> HomePlanLine(t("📅 Holnap, $time", "📅 Tomorrow, $time", "📅 Morgen, $time", "📅 Mâine, $time"), HomePlanTiming.LATER)
        else -> {
            val locale = Locale.forLanguageTag(language.code)
            val date = start.format(DateTimeFormatter.ofPattern("EEE, MMM d.", locale))
            HomePlanLine(if (compact) "📅 $date · $time" else t("📅 Következő: $date · $time", "📅 Next: $date · $time", "📅 Nächstes: $date · $time", "📅 Următorul: $date · $time"), HomePlanTiming.LATER)
        }
    }
}
