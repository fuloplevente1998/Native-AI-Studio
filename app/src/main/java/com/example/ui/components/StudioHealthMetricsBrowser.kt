package com.example.ui.components

import com.example.ui.theme.trainPilotGlassRim

import com.example.ui.theme.glassTile
import com.example.ui.theme.GlassMaterialRole
import com.example.ui.theme.overlayRoleFill
import com.example.ui.theme.overlayRoleRim
import com.example.ui.theme.LocalGlassOverlay

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
import com.example.model.LoggedWorkoutEntry
import com.example.model.localWorkoutDate
import com.repforge.app.HealthJournalStore
import com.repforge.app.NativeDataReplacement
import com.repforge.app.ui.health.*
import com.repforge.app.ui.theme.TrainPilotFrames
import com.repforge.app.ui.theme.TrainPilotOutlinedButton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/** One compact, paged journal of daily summaries and saved workouts. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun StudioHealthMetricsBrowser(
    language: String,
    refreshKey: String,
    onRefresh: (() -> Unit)? = null,
    refreshBusy: Boolean = false,
    refreshError: String? = null,
    /** 2783: (day ISO, record type) to open directly, e.g. from a Today tile; [focusKey] repeats it. */
    focus: Pair<String, String>? = null,
    focusKey: Int = 0,
    samsungSupplement: Boolean = false,
    workouts: List<LoggedWorkoutEntry> = emptyList(),
    workoutTitle: (LoggedWorkoutEntry) -> String = { it.titleHu },
    onOpenWorkout: (String) -> Unit = {}
) {
    val context = LocalContext.current.applicationContext
    val palette = LocalTrainPilotPalette.current
    fun label(hu: String, en: String, de: String = en, ro: String = en) = when (language.lowercase(Locale.ROOT)) {
        "en" -> en; "de" -> de; "ro" -> ro; else -> hu
    }
    val primary = HealthJournalStore.HC
    val supplement = HealthJournalStore.SAMSUNG
    val workoutsByDay = remember(workouts) { workouts.groupBy { it.localWorkoutDate() } }
    var from by rememberSaveable { mutableStateOf(LocalDate.now().minusDays(29).toString()) }
    var to by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var end by rememberSaveable { mutableStateOf(to) }
    // 2772: one filter button opens the same period sheet as the Progress log.
    var showCalendar by rememberSaveable { mutableStateOf(false) }
    var pickStart by rememberSaveable { mutableStateOf<String?>(null) }
    var pickEnd by rememberSaveable { mutableStateOf<String?>(null) }
    var calendarMonth by rememberSaveable { mutableStateOf(java.time.YearMonth.from(LocalDate.parse(to)).toString()) }
    var reload by rememberSaveable { mutableIntStateOf(0) }
    val range = remember(from, to, end) { HealthJournalRange(LocalDate.parse(from), LocalDate.parse(to), LocalDate.parse(end)) }
    LaunchedEffect(focus, focusKey) {
        val day = focus?.first?.let(LocalDate::parse) ?: return@LaunchedEffect
        if (day !in range.days) {
            if (day.isBefore(range.from)) from = day.toString()
            if (day.isAfter(range.to)) to = day.toString()
            end = day.toString()
        }
    }
    val revision = "$refreshKey:$reload"
    var snapshotJson by remember(samsungSupplement, range, revision) { mutableStateOf<String?>(null) }
    var failed by remember(samsungSupplement, range, revision) { mutableStateOf(false) }
    val snapshot = remember(snapshotJson) { snapshotJson?.let(::JSONObject) }
    LaunchedEffect(samsungSupplement, range, revision) {
        if (snapshotJson != null) return@LaunchedEffect
        try {
            val ticket = NativeDataReplacement.continuationForCurrentData(context)
            val result = withContext(Dispatchers.IO) {
                NativeDataReplacement.mutate(context) {
                    val store = HealthJournalStore.get(context)
                    JSONObject().apply {
                        put(primary, NativeHealthJournal.load(store, primary, "", range))
                        if (samsungSupplement) put(supplement, NativeHealthJournal.load(store, supplement, "", range))
                    }
                }
            }
            ticket.requireCurrent(context)
            snapshotJson = result.toString()
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { failed = true }
    }
    val periodFormat = remember(language) { java.time.format.DateTimeFormatter.ofPattern("MMM d.", Locale.forLanguageTag(language)) }
    val dayFormat = remember(language) { java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)
        .withLocale(Locale.forLanguageTag(language)) }
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
            if (onRefresh != null) onRefresh() else reload++
        }, enabled = !refreshBusy && (snapshot != null || failed), modifier = Modifier.testTag("journal_reload")) {
            if (refreshBusy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Icon(Icons.Default.Refresh,
                contentDescription = label("Egészségadatok frissítése", "Refresh health data", "Gesundheitsdaten aktualisieren", "Actualizează datele de sănătate"),
                tint = palette.primary)
        }
    }
    if (!refreshError.isNullOrBlank()) Text(refreshError, color = palette.danger, modifier = Modifier.testTag("journal_refresh_error"))
    when {
        failed -> Text(label("Az adatok nem tölthetők be. Próbáld újra.", "Could not load data. Try again.",
            "Daten konnten nicht geladen werden. Bitte erneut versuchen.", "Datele nu au putut fi încărcate. Încearcă din nou."), color = palette.danger)
        snapshot == null -> LinearProgressIndicator(Modifier.fillMaxWidth())
        else -> range.days.forEach { date -> key(date) {
            val primaryPage = snapshot.getJSONObject(primary)
            val daySources = remember(snapshot, date) {
                HealthDaySources(date, NativeHealthJournal.day(primaryPage, primary, "", date),
                    snapshot.optJSONObject(supplement)?.let { NativeHealthJournal.day(it, supplement, "", date) },
                    Instant.now(), ZoneId.systemDefault())
            }
            val sleep = remember(daySources) { daySources.sleep() }
            val chosen = remember(daySources) {
                NativeHealthMetrics.definitions.mapNotNull { metric ->
                    daySources.metric(metric.key)?.let { metric.key to it }
                }.toMap()
            }
            val metrics = remember(chosen, language) {
                val values = JSONObject()
                chosen.forEach { (key, reading) -> values.put(key, reading.number) }
                NativeHealthMetrics.parse(context, values, language)
            }
            val dayWorkouts = workoutsByDay[date].orEmpty()
            var expanded by rememberSaveable(samsungSupplement, date) { mutableStateOf(false) }
            var type by rememberSaveable(samsungSupplement, date) { mutableStateOf("") }
            LaunchedEffect(focus, focusKey) {
                if (focus != null && focus.first == date.toString()) {
                    expanded = true
                    type = focus.second
                }
            }
            val sleepText = sleep?.let { value ->
                val minutes = value.number.toInt()
                if (minutes >= 60) "${minutes / 60} ${label("ó", "h", "Std.", "h")} ${minutes % 60} ${label("p", "m", "Min.", "m")}" else "$minutes min"
            }
            val available = metrics.filter { it.isAvailable }
            Surface(shape = RoundedCornerShape(14.dp), color = palette.overlayRoleFill(palette.glassTile(palette.surfaceElevated, elevated = true), GlassMaterialRole.Tile),
                border = com.repforge.app.ui.theme.trainPilotFrameStroke(TrainPilotFrames.stroke(1.dp, palette.cardBorder)), modifier = Modifier.fillMaxWidth().overlayRoleRim(palette, RoundedCornerShape(14.dp), GlassMaterialRole.Tile).trainPilotAnimatedExpansion()) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("journal_day_$date")) {
                        Text(date.format(dayFormat), fontWeight = FontWeight.Bold, color = palette.textPrimary,
                            modifier = Modifier.weight(1f))
                        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = label("Napi részletek", "Day details", "Tagesdetails", "Detalii zilnice"), tint = palette.primary)
                    }
                    if (!expanded) {
                        val steps = metrics.first { it.key == "steps" }
                        val line = buildList {
                            add(Triple("steps", if (steps.isAvailable) steps.valueText else "—", steps.unit.ifBlank { steps.label }))
                            sleepText?.let { add(Triple("sleep", it, label("alvás", "sleep", "Schlaf", "somn"))) }
                            listOf("activeCalories", "averageHeartRate").forEach { key -> metrics.first { it.key == key }.takeIf { it.isAvailable }
                                ?.let { add(Triple(key, it.valueText, it.unit)) } }
                            if (dayWorkouts.isNotEmpty()) add(Triple("workouts", dayWorkouts.size.toString(),
                                label("edzés", if (dayWorkouts.size == 1) "workout" else "workouts", "Training", "antrenamente")))
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
                        val tiles = buildList {
                            sleepText?.let { add(Triple("sleep", label("Alvás", "Sleep", "Schlaf", "Somn"), it)) }
                            available.filter { it.key in visibleHealthJournalMetrics || type == NativeDetailedHealthMetrics.recordType(it.key) }
                                .forEach { add(Triple(it.key, it.label, "${it.valueText} ${it.unit}".trim())) }
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
                        dayWorkouts.forEach { workout ->
                            TextButton(onClick = { onOpenWorkout(workout.id) }, modifier = Modifier.fillMaxWidth()
                                .heightIn(min = 48.dp).testTag("health_journal_workout_${workout.id}")) {
                                Text(workoutTitle(workout), modifier = Modifier.weight(1f), color = palette.textPrimary)
                                if (workout.durationMinutes > 0) Text("${workout.durationMinutes} min", color = palette.textSecondary)
                            }
                        }
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

private val visibleHealthJournalMetrics = setOf(
    "steps", "activeCalories", "totalCalories", "distanceMeters", "averageHeartRate"
)
