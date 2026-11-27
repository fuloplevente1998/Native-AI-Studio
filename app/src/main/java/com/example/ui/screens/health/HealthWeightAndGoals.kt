package com.example.ui.screens

import com.example.ui.components.TrainPilotMenuItem

import com.example.ui.components.healthDetailsToggle
import com.example.ui.theme.glassTile
import com.example.ui.theme.trainPilotGlassRim
import com.example.ui.theme.GlassMaterialRole
import com.example.ui.theme.glassCardFill
import com.example.ui.theme.trainPilotGlassMaterial

import com.repforge.app.ui.theme.TrainPilotCardSpacing

import com.repforge.app.ui.theme.trainPilotClickable
import com.repforge.app.ui.theme.trainPilotBorder

import androidx.compose.material3.LinearProgressIndicator

import com.example.model.localWorkoutDate

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import com.example.ui.components.trainPilotChartMarkColor
import com.example.ui.components.trainPilotChartTap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.localization.LocalizedStrings
import com.repforge.app.ui.theme.TrainPilotTextButton as TextButton
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState

/** 2783: Today tile → journal record type to open directly, or null when the tile has no data (or no journal type). */
internal fun todayTileJournalType(key: String, state: com.example.ui.viewmodel.TrainPilotUiState): String? = when (key) {
    "sleep" -> "SleepSessionRecord".takeIf { state.sleepHoursDecimal != "—" }
    "calories" -> com.repforge.app.ui.health.NativeDetailedHealthMetrics.recordType("activeCalories")
        .takeIf { "activeCalories" in state.dailyHealthAvailableFields }
    "steps" -> com.repforge.app.ui.health.NativeDetailedHealthMetrics.recordType("steps")
        .takeIf { "steps" in state.dailyHealthAvailableFields }
    // Pulse keeps its own daily pulse-trend view.
    else -> null
}

/** 2785: measurements of the selected weight period only (0 = 7 d, 1 = 30 d, 2 = 3 mo, 3 = 1 yr, else all), oldest first. */
internal fun weightMeasurementsInRange(entries: List<WeightMeasurementEntry>, rangeIndex: Int, today: java.time.LocalDate): List<WeightMeasurementEntry> {
    val cutoff = when (rangeIndex) {
        0 -> today.minusDays(7)
        1 -> today.minusDays(30)
        2 -> today.minusMonths(3)
        3 -> today.minusYears(1)
        else -> null
    }
    return entries.filter { cutoff == null || !it.parsedDate.isBefore(cutoff) }.sortedBy { it.parsedDate }
}

internal fun recentWeightTrend(entries: List<WeightMeasurementEntry>): List<WeightMeasurementEntry> =
    entries.sortedByDescending { it.parsedDate }.take(7).asReversed()

