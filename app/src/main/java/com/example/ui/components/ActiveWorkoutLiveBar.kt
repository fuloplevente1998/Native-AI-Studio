package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.AppLanguage
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState
import com.repforge.app.ActiveWorkoutStore
import com.repforge.app.StudioWorkoutSessionPresentation
import com.repforge.app.ui.theme.trainPilotBorder
import com.repforge.app.ui.theme.trainPilotClickable
import kotlinx.coroutines.delay
import java.time.Instant

private fun lb(lang: AppLanguage, hu: String, en: String, de: String, ro: String) = when (lang) {
    AppLanguage.HU -> hu; AppLanguage.EN -> en; AppLanguage.DE -> de; AppLanguage.RO -> ro
}

/** Elapsed workout time and paused state, the same source as the workout page header. */
@Composable
fun rememberWorkoutElapsed(state: TrainPilotUiState): Pair<String, Boolean> {
    val frame by ActiveWorkoutStore.snapshotFlow.collectAsState()
    val snapshot = frame?.takeIf {
        it.optBoolean("active") &&
            (state.activeWorkoutId.isBlank() || it.optString("workoutId") == state.activeWorkoutId) &&
            (state.activeWorkoutGeneration <= 0L || it.optLong("generation") == state.activeWorkoutGeneration)
    }
    val paused = snapshot?.optBoolean("workoutPaused") == true
    var now by remember(state.activeWorkoutId) { mutableStateOf(Instant.now()) }
    LaunchedEffect(state.activeWorkoutId, paused, state.isWorkoutSessionActive) {
        now = Instant.now()
        while (state.isWorkoutSessionActive && !paused) { delay(1000); now = Instant.now() }
    }
    val seconds = snapshot?.let { StudioWorkoutSessionPresentation.elapsedSeconds(it, now) }
        ?: state.workoutStartedEpochMs.takeIf { it > 0L }?.let { ((now.toEpochMilli() - it) / 1000L).coerceAtLeast(0L) }
    return (seconds?.let(StudioWorkoutSessionPresentation::durationLabel) ?: "00:00") to paused
}

/**
 * 2774: the single bar shown over every page while a workout is active (outside the workout
 * screen): running/paused state, elapsed time, "Finish" (same finish sheet as in the workout)
 * and "Continue" (back to the workout, resuming it).
 */
@Composable
fun ActiveWorkoutLiveBar(
    state: TrainPilotUiState,
    onContinue: () -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalTrainPilotPalette.current
    val lang = state.language
    val (elapsed, paused) = rememberWorkoutElapsed(state)
    var showFinish by rememberSaveable { mutableStateOf(false) }
    val enabled = !state.isSavingWorkout && !state.watchControlPending
    val day = state.activeWorkoutExercises.takeIf { it.isNotEmpty() }
        ?.let { "${state.activeExerciseIndex + 1}/${it.size}" }.orEmpty()
    val shape = RoundedCornerShape(16.dp)
    Surface(
        shape = shape,
        color = palette.surfaceElevated.copy(alpha = 0.98f),
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth().trainPilotBorder(1.5.dp, palette.primary, shape)
            .trainPilotClickable(shape) { onContinue() }.testTag("floating_return_to_active_workout_bar")
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // 2778: drawn pause icon in the theme colour (like the home card), not the orange emoji square.
            if (paused) androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Rounded.Pause, contentDescription = null,
                tint = palette.primary, modifier = Modifier.size(22.dp).testTag("live_bar_paused_icon"))
            else Box(Modifier.size(9.dp).clip(RoundedCornerShape(5.dp)).background(palette.success))
            Column(Modifier.weight(1f)) {
                Text(elapsed, style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = com.repforge.app.ui.theme.TrainPilotNumericFont, fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Bold, color = palette.primary, maxLines = 1,
                    modifier = Modifier.testTag("live_bar_elapsed"))
                Text((if (paused) lb(lang, "Szünetel", "Paused", "Pausiert", "În pauză") else lb(lang, "Fut", "Running", "Läuft", "În curs")) +
                    (if (day.isNotBlank()) " · $day" else ""),
                    style = MaterialTheme.typography.labelSmall, color = if (paused) palette.primary else palette.textSecondary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("live_bar_status"))
            }
            val small = RoundedCornerShape(12.dp)
            Surface(shape = small, color = palette.surfaceCard, modifier = Modifier.heightIn(min = 44.dp)
                .trainPilotBorder(1.dp, palette.primary.copy(alpha = 0.6f), small)
                .trainPilotClickable(small, enabled = enabled) { showFinish = true }.testTag("live_bar_finish")) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 9.dp, vertical = 10.dp)) {
                    // 2775: drawn stop icon instead of the emoji square.
                    androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Rounded.Stop, contentDescription = null,
                        tint = palette.primary, modifier = Modifier.size(16.dp).testTag("live_bar_finish_icon"))
                    androidx.compose.foundation.layout.Spacer(Modifier.width(3.dp))
                    Text(lb(lang, "Befejezés", "Finish", "Beenden", "Finalizare"), color = palette.textPrimary,
                        fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
            }
            Surface(shape = small, color = palette.primary, modifier = Modifier.heightIn(min = 44.dp)
                .trainPilotClickable(small) { onContinue() }.testTag("live_bar_continue")) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
                    Text(lb(lang, "Folytatás ▶", "Continue ▶", "Weiter ▶", "Continuă ▶"), color = palette.onPrimary,
                        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
            }
        }
    }
    if (showFinish) WorkoutFinishSheet(lang, enabled,
        state.activeSetsByExerciseId.values.sumOf { sets -> sets.count { it.completed } },
        state.activeSetsByExerciseId.values.sumOf { it.size }, elapsed, "live_bar",
        onFinish = onFinish, onDiscard = onDiscard, onDismiss = { showFinish = false })
}
