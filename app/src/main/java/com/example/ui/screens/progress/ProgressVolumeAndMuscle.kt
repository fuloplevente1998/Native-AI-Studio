package com.example.ui.screens
import com.example.ui.theme.glassTileBackdrop
import com.example.ui.theme.glassTile

import com.example.ui.components.PageLayoutBlock
import com.example.ui.components.PageLayoutHorizontalRow
import com.example.ui.components.PageLayoutTile
import com.example.ui.components.PageLayoutState
import androidx.compose.runtime.key
import androidx.compose.foundation.layout.widthIn
import com.repforge.app.ui.theme.trainPilotSoftGlow
import com.example.ui.components.trainPilotChartSelection

import com.repforge.app.ui.theme.TrainPilotCardSpacing

import com.example.ui.components.trainPilotAnimatedExpansion
import com.repforge.app.ui.theme.trainPilotClickable
import com.repforge.app.ui.theme.trainPilotBorder

import com.example.model.StudioProgressWindow
import com.example.model.progressPeriodDays

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.AutoAwesome
import com.example.ui.components.AnatomicalMuscleHeatmapCard
import com.example.ui.components.SmallExerciseVideoButton
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState

/**
 * Exact Compact `main` Fejlődés Layout (`Screenshot_2026-10-27-15-00-06-481_com.repforge.app.jpg`):
 * 1. Összes volumen (20 800 kg) with side-by-side 2x2 period selector (1 nap, 7 nap, 30 nap, 3 hó) + bar chart, expandable on tap!
 * 2. 2x2 Compact Expandable Metric Cards (PR rekordok 45, Edzések száma 25, Átlagos volumen 904,3 kg, Átl. edzésidő 45 min)
 * 3. Compact Side-by-Side Izomcsoport-terhelés (Front & Back bodies on left + 6 muscle bars on right)
 * 4. Compact Expandable Fejlődési javaslat card at the bottom
 */
