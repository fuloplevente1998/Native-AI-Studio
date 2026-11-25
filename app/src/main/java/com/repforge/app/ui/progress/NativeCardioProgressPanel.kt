package com.repforge.app.ui.progress

import com.example.ui.components.chartDateLabel
import com.example.ui.components.trainPilotChartMarkColor
import com.example.ui.components.trainPilotChartSelection
import com.repforge.app.ui.theme.TrainPilotFrames
import com.repforge.app.ui.theme.trainPilotBorder
import com.repforge.app.ui.theme.trainPilotClickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import com.repforge.app.ui.theme.TrainPilotTextButton as TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.repforge.app.R
import com.repforge.app.ui.text.nativeLocalizedName
import com.repforge.app.ui.text.nativeResources
import com.repforge.app.ui.theme.LocalTrainPilotColors
import java.util.Locale
import kotlin.math.roundToLong

/** The real Room-backed Cardio section of the native Progress page. */
@Composable
internal fun NativeCardioProgressPanel(state: ProgressUiState, onRetry: () -> Unit) {
    val colors = LocalTrainPilotColors.current
    val context = LocalContext.current
    val strings = remember(context, state.language) { nativeResources(context, state.language) }
    val model = state.cardio
    var recordsOpen by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().testTag("progress_cardio"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = colors.card),
        border = com.repforge.app.ui.theme.trainPilotFrameStroke(TrainPilotFrames.stroke(1.25.dp, colors.emphasisBorder))) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            when {
                state.cardioLoading -> Text(strings.getString(R.string.cardio_progress_loading),
                    color = colors.muted, modifier = Modifier.testTag("progress_cardio_loading"))
                state.cardioError -> {
                    Text(strings.getString(R.string.cardio_progress_error),
                        color = colors.danger, modifier = Modifier.testTag("progress_cardio_error"))
                    TextButton(onClick = onRetry,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                            .testTag("progress_cardio_retry")) {
                        Text(strings.getString(R.string.cardio_progress_retry))
                    }
                }
                model == null || model.totalSetCount == 0 -> {
                    Text(strings.getString(R.string.cardio_progress_empty),
                        color = colors.muted, modifier = Modifier.testTag("progress_cardio_empty"))
                }
                else -> {
                    // 2782: same layout as Steps – three tiles, a tappable 8-week chart with a fixed
                    // selected-week line, then the records (two rows, the rest on demand).
                    fun tr(hu: String, en: String, de: String, ro: String) = when (state.language.lowercase(Locale.ROOT).take(2)) {
                        "en" -> en; "de" -> de; "ro" -> ro; else -> hu }
                    val current = model.weeks.lastOrNull()
                    val previous = model.weeks.getOrNull(model.weeks.size - 2)
                    fun sessions(n: Int?) = n?.let { tr("$it edzés", "$it workouts", "$it Trainings", "$it antrenamente") } ?: "—"
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatisticsMetricTile(tr("Ez a hét", "This week", "Diese Woche", "Săpt. aceasta") + " · " + sessions(current?.workoutCount),
                            "${kilometers(model.currentWeek.meters, state.language)} km", Modifier.weight(1f), "progress_cardio_week_distance")
                        StatisticsMetricTile(tr("Előző hét", "Previous week", "Vorwoche", "Săpt. trecută") + " · " + sessions(previous?.workoutCount),
                            "${kilometers(model.previousWeek.meters, state.language)} km", Modifier.weight(1f), "progress_cardio_previous_week_distance")
                        StatisticsMetricTile(tr("Összesen", "Total", "Gesamt", "Total"),
                            "${kilometers(model.totals.meters, state.language)} km", Modifier.weight(1f), "progress_cardio_total_distance")
                    }
                    Text(strings.getString(R.string.cardio_progress_eight_weeks), color = colors.muted, fontSize = 13.sp)
                    if (model.weeks.isNotEmpty()) {
                        var selectedWeek by remember(model.weeks.size, model.weeks.last().weekStartDate) {
                            mutableIntStateOf(model.weeks.lastIndex)
                        }
                        fun distanceLabel(week: CardioWeekStats) =
                            if (week.meters > 0.0) "${kilometers(week.meters, state.language)} km"
                            else tr("nincs edzés", "no workout", "kein Training", "fără antrenament")
                        val week = model.weeks[selectedWeek.coerceIn(0, model.weeks.lastIndex)]
                        val end = week.weekStartDate.plusDays(6)
                        Column(Modifier.fillMaxWidth().background(colors.card2, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp).testTag("progress_cardio_selected_week")) {
                            Row(Modifier.fillMaxWidth()) {
                                Text("${week.weekStartDate.monthValue}.${"%02d".format(week.weekStartDate.dayOfMonth)}.–" +
                                    "${end.monthValue}.${"%02d".format(end.dayOfMonth)}.", color = colors.text,
                                    fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                Text(distanceLabel(week),
                                    color = if (week.meters > 0.0) colors.accentText else colors.muted,
                                    fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.testTag("progress_cardio_selected_week_value"))
                            }
                            if (week.workoutCount > 0) Text("${sessions(week.workoutCount)} · ${cardioDuration(week.seconds)} · " +
                                "${NativeCardioModel.tp107FormatPace(week.pace)} min/km", color = colors.muted, fontSize = 11.sp)
                        }
                        CardioEightWeekChart(model.weeks, selectedWeek, state.language, ::distanceLabel) { selectedWeek = it }
                    }
                    if (model.totals.timeOnlySeconds > 0) {
                        Text(strings.getString(R.string.cardio_progress_time_only,
                            cardioDuration(model.totals.timeOnlySeconds)),
                            color = colors.muted, fontSize = 12.sp,
                            modifier = Modifier.testTag("progress_cardio_time_only"))
                    }
                    Text(strings.getString(R.string.cardio_progress_records),
                        color = colors.text, fontWeight = FontWeight.SemiBold)
                    if (model.records.isEmpty()) Text(
                        strings.getString(R.string.cardio_progress_no_records),
                        color = colors.muted, modifier = Modifier.testTag("progress_cardio_no_records"))
                    val activityNames = model.activities.associate { activity ->
                        activity.exerciseId to cardioActivityName(state.language, activity)
                    }
                    val displayedRecords = if (recordsOpen) model.records else model.records.take(2)
                    displayedRecords.forEach { record ->
                        Column(Modifier.fillMaxWidth()
                            .background(colors.card2, MaterialTheme.shapes.large)
                            .trainPilotBorder(1.dp, colors.line, MaterialTheme.shapes.large).padding(10.dp)
                            .testTag("progress_cardio_record_${record.exerciseId}_${record.roundedDistanceMeters}")) {
                            Text("${activityNames[record.exerciseId] ?: record.exerciseId} · " +
                                strings.getString(R.string.cardio_progress_record_distance,
                                    record.roundedDistanceMeters, record.count),
                                color = colors.text, fontWeight = FontWeight.SemiBold)
                            Text(strings.getString(R.string.cardio_progress_record_best,
                                cardioDuration(record.best.seconds),
                                NativeCardioModel.tp107FormatPace(record.best.pace)), color = colors.text)
                            record.second?.let { second ->
                                Text(strings.getString(R.string.cardio_progress_record_second,
                                    cardioDuration(second.seconds),
                                    NativeCardioModel.tp107FormatPace(second.pace)),
                                    color = colors.muted, fontSize = 12.sp)
                            }
                        }
                    }
                    if (model.records.size > 2) TextButton(
                        onClick = { recordsOpen = !recordsOpen },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                            .testTag("progress_cardio_expand")) {
                        Text(if (recordsOpen) "▴ " + strings.getString(R.string.cardio_progress_records)
                            else "▾ " + strings.getString(R.string.cardio_progress_records) + " (${model.records.size})")
                    }
                }
            }
        }
    }
}

