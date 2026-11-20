package com.example.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    // A plain Text (works inside intrinsic-size rows): on overflow the font steps down until it fits;
    // drawing waits until the final size, so nothing flickers.
    val base = if (fontWeight != null) style.copy(fontWeight = fontWeight) else style
    val start = if (base.fontSize.isSp) base.fontSize.value else 16f
    var size by remember(text, start) { mutableFloatStateOf(start) }
    var wrap by remember(text, start) { mutableStateOf(false) }
    var ready by remember(text, start) { mutableStateOf(false) }
    Text(text, modifier = modifier.drawWithContent { if (ready) drawContent() },
        style = base.copy(fontSize = size.sp), color = color, textAlign = textAlign,
        maxLines = if (wrap) 2 else 1, softWrap = wrap,
        onTextLayout = { layout ->
            when {
                wrap -> ready = true
                layout.didOverflowWidth && size > minSize.value -> size = (size - 0.5f).coerceAtLeast(minSize.value)
                layout.didOverflowWidth -> wrap = true
                else -> ready = true
            }
        })
}

/** 2784: side-by-side labels share the row by length, so a longer word gets more room (no label below half). */
fun trainPilotLabelWeight(label: String, all: List<String>): Float {
    val average = all.map { it.length }.average().toFloat().coerceAtLeast(1f)
    return (label.length / average).coerceIn(0.6f, 1.6f)
}
