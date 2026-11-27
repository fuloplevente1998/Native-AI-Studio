package com.example.ui.components

import com.example.ui.theme.glassHeaderMaterial

import com.example.ui.theme.trainPilotGlassRim

import com.example.ui.theme.glassTile
import com.example.ui.theme.GlassMaterialRole
import com.example.ui.theme.overlayRoleFill
import com.example.ui.theme.overlayRoleRim
import com.example.ui.theme.LocalGlassOverlay

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.CalendarMonth
import com.repforge.app.ui.theme.trainPilotBorder
import com.repforge.app.ui.theme.trainPilotFrame
import com.repforge.app.ui.theme.trainPilotClickable
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import com.repforge.app.ui.theme.TrainPilotTextButton as TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.repforge.app.ui.theme.TrainPilotOutlinedButton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.ZoneId
import java.util.Locale

private data class JournalPage(val range: HealthJournalRange, val snapshot: JSONObject)

/** A continuous scrollable journal backed by bounded queries of daily summaries. */
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
    onOpenWorkout: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    pinnedHeader: (@Composable () -> Unit)? = null
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
    // 2772: one filter button opens the same period sheet as the Progress log.
    var showCalendar by rememberSaveable { mutableStateOf(false) }
    var pickStart by rememberSaveable { mutableStateOf<String?>(null) }
    var pickEnd by rememberSaveable { mutableStateOf<String?>(null) }
    var calendarMonth by rememberSaveable { mutableStateOf(java.time.YearMonth.from(LocalDate.parse(to)).toString()) }
    var reload by rememberSaveable { mutableIntStateOf(0) }
    val range = remember(from, to) { HealthJournalRange(LocalDate.parse(from), LocalDate.parse(to)) }
    val listState = key(from, to) { rememberLazyListState() }
    val focusedDay = focus?.first?.let(LocalDate::parse)
    var focused by remember(focus, focusKey) { mutableStateOf(false) }
    LaunchedEffect(focus, focusKey) {
        val date = focusedDay ?: return@LaunchedEffect
        if (date < range.from) from = date.toString()
        if (date > range.to) to = date.toString()
    }

    val revision = "$refreshKey:$reload"
    var pages by remember(samsungSupplement, range, revision) { mutableStateOf(emptyList<JournalPage>()) }
    var failed by remember(samsungSupplement, range, revision) { mutableStateOf(false) }
    var requestedPages by rememberSaveable(from, to, samsungSupplement) { mutableIntStateOf(1) }
    val days = remember(pages) { pages.flatMap { page -> page.range.days.map { it to page.snapshot } } }
    val lastPage = pages.lastOrNull()?.range
    val hasOlderDays = lastPage != null && lastPage.pageStart > range.from
    LaunchedEffect(listState, pages.size, hasOlderDays) {
        if (!hasOlderDays || days.isEmpty()) return@LaunchedEffect
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }.collect { last ->
            if (last >= 0 && last + 2 >= days.lastIndex) {
                requestedPages = maxOf(requestedPages, pages.size + 1)
            }
        }
    }
    LaunchedEffect(focusedDay, focusKey, range, days.size) {
        val date = focusedDay ?: return@LaunchedEffect
        if (focused || date < range.from || date > range.to) return@LaunchedEffect
        val index = ChronoUnit.DAYS.between(date, range.to).toInt()
        requestedPages = maxOf(requestedPages, index / HealthJournalRange.PAGE_DAYS + 1)
        if (index in days.indices) {
            listState.scrollToItem(index)
            focused = true
        }
    }
    LaunchedEffect(samsungSupplement, range, revision, pages.size, requestedPages, failed) {
        if (failed || pages.size >= requestedPages || (lastPage != null && !hasOlderDays)) return@LaunchedEffect
        val next = lastPage?.older() ?: range
        try {
            val ticket = NativeDataReplacement.continuationForCurrentData(context)
            val result = withContext(Dispatchers.IO) {
                NativeDataReplacement.mutate(context) {
                    val store = HealthJournalStore.get(context)
                    JSONObject().apply {
                        put(primary, NativeHealthJournal.load(store, primary, "", next))
                        if (samsungSupplement) put(supplement, NativeHealthJournal.load(store, supplement, "", next))
                    }
                }
            }
            ticket.requireCurrent(context)
            pages = pages + JournalPage(next, result)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { failed = true }
    }
    val periodFormat = remember(language) { java.time.format.DateTimeFormatter.ofPattern("MMM d.", Locale.forLanguageTag(language)) }
    val dayFormat = remember(language) { java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)
        .withLocale(Locale.forLanguageTag(language)) }
    com.example.ui.theme.GlassScrollChrome(
        modifier = modifier.fillMaxWidth(),
        rootInset = 0.dp,
        localWindow = true,
        top = {
            val shape = RoundedCornerShape(18.dp)
            Surface(shape = shape,
                color = if (palette.isLiquidGlass) androidx.compose.ui.graphics.Color.Transparent else palette.surface,
                modifier = Modifier.fillMaxWidth().glassHeaderMaterial(palette, shape)
                    .trainPilotBorder(1.dp, palette.cardBorder, shape)) {
                Column(Modifier.fillMaxWidth().padding(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    pinnedHeader?.invoke()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            val pillShape = if (palette.isLiquidGlass) RoundedCornerShape(50) else RoundedCornerShape(14.dp)
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
            TrainPilotIconButton(onClick = {
                if (onRefresh != null) onRefresh() else reload++
            }, enabled = !refreshBusy && (pages.isNotEmpty() || failed), modifier = Modifier.testTag("journal_reload")) {
                if (refreshBusy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.Refresh,
                    contentDescription = label("Egészségadatok frissítése", "Refresh health data", "Gesundheitsdaten aktualisieren", "Actualizează datele de sănătate"),
                    tint = palette.primary)
            }
        }
        if (!refreshError.isNullOrBlank()) Text(refreshError, color = palette.danger, modifier = Modifier.testTag("journal_refresh_error"))
                }
            }
        }
    ) { scrollModifier, topInset, bottomInset ->
        LazyColumn(
            state = listState,
            modifier = scrollModifier.trainPilotListPanel()
                .testTag("health_journal_sheet_scroll"),
            contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 2.dp + topInset, bottom = 2.dp + bottomInset)
        ) {
            itemsIndexed(days, key = { _, (date, _) -> date.toString() }) { index, (date, snapshot) ->
                if (index > 0) {
                    TrainPilotListDivider()
                }
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
                Surface(color = Color.Transparent, modifier = Modifier.fillMaxWidth().trainPilotAnimatedExpansion()) {
                    Column {
                        val headerShape = RoundedCornerShape(12.dp)
                        Row(
                            Modifier.fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .trainPilotClickable(headerShape, stretchOnDrag = true) { expanded = !expanded }
                                .testTag("journal_day_$date")
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(date.format(dayFormat), fontWeight = FontWeight.Bold, color = palette.textPrimary)
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
                                }
                            }
                            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = label("Napi részletek", "Day details", "Tagesdetails", "Detalii zilnice"),
                                tint = palette.primary, modifier = Modifier.align(Alignment.CenterVertically))
                        }
                        if (expanded) Column(
                            Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
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
                                        val legacyFill = if (type == recordType) palette.primary.copy(alpha = 0.16f)
                                            else if (palette.isLiquidGlass) palette.surfaceCard else palette.surfaceElevated
                                        Surface(shape = tileShape, color = palette.overlayRoleFill(
                                            legacyFill, role),
                                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).overlayRoleRim(palette, tileShape, role)
                                                .then(if (palette.isLiquidGlass) Modifier else Modifier.trainPilotFrame(
                                                    0.75.dp, palette.cardBorder, tileShape))
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
            }
            if (failed) item(key = "journal_load_error") {
                TextButton(onClick = { failed = false }, modifier = Modifier.fillMaxWidth().testTag("journal_retry_page")) {
                    Text(label("Az adatok nem tölthetők be. Próbáld újra.", "Could not load data. Try again.",
                        "Daten konnten nicht geladen werden. Bitte erneut versuchen.", "Datele nu au putut fi încărcate. Încearcă din nou."),
                        color = palette.danger)
                }
            } else if (pages.size < requestedPages && (pages.isEmpty() || hasOlderDays)) item(key = "journal_loading") {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
    }
    if (showCalendar) {
        fun applyPeriod(a: String, b: String) {
            val lo = minOf(a, b); val hi = maxOf(a, b)
            from = lo; to = hi
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
