package com.example.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalTrainPilotPalette

/** Use the active palette's contrast for a neutral measurement and its accent for selection. */
fun trainPilotChartMarkColor(isSelected: Boolean, accent: Color, neutral: Color): Color {
    if (isSelected) return accent
    val gray = 0.2126f * neutral.red + 0.7152f * neutral.green + 0.0722f * neutral.blue
    return Color(gray, gray, gray, neutral.alpha * 0.7f)
}

fun Modifier.trainPilotChartSelection(isSelected: Boolean, label: String): Modifier = semantics {
    selected = isSelected
    contentDescription = label
}

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
@Composable
fun Modifier.trainPilotChartTap(
    key: Any?,
    nearest: (Float, Float) -> Int,
    selectedLabel: String? = null,
    onSelect: (Int) -> Unit
): Modifier {
    val currentNearest = rememberUpdatedState(nearest)
    val currentOnSelect = rememberUpdatedState(onSelect)
    return pointerInput(key) {
        detectTapGestures { tap -> currentOnSelect.value(currentNearest.value(tap.x, size.width.toFloat())) }
    }.semantics {
        if (selectedLabel != null) stateDescription = selectedLabel
    }
}

/** Localised medium date for chart info lines ("2026. nov. 18.", "Nov 18, 2026"). */
fun chartDateLabel(date: java.time.LocalDate, lang: String): String =
    date.format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)
        .withLocale(java.util.Locale.forLanguageTag(lang.lowercase().take(2))))

/** Keep a compact minimum while allowing selected measurements to wrap at large text sizes. */
@Composable
fun TrainPilotChartInfoLine(text: String, modifier: Modifier = Modifier) {
    val p = LocalTrainPilotPalette.current
    Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = p.primary,
        modifier = modifier.fillMaxWidth().heightIn(min = 22.dp).semantics { liveRegion = LiveRegionMode.Polite })
}
