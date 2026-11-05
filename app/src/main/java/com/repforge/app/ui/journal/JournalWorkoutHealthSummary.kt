package com.repforge.app.ui.journal

import android.content.res.Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.repforge.app.NativeWorkoutHealthSummary
import com.repforge.app.R
import com.repforge.app.data.db.WorkoutHistoryWithDetails
import com.repforge.app.ui.theme.LocalTrainPilotColors
import com.repforge.app.ui.theme.TrainPilotFrames
import java.text.NumberFormat
import java.util.Locale

@Composable
internal fun JournalWorkoutHealthSummary(detail: WorkoutHistoryWithDetails, text: Resources, language: String) {
    val colors = LocalTrainPilotColors.current
    val result = remember(detail.workout) { NativeWorkoutHealthSummary.read(detail.workout) }
    val number = remember(language) { NumberFormat.getNumberInstance(Locale.forLanguageTag(language)).apply { maximumFractionDigits = 1 } }
    fun local(hu: String, en: String, de: String, ro: String) = when (language.substringBefore('-')) {
        "hu" -> hu; "de" -> de; "ro" -> ro; else -> en
    }
    Column(Modifier.fillMaxWidth().background(colors.card, MaterialTheme.shapes.large)
        .border(TrainPilotFrames.page(colors.line, colors.accent), MaterialTheme.shapes.large)
        .padding(12.dp).testTag("journal_workout_health_summary"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(local("Edzés pulzus és energia", "Workout heart rate and energy", "Trainingspuls und Energie", "Puls și energie antrenament"),
            style = MaterialTheme.typography.titleMedium, color = colors.text)
        for ((tag, metric) in listOf("summary_heart" to result.heart, "summary_energy" to result.energy)) {
            JournalHealthMetrics(metric.values, tag, text, number)
            if (metric.values.isEmpty()) Text(text.getString(R.string.journal_no_data), color = colors.muted,
                modifier = Modifier.testTag("journal_${tag}_missing"))
            val providers = metric.evidence.map { NativeWorkoutHealthSummary.label(it.provider) }.distinct()
            if (providers.isNotEmpty()) Text("${text.getString(R.string.journal_health_sources)}: ${providers.joinToString(", ")}",
                color = colors.muted, modifier = Modifier.testTag("journal_${tag}_sources"))
            if (metric.evidence.any { it.provider == NativeWorkoutHealthSummary.WEAR && it.legacyWindow }) Text(
                local("Mentett órás összesítő; részletes mérési időbélyegek nélkül.",
                    "Saved watch summary; detailed measurement timestamps are unavailable.",
                    "Gespeicherte Uhrübersicht; detaillierte Messzeitstempel fehlen.",
                    "Rezumat salvat de ceas; marcajele temporale detaliate lipsesc."), color = colors.muted)
            if (metric.incomplete) Text(local("Részleges vagy hiányzó mérés; a hiányzó időszakot nem becsüljük.",
                "Partial or missing measurement; missing periods are not estimated.",
                "Teilweise oder fehlende Messung; fehlende Zeiträume werden nicht geschätzt.",
                "Măsurare parțială sau lipsă; perioadele lipsă nu sunt estimate."), color = colors.muted)
            if (metric.overlapExcluded) Text(local("Az átfedő összesítők egyszer számítanak; a nem szétválasztható részek kimaradnak.",
                "Overlapping summaries count once; portions that cannot be separated are omitted.",
                "Überlappende Summen zählen einmal; untrennbare Teile werden ausgelassen.",
                "Totalurile suprapuse se numără o dată; părțile inseparabile sunt omise."), color = colors.muted)
            if (metric.values.containsKey("workoutCalories")) {
                Text(local("Edzéshez rendelt energia; az aktív/teljes jelentést a szolgáltató részletei jelzik.",
                    "Session-linked energy; see the provider details for its active/total definition.",
                    "Trainingsbezogene Energie; Aktiv-/Gesamtdefinition in den Quelldetails.",
                    "Energie asociată sesiunii; definiția activă/totală este în detaliile sursei."), color = colors.muted)
            }
        }
        if (result.rejectedEvidence) Text(text.getString(R.string.journal_health_stale), color = colors.muted)
        Text(local("Átfedésnél: mentett WearOS → Samsung Health → Health Connect. A különböző energiafajtákat nem adjuk össze. Részletek a Forrás fülön.",
            "For overlaps: saved WearOS → Samsung Health → Health Connect. Energy types stay separate. Details in Source.",
            "Bei Überlappung: gespeichertes WearOS → Samsung Health → Health Connect. Energiearten bleiben getrennt. Details unter Quelle.",
            "La suprapunere: WearOS salvat → Samsung Health → Health Connect. Tipurile de energie rămân separate. Detalii în Sursă."), color = colors.muted)
    }
}
