package com.repforge.app.ui.journal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.repforge.app.HealthJournalStore
import com.repforge.app.NativeWorkoutHealth
import com.repforge.app.NativeWorkoutHealthSource
import com.repforge.app.NativeSavedWearHealth
import com.repforge.app.R
import com.repforge.app.data.db.WorkoutHistoryWithDetails
import com.repforge.app.ui.theme.LocalTrainPilotColors
import org.json.JSONObject
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** Three independent sources. Each opens its own saved data, never substitutes another provider. */
@Composable
internal fun JournalHealthPanel(detail: WorkoutHistoryWithDetails, text: android.content.res.Resources,
    language: String, refreshing: Boolean, error: Int?, enabled: Boolean,
    provider: String = HealthJournalStore.HC, status: String? = null,
    selectProvider: (String) -> Unit = {}, permissions: (@Composable (String) -> Unit)? = null,
    refresh: () -> Unit) {
    val colors = LocalTrainPilotColors.current
    val workout = detail.workout
    val raw = remember(workout.rawJson) { parseHealth(workout.rawJson) }
    val wear = remember(workout.wearMetricsJson, workout.rawJson) { NativeSavedWearHealth.read(workout) }
    val document = remember(workout.health240Json) { parseHealth(workout.health240Json) }
    val locale = remember(language) { Locale.forLanguageTag(language) }
    val number = remember(locale) { NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 1 } }
    val date = remember(locale) { DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(locale).withZone(ZoneId.systemDefault()) }
    fun time(value: String) = runCatching { date.format(Instant.parse(value)) }.getOrDefault("—")
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        JournalHealthSourceCard(workout.id, "wear", "TrainPilot WearOS", "journal_provider_wear", text, enabled) {
            Text("Health Services", color = colors.muted)
            if (wear != null && raw?.optBoolean("nativeHealthWindowStale") == true) {
                Text(text.getString(R.string.journal_edit_stale), color = colors.muted)
                raw.optJSONObject("nativeOriginalHealthWindow")?.let {
                    Text("${text.getString(R.string.journal_health_window)}: ${time(it.optString("started"))} – ${time(it.optString("finished"))}", color = colors.muted)
                }
            }
            val metrics = wear?.let { NativeWorkoutHealth.metrics(it, true) }.orEmpty()
            JournalHealthMetrics(metrics, "wear", text, number)
            if (metrics.isEmpty()) Text(text.getString(R.string.journal_no_data), color = colors.muted)
            if (wear?.optBoolean("partial") == true) Text(text.getString(R.string.journal_health_partial), color = colors.muted)
            Text(text.getString(R.string.journal_health_separate_energy), color = colors.muted)
        }
        for (source in listOf(HealthJournalStore.SAMSUNG, HealthJournalStore.HC)) key(source) {
            val tag = if (source == HealthJournalStore.HC) "hc" else "samsung"
            val health = NativeWorkoutHealthSource.snapshot(document, source)
            val current = health != null && NativeWorkoutHealth.matchesWindow(health, workout)
            val selected = source == provider
            JournalHealthSourceCard(workout.id, tag, NativeWorkoutHealthSource.label(source), "journal_provider_$source", text,
                enabled && (!refreshing || selected), onOpen = { selectProvider(source) }) {
                Text("${text.getString(R.string.journal_health_window)}: ${time(workout.started)} – ${time(workout.finished)}")
                if (!current) Text(text.getString(if (health == null) R.string.journal_no_data else R.string.journal_health_stale),
                    color = colors.muted, modifier = Modifier.testTag("journal_${tag}_health_status"))
                else {
                    val data = health!!
                    val metrics = NativeWorkoutHealth.metrics(data, false)
                    JournalHealthMetrics(metrics, tag, text, number)
                    if (metrics.isEmpty()) Text(text.getString(R.string.journal_health_empty), color = colors.muted)
                    if (data.optBoolean("partial") || (data.optJSONArray("warnings")?.length() ?: 0) > 0)
                        Text(text.getString(R.string.journal_health_partial), color = colors.muted)
                    if (data.optBoolean("workoutEnergyProrated")) Text(text.getString(R.string.journal_health_prorated), color = colors.muted)
                    val sources = data.optJSONArray("sources")
                    val labels = data.optJSONObject("sourceLabels")
                    val names = (0 until (sources?.length() ?: 0)).map { sources!!.optString(it) }
                        .filter { it.isNotBlank() }.map { labels?.optString(it)?.takeIf(String::isNotBlank) ?: it }.distinct()
                    if (names.isNotEmpty()) Text("${text.getString(R.string.journal_health_sources)}: ${names.joinToString(", ")}", color = colors.muted)
                    Text("${text.getString(R.string.journal_health_synced)}: ${time(data.optString("syncedAt"))}", color = colors.muted)
                }
                OutlinedButton(onClick = { selectProvider(source); refresh() }, enabled = enabled && !refreshing,
                    modifier = Modifier.fillMaxWidth().testTag("journal_${tag}_health_refresh")) {
                    Text(if (refreshing && selected) text.getString(R.string.journal_health_refreshing)
                        else "${NativeWorkoutHealthSource.label(source)} • ${text.getString(R.string.journal_health_read)}")
                }
                health?.optJSONObject("errors")?.let { errors ->
                    val codes = errors.keys().asSequence().map { errors.optString(it) }.filter(String::isNotBlank).distinct().toList()
                    if (codes.isNotEmpty()) Text("${NativeWorkoutHealthSource.label(source)}: ${codes.joinToString()}", color = colors.dangerText)
                }
                if (selected) {
                    status?.let { Text(it, color = colors.muted, modifier = Modifier.testTag("journal_${tag}_health_result")) }
                    error?.let { Text(text.getString(it), color = colors.dangerText, modifier = Modifier.testTag("journal_${tag}_health_error")) }
                    permissions?.invoke(source)
                }
            }
        }
    }
}