/** Pure model falls back to ID for missing translations; HU is the real fallback. */
internal fun cardioActivityName(language: String, activity: CardioActivityOption): String {
    val hu = activity.nameHu.takeIf { it.isNotBlank() && it != activity.exerciseId }
        ?: activity.exerciseId
    fun translated(text: String): String =
        text.takeIf { it.isNotBlank() && it != activity.exerciseId }.orEmpty()
    return nativeLocalizedName(language, hu, translated(activity.nameEn),
        translated(activity.nameDe), translated(activity.nameRo))
}

@Composable
private fun CardioEightWeekChart(
    weeks: List<CardioWeekStats>,
    selected: Int,
    language: String,
    distanceLabel: (CardioWeekStats) -> String,
    onSelect: (Int) -> Unit
) {
    val c = LocalTrainPilotColors.current
    val upper = (weeks.maxOfOrNull { it.meters } ?: 0.0).coerceAtLeast(1.0)
    Row(Modifier.fillMaxWidth().height(110.dp).testTag("progress_cardio_chart"), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        weeks.forEachIndexed { i, w ->
            val on = i == selected
            val mark = trainPilotChartMarkColor(on, c.accentText, c.muted)
            val label = "${chartDateLabel(w.weekStartDate, language)} – ${chartDateLabel(w.weekStartDate.plusDays(6), language)} · ${distanceLabel(w)}"
            Column(Modifier.weight(1f).fillMaxHeight().trainPilotClickable(RoundedCornerShape(6.dp)) { onSelect(i) }
                .trainPilotChartSelection(on, label).testTag("progress_cardio_week_$i"),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.BottomCenter) {
                    // An empty week stays an empty slot, never a 0 bar.
                    if (w.meters <= 0.0) Box(Modifier.fillMaxWidth().height(2.dp).background(mark))
                    else Box(Modifier.fillMaxWidth().fillMaxHeight((w.meters / upper).toFloat().coerceIn(0.04f, 1f))
                        .background(mark, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
                }
                Text("${w.weekStartDate.dayOfMonth}.${w.weekStartDate.monthValue}.", color = if (on) c.accentText else c.muted,
                    fontSize = 9.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.Center, maxLines = 1)
            }
        }
    }
}

private fun kilometers(meters: Double, language: String): String =
    String.format(Locale.forLanguageTag(language), "%.2f", meters / 1000.0)

private fun cardioDuration(seconds: Double): String {
    val safe = if (seconds.isFinite() && seconds > 0.0) seconds.roundToLong() else 0L
    return String.format(Locale.ROOT, "%02d:%02d:%02d",
        safe / 3600L, safe / 60L % 60L, safe % 60L)
}
