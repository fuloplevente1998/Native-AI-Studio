package com.example.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalTrainPilotPalette

/**
 * 2785: every chart is tappable. The tapped bar/point is highlighted and a fixed-height info line shows
 * its date/time, value and unit; the default selection is the latest point.
 */

/** Nearest of [count] evenly spaced points (first at x = 0, last at x = width) to the tap at [x]. */
fun chartNearestIndex(x: Float, width: Float, count: Int): Int {
    if (count <= 1 || width <= 0f) return 0
    return Math.round(x.coerceIn(0f, width) / width * (count - 1)).coerceIn(0, count - 1)
}

/** Nearest point when points sit at their own horizontal fractions (0..1), e.g. a time axis. */
fun chartNearestByFraction(x: Float, width: Float, fractions: List<Float>): Int {
    if (fractions.isEmpty() || width <= 0f) return 0
    val f = x.coerceIn(0f, width) / width
    return fractions.indices.minByOrNull { kotlin.math.abs(fractions[it] - f) } ?: 0
}

/** Tap on a line chart selects the nearest point; [nearest] maps (x, width) to an index. */
fun Modifier.trainPilotChartTap(key: Any?, nearest: (Float, Float) -> Int, onSelect: (Int) -> Unit): Modifier =
    pointerInput(key) { detectTapGestures { o -> onSelect(nearest(o.x, size.width.toFloat())) } }

/** Localised medium date for chart info lines ("2026. nov. 18.", "Nov 18, 2026"). */
fun chartDateLabel(date: java.time.LocalDate, lang: String): String =
    date.format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)
        .withLocale(java.util.Locale.forLanguageTag(lang.lowercase().take(2))))

/** Fixed-height, one-line info line under a chart (the layout never jumps when the selection changes). */
@Composable
fun TrainPilotChartInfoLine(text: String, modifier: Modifier = Modifier) {
    val p = LocalTrainPilotPalette.current
    Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = p.primary,
        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = modifier.fillMaxWidth().height(22.dp))
}
