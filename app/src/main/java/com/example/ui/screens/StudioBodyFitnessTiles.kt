package com.example.ui.screens

import com.example.ui.theme.trainPilotGlassRim
import com.example.ui.theme.trainPilotGlassMaterial
import com.example.ui.theme.glassTile
import com.example.ui.theme.glassCardFill
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import com.example.ui.components.PageLayoutBlock
import com.example.ui.components.PageLayoutGrid
import com.example.ui.components.PageLayoutState
import com.example.ui.components.TrainPilotFitText
import com.repforge.app.ui.theme.trainPilotBorder
import com.repforge.app.ui.theme.trainPilotClickable
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
internal fun StudioBodyFitnessTiles(state: TrainPilotUiState, layout: PageLayoutState, onSelect: (String, BodyFitnessReading?) -> Unit) {
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
        PageLayoutBlock("bodyFat", tr("Testzsír", "Body fat", "Körperfett", "Grăsime corporală")),
        PageLayoutBlock("spo2", "SpO₂"),
        PageLayoutBlock("vo2", "VO₂max")
    )
    Column(Modifier.fillMaxWidth().testTag("health_body_fitness_section"),
        verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            tr("Test és fittség", "Body & Fitness", "Körper & Fitness", "Corp și fitness"),
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            color = palette.textPrimary,
            modifier = Modifier.padding(end = if (layout?.draft != null) 32.dp else 0.dp)
        )
        PageLayoutGrid(layout, "body_fitness_values", tiles, 6.dp, compactEditor = true) { key, modifier ->
            val title = tiles.first { it.id == key }.title
            val readPermission = when (key) {
                "bodyFat" -> "READ_BODY_FAT"
                "spo2" -> "READ_OXYGEN_SATURATION"
                else -> "READ_VO2_MAX"
            }
            val permissionMissing = provider == HealthJournalStore.HC &&
                !com.repforge.app.HealthConnectRepository.isAllowed(context, readPermission)
            val reading = if (permissionMissing) null else readings[key]
            val formatted = reading?.let {
                String.format(Locale.getDefault(), "%.1f", it.value) +
                    if (key == "vo2") "" else "%"
            } ?: "—"
            val timestamp = reading?.let {
                Instant.ofEpochMilli(it.atMs).atZone(zone).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            }
            val shape = RoundedCornerShape(12.dp)
            Surface(
                shape = shape,
                // 2811/G4: inner tile of the "Test és fittség" panel = translucent tile + Tile rim (no own lens).
                color = palette.glassTile(palette.surfaceCard),
                modifier = modifier.trainPilotGlassRim(palette, shape, com.example.ui.theme.GlassRole.Tile).heightIn(min = 70.dp)
                    .trainPilotBorder(1.dp, palette.cardBorder, shape)
                    .trainPilotClickable(shape) { onSelect(key, reading) }
                    .testTag("health_body_fitness_$key")
            ) {
                Column(Modifier.padding(horizontal = 7.dp, vertical = 5.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        val icon = when (key) {
                            "bodyFat" -> Icons.Default.MonitorWeight
                            "spo2" -> Icons.Default.Favorite
                            else -> Icons.Default.FitnessCenter
                        }
                        Icon(icon, contentDescription = null, tint = palette.primary, modifier = Modifier.size(14.dp))
                        TrainPilotFitText(title, style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary, modifier = Modifier.weight(1f))
                    }
                    Text(formatted, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, color = palette.textPrimary, maxLines = 1)
                    Text(
                        if (timestamp != null) "$providerLabel · $timestamp"
                        else if (permissionMissing) tr(
                            "Engedély szükséges", "Permission required",
                            "Berechtigung erforderlich", "Permisiune necesară")
                        else tr("Nincs adat", "No data", "Keine Daten", "Fără date"),
                        color = palette.textSecondary, fontSize = 10.sp,
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
