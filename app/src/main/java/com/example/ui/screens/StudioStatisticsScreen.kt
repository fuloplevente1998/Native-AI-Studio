package com.example.ui.screens
import com.example.ui.theme.glassTileBackdrop
import com.example.ui.theme.trainPilotGlassRim
import com.example.ui.theme.glassTile

import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.ui.components.trainPilotAnimatedExpansion
import com.repforge.app.ui.theme.trainPilotClickable
import com.repforge.app.ui.theme.TrainPilotOutlinedButton

import com.repforge.app.ui.theme.trainPilotBorder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import com.repforge.app.ui.theme.TrainPilotTextButton as TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.localization.TrainPilotStrings
import com.example.ui.components.LocalSyncDailyHealth
import com.example.ui.components.TrainPilotChartInfoLine
import com.example.ui.components.chartDateLabel
import com.example.ui.components.chartNearestIndex
import com.example.ui.components.trainPilotChartMarkColor
import com.example.ui.components.trainPilotChartSelection
import com.example.ui.components.trainPilotChartTap
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState
import com.repforge.app.HealthJournalStore
import com.repforge.app.SavedExerciseBest
import com.repforge.app.ui.progress.*
import com.repforge.app.ui.theme.LocalTrainPilotColors
import com.repforge.app.ui.theme.TrainPilotExtendedColors
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@Composable
internal fun StudioStatisticsScreen(state: TrainPilotUiState, onSelectTab: (Int) -> Unit,
    onStepsMode: (Boolean) -> Unit, onRefreshRecords: () -> Unit,
    model: ProgressViewModel = viewModel(key = "studio-statistics")) {
    val native by model.uiState.collectAsStateWithLifecycle()
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, model) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) model.onCardioScreenResumed()
        }
        owner.lifecycle.addObserver(observer)
        model.onCardioScreenResumed()
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(native.dataRevision) { onRefreshRecords() }
    // The Cardio overview always covers every saved activity, including when
    // Statistics is restored after an exercise-scoped record was viewed.
    LaunchedEffect(state.statsSubTab, state.cardioFilterStepsMode, native.selectedCardioId) {
        if (state.statsSubTab == 1 && !state.cardioFilterStepsMode &&
            native.selectedCardioId != NativeCardioModel.ALL_ACTIVITIES) {
            model.selectCardioActivity(NativeCardioModel.ALL_ACTIVITIES)
        }
    }
    LaunchedEffect(model) {
        while (true) { delay(60_000); model.onCardioCalendarTick() }
    }
    StudioStatisticsContent(state, native, onSelectTab, onStepsMode,
        onExercise = { id -> model.selectExercise(id); model.selectCardioActivity(id) },
        onActivity = model::selectCardioActivity, onRetry = model::retryCardio,
        onRefreshRecords = onRefreshRecords)
}

private fun statsTr(lang: com.example.model.AppLanguage, hu: String, en: String, de: String, ro: String): String = when (lang) {
    com.example.model.AppLanguage.HU -> hu
    com.example.model.AppLanguage.EN -> en
    com.example.model.AppLanguage.DE -> de
    com.example.model.AppLanguage.RO -> ro
}

