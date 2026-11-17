package com.example.ui.screens

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import java.util.Locale

internal fun studioTimerLabel(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    return if (safe >= 3600) String.format(Locale.ROOT, "%02d:%02d:%02d", safe / 3600, safe / 60 % 60, safe % 60)
    else String.format(Locale.ROOT, "%02d:%02d", safe / 60, safe % 60)
}

@Composable
internal fun StudioTimerReadout(seconds: Int, style: TextStyle = MaterialTheme.typography.displayLarge,
    fontWeight: FontWeight = FontWeight.Bold, color: Color, modifier: Modifier = Modifier) {
    val label = studioTimerLabel(seconds)
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier) {
        // Measure the actual bundled font instead of estimating digit widths. Preserve
        // the requested large size whenever it fits; scale line height with the font.
        val requested = style.copy(fontWeight = fontWeight, fontFeatureSettings = "tnum")
        val measured = measurer.measure(AnnotatedString(label), style = requested,
            maxLines = 1, softWrap = false)
        val available = with(density) { maxWidth.toPx() }
        val ratio = if (measured.size.width > 0) (available / measured.size.width).coerceIn(0f, 1f) else 1f
        val fitted = requested.copy(fontSize = (requested.fontSize.value * ratio).coerceAtLeast(1f).sp,
            lineHeight = if (requested.lineHeight.isSp) (requested.lineHeight.value * ratio).sp else requested.lineHeight)
        Text(label, style = fitted, fontWeight = fontWeight, color = color,
            maxLines = 1, softWrap = false)
    }
}
