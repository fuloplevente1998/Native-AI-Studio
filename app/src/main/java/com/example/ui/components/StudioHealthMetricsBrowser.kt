package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.CalendarMonth
import com.repforge.app.ui.theme.trainPilotBorder
import com.repforge.app.ui.theme.trainPilotClickable
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import com.repforge.app.ui.theme.TrainPilotTextButton as TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalTrainPilotPalette
import com.repforge.app.HealthJournalStore
import com.repforge.app.NativeDataReplacement
import com.repforge.app.NativeWorkoutHealthSource
import com.repforge.app.ui.health.*
import com.repforge.app.ui.theme.TrainPilotFrames
import com.repforge.app.ui.theme.TrainPilotOutlinedButton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.LocalDate
import java.util.Locale

/** One read-only, paged journal. A day header and its details share one admitted projection. */
@Composable
internal fun StudioHealthMetricsBrowser(
    initialProvider: String,
    language: String,
    refreshKey: String,
    onRefreshProvider: ((String) -> Unit)? = null,
    refreshBusy: Boolean = false,
    refreshError: String? = null
) {
    val context = LocalContext.current.applicationContext
    val palette = LocalTrainPilotPalette.current
    fun label(hu: String, en: String, de: String = en, ro: String = en) = when (language.lowercase(Locale.ROOT)) {
        "en" -> en; "de" -> de; "ro" -> ro; else -> hu
    }
    var provider by rememberSaveable(initialProvider) { mutableStateOf(initialProvider) }
    // Provider projection is authoritative; origins remain record metadata, never another filter.
    val source = ""
    var from by rememberSaveable { mutableStateOf(LocalDate.now().minusDays(29).toString()) }
    var to by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var end by rememberSaveable { mutableStateOf(to) }
    // 2772: one filter button opens the same period sheet as the Progress log.
    var showCalendar by rememberSaveable { mutableStateOf(false) }
    var pickStart by rememberSaveable { mutableStateOf<String?>(null) }
    var pickEnd by rememberSaveable { mutableStateOf<String?>(null) }
    var calendarMonth by rememberSaveable { mutableStateOf(java.time.YearMonth.from(LocalDate.parse(to)).toString()) }
    var reload by rememberSaveable { mutableIntStateOf(0) }
    val range = HealthJournalRange(LocalDate.parse(from), LocalDate.parse(to), LocalDate.parse(end))
    val revision = "$refreshKey:$reload"
    var snapshotJson by remember(provider, source, from, to, end, revision) { mutableStateOf<String?>(null) }
    var failed by remember(provider, source, from, to, end, revision) { mutableStateOf(false) }
    val snapshot = remember(snapshotJson) { snapshotJson?.let(::JSONObject) }
    LaunchedEffect(provider, source, range, revision) {
        if (snapshotJson != null) return@LaunchedEffect
        try {
            val ticket = NativeDataReplacement.continuationForCurrentData(context)
            val result = withContext(Dispatchers.IO) {
                NativeDataReplacement.mutate(context) { NativeHealthJournal.load(HealthJournalStore.get(context), provider, source, range) }
            }
            ticket.requireCurrent(context)
            snapshotJson = result.toString()
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { failed = true }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(HealthJournalStore.SAMSUNG, HealthJournalStore.HC).forEach { selected ->
            FilterChip(selected = provider == selected, onClick = { if (provider != selected) provider = selected },
                colors = FilterChipDefaults.filterChipColors(containerColor = palette.surfaceCard,
                    labelColor = palette.textPrimary, selectedContainerColor = palette.primary,
                    selectedLabelColor = palette.onPrimary),
                border = TrainPilotFrames.stroke(1.dp, if (provider == selected) palette.primary else palette.cardBorder),
                label = { Text(NativeWorkoutHealthSource.label(selected)) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    .testTag("journal_provider_$selected"))
        }
    }
    val periodFormat = java.time.format.DateTimeFormatter.ofPattern("MMM d.", Locale.forLanguageTag(language))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        val pillShape = RoundedCornerShape(14.dp)
        Surface(shape = pillShape, color = palette.surfaceCard, modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            .trainPilotBorder(1.dp, palette.primary.copy(alpha = 0.5f), pillShape)
            .trainPilotClickable(pillShape) {
                pickStart = from; pickEnd = to
                calendarMonth = java.time.YearMonth.from(LocalDate.parse(to)).toString()
                showCalendar = true
            }.testTag("journal_period_button")) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.CalendarMonth, null, tint = palette.primary, modifier = Modifier.size(18.dp))
                Text("${LocalDate.parse(from).format(periodFormat)} – ${LocalDate.parse(to).format(periodFormat)}",
                    color = palette.textPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(if (showCalendar) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = palette.primary)
            }
        }
        IconButton(onClick = {
            if (onRefreshProvider != null) onRefreshProvider(provider) else reload++
        }, enabled = !refreshBusy && (snapshot != null || failed), modifier = Modifier.testTag("journal_reload")) {
            if (refreshBusy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Icon(Icons.Default.Refresh,
                contentDescription = label("Szolgáltató frissítése", "Refresh provider", "Anbieter aktualisieren", "Actualizează furnizorul"),
                tint = palette.primary)
        }
    }
    if (!refreshError.isNullOrBlank()) Text(refreshError, color = palette.danger, modifier = Modifier.testTag("journal_refresh_error"))
    when {
        failed -> Text(label("Az adatok nem tölthetők be. Próbáld újra.", "Could not load data. Try again.",
            "Daten konnten nicht geladen werden. Bitte erneut versuchen.", "Datele nu au putut fi încărcate. Încearcă din nou."), color = palette.danger)
        snapshot == null -> LinearProgressIndicator(Modifier.fillMaxWidth())
        else -> range.days.forEach { date -> key(date) {
            val raw = NativeHealthJournal.day(snapshot, provider, source, date)
            val metrics = NativeHealthMetrics.parse(context, NativeDetailedHealthMetrics.admitted(raw, provider, date), language)
            var expanded by rememberSaveable(provider, source, date) { mutableStateOf(false) }
            var type by rememberSaveable(provider, source, date) { mutableStateOf("") }
            Surface(shape = RoundedCornerShape(14.dp), color = palette.surfaceElevated,
                border = TrainPilotFrames.stroke(1.dp, palette.cardBorder), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("journal_day_$date")) {
                        Column(Modifier.weight(1f)) {
                            Text(date.toString(), fontWeight = FontWeight.Bold, color = palette.textPrimary)
                            Text(NativeWorkoutHealthSource.label(provider), color = palette.primary, style = MaterialTheme.typography.labelSmall)
                            if (source.isNotEmpty()) Text(friendlyHealthSourceName(source, "", NativeWorkoutHealthSource.label(provider)),
                                color = palette.textSecondary, style = MaterialTheme.typography.labelSmall)
                        }
                        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = label("Napi részletek", "Day details", "Tagesdetails", "Detalii zilnice"), tint = palette.primary)
                    }
                    val sleep = NativeHealthJournal.sleepMinutes(raw, provider, date)
                    val summary = listOf(Triple("sleep", label("Alvás", "Sleep", "Schlaf", "Somn"), sleep?.let { "${it.toInt()} min" } ?: "—")) +
                        listOf("steps", "activeCalories", "averageHeartRate").map { key ->
                            val metric = metrics.first { it.key == key }
                            Triple(key, metric.label, if (metric.isAvailable) "${metric.valueText} ${metric.unit}".trim() else "—")
                        }
                    // Two columns keep the existing compact boxed journal readable with larger text.
                    summary.chunked(2).forEach { cells ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            cells.forEach { (metricKey, title, value) ->
                                Surface(shape = RoundedCornerShape(10.dp), color = palette.surfaceCard, modifier = Modifier.weight(1f)) {
                                    Column(Modifier.padding(8.dp)) {
                                        Text(title, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                        Text(value, color = palette.textPrimary, fontWeight = FontWeight.Bold,
                                            modifier = Modifier.testTag("journal_summary_${date}_$metricKey"))
                                    }
                                }
                            }
                        }
                    }
                    if (expanded) {
                        Text(label("Elérhető napi mérések", "Available daily measurements",
                            "Verfügbare Tagesmessungen", "Măsurători zilnice disponibile"), color = palette.primary)
                        if (raw?.optBoolean("partial") == true) Text(label("Részleges adat / újraolvasás szükséges", "Partial data / reload required",
                            "Teilweise Daten / neu laden erforderlich", "Date parțiale / reîncărcare necesară"), color = palette.textSecondary)
                        metrics.filter { metric -> metric.isAvailable &&
                            NativeDetailedHealthMetrics.status(raw, metric.key, provider, date) == NativeDetailedHealthMetrics.Status.AVAILABLE
                        }.forEach { metric ->
                            HorizontalDivider(color = palette.cardBorder)
                            TextButton(onClick = { type = NativeDetailedHealthMetrics.recordType(metric.key) },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("journal_metric_${date}_${metric.key}")) {
                                Column(Modifier.weight(1f)) {
                                    Text(metric.label, color = palette.textPrimary)
                                    val description = listOfNotNull(NativeWorkoutHealthSource.label(provider),
                                        NativeDetailedHealthMetrics.origin(raw, metric.key),
                                        NativeDetailedHealthMetrics.timestamp(raw, metric.key), date.toString()).distinct().joinToString(" · ")
                                    Text(description, color = palette.textSecondary, style = MaterialTheme.typography.labelSmall)
                                }
                                Text(if (metric.isAvailable) "${metric.valueText} ${metric.unit}".trim() else "—", color = palette.primary)
                            }
                        }
                        HorizontalDivider(color = palette.cardBorder)
                        TextButton(onClick = { type = "" }, modifier = Modifier.testTag("journal_records_all_$date")) {
                            Text(label("Összes napi rekord / alvásfázisok", "All day records / sleep stages", "Alle Tageseinträge / Schlafphasen", "Toate înregistrările zilei / faze de somn"))
                        }
                        if (type.isNotEmpty()) Text(com.repforge.app.ui.text.nativeResources(context, language).getString(NativeHealthRecords.title(type)), color = palette.primary)
                    }
                    StudioNativeEditorTheme {
                        HealthRecordDayDetails(date, provider, source, type, language, snapshot.getLong("generation"),
                            "$revision:${snapshot.getLong("generation")}", expanded)
                    }
                }
            }
        } }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = { end = range.newer().pageEnd.toString() }, enabled = range.pageEnd < range.to,
            modifier = Modifier.weight(1f).testTag("journal_newer")) { Text(label("Újabb napok", "Newer days", "Neuere Tage", "Zile mai noi")) }
        TextButton(onClick = { end = range.older().pageEnd.toString() }, enabled = range.pageStart > range.from,
            modifier = Modifier.weight(1f).testTag("journal_older")) { Text(label("Korábbi napok", "Older days", "Ältere Tage", "Zile mai vechi")) }
    }
    if (showCalendar) {
        fun applyPeriod(a: String, b: String) {
            val lo = minOf(a, b); val hi = maxOf(a, b)
            from = lo; to = hi; end = hi
        }
        val locale = Locale.forLanguageTag(language)
        val month = java.time.YearMonth.parse(calendarMonth)
        val ps = pickStart?.let(LocalDate::parse); val pe = pickEnd?.let(LocalDate::parse)
        TrainPilotRangeCalendarSheet(
            tagPrefix = "journal",
            title = label("Egészségnapló időszak", "Health journal period", "Gesundheitsjournal-Zeitraum", "Perioada jurnalului de sănătate"),
            monthTitle = "${month.year}. ${month.month.getDisplayName(java.time.format.TextStyle.FULL_STANDALONE, locale)}",
            hint = when {
                ps == null -> label("Jelölj ki két napot az időszak megtekintéséhez.", "Select two days to view a date range.", "Wähle zwei Tage, um den Zeitraum anzuzeigen.", "Selectează două zile pentru a vedea intervalul.")
                pe == null -> label("Kezdőnap: ${ps.format(periodFormat)} • Jelöld ki a zárónapot.", "From: ${ps.format(periodFormat)} • Select the end day.", "Ab: ${ps.format(periodFormat)} • Wähle den Endtag.", "De la: ${ps.format(periodFormat)} • Selectează ziua finală.")
                else -> "${minOf(ps, pe).format(periodFormat)} – ${maxOf(ps, pe).format(periodFormat)}"
            },
            weekdayHeaders = (1..7).map { java.time.DayOfWeek.of(it).getDisplayName(java.time.format.TextStyle.SHORT, locale) },
            month = month,
            start = ps,
            end = pe,
            today = LocalDate.now(),
            hasMark = { false },
            todayLabel = label("Mai nap", "Today", "Heute", "Azi"),
            clearLabel = label("Szűrő törlése", "Clear filter", "Filter löschen", "Șterge filtrul"),
            doneLabel = label("Rendben", "OK", "OK", "OK"),
            onMonthChange = { calendarMonth = it.toString() },
            onDayClick = { day ->
                val d = day.toString()
                if (pickStart == null || pickEnd != null) { pickStart = d; pickEnd = null }
                else { pickEnd = d; applyPeriod(pickStart!!, d) }
            },
            onToday = {
                val t = LocalDate.now().toString()
                pickStart = t; pickEnd = t; calendarMonth = java.time.YearMonth.now().toString(); applyPeriod(t, t)
            },
            onClear = {
                val a = LocalDate.now().minusDays(29).toString(); val b = LocalDate.now().toString()
                pickStart = a; pickEnd = b; calendarMonth = java.time.YearMonth.now().toString(); applyPeriod(a, b)
            },
            onDismiss = {
                // A single tapped day means that day only.
                if (pickStart != null && pickEnd == null) applyPeriod(pickStart!!, pickStart!!)
                showCalendar = false
            }
        )
    }
}
