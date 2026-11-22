package com.repforge.app.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.ClipOp
import com.example.ui.theme.LocalTrainPilotPalette
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

/** Static outer edge lighting for the eight Glow palettes. */
@Composable
fun Modifier.trainPilotSoftGlow(accent: Color, shape: Shape, elevation: Dp = 6.dp): Modifier {
    if (!LocalTrainPilotPalette.current.hasGlow) return this
    return drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val edge = Path().apply { addOutline(outline) }
        val width = elevation.coerceIn(3.dp, 6.dp).toPx()
        val intensity = if (elevation >= 4.dp) 1f else 0.72f
        onDrawBehind {
            clipPath(edge, ClipOp.Difference) {
                for (layer in 6 downTo 1) {
                    drawPath(edge, accent.copy(alpha = accent.alpha * intensity * if (layer == 1) 0.38f else 0.055f),
                        style = Stroke(width = width * layer / 3f))
                }
            }
        }
    }
}
