package com.example.ui.screens
import com.example.ui.theme.glassStateTile
import com.example.ui.theme.glassTile

import com.repforge.app.ui.theme.TrainPilotFrames

import com.repforge.app.ui.theme.trainPilotClickable
import com.repforge.app.ui.theme.trainPilotBorder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.example.ui.components.trainPilotChartMarkColor
import com.example.ui.components.trainPilotChartSelection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState

/** 2785: horizontal positions (0..1) of pulse readings on their own time axis. */
internal fun pulseTimeFractions(readings: List<com.repforge.app.NativeDailyHealthDetails.PulsePoint>): List<Float> {
    val times = readings.map { runCatching { java.time.Instant.parse(it.timeIso).toEpochMilli() }.getOrDefault(0L) }
    val first = times.firstOrNull() ?: return emptyList()
    val delta = ((times.lastOrNull() ?: first) - first).coerceAtLeast(1L).toFloat()
    return times.map { ((it - first) / delta).coerceIn(0f, 1f) }
}

/** Local clock time of a reading ("14:32"). */
internal fun pulseClockLabel(timeIso: String, zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): String =
    runCatching { java.time.Instant.parse(timeIso).atZone(zone).toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")) }
        .getOrDefault("—")

/** 2785: "nov. 18. · 8 432 lépés"; a missing day says "nincs adat" (never 0). */
internal fun todayTileWeekLabel(key: String, date: java.time.LocalDate, value: Double?, lang: String): String {
    val l = lang.take(2)
    val locale = java.util.Locale.forLanguageTag(l)
    val day = com.example.ui.components.chartDateLabel(date, l)
    // 2788: the workout journal is complete, so a day without a workout is "no workout", not "no data".
    if (value == null && key == "workout") return "$day · " + when (l) { "en" -> "no workout"; "de" -> "kein Training"; "ro" -> "fără antrenament"; else -> "nincs edzés" }
    if (value == null) return "$day · " + when (l) { "en" -> "no data"; "de" -> "keine Daten"; "ro" -> "fără date"; else -> "nincs adat" }
    val number = java.text.NumberFormat.getIntegerInstance(locale).format(Math.round(value))
    val unit = when (key) {
        "steps" -> when (l) { "en" -> "steps"; "de" -> "Schritte"; "ro" -> "pași"; else -> "lépés" }
        "calories" -> "kcal"
        "pulse" -> "bpm"
        else -> when (l) { "en" -> "min"; "de" -> "Min."; "ro" -> "min"; else -> "perc" }
    }
    return "$day · $number $unit"
}

/**
 * 2788: one calorie day as text. Total = resting + active (measured by the provider); the two are never
 * added together. "nincs adat" for what was not measured; no estimate.
 */
internal fun calorieDayLabel(date: java.time.LocalDate, total: Int?, active: Int?, lang: String, sameSource: Boolean = true): String {
    val l = lang.take(2)
    val day = com.example.ui.components.chartDateLabel(date, l)
    val nf = java.text.NumberFormat.getIntegerInstance(java.util.Locale.forLanguageTag(l))
    val noData = when (l) { "en" -> "no data"; "de" -> "keine Daten"; "ro" -> "fără date"; else -> "nincs adat" }
    val totalWord = when (l) { "en" -> "total"; "de" -> "gesamt"; "ro" -> "total"; else -> "teljes" }
    val activeWord = when (l) { "en" -> "active"; "de" -> "aktiv"; "ro" -> "activ"; else -> "aktív" }
    val ofWhich = when (l) { "en" -> "of which active"; "de" -> "davon aktiv"; "ro" -> "din care activ"; else -> "ebből aktív" }
    return when {
        total != null && active != null -> "$day · $totalWord ${nf.format(total)} kcal · ${if (sameSource) ofWhich else activeWord} ${nf.format(active)}"
        total != null -> "$day · $totalWord ${nf.format(total)} kcal · $activeWord: $noData"
        active != null -> "$day · $activeWord ${nf.format(active)} kcal · $totalWord: $noData"
        else -> "$day · $noData"
    }
}

/**
 * 2784/2788: extras of a Today tile sheet. The sheet is about TODAY: steps and workouts link to the 7-day
 * history in Weekly goals (no second copy of that chart here); calories show total vs active per day;
 * pulse keeps its 7-day averages. Sleep and calories can show the day's raw records.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TodayTileSheetExtras(key: String, uiState: TrainPilotUiState, today: java.time.LocalDate, onOpenWeekly: () -> Unit) {
    val palette = LocalTrainPilotPalette.current
    val lang = uiState.language
    val days = (6 downTo 0).map { today.minusDays(it.toLong()).toString() }
    val weekly = (uiState.weeklyHealthDays + uiState.dailyActivityDays).associateBy { it.dateIso }
    val daily = weekly[today.toString()]
    if (key == "steps") com.example.ui.components.StudioHourlyStepsChart(
        day = today,
        provider = daily?.stepProvider ?: if (uiState.primaryHealthSourceSamsung) com.repforge.app.HealthJournalStore.SAMSUNG
            else com.repforge.app.HealthJournalStore.HC,
        origin = daily?.stepOriginPackage.orEmpty(),
        revision = uiState.healthRefreshRevision,
        language = lang
    )
    if (key == "steps" || key == "workout") {
        Surface(shape = RoundedCornerShape(10.dp), color = palette.glassTile(palette.surfaceCard),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                .trainPilotBorder(1.dp, palette.primary.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                .trainPilotClickable(RoundedCornerShape(10.dp)) { onOpenWeekly() }.testTag("health_today_tile_open_weekly_$key")) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (key == "steps") tr(lang, "Heti lépések és előzmények", "Weekly steps and history", "Wochenschritte und Verlauf", "Pașii săptămânii și istoric")
                    else tr(lang, "Heti edzések és előzmények", "Weekly workouts and history", "Wochentrainings und Verlauf", "Antrenamentele săptămânii și istoric"),
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = palette.primary, modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = palette.primary, modifier = Modifier.size(16.dp))
            }
        }
    } else if (key == "calories") {
        val totals = days.map { d -> weekly[d]?.totalCaloriesKcal }
        val actives = days.map { d -> weekly[d]?.activeCaloriesMeasured }
        val sameSources = days.map { d -> weekly[d]?.let { it.activeCaloriesProvider == it.totalCaloriesProvider } ?: true }
        if (totals.any { it != null } || actives.any { it != null }) {
            Text(tr(lang, "Napi kalóriaégetés · 7 nap", "Daily calories burned · 7 days", "Täglicher Kalorienverbrauch · 7 Tage", "Calorii arse zilnic · 7 zile"),
                style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
            val max = days.indices.maxOf { (totals[it] ?: actives[it] ?: 0) }.coerceAtLeast(1).toFloat()
            var sel by remember(key, today) { androidx.compose.runtime.mutableIntStateOf(days.lastIndex) }
            val neutral = trainPilotChartMarkColor(false, palette.primary, palette.textSecondary)
            val resting = neutral.copy(alpha = 0.36f)
            val unsplit = neutral.copy(alpha = 0.55f)
            val langCode = lang.code.lowercase()
            val shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
            Row(
                Modifier.fillMaxWidth().height(72.dp).testTag("health_today_tile_week_calories"),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                days.indices.forEach { i ->
                    val selected = i == sel
                    val total = totals[i]
                    val active = actives[i]
                    val color = trainPilotChartMarkColor(selected, palette.primary, palette.textSecondary)
                    val label = calorieDayLabel(java.time.LocalDate.parse(days[i]), total, active, langCode, sameSources[i])
                    Box(
                        Modifier.weight(1f).fillMaxHeight()
                            .trainPilotClickable(RoundedCornerShape(6.dp)) { sel = i }
                            .trainPilotChartSelection(selected, label)
                            .testTag("health_today_tile_week_calories_$i"),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        if (sameSources[i] && total != null && active != null && total > 0 && active in 0..total) {
                            Column(Modifier.fillMaxWidth().fillMaxHeight((total / max).coerceIn(0.05f, 1f)).clip(shape)) {
                                val activeShare = active.toFloat() / total
                                if (activeShare < 1f) Box(
                                    Modifier.fillMaxWidth().weight(1f - activeShare)
                                        .background(if (selected) color.copy(alpha = 0.42f) else resting)
                                )
                                if (activeShare > 0f) Box(Modifier.fillMaxWidth().weight(activeShare).background(color))
                            }
                        } else if (total != null) {
                            Box(Modifier.fillMaxWidth().fillMaxHeight((total / max).coerceIn(0.05f, 1f))
                                .background(if (selected) color.copy(alpha = 0.65f) else unsplit, shape))
                        } else if (active != null) {
                            Box(Modifier.fillMaxWidth().fillMaxHeight((active / max).coerceIn(0.05f, 1f)).background(color, shape))
                        } else {
                            Box(Modifier.fillMaxWidth().height(2.dp).background(color))
                        }
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CalorieChartLegend(neutral, tr(lang, "aktív (mozgás)", "active (movement)", "aktiv (Bewegung)", "activ (mișcare)"))
                CalorieChartLegend(resting, tr(lang, "nyugalmi", "resting", "Ruhe", "repaus"))
                if (days.indices.any { i ->
                        val total = totals[i]
                        total != null && (!sameSources[i] || total <= 0 || actives[i]?.let { it in 0..total } != true)
                    }) {
                    CalorieChartLegend(unsplit, tr(lang, "teljes (bontás nélkül)", "total (no breakdown)",
                        "gesamt (ohne Aufteilung)", "total (fără defalcare)"))
                }
            }
            val summaryShape = RoundedCornerShape(10.dp)
            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = summaryShape,
                color = palette.glassStateTile(palette.primary, palette.primary.copy(alpha = 0.12f)),
                border = TrainPilotFrames.control(palette.primary, selected = true)
            ) {
                Text(
                    text = calorieDayLabel(java.time.LocalDate.parse(days[sel]), totals[sel], actives[sel], langCode, sameSources[sel]),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).testTag("health_today_tile_week_info_calories"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textPrimary
                )
            }
        }
    } else if (key == "pulse") {
        val values = days.map { d -> weekly[d]?.pulse?.averageBpm }
        if (values.any { it != null }) {
            Text(tr(lang, "Utolsó 7 nap", "Last 7 days", "Letzte 7 Tage", "Ultimele 7 zile"), style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
            com.example.ui.components.WeeklyDayBars(days.map { java.time.LocalDate.parse(it) }, values,
                label = { date, v -> todayTileWeekLabel("pulse", date, v, lang.name.lowercase()) }, tag = "health_today_tile_week_pulse",
                lang = lang.name.lowercase())
        }
    }
    if (key != "sleep") return
    val context = androidx.compose.ui.platform.LocalContext.current.applicationContext
    var energyType by remember(today) { androidx.compose.runtime.mutableStateOf("ActiveCaloriesBurnedRecord") }
    val type = if (key == "sleep") "SleepSessionRecord" else energyType
    val todayReadings = weekly[today.toString()]
    val provider = when (type) {
        "SleepSessionRecord" -> todayReadings?.sleepProvider
        "TotalCaloriesBurnedRecord" -> todayReadings?.totalCaloriesProvider
        else -> todayReadings?.activeCaloriesProvider
    } ?: com.repforge.app.HealthJournalStore.HC
    val snapshot by androidx.compose.runtime.produceState<org.json.JSONObject?>(null, today, provider, uiState.healthRefreshRevision) {
        value = null
        try {
            value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.repforge.app.ui.health.NativeHealthJournal.load(com.repforge.app.HealthJournalStore.get(context),
                    provider, "", com.repforge.app.ui.health.HealthJournalRange(today,today))
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { value = null }
    }
    val data = snapshot ?: return
    val g = data.getLong("generation")
    if (key == "calories") {
        Text(com.repforge.app.NativeWorkoutHealthSource.label(provider), style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
        val filters = listOf(
            "ActiveCaloriesBurnedRecord" to tr(lang,"Aktív kcal","Active kcal","Aktiv kcal","Kcal active"),
            "TotalCaloriesBurnedRecord" to tr(lang,"Teljes kcal","Total kcal","Gesamt kcal","Kcal totale"))
        if (palette.isLiquidGlass) {
            com.example.ui.components.LiquidSegmentedTabs(filters.map { it.second }, filters.indexOfFirst { it.first == type },
                { energyType = filters[it].first }, "today_energy_filter_track",
                itemTag = { "today_energy_filter_${filters[it].first}" }) { _, label, _ -> Text(label) }
        } else FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for ((filter, label) in filters) {
                com.example.ui.components.TrainPilotFilterChip(selected = type == filter, onClick = { energyType = filter },
                    label = { Text(label) }, modifier = Modifier.testTag("today_energy_filter_$filter"))
            }
        }
    }
    // Same persisted provider/day projection as the health journal, no additional SDK read.
    com.repforge.app.ui.health.HealthRecordDayDetails(today, provider, "", type, lang.name.lowercase(), g,
        "${uiState.healthRefreshRevision}", visible = true,
        title = tr(lang, "Mai rekordok", "Today's records", "Heutige Einträge", "Înregistrările de azi"), hideWhenEmpty = true,
        dailySummary = com.repforge.app.ui.health.NativeHealthJournal.day(data,provider,"",today))
}


@Composable
internal fun CalorieChartLegend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(9.dp).background(color, RoundedCornerShape(2.dp)))
        Text(label, style = MaterialTheme.typography.labelSmall, color = LocalTrainPilotPalette.current.textSecondary)
    }
}
