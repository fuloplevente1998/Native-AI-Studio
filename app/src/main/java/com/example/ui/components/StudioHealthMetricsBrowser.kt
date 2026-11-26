package com.example.ui.components

import com.example.ui.theme.trainPilotGlassRim

import com.example.ui.theme.selectionColors
import com.example.ui.theme.glassStateTile
import com.example.ui.theme.trainPilotGlassMaterial
import com.example.ui.theme.glassTile
import com.example.ui.theme.GlassMaterialRole
import com.example.ui.theme.overlayRoleFill
import com.example.ui.theme.overlayRoleRim
import com.example.ui.theme.LocalGlassOverlay
import com.example.ui.theme.glassCardFill
import com.example.ui.components.trainPilotAnimatedExpansion

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun StudioHealthMetricsBrowser(
    initialProvider: String,
    language: String,
    refreshKey: String,
    onRefreshProvider: ((String) -> Unit)? = null,
    refreshBusy: Boolean = false,
    refreshError: String? = null,
    /** 2783: (day ISO, record type) to open directly, e.g. from a Today tile; [focusKey] repeats it. */
    focus: Pair<String, String>? = null,
    focusKey: Int = 0
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
    LaunchedEffect(focus, focusKey) {
        val day = focus?.first?.let(LocalDate::parse) ?: return@LaunchedEffect
        if (day !in range.days) {
            if (day.isBefore(range.from)) from = day.toString()
            if (day.isAfter(range.to)) to = day.toString()
            end = day.toString()
        }
    }
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
            com.example.ui.components.TrainPilotFilterChip(selected = provider == selected, onClick = { if (provider != selected) provider = selected },
                colors = FilterChipDefaults.filterChipColors(containerColor = palette.glassTile(palette.surfaceCard),
                    labelColor = palette.textPrimary, selectedContainerColor = palette.glassStateTile(palette.primary, palette.primary, strong = true),
                    selectedLabelColor = if (palette.isLiquidGlass) palette.selectionColors().foreground else palette.onPrimary),
                border = com.repforge.app.ui.theme.trainPilotFrameStroke(TrainPilotFrames.stroke(1.dp, if (provider == selected) palette.primary else palette.cardBorder)),
                label = { Text(NativeWorkoutHealthSource.label(selected)) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    .testTag("journal_provider_$selected"))
        }
    }
    val periodFormat = java.time.format.DateTimeFormatter.ofPattern("MMM d.", Locale.forLanguageTag(language))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        val pillShape = RoundedCornerShape(14.dp)
        Surface(shape = pillShape, color = palette.overlayRoleFill(palette.glassTile(palette.surfaceCard), GlassMaterialRole.Button), modifier = Modifier
            .then(if (LocalGlassOverlay.current && palette.isLiquidGlass) Modifier.overlayRoleRim(palette, pillShape, GlassMaterialRole.Button)
                else Modifier.trainPilotGlassRim(palette, pillShape)).weight(1f).heightIn(min = 48.dp)
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
            // 2778: detailed records start folded; the header opens/closes them.
            var recordsOpen by rememberSaveable(provider, source, date) { mutableStateOf(false) }
            androidx.compose.runtime.LaunchedEffect(focusKey) {
                if (focus != null && focus.first == date.toString()) { expanded = true; type = focus.second; recordsOpen = true }
            }
            Surface(shape = RoundedCornerShape(14.dp), color = palette.overlayRoleFill(palette.glassTile(palette.surfaceElevated, elevated = true), GlassMaterialRole.Tile),
                border = com.repforge.app.ui.theme.trainPilotFrameStroke(TrainPilotFrames.stroke(1.dp, palette.cardBorder)), modifier = Modifier.fillMaxWidth().overlayRoleRim(palette, RoundedCornerShape(14.dp), GlassMaterialRole.Tile).trainPilotAnimatedExpansion()) {
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
                    val sleepText = sleep?.let { m -> val t = m.toInt(); if (t >= 60) "${t / 60} ${label("ó", "h", "Std.", "h")} ${t % 60} ${label("p", "m", "Min.", "m")}" else "$t min" }
                    val available = metrics.filter { metric -> metric.isAvailable &&
                        NativeDetailedHealthMetrics.status(raw, metric.key, provider, date) == NativeDetailedHealthMetrics.Status.AVAILABLE }
                    if (!expanded) {
                        // 2776: a closed day is one summary line; values keep their own tags.
                        val steps = metrics.first { it.key == "steps" }
                        val line = buildList {
                            add(Triple("steps", if (steps.isAvailable) steps.valueText else "—", steps.unit.ifBlank { steps.label }))
                            sleepText?.let { add(Triple("sleep", it, label("alvás", "sleep", "Schlaf", "somn"))) }
                            listOf("activeCalories", "averageHeartRate").forEach { key -> metrics.first { it.key == key }.takeIf { it.isAvailable }
                                ?.let { add(Triple(key, it.valueText, it.unit)) } }
                        }
                        androidx.compose.foundation.layout.FlowRow(Modifier.fillMaxWidth().testTag("journal_day_line_$date"),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            line.forEachIndexed { index, (metricKey, value, unit) ->
                                if (index > 0) Text("·", color = palette.textSecondary, style = MaterialTheme.typography.labelMedium)
                                Text(value, color = palette.textPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.testTag("journal_summary_${date}_$metricKey"))
                                if (unit.isNotBlank()) Text(unit, color = palette.textSecondary, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    } else {
                        if (raw?.optBoolean("partial") == true) Text(label("Részleges adat / újraolvasás szükséges", "Partial data / reload required",
                            "Teilweise Daten / neu laden erforderlich", "Date parțiale / reîncărcare necesară"), color = palette.textSecondary)
                        // 2776: one 3-column grid instead of 4 tiles + a repeated list; a tile filters the records below.
                        val tiles = buildList {
                            sleepText?.let { add(Triple("sleep", label("Alvás", "Sleep", "Schlaf", "Somn"), it)) }
                            available.forEach { add(Triple(it.key, it.label, "${it.valueText} ${it.unit}".trim())) }
                        }
                        tiles.chunked(3).forEach { cells ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                cells.forEach { (metricKey, title, value) ->
                                    val recordType = if (metricKey == "sleep") "SleepSessionRecord" else NativeDetailedHealthMetrics.recordType(metricKey)
                                    val tileShape = RoundedCornerShape(10.dp)
                                    val role = if (type == recordType) GlassMaterialRole.Selection else GlassMaterialRole.Tile
                                    Surface(shape = tileShape, color = palette.overlayRoleFill(
                                        if (type == recordType) palette.primary.copy(alpha = 0.16f) else palette.surfaceCard, role),
                                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).overlayRoleRim(palette, tileShape, role)
                                            .trainPilotClickable(tileShape) { type = if (type == recordType) "" else recordType; recordsOpen = true }
                                            .testTag("journal_metric_${date}_$metricKey")) {
                                        Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                            Text(title, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary, maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                            Text(value, color = palette.textPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                                        }
                                    }
                                }
                                repeat(3 - cells.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                        // 2780: say why a Health Connect day has fewer tiles instead of leaving a silent gap.
                        if (healthConnectLacksActiveCalories(provider, available.map { it.key }.toSet()))
                            Text(label("Erre a napra nincs elérhető aktív kalória a Health Connectben. Ellenőrizd az engedélyeket, a szinkront és a Samsung Health forrást.",
                                "No active calories are available in Health Connect for this day. Check permissions, sync and the Samsung Health source.",
                                "Für diesen Tag sind in Health Connect keine aktiven Kalorien verfügbar. Berechtigungen, Synchronisierung und Samsung Health prüfen.",
                                "Pentru această zi nu sunt disponibile calorii active în Health Connect. Verifică permisiunile, sincronizarea și sursa Samsung Health."),
                                color = palette.textSecondary, style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.testTag("journal_hc_missing_hint_$date"))
                        val headerShape = RoundedCornerShape(10.dp)
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).trainPilotClickable(headerShape) { recordsOpen = !recordsOpen }
                            .testTag("journal_records_toggle_$date"), verticalAlignment = Alignment.CenterVertically) {
                            Text(label("Részletes rekordok", "Detailed records", "Detaillierte Einträge", "Înregistrări detaliate"),
                                color = palette.primary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Icon(if (recordsOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null, tint = palette.primary)
                        }
                        if (recordsOpen) {
                        val filters = listOf(
                            "" to label("Mind", "All", "Alle", "Toate"),
                            "StepsRecord" to label("Lépés", "Steps", "Schritte", "Pași"),
                            "ActiveCaloriesBurnedRecord" to label("Aktív kcal", "Active kcal", "Aktiv kcal", "Kcal active"),
                            "TotalCaloriesBurnedRecord" to label("Teljes kcal", "Total kcal", "Gesamt kcal", "Kcal totale"),
                            "DistanceRecord" to label("Táv", "Distance", "Distanz", "Distanță"),
                            "HeartRateRecord" to label("Pulzus", "Heart rate", "Puls", "Puls"),
                            "SleepSessionRecord" to label("Alvás", "Sleep", "Schlaf", "Somn"))
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            filters.forEach { (filterType, filterLabel) ->
                                com.example.ui.components.TrainPilotFilterChip(selected = type == filterType, onClick = { type = filterType },
                                    colors = FilterChipDefaults.filterChipColors(containerColor = palette.glassTile(palette.surfaceCard),
                                        labelColor = palette.textPrimary, selectedContainerColor = palette.glassStateTile(palette.primary, palette.primary, strong = true),
                                        selectedLabelColor = if (palette.isLiquidGlass) palette.selectionColors().foreground else palette.onPrimary),
                                    label = { Text(filterLabel) },
                                    modifier = Modifier.heightIn(min = 48.dp).testTag(
                                        if (filterType.isEmpty()) "journal_records_all_$date" else "journal_records_filter_${date}_$filterType"))
                            }
                        }
                        }
                    }
                    StudioNativeEditorTheme {
                        HealthRecordDayDetails(date, provider, source, type, language, snapshot.getLong("generation"),
                            "$revision:${snapshot.getLong("generation")}", expanded && recordsOpen,
                            dailySummary = raw)
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

/** Selected Health Connect day lacks an admitted active-energy value; the reason is not inferred. */
internal fun healthConnectLacksActiveCalories(provider: String, availableKeys: Set<String>): Boolean =
    provider == com.repforge.app.HealthJournalStore.HC && "activeCalories" !in availableKeys && availableKeys.isNotEmpty()