@Composable
internal fun StudioStatisticsContent(state: TrainPilotUiState, native: ProgressUiState,
    onSelectTab: (Int) -> Unit, onStepsMode: (Boolean) -> Unit, onExercise: (String) -> Unit,
    onActivity: (String) -> Unit, onRetry: () -> Unit, onRefreshRecords: () -> Unit) {
    val palette = LocalTrainPilotPalette.current
    val strings = TrainPilotStrings.forLanguage(state.language)
    val lang = state.language
    val number = remember(state.language) { NumberFormat.getNumberInstance(Locale.forLanguageTag(state.language.name.lowercase()))
        .apply { maximumFractionDigits = 2 } }
    var selectedRecord by rememberSaveable { mutableStateOf<String?>(null) }
    val colors = remember(palette) {
        TrainPilotExtendedColors(bg = palette.background, card = palette.surfaceCard, card2 = palette.surfaceElevated,
            line = palette.cardBorder, text = palette.textPrimary, muted = palette.textSecondary,
            accent = palette.primary, accent2 = palette.secondary, onAccent = palette.onPrimary,
            dangerText = palette.danger, success = palette.success, emphasisBorder = palette.sectionBorder)
    }
    CompositionLocalProvider(LocalTrainPilotColors provides colors) {
        val scrollState = rememberScrollState()
        com.example.ui.components.TrainPilotScrollableBox(
            scrollState = scrollState,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(Modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = 12.dp, vertical = 4.dp)
                .testTag("studio_statistics_screen"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // 2779: three tabs in one row: Strength (records) · Cardio · Steps. The stored state stays
            // statsSubTab 0/1 + cardioFilterStepsMode, so the view model and saved settings are unchanged.
            val visibleTab = when {
                state.statsSubTab == 0 -> 0
                state.cardioFilterStepsMode -> 2
                else -> 1
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val subTabLabels = listOf(statsTr(lang, "Gyakorlatok", "Exercises", "Übungen", "Exerciții"), statsTr(lang, "Kardió", "Cardio", "Cardio", "Cardio"),
                    statsTr(lang, "Lépések", "Steps", "Schritte", "Pași"))
                subTabLabels.forEachIndexed { index, label ->
                    com.example.ui.components.TrainPilotFilterChip(selected = visibleTab == index, onClick = {
                        when (index) {
                            0 -> onSelectTab(0)
                            1 -> { onActivity(NativeCardioModel.ALL_ACTIVITIES); onSelectTab(1); onStepsMode(false) }
                            else -> { onSelectTab(1); onStepsMode(true) }
                        }
                    }, label = { com.example.ui.components.TrainPilotFitText(label, modifier = Modifier.testTag("statistics_subtab_label_$index")) },
                        modifier = Modifier.weight(com.example.ui.components.trainPilotLabelWeight(label, subTabLabels)).heightIn(min = 48.dp)
                        .testTag("statistics_subtab_$index"))
                }
            }
            if (state.statsSubTab == 0) {
                when {
                    state.savedRecordsLoading -> Text(statsTr(lang, "Rekordok betöltése…", "Loading records…", "Rekorde werden geladen…", "Se încarcă recordurile…"), color = palette.textSecondary)
                    state.savedRecordsError -> {
                        Text(statsTr(lang, "A rekordok nem olvashatók.", "Records could not be read.", "Rekorde konnten nicht geladen werden.", "Recordurile nu pot fi citite."), color = palette.danger)
                        TextButton(onClick = onRefreshRecords) { Text(statsTr(lang, "Újrapróbálás", "Retry", "Erneut versuchen", "Reîncearcă")) }
                    }
                    state.savedExerciseBests.isEmpty() -> Text(statsTr(lang, "Még nincs mentett, befejezett sorozat.", "No saved completed sets yet.", "Noch keine gespeicherten Sätze.", "Nicio serie finalizată salvată încă."), color = palette.textSecondary)
                    else -> state.savedExerciseBests.forEach { record ->
                        val expanded = selectedRecord == record.exerciseId
                        // 2783: mini trend (last 8 workouts) on every card, the big chart with periods when opened.
                        val trendKind = exerciseTrendKind(record)
                        val trend = remember(record, state.loggedWorkouts) { exerciseTrend(record, state.loggedWorkouts) }
                        Surface(shape = MaterialTheme.shapes.large, color = palette.glassTile(palette.surfaceCard),
                            modifier = Modifier.fillMaxWidth().trainPilotGlassRim(palette, MaterialTheme.shapes.large)
                                .trainPilotBorder(1.25.dp, palette.sectionBorder, MaterialTheme.shapes.large)
                                .trainPilotAnimatedExpansion().testTag("statistics_record_${record.exerciseId}").glassTileBackdrop(palette, MaterialTheme.shapes.large)) {
                            Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).trainPilotClickable(RoundedCornerShape(14.dp)) {
                                    selectedRecord = if (expanded) null else record.exerciseId
                                    if (!expanded) onExercise(record.exerciseId)
                                }, verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        val recordTitle = state.exercises.firstOrNull { it.id == record.exerciseId }?.localizedName(lang)
                                            ?: if (lang == com.example.model.AppLanguage.HU) record.nameHu else record.nameEn.ifBlank { record.nameHu }
                                        // 2784: explicit line height + a gap, so a two-line name never covers the subtitle.
                                        Text(recordTitle,
                                            color = palette.textPrimary, style = MaterialTheme.typography.titleMedium.copy(lineHeight = 22.sp),
                                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        Text(savedRecordLabel(record, number, lang), color = palette.textSecondary,
                                            style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
                                    }
                                    if (trendKind != ExerciseTrendKind.NONE) {
                                        val mini = trend.takeLast(8)
                                        if (mini.size >= 2) Column(horizontalAlignment = Alignment.End,
                                            modifier = Modifier.padding(end = 4.dp).testTag("statistics_record_mini_${record.exerciseId}")) {
                                            ExerciseSparkline(mini, palette.primary)
                                            val change = exerciseTrendChange(mini, trendKind, lang.name.lowercase()).orEmpty()
                                            Text(change, style = MaterialTheme.typography.labelSmall,
                                                color = if (change.startsWith("▲")) palette.success else if (change.startsWith("▼")) palette.danger else palette.textSecondary,
                                                modifier = Modifier.testTag("statistics_record_change_${record.exerciseId}"))
                                        } else if (mini.size == 1) Text(statsTr(lang, "1 mérés", "1 entry", "1 Messung", "1 măsurătoare"),
                                            style = MaterialTheme.typography.labelSmall, color = palette.textSecondary, modifier = Modifier.padding(end = 4.dp))
                                    }
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = palette.primary)
                                }
                                if (expanded && trendKind != ExerciseTrendKind.NONE && trend.size >= 2) {
                                    HorizontalDivider(color = palette.cardBorder)
                                    var periodDays by rememberSaveable(record.exerciseId) { mutableStateOf(90) }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(30 to statsTr(lang, "30 nap", "30 days", "30 Tage", "30 zile"), 90 to statsTr(lang, "90 nap", "90 days", "90 Tage", "90 zile"),
                                            0 to statsTr(lang, "Mind", "All", "Alle", "Toate")).forEach { (days, label) ->
                                            com.example.ui.components.TrainPilotFilterChip(selected = periodDays == days, onClick = { periodDays = days }, label = { Text(label) },
                                                modifier = Modifier.heightIn(min = 48.dp).testTag("statistics_record_period_${record.exerciseId}_$days"))
                                        }
                                    }
                                    val shown = if (periodDays == 0) trend else trend.filter { !it.date.isBefore(LocalDate.now().minusDays(periodDays.toLong() - 1)) }
                                    if (shown.size >= 2) {
                                        // 2785: tap a point → highlighted, info line shows date + value (default: latest).
                                        var sel by remember(record.exerciseId, periodDays, shown) { mutableIntStateOf(shown.lastIndex) }
                                        val selectedLabel = exerciseTrendPointLabel(shown[sel], trendKind, lang.code)
                                        Box(Modifier.fillMaxWidth().height(160.dp).testTag("statistics_record_chart_${record.exerciseId}")
                                            .trainPilotChartTap(shown, { x, w -> chartNearestIndex(x, w, shown.size) }, selectedLabel) { sel = it }) {
                                            TrendCanvas(shown.map { it.value }, sel)
                                        }
                                        TrainPilotChartInfoLine(selectedLabel,
                                            Modifier.testTag("statistics_record_chart_info_${record.exerciseId}"))
                                    } else Text(statsTr(lang, "Ebben az időszakban kevés a mérés.", "Too few entries in this period.", "Zu wenige Messungen in diesem Zeitraum.", "Prea puține măsurători în această perioadă."),
                                        color = palette.textSecondary, style = MaterialTheme.typography.bodySmall)
                                    Text(when (trendKind) {
                                        ExerciseTrendKind.E1RM -> statsTr(lang, "Becsült 1RM (Epley), kg", "Estimated 1RM (Epley), kg", "Geschätztes 1RM (Epley), kg", "1RM estimat (Epley), kg")
                                        ExerciseTrendKind.SECONDS -> statsTr(lang, "Legjobb idő edzésenként, mp", "Best time per workout, s", "Beste Zeit je Training, Sek.", "Cel mai bun timp pe antrenament, sec")
                                        else -> statsTr(lang, "Legtöbb ismétlés edzésenként", "Most reps per workout", "Meiste Wdh. je Training", "Cele mai multe repetări pe antrenament")
                                    } + exerciseTrendChange(shown, trendKind, lang.name.lowercase())?.let { " · $it" }.orEmpty(),
                                        color = palette.textSecondary, style = MaterialTheme.typography.bodySmall)
                                } else if (expanded) {
                                    HorizontalDivider(color = palette.cardBorder)
                                    if (record.weightKg != null && native.selectedExerciseId == record.exerciseId) {
                                        Text(statsTr(lang, "Becsült 1RM • Epley • utolsó 90 nap", "Estimated 1RM • Epley • last 90 days", "Geschätztes 1RM • Epley • letzte 90 Tage", "1RM estimat • Epley • ultimele 90 de zile"), color = palette.textSecondary)
                                        if (native.strengthLoading) Text(statsTr(lang, "Betöltés…", "Loading…", "Laden…", "Se încarcă…"), color = palette.textSecondary)
                                        else if (native.strengthError) Text(statsTr(lang, "Az 1RM-adatsor nem olvasható.", "1RM series could not be read.", "1RM-Datenreihe nicht lesbar.", "Seria 1RM nu poate fi citită."), color = palette.danger)
                                        else if (native.exercise1RmTrend.isEmpty()) Text(statsTr(lang, "Nincs érvényes súly/ismétlés alapú 1RM-adat.", "No valid weight/reps 1RM data.", "Keine gültigen 1RM-Daten vorhanden.", "Fără date 1RM valide bazate pe greutate/repetări."), color = palette.textSecondary)
                                        else {
                                            Text(statsTr(lang, "Legutóbbi becslés: ${number.format(native.exercise1RmTrend.last().e1rmKg)} kg", "Latest estimate: ${number.format(native.exercise1RmTrend.last().e1rmKg)} kg", "Letzte Schätzung: ${number.format(native.exercise1RmTrend.last().e1rmKg)} kg", "Ultima estimare: ${number.format(native.exercise1RmTrend.last().e1rmKg)} kg"), color = palette.textPrimary)
                                            val points = native.exercise1RmTrend
                                            var sel by remember(record.exerciseId, points) { mutableIntStateOf(points.lastIndex) }
                                            val point = points[sel]
                                            val selectedLabel = "${point.date} · ${number.format(point.e1rmKg)} kg"
                                            Box(Modifier.fillMaxWidth().height(160.dp)
                                                .testTag("statistics_record_chart_${record.exerciseId}")
                                                .trainPilotChartTap(points, { x, w -> chartNearestIndex(x, w, points.size) }, selectedLabel) { sel = it }) {
                                                TrendCanvas(points.map { it.e1rmKg }, sel)
                                            }
                                            TrainPilotChartInfoLine(selectedLabel,
                                                Modifier.testTag("statistics_record_chart_info_${record.exerciseId}"))
                                        }
                                    } else if (record.distanceMeters != null || record.measurementType == "distance" || record.repUnit in listOf("km", "m")) {
                                        NativeCardioProgressPanel(native, onRetry)
                                    } else Text(savedRecordLabel(record, number, lang), color = palette.textPrimary)
                                }
                            }
                        }
                    }
                }
            } else {
                if (state.cardioFilterStepsMode) DailyActivityStatistics(state, number)
                else NativeCardioProgressPanel(native, onRetry)
            }
        }
    }
}
}

