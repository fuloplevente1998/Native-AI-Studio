package com.example.ui.screens

import com.repforge.app.ui.theme.TrainPilotOutlinedButton

import com.repforge.app.ui.theme.trainPilotBorder

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.localization.TrainPilotStrings
import com.example.ui.components.LocalSyncDailyHealth
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
    LaunchedEffect(model) {
        while (true) { delay(60_000); model.onCardioCalendarTick() }
    }
    StudioStatisticsContent(state, native, { index ->
        if (index == 1) {
            model.selectCardioActivity(NativeCardioModel.ALL_ACTIVITIES)
            onStepsMode(true)
        }
        onSelectTab(index)
    }, onStepsMode,
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(strings.personalRecordsSubTab, strings.cardioStatsSubTab).forEachIndexed { index, label ->
                    FilterChip(selected = state.statsSubTab == index, onClick = { onSelectTab(index) },
                        label = { Text(label) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)
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
                        Surface(shape = MaterialTheme.shapes.large, color = palette.surfaceCard,
                            modifier = Modifier.fillMaxWidth().trainPilotBorder(1.25.dp, palette.sectionBorder, MaterialTheme.shapes.large)
                                .testTag("statistics_record_${record.exerciseId}")) {
                            Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable {
                                    selectedRecord = if (expanded) null else record.exerciseId
                                    if (!expanded) onExercise(record.exerciseId)
                                }, verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        val recordTitle = state.exercises.firstOrNull { it.id == record.exerciseId }?.localizedName(lang)
                                            ?: if (lang == com.example.model.AppLanguage.HU) record.nameHu else record.nameEn.ifBlank { record.nameHu }
                                        Text(recordTitle,
                                            color = palette.textPrimary, style = MaterialTheme.typography.titleMedium,
                                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        Text(savedRecordLabel(record, number, lang), color = palette.textSecondary,
                                            style = MaterialTheme.typography.bodySmall)
                                    }
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = palette.primary)
                                }
                                if (expanded) {
                                    HorizontalDivider(color = palette.cardBorder)
                                    if (record.weightKg != null && native.selectedExerciseId == record.exerciseId) {
                                        Text(statsTr(lang, "Becsült 1RM • Epley • utolsó 90 nap", "Estimated 1RM • Epley • last 90 days", "Geschätztes 1RM • Epley • letzte 90 Tage", "1RM estimat • Epley • ultimele 90 de zile"), color = palette.textSecondary)
                                        if (native.strengthLoading) Text(statsTr(lang, "Betöltés…", "Loading…", "Laden…", "Se încarcă…"), color = palette.textSecondary)
                                        else if (native.strengthError) Text(statsTr(lang, "Az 1RM-adatsor nem olvasható.", "1RM series could not be read.", "1RM-Datenreihe nicht lesbar.", "Seria 1RM nu poate fi citită."), color = palette.danger)
                                        else if (native.exercise1RmTrend.isEmpty()) Text(statsTr(lang, "Nincs érvényes súly/ismétlés alapú 1RM-adat.", "No valid weight/reps 1RM data.", "Keine gültigen 1RM-Daten vorhanden.", "Fără date 1RM valide bazate pe greutate/repetări."), color = palette.textSecondary)
                                        else {
                                            Text(statsTr(lang, "Legutóbbi becslés: ${number.format(native.exercise1RmTrend.last().e1rmKg)} kg", "Latest estimate: ${number.format(native.exercise1RmTrend.last().e1rmKg)} kg", "Letzte Schätzung: ${number.format(native.exercise1RmTrend.last().e1rmKg)} kg", "Ultima estimare: ${number.format(native.exercise1RmTrend.last().e1rmKg)} kg"), color = palette.textPrimary)
                                            Box(Modifier.fillMaxWidth().height(160.dp)
                                                .testTag("statistics_record_chart_${record.exerciseId}")) {
                                                TrendCanvas(native.exercise1RmTrend.map { it.e1rmKg })
                                            }
                                        }
                                    } else if (record.distanceMeters != null || record.measurementType == "distance" || record.repUnit in listOf("km", "m")) {
                                        NativeCardioProgressPanel(native, onActivity, onRetry)
                                    } else Text(savedRecordLabel(record, number, lang), color = palette.textPrimary)
                                }
                            }
                        }
                    }
                }
            } else {
                TrainPilotOutlinedButton(
                    onClick = { onStepsMode(!state.cardioFilterStepsMode) },
                    modifier = Modifier.fillMaxWidth().testTag("statistics_cardio_mode_button")
                ) {
                    Text(if (state.cardioFilterStepsMode) statsTr(lang, "Napi aktivitás / lépések", "Daily Activity / Steps", "Tägliche Aktivität / Schritte", "Activitate zilnică / pași") else statsTr(lang, "Minden mozgás", "All Activities", "Alle Aktivitäten", "Toate activitățile"))
                }
                if (!state.cardioFilterStepsMode) NativeCardioProgressPanel(native, onActivity, onRetry)
                if (state.cardioFilterStepsMode || native.selectedCardioId == NativeCardioModel.ALL_ACTIVITIES) {
                    DailyActivityStatistics(state, number)
                }
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

@Composable
private fun DailyActivityStatistics(state: TrainPilotUiState, number: NumberFormat) {
    val palette = LocalTrainPilotPalette.current
    val lang = state.language
    val refresh = LocalSyncDailyHealth.current
    var explanationOpen by rememberSaveable { mutableStateOf(false) }
    val today = LocalDate.now()
    val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val previous = weekStart.minusWeeks(1)
    val days = state.dailyActivityDays.filter {
        val day = runCatching { LocalDate.parse(it.dateIso) }.getOrNull()
        day != null && !day.isBefore(previous) && !day.isAfter(today)
    }
    fun total(from: LocalDate, until: LocalDate): String {
        val available = days.filter { it.stepsAvailable && it.dateIso >= from.toString() && it.dateIso < until.toString() }
        return if (available.isEmpty()) "—" else number.format(available.sumOf { it.steps })
    }
    Surface(shape = MaterialTheme.shapes.large, color = palette.surfaceCard,
        modifier = Modifier.fillMaxWidth().trainPilotBorder(1.25.dp, palette.sectionBorder, MaterialTheme.shapes.large)
            .testTag("statistics_daily_activity")) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val todayDay = days.firstOrNull { it.dateIso == today.toString() }
            val outside = if (todayDay?.stepsAvailable == true && todayDay.workoutCount == 0)
                number.format(todayDay.steps) else "—"
            listOf(
                listOf(
                    statsTr(lang, "Mai összes lépés", "Today's Total Steps", "Heutige Gesamtschritte", "Total pași azi") to total(today, today.plusDays(1)),
                    statsTr(lang, "Edzésen kívül", "Outside Workouts", "Außerhalb Trainings", "În afara antrenamentelor") to outside
                ),
                listOf(
                    statsTr(lang, "Heti lépések", "Weekly Steps", "Wochenschritte", "Pași săptămânali") to total(weekStart, today.plusDays(1)),
                    statsTr(lang, "Előző hét", "Previous Week", "Vorwoche", "Săptămâna trecută") to total(previous, weekStart)
                )
            ).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { (label, value) ->
                        StatisticsMetricTile(label, value, Modifier.weight(1f))
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
                    "Teljes napi lépések, egy kiválasztott forrásból. Ha az edzésen kívüli rész nem különíthető el, gondolatjelet mutatunk.",
                    "Total daily steps from the selected source. If non-workout steps cannot be separated, a dash is shown.",
                    "Gesamte Tagesschritte aus der gewählten Quelle. Wenn Schritte außerhalb des Trainings nicht trennbar sind, wird ein Strich gezeigt.",
                    "Totalul pașilor zilnici din sursa selectată. Dacă pașii din afara antrenamentului nu pot fi separați, se afișează o linie."
                ),
                color = palette.textSecondary, style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth()) {
                Text(statsTr(lang, "Dátum / forrás", "Date / Source", "Datum / Quelle", "Dată / Sursă"), Modifier.weight(1f), color = palette.textSecondary)
                Text(statsTr(lang, "Összes / edzésen kívül", "Total / Outside workout", "Gesamt / Außerhalb", "Total / În afară"), color = palette.textSecondary)
            }
            days.reversed().forEach { day ->
                HorizontalDivider(color = palette.cardBorder)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(day.dateIso, color = palette.textPrimary)
                        val localizedSource = if (day.stepsSourceLabelHu.contains("Nincs", ignoreCase = true)) {
                            statsTr(lang, "Nincs szinkronizálva", "Not synced", "Nicht synchronisiert", "Nesincronizat")
                        } else day.stepsSourceLabelHu
                        Text(localizedSource, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                    }
                    Text(if (!day.stepsAvailable) "— / —" else
                        "${number.format(day.steps)} / ${if (day.workoutCount == 0) number.format(day.steps) else "—"}", color = palette.textPrimary)
                }
            }
        }
    }
}
