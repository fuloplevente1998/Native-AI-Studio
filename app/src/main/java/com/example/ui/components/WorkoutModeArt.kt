package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalTrainPilotPalette

internal enum class QuickWorkoutMode { REPS, TIMED, OUTDOOR }

@Composable
internal fun WorkoutModeArt(
    mode: QuickWorkoutMode,
    modifier: Modifier = Modifier,
    height: Dp = 64.dp,
    selected: Boolean = false
) {
    val palette = LocalTrainPilotPalette.current
    val graphic = modifier.testTag("workout_mode_art_${mode.name.lowercase()}")
    if (mode == QuickWorkoutMode.OUTDOOR) {
        val shape = RoundedCornerShape(14.dp)
        Canvas(graphic.fillMaxWidth().height(height).clip(shape)
            .background(Color(0xFF14261C)).border(1.dp, palette.success.copy(alpha = 0.45f), shape)) {
            val w = size.width
            val h = size.height
            val scale = h / 64.dp.toPx()
            val start = Offset(w * 0.22f, h * 0.78f)
            val middle = Offset(w * 0.52f, h * 0.24f)
            val end = Offset(w * 0.82f, h * 0.65f)
            val route = Color(0xFFF59E0B)
            val stroke = 3.5.dp.toPx() * scale
            drawLine(Color(0xFF1E3A5F), Offset(0f, h * 0.2f), Offset(w * 0.35f, h), strokeWidth = 12.dp.toPx() * scale)
            drawLine(route, start, middle, stroke, StrokeCap.Round)
            drawLine(route, middle, end, stroke, StrokeCap.Round)
            drawCircle(Color(0xFF22C55E), 5.dp.toPx() * scale, start)
            drawCircle(route, 5.dp.toPx() * scale, end)
        }
    } else {
        val tint = if (selected) palette.onPrimary else palette.primary
        Surface(shape = RoundedCornerShape(18.dp), color = tint.copy(alpha = 0.16f),
            modifier = graphic.size(height)) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(if (mode == QuickWorkoutMode.REPS) Icons.Default.FitnessCenter else Icons.Default.Timer,
                    null, tint = tint, modifier = Modifier.size(height * 0.5625f))
            }
        }
    }
}
