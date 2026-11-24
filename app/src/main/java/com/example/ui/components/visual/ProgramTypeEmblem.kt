package com.example.ui.components

import com.repforge.app.ui.theme.trainPilotBorder

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalTrainPilotPalette

/**
 * Custom Program Category Emblem Icon matching Mockup 1/4 #3 (`Programok`):
 * - Home + Dumbbell for "Otthoni" programs
 * - Bodyweight / Calisthenics athlete for "Saját testsúlyos" / "Kezdő" programs
 * - Heavy Barbell with plates for "Felső / Alsó" / "Súlyzós" / "Erő" programs
 */
@Composable
fun ProgramTypeEmblemIcon(
    programNameOrId: String,
    isHighlighted: Boolean = false,
    modifier: Modifier = Modifier.size(50.dp)
) {
    val palette = LocalTrainPilotPalette.current
    val lower = programNameOrId.lowercase()
    val accent = if (isHighlighted) palette.primary else palette.textPrimary

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isHighlighted) palette.primary.copy(alpha = 0.16f) else palette.surfaceElevated,
        modifier = modifier.trainPilotBorder(
            1.dp,
            if (isHighlighted) palette.primary.copy(alpha = 0.5f) else palette.cardBorder,
            RoundedCornerShape(14.dp)
        )
    ) {
        val goldColor = palette.primary
        val secondaryColor = palette.secondary
        val greenColor = palette.success
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            when {
                // 1. OTTHONI (House Roof + Dumbbell inside)
                lower.contains("otthon") || lower.contains("home") || lower.contains("alap") -> {
                    val roof = Path().apply {
                        moveTo(w * 0.18f, h * 0.46f)
                        lineTo(w * 0.50f, h * 0.20f)
                        lineTo(w * 0.82f, h * 0.46f)
                    }
                    drawPath(
                        path = roof,
                        color = goldColor,
                        style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    // Dumbbell bar & plates inside house
                    drawLine(
                        color = accent,
                        start = Offset(w * 0.30f, h * 0.64f),
                        end = Offset(w * 0.70f, h * 0.64f),
                        strokeWidth = 2.6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawRoundRect(
                        color = goldColor,
                        topLeft = Offset(w * 0.24f, h * 0.52f),
                        size = Size(w * 0.10f, h * 0.24f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                    drawRoundRect(
                        color = goldColor,
                        topLeft = Offset(w * 0.66f, h * 0.52f),
                        size = Size(w * 0.10f, h * 0.24f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }

                // 2. SAJÁT TESTSÚLYOS (Calisthenics / Pull-up / Bodyweight figure)
                lower.contains("testsúly") || lower.contains("body") || lower.contains("kezdő") || lower.contains("calisthen") -> {
                    // Pull-up bar at top
                    drawLine(
                        color = goldColor,
                        start = Offset(w * 0.20f, h * 0.22f),
                        end = Offset(w * 0.80f, h * 0.22f),
                        strokeWidth = 2.4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    // Athlete head
                    drawCircle(color = greenColor, radius = w * 0.085f, center = Offset(w * 0.50f, h * 0.38f))
                    // Arms reaching up to bar
                    drawLine(
                        color = greenColor,
                        start = Offset(w * 0.42f, h * 0.46f),
                        end = Offset(w * 0.30f, h * 0.24f),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = greenColor,
                        start = Offset(w * 0.58f, h * 0.46f),
                        end = Offset(w * 0.70f, h * 0.24f),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    // V-torso & legs
                    drawLine(
                        color = accent,
                        start = Offset(w * 0.50f, h * 0.46f),
                        end = Offset(w * 0.50f, h * 0.76f),
                        strokeWidth = 3.2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // 3. SÚLYZÓS / FELSŐ-ALSÓ / ERŐ (Heavy Olympic Barbell with dual plates)
                else -> {
                    drawLine(
                        color = accent,
                        start = Offset(w * 0.14f, h * 0.50f),
                        end = Offset(w * 0.86f, h * 0.50f),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    // Inner heavy plates
                    drawRoundRect(
                        color = goldColor,
                        topLeft = Offset(w * 0.24f, h * 0.28f),
                        size = Size(w * 0.10f, h * 0.44f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                    drawRoundRect(
                        color = goldColor,
                        topLeft = Offset(w * 0.66f, h * 0.28f),
                        size = Size(w * 0.10f, h * 0.44f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                    // Outer smaller plates
                    drawRoundRect(
                        color = secondaryColor,
                        topLeft = Offset(w * 0.15f, h * 0.35f),
                        size = Size(w * 0.07f, h * 0.30f),
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                    drawRoundRect(
                        color = secondaryColor,
                        topLeft = Offset(w * 0.78f, h * 0.35f),
                        size = Size(w * 0.07f, h * 0.30f),
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                }
            }
        }
    }
}

@Composable
fun ProgramTypeEmblemIcon(
    programTitle: String,
    isHighlighted: Boolean = false,
    size: androidx.compose.ui.unit.Dp
) {
    ProgramTypeEmblemIcon(
        programNameOrId = programTitle,
        isHighlighted = isHighlighted,
        modifier = Modifier.size(size)
    )
}
