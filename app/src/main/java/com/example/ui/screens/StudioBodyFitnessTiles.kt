package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState
import com.repforge.app.HealthJournalStore
import com.repforge.app.ui.health.HealthRecordQuery
import com.repforge.app.ui.health.NativeHealthRecords
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Last locally recorded, timestamped measurement from exactly the selected provider. */
internal data class BodyFitnessReading(val value: Double, val atMs: Long)

internal fun latestBodyFitnessMeasurement(
    rows: List<com.repforge.app.ui.health.HealthRecordRow>,
    maxPercent: Double = Double.POSITIVE_INFINITY
): BodyFitnessReading? = rows.firstNotNullOfOrNull { row ->
    val data = runCatching { JSONObject(row.data) }.getOrNull() ?: return@firstNotNullOfOrNull null
    val value = NativeHealthRecords.number(data, "value") ?: return@firstNotNullOfOrNull null
    if (value <= 0.0 || value > maxPercent || row.startMs <= 0L) null
    else BodyFitnessReading(value, row.startMs)
}

@Composable
internal fun StudioBodyFitnessTiles(state: TrainPilotUiState, onSelect: (String, BodyFitnessReading?) -> Unit) {
    val context = LocalContext.current.applicationContext
    val palette = LocalTrainPilotPalette.current
    val provider = if (state.primaryHealthSourceSamsung) HealthJournalStore.SAMSUNG else HealthJournalStore.HC
    val providerLabel = if (state.primaryHealthSourceSamsung) "Samsung Health" else "Health Connect"
    val readings by produceState<Map<String, BodyFitnessReading>>(
        initialValue = emptyMap(), provider, state.healthRefreshRevision
    ) {
        value = withContext(Dispatchers.IO) {
            val privacy = com.repforge.app.NativePrivacyControls
            val enabled = privacy.enabled(context,
                if (provider == HealthJournalStore.HC) privacy.HEALTH_CONNECT else privacy.SAMSUNG_HEALTH)
            if (!enabled) emptyMap()
            else {
                val records = NativeHealthRecords(HealthJournalStore.get(context))
                val today = LocalDate.now()
                listOf(
                    listOf("bodyFat", "BodyFatRecord", "READ_BODY_FAT"),
                    listOf("spo2", "OxygenSaturationRecord", "READ_OXYGEN_SATURATION"),
                    listOf("vo2", "Vo2MaxRecord", "READ_VO2_MAX")
                ).mapNotNull { (key, type, grant) ->
                    // A persisted measurement is not an authorization to display it after
                    // its Health Connect permission has been revoked.
                    if (provider == HealthJournalStore.HC &&
                        !com.repforge.app.HealthConnectRepository.isAllowed(context, grant)) return@mapNotNull null
                    val page = runCatching {
                        records.load(HealthRecordQuery(
                            day = today.minusYears(2), channel = provider, type = type, endDay = today
                        ))
                    }.getOrNull()
                    val maximum = if (key == "vo2") Double.POSITIVE_INFINITY else 100.0
                    val reading = page?.let { latestBodyFitnessMeasurement(it.rows, maximum) }
                    reading?.let { key to it }
                }.toMap()
            }
        }
    }

    fun tr(hu: String, en: String, de: String, ro: String) = when (state.language) {
        AppLanguage.HU -> hu
        AppLanguage.EN -> en
        AppLanguage.DE -> de
        AppLanguage.RO -> ro
    }
    val zone = ZoneId.systemDefault()
    val tiles = listOf(
        "bodyFat" to tr("Testzsír", "Body fat", "Körperfett", "Grăsime corporală"),
        "spo2" to "SpO₂",
        "vo2" to "VO₂max"
    )
    Column(Modifier.fillMaxWidth().testTag("health_body_fitness_section"),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            tr("Test és fittség", "Body & Fitness", "Körper & Fitness", "Corp și fitness"),
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            color = palette.textPrimary
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            tiles.forEach { (key, title) ->
                val reading = readings[key]
                val readPermission = when (key) {
                    "bodyFat" -> "READ_BODY_FAT"
                    "spo2" -> "READ_OXYGEN_SATURATION"
                    else -> "READ_VO2_MAX"
                }
                val permissionMissing = provider == HealthJournalStore.HC &&
                    !com.repforge.app.HealthConnectRepository.isAllowed(context, readPermission)
                val formatted = reading?.let {
                    String.format(Locale.getDefault(), "%.1f", it.value) +
                        if (key == "vo2") "" else "%"
                } ?: "—"
                val timestamp = reading?.let {
                    Instant.ofEpochMilli(it.atMs).atZone(zone).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.surfaceCard,
                    modifier = Modifier.weight(1f).heightIn(min = 110.dp)
                        .clickable { onSelect(key, reading) }
                        .testTag("health_body_fitness_$key")
                ) {
                    Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(title, style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(formatted, style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, color = palette.textPrimary, maxLines = 1)
                        Text(
                            if (timestamp != null) "$providerLabel · $timestamp"
                            else if (permissionMissing) tr(
                                "Engedély szükséges", "Permission required",
                                "Berechtigung erforderlich", "Permisiune necesară")
                            else tr("Nincs naplózott adat", "No recorded data",
                                "Keine gespeicherten Daten", "Fără date salvate"),
                            color = palette.textSecondary, fontSize = 10.sp,
                            maxLines = 3, overflow = TextOverflow.Ellipsis
                        )
                        if (key == "vo2") Text("ml/kg/min", fontSize = 10.sp, color = palette.textSecondary)
                    }
                }
            }
        }
    }
}
