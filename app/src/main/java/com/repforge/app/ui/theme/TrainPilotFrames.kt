package com.repforge.app.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.ClipOp
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.theme.selectionColors
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
    composed { if (liquidGlassDropsFrame(LocalTrainPilotPalette.current, color)) Modifier
        else Modifier.border(TrainPilotFrames.stroke(width, color), shape) }
fun Modifier.trainPilotBorder(border: BorderStroke, shape: Shape = RectangleShape): Modifier =
    composed { if (liquidGlassDropsStroke(LocalTrainPilotPalette.current, border)) Modifier
        else Modifier.border(TrainPilotFrames.stroke(border.width, border.brush), shape) }

fun Modifier.trainPilotBorder(width: Dp, brush: Brush, shape: Shape = RectangleShape): Modifier =
    composed { if (liquidGlassDropsStroke(LocalTrainPilotPalette.current, BorderStroke(width, brush))) Modifier
        else Modifier.border(TrainPilotFrames.stroke(width, brush), shape) }

/**
 * 2809/2: a frame drawn exactly as given (`Modifier.border`) in every legacy palette and custom theme,
 * and only through the single Liquid Glass gate ([liquidGlassDropsStroke]) in the glass family.
 */
fun Modifier.trainPilotFrame(border: BorderStroke, shape: Shape): Modifier =
    composed { if (liquidGlassDropsStroke(LocalTrainPilotPalette.current, border)) Modifier else Modifier.border(border, shape) }

fun Modifier.trainPilotFrame(width: Dp, color: Color, shape: Shape): Modifier =
    composed { if (liquidGlassDropsFrame(LocalTrainPilotPalette.current, color)) Modifier else Modifier.border(width, color, shape) }

/** 2809/2: the same gate for `border =` parameters (Surface, Card, buttons, chips): null = no frame. */
@Composable
fun trainPilotFrameStroke(border: BorderStroke?): BorderStroke? =
    if (liquidGlassDropsStroke(LocalTrainPilotPalette.current, border)) null else border

/**
 * 2808 #13: Liquid Glass palettes have no hairline frames – the glass rim/edge light defines the shape.
 * Only neutral structural frames are dropped (card/section border, text-derived outlines);
 * semantic frames (accent/selection, danger, success, warning, save/delete action outlines) stay so state never depends on the backdrop.
 */
fun liquidGlassDropsFrame(palette: com.example.ui.theme.TrainPilotPalette, color: Color): Boolean {
    if (!palette.isLiquidGlass) return false
    // 2810/4: text-derived outlines are neutral even if a lifted accent ends up at the same colour.
    if (color.copy(alpha = 1f) == palette.textPrimary.copy(alpha = 1f) && palette.primary.copy(alpha = 1f) != color.copy(alpha = 1f)) return true
    val semantic = listOf(palette.primary, palette.selectionColors().border, palette.danger, palette.success, palette.warning,
        TrainPilotActionAppearance.DARK_SAVE_OUTLINE, TrainPilotActionAppearance.DARK_NEGATIVE_OUTLINE)
    if (semantic.any { it.copy(alpha = 1f) == color.copy(alpha = 1f) }) return false
    return true
}

/**
 * 2809/2: the single decision for every stroke. Solid strokes follow [liquidGlassDropsFrame]; gradient
 * strokes are the decorative page/section frames (line + accent blend) and vanish in Liquid Glass.
 * Legacy palettes and custom themes never drop a frame.
 */
fun liquidGlassDropsStroke(palette: com.example.ui.theme.TrainPilotPalette, border: BorderStroke?): Boolean {
    if (border == null || !palette.isLiquidGlass) return false
    val brush = border.brush
    return if (brush is SolidColor) liquidGlassDropsFrame(palette, brush.value) else true
}

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
