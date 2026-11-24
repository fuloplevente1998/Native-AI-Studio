package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalTrainPilotPalette

/**
 * Custom Smartwatch (Wear OS) Icon Badge matching Mockup 3/4 #2 & Mockup 4/4 #2.
 */
@Composable
fun MockupWatchGraphicBadge(
    modifier: Modifier = Modifier.size(44.dp)
) {
    val palette = LocalTrainPilotPalette.current
    val gold = palette.primary
    val green = palette.success
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Top & bottom watch straps
        drawRoundRect(
            color = Color(0xFF334155),
            topLeft = Offset(w * 0.34f, h * 0.06f),
            size = Size(w * 0.32f, h * 0.88f),
            cornerRadius = CornerRadius(8f, 8f)
        )
        // Watch crown on right
        drawRoundRect(
            color = gold,
            topLeft = Offset(w * 0.82f, h * 0.36f),
            size = Size(w * 0.10f, h * 0.16f),
            cornerRadius = CornerRadius(4f, 4f)
        )
        // Watch case & screen
        drawCircle(color = Color(0xFF1E293B), radius = w * 0.36f, center = Offset(w * 0.50f, h * 0.50f))
        drawCircle(
            color = gold,
            radius = w * 0.36f,
            center = Offset(w * 0.50f, h * 0.50f),
            style = Stroke(width = 2.dp.toPx())
        )
        // Mini ECG pulse line on watch face
        val pulsePath = Path().apply {
            moveTo(w * 0.28f, h * 0.52f)
            lineTo(w * 0.40f, h * 0.52f)
            lineTo(w * 0.46f, h * 0.36f)
            lineTo(w * 0.54f, h * 0.64f)
            lineTo(w * 0.60f, h * 0.48f)
            lineTo(w * 0.72f, h * 0.48f)
        }
        drawPath(pulsePath, color = gold, style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        // Green connected dot at top-right of bezel
        drawCircle(color = green, radius = w * 0.08f, center = Offset(w * 0.74f, h * 0.24f))
    }
}

/**
 * Custom Health Connect Dual-Link Heart Emblem matching Mockup 3/4 #2 & Mockup 4/4 #2.
 */
@Composable
fun MockupHealthConnectBadge(
    modifier: Modifier = Modifier.size(36.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Soft rounded background badge
        drawRoundRect(
            color = Color(0xFF0EA5E9).copy(alpha = 0.16f),
            size = size,
            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
        )
        // Left interlocking cyan loop
        drawCircle(
            color = Color(0xFF38BDF8),
            radius = w * 0.22f,
            center = Offset(w * 0.40f, h * 0.50f),
            style = Stroke(width = 2.6.dp.toPx())
        )
        // Right interlocking emerald/gold loop
        drawCircle(
            color = Color(0xFF34D399),
            radius = w * 0.22f,
            center = Offset(w * 0.60f, h * 0.50f),
            style = Stroke(width = 2.6.dp.toPx())
        )
    }
}

@Composable
fun MockupWatchGraphicBadge(
    size: androidx.compose.ui.unit.Dp
) {
    MockupWatchGraphicBadge(
        modifier = Modifier.size(size)
    )
}

@Composable
fun MockupHealthConnectBadge(
    size: androidx.compose.ui.unit.Dp
) {
    MockupHealthConnectBadge(
        modifier = Modifier.size(size)
    )
}
