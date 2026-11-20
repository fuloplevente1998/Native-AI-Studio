package com.example.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * 2784: a label that is never cut or abbreviated. It keeps the whole word on one line by stepping the
 * font down (not below [minSize]); only if that is still too wide it wraps to a second line.
 */
@Composable
fun TrainPilotFitText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    minSize: TextUnit = 12.sp
) {
    BoxWithConstraints(modifier) {
        val measurer = rememberTextMeasurer()
        val base = if (fontWeight != null) style.copy(fontWeight = fontWeight) else style
        val maxWidth = constraints.maxWidth
        val fitted = remember(text, base, maxWidth) {
            val start = if (base.fontSize.isSp) base.fontSize.value else 16f
            var size = start
            fun width(s: Float) = measurer.measure(text, base.copy(fontSize = s.sp), maxLines = 1, softWrap = false).size.width
            if (maxWidth != Constraints.Infinity) while (size > minSize.value && width(size) > maxWidth) size -= 0.5f
            size to (maxWidth == Constraints.Infinity || width(size) <= maxWidth)
        }
        Text(text, style = base.copy(fontSize = fitted.first.sp), color = color, textAlign = textAlign,
            maxLines = if (fitted.second) 1 else 2, softWrap = !fitted.second)
    }
}

/** 2784: side-by-side labels share the row by length, so a longer word gets more room (no label below half). */
fun trainPilotLabelWeight(label: String, all: List<String>): Float {
    val average = all.map { it.length }.average().toFloat().coerceAtLeast(1f)
    return (label.length / average).coerceIn(0.6f, 1.6f)
}
