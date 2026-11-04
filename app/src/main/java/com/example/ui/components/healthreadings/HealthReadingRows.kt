package com.example.ui.components.healthreadings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.ui.theme.LocalTrainPilotPalette
import com.repforge.app.healthreadings.PulseDayReading
import com.repforge.app.healthreadings.RecoveryDayReading
import com.repforge.app.healthreadings.RecoverySource
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/** Compact TrainPilot Graphite & Gold rows for the 7-day pulse overview. */
@Composable
fun HealthPulseWeekRows(
    days: List<PulseDayReading>,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault()
) {
    val palette = LocalTrainPilotPalette.current
    val lang = com.example.localization.LocalTrainPilotStrings.current.language
    fun trH(hu: String, en: String, de: String, ro: String): String = when (lang) {
        AppLanguage.HU -> hu
        AppLanguage.EN -> en
        AppLanguage.DE -> de
        AppLanguage.RO -> ro
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        days.forEach { day ->
            HealthPulseDayRow(day, zone = zone)
        }
        if (days.isNotEmpty()) {
            Text(
                text = trH(
                    "A kiválasztott adatforrás tárolt mérései (nem élő érték).",
                    "Stored readings from the selected data source (not live).",
                    "Gespeicherte Messwerte der gewählten Datenquelle (kein Live-Wert).",
                    "Măsurători stocate din sursa de date selectată (nu în timp real)."
                ),
                style = MaterialTheme.typography.labelSmall,
                color = palette.textMuted,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun HealthPulseDayRow(
    reading: PulseDayReading,
    modifier: Modifier = Modifier,
    zone: ZoneId = ZoneId.systemDefault()
) {
    val palette = LocalTrainPilotPalette.current
    val lang = com.example.localization.LocalTrainPilotStrings.current.language
    fun trH(hu: String, en: String, de: String, ro: String): String = when (lang) {
        AppLanguage.HU -> hu
        AppLanguage.EN -> en
        AppLanguage.DE -> de
        AppLanguage.RO -> ro
    }
    val date = reading.day.format(DateTimeFormatter.ofPattern("MM. dd."))

    val headlineBpm = when {
        reading.averageBpm != null -> "${reading.averageBpm.roundToInt()} bpm"
        reading.lastBpm != null -> "${reading.lastBpm} bpm"
        reading.restingBpm != null -> "${reading.restingBpm.roundToInt()} bpm"
        else -> "—"
    }

    val kindBadge = when {
        reading.averageBpm != null -> if (reading.averageDescriptionHu?.contains("minta", ignoreCase = true) == true) {
            trH("Minták átlaga", "Sample avg", "Stichproben-Ø", "Media eșantioanelor")
        } else {
            trH("Napi átlag", "Daily avg", "Tages-Ø", "Media zilnică")
        }
        reading.lastBpm != null -> trH("Utolsó mérés", "Last reading", "Letzte Messung", "Ultima citire")
        reading.restingBpm != null -> trH("Nyugalmi", "Resting", "Ruhepuls", "Repaus")
        else -> trH("Nincs adat", "No data", "Keine Daten", "Fără date")
    }

    val details = buildList {
        val lastAt = reading.lastAt
        if (lastAt != null && reading.lastBpm != null) {
            val timeStr = lastAt.atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm"))
            add(
                trH(
                    "Utolsó: ${reading.lastBpm} bpm ($timeStr)",
                    "Last: ${reading.lastBpm} bpm ($timeStr)",
                    "Letzter: ${reading.lastBpm} bpm ($timeStr)",
                    "Ultimul: ${reading.lastBpm} bpm ($timeStr)"
                )
            )
        }
        if (reading.restingBpm != null && reading.averageBpm != null) {
            add(
                trH(
                    "Nyugalmi: ${reading.restingBpm.toInt()} bpm",
                    "Resting: ${reading.restingBpm.toInt()} bpm",
                    "Ruhe: ${reading.restingBpm.toInt()} bpm",
                    "Repaus: ${reading.restingBpm.toInt()} bpm"
                )
            )
        }
        if (reading.observedMinBpm != null || reading.observedMaxBpm != null) {
            val low = reading.observedMinBpm?.toInt()?.toString() ?: "—"
            val high = reading.observedMaxBpm?.toInt()?.toString() ?: "—"
            add(
                trH(
                    "Tartomány: $low–$high bpm",
                    "Range: $low–$high bpm",
                    "Bereich: $low–$high bpm",
                    "Interval: $low–$high bpm"
                )
            )
        }
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = palette.surfaceElevated,
        border = BorderStroke(1.dp, palette.cardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = date,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.textPrimary
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(palette.primary.copy(alpha = 0.14f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = kindBadge,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.primary
                        )
                    }
                }
                Text(
                    text = headlineBpm,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (headlineBpm == "—") palette.textSecondary else palette.primary
                )
            }
            if (details.isNotEmpty()) {
                Text(
                    text = details.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
            }
        }
    }
}

@Composable
fun HealthRecoveryRow(reading: RecoveryDayReading, modifier: Modifier = Modifier) {
    val palette = LocalTrainPilotPalette.current
    val lang = com.example.localization.LocalTrainPilotStrings.current.language
    fun trH(hu: String, en: String, de: String, ro: String): String = when (lang) {
        AppLanguage.HU -> hu
        AppLanguage.EN -> en
        AppLanguage.DE -> de
        AppLanguage.RO -> ro
    }
    val origin = when (reading.source) {
        RecoverySource.SAMSUNG_ENERGY -> trH("Samsung Energia", "Samsung Energy", "Samsung Energie", "Energie Samsung")
        RecoverySource.TRAINPILOT_ESTIMATE -> trH("TrainPilot-becslés", "TrainPilot estimate", "TrainPilot-Schätzung", "Estimare TrainPilot")
        RecoverySource.NONE -> trH("Regeneráció", "Recovery", "Regeneration", "Recuperare")
    }
    val localizedExplanation = when (reading.source) {
        RecoverySource.SAMSUNG_ENERGY -> trH(
            "Samsung Health által számított napi érték.",
            "Daily score calculated by Samsung Health.",
            "Von Samsung Health berechneter Tageswert.",
            "Scor zilnic calculat de Samsung Health."
        )
        RecoverySource.TRAINPILOT_ESTIMATE -> trH(
            "TrainPilot-becslés az elérhető adatokból; nem Samsung Health-pontszám.",
            "TrainPilot estimate from available metrics; not a Samsung Health score.",
            "TrainPilot-Schätzung aus verfügbaren Daten; kein Samsung-Health-Wert.",
            "Estimare TrainPilot din datele disponibile; nu este scor Samsung Health."
        )
        RecoverySource.NONE -> trH(
            "Nincs igazolt regenerációs mérés.",
            "No verified recovery reading.",
            "Keine bestätigte Erholungsmessung.",
            "Nicio măsurătoare de recuperare verificată."
        )
    }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = palette.surfaceElevated,
        border = BorderStroke(1.dp, palette.cardBorder),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(origin, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                Text(reading.displayScoreHu, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = palette.primary)
            }
            Text(
                text = if (lang == AppLanguage.HU) reading.explanationHu else localizedExplanation,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary
            )
        }
    }
}