@Composable
internal fun MainProgressVolumeAndMuscleSection(
    uiState: TrainPilotUiState,
    onSelectRange: (Int) -> Unit,
    onSelectVolumeBar: (Int) -> Unit,
    onSelectExpandedStatsCard: (Int) -> Unit,
    onOpenWorkoutDetail: (String) -> Unit,
    layout: PageLayoutState? = null,
    photoBlock: @Composable () -> Unit = {}
) {
    val palette = LocalTrainPilotPalette.current
    val strings = com.example.localization.LocalTrainPilotStrings.current
    var expandedVolumeDetails by remember { mutableStateOf(false) }
    var expandedAdviceDetails by remember { mutableStateOf(false) }

    val today = remember { java.time.LocalDate.now() }
    val compactDateFmt = remember { java.time.format.DateTimeFormatter.ofPattern("MM.dd.") }

    val period = StudioProgressWindow(progressPeriodDays(uiState.selectedProgressRangeIndex), today)
    val periodWorkouts = remember(uiState.loggedWorkouts, period) { period.workouts(uiState.loggedWorkouts) }
    val volumeBars = remember(uiState.loggedWorkouts, period) { period.volumes(uiState.loggedWorkouts) }
    val expandedWeeklyBars = volumeBars
    val loggedTotalKg = periodWorkouts.sumOf { it.totalVolumeKg }
    val volumeChange = period.volumeChangePercent(uiState.loggedWorkouts)
    val totalVolumeText = "%,d kg".format(java.util.Locale.US, loggedTotalKg).replace(',', ' ')
    val dateSubtitle = "${period.start.format(compactDateFmt)} – ${period.end.format(compactDateFmt)}"
    val prRecords = remember(uiState.loggedWorkouts) { loggedPrRecords(uiState.loggedWorkouts) }
    // 2783: the sheet lists exactly the records the tile counts (selected period only).
    val periodPrRecords = prRecords.filter {
        period.contains(runCatching { java.time.LocalDate.parse(it.dateLabel.take(10)) }.getOrNull())
    }
    val prCountValue = periodPrRecords.size.toString()
    val workoutCountValue = periodWorkouts.size.toString()
    val avgVolumeValue = if (periodWorkouts.isEmpty()) "0 kg" else
        "${"%.1f".format(loggedTotalKg.toFloat() / periodWorkouts.size).replace('.', ',')} kg"
    val avgDurationValue = if (periodWorkouts.isEmpty()) "0 min" else
        "${periodWorkouts.sumOf { it.durationMinutes } / periodWorkouts.size} min"

    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        val order = layout?.order ?: listOf("volume", "metrics", "muscles", "advice")
        order.forEach { id ->
            key(id) {
                PageLayoutTile(layout, id, hasHorizontalTiles = id == "metrics") {
                    when (id) {
                        "photos" -> photoBlock()
                        "volume" -> {
                            // 1. EDZÉSVOLUMEN: compact trend + one-row periods; full trend expands in place.
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = palette.glassTile(palette.surfaceCard),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .trainPilotAnimatedExpansion()
                                    .trainPilotBorder(1.dp, palette.primary.copy(alpha = 0.65f), RoundedCornerShape(18.dp))
                                    .testTag("progress_total_volume_card")
                                    .trainPilotSoftGlow(palette.primary, RoundedCornerShape(18.dp), 3.dp).glassTileBackdrop(palette, RoundedCornerShape(18.dp))
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                                    // Header row (clickable to expand/collapse between compact & Screenshot 6 large Volumen trend)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("progress_volume_expand")
                                            .trainPilotClickable(RoundedCornerShape(10.dp)) { expandedVolumeDetails = !expandedVolumeDetails }
                                            .testTag("progress_total_volume_header"),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = strings.totalVolumeTitle,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = palette.textSecondary
                                            )
                                            Text(
                                                text = totalVolumeText,
                                                modifier = Modifier.testTag("progress_volume_total"),
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = palette.textPrimary
                                            )
                                            Text(
                                                text = dateSubtitle,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = palette.textSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column(horizontalAlignment = Alignment.End) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = when {
                                                        volumeChange == null -> "—"
                                                        volumeChange > 0 -> "▲ +$volumeChange%"
                                                        volumeChange < 0 -> "▼ $volumeChange%"
                                                        else -> "0%"
                                                    },
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = when { volumeChange == null || volumeChange == 0L -> palette.textSecondary
                                                        volumeChange > 0 -> palette.success else -> palette.danger },
                                                    modifier = Modifier.testTag("progress_volume_change")
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = if (expandedVolumeDetails) Icons.Default.ExpandMore else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                    contentDescription = trL(uiState.language, "Volumen grafikon lenyitása", "Expand volume chart", "Volumendiagramm öffnen", "Extinde graficul volumului"),
                                                    tint = palette.primary,
                                                    modifier = Modifier.size(18.dp).testTag("progress_volume_arrow")
                                                )
                                            }
                                            if (expandedVolumeDetails) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = strings.comparedToPrevPeriod,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = palette.textSecondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            } else {
                                                // The chart sits beside the total. A selected bar opens the full trend.
                                                Box(Modifier.width(128.dp).height(52.dp).testTag("progress_volume_chart_open")) {
                                                    StudioVolumeChart(volumeBars, period, uiState.selectedVolumeBarIndex, false) { index ->
                                                        onSelectVolumeBar(index)
                                                        expandedVolumeDetails = true
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    val rangeLabels = listOf(
                                        trL(uiState.language, "1 nap", "1 day", "1 Tag", "1 zi"),
                                        trL(uiState.language, "7 nap", "7 days", "7 Tage", "7 zile"),
                                        trL(uiState.language, "30 nap", "30 days", "30 Tage", "30 zile"),
                                        trL(uiState.language, "3 hó", "3 mo", "3 Mon.", "3 luni")
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth().testTag("progress_compact_ranges"),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        rangeLabels.forEachIndexed { index, label ->
                                            val selected = uiState.selectedProgressRangeIndex == index
                                            val shape = RoundedCornerShape(10.dp)
                                            Box(
                                                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                                    .testTag("progress_range_$index")
                                                    .trainPilotClickable(shape) { onSelectRange(index) }
                                                    .trainPilotChartSelection(selected, label),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Surface(
                                                    shape = shape,
                                                    color = if (selected) palette.primary else palette.surfaceElevated,
                                                    modifier = Modifier.fillMaxWidth().heightIn(min = 32.dp)
                                                        .trainPilotBorder(1.dp, if (selected) palette.primary else palette.cardBorder, shape)
                                                        .testTag("progress_range_visual_$index")
                                                ) {
                                                    Box(Modifier.padding(horizontal = 3.dp, vertical = 3.dp), contentAlignment = Alignment.Center) {
                                                        com.example.ui.components.TrainPilotFitText(
                                                            text = label,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (selected) palette.onPrimary else palette.textPrimary
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (expandedVolumeDetails) {
                                        val isWeeklyBucket = period.days >= 30
                                        Spacer(modifier = Modifier.height(14.dp))

                                        // 2. "Volumen trend" Title + "Heti bontás" Pill
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = trL(uiState.language, "Volumen trend", "Volume trend", "Volumentrend", "Trend volum"),
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = palette.textPrimary
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = palette.glassTile(palette.surfaceElevated, elevated = true),
                                                modifier = Modifier.trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(12.dp)).glassTileBackdrop(palette, RoundedCornerShape(12.dp))
                                            ) {
                                                Text(
                                                    text = if (isWeeklyBucket) trL(uiState.language, "Heti bontás", "Weekly breakdown", "Wöchentlich", "Săptămânal") else trL(uiState.language, "Napi bontás", "Daily breakdown", "Täglich", "Zilnic"),
                                                    modifier = Modifier.testTag("progress_volume_bucket_unit").padding(horizontal = 12.dp, vertical = 6.dp),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = palette.textSecondary
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        StudioVolumeChart(volumeBars, period, uiState.selectedVolumeBarIndex, true, onSelectVolumeBar)

                                        Spacer(modifier = Modifier.height(10.dp))

                                        val selectedBar = expandedWeeklyBars.getOrNull(uiState.selectedVolumeBarIndex)
                                        Text(
                                            text = if (selectedBar != null) {
                                                trL(
                                                    uiState.language,
                                                    "Kiválasztott ${if (isWeeklyBucket) "hét" else "nap"} (${selectedBar.first}): ${selectedBar.second} kg összvolumen",
                                                    "Selected ${if (isWeeklyBucket) "week" else "day"} (${selectedBar.first}): ${selectedBar.second} kg total volume",
                                                    "Ausgewählte(r) ${if (isWeeklyBucket) "Woche" else "Tag"} (${selectedBar.first}): ${selectedBar.second} kg Gesamtvolumen",
                                                    "${if (isWeeklyBucket) "Săptămâna selectată" else "Ziua selectată"} (${selectedBar.first}): ${selectedBar.second} kg volum total"
                                                )
                                            } else {
                                                trL(uiState.language, "Válassz egy oszlopot a részletekhez.", "Select a bar for details.", "Wähle einen Balken für Details.", "Selectează o coloană pentru detalii.")
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (selectedBar != null) palette.primary else palette.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                        "metrics" -> {
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                PageLayoutHorizontalRow(layout, "progress_metrics_0", listOf(
                                    PageLayoutBlock("records", strings.prRecordsTitle),
                                    PageLayoutBlock("workouts", strings.workoutCountTitle)
                                ), 6.dp) { metric, modifier ->
                                    if (metric == "records") ExpandableStatSummaryTile(
                                    icon = Icons.Default.EmojiEvents,
                                    title = strings.prRecordsTitle,
                                    value = prCountValue,
                                    dashSuffix = "—",
                                    subtitle = null,
                                    selected = uiState.expandedStatsCardIndex == 0,
                                    onClick = { onSelectExpandedStatsCard(0) },
                                    modifier = modifier
                                ) else ExpandableStatSummaryTile(
                                    icon = Icons.Default.CalendarMonth,
                                    title = strings.workoutCountTitle,
                                    value = workoutCountValue,
                                    dashSuffix = "—",
                                    subtitle = null,
                                    selected = uiState.expandedStatsCardIndex == 1,
                                    onClick = { onSelectExpandedStatsCard(1) },
                                    modifier = modifier
                                )
                                }
                                PageLayoutHorizontalRow(layout, "progress_metrics_1", listOf(
                                    PageLayoutBlock("avg_volume", strings.avgVolumeTitle),
                                    PageLayoutBlock("avg_duration", strings.avgDurationTitle)
                                ), 6.dp) { metric, modifier ->
                                    if (metric == "avg_volume") ExpandableStatSummaryTile(
                                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                                    title = strings.avgVolumeTitle,
                                    value = avgVolumeValue,
                                    dashSuffix = "—",
                                    subtitle = null,
                                    selected = uiState.expandedStatsCardIndex == 2,
                                    onClick = { onSelectExpandedStatsCard(2) },
                                    modifier = modifier
                                ) else ExpandableStatSummaryTile(
                                    icon = Icons.Default.Schedule,
                                    title = strings.avgDurationTitle,
                                    value = avgDurationValue,
                                    dashSuffix = null,
                                    subtitle = null,
                                    selected = uiState.expandedStatsCardIndex == 3,
                                    onClick = { onSelectExpandedStatsCard(3) },
                                    modifier = modifier
                                )
                                }
                            }

                            // Expandable Dropdown Card when any of the 4 tiles is tapped (collapsed by default so everything fits on screen!)
                            // 2771: details open from the bottom instead of pushing the page down.
                            if (uiState.expandedStatsCardIndex in 0..3) com.example.ui.components.TrainPilotScreenSheet(
                                onDismiss = { onSelectExpandedStatsCard(uiState.expandedStatsCardIndex) },
                                testTag = "stats_details_sheet"
                            ) {
                                val compactPrPanel = uiState.expandedStatsCardIndex == 0
                                val prPanelMaxHeight = minOf(420.dp,
                                    (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp * 0.66f).dp)
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = palette.glassTile(palette.surfaceCard),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = TrainPilotCardSpacing.ScreenGutter, end = TrainPilotCardSpacing.ScreenGutter,
                                            bottom = if (compactPrPanel) 8.dp else 16.dp)
                                        .trainPilotBorder(1.dp, palette.primary.copy(alpha = 0.55f), RoundedCornerShape(18.dp)).glassTileBackdrop(palette, RoundedCornerShape(18.dp))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .then(if (compactPrPanel) Modifier.heightIn(max = prPanelMaxHeight) else Modifier)
                                            .verticalScroll(rememberScrollState())
                                            .padding(horizontal = if (compactPrPanel) 10.dp else 12.dp,
                                                vertical = if (compactPrPanel) 6.dp else 12.dp)
                                            .testTag("stats_details_sheet_content"),
                                        verticalArrangement = Arrangement.spacedBy(if (compactPrPanel) 4.dp else 6.dp)
                                    ) {
                                        when (uiState.expandedStatsCardIndex) {
                                            0 -> {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("${strings.prRecordsTitle} ($prCountValue)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                        modifier = Modifier
                                                            .widthIn(min = 48.dp).heightIn(min = 48.dp)
                                                            .testTag("stats_pr_close")
                                                            .trainPilotClickable(RoundedCornerShape(8.dp)) { onSelectExpandedStatsCard(0) }
                                                    ) {
                                                        Text(trL(uiState.language, "Bezárás", "Close", "Schließen", "Închide"), style = MaterialTheme.typography.labelSmall, color = palette.primary)
                                                        Icon(imageVector = Icons.Default.ExpandLess, contentDescription = null, tint = palette.primary, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                                if (periodPrRecords.isEmpty()) {
                                                    Text(
                                                        text = trL(uiState.language, "Még nincs rögzített PR rekord. Fejezz be egy edzést a PR-ek számításához!", "No PR records logged yet. Finish a workout to calculate PRs!", "Noch keine PR-Rekorde erfasst. Beende ein Training zur PR-Berechnung!", "Niciun record PR înregistrat încă. Finalizează un antrenament pentru calculul PR-urilor!"),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = palette.textSecondary
                                                    )
                                                } else {
                                                    periodPrRecords.forEach { pr ->
                                                        val locPrName = pr.localizedExerciseName(uiState.language, uiState.exercises)
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .glassTileBackdrop(palette, RoundedCornerShape(10.dp)).clip(RoundedCornerShape(10.dp))
                                                                .background(palette.glassTile(palette.surfaceElevated, elevated = true))
                                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                                                .testTag("stats_pr_record_row"),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(locPrName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                                                                Text(pr.localizedTypeLabel(uiState.language), style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                                                            }
                                                            SmallExerciseVideoButton(
                                                                exerciseName = locPrName,
                                                                compactIconOnly = true,
                                                                exerciseId = uiState.exercises.firstOrNull { it.nameHu.equals(pr.exerciseNameHu, ignoreCase = true) }?.id
                                                            )
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text("${pr.previousLabel} → ${pr.localizedNewLabel(uiState.language)}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = palette.primary)
                                                        }
                                                    }
                                                }
                                            }
                                            1 -> {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("${strings.workoutCountTitle} ($workoutCountValue)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                        modifier = Modifier.trainPilotClickable(RoundedCornerShape(8.dp)) { onSelectExpandedStatsCard(1) }
                                                    ) {
                                                        Text(trL(uiState.language, "Bezárás", "Close", "Schließen", "Închide"), style = MaterialTheme.typography.labelSmall, color = palette.primary)
                                                        Icon(imageVector = Icons.Default.ExpandLess, contentDescription = null, tint = palette.primary, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                                if (periodWorkouts.isEmpty()) {
                                                    Text(
                                                        text = trL(uiState.language, "Még nincs naplózott edzés.", "No logged workouts yet.", "Noch keine protokollierten Trainings.", "Niciun antrenament înregistrat încă."),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = palette.textSecondary
                                                    )
                                                } else {
                                                    periodWorkouts.forEach { w ->
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .glassTileBackdrop(palette, RoundedCornerShape(10.dp)).clip(RoundedCornerShape(10.dp))
                                                                .background(palette.glassTile(palette.surfaceElevated, elevated = true))
                                                                .trainPilotClickable(RoundedCornerShape(10.dp)) { onOpenWorkoutDetail(w.id) }
                                                                .testTag("progress_workout_${w.id}")
                                                                .padding(8.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Text("${w.dateLabelHu} • ${w.localizedTitle(uiState.language, uiState.programs, uiState.exercises)}", style = MaterialTheme.typography.bodySmall, color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                                                            Text("${w.durationMinutes} min >", style = MaterialTheme.typography.labelSmall, color = palette.primary, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }
                                            2 -> {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("${strings.avgVolumeTitle} • $avgVolumeValue", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                        modifier = Modifier.trainPilotClickable(RoundedCornerShape(8.dp)) { onSelectExpandedStatsCard(2) }
                                                    ) {
                                                        Text(trL(uiState.language, "Bezárás", "Close", "Schließen", "Închide"), style = MaterialTheme.typography.labelSmall, color = palette.primary)
                                                        Icon(imageVector = Icons.Default.ExpandLess, contentDescription = null, tint = palette.primary, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                                if (periodWorkouts.isEmpty()) {
                                                    Text(trL(uiState.language, "Még nincs rögzített súlyzós volumen.", "No logged strength volume yet.", "Noch kein Kraftvolumen protokolliert.", "Niciun volum de forță înregistrat încă."), style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                                                } else {
                                                    periodWorkouts.forEach { w ->
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .glassTileBackdrop(palette, RoundedCornerShape(10.dp)).clip(RoundedCornerShape(10.dp))
                                                                .background(palette.glassTile(palette.surfaceElevated, elevated = true))
                                                                .trainPilotClickable(RoundedCornerShape(10.dp)) { onOpenWorkoutDetail(w.id) }
                                                                .testTag("progress_workout_${w.id}")
                                                                .padding(8.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Text("${w.dateLabelHu} • ${w.localizedTitle(uiState.language, uiState.programs, uiState.exercises)}", style = MaterialTheme.typography.bodySmall, color = palette.textPrimary)
                                                            Text("${w.totalVolumeKg} kg >", style = MaterialTheme.typography.labelMedium, color = palette.primary, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }
                                            else -> {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("${strings.avgDurationTitle} • $avgDurationValue", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                        modifier = Modifier.trainPilotClickable(RoundedCornerShape(8.dp)) { onSelectExpandedStatsCard(3) }
                                                    ) {
                                                        Text(trL(uiState.language, "Bezárás", "Close", "Schließen", "Închide"), style = MaterialTheme.typography.labelSmall, color = palette.primary)
                                                        Icon(imageVector = Icons.Default.ExpandLess, contentDescription = null, tint = palette.primary, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                                if (periodWorkouts.isEmpty()) {
                                                    Text(trL(uiState.language, "Még nincs rögzített edzésidő.", "No logged workout duration yet.", "Noch keine Trainingszeit protokolliert.", "Nicio durată de antrenament înregistrată încă."), style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                                                } else {
                                                    periodWorkouts.forEach { w ->
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .glassTileBackdrop(palette, RoundedCornerShape(10.dp)).clip(RoundedCornerShape(10.dp))
                                                                .background(palette.glassTile(palette.surfaceElevated, elevated = true))
                                                                .trainPilotClickable(RoundedCornerShape(10.dp)) { onOpenWorkoutDetail(w.id) }
                                                                .testTag("progress_workout_${w.id}")
                                                                .padding(8.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Text("${w.dateLabelHu} • ${w.localizedTitle(uiState.language, uiState.programs, uiState.exercises)}", style = MaterialTheme.typography.bodySmall, color = palette.textPrimary)
                                                            Text("${w.durationMinutes} min >", style = MaterialTheme.typography.labelMedium, color = palette.primary, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        "muscles" -> {
                            // 3. COMPACT SIDE-BY-SIDE IZOMCSOPORT-TERHELÉS CARD (Front & Back bodies on left + 6 muscle bars on right)
                            AnatomicalMuscleHeatmapCard(
                                title = strings.muscleLoadTitle,
                                subtitle = trL(uiState.language, "Relatív sorozatterhelés, nem fáradtság.", "Relative set load, not fatigue.", "Relative Satzbelastung, keine Ermüdung.", "Încărcare relativă a seriilor, nu oboseală."),
                                realMuscleLoads = uiState.realMuscleLoads,
                                periodDays = progressPeriodDays(uiState.selectedProgressRangeIndex),
                                onPeriodSelected = { periodDays ->
                                    val rIdx = when (periodDays) {
                                        1 -> 0
                                        7 -> 1
                                        30 -> 2
                                        else -> 3
                                    }
                                    onSelectRange(rIdx)
                                },
                                onOpenWorkout = onOpenWorkoutDetail
                            )
                        }
                        "advice" -> {
                            // 4. COMPACT EXPANDABLE "Fejlődési javaslat" CARD AT BOTTOM (`Screenshot_2026-10-27-15-00-06-481_com.repforge.app.jpg`)
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = palette.glassTile(palette.surfaceCard),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .trainPilotBorder(
                                        width = 1.dp,
                                        color = if (expandedAdviceDetails) palette.primary else palette.cardBorder,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .trainPilotAnimatedExpansion()
                                    .trainPilotClickable(RoundedCornerShape(16.dp)) { expandedAdviceDetails = !expandedAdviceDetails }
                                    .testTag("progress_advice_card").glassTileBackdrop(palette, RoundedCornerShape(16.dp))
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(palette.primary.copy(alpha = 0.16f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = palette.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            val topMuscle = uiState.realMuscleLoads.maxByOrNull { it.percentage }?.takeIf { it.percentage > 0 }
                                            Text(
                                                text = strings.progressAdviceTitle,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = palette.textPrimary
                                            )
                                            Text(
                                                text = if (topMuscle != null) {
                                                    val topName = topMuscle.localizedName(uiState.language)
                                                    val topSets = topMuscle.localizedSetsText(uiState.language)
                                                    trL(
                                                        uiState.language,
                                                        "A legtöbb sorozatterhelést ez az izomcsoport kapta: $topName ($topSets).",
                                                        "Highest set load in this period: $topName ($topSets).",
                                                        "Die meiste Satzbelastung erhielt: $topName ($topSets).",
                                                        "Cea mai mare încărcare pe serii a primit-o: $topName ($topSets)."
                                                    )
                                                } else {
                                                    trL(
                                                        uiState.language,
                                                        "Rögzíts edzéseket az izomcsoport-arányok és egyéni fejlődési javaslatok kiszámításához.",
                                                        "Log workouts to calculate muscle group ratios and personalized progression tips.",
                                                        "Protokolliere Trainings, um Muskelgruppen-Anteile und Progressionstipps zu berechnen.",
                                                        "Înregistrează antrenamente pentru a calcula proporțiile grupelor musculare și recomandările."
                                                    )
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = palette.textSecondary,
                                                maxLines = if (expandedAdviceDetails) 4 else 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Icon(
                                            imageVector = if (expandedAdviceDetails) Icons.Default.ExpandMore else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = palette.primary
                                        )
                                    }

                                    // 2771: tip details open from the bottom instead of pushing the page down.
                                    if (expandedAdviceDetails) com.example.ui.components.TrainPilotScreenSheet(
                                        onDismiss = { expandedAdviceDetails = false },
                                        testTag = "advice_details_sheet"
                                    ) {
                                        val lowMuscle = uiState.realMuscleLoads.minByOrNull { it.percentage }
                                        val latestPr = prRecords.firstOrNull()
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .verticalScroll(rememberScrollState())
                                                .padding(start = TrainPilotCardSpacing.ScreenGutter, end = TrainPilotCardSpacing.ScreenGutter, bottom = 16.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            HorizontalDivider(color = palette.cardBorder)
                                            Text(
                                                text = if (lowMuscle != null && periodWorkouts.isNotEmpty()) {
                                                    val lowName = lowMuscle.localizedName(uiState.language)
                                                    val lowSets = lowMuscle.localizedSetsText(uiState.language)
                                                    trL(
                                                        uiState.language,
                                                        "• Egyensúly javaslat: A(z) $lowName ($lowSets) terhelését érdemes +2 munkasorozattal növelni a következő héten.",
                                                        "• Balance tip: Consider adding +2 working sets for $lowName ($lowSets) next week.",
                                                        "• Balance-Tipp: Erhöhe die Belastung für $lowName ($lowSets) nächste Woche um +2 Arbeitssätze.",
                                                        "• Recomandare echilibru: Crește încărcarea pentru $lowName ($lowSets) cu +2 serii săptămâna viitoare."
                                                    )
                                                } else {
                                                    trL(
                                                        uiState.language,
                                                        "• Egyensúly javaslat: Még nincs rögzített izomcsoport-terhelés az elmúlt időszakban.",
                                                        "• Balance tip: No muscle group load logged in the recent period yet.",
                                                        "• Balance-Tipp: Noch keine Muskelgruppenbelastung im letzten Zeitraum erfasst.",
                                                        "• Recomandare echilibru: Nu există încă încărcare pe grupe musculare în perioada recentă."
                                                    )
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = palette.textPrimary
                                            )
                                            Text(
                                                text = if (latestPr != null) {
                                                    val prExName = latestPr.localizedExerciseName(uiState.language, uiState.exercises)
                                                    val prNewLbl = latestPr.localizedNewLabel(uiState.language)
                                                    val prTypeLbl = latestPr.localizedTypeLabel(uiState.language)
                                                    trL(
                                                        uiState.language,
                                                        "• Legutóbbi csúcsteljesítmény: $prExName ($prNewLbl • $prTypeLbl).",
                                                        "• Latest personal best: $prExName ($prNewLbl • $prTypeLbl).",
                                                        "• Letzte Bestleistung: $prExName ($prNewLbl • $prTypeLbl).",
                                                        "• Ultima performanță de vârf: $prExName ($prNewLbl • $prTypeLbl)."
                                                    )
                                                } else {
                                                    trL(
                                                        uiState.language,
                                                        "• Progresszió: A rögzített sorozatok után itt jelenik meg a következő súlyemelési javaslat.",
                                                        "• Progression: Your next weight progression suggestion will appear here after logged sets.",
                                                        "• Progression: Nach erfassten Sätzen erscheint hier die nächste Gewichtserhöhungsempfehlung.",
                                                        "• Progresie: După seriile înregistrate, aici va apărea următoarea recomandare de creștere a greutății."
                                                    )
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = palette.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ExpandableStatSummaryTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    dashSuffix: String?,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = palette.glassTile(palette.surfaceCard),
        modifier = modifier
            .trainPilotBorder(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) palette.primary else palette.cardBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .trainPilotClickable(RoundedCornerShape(14.dp)) { onClick() }.glassTileBackdrop(palette, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(palette.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = palette.primary,
                    modifier = Modifier.size(15.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        maxLines = 1
                    )
                    if (dashSuffix != null) {
                        Text(
                            text = dashSuffix,
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary
                        )
                    }
                }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = if (selected) Icons.Default.ExpandMore else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = palette.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
