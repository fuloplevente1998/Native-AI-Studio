package com.repforge.app.ui.health

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.repforge.app.HealthJournalStore
import org.json.JSONObject
import java.text.NumberFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

internal data class SamsungHourlyEnergy(val start: Instant, val end: Instant, val active: Double?, val total: Double?)

/** SDK hourly aggregates are not raw sensor observations. Never derive them from a daily total. */
internal fun samsungHourlyEnergy(raw: JSONObject?, day: LocalDate, provider: String, source: String, type: String,
    zone: ZoneId = ZoneId.systemDefault(), now: Instant = Instant.now()): List<SamsungHourlyEnergy> {
    if (raw == null || provider != HealthJournalStore.SAMSUNG || source.isNotBlank() ||
        raw.optString("channel") != provider || raw.optString("day") != day.toString() ||
        raw.optString("source") != "@aggregate" || type !in setOf("", "ActiveCaloriesBurnedRecord", "TotalCaloriesBurnedRecord"))
        return emptyList()
    val from = day.atStartOfDay(zone).toInstant()
    val until = minOf(day.plusDays(1).atStartOfDay(zone).toInstant(), now)
    val readEnd = runCatching { Instant.parse(raw.getString("end")) }.getOrNull() ?: return emptyList()
    if (readEnd > until || readEnd <= from) return emptyList()
    fun field(key: String, permission: String): Map<Pair<Instant, Instant>, Double> = runCatching {
        if (raw.optJSONObject("permissions")?.optBoolean(permission) != true) return emptyMap()
        val failed = raw.optJSONArray("failedFields")
        if ((0 until (failed?.length() ?: 0)).any { failed?.optString(it) == key } ||
            raw.optJSONObject("errors")?.optString(key).orEmpty().isNotBlank()) return emptyMap()
        val array = raw.optJSONArray(key) ?: return emptyMap()
        require(array.length() <= 25)
        val values = linkedMapOf<Pair<Instant, Instant>, Double>()
        for (i in 0 until array.length()) {
            val row = array.getJSONObject(i)
            val start = Instant.parse(row.getString("start")); val end = Instant.parse(row.getString("end"))
            val kcal = (row.opt("kcal") as? Number)?.toDouble() ?: error("Missing energy")
            require(start >= from && end <= readEnd && end > start && Duration.between(start, end) <= Duration.ofHours(1) &&
                kcal.isFinite() && kcal >= 0 && values.put(start to end, kcal) == null)
        }
        val sorted = values.keys.sortedBy { it.first }
        require(sorted.zipWithNext().none { (a,b) -> a.second > b.first })
        values
    }.getOrDefault(emptyMap())
    val active = if (type == "TotalCaloriesBurnedRecord") emptyMap() else field("activeCaloriesHourly", "READ_ACTIVE_CALORIES_BURNED")
    val total = if (type == "ActiveCaloriesBurnedRecord") emptyMap() else field("totalCaloriesHourly", "READ_TOTAL_CALORIES_BURNED")
    return (active.keys + total.keys).sortedByDescending { it.first }.map {
        SamsungHourlyEnergy(it.first, it.second, active[it], total[it])
    }
}

@Composable
internal fun SamsungHourlyEnergyRows(rows: List<SamsungHourlyEnergy>, language: String) {
    fun label(hu: String, en: String, de: String, ro: String) = when (language.substringBefore('-')) {
        "hu" -> hu; "de" -> de; "ro" -> ro; else -> en
    }
    val number = remember(language) { NumberFormat.getNumberInstance(Locale.forLanguageTag(language)).apply { maximumFractionDigits = 1 } }
    val time = remember { DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()) }
    Column(Modifier.fillMaxWidth().testTag("samsung_hourly_energy"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label("Samsung Health · óránkénti összesítések", "Samsung Health · hourly aggregates",
            "Samsung Health · Stundenaggregate", "Samsung Health · totaluri orare"), style = MaterialTheme.typography.labelMedium)
        rows.forEach { row ->
            val values = listOfNotNull(row.active?.let { "${label("Aktív", "Active", "Aktiv", "Activ")}: ${number.format(it)} kcal" },
                row.total?.let { "${label("Teljes", "Total", "Gesamt", "Total")}: ${number.format(it)} kcal" })
            Text("${time.format(row.start)}–${time.format(row.end)} · ${values.joinToString(" · ")}",
                modifier = Modifier.fillMaxWidth().testTag("samsung_hourly_energy_${row.start}"),
                style = MaterialTheme.typography.bodyMedium)
        }
    }
}