internal fun savedRecordLabel(
    record: SavedExerciseBest,
    number: NumberFormat,
    language: com.example.model.AppLanguage = com.example.model.AppLanguage.HU
): String = when {
    record.distanceMeters != null -> statsTr(
        language,
        "Leghosszabb táv: ${number.format(record.distanceMeters / 1000)} km",
        "Longest distance: ${number.format(record.distanceMeters / 1000)} km",
        "Längste Distanz: ${number.format(record.distanceMeters / 1000)} km",
        "Cea mai lungă distanță: ${number.format(record.distanceMeters / 1000)} km"
    )
    record.measurementType == "distance" || record.repUnit in listOf("km", "m") -> statsTr(
        language,
        "Nincs használható távadat",
        "No usable distance data",
        "Keine nutzbaren Distanzdaten",
        "Fără date de distanță utilizabile"
    )
    record.seconds != null -> statsTr(
        language,
        "Legjobb idő: ${record.seconds} mp",
        "Best time: ${record.seconds} sec",
        "Beste Zeit: ${record.seconds} Sek.",
        "Cel mai bun timp: ${record.seconds} sec"
    )
    record.weightKg != null -> statsTr(
        language,
        "Legnagyobb súly: ${number.format(record.weightKg)} kg",
        "Heaviest weight: ${number.format(record.weightKg)} kg",
        "Höchstes Gewicht: ${number.format(record.weightKg)} kg",
        "Cea mai mare greutate: ${number.format(record.weightKg)} kg"
    )
    record.repetitions != null -> statsTr(
        language,
        "Legjobb ismétlés: ${record.repetitions}",
        "Best reps: ${record.repetitions}",
        "Beste Wdh.: ${record.repetitions}",
        "Cele mai multe repetări: ${record.repetitions}"
    )
    else -> statsTr(
        language,
        "Nincs használható mért sorozat",
        "No usable measured set",
        "Kein nutzbarer gemessener Satz",
        "Nicio serie măsurată utilizabilă"
    )
}