/** 2786: the big weekly goals view (opened from the compact block in "Today" or from Home). Real data only. */
@Composable
internal fun HealthWeeklyGoalsPanel(uiState: TrainPilotUiState) {
    val palette = LocalTrainPilotPalette.current
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    val today = java.time.LocalDate.now()
    val cachedDays = uiState.weeklyHealthDays.associateBy { it.dateIso }
    val goalDays = (6 downTo 0).map { offset ->
        val date = today.minusDays(offset.toLong())
        val workouts = uiState.loggedWorkouts.filter { it.localWorkoutDate() == date }
        val cached = cachedDays[date.toString()]
            ?: com.example.ui.viewmodel.WeeklyHealthDaySummary(dateIso = date.toString(), dayLabelHu = date.toString(), steps = 0, activeCaloriesKcal = 0, sleepLabel = "—", workoutMinutes = 0, workoutCount = 0, sourceLabelHu = "Nincs rögzített adat")
        cached.copy(workoutMinutes = workouts.sumOf { it.durationMinutes.coerceAtLeast(0) }, workoutCount = workouts.size)
    }
    val weeklyStepsTarget = (uiState.stepsGoal.coerceAtLeast(1) * 7)
    val weeklyStepsCurrent = goalDays.filter { it.stepsAvailable }.sumOf { it.steps }
    val weeklyStepsPct = kotlin.math.round((weeklyStepsCurrent.toDouble() / weeklyStepsTarget.toDouble()) * 100.0).toInt()
    val weeklyMinutesCurrent = goalDays.sumOf { it.workoutMinutes }
    val primaryProviderName = if (uiState.primaryHealthSourceSamsung) "Samsung Health" else "Health Connect"

    val goal = com.example.ui.components.weeklyGoalSummary(uiState, today)
    val lang = uiState.language
    val langCode = lang.name.lowercase()
    val minUnit = tr(lang, "perc", "min", "Min.", "min")
    val nf = java.text.NumberFormat.getIntegerInstance(java.util.Locale.forLanguageTag(langCode))
    val dates = goalDays.map { java.time.LocalDate.parse(it.dateIso) }
    val weekWorkouts = goalDays.sumOf { it.workoutCount }

    @Composable
    fun Section(title: String, tag: String, progress: @Composable () -> Unit, content: @Composable () -> Unit) {
        var expanded by rememberSaveable { mutableStateOf(false) }
        Surface(shape = RoundedCornerShape(18.dp), color = palette.glassTile(palette.background.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth().trainPilotGlassRim(palette, RoundedCornerShape(18.dp), GlassMaterialRole.Tile)
                .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(18.dp)).testTag(tag)) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = if (expanded) 8.dp else 4.dp),
                verticalArrangement = Arrangement.spacedBy(if (expanded) 4.dp else 0.dp)) {
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .healthDetailsToggle(expanded, lang, RoundedCornerShape(10.dp)) { expanded = !expanded }
                    .testTag("${tag}_toggle"), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                        color = palette.textPrimary, modifier = Modifier.weight(1f))
                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = palette.primary)
                }
                if (expanded) content()
                progress()
            }
        }
    }

    @Composable
    fun GoalProgress(summary: String, fraction: Float, tag: String) {
        Column(Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(tr(lang, "Célteljesítés", "Goal progress", "Zielfortschritt", "Progresul obiectivului"),
                    style = MaterialTheme.typography.labelMedium, color = palette.textSecondary,
                    modifier = Modifier.weight(1f))
                Text(summary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
                    color = palette.textPrimary, modifier = Modifier.testTag(tag))
            }
            LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth(),
                color = palette.primary, trackColor = palette.surfaceElevated)
        }
    }

    // The week remains interactive. Only the selected day's chart value is visible by default;
    // complete measured/journal history is available in the existing bottom sheet.
    val workouts = uiState.loggedWorkouts.filter { w -> w.localWorkoutDate()?.let { it in dates } == true }
        .sortedByDescending { it.localWorkoutDate() }
    var historySheet by rememberSaveable { mutableStateOf<String?>(null) }
    Section(tr(lang, "Edzések", "Workouts", "Trainings", "Antrenamente"), "health_weekly_workouts_section", progress = {
        GoalProgress(
            "${goal.workoutsDone} / ${goal.workoutsTarget} " +
                if (goal.monthly) tr(lang, "ebben a hónapban", "this month", "diesen Monat", "luna aceasta")
                else tr(lang, "ezen a héten", "this week", "diese Woche", "săptămâna aceasta"),
            goal.workoutFraction, "health_weekly_workouts_section_summary")
    }) {
        Text(tr(lang, "Utolsó 7 nap: $weekWorkouts edzés · $weeklyMinutesCurrent perc", "Last 7 days: $weekWorkouts workouts · $weeklyMinutesCurrent min",
                "Letzte 7 Tage: $weekWorkouts Trainings · $weeklyMinutesCurrent Min.", "Ultimele 7 zile: $weekWorkouts antrenamente · $weeklyMinutesCurrent min"),
            style = MaterialTheme.typography.labelMedium, color = palette.textPrimary, modifier = Modifier.testTag("health_weekly_minutes_total"))
        com.example.ui.components.WeeklyDayBars(dates, goalDays.map { d -> d.workoutMinutes.toDouble().takeIf { d.workoutCount > 0 } },
            label = { date, v -> todayTileWeekLabel("workout", date, v, langCode) }, tag = "health_weekly_minutes_bars",
            lang = langCode, showScale = true, detailIcon = Icons.Default.FitnessCenter)

        if (workouts.isEmpty()) {
            Text(tr(lang, "Az elmúlt 7 napban nincs edzés.", "No workouts in the last 7 days.", "Keine Trainings in den letzten 7 Tagen.", "Niciun antrenament în ultimele 7 zile."),
                style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
        } else {
            TextButton(onClick = { historySheet = "workouts" },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("health_weekly_workouts_history")) {
                Text(tr(lang, "Edzéstartalom és előzmények ›", "Workout details and history ›", "Trainingsdetails und Verlauf ›", "Detalii și istoric antrenamente ›"))
            }
        }
    }

    val measuredDays = goalDays.count { it.stepsAvailable }
    Section(tr(lang, "Lépések", "Steps", "Schritte", "Pași"), "health_weekly_steps_section", progress = {
        GoalProgress(
            if (measuredDays == 0) "—" else "${nf.format(weeklyStepsCurrent)} / ${nf.format(weeklyStepsTarget)} · $weeklyStepsPct%",
            if (measuredDays == 0) 0f else (weeklyStepsPct / 100f).coerceIn(0f, 1f),
            "health_weekly_steps_section_summary")
    }) {
        Text(tr(lang, "Utolsó 7 nap · $measuredDays/7 nap adat · $primaryProviderName", "Last 7 days · $measuredDays/7 days of data · $primaryProviderName",
                "Letzte 7 Tage · $measuredDays/7 Tage mit Daten · $primaryProviderName", "Ultimele 7 zile · $measuredDays/7 zile cu date · $primaryProviderName"),
            style = MaterialTheme.typography.labelSmall, color = palette.textSecondary, modifier = Modifier.testTag("health_weekly_steps_total"))
        if (measuredDays == 0) {
            Text(todayTileWeekLabel("steps", today, null, langCode),
                style = MaterialTheme.typography.bodySmall, color = palette.textSecondary,
                modifier = Modifier.testTag("health_weekly_steps_empty"))
        } else {
            com.example.ui.components.WeeklyDayBars(dates, goalDays.map { d -> d.steps.toDouble().takeIf { d.stepsAvailable } },
                label = { date, v -> todayTileWeekLabel("steps", date, v, langCode) + if (date == today) " · " + tr(lang, "napi cél", "daily goal", "Tagesziel", "obiectiv zilnic") + " ${nf.format(uiState.stepsGoal)}" else "" },
                tag = "health_weekly_steps_bars", lang = langCode, showScale = true,
                detailIcon = Icons.AutoMirrored.Filled.DirectionsWalk)
        }

        if (measuredDays > 0) {
            TextButton(onClick = { historySheet = "steps" },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("health_weekly_steps_history")) {
                Text(tr(lang, "Lépéselőzmények megtekintése ›", "View step history ›", "Schrittverlauf anzeigen ›", "Vezi istoricul pașilor ›"))
            }
        }
    }

    // The caller's scrolling page never grows when a history list opens.
    // The shared modal already supports bottom-up entrance, swipe dismissal and system back.
    if (historySheet != null) com.example.ui.components.TrainPilotScreenSheet(
        onDismiss = { historySheet = null }, testTag = "health_weekly_history_sheet"
    ) {
        Column(Modifier.fillMaxWidth()
            .heightIn(max = (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp * 0.68f).dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (historySheet == "workouts") tr(lang, "Edzéselőzmények", "Workout history", "Trainingsverlauf", "Istoric antrenamente")
                    else tr(lang, "Napi lépések", "Daily steps", "Tagesschritte", "Pași zilnici"),
                    style = MaterialTheme.typography.titleMedium, color = palette.textPrimary, modifier = Modifier.weight(1f))
                if (palette.isLiquidGlass) {
                    com.example.ui.components.TrainPilotRedCloseButton(onClick = { historySheet = null }, testTag = "health_weekly_history_close")
                } else IconButton(onClick = { historySheet = null }, modifier = Modifier.testTag("health_weekly_history_close")) {
                    Icon(Icons.Default.Close, contentDescription = tr(lang, "Bezárás", "Close", "Schließen", "Închide"), tint = palette.textPrimary)
                }
            }
            if (historySheet == "workouts") {
                workouts.forEach { w ->
                    HorizontalDivider(color = palette.cardBorder)
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        .testTag("health_weekly_workout_row_${w.id}"), verticalAlignment = Alignment.CenterVertically) {
                        Text(w.localWorkoutDate()?.let { com.example.ui.components.chartDateLabel(it, langCode) }.orEmpty(),
                            style = MaterialTheme.typography.labelMedium, color = palette.textSecondary, modifier = Modifier.weight(1f))
                        Text("${w.localizedTitle(lang, uiState.programs, uiState.exercises)} · ${w.durationMinutes.coerceAtLeast(0)} $minUnit",
                            style = MaterialTheme.typography.bodySmall, color = palette.textPrimary, maxLines = 2,
                            overflow = TextOverflow.Ellipsis)
                    }
                }
            } else {
                goalDays.asReversed().forEach { day ->
                    HorizontalDivider(color = palette.cardBorder)
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).testTag("health_weekly_steps_${day.dateIso}"),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(com.example.ui.components.chartDateLabel(java.time.LocalDate.parse(day.dateIso), langCode),
                            style = MaterialTheme.typography.labelMedium, color = palette.textSecondary, modifier = Modifier.weight(1f))
                        Text(if (day.stepsAvailable) "${nf.format(day.steps)} ${tr(lang, "lépés", "steps", "Schritte", "pași")}"
                            else tr(lang, "nincs adat", "no data", "keine Daten", "fără date"),
                            style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                    }
                }
            }
        }
    }
  }
}

