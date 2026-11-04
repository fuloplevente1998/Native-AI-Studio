package com.repforge.app.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** One phone UI hierarchy: page/group > inner panel > control. Not physical CSS pixels. */
object TrainPilotFrames {
    val pageWidth = 1.dp
    val panelWidth = 0.75.dp
    val controlWidth = 0.5.dp

    fun page(line: Color, accent: Color) = BorderStroke(pageWidth, Brush.linearGradient(listOf(
        line.copy(alpha = 0.85f), accent.copy(alpha = 0.45f), line.copy(alpha = 0.85f))))
    fun control(color: Color, selected: Boolean = false) = BorderStroke(controlWidth,
        color.copy(alpha = color.alpha * if (selected) 0.75f else 0.45f))
    fun stroke(width: Dp, color: Color) = BorderStroke(minOf(width, panelWidth), color.copy(alpha = color.alpha * 0.40f))
    fun stroke(width: Dp, brush: Brush): BorderStroke = BorderStroke(minOf(width, panelWidth),
        if (brush is SolidColor) SolidColor(brush.value.copy(alpha = brush.value.alpha * 0.40f)) else brush)
}

/** Migrate existing palette frames without changing shape, padding, content or hit areas. */
fun Modifier.trainPilotBorder(width: Dp, color: Color, shape: Shape = RectangleShape): Modifier =
    border(TrainPilotFrames.stroke(width, color), shape)
fun Modifier.trainPilotBorder(border: BorderStroke, shape: Shape = RectangleShape): Modifier =
    this.border(TrainPilotFrames.stroke(border.width, border.brush), shape)

fun Modifier.trainPilotBorder(width: Dp, brush: Brush, shape: Shape = RectangleShape): Modifier =
    border(TrainPilotFrames.stroke(width, brush), shape)

/** Subtle static edge fade. Uses the existing palette; never changes layout or touch targets. */
fun Modifier.trainPilotSoftGlow(accent: Color, shape: Shape, elevation: Dp = 6.dp): Modifier =
    this.shadow(elevation, shape = shape, clip = false,
        ambientColor = accent.copy(alpha = 0.08f), spotColor = accent.copy(alpha = 0.16f))
