package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.example.model.LoggedWorkoutEntry
import com.example.model.localWorkoutDate
import com.repforge.app.SavedExerciseBest
import java.time.LocalDate

/** 2783: one value per saved workout of an exercise, oldest first (measured sets only, nothing invented). */
internal data class ExerciseTrendPoint(val date: LocalDate, val value: Double)

internal enum class ExerciseTrendKind { E1RM, REPS, SECONDS, NONE }

internal fun exerciseTrendKind(record: SavedExerciseBest): ExerciseTrendKind = when {
    record.distanceMeters != null || record.measurementType == "distance" || record.repUnit in listOf("km", "m") -> ExerciseTrendKind.NONE
    record.weightKg != null -> ExerciseTrendKind.E1RM
    record.seconds != null -> ExerciseTrendKind.SECONDS
    record.repetitions != null -> ExerciseTrendKind.REPS
    else -> ExerciseTrendKind.NONE
}

internal fun exerciseTrend(record: SavedExerciseBest, workouts: List<LoggedWorkoutEntry>): List<ExerciseTrendPoint> {
    val kind = exerciseTrendKind(record)
    if (kind == ExerciseTrendKind.NONE) return emptyList()
    val names = setOf(record.nameHu, record.nameEn).filter { it.isNotBlank() }.toSet()
    return workouts.mapNotNull { w ->
        val date = w.localWorkoutDate() ?: return@mapNotNull null
        val sets = w.exerciseDetails.filter { it.nameHu in names }.flatMap { it.sets }.filter { it.completed }
        val best = sets.mapNotNull { s ->
            when (kind) {
                ExerciseTrendKind.E1RM -> if (s.weightKg > 0f && s.reps > 0) s.weightKg * (1.0 + s.reps / 30.0) else null
                ExerciseTrendKind.SECONDS -> listOf(s.reps, if (s.leftRecorded) s.leftSec else 0).maxOrNull()?.takeIf { it > 0 }?.toDouble()
                ExerciseTrendKind.REPS -> s.reps.takeIf { it > 0 }?.toDouble()
                ExerciseTrendKind.NONE -> null
            }
        }.maxOrNull() ?: return@mapNotNull null
        ExerciseTrendPoint(date, best)
    }.sortedBy { it.date }
}

internal fun exerciseTrendUnit(kind: ExerciseTrendKind, lang: String): String = when (kind) {
    ExerciseTrendKind.E1RM -> "kg"
    ExerciseTrendKind.SECONDS -> when (lang.take(2)) { "en", "ro" -> "s"; "de" -> "Sek."; else -> "mp" }
    else -> when (lang.take(2)) { "en" -> "reps"; "de" -> "Wdh."; "ro" -> "rep."; else -> "ism." }
}

/** 2785: chart info line, e.g. "2026. nov. 18. · 92,5 kg". */
internal fun exerciseTrendPointLabel(point: ExerciseTrendPoint, kind: ExerciseTrendKind, lang: String): String {
    val rounded = Math.round(point.value * 10) / 10.0
    val value = (if (kind == ExerciseTrendKind.E1RM && rounded % 1.0 != 0.0) "%.1f" else "%.0f").format(rounded).replace('.', ',')
    return "${com.example.ui.components.chartDateLabel(point.date, lang)} · $value ${exerciseTrendUnit(kind, lang)}"
}

/** "▲ +2,5 kg", "± 0", "▼ −3 ism." – first vs last of the shown points; null with fewer than two. */
internal fun exerciseTrendChange(points: List<ExerciseTrendPoint>, kind: ExerciseTrendKind, lang: String): String? {
    if (points.size < 2) return null
    val delta = points.last().value - points.first().value
    val unit = exerciseTrendUnit(kind, lang)
    val rounded = Math.round(delta * 10) / 10.0
    if (rounded == 0.0) return "± 0"
    val text = (if (rounded % 1.0 == 0.0) "%.0f" else "%.1f").format(kotlin.math.abs(rounded)).replace('.', ',')
    return if (rounded > 0) "▲ +$text $unit" else "▼ −$text $unit"
}

@Composable
internal fun ExerciseSparkline(points: List<ExerciseTrendPoint>, color: Color, modifier: Modifier = Modifier.size(width = 76.dp, height = 28.dp)) {
    Canvas(modifier) {
        if (points.size < 2) return@Canvas
        val min = points.minOf { it.value }; val max = points.maxOf { it.value }
        val span = (max - min).takeIf { it > 0.0 } ?: 1.0
        val step = size.width / (points.size - 1)
        val pos = points.mapIndexed { i, p -> Offset(i * step, (size.height - 3f) - ((p.value - min) / span).toFloat() * (size.height - 6f)) }
        // 2784: curved line (controls stay inside each pair's range, no invented peaks).
        val path = androidx.compose.ui.graphics.Path()
        pos.forEachIndexed { i, o ->
            if (i == 0) path.moveTo(o.x, o.y) else { val p = pos[i - 1]; val mx = (p.x + o.x) / 2f; path.cubicTo(mx, p.y, mx, o.y, o.x, o.y) }
        }
        drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(color, radius = 3.dp.toPx(), center = pos.last())
    }
}