@Composable
internal fun WeightLogBody(
    uiState: TrainPilotUiState,
    expanded: Boolean,
    strings: com.example.localization.LocalizedStrings,
    chronological: List<WeightMeasurementEntry>,
    newestFirst: List<WeightMeasurementEntry>,
    dateFormatter: java.time.format.DateTimeFormatter,
    onToggle: () -> Unit,
    onSelectRange: (Int) -> Unit,
    onAdd: () -> Unit,
    onEdit: (WeightMeasurementEntry) -> Unit,
    onPhoto: (WeightMeasurementEntry) -> Unit,
    onDelete: (WeightMeasurementEntry) -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val lang = uiState.language
    val kg: (Float) -> String = { "${"%.1f".format(it).replace('.', ',')}" }
    Spacer(Modifier.height(8.dp))
    if (expanded) {
        val labels = listOf(tr(lang, "7 nap", "7 days", "7 Tage", "7 zile"), tr(lang, "30 nap", "30 days", "30 Tage", "30 zile"),
            tr(lang, "3 hó", "3 mo", "3 Mon.", "3 luni"), tr(lang, "1 év", "1 yr", "1 Jahr", "1 an"), tr(lang, "Mind", "All", "Alle", "Toate"))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            labels.forEachIndexed { idx, label ->
                com.example.ui.components.TrainPilotFilterChip(selected = uiState.selectedWeightRangeIndex == idx, onClick = { onSelectRange(idx) }, label = { Text(label, maxLines = 1) },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("health_weight_range_$idx"))
            }
        }
    }
    Surface(shape = RoundedCornerShape(14.dp), color = palette.glassTile(palette.surfaceCard.copy(alpha = 0.7f), elevated = true),
        modifier = Modifier.fillMaxWidth().trainPilotGlassRim(palette, RoundedCornerShape(14.dp), GlassMaterialRole.Tile)
            .trainPilotBorder(1.dp, palette.cardBorder.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .trainPilotClickable(RoundedCornerShape(14.dp)) { if (!expanded) onToggle() }
            .testTag(if (expanded) "health_weight_chart_large" else "health_weight_chart_compact")) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            val latest = newestFirst.firstOrNull()
            if (latest == null) {
                Box(Modifier.fillMaxWidth().heightIn(min = 48.dp), contentAlignment = Alignment.Center) {
                    Text(tr(lang, "Még nincs rögzített testsúlymérés.", "No weight entries recorded yet.", "Noch keine Gewichtsmessung.", "Nicio greutate înregistrată."),
                        style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                }
            } else if (expanded && chronological.size < 2) {
                val measurement = chronological.singleOrNull() ?: latest
                val last = "${measurement.dateLabel} · ${measurement.weightLabel}"
                Box(Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("health_weight_range_empty"), contentAlignment = Alignment.Center) {
                    Text(if (chronological.isEmpty()) tr(lang, "Nincs mérés ebben az időszakban · utolsó: $last", "No entries in this period · latest: $last",
                            "Keine Messung in diesem Zeitraum · letzte: $last", "Nicio măsurătoare în această perioadă · ultima: $last")
                        else tr(lang, "1 mérés ebben az időszakban: $last", "1 entry in this period: $last", "1 Messung in diesem Zeitraum: $last", "1 măsurătoare în această perioadă: $last"),
                        style = MaterialTheme.typography.bodySmall, color = palette.textSecondary, textAlign = TextAlign.Center)
                }
            } else {
                val pts = chronological.map { it.weightValue }
                var selectedPoint by remember(chronological) { androidx.compose.runtime.mutableIntStateOf(chronological.lastIndex) }
                val selectedDotCenter = palette.surfaceCard
                val minW = (pts.minOrNull() ?: 0f) - 0.5f
                val maxW = (pts.maxOrNull() ?: 1f) + 0.5f
                Row(Modifier.fillMaxWidth()) {
                    if (expanded) Column(Modifier.height(150.dp).padding(end = 6.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        listOf(maxW, (maxW + minW) / 2f, minW).forEach {
                            Text(kg(it), style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                        }
                    }
                    val lineColor = trainPilotChartMarkColor(!expanded, palette.primary, palette.textSecondary)
                    val grid = palette.cardBorder
                    val selectedLabel = chronological.getOrNull(selectedPoint)?.let { "${it.dateLabel} · ${it.weightLabel}" }
                    val tapModifier = if (expanded) Modifier.testTag("health_weight_chart_canvas")
                        .trainPilotChartTap(pts.size, { x, w -> com.example.ui.components.chartNearestIndex(x, w, pts.size) },
                            selectedLabel = selectedLabel) { selectedPoint = it }
                    else Modifier
                    Canvas(
                        Modifier.weight(1f).height(if (expanded) 150.dp else 44.dp)
                            .then(if (expanded) tapModifier else Modifier.testTag("health_weight_chart_compact_canvas"))
                    ) {
                        val inset = 6.dp.toPx()
                        val h = size.height - inset * 2
                        val offsets = pts.mapIndexed { idx, w ->
                            val x = if (pts.size > 1) idx.toFloat() / (pts.size - 1) * size.width else size.width / 2f
                            Offset(x, inset + h - ((w - minW) / (maxW - minW).coerceAtLeast(0.1f)) * h)
                        }
                        if (expanded) for (i in 0..2) { val y = inset + h * i / 2f; drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx()) }
                        val path = Path()
                        offsets.forEachIndexed { i, pt ->
                            if (i == 0) path.moveTo(pt.x, pt.y) else { val prev = offsets[i - 1]; val cx = (prev.x + pt.x) / 2f; path.cubicTo(cx, prev.y, cx, pt.y, pt.x, pt.y) }
                        }
                        if (offsets.size > 1) {
                            val fill = Path().apply { addPath(path); lineTo(offsets.last().x, size.height); lineTo(offsets.first().x, size.height); close() }
                            drawPath(fill, androidx.compose.ui.graphics.Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.25f), lineColor.copy(alpha = 0f))))
                        }
                        drawPath(path, lineColor, style = Stroke(width = (if (expanded) 3f else 2.5f).dp.toPx(), cap = StrokeCap.Round))
                        if (expanded) {
                            offsets.forEach { drawCircle(lineColor, radius = 3.5.dp.toPx(), center = it) }
                            offsets.getOrNull(selectedPoint)?.let { o ->
                                drawLine(palette.primary.copy(alpha = 0.55f), Offset(o.x, 0f), Offset(o.x, size.height), strokeWidth = 1.5.dp.toPx())
                                drawCircle(palette.primary, radius = 6.5.dp.toPx(), center = o)
                                drawCircle(selectedDotCenter, radius = 3.dp.toPx(), center = o)
                            }
                        } else drawCircle(lineColor, radius = 3.5.dp.toPx(), center = offsets.last())
                    }
                }
                val dates = if (expanded) chronological.takeLast(5) else listOfNotNull(chronological.firstOrNull(),
                    chronological.getOrNull(chronological.size / 2), chronological.lastOrNull()).distinct()
                Row(Modifier.fillMaxWidth().padding(start = if (expanded) 30.dp else 0.dp, top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    dates.forEach { entry ->
                        Text(entry.parsedDate.format(dateFormatter), style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                    }
                }
                if (expanded) chronological.getOrNull(selectedPoint)?.let { entry ->
                    com.example.ui.components.TrainPilotChartInfoLine("${entry.dateLabel} · ${entry.weightLabel}",
                        Modifier.padding(top = 4.dp).testTag("health_weight_chart_info"))
                }
            }
        }
    }
    if (!expanded) return
    Spacer(Modifier.height(10.dp))
    // Body & Fitness is presented directly beneath the six Today tiles, not hidden in Weight.
    if (newestFirst.isEmpty()) {
        Text(strings.notEnoughRecentData, style = MaterialTheme.typography.bodyMedium, color = palette.textSecondary)
    } else {
        var all by rememberSaveable { mutableStateOf(false) }
        Text(strings.recentMeasurementsTitle, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = palette.textSecondary)
        (if (all) newestFirst else newestFirst.take(3)).forEach { entry ->
            var menu by remember(entry.id) { mutableStateOf(false) }
            HorizontalDivider(color = palette.cardBorder)
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("health_weight_row_${entry.id}"), verticalAlignment = Alignment.CenterVertically) {
                Text(entry.weightLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.textPrimary, modifier = Modifier.weight(1f))
                Text(entry.dateLabel, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                if (entry.photoCount > 0) {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        contentDescription = tr(lang, "Csatolt fotó", "Attached photo", "Angehängtes Foto", "Fotografie atașată"),
                        tint = palette.primary,
                        modifier = Modifier.size(18.dp).testTag("health_weight_photo_count_${entry.id}")
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("${entry.photoCount}", style = MaterialTheme.typography.labelSmall, color = palette.primary)
                    Spacer(Modifier.width(4.dp))
                }
                Box {
                    IconButton(onClick = { menu = true }, modifier = Modifier.testTag("health_weight_menu_${entry.id}")) {
                        Icon(Icons.Default.MoreVert, contentDescription = tr(lang, "Műveletek", "Actions", "Aktionen", "Acțiuni"), tint = palette.textSecondary)
                    }
                    com.example.ui.components.TrainPilotDropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        TrainPilotMenuItem(text = { Text(strings.editAction) }, onClick = { menu = false; onEdit(entry) },
                            modifier = Modifier.testTag("health_weight_edit_${entry.id}"))
                        TrainPilotMenuItem(
                            text = { Text(tr(lang, "Fotó hozzáadása", "Add photo", "Foto hinzufügen", "Adaugă fotografie")) },
                            onClick = { menu = false; onPhoto(entry) },
                            modifier = Modifier.testTag("health_weight_photo_${entry.id}")
                        )
                        TrainPilotMenuItem(text = { Text(strings.deleteAction, color = palette.danger) }, onClick = { menu = false; onDelete(entry) },
                            modifier = Modifier.testTag("health_weight_delete_${entry.id}"))
                    }
                }
            }
        }
        if (newestFirst.size > 3) TextButton(onClick = { all = !all }, modifier = Modifier.heightIn(min = 48.dp).testTag("health_weight_all_toggle")) {
            Text(if (all) tr(lang, "▴ Kevesebb", "▴ Less", "▴ Weniger", "▴ Mai puțin")
                else tr(lang, "▾ Összes mérés (${newestFirst.size})", "▾ All entries (${newestFirst.size})", "▾ Alle Messungen (${newestFirst.size})", "▾ Toate (${newestFirst.size})"))
        }
    }
    Spacer(Modifier.height(6.dp))
    Surface(shape = RoundedCornerShape(14.dp), color = palette.glassCardFill(palette.surfaceCard),
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .trainPilotGlassMaterial(palette, RoundedCornerShape(14.dp), GlassMaterialRole.Button)
            .trainPilotBorder(1.dp, palette.primary.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .trainPilotClickable(RoundedCornerShape(14.dp)) { onAdd() }.testTag("health_add_weight_button")) {
        Box(Modifier.padding(12.dp), contentAlignment = Alignment.Center) {
            Text(strings.newMeasurementAction, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.primary)
        }
    }
}
