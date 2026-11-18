package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.localization.LocalTrainPilotStrings
import com.example.localization.TrainPilotStrings
import com.example.ui.viewmodel.TrainPilotViewModel
import kotlinx.coroutines.launch

/** One planner and persistence path for first run and Home's Profile and new plan. */
@Composable
internal fun StudioPersonalPlannerDialog(
    viewModel: TrainPilotViewModel,
    onDismiss: () -> Unit,
    completeOnboarding: Boolean = false
) {
    val state by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    fun save(recommended: Boolean): PersonalPlannerSave = { h, w, goal, exp, loc, eq, days, dur, muscles, exercises ->
        if (!saving) {
            saving = true
            failed = false
            viewModel.saveInitialProfileAndPlan(h, w, goal, exp, loc, eq, days, dur, muscles, exercises,
                completeOnboarding = completeOnboarding, useRecommendedProgram = recommended,
                onSaveResult = { success -> scope.launch { saving = false; failed = !success } })
        }
    }
    CompositionLocalProvider(LocalTrainPilotStrings provides TrainPilotStrings.forLanguage(state.language)) {
        PersonalPlannerModalDialog(
            initialHeightCm = state.userHeightCm,
            initialWeightKg = state.userWeightKg,
            initialGoalHu = state.userFitnessGoalHu,
            initialExperienceHu = state.userExperienceLevelHu,
            initialLocationHu = state.userWorkoutLocationHu,
            initialEquipmentHu = state.userSelectedEquipmentHu,
            initialDaysPerWeek = state.userScheduleDaysPerWeek,
            initialDurationMin = state.userWorkoutDurationMin,
            initialExcludedMuscles = state.userExcludedMusclesHu,
            initialExcludedExercises = state.userExcludedExercisesHu,
            saving = saving,
            failed = failed,
            gentlerStart = com.example.model.suggestGentlerStarterPlan(state.todayRecoveryReading),
            onSaveAndOpenPlan = save(false),
            onUseRecommendedPlan = save(true),
            recommendProgram = { equipment, location, experience, goal, days, duration, muscles, exercises ->
                // Keep the recommendation current while the Room catalog is loading.
                if (state.programs.isEmpty()) null
                else viewModel.recommendStarterProgram(equipment, location, experience, goal, days, duration, muscles, exercises)
            },
            onDismiss = { if (!saving) onDismiss() }
        )
    }
}
