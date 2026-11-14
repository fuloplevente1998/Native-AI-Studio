package com.repforge.app.ui.progress

import com.repforge.app.ui.theme.TrainPilotFrames
import com.repforge.app.ui.theme.trainPilotBorder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Surface

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
internal fun NativeCardioProgressPanel(state: ProgressUiState,
    onSelectActivity: (String) -> Unit, onRetry: () -> Unit
) {
    val colors = LocalTrainPilotColors.current
    val context = LocalContext.current
    val strings = remember(context, state.language) { nativeResources(context, state.language) }
    val model = state.cardio
    var visibleRecordCount by remember(state.selectedCardioId) { mutableIntStateOf(12) }
    Card(Modifier.fillMaxWidth().testTag("progress_cardio"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = colors.card),
        border = TrainPilotFrames.stroke(1.25.dp, colors.emphasisBorder)) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            LazyRow(Modifier.fillMaxWidth().testTag("progress_cardio_choices"),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item(key = "all") {
                    CardioChoice(strings.getString(R.string.cardio_progress_all),
                        state.selectedCardioId.isEmpty(), "progress_cardio_all") {
                        onSelectActivity(NativeCardioModel.ALL_ACTIVITIES)
                    }
                }
                items(model?.activities ?: state.cardioActivities, key = { it.exerciseId }) { activity ->
                    CardioChoice(cardioActivityName(state.language, activity),
                        activity.exerciseId == state.selectedCardioId,
                        "progress_cardio_activity_${activity.exerciseId}") {
                        onSelectActivity(activity.exerciseId)
                    }
                }
            }
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
                    Text(strings.getString(R.string.cardio_progress_all_time),
                        color = colors.text, fontWeight = FontWeight.SemiBold)
                    CardioMetrics(kilometers(model.totals.meters, state.language),
                        cardioDuration(model.totals.seconds), NativeCardioModel.tp107FormatPace(model.totals.pace),
                        "progress_cardio_total")
                    Text(strings.getString(R.string.cardio_progress_current_week),
                        color = colors.text, fontWeight = FontWeight.SemiBold)
                    CardioMetrics(kilometers(model.currentWeek.meters, state.language),
                        cardioDuration(model.currentWeek.seconds),
                        NativeCardioModel.tp107FormatPace(model.currentWeek.pace),
                        "progress_cardio_week")
                    Text(strings.getString(R.string.cardio_progress_previous_week),
                        color = colors.text, fontWeight = FontWeight.SemiBold)
                    CardioMetrics(kilometers(model.previousWeek.meters, state.language),
                        cardioDuration(model.previousWeek.seconds),
                        NativeCardioModel.tp107FormatPace(model.previousWeek.pace),
                        "progress_cardio_previous_week")
                    Text(strings.getString(R.string.cardio_progress_eight_weeks),
                        color = colors.text, fontWeight = FontWeight.SemiBold)
                    CardioEightWeekChart(model.weeks)
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
                    val displayedRecords = model.records.take(visibleRecordCount)
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
                    if (model.records.size > visibleRecordCount) TextButton(
                        onClick = { visibleRecordCount = (visibleRecordCount + 12).coerceAtMost(model.records.size) },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                            .testTag("progress_cardio_expand")) {
                        Text(strings.getString(R.string.cardio_progress_more,
                            (model.records.size - visibleRecordCount).coerceAtMost(12)))
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
private fun CardioChoice(label: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) },
        modifier = Modifier.heightIn(min = 48.dp).testTag(tag))
}

@Composable
private fun CardioMetrics(distance: String, duration: String, pace: String, tag: String) {
    val labels = listOf("km", "hh:mm:ss", "min/km")
    val values = listOf(distance, duration, pace)
    val tags = listOf("distance", "duration", "pace")
    BoxWithConstraints(Modifier.fillMaxWidth().testTag(tag)) {
        val columns = if (maxWidth < 420.dp) 2 else 3
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            values.indices.toList().chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { i ->
                        StatisticsMetricTile(labels[i], values[i], Modifier.weight(1f), "${tag}_${tags[i]}")
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun CardioEightWeekChart(weeks: List<CardioWeekStats>) {
    val c = LocalTrainPilotColors.current
    Column(Modifier.fillMaxWidth().testTag("progress_cardio_chart")) {
        Canvas(Modifier.fillMaxWidth().height(124.dp)) {
            if (weeks.isEmpty()) return@Canvas
            val upper = (weeks.maxOfOrNull { it.meters } ?: 0.0).coerceAtLeast(1.0)
            val width = size.width / weeks.size
            weeks.forEachIndexed { i, week ->
                val height = (week.meters / upper).coerceIn(0.0, 1.0).toFloat() * size.height
                if (height > 0f) drawRoundRect(
                    color = if (i == weeks.lastIndex) c.accentText else c.accent2,
                    topLeft = Offset(width * (i + 0.2f), size.height - height),
                    size = Size(width * 0.6f, height),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()))
            }
        }
        Row(Modifier.fillMaxWidth()) {
            weeks.forEach { w ->
                Text("${w.weekStartDate.dayOfMonth}.${w.weekStartDate.monthValue}.",
                    modifier = Modifier.weight(1f), color = c.muted,
                    fontSize = 9.sp, textAlign = TextAlign.Center)
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