@Composable
private fun JournalHealthSourceCard(workoutId: String, source: String, title: String, tag: String,
    text: android.content.res.Resources, enabled: Boolean, onOpen: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit) {
    var expanded by rememberSaveable(workoutId, source) { mutableStateOf(false) }
    val colors = LocalTrainPilotColors.current
    Card(colors = CardDefaults.cardColors(containerColor = colors.card), modifier = Modifier.fillMaxWidth()
        .border(1.25.dp, colors.emphasisBorder, MaterialTheme.shapes.medium).testTag("journal_health_$source")) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .clickable(enabled = enabled, role = Role.Button) {
                if (!expanded) onOpen()
                expanded = !expanded
            }.semantics { stateDescription = text.getString(if (expanded) R.string.journal_source_expanded else R.string.journal_source_collapsed) }
            .testTag(tag).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = colors.text, modifier = Modifier.weight(1f))
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = colors.accent)
        }
        if (expanded) Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
            .testTag("journal_${source}_content"), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

private fun parseHealth(raw: String?): JSONObject? = raw?.let { runCatching { JSONObject(it) }.getOrNull() }

@Composable
internal fun JournalHealthMetrics(metrics: Map<String, Double>, source: String, text: android.content.res.Resources, number: NumberFormat) {
    val colors = LocalTrainPilotColors.current
    for ((key, rawValue) in metrics) {
        val label = when (key) {
            "averageHeartRate" -> R.string.health_metric_averageheartrate
            "minHeartRate" -> R.string.health_metric_minheartrate
            "maxHeartRate" -> R.string.health_metric_maxheartrate
            "heartRateSamples" -> R.string.health_metric_heartratesamples
            "steps" -> R.string.health_metric_steps
            "activeCalories" -> R.string.health_metric_activecalories
            "totalCalories" -> R.string.health_metric_totalcalories
            "workoutCalories" -> R.string.journal_health_workout_energy
            "distanceMeters" -> R.string.journal_health_distance
            "activeDurationSeconds" -> R.string.journal_health_active_duration
            "exerciseMinutes" -> R.string.journal_health_exercise_minutes
            "exerciseSessionCount" -> R.string.journal_health_exercise_count
            "averageSpeedMps" -> R.string.journal_health_average_speed
            "maxSpeedMps" -> R.string.journal_health_max_speed
            else -> continue
        }
        val unit = when (key) {
            "averageHeartRate", "minHeartRate", "maxHeartRate" -> "bpm"
            "activeCalories", "totalCalories", "workoutCalories" -> "kcal"
            "distanceMeters" -> "m"
            "activeDurationSeconds" -> "s"
            "exerciseMinutes" -> "min"
            "averageSpeedMps", "maxSpeedMps" -> "km/h"
            else -> ""
        }
        val value = if (key in setOf("averageSpeedMps", "maxSpeedMps")) rawValue * 3.6 else rawValue
        if (!value.isFinite()) continue
        Column(Modifier.fillMaxWidth().testTag("journal_${source}_$key")) {
            Text(text.getString(label), style = MaterialTheme.typography.labelMedium, color = colors.muted)
            Text("${number.format(value)} $unit".trim(), modifier = Modifier.testTag("journal_${source}_${key}_value"))
        }
    }
}
