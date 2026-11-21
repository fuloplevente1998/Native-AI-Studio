package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AppLanguage
import com.example.model.WorkoutGoalPeriod
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState
import com.repforge.app.ui.theme.trainPilotBorder
import com.repforge.app.ui.theme.trainPilotClickable
import com.repforge.app.ui.theme.trainPilotCombinedClickable
import java.time.LocalDate

/**
 * 2786: one shared "weekly goals" summary for Home (compact card) and Health (inside "Today", opens big).
 * Only measured values: steps are summed from days that really have step data; no data = null ("—"), never 0.
 */
data class WeeklyGoalSummary(
    val workoutsDone: Int,
    val workoutsTarget: Int,
    val monthly: Boolean,
    /** Sum of the last 7 days' measured steps, or null when none of those days has step data. */
    val steps: Int?,
    val stepsTarget: Int,
    val stepDays: Int
) {
    val workoutFraction: Float get() = if (workoutsTarget <= 0) 0f else (workoutsDone.toFloat() / workoutsTarget).coerceIn(0f, 1f)
    val stepFraction: Float get() = steps?.let { (it.toFloat() / stepsTarget.coerceAtLeast(1)).coerceIn(0f, 1f) } ?: 0f
    /** Actual progress may exceed 100%; only the visual bar is capped. */
    val stepPercent: Int? get() = steps?.let { kotlin.math.round(it.toDouble() * 100.0 / stepsTarget.coerceAtLeast(1)).toInt() }
}

fun weeklyGoalSummary(state: TrainPilotUiState, today: LocalDate = LocalDate.now()): WeeklyGoalSummary {
    val week = (6 downTo 0).map { today.minusDays(it.toLong()).toString() }.toSet()
    val measured = state.weeklyHealthDays.filter { it.dateIso in week && it.stepsAvailable }.distinctBy { it.dateIso }
    return WeeklyGoalSummary(
        workoutsDone = state.workoutGoalCompleted,
        workoutsTarget = state.workoutGoalTarget,
        monthly = state.workoutGoalPeriod == WorkoutGoalPeriod.MONTHLY,
        steps = measured.takeIf { it.isNotEmpty() }?.sumOf { it.steps },
        stepsTarget = state.stepsGoal.coerceAtLeast(1) * 7,
        stepDays = measured.size
    )
}

private fun wgTr(lang: AppLanguage, hu: String, en: String, de: String, ro: String) = when (lang) {
    AppLanguage.HU -> hu; AppLanguage.EN -> en; AppLanguage.DE -> de; AppLanguage.RO -> ro
}

@Composable
internal fun Modifier.healthDetailsToggle(
    expanded: Boolean,
    language: AppLanguage,
    shape: Shape,
    onClick: () -> Unit
): Modifier {
    val action = if (expanded) {
        wgTr(language, "Részletek bezárása", "Close details", "Details schließen", "Închide detaliile")
    } else {
        wgTr(language, "Részletek megnyitása", "Open details", "Details öffnen", "Deschide detaliile")
    }
    return trainPilotCombinedClickable(shape, onClickLabel = action, onClick = onClick)
}

internal fun weeklyGoalNumber(value: Int, lang: AppLanguage): String =
    java.text.NumberFormat.getIntegerInstance(java.util.Locale.forLanguageTag(lang.code)).format(value)

/**
 * Home landing card with goal bars, or a collapsible goals header when [expanded] is provided.
 * The whole block is one tap target ([onOpen]); [onGoals] opens the existing goals dialog.
 */
