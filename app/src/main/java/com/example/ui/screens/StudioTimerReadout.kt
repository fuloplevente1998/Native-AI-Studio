package com.example.ui.screens

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
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
    val scale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier) {
        // Keep all digits visible, including hours, under system font scaling.
        val size = (maxWidth.value / (label.length * .7f) / scale).coerceIn(1f, style.fontSize.value.takeIf { it.isFinite() && it > 0 } ?: 56f).sp
        Text(label, style = style.copy(fontSize = size, fontFeatureSettings = "tnum"),
            fontWeight = fontWeight, color = color, maxLines = 1, softWrap = false)
    }
}