/** 2779: last 14 days, oldest first; a day without synced steps stays null (missing ≠ 0). */
internal fun dailyStepBars(days: List<com.example.ui.viewmodel.WeeklyHealthDaySummary>, today: LocalDate): List<Int?> {
    val byDate = days.associateBy { it.dateIso }
    return (13 downTo 0).map { back -> byDate[today.minusDays(back.toLong()).toString()]?.takeIf { it.stepsAvailable }?.steps }
}

@Composable
private fun DailyActivityStatistics(state: TrainPilotUiState, number: NumberFormat) {
    val palette = LocalTrainPilotPalette.current
    val lang = state.language
    val refresh = LocalSyncDailyHealth.current
    var explanationOpen by rememberSaveable { mutableStateOf(false) }
    var listOpen by rememberSaveable { mutableStateOf(false) }
    val today = LocalDate.now()
    val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val previous = weekStart.minusWeeks(1)
    val windowStart = minOf(previous, today.minusDays(13))
    val days = state.dailyActivityDays.filter {
        val day = runCatching { LocalDate.parse(it.dateIso) }.getOrNull()
        day != null && !day.isBefore(windowStart) && !day.isAfter(today)
    }
    fun total(from: LocalDate, until: LocalDate): String {
        val available = days.filter { it.stepsAvailable && it.dateIso >= from.toString() && it.dateIso < until.toString() }
        return if (available.isEmpty()) "—" else number.format(available.sumOf { it.steps })
    }
    Surface(shape = MaterialTheme.shapes.large, color = palette.glassTile(palette.surfaceCard),
        modifier = Modifier.fillMaxWidth().trainPilotGlassRim(palette, MaterialTheme.shapes.large)
            .trainPilotBorder(1.25.dp, palette.sectionBorder, MaterialTheme.shapes.large)
            .testTag("statistics_daily_activity").glassTileBackdrop(palette, MaterialTheme.shapes.large)) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatisticsMetricTile(statsTr(lang, "Ma", "Today", "Heute", "Azi"), total(today, today.plusDays(1)), Modifier.weight(1f),
                    valueTag = "statistics_steps_today")
                StatisticsMetricTile(statsTr(lang, "Ez a hét", "This week", "Diese Woche", "Săpt. aceasta"), total(weekStart, today.plusDays(1)),
                    Modifier.weight(1f), valueTag = "statistics_steps_week")
                StatisticsMetricTile(statsTr(lang, "Előző hét", "Previous week", "Vorwoche", "Săpt. trecută"), total(previous, weekStart),
                    Modifier.weight(1f), valueTag = "statistics_steps_previous_week")
            }
            Text(statsTr(lang, "Utolsó 14 nap", "Last 14 days", "Letzte 14 Tage", "Ultimele 14 zile"),
                color = palette.textSecondary, style = MaterialTheme.typography.labelMedium)
            val bars = remember(days, today) { dailyStepBars(days, today) }
            val max = bars.filterNotNull().maxOrNull()?.coerceAtLeast(1) ?: 1
            // The bar chart alone carries the selection; one detail strip now sits below it.
            var selectedBar by rememberSaveable(today.toString()) { mutableStateOf(bars.lastIndex) }
            fun stepValueLabel(value: Int?): String = value?.let {
                val formatted = number.format(it)
                statsTr(lang, "$formatted lépés", "$formatted steps", "$formatted Schritte", "$formatted pași")
            } ?: statsTr(lang, "nincs szinkronizálva", "not synced", "nicht synchronisiert", "nesincronizat")
            val selectedDate = today.minusDays((bars.lastIndex - selectedBar).toLong())
            val selectedDay = days.firstOrNull { it.dateIso == selectedDate.toString() }
            val locale = Locale.forLanguageTag(lang.name.lowercase())
            val weekdayFmt = remember(locale) { java.time.format.DateTimeFormatter.ofPattern("EEEE", locale) }
            val letters = remember(locale) { java.time.format.DateTimeFormatter.ofPattern("EEEEE", locale) }
            // A readable fixed scale without counting missing days as measured zeros.
            val scaleTick = when {
                max > 10000 -> 5000
                max > 2000 -> 2500
                max > 100 -> 100
                else -> 25
            }
            val scaleMax = ((max + scaleTick - 1) / scaleTick * scaleTick).coerceAtLeast(scaleTick)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.width(46.dp).height(92.dp), verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End) {
                    Text(number.format(scaleMax), style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                    Text(number.format(scaleMax / 2), style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                    Text("0", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                }
                Row(Modifier.weight(1f).height(92.dp).testTag("statistics_steps_chart"),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                bars.forEachIndexed { index, value ->
                    val selected = index == selectedBar
                    val date = today.minusDays((bars.lastIndex - index).toLong())
                    val label = "${chartDateLabel(date, lang.code)} · ${stepValueLabel(value)}"
                    val mark = trainPilotChartMarkColor(selected, palette.primary, palette.textSecondary)
                    Column(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(6.dp))
                        .trainPilotClickable(RoundedCornerShape(6.dp)) { selectedBar = index }
                        .trainPilotChartSelection(selected, label)
                        .testTag("statistics_steps_column_$index"), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.BottomCenter) {
                            if (value == null) Box(Modifier.fillMaxWidth().height(2.dp)
                                .background(mark, RoundedCornerShape(1.dp))
                                .testTag("statistics_steps_bar_missing_$index"))
                            else Box(Modifier.fillMaxWidth().fillMaxHeight((value.toFloat() / scaleMax).coerceIn(0.04f, 1f))
                                .background(mark, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .testTag("statistics_steps_bar_$index"))
                        }
                        Text(date.format(letters),
                            color = if (selected) palette.primary else palette.textSecondary,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
                }
            }
            Column(Modifier.fillMaxWidth().glassTileBackdrop(palette, RoundedCornerShape(10.dp)).clip(RoundedCornerShape(10.dp)).background(palette.glassTile(palette.surfaceElevated, elevated = true))
                .padding(horizontal = 10.dp, vertical = 6.dp).testTag("statistics_steps_selected")) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null,
                        tint = palette.primary, modifier = Modifier.size(18.dp))
                    Text("${selectedDate.monthValue}.${"%02d".format(selectedDate.dayOfMonth)}. (${selectedDate.format(weekdayFmt)})",
                        color = palette.textPrimary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(stepValueLabel(bars[selectedBar]),
                        color = if (bars[selectedBar] != null) palette.primary else palette.textSecondary,
                        style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("statistics_steps_selected_value"))
                }
                val outsideSelected = selectedDay?.takeIf { it.stepsAvailable }?.let {
                    if (it.workoutCount == 0) number.format(it.steps) else "—" } ?: "—"
                val sourceSelected = selectedDay?.stepsSourceLabelHu?.takeIf { it.isNotBlank() && !it.contains("Nincs", ignoreCase = true) } ?: "—"
                Text(statsTr(lang, "$sourceSelected · edzésen kívül: $outsideSelected", "$sourceSelected · outside workouts: $outsideSelected",
                    "$sourceSelected · außerhalb: $outsideSelected", "$sourceSelected · în afara antrenamentelor: $outsideSelected"),
                    color = palette.textSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            val ordered = days.reversed()
            // Always-visible history rows duplicated the highlighted graph day. The chart
            // now owns the one selected-day detail; full history slides up separately.
            if (ordered.size > 1) TextButton(onClick = { listOpen = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("statistics_steps_list_toggle")) {
                Text(statsTr(lang, "▾ Napi lista (${ordered.size})", "▾ Daily list (${ordered.size})",
                    "▾ Tagesliste (${ordered.size})", "▾ Listă zilnică (${ordered.size})"))
            }
            if (listOpen) com.example.ui.components.TrainPilotScreenSheet(
                onDismiss = { listOpen = false }, testTag = "statistics_steps_daily_sheet"
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Fixed header/close action above the independently scrolling daily list.
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(statsTr(lang, "Napi lista (${ordered.size})", "Daily list (${ordered.size})",
                                "Tagesliste (${ordered.size})", "Listă zilnică (${ordered.size})"),
                            color = palette.textPrimary, style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f).testTag("statistics_steps_sheet_header"))
                        IconButton(onClick = { listOpen = false }, modifier = Modifier.testTag("statistics_steps_sheet_close")) {
                            Icon(Icons.Default.Close, contentDescription = statsTr(lang, "Bezárás", "Close", "Schließen", "Închide"),
                                tint = palette.primary)
                        }
                    }
                    Column(Modifier.fillMaxWidth()
                        .heightIn(max = (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp * 0.62f).dp)
                        .verticalScroll(rememberScrollState()).testTag("statistics_steps_sheet_list"),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ordered.forEach { day ->
                            HorizontalDivider(color = palette.cardBorder)
                            Row(Modifier.fillMaxWidth()
                                .background(if (day.dateIso == selectedDate.toString()) palette.primary.copy(alpha = 0.12f)
                                    else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(8.dp))
                                .testTag("statistics_steps_row_${day.dateIso}"),
                                verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(day.dateIso, color = palette.textPrimary, style = MaterialTheme.typography.bodySmall)
                                    Text(if (day.stepsSourceLabelHu.contains("Nincs", ignoreCase = true))
                                        statsTr(lang, "Nincs szinkronizálva", "Not synced", "Nicht synchronisiert", "Nesincronizat")
                                        else day.stepsSourceLabelHu, style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSecondary)
                                }
                                Text(if (!day.stepsAvailable) "— / —" else
                                    "${number.format(day.steps)} / ${if (day.workoutCount == 0) number.format(day.steps) else "—"}",
                                    color = palette.textPrimary, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
            Text(statsTr(lang, "Szinkronizált napok: ${days.count { it.stepsAvailable }}/${days.size}", "Synced days: ${days.count { it.stepsAvailable }}/${days.size}", "Synchronisierte Tage: ${days.count { it.stepsAvailable }}/${days.size}", "Zile sincronizate: ${days.count { it.stepsAvailable }}/${days.size}"),
                color = palette.textSecondary, style = MaterialTheme.typography.labelSmall)
            TrainPilotOutlinedButton(onClick = { refresh(if (state.primaryHealthSourceSamsung) HealthJournalStore.SAMSUNG else HealthJournalStore.HC) },
                enabled = !state.healthSyncBusy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(statsTr(lang, "Lépésadatok frissítése", "Refresh Step Data", "Schrittdaten aktualisieren", "Actualizează datele de pași")) }
            TextButton(onClick = { explanationOpen = !explanationOpen },
                modifier = Modifier.heightIn(min = 48.dp).testTag("statistics_activity_explanation")) {
                Text(if (explanationOpen) statsTr(lang, "Magyarázat bezárása", "Hide Explanation", "Erklärung schließen", "Închide explicația") else statsTr(lang, "Mit jelentenek az adatok?", "What do these numbers mean?", "Was bedeuten diese Daten?", "Ce înseamnă aceste date?"))
            }
            if (explanationOpen) Text(
                statsTr(
                    lang,
                    "Teljes napi lépések, egy kiválasztott forrásból. A sorokban: összes / edzésen kívül. Ha az edzésen kívüli rész nem különíthető el, vagy egy nap nincs szinkronizálva, gondolatjelet mutatunk (a grafikonon üres hely).",
                    "Total daily steps from the selected source. Rows show total / outside workouts. If non-workout steps cannot be separated or a day is not synced, a dash is shown (an empty slot in the chart).",
                    "Gesamte Tagesschritte aus der gewählten Quelle. Zeilen: gesamt / außerhalb. Nicht trennbare oder nicht synchronisierte Tage zeigen einen Strich (leerer Platz im Diagramm).",
                    "Totalul pașilor zilnici din sursa selectată. Rânduri: total / în afară. Zilele neseparabile sau nesincronizate afișează o linie (loc gol în grafic)."
                ),
                color = palette.textSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}
