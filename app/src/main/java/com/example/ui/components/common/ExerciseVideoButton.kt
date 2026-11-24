package com.example.ui.components

import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalTrainPilotPalette

import androidx.compose.foundation.layout.sizeIn
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.localization.LocalTrainPilotStrings

val LocalOpenExerciseVideo = androidx.compose.runtime.compositionLocalOf<(String, String?) -> Unit> { { _, _ -> } }

/**
 * Compact reusable "▶ Videó" button placed on exercise rows in lists, workouts, quick workouts, program editor, and logs.
 * Opens the embedded YouTube exercise demonstration popup dialog.
 */
@Composable
fun SmallExerciseVideoButton(
    exerciseName: String,
    modifier: Modifier = Modifier,
    compactIconOnly: Boolean = false,
    exerciseId: String? = null
) {
    val palette = LocalTrainPilotPalette.current
    val strings = LocalTrainPilotStrings.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val openVideo = LocalOpenExerciseVideo.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(interactionSource = interaction, indication = null) { openVideo(exerciseName, exerciseId) }
            .semantics { contentDescription = "${strings.videoLabel}: $exerciseName" }
            .testTag("exercise_video_btn_${exerciseName.hashCode()}")
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFE53935).copy(alpha = if (pressed) 0.24f else 0.16f),
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).indication(interaction, ripple()).border(1.dp, Color(0xFFE53935).copy(alpha = 0.55f), RoundedCornerShape(8.dp))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = if (compactIconOnly) 8.dp else 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(7.dp)) {
                        val path = Path().apply {
                            moveTo(size.width * 0.22f, size.height * 0.15f)
                            lineTo(size.width * 0.88f, size.height * 0.50f)
                            lineTo(size.width * 0.22f, size.height * 0.85f)
                            close()
                        }
                        drawPath(path = path, color = Color.White)
                    }
                }
                if (!compactIconOnly) {
                    Text(
                        text = strings.videoLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        color = palette.textPrimary
                    )
                }
            }
        }
    }
}
