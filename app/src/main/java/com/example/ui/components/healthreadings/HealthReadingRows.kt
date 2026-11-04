package com.example.ui.components.healthreadings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.repforge.app.healthreadings.PulseDayReading
import com.repforge.app.healthreadings.RecoveryDayReading
import com.repforge.app.healthreadings.RecoverySource
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Small inner rows; the host screen owns the stronger outer TrainPilot frame. */
@Composable
fun HealthPulseWeekRows(
    days: List<PulseDayReading>,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault()
) {
    Column(modifier = modifier.fillMaxWidth()) {
        days.forEachIndexed { index, day ->
            if (index > 0) HorizontalDivider()
            HealthPulseDayRow(day, zone = zone)
        }
    }
}

@Composable
fun HealthPulseDayRow(
    reading: PulseDayReading,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault()
) {
    val lang = com.example.localization.LocalTrainPilotStrings.current.language
    fun trH(hu: String, en: String, de: String, ro: String): String = when (lang) {
        com.example.model.AppLanguage.HU -> hu
        com.example.model.AppLanguage.EN -> en
        com.example.model.AppLanguage.DE -> de
        com.example.model.AppLanguage.RO -> ro
    }
    val date = reading.day.format(DateTimeFormatter.ofPattern("MM. dd."))
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(date, style = MaterialTheme.typography.labelMedium)
            Text(reading.mainLabelHu, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        val lastAt = reading.lastAt
        if (lastAt != null) {
            val timeStr = lastAt.atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm"))
            Text(
                trH(
                    "Utolsó pulzus: ${reading.lastBpm} bpm · $timeStr (nem élő érték)",
                    "Last heart rate: ${reading.lastBpm} bpm · $timeStr (not live)",
                    "Letzter Puls: ${reading.lastBpm} bpm · $timeStr (kein Live-Wert)",
                    "Ultimul puls: ${reading.lastBpm} bpm · $timeStr (nu este în timp real)"
                ),
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (reading.restingBpm != null) {
            Text(
                trH(
                    "Nyugalmi pulzus: ${reading.restingBpm.toInt()} bpm",
                    "Resting heart rate: ${reading.restingBpm.toInt()} bpm",
                    "Ruhepuls: ${reading.restingBpm.toInt()} bpm",
                    "Puls în repaus: ${reading.restingBpm.toInt()} bpm"
                ),
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (reading.observedMinBpm != null || reading.observedMaxBpm != null) {
            val low = reading.observedMinBpm?.toInt()?.toString() ?: "—"
            val high = reading.observedMaxBpm?.toInt()?.toString() ?: "—"
            val rangeDesc = reading.rangeDescriptionHu ?: trH("Megfigyelt értékek", "Observed values", "Beobachtete Werte", "Valori observate")
            Text("$rangeDesc: $low–$high bpm", style = MaterialTheme.typography.bodySmall)
        }
        if (reading.totalSamples != null && reading.totalSamples > reading.displayedSamples.size) {
            Text(
                trH(
                    "A megjelenített ${reading.displayedSamples.size} minta csak kivonat, nem napi átlag.",
                    "The displayed ${reading.displayedSamples.size} samples are an excerpt, not a daily average.",
                    "Die angezeigten ${reading.displayedSamples.size} Messwerte sind nur ein Auszug, kein Tagesdurchschnitt.",
                    "Cele ${reading.displayedSamples.size} eșantioane afișate sunt doar un extras, nu media zilnică."
                ),
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(reading.explanationHu, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun HealthRecoveryRow(reading: RecoveryDayReading, modifier: Modifier = Modifier) {
    val lang = com.example.localization.LocalTrainPilotStrings.current.language
    fun trH(hu: String, en: String, de: String, ro: String): String = when (lang) {
        com.example.model.AppLanguage.HU -> hu
        com.example.model.AppLanguage.EN -> en
        com.example.model.AppLanguage.DE -> de
        com.example.model.AppLanguage.RO -> ro
    }
    val origin = when (reading.source) {
        RecoverySource.SAMSUNG_ENERGY -> trH("Samsung Energia", "Samsung Energy", "Samsung Energie", "Energie Samsung")
        RecoverySource.TRAINPILOT_ESTIMATE -> trH("TrainPilot-becslés", "TrainPilot estimate", "TrainPilot-Schätzung", "Estimare TrainPilot")
        RecoverySource.NONE -> trH("Regeneráció", "Recovery", "Regeneration", "Recuperare")
    }
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(origin, style = MaterialTheme.typography.labelLarge)
            Text(reading.displayScoreHu, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        Text(reading.explanationHu, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
