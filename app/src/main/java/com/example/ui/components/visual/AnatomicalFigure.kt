package com.example.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

internal fun DrawScope.drawAnatomicalFigure(
    cx: Float,
    h: Float,
    isFront: Boolean,
    baseColor: Color,
    outlineColor: Color,
    chestColor: Color,
    shoulderColor: Color,
    armColor: Color,
    coreColor: Color,
    legColor: Color,
    backColor: Color
) {
    val stroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

    // 1. Head & Neck
    drawCircle(
        color = baseColor,
        radius = h * 0.065f,
        center = Offset(cx, h * 0.10f)
    )
    drawCircle(
        color = outlineColor,
        radius = h * 0.065f,
        center = Offset(cx, h * 0.10f),
        style = stroke
    )
    // Neck / Upper Traps
    drawRoundRect(
        color = if (isFront) baseColor else backColor,
        topLeft = Offset(cx - h * 0.035f, h * 0.16f),
        size = Size(h * 0.07f, h * 0.04f),
        cornerRadius = CornerRadius(6f, 6f)
    )

    // 2. Shoulders (Deltoids Left & Right)
    drawOval(
        color = shoulderColor,
        topLeft = Offset(cx - h * 0.175f, h * 0.19f),
        size = Size(h * 0.075f, h * 0.085f)
    )
    drawOval(
        color = shoulderColor,
        topLeft = Offset(cx + h * 0.10f, h * 0.19f),
        size = Size(h * 0.075f, h * 0.085f)
    )

    if (isFront) {
        // 3A. Front Chest (Pectorals)
        drawRoundRect(
            color = chestColor,
            topLeft = Offset(cx - h * 0.11f, h * 0.20f),
            size = Size(h * 0.102f, h * 0.095f),
            cornerRadius = CornerRadius(12f, 12f)
        )
        drawRoundRect(
            color = chestColor,
            topLeft = Offset(cx + h * 0.008f, h * 0.20f),
            size = Size(h * 0.102f, h * 0.095f),
            cornerRadius = CornerRadius(12f, 12f)
        )

        // 4A. Front Core / Abs (6-pack blocks)
        drawRoundRect(
            color = coreColor,
            topLeft = Offset(cx - h * 0.085f, h * 0.305f),
            size = Size(h * 0.17f, h * 0.165f),
            cornerRadius = CornerRadius(10f, 10f)
        )
        // Center linea alba & transverse lines
        drawLine(
            color = outlineColor,
            start = Offset(cx, h * 0.31f),
            end = Offset(cx, h * 0.46f),
            strokeWidth = 1.5.dp.toPx()
        )
    } else {
        // 3B. Back Lats & Rhomboids (V-Taper)
        val backPath = Path().apply {
            moveTo(cx - h * 0.115f, h * 0.20f)
            lineTo(cx + h * 0.115f, h * 0.20f)
            lineTo(cx + h * 0.08f, h * 0.42f)
            lineTo(cx - h * 0.08f, h * 0.42f)
            close()
        }
        drawPath(path = backPath, color = backColor)
        drawPath(path = backPath, color = outlineColor, style = stroke)

        // 4B. Glutes (Farizom - part of Legs/Posterior chain)
        drawRoundRect(
            color = legColor,
            topLeft = Offset(cx - h * 0.095f, h * 0.43f),
            size = Size(h * 0.09f, h * 0.085f),
            cornerRadius = CornerRadius(14f, 14f)
        )
        drawRoundRect(
            color = legColor,
            topLeft = Offset(cx + h * 0.005f, h * 0.43f),
            size = Size(h * 0.09f, h * 0.085f),
            cornerRadius = CornerRadius(14f, 14f)
        )
    }

    // 5. Arms (Biceps/Triceps + Forearms)
    // Left Upper Arm
    drawRoundRect(
        color = armColor,
        topLeft = Offset(cx - h * 0.185f, h * 0.275f),
        size = Size(h * 0.06f, h * 0.11f),
        cornerRadius = CornerRadius(10f, 10f)
    )
    // Right Upper Arm
    drawRoundRect(
        color = armColor,
        topLeft = Offset(cx + h * 0.125f, h * 0.275f),
        size = Size(h * 0.06f, h * 0.11f),
        cornerRadius = CornerRadius(10f, 10f)
    )
    // Left Forearm
    drawRoundRect(
        color = armColor.copy(alpha = 0.8f),
        topLeft = Offset(cx - h * 0.20f, h * 0.395f),
        size = Size(h * 0.052f, h * 0.11f),
        cornerRadius = CornerRadius(8f, 8f)
    )
    // Right Forearm
    drawRoundRect(
        color = armColor.copy(alpha = 0.8f),
        topLeft = Offset(cx + h * 0.148f, h * 0.395f),
        size = Size(h * 0.052f, h * 0.11f),
        cornerRadius = CornerRadius(8f, 8f)
    )

    // 6. Upper Legs (Quads / Hamstrings)
    drawRoundRect(
        color = legColor,
        topLeft = Offset(cx - h * 0.098f, h * 0.515f),
        size = Size(h * 0.086f, h * 0.205f),
        cornerRadius = CornerRadius(14f, 14f)
    )
    drawRoundRect(
        color = legColor,
        topLeft = Offset(cx + h * 0.012f, h * 0.515f),
        size = Size(h * 0.086f, h * 0.205f),
        cornerRadius = CornerRadius(14f, 14f)
    )

    // 7. Lower Legs (Calves / Vádli)
    drawRoundRect(
        color = legColor.copy(alpha = 0.85f),
        topLeft = Offset(cx - h * 0.09f, h * 0.735f),
        size = Size(h * 0.072f, h * 0.19f),
        cornerRadius = CornerRadius(12f, 12f)
    )
    drawRoundRect(
        color = legColor.copy(alpha = 0.85f),
        topLeft = Offset(cx + h * 0.018f, h * 0.735f),
        size = Size(h * 0.072f, h * 0.19f),
        cornerRadius = CornerRadius(12f, 12f)
    )
}
