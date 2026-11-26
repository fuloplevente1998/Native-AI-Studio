package com.repforge.app.ui.journal

import android.content.res.Resources
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.repforge.app.data.db.WorkoutHistoryWithDetails
import com.repforge.app.ui.theme.LocalTrainPilotColors
import org.json.JSONObject
import java.text.NumberFormat
import java.time.Instant
import java.util.Locale

@Composable
internal fun JournalExternalWorkoutMetrics(detail: WorkoutHistoryWithDetails, text: Resources, language: String,
    /** #359: source package/title/device belong to the Forrás view only, not to the statistics summary. */
    showSource: Boolean = true) {
    val colors = LocalTrainPilotColors.current
    val workout = detail.workout
    val source = remember(workout.rawJson) {
        runCatching { JSONObject(workout.rawJson).optJSONObject("externalWorkout359") }.getOrNull()
    }
    val number = remember(language) { NumberFormat.getNumberInstance(Locale.forLanguageTag(language)).apply { maximumFractionDigits = 1 } }
    fun local(hu: String, en: String, de: String, ro: String) = when (language.lowercase(Locale.ROOT).substringBefore('-')) {
        "hu" -> hu; "de" -> de; "ro" -> ro; else -> en
    }
    val matches = source != null && runCatching {
        Instant.parse(source.getString("start")) == Instant.parse(workout.started) &&
            Instant.parse(source.getString("end")) == Instant.parse(workout.finished)
    }.getOrDefault(false)
    val deleted = source?.optBoolean("sourceDeleted") == true
    val values = if (matches && !deleted && source?.optBoolean("windowChanged") != true)
        source?.optJSONObject("metrics")?.optJSONObject("values") else null
    val metrics = values?.keys()?.asSequence()?.mapNotNull { key ->
        (values.opt(key) as? Number)?.toDouble()?.takeIf { it.isFinite() && it >= 0 }?.let { key to it }
    }?.toMap().orEmpty()
    Column(Modifier.fillMaxWidth().padding(12.dp).testTag("journal_external_metrics"),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(local("Külső edzés mérései", "External workout measurements", "Externe Trainingsmessungen", "Măsurători antrenament extern"),
            style = MaterialTheme.typography.titleMedium, color = colors.text)
        if (showSource) source?.optString("source")?.takeIf { it.isNotBlank() }?.let { origin ->
            val provider = source.optString("provider")
            val label = if (provider == com.repforge.app.HealthJournalStore.HC &&
                origin == "com.sec.android.app.shealth")
                com.repforge.app.NativeWorkoutHealthSummary.originLabel(provider, setOf(origin), language)
            else origin
            Text(label, color = colors.muted, modifier = Modifier.testTag("journal_external_source_label"))
        }
        if (showSource) source?.optString("title")?.takeIf { it.isNotBlank() }?.let { Text(it, color = colors.text) }
        if (showSource) source?.optJSONObject("device")?.let { device ->
            val name = listOf("manufacturer", "model").mapNotNull { key ->
                device.optString(key).takeIf { !device.isNull(key) && it.isNotBlank() }
            }.joinToString(" ")
            if (name.isNotBlank()) Text(name, color = colors.muted)
        }
        source?.optJSONArray("laps")?.takeIf { matches && !deleted && it.length() > 0 }?.let { laps ->
            Text(local("Körök: ${laps.length()}", "Laps: ${laps.length()}", "Runden: ${laps.length()}", "Tururi: ${laps.length()}"), color = colors.muted)
            for (i in 0 until minOf(laps.length(), 100)) {
                val lap = laps.optJSONObject(i) ?: continue
                val length = (lap.opt("lengthMeters") as? Number)?.toDouble()
                val seconds = (lap.optLong("endMs") - lap.optLong("startMs")) / 1000
                Text("${i+1}. · ${seconds.coerceAtLeast(0)} s" + (length?.let { " · ${number.format(it)} m" } ?: ""), color = colors.muted)
            }
        }
        source?.optJSONArray("segments")?.takeIf { matches && !deleted && it.length() > 0 }?.let { segments ->
            Text(local("Forrás szegmensek: ${segments.length()}", "Source segments: ${segments.length()}", "Quellsegmente: ${segments.length()}", "Segmente sursă: ${segments.length()}"), color = colors.muted)
            for (i in 0 until minOf(segments.length(), 100)) {
                val segment = segments.optJSONObject(i) ?: continue
                val reps = (segment.opt("repetitions") as? Number)?.toInt()
                val type = segment.optInt("segmentType")
                val extra = listOfNotNull(
                    (segment.opt("weightKg") as? Number)?.let { "${number.format(it)} kg" },
                    (segment.opt("setIndex") as? Number)?.let { local("Sorozatindex", "Set index", "Satzindex", "Index serie") + ": " + it },
                    (segment.opt("rpe") as? Number)?.let { "RPE ${number.format(it)}/10" }
                )
                Text("${i+1}. · #$type" + (reps?.let { " · $it ×" } ?: "") +
                    extra.joinToString(" · ", prefix = if (extra.isEmpty()) "" else " · "), color = colors.muted)
            }
        }
        if (deleted) Text(local("A forrás törölte az edzést; a helyi naplóadatok megmaradtak.",
            "Source deleted the workout; local journal data was kept.", "Quelle hat das Training gelöscht; lokale Protokolldaten bleiben erhalten.",
            "Sursa a șters antrenamentul; datele locale au fost păstrate."), color = colors.muted)
        if (source != null && !matches) Text(local("A forrás és a helyi edzés időablaka eltér. Ellenőrzés szükséges.",
            "Source and local workout windows differ. Review required.", "Quell- und lokales Zeitfenster unterscheiden sich. Prüfung erforderlich.",
            "Intervalele sursei și antrenamentului local diferă. Necesită verificare."), color = colors.muted)
        if (matches && !deleted) source?.optJSONObject("sessionEvidence")?.let { evidence ->
            evidence.optString("exerciseTypeName").takeIf { it.isNotBlank() }?.let { Text(it, color = colors.muted) }
            (evidence.opt("count") as? Number)?.let {
                Text(local("Forrás szerinti darabszám", "Source count", "Quellanzahl", "Număr sursă") +
                    ": $it · " + evidence.optString("countTypeName"), color = colors.muted)
            }
            (evidence.opt("rpe") as? Number)?.let { Text("RPE: ${number.format(it)}/10", color = colors.muted) }
        }
        JournalLinkedExternalSources(workout.rawJson, text, language)
        if (metrics.isEmpty()) Text(text.getString(com.repforge.app.R.string.journal_no_data), color = colors.muted)
        else {
            JournalHealthMetrics(metrics, "external", text, number)
            metrics["vo2Max"]?.let { Text("VO₂max: ${number.format(it)} ml/kg/min", color = colors.muted) }
            metrics["sessionDurationSeconds"]?.let { Text(local("Forrás szerinti edzésidő", "Provider session duration", "Trainingsdauer laut Quelle", "Durata sesiunii din sursă") +
                ": ${number.format(it / 60)} min", color = colors.muted) }
            metrics["recordedPauseSeconds"]?.let { Text(local("Rögzített szünet", "Recorded pause", "Erfasste Pause", "Pauză înregistrată") +
                ": ${number.format(it)} s", color = colors.muted) }
            metrics["averageSpeedMps"]?.takeIf { it > 0 }?.let { speed ->
                val pace = (1000.0 / speed).takeIf { it.isFinite() && it < 86400 }?.toInt()
                if (pace != null) Text(local("Tempó (átlagsebességből)", "Pace (from average speed)",
                    "Tempo (aus Durchschnittsgeschwindigkeit)", "Ritm (din viteza medie)") +
                    ": ${pace / 60}:${(pace % 60).toString().padStart(2, '0')} min/km", color = colors.muted)
            }
            source?.optJSONObject("metrics")?.optJSONObject("series")?.optJSONArray("HeartRate")?.let {
                ExternalHeartRateZones(it, language)
            }
            if (metrics.containsKey("workoutCalories")) Text(local("Edzésenergia, aktív/teljes bontás nélkül.",
                "Session energy, active/total split unavailable.", "Trainingsenergie ohne Aktiv-/Gesamtaufteilung.",
                "Energie sesiune fără separare activă/totală."), color = colors.muted)
            Text(local("Részleges, azonos forrású mérés; edzésösszesítő vagy minták alapján.",
                "Partial same-source evidence; session summary or measured samples.",
                "Teilweise Messwerte derselben Quelle; Sitzungsübersicht oder Stichproben.",
                "Date parțiale din aceeași sursă; sumarul sesiunii sau eșantioane măsurate."), color = colors.muted)
        }
    }
}
