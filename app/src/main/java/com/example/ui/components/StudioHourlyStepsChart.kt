package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.model.AppLanguage
import com.example.ui.theme.LocalTrainPilotPalette
import com.repforge.app.HealthJournalStore
import com.repforge.app.NativeDataReplacement
import com.repforge.app.ui.health.HourlySteps
import com.repforge.app.ui.health.NativeHealthRecords
import com.repforge.app.ui.health.readHourlySteps
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

@Composable
internal fun StudioHourlyStepsChart(day: LocalDate, provider: String, origin: String, revision: Long, language: AppLanguage) {
    val context = LocalContext.current.applicationContext
    val palette = LocalTrainPilotPalette.current
    val rows by produceState<List<HourlySteps>?>(null, day, provider, origin, revision) {
        value = null
        try {
            val ticket = NativeDataReplacement.continuationForCurrentData(context)
            val result = withContext(Dispatchers.IO) {
                readHourlySteps(NativeHealthRecords(HealthJournalStore.get(context)), day, provider, origin,
                    ZoneId.systemDefault(), Instant.now())
            }
            ticket.requireCurrent(context)
            value = result
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { value = emptyList() }
    }
    fun word(hu: String, en: String, de: String, ro: String) = when (language) {
        AppLanguage.HU -> hu
        AppLanguage.EN -> en
        AppLanguage.DE -> de
        AppLanguage.RO -> ro
    }
    Text(word("Lépések óránként", "Steps by hour", "Schritte pro Stunde", "Pași pe oră"),
        color = palette.textPrimary, style = MaterialTheme.typography.titleSmall)
    val values = rows
    if (values.isNullOrEmpty()) {
        Text(if (values == null) word("Betöltés…", "Loading…", "Wird geladen…", "Se încarcă…")
            else word("Nincs óránkénti adat.", "No hourly data.", "Keine Stundendaten.", "Fără date orare."),
            color = palette.textSecondary, style = MaterialTheme.typography.bodySmall)
        return
    }
    var selection by remember(day, provider, origin) { mutableIntStateOf(-1) }
    val active = selection.takeIf { it in 0..23 } ?: (values.indexOfLast { it.count != null }.takeIf { it >= 0 }
        ?: values.indexOfLast { !it.future }.coerceAtLeast(0))
    val maximum = (values.maxOfOrNull { it.count ?: 0L } ?: 0L).coerceAtLeast(1)
    val entry = values[active]
    val hourLabel = "%02d:00–%02d:00".format(Locale.ROOT, active, active + 1)
    val valueText = when {
        entry.future -> word("Később", "Upcoming", "Später", "Mai târziu")
        entry.count == null -> word("Nincs adat", "No data", "Keine Daten", "Fără date")
        else -> "${entry.count} " + word("lépés", "steps", "Schritte", "pași")
    }
    Canvas(Modifier.fillMaxWidth().height(72.dp).testTag("health_hourly_steps_chart")
        .trainPilotChartTap(day, { x, width -> (x / width * 24f).toInt().coerceIn(0, 23) },
            "$hourLabel · $valueText") { selection = it }
        .semantics {
            customActions = listOf(
                CustomAccessibilityAction(word("Előző óra", "Previous hour", "Vorherige Stunde", "Ora anterioară")) {
                    selection = (active - 1).coerceAtLeast(0); true
                },
                CustomAccessibilityAction(word("Következő óra", "Next hour", "Nächste Stunde", "Ora următoare")) {
                    selection = (active + 1).coerceAtMost(23); true
                }
            )
        }) {
        val pitch = size.width / values.size
        for (row in values) {
            val h = if (row.count == null) (if (row.future) 1.dp else 2.dp).toPx()
                else (size.height * row.count.toDouble() / maximum).toFloat().coerceAtLeast(4.dp.toPx())
            val color = when {
                row.hour == active -> palette.primary
                row.count != null -> trainPilotChartMarkColor(false, palette.primary, palette.textSecondary)
                row.future -> palette.cardBorder
                else -> palette.textSecondary.copy(alpha = 0.38f)
            }
            drawRect(color, Offset(pitch * row.hour + pitch * 0.16f, size.height - h),
                Size(pitch * 0.68f, h))
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf("00", "06", "12", "18", "24").forEach {
            Text(it, color = palette.textSecondary, style = MaterialTheme.typography.labelSmall)
        }
    }
    TrainPilotChartInfoLine("$hourLabel · $valueText")
}
