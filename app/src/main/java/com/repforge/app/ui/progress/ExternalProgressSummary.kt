package com.repforge.app.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.repforge.app.data.db.WorkoutHistoryDao
import com.repforge.app.ui.theme.LocalTrainPilotColors
import org.json.JSONObject
import java.text.NumberFormat
import java.time.Instant
import java.util.Locale

/** Counts only canonical external history rows, never linked mirrors or fabricated sets. */
data class ExternalProgressSummary(val count: Int, val elapsedSeconds: Long, val meters: Double?, val distanceCount: Int) {
    companion object {
        internal suspend fun load(dao: WorkoutHistoryDao, now: Long): ExternalProgressSummary {
            var after = ""
            var count = 0; var elapsed = 0L; var meters = 0.0; var distanceCount = 0
            while (true) {
                val rows = dao.pageExternalProgress(now - 30L * 86_400_000L, now, after, 64)
                if (rows.isEmpty()) break
                for (row in rows) {
                    if (row.finishedEpochMs <= row.startedEpochMs ||
                        dao.isTombstoned(row.id, JSONObject.quote(row.id))) continue
                    count++
                    elapsed += (row.finishedEpochMs - row.startedEpochMs) / 1000
                    val source = runCatching { JSONObject(row.rawJson).optJSONObject("externalWorkout359") }.getOrNull() ?: continue
                    val same = runCatching { Instant.parse(source.getString("start")).toEpochMilli() == row.startedEpochMs &&
                        Instant.parse(source.getString("end")).toEpochMilli() == row.finishedEpochMs }.getOrDefault(false)
                    if (!same || source.optBoolean("sourceDeleted") || source.optBoolean("windowChanged")) continue
                    val value = (source.optJSONObject("metrics")?.optJSONObject("values")?.opt("distanceMeters") as? Number)?.toDouble()
                    if (value != null && value.isFinite() && value >= 0) { meters += value; distanceCount++ }
                }
                after = rows.last().id
            }
            return ExternalProgressSummary(count, elapsed, meters.takeIf { distanceCount > 0 }, distanceCount)
        }
    }
}

@Composable
internal fun ExternalProgressPanel(summary: ExternalProgressSummary, language: String) {
    val colors = LocalTrainPilotColors.current
    val locale = Locale.forLanguageTag(language)
    val number = NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 2 }
    fun tr(hu: String, en: String, de: String, ro: String) = when (language.lowercase(Locale.ROOT).take(2)) {
        "hu" -> hu; "de" -> de; "ro" -> ro; else -> en
    }
    Column(Modifier.fillMaxWidth().testTag("progress_external_workouts"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(tr("Külső edzések · 30 nap", "External workouts · 30 days", "Externe Trainings · 30 Tage", "Antrenamente externe · 30 zile"), color = colors.text)
        Text(tr("${summary.count} edzés · eltelt idő ${summary.elapsedSeconds / 60} perc",
            "${summary.count} workouts · elapsed ${summary.elapsedSeconds / 60} min",
            "${summary.count} Trainings · verstrichen ${summary.elapsedSeconds / 60} Min.",
            "${summary.count} antrenamente · timp scurs ${summary.elapsedSeconds / 60} min"), color = colors.muted)
        val distance = summary.meters?.let { "${number.format(it / 1000)} km (${summary.distanceCount}/${summary.count})" } ?: "—"
        Text(tr("Tárolt részleges távolság", "Cached partial distance", "Gespeicherte Teildistanz", "Distanță parțială stocată") + ": $distance", color = colors.muted)
        Text(tr("A külső mérés nem súlyzós sorozat.", "External measurement is not a strength set.",
            "Externe Messung ist kein Krafttrainingssatz.", "Măsurarea externă nu este o serie de forță."), color = colors.muted)
    }
}
