package com.example.ui.components

import com.repforge.app.ui.theme.trainPilotBorder

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalTrainPilotPalette

@Composable
fun ExercisePostureMiniIllustration(
    exerciseNameOrId: String,
    modifier: Modifier = Modifier.size(54.dp)
) {
    val palette = LocalTrainPilotPalette.current
    val lower = exerciseNameOrId.lowercase()

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = palette.surfaceElevated,
        modifier = modifier.trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
    ) {
        val primaryGold = palette.primary
        val muscleOrange = Color(0xFFF97316)
        val bodySlate = Color(0xFFCBD5E1)
        val headSlate = Color(0xFF94A3B8)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val strokeBody = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            val strokeMuscle = Stroke(width = 4.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

            when {
                // 1. SQUAT / GOBLET SQUAT / GUGGOLÁS (Squatting posture + highlighted quads/glutes)
                lower.contains("guggol") || lower.contains("squat") || lower.contains("lábnyom") -> {
                    // Head
                    drawCircle(color = headSlate, radius = w * 0.09f, center = Offset(w * 0.48f, h * 0.22f))
                    // Torso leaning slightly forward
                    drawLine(
                        color = bodySlate,
                        start = Offset(w * 0.48f, h * 0.31f),
                        end = Offset(w * 0.36f, h * 0.56f),
                        strokeWidth = strokeBody.width,
                        cap = StrokeCap.Round
                    )
                    // Highlighted Quads & Glutes (horizontal thigh in squat position)
                    drawLine(
                        color = muscleOrange,
                        start = Offset(w * 0.36f, h * 0.56f),
                        end = Offset(w * 0.62f, h * 0.58f),
                        strokeWidth = strokeMuscle.width,
                        cap = StrokeCap.Round
                    )
                    // Lower leg (shin)
                    drawLine(
                        color = bodySlate,
                        start = Offset(w * 0.62f, h * 0.58f),
                        end = Offset(w * 0.56f, h * 0.84f),
                        strokeWidth = strokeBody.width,
                        cap = StrokeCap.Round
                    )
                    // Arms holding Kettlebell / Dumbbell at chest (Goblet)
                    drawLine(
                        color = bodySlate,
                        start = Offset(w * 0.46f, h * 0.36f),
                        end = Offset(w * 0.64f, h * 0.42f),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(color = primaryGold, radius = w * 0.08f, center = Offset(w * 0.68f, h * 0.42f))
                    // Up/down motion arrow on left
                    drawLine(
                        color = primaryGold,
                        start = Offset(w * 0.18f, h * 0.28f),
                        end = Offset(w * 0.18f, h * 0.72f),
                        strokeWidth = 1.6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // 2. PUSH-UP / BENCH PRESS / FEKVŐTÁMASZ / FEKVENYOMÁS (Horizontal push posture + chest/triceps highlight)
                lower.contains("fekvő") || lower.contains("fekvenyom") || lower.contains("push") || lower.contains("bench") || lower.contains("mell") -> {
                    // Floor / Bench reference line
                    drawLine(
                        color = headSlate.copy(alpha = 0.35f),
                        start = Offset(w * 0.14f, h * 0.76f),
                        end = Offset(w * 0.86f, h * 0.76f),
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    // Head
                    drawCircle(color = headSlate, radius = w * 0.085f, center = Offset(w * 0.76f, h * 0.40f))
                    // Inclined Push-up Body line (shoulders to feet)
                    drawLine(
                        color = bodySlate,
                        start = Offset(w * 0.68f, h * 0.44f),
                        end = Offset(w * 0.18f, h * 0.72f),
                        strokeWidth = strokeBody.width,
                        cap = StrokeCap.Round
                    )
                    // Highlighted Chest & Anterior Shoulder
                    drawCircle(color = muscleOrange, radius = w * 0.095f, center = Offset(w * 0.63f, h * 0.47f))
                    // Pushing arm to floor
                    drawLine(
                        color = muscleOrange,
                        start = Offset(w * 0.65f, h * 0.47f),
                        end = Offset(w * 0.66f, h * 0.75f),
                        strokeWidth = strokeBody.width,
                        cap = StrokeCap.Round
                    )
                }

                // 3. ROW / PULL / EVEZÉS / HÚZÓDZKODÁS / FELHÚZÁS (Hinged back posture + lats/upper back highlight)
                lower.contains("evez") || lower.contains("row") || lower.contains("hát") || lower.contains("húzó") || lower.contains("felhúz") || lower.contains("deadlift") -> {
                    // Head
                    drawCircle(color = headSlate, radius = w * 0.085f, center = Offset(w * 0.66f, h * 0.26f))
                    // Hinged Torso (Highlighted Back / Lats)
                    drawLine(
                        color = muscleOrange,
                        start = Offset(w * 0.60f, h * 0.33f),
                        end = Offset(w * 0.32f, h * 0.54f),
                        strokeWidth = strokeMuscle.width,
                        cap = StrokeCap.Round
                    )
                    // Bent Legs
                    drawLine(
                        color = bodySlate,
                        start = Offset(w * 0.32f, h * 0.54f),
                        end = Offset(w * 0.42f, h * 0.82f),
                        strokeWidth = strokeBody.width,
                        cap = StrokeCap.Round
                    )
                    // Pulling arm + band/dumbbell
                    drawLine(
                        color = primaryGold,
                        start = Offset(w * 0.54f, h * 0.38f),
                        end = Offset(w * 0.76f, h * 0.56f),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(color = primaryGold, radius = w * 0.065f, center = Offset(w * 0.76f, h * 0.58f))
                }

                // 4. LUNGE / KITÖRÉS / SPLIT SQUAT (Split-stance lunge posture)
                lower.contains("kitör") || lower.contains("lunge") || lower.contains("bolgár") -> {
                    // Head
                    drawCircle(color = headSlate, radius = w * 0.085f, center = Offset(w * 0.50f, h * 0.20f))
                    // Upright Torso
                    drawLine(
                        color = bodySlate,
                        start = Offset(w * 0.50f, h * 0.29f),
                        end = Offset(w * 0.50f, h * 0.54f),
                        strokeWidth = strokeBody.width,
                        cap = StrokeCap.Round
                    )
                    // Front Lunge Leg (Highlighted Quad/Glute)
                    drawLine(
                        color = muscleOrange,
                        start = Offset(w * 0.50f, h * 0.54f),
                        end = Offset(w * 0.72f, h * 0.56f),
                        strokeWidth = strokeMuscle.width,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = bodySlate,
                        start = Offset(w * 0.72f, h * 0.56f),
                        end = Offset(w * 0.72f, h * 0.82f),
                        strokeWidth = strokeBody.width,
                        cap = StrokeCap.Round
                    )
                    // Rear Knee-down Leg
                    drawLine(
                        color = muscleOrange.copy(alpha = 0.85f),
                        start = Offset(w * 0.50f, h * 0.54f),
                        end = Offset(w * 0.34f, h * 0.78f),
                        strokeWidth = strokeBody.width,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = bodySlate,
                        start = Offset(w * 0.34f, h * 0.78f),
                        end = Offset(w * 0.18f, h * 0.80f),
                        strokeWidth = strokeBody.width,
                        cap = StrokeCap.Round
                    )
                }

                // 5. PLANK / OLDALSÓ PLANK / CORE / HASPRÉS (Core stability posture + glowing abs/obliques)
                lower.contains("plank") || lower.contains("core") || lower.contains("has") || lower.contains("dead bug") -> {
                    // Floor line
                    drawLine(
                        color = headSlate.copy(alpha = 0.35f),
                        start = Offset(w * 0.12f, h * 0.76f),
                        end = Offset(w * 0.88f, h * 0.76f),
                        strokeWidth = 1.5.dp.toPx()
                    )
                    // Head
                    drawCircle(color = headSlate, radius = w * 0.08f, center = Offset(w * 0.78f, h * 0.48f))
                    // Forearm support on floor
                    drawLine(
                        color = bodySlate,
                        start = Offset(w * 0.68f, h * 0.52f),
                        end = Offset(w * 0.68f, h * 0.74f),
                        strokeWidth = 2.8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = bodySlate,
                        start = Offset(w * 0.68f, h * 0.74f),
                        end = Offset(w * 0.80f, h * 0.74f),
                        strokeWidth = 2.8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    // Highlighted Core / Torso bridge
                    drawLine(
                        color = muscleOrange,
                        start = Offset(w * 0.68f, h * 0.52f),
                        end = Offset(w * 0.42f, h * 0.58f),
                        strokeWidth = strokeMuscle.width,
                        cap = StrokeCap.Round
                    )
                    // Legs to toes
                    drawLine(
                        color = bodySlate,
                        start = Offset(w * 0.42f, h * 0.58f),
                        end = Offset(w * 0.16f, h * 0.74f),
                        strokeWidth = strokeBody.width,
                        cap = StrokeCap.Round
                    )
                }

                // 6. OVERHEAD / SHOULDER / ARM PRESS (Standing overhead press / curl posture)
                else -> {
                    // Head
                    drawCircle(color = headSlate, radius = w * 0.085f, center = Offset(w * 0.50f, h * 0.30f))
                    // Upright Torso
                    drawLine(
                        color = bodySlate,
                        start = Offset(w * 0.50f, h * 0.39f),
                        end = Offset(w * 0.50f, h * 0.64f),
                        strokeWidth = strokeBody.width,
                        cap = StrokeCap.Round
                    )
                    // Highlighted Shoulders & Raised Arms
                    drawCircle(color = muscleOrange, radius = w * 0.075f, center = Offset(w * 0.36f, h * 0.42f))
                    drawCircle(color = muscleOrange, radius = w * 0.075f, center = Offset(w * 0.64f, h * 0.42f))
                    drawLine(
                        color = muscleOrange,
                        start = Offset(w * 0.34f, h * 0.42f),
                        end = Offset(w * 0.28f, h * 0.20f),
                        strokeWidth = 2.8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = muscleOrange,
                        start = Offset(w * 0.66f, h * 0.42f),
                        end = Offset(w * 0.72f, h * 0.20f),
                        strokeWidth = 2.8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    // Overhead barbell / dumbbells
                    drawLine(
                        color = primaryGold,
                        start = Offset(w * 0.18f, h * 0.18f),
                        end = Offset(w * 0.82f, h * 0.18f),
                        strokeWidth = 2.6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    // Legs
                    drawLine(
                        color = bodySlate,
                        start = Offset(w * 0.46f, h * 0.64f),
                        end = Offset(w * 0.40f, h * 0.86f),
                        strokeWidth = 2.8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = bodySlate,
                        start = Offset(w * 0.54f, h * 0.64f),
                        end = Offset(w * 0.60f, h * 0.86f),
                        strokeWidth = 2.8.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

@Composable
fun ExercisePostureMiniIllustration(
    exerciseName: String,
    size: androidx.compose.ui.unit.Dp
) {
    ExercisePostureMiniIllustration(
        exerciseNameOrId = exerciseName,
        modifier = Modifier.size(size)
    )
}
