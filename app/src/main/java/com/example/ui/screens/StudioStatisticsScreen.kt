package com.example.ui.screens

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
    StudioStatisticsContent(state, native, onSelectTab, onStepsMode,
        onExercise = { id -> model.selectExercise(id); model.selectCardioActivity(id) },
        onActivity = model::selectCardioActivity, onRetry = model::retryCardio,
        onRefreshRecords = onRefreshRecords)
}

@Composable
internal fun StudioStatisticsContent(state: TrainPilotUiState, native: ProgressUiState,
    onSelectTab: (Int) -> Unit, onStepsMode: (Boolean) -> Unit, onExercise: (String) -> Unit,
    onActivity: (String) -> Unit, onRetry: () -> Unit, onRefreshRecords: () -> Unit) {
    val palette = LocalTrainPilotPalette.current
    val strings = TrainPilotStrings.forLanguage(state.language)
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
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)
            .testTag("studio_statistics_screen"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(strings.personalRecordsSubTab, strings.cardioStatsSubTab).forEachIndexed { index, label ->
                    FilterChip(selected = state.statsSubTab == index, onClick = { onSelectTab(index) },
                        label = { Text(label) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                            .testTag("statistics_subtab_$index"))
                }
            }
            if (state.statsSubTab == 0) {
                Text(strings.personalRecordsSubTab, style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary, modifier = Modifier.testTag("statistics_records_title"))
                Text("Minden naplózott, mért gyakorlat a terhelés típusának megfelelő statisztikával.",
                    style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                when {
                    state.savedRecordsLoading -> Text("Rekordok betöltése…", color = palette.textSecondary)
                    state.savedRecordsError -> {
                        Text("A rekordok nem olvashatók.", color = palette.danger)
                        TextButton(onClick = onRefreshRecords) { Text("Újrapróbálás") }
                    }
                    state.savedExerciseBests.isEmpty() -> Text("Még nincs mentett, befejezett sorozat.", color = palette.textSecondary)
                    else -> state.savedExerciseBests.forEach { record ->
                        val expanded = selectedRecord == record.exerciseId
                        Surface(shape = MaterialTheme.shapes.large, color = palette.surfaceCard,
                            modifier = Modifier.fillMaxWidth().border(1.25.dp, palette.sectionBorder, MaterialTheme.shapes.large)
                                .testTag("statistics_record_${record.exerciseId}")) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable {
                                    selectedRecord = if (expanded) null else record.exerciseId
                                    if (!expanded) onExercise(record.exerciseId)
                                }, verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(if (state.language.name == "HU") record.nameHu else record.nameEn,
                                            color = palette.textPrimary, style = MaterialTheme.typography.titleMedium)
                                        Text(savedRecordLabel(record, number), color = palette.textSecondary,
                                            style = MaterialTheme.typography.bodySmall)
                                    }
                                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = palette.primary)
                                }
                                if (expanded) {
                                    HorizontalDivider(color = palette.cardBorder)
                                    if (record.weightKg != null && native.selectedExerciseId == record.exerciseId) {
                                        Text("Becsült 1RM • Epley • utolsó 90 nap", color = palette.textSecondary)
                                        if (native.strengthLoading) Text("Betöltés…", color = palette.textSecondary)
                                        else if (native.strengthError) Text("Az 1RM-adatsor nem olvasható.", color = palette.danger)
                                        else if (native.exercise1RmTrend.isEmpty()) Text("Nincs érvényes súly/ismétlés alapú 1RM-adat.", color = palette.textSecondary)
                                        else {
                                            Text("Legutóbbi becslés: ${number.format(native.exercise1RmTrend.last().e1rmKg)} kg", color = palette.textPrimary)
                                            TrendCanvas(native.exercise1RmTrend.map { it.e1rmKg })
                                        }
                                    } else if (record.distanceMeters != null || record.measurementType == "distance" || record.repUnit in listOf("km", "m")) {
                                        NativeCardioProgressPanel(native, onActivity, onRetry)
                                    } else Text(savedRecordLabel(record, number), color = palette.textPrimary)
                                }
                            }
                        }
                    }
                }
            } else {
                Text(strings.cardioStatsSubTab, style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)
                OutlinedButton(onClick = { onStepsMode(!state.cardioFilterStepsMode) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (state.cardioFilterStepsMode) "Napi aktivitás / lépések" else "Minden mozgás")
                }
                if (!state.cardioFilterStepsMode) NativeCardioProgressPanel(native, onActivity, onRetry)
                DailyActivityStatistics(state, number)
            }
        }
    }
}

internal fun savedRecordLabel(record: SavedExerciseBest, number: NumberFormat): String = when {
    record.distanceMeters != null -> "Leghosszabb táv: ${number.format(record.distanceMeters / 1000)} km"
    record.seconds != null -> "Legjobb idő: ${record.seconds} mp"
    record.weightKg != null -> "Legnagyobb súly: ${number.format(record.weightKg)} kg"
    record.repetitions != null -> "Legjobb ismétlés: ${record.repetitions}"
    else -> "Nincs használható mért sorozat"
}

@Composable
private fun DailyActivityStatistics(state: TrainPilotUiState, number: NumberFormat) {
    val palette = LocalTrainPilotPalette.current
    val refresh = LocalSyncDailyHealth.current
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
        modifier = Modifier.fillMaxWidth().border(1.25.dp, palette.sectionBorder, MaterialTheme.shapes.large)
            .testTag("statistics_daily_activity")) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Napi aktivitás", color = palette.textPrimary, style = MaterialTheme.typography.titleMedium)
            Text("Teljes napi lépések, egy kiválasztott forrásból. Ha az edzésen kívüli rész nem különíthető el, gondolatjelet mutatunk.",
                color = palette.textSecondary, style = MaterialTheme.typography.bodySmall)
            Text("Mai összes lépés: ${total(today, today.plusDays(1))}", color = palette.textPrimary)
            Text("Heti lépések: ${total(weekStart, today.plusDays(1))}", color = palette.textPrimary)
            Text("Előző hét: ${total(previous, weekStart)}", color = palette.textPrimary)
            Text("Szinkronizált napok: ${days.count { it.stepsAvailable }}/${days.size}", color = palette.textSecondary)
            OutlinedButton(onClick = { refresh(if (state.primaryHealthSourceSamsung) HealthJournalStore.SAMSUNG else HealthJournalStore.HC) },
                enabled = !state.healthSyncBusy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Lépésadatok frissítése") }
            Row(Modifier.fillMaxWidth()) {
                Text("Dátum / forrás", Modifier.weight(1f), color = palette.textSecondary)
                Text("Összes / edzésen kívül", color = palette.textSecondary)
            }
            days.reversed().forEach { day ->
                HorizontalDivider(color = palette.cardBorder)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(day.dateIso, color = palette.textPrimary)
                        Text(day.stepsSourceLabelHu, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                    }
                    Text(if (!day.stepsAvailable) "— / —" else
                        "${number.format(day.steps)} / ${if (day.workoutCount == 0) number.format(day.steps) else "—"}", color = palette.textPrimary)
                }
            }
        }
    }
}