@Composable
fun WeeklyGoalsCompact(
    state: TrainPilotUiState,
    framed: Boolean,
    onOpen: () -> Unit,
    onGoals: (() -> Unit)?,
    modifier: Modifier = Modifier,
    expanded: Boolean? = null
) {
    val p = LocalTrainPilotPalette.current
    val lang = state.language
    val monthly = state.workoutGoalPeriod == WorkoutGoalPeriod.MONTHLY
    val shape = RoundedCornerShape(if (framed) 16.dp else 12.dp)
    Surface(shape = shape, color = if (framed) p.surface else p.surfaceElevated.copy(alpha = 0.6f),
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp)
            .then(if (framed) Modifier.trainPilotBorder(1.dp, p.cardBorder, shape) else Modifier)
            .then(
                if (expanded == null) Modifier.trainPilotClickable(shape, onClick = onOpen)
                else Modifier.healthDetailsToggle(expanded, lang, shape, onOpen)
            )) {
        Row(Modifier.padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (monthly) wgTr(lang, "Havi célok", "Monthly goals", "Monatsziele", "Obiective lunare")
                        else wgTr(lang, "Heti célok", "Weekly goals", "Wochenziele", "Obiective săptămânale"),
                        style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = p.textPrimary,
                        modifier = Modifier.weight(1f))
                    if (expanded != null) Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null,
                        tint = p.primary, modifier = Modifier.size(18.dp))
                }
                if (expanded == null) {
                    val s = weeklyGoalSummary(state)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val period = if (s.monthly) wgTr(lang, "Edzés (hó)", "Workouts (mo)", "Trainings (Mon.)", "Antr. (lună)")
                            else wgTr(lang, "Edzés", "Workouts", "Trainings", "Antrenamente")
                        WeeklyGoalBar(period, "${s.workoutsDone} / ${s.workoutsTarget}", s.workoutFraction, Modifier.weight(1f).testTag("weekly_goals_workouts"))
                        WeeklyGoalBar(wgTr(lang, "Lépés · 7 nap", "Steps · 7 days", "Schritte · 7 Tage", "Pași · 7 zile"),
                            s.stepPercent?.let { "$it%" } ?: "—", s.stepFraction, Modifier.weight(1f).testTag("weekly_goals_steps"))
                    }
                }
            }
            if (onGoals != null && expanded != false) Surface(shape = RoundedCornerShape(12.dp), color = p.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(if (expanded == null) 44.dp else 48.dp).trainPilotBorder(1.dp, p.primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .trainPilotClickable(RoundedCornerShape(12.dp)) { onGoals() }.testTag("weekly_goals_settings")) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Tune, contentDescription = wgTr(lang, "Célok beállítása", "Set goals", "Ziele festlegen", "Setează obiective"),
                        tint = p.primary, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun WeeklyGoalBar(label: String, value: String, fraction: Float, modifier: Modifier) {
    val p = LocalTrainPilotPalette.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = p.textSecondary, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = p.textPrimary, maxLines = 1)
        }
        Box(Modifier.fillMaxWidth().height(7.dp).background(p.surfaceElevated, RoundedCornerShape(4.dp))) {
            if (fraction > 0f) Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(p.primary, RoundedCornerShape(4.dp)))
        }
    }
}

/**
 * 2786: 7 tappable day columns with a fixed info line (default: today). [values] null = no data that day.
 */
@Composable
fun WeeklyDayBars(days: List<LocalDate>, values: List<Double?>, label: (LocalDate, Double?) -> String, tag: String,
    lang: String = "hu") {
    val p = LocalTrainPilotPalette.current
    if (values.none { it != null }) return
    val max = values.filterNotNull().maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
    var sel by remember(tag, days.lastOrNull()) { mutableIntStateOf(values.lastIndex) }
    Row(Modifier.fillMaxWidth().height(64.dp).testTag(tag), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.Bottom) {
        values.forEachIndexed { i, v ->
            val on = i == sel
            val color = trainPilotChartMarkColor(on, p.primary, p.textSecondary)
            Box(
                Modifier.weight(1f).fillMaxHeight()
                    .trainPilotClickable(RoundedCornerShape(6.dp)) { sel = i }
                    .trainPilotChartSelection(on, label(days[i], v))
                    .testTag("${tag}_$i"),
                contentAlignment = Alignment.BottomCenter
            ) {
                if (v == null) Box(Modifier.fillMaxWidth().height(2.dp).background(color))
                else Box(Modifier.fillMaxWidth().fillMaxHeight((v / max).toFloat().coerceIn(0.05f, 1f))
                    .background(color, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)))
            }
        }
    }
    // 2788: short day names under the columns (H, K, Sze … / Mon, Tue …).
    val locale = java.util.Locale.forLanguageTag(lang.take(2))
    Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        days.forEachIndexed { i, d ->
            Text(d.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, locale).trimEnd('.'),
                style = MaterialTheme.typography.labelSmall, color = if (i == sel) p.primary else p.textSecondary,
                maxLines = 1, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.weight(1f))
        }
    }
    Spacer(Modifier.height(2.dp))
    TrainPilotChartInfoLine(label(days[sel], values[sel]), Modifier.testTag("${tag}_info"))
}
