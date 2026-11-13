package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.ExerciseItem
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState
import com.repforge.app.ui.theme.trainPilotBorder

/** Navigation only: selection never records a set or changes exercise completion. */
@Composable
internal fun StudioExerciseSequenceStrip(
    state: TrainPilotUiState,
    exercises: List<ExerciseItem>,
    currentIndex: Int,
    onSelect: (ExerciseItem) -> Unit
) {
    if (exercises.size < 2) return
    val selected = currentIndex.coerceIn(exercises.indices)
    val scroll = rememberLazyListState(initialFirstVisibleItemIndex = selected)
    val ids = exercises.map { it.id }
    LaunchedEffect(state.activeWorkoutId, state.activeWorkoutGeneration, ids, selected) {
        scroll.animateScrollToItem(selected, scrollOffset = 0)
    }
    val palette = LocalTrainPilotPalette.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        LazyRow(
            state = scroll,
            modifier = Modifier.fillMaxWidth().testTag("active_workout_exercise_sequence"),
            // Let even the final chip reach the leading edge of the viewport.
            contentPadding = PaddingValues(end = (maxWidth - 48.dp).coerceAtLeast(0.dp)),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(exercises, key = { _, exercise -> exercise.id }) { index, exercise ->
                val current = index == selected
                val done = exerciseFullyCompleted(
                    if (current) state.activeSets else state.activeSetsByExerciseId[exercise.id])
                val shape = RoundedCornerShape(12.dp)
                val border = when { current -> palette.primary; done -> palette.success; else -> palette.cardBorder }
                Surface(
                    shape = shape,
                    color = when { current -> palette.primary; done -> palette.success.copy(alpha = .18f); else -> palette.surfaceElevated },
                    modifier = Modifier.widthIn(min = 48.dp)
                        .trainPilotBorder(1.dp, border, shape).clip(shape)
                        .clickable { onSelect(exercise) }.testTag("active_exercise_chip_$index")
                ) {
                    Text("${index + 1}. ${exercise.localizedName(state.language)}" + if (done && !current) " ✓" else "",
                        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                        color = when { current -> palette.onPrimary; done -> palette.success; else -> palette.textSecondary },
                        maxLines = 1, softWrap = false,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                }
            }
        }
    }
}
