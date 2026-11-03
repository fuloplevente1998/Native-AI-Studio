package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.repforge.app.HealthJournalStore
import com.repforge.app.NativeDataReplacement
import com.repforge.app.NativeHealthProviderData
import com.repforge.app.NativeWorkoutHealthSource
import com.repforge.app.ui.health.HealthRecordBrowser
import com.repforge.app.ui.health.NativeDetailedHealthMetrics
import com.repforge.app.ui.health.NativeHealthMetrics
import com.repforge.app.ui.theme.TrainPilotOutlinedButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.LocalDate

/** A read-only exact-day browser. Changing its day/provider never changes today's tiles. */
@Composable
internal fun StudioHealthMetricsBrowser(initialProvider: String, language: String, refreshKey: String) {
    var opened by rememberSaveable { mutableStateOf(false) }
    var provider by rememberSaveable(initialProvider) { mutableStateOf(initialProvider) }
    var day by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var reload by remember { mutableIntStateOf(0) }
    var recordType by rememberSaveable { mutableStateOf<String?>(null) }
    var editingDate by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current.applicationContext
    fun label(hu: String, en: String) = if (language == "hu") hu else en
    TrainPilotOutlinedButton(onClick = { opened = true }, modifier = Modifier.fillMaxWidth().testTag("health_all_metrics")) {
        Text(label("Mind a 26 metrika • dátum és forrás", "All 26 metrics • date and source"))
    }
    if (!opened) return
    val date = LocalDate.parse(day)
    var raw by remember(provider, day, reload, refreshKey) { mutableStateOf<JSONObject?>(null) }
    var loading by remember(provider, day, reload, refreshKey) { mutableStateOf(true) }
    var failed by remember(provider, day, reload, refreshKey) { mutableStateOf(false) }
    LaunchedEffect(provider, day, reload, refreshKey) {
        try {
            val store = HealthJournalStore.get(context)
            val ticket = NativeDataReplacement.continuationForCurrentData(context)
            val generation = withContext(Dispatchers.IO) { store.generation() }
            val result = withContext(Dispatchers.IO) {
                check(store.pendingRestore().isEmpty())
                NativeHealthProviderData.day(store, provider, date)
            }
            ticket.requireCurrent(context)
            check(withContext(Dispatchers.IO) { store.generation() == generation && store.pendingRestore().isEmpty() })
            raw = result
        } catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel
        } catch (_: Exception) { failed = true
        } finally { loading = false }
    }
    if (recordType != null) {
        StudioNativeEditorTheme {
            HealthRecordBrowser(day, provider, recordType!!, language, onClose = { recordType = null })
        }
    } else CompactCalendarDialog(label("Egészségmetrikák", "Health metrics"), "health_metrics_dialog", { opened = false }) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(HealthJournalStore.SAMSUNG, HealthJournalStore.HC).forEach { source ->
                FilterChip(selected = provider == source, onClick = { provider = source },
                    label = { Text(NativeWorkoutHealthSource.label(source)) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp))
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { day = date.minusDays(1).toString() }, modifier = Modifier.testTag("metrics_previous_day")) { Text("‹") }
            TextButton(onClick = { editingDate = true }, modifier = Modifier.weight(1f).testTag("metrics_select_date")) { Text(day) }
            TextButton(onClick = { day = date.plusDays(1).toString() }, enabled = date < LocalDate.now(),
                modifier = Modifier.testTag("metrics_next_day")) { Text("›") }
        }
        Row(Modifier.fillMaxWidth()) {
            TextButton(onClick = { day = LocalDate.now().toString() }, modifier = Modifier.weight(1f)) { Text(label("Ma", "Today")) }
            TextButton(onClick = { reload++ }, modifier = Modifier.weight(1f).testTag("metrics_reload")) { Text(label("Újraolvasás", "Reload")) }
            TextButton(onClick = { recordType = "" }, modifier = Modifier.weight(1f)) { Text(label("Rekordok", "Records")) }
        }
        when {
            loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
            failed -> Text(label("Az adatok nem tölthetők be. Próbáld újra.", "Could not load data. Try again."), color = MaterialTheme.colorScheme.error)
            else -> {
                Text(label("Mentett napi mérések", "Stored daily measurements"), style = MaterialTheme.typography.labelSmall)
                val metrics = NativeHealthMetrics.parse(context, NativeDetailedHealthMetrics.admitted(raw, provider, date), language)
                metrics.forEach { metric ->
                    val status = NativeDetailedHealthMetrics.status(raw, metric.key, provider, date)
                    HorizontalDivider()
                    TextButton(onClick = { recordType = NativeDetailedHealthMetrics.recordType(metric.key) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("health_metric_${metric.key}")) {
                        Column(Modifier.weight(1f)) {
                            Text(metric.label, style = MaterialTheme.typography.bodyMedium)
                            val source = when (status) {
                                NativeDetailedHealthMetrics.Status.AVAILABLE -> {
                                    val originKey = when (metric.key) {
                                        "weightKg" -> "weightSource"; "bodyFatPercent" -> "bodyFatSource"
                                        "oxygenSaturationPercent" -> "oxygenSaturationSource"; "hrvRmssdMs" -> "hrvSource"
                                        else -> metric.key + "Source"
                                    }
                                    val origin = raw?.optString(originKey).orEmpty().takeIf { it.isNotBlank() && it != "null" }
                                    listOfNotNull(NativeWorkoutHealthSource.label(provider), origin,
                                        raw?.optString(metric.key + "Time")?.takeIf { it.isNotBlank() && it != "null" }).distinct().joinToString(" · ")
                                }
                                NativeDetailedHealthMetrics.Status.DENIED -> label("Olvasási engedély hiányzik", "Read permission missing")
                                NativeDetailedHealthMetrics.Status.ERROR -> label("Az olvasás hibát jelzett", "Read failed")
                                NativeDetailedHealthMetrics.Status.NO_DATA -> label("Nincs elérhető adat", "No available data")
                            }
                            Text(source, style = MaterialTheme.typography.labelSmall)
                        }
                        Text(if (metric.isAvailable) "${metric.valueText} ${metric.unit}".trim() else "—")
                    }
                }
            }
        }
    }
    if (editingDate) {
        var input by rememberSaveable(day) { mutableStateOf(day) }
        val parsed = runCatching { LocalDate.parse(input) }.getOrNull()?.takeIf { it <= LocalDate.now() }
        TrainPilotAlertDialog(onDismissRequest = { editingDate = false },
            title = { Text(label("Dátum választása", "Select date")) },
            text = { OutlinedTextField(input, { input = it }, singleLine = true,
                label = { Text("YYYY-MM-DD") }, isError = parsed == null, modifier = Modifier.testTag("metrics_date_input")) },
            confirmButton = { TextButton(onClick = { day = parsed!!.toString(); editingDate = false }, enabled = parsed != null,
                modifier = Modifier.testTag("metrics_date_confirm")) { Text("OK") } },
            dismissButton = { TextButton(onClick = { editingDate = false }) { Text(label("Mégse", "Cancel")) } })
    }
}
