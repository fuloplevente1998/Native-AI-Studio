package com.example.ui.components

import com.repforge.app.ui.theme.TrainPilotOutlinedButton

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import com.repforge.app.ui.theme.TrainPilotOutlinedButton as OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.LocalTrainPilotPalette
import com.repforge.app.NativeWorkoutHealth
import com.repforge.app.R
import com.repforge.app.ui.journal.JournalHealthPanel
import com.repforge.app.ui.journal.JournalViewModel
import com.repforge.app.ui.text.nativeResources
import com.repforge.app.ui.theme.LocalTrainPilotColors
import com.repforge.app.ui.theme.TrainPilotExtendedColors

internal enum class WorkoutHealthView { Sources, Summary }
internal val LocalWorkoutHealthView = staticCompositionLocalOf { WorkoutHealthView.Sources }

/** Reuse the native Room observer; this history view no longer reads provider windows. */
@Suppress("UNUSED_PARAMETER")
@Composable
internal fun studioWorkoutHealthContent(
    workoutId: String,
    language: String,
    requestPermissions: () -> Unit,
    onSources: (String, Boolean, Boolean) -> Unit,
    model: JournalViewModel = viewModel(key = "studio-workout-health")
): @Composable () -> Unit {
    val context = LocalContext.current
    val state by model.uiState.collectAsStateWithLifecycle()
    val palette = LocalTrainPilotPalette.current
    val text = remember(context, language) { nativeResources(context, language) }
    DisposableEffect(workoutId, model) {
        if (workoutId.isNotBlank()) model.openDetails(workoutId)
        onDispose { model.closeDetails() }
    }
    LaunchedEffect(workoutId, state.deleting, state.selectedWorkoutId) {
        if (workoutId.isNotBlank() && !state.deleting && state.selectedWorkoutId != workoutId &&
            state.deletedWorkoutId != workoutId) model.openDetails(workoutId)
    }
    val detail = state.detail?.takeIf { it.workout.id == workoutId }
    LaunchedEffect(detail?.workout) {
        val workout = detail?.workout ?: return@LaunchedEffect
        val wear = com.repforge.app.NativeWorkoutHealthSummary.readJournal(workout)
        // The details are read-only; do not query HC/Samsung when opening a TrainPilot session.
        // Imported sessions keep their own stored source evidence.
        onSources(workout.id, com.repforge.app.NativeWorkoutHealthSummary.journalValues(wear).isNotEmpty(),
            workout.originDevice == "external" && workout.health240Json?.isNotBlank() == true)
    }
    val nativeColors = remember(palette) {
        TrainPilotExtendedColors(bg = palette.background, card = palette.surfaceCard, card2 = palette.surfaceElevated,
            line = palette.cardBorder, text = palette.textPrimary, muted = palette.textSecondary,
            accent = palette.primary, accent2 = palette.secondary, onAccent = palette.onPrimary,
            dangerText = palette.danger, success = palette.success, emphasisBorder = palette.sectionBorder)
    }
    return {
        CompositionLocalProvider(LocalTrainPilotColors provides nativeColors) {
            if (detail != null) {
                Column(Modifier.fillMaxWidth().testTag("studio_workout_health")) {
                    if (LocalWorkoutHealthView.current == WorkoutHealthView.Summary) {
                        com.repforge.app.ui.journal.JournalWorkoutHealthSummary(detail, text, language)
                    } else JournalHealthPanel(detail, text, language, false, null, !state.deleting) { }
                }
            } else {
                Text(text.getString(if (state.detailLoading) R.string.journal_health_refreshing else R.string.journal_no_data),
                    color = palette.textSecondary, modifier = Modifier.testTag("studio_workout_health_loading"))
            }
        }
    }
}

internal fun Context.healthActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        val base = current.baseContext
        if (base === current) return null
        current = base
    }
    return current as? Activity
}
