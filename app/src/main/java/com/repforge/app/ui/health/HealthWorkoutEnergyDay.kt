package com.repforge.app.ui.health

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.repforge.app.*
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.db.WorkoutHistoryEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.text.NumberFormat
import java.util.Locale

internal data class WorkoutDayEnergy(val id: String, val name: String, val start: Instant, val end: Instant,
    val active: Double?, val total: Double?, val partial: Boolean,
    val activeCoverage: WorkoutEnergyCoverage.Status, val totalCoverage: WorkoutEnergyCoverage.Status)

/** Exact provider snapshots only; no daily aggregate, synthetic sensor records or cross-source sum. */
internal fun workoutDayEnergy(row: WorkoutHistoryEntity, provider: String): WorkoutDayEnergy? {
    if (row.originDevice !in setOf("phone", "watch") || provider !in NativeWorkoutHealthSource.providers) return null
    return runCatching {
        val document = row.health240Json?.let { JSONObject(it) } ?: return null
        val data = NativeWorkoutHealthSource.snapshot(document, provider) ?: return null
        if (!NativeWorkoutHealth.matchesWindow(data, row)) return null
        val result = NativeWorkoutHealthSummary.readProvider(row,provider)
        val active = result.activeEnergy.values["activeCalories"]
        val total = result.totalEnergy.values["totalCalories"]
        if (active == null && total == null) return null
        WorkoutDayEnergy(row.id, row.programName, Instant.parse(row.started), Instant.parse(row.finished),
            active, total, data.optBoolean("partial") || result.rejectedEvidence ||
                (active != null && result.activeEnergy.incomplete) || (total != null && result.totalEnergy.incomplete),
            WorkoutEnergyCoverage.aggregateStatus(data,"activeCalories"), WorkoutEnergyCoverage.aggregateStatus(data,"totalCalories"))
    }.getOrNull()
}

@Composable
internal fun HealthWorkoutEnergyDay(day: LocalDate, provider: String, type: String, language: String,
    generation: Long, refreshKey: String) {
    val context = LocalContext.current
    val zone = remember(day) { ZoneId.systemDefault() }
    var rows by remember(day, provider, type, generation, refreshKey) { mutableStateOf(emptyList<WorkoutDayEnergy>()) }
    LaunchedEffect(day, provider, type, generation, refreshKey, zone) {
        if (provider !in NativeWorkoutHealthSource.providers) return@LaunchedEffect
        val feature = if (provider == HealthJournalStore.HC) NativePrivacyControls.HEALTH_CONNECT else NativePrivacyControls.SAMSUNG_HEALTH
        try {
            val ticket = NativeDataReplacement.continuationForCurrentData(context)
            val dao = withContext(Dispatchers.IO) { TrainPilotDatabase.getInstance(context).workoutHistoryDao() }
            dao.observeNativeWorkoutHealthDay(day.atStartOfDay(zone).toInstant().toEpochMilli(),
                day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()).collectLatest { saved ->
                val measured = withContext(Dispatchers.IO) {
                    ticket.requireCurrent(context)
                    if (!NativePrivacyControls.enabled(context, feature) ||
                        HealthJournalStore.get(context).generation() != generation) emptyList()
                    else saved.mapNotNull { workoutDayEnergy(it, provider) }
                        .filter { when (type) {
                            "ActiveCaloriesBurnedRecord" -> it.active != null
                            "TotalCaloriesBurnedRecord" -> it.total != null
                            else -> true
                        } }
                }
                ticket.requireCurrent(context)
                rows = measured
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { rows = emptyList() }
    }
    if (rows.isEmpty()) return
    fun word(hu: String, en: String, de: String, ro: String) = when (language.substringBefore('-')) {
        "hu" -> hu; "de" -> de; "ro" -> ro; else -> en
    }
    val number = remember(language) { NumberFormat.getNumberInstance(Locale.forLanguageTag(language)).apply { maximumFractionDigits = 1 } }
    val time = remember(zone) { DateTimeFormatter.ofPattern("MM.dd. HH:mm").withZone(zone) }
    Column(Modifier.fillMaxWidth().testTag("health_workout_energy_day_$day"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(word("Mentett edzések mért energiája (nem napi összeg)", "Saved workouts: measured energy (not daily total)",
            "Gespeicherte Trainingsenergie (keine Tagessumme)", "Energie măsurată la antrenamente (nu total zilnic)"),
            style = MaterialTheme.typography.labelMedium)
        fun coverageLabel(status: WorkoutEnergyCoverage.Status): String = when(status) {
            WorkoutEnergyCoverage.Status.PARTIAL_RAW -> word(" · hiányos rekordlefedettség", " · incomplete record coverage", " · lückenhafte Datensätze", " · acoperire parțială")
            WorkoutEnergyCoverage.Status.UNKNOWN -> word(" · lefedettség nem igazolt", " · coverage unverified", " · Abdeckung ungeprüft", " · acoperire neverificată")
            WorkoutEnergyCoverage.Status.MISSING -> word(" · nincs mérés", " · no measurement", " · keine Messung", " · fără măsurare")
            else -> ""
        }
        for (row in rows) {
            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("health_workout_energy_${row.id}")) {
                Text(row.name.ifBlank { word("Edzés", "Workout", "Training", "Antrenament") })
                Text("${time.format(row.start)} – ${time.format(row.end)} · ${NativeWorkoutHealthSource.label(provider)}",
                    style = MaterialTheme.typography.labelSmall)
                if (type != "TotalCaloriesBurnedRecord") row.active?.let {
                    Text("${word("Aktív", "Active", "Aktiv", "Activ")}: ${number.format(it)} kcal${coverageLabel(row.activeCoverage)}")
                }
                if (type != "ActiveCaloriesBurnedRecord") row.total?.let {
                    Text("${word("Teljes", "Total", "Gesamt", "Total")}: ${number.format(it)} kcal${coverageLabel(row.totalCoverage)}")
                }
                if (row.partial) Text(word("Részleges mérés", "Partial measurement", "Teilweise Messung", "Măsurare parțială"),
                    style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
