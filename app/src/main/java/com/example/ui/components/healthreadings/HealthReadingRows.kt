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
            Text(
                "Utolsó pulzus: ${reading.lastBpm} bpm · ${lastAt.atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm"))} (nem élő érték)",
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (reading.restingBpm != null) {
            Text("Nyugalmi pulzus: ${reading.restingBpm.toInt()} bpm", style = MaterialTheme.typography.bodySmall)
        }
        if (reading.observedMinBpm != null || reading.observedMaxBpm != null) {
            val low = reading.observedMinBpm?.toInt()?.toString() ?: "—"
            val high = reading.observedMaxBpm?.toInt()?.toString() ?: "—"
            Text("${reading.rangeDescriptionHu ?: "Megfigyelt értékek"}: $low–$high bpm", style = MaterialTheme.typography.bodySmall)
        }
        if (reading.totalSamples != null && reading.totalSamples > reading.displayedSamples.size) {
            Text("A megjelenített ${reading.displayedSamples.size} minta csak kivonat, nem napi átlag.",
                style = MaterialTheme.typography.bodySmall)
        }
        Text(reading.explanationHu, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun HealthRecoveryRow(reading: RecoveryDayReading, modifier: Modifier = Modifier) {
    val origin = when (reading.source) {
        RecoverySource.SAMSUNG_ENERGY -> "Samsung Energia"
        RecoverySource.TRAINPILOT_ESTIMATE -> "TrainPilot-becslés"
        RecoverySource.NONE -> "Regeneráció"
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
