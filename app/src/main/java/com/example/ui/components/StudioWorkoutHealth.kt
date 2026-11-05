package com.example.ui.components

import com.repforge.app.ui.theme.TrainPilotOutlinedButton

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
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

/** Reuse the native Room observer and guarded Health reader behind the Studio journal. */
@Composable
internal fun studioWorkoutHealthContent(
    workoutId: String,
    language: String,
    requestPermissions: () -> Unit,
    onSources: (String, Boolean, Boolean) -> Unit,
    model: JournalViewModel = viewModel(key = "studio-workout-health")
): @Composable () -> Unit {
    val context = LocalContext.current
    val activity = remember(context) { context.healthActivity() }
    val owner = LocalLifecycleOwner.current
    val state by model.uiState.collectAsStateWithLifecycle()
    val palette = LocalTrainPilotPalette.current
    val text = remember(context, language) { nativeResources(context, language) }
    var resumed by remember(owner) { mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    val expandedSource = rememberSaveable(workoutId) { mutableStateOf<String?>(null) }
    var attemptedProviders by rememberSaveable(workoutId) { mutableStateOf(listOf<String>()) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, _ -> resumed = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    DisposableEffect(workoutId, model) {
        if (workoutId.isNotBlank()) model.openDetails(workoutId)
        onDispose { model.closeDetails() }
    }
    LaunchedEffect(workoutId, state.deleting, state.selectedWorkoutId) {
        if (workoutId.isNotBlank() && !state.deleting && state.selectedWorkoutId != workoutId &&
            state.deletedWorkoutId != workoutId) model.openDetails(workoutId)
    }
    val detail = state.detail?.takeIf { it.workout.id == workoutId }
    LaunchedEffect(detail?.workout, resumed, state.healthProvider) {
        val workout = detail?.workout ?: return@LaunchedEffect
        val document = workout.health240Json?.let { runCatching { org.json.JSONObject(it) }.getOrNull() }
        val cached = com.repforge.app.NativeWorkoutHealthSource.snapshot(document, state.healthProvider)
        val hasHealthMetrics = cached != null && NativeWorkoutHealth.matchesWindow(cached, workout) &&
            NativeWorkoutHealth.metrics(cached, false).isNotEmpty()
        val wear = com.repforge.app.NativeSavedWearHealth.read(workout)
        val hc = com.repforge.app.NativeWorkoutHealthSource.snapshot(document, com.repforge.app.HealthJournalStore.HC)
        onSources(workout.id, wear != null && NativeWorkoutHealth.metrics(wear, true).isNotEmpty(),
            hc != null && NativeWorkoutHealth.matchesWindow(hc, workout) && NativeWorkoutHealth.metrics(hc, false).isNotEmpty())
        // Health Connect can receive watch/Samsung records after the workout was saved.
        // A previous empty response must not suppress future foreground refresh attempts.
        if (resumed && state.healthProvider !in attemptedProviders && !hasHealthMetrics) {
            attemptedProviders = attemptedProviders + state.healthProvider
            // Foreground only; the native adapter checks actual OS grants, active windows,
            // deletion/restore generations and lifecycle again before attaching the response.
            model.refreshWorkoutHealth(activity)
        }
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
                        state.healthError?.let { Text(text.getString(it), color = palette.danger,
                            modifier = Modifier.testTag("journal_summary_refresh_error")) }
                    } else JournalHealthPanel(detail, text, language, state.healthRefreshing, state.healthError, !state.deleting,
                        provider = state.healthProvider, status = state.healthStatus, selectProvider = model::selectWorkoutHealthProvider,
                        expandedState = expandedSource,
                        permissions = { _ ->
                            if (state.healthError in setOf(R.string.journal_health_permission, R.string.journal_samsung_permission)) {
                                TrainPilotOutlinedButton(onClick = {
                                    attemptedProviders = attemptedProviders - state.healthProvider
                                    if (state.healthProvider == com.repforge.app.HealthJournalStore.SAMSUNG && activity != null)
                                        model.requestSamsungWorkoutPermission(activity)
                                    else requestPermissions()
                                }, modifier = Modifier.fillMaxWidth().testTag("journal_health_permissions")) {
                                    Text(if (state.healthProvider == com.repforge.app.HealthJournalStore.SAMSUNG)
                                        "Samsung Health • ${text.getString(R.string.journal_health_permissions)}"
                                        else text.getString(R.string.journal_health_permissions))
                                }
                            }
                        }) { model.refreshWorkoutHealth(activity) }

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
