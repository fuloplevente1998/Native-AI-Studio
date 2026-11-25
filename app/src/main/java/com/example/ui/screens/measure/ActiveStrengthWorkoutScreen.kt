package com.example.ui.screens

import com.example.ui.theme.glassCardFill
import com.example.ui.theme.trainPilotGlassMaterial
import com.repforge.app.ui.theme.trainPilotClickable
import com.example.ui.components.activeWorkoutDockFrame
import androidx.compose.ui.semantics.contentDescription

import com.repforge.app.ui.theme.TrainPilotCardSpacing

import com.repforge.app.ui.theme.TrainPilotOutlinedButton

import com.repforge.app.ui.theme.trainPilotBorder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import com.example.localization.LocalizedStrings
import com.example.model.ExerciseItem
import com.example.model.SubScreen
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState

/**
 * 1. SÚLYZÓS EDZÉS (Mockup 2/4 #1)
 */
@Composable
fun ActiveStrengthWorkoutScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectMeasurementMode: (SubScreen) -> Unit,
    onToggleSetCompleted: (Int) -> Unit,
    onAdjustSetValues: (Int, Float, Int, Int) -> Unit,
    onUndoLastSet: () -> Unit,
    onAdjustRestTimer: (Int) -> Unit,
    onToggleRestRunning: () -> Unit,
    onSkipRest: () -> Unit,
    onOpenExerciseDetail: (String) -> Unit,
    onUpdateSetValuesExact: (Int, Float, Int, Int?) -> Unit = { _, _, _, _ -> },
    onDeleteSet: (Int) -> Unit = {},
    onAddSet: () -> Unit = {},
    onToggleAutoMode: (Boolean) -> Unit = {},
    onPreviousExercise: () -> Unit = onBack,
    onNextExercise: () -> Unit = { onSelectMeasurementMode(SubScreen.WORKOUT_SUMMARY) },
    onSwitchExercise: (ExerciseItem) -> Unit = {},
    onOpenExerciseLibrary: () -> Unit = {},
    exerciseFeedback: @Composable () -> Unit = {},
    onFinishWorkout: () -> Unit = { onSelectMeasurementMode(SubScreen.WORKOUT_SUMMARY) },
    onDiscardWorkout: () -> Unit = {}
) {
    val palette = LocalTrainPilotPalette.current
    val restMin = uiState.restTimerRemainingSec / 60
    val restSec = uiState.restTimerRemainingSec % 60
    val formattedRest = String.format("%02d:%02d", restMin, restSec)

    val workoutExercises = remember(uiState.activeWorkoutExercises, uiState.exercises) {
        uiState.activeWorkoutExercises.ifEmpty { uiState.exercises.take(6) }
    }
    val totalExercisesCount = workoutExercises.size.coerceAtLeast(1)
    val currentExerciseIdx = uiState.activeExerciseIndex.coerceIn(0, totalExercisesCount - 1)
    val currentExercise = workoutExercises.getOrNull(currentExerciseIdx)
        ?: uiState.exercises.find { it.id == uiState.selectedExerciseId }
        ?: uiState.exercises.firstOrNull()
    val lang = uiState.language
    val currentExerciseName = currentExercise?.localizedName(lang) ?: tr(lang, "Goblet guggolás", "Goblet Squat", "Goblet Squat", "Genuflexiune Goblet")
    val currentExerciseId = currentExercise?.id ?: "goblet_squat"
    val secWord = tr(lang, "mp", "s", "Sek.", "sec")
    val currentExerciseSubtitle = currentExercise?.let {
        listOf(it.localizedPrimaryMuscles(lang), it.localizedEquipment(lang))
            .filter(String::isNotBlank).joinToString(" • ")
    }.orEmpty()
    val coachOwner = com.example.ui.components.rememberStudioWorkoutCoachOwner()
    // 2775: previous saved values per set; empty without history (nothing shown, nothing invented).
    val lastSets = remember(uiState.loggedWorkouts, currentExercise, uiState.activeWorkoutId) {
        com.example.ui.components.lastLoggedSets(uiState.loggedWorkouts, currentExercise, uiState.activeWorkoutId)
    }
    val columnLabels = com.example.ui.components.strengthColumnLabels(currentExercise, uiState.language)

    val listState = rememberLazyListState()
    // #347: the rest starts compact after a set is ticked; the automatic scroll after ticking never opens it.
    // 2808/9: the user's own scroll (NestedScrollSource.UserInput, with its fling) sizes it by position again:
    // down onto the set rows -> large, back up above them -> compact. The arrow / dock drag still toggle.
    var restTimerLarge by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.restTimerRunning) { if (uiState.restTimerRunning) restTimerLarge = false }
    val restVisible = com.repforge.app.StudioRestTimer.visible(uiState)
    val userScrollSizesRest = com.example.ui.components.rememberRestDockScrollConnection(
        listState, restVisible, restTimerLarge) { restTimerLarge = it }
    val isRestTimerLargeMode = restTimerLarge

    StudioRestCountdown(uiState, onAdjustRestTimer)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("active_strength_screen")
    ) {
        uiState.statusBannerMessage?.let { Text(it, color = palette.danger) }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .nestedScroll(userScrollSizesRest),
            contentPadding = PaddingValues(start = TrainPilotCardSpacing.ScreenGutter, end = TrainPilotCardSpacing.ScreenGutter, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)
        ) {
            // 2775: shared header (title, day badge + Coach, exercise strip) and exercise title row.
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                com.example.ui.components.StudioActiveWorkoutTop(uiState, workoutExercises, currentExerciseIdx,
                    onBack, onOpenSettings, onSwitchExercise)
                com.example.ui.components.StudioActiveExerciseTitleRow(lang, currentExerciseName, currentExerciseSubtitle,
                    currentExercise, currentExerciseId, onOpenExerciseLibrary, onOpenExerciseDetail,
                    uiState.selectedExerciseId, coachOwner)
            }
        }

        // Section header
        item {
            Text(
                text = tr(uiState.language, "Sorozatok", "Sets", "Sätze", "Serii"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
        }

        // Inline set cards share the editor mockup style; values remain native-backed.
        items(uiState.activeSets, key = { it.id.ifBlank { it.setNumber.toString() } }) { setItem ->
            val canEdit = uiState.phoneControlsWorkout && !uiState.watchControlPending && !uiState.isSavingWorkout
            val identity = "${uiState.activeWorkoutId}/${uiState.activeWorkoutGeneration}/$currentExerciseId/${setItem.id}/${setItem.setNumber}"
            com.example.ui.components.StudioStrengthSetCard(
                item = setItem, identity = identity, language = lang,
                weightLabel = columnLabels.weight, repsLabel = columnLabels.repetitions,
                enabled = canEdit,
                active = !setItem.completed && uiState.activeSets.firstOrNull { !it.completed }?.setNumber == setItem.setNumber,
                onValues = { kg, reps, rir -> onUpdateSetValuesExact(setItem.setNumber, kg, reps, rir) },
                onToggle = { onToggleSetCompleted(setItem.setNumber) },
                onDelete = { onDeleteSet(setItem.setNumber) },
                lastLabel = lastSets.firstOrNull { it.setNumber == setItem.setNumber }
                    ?.takeIf { com.example.ui.components.lastSetDiffers(it, setItem) }
                    ?.let { com.example.ui.components.lastSetLabel(it, lang) }
            )
        }

        // Add new set button
        item {
            TrainPilotOutlinedButton(
                onClick = onAddSet,
                enabled = uiState.phoneControlsWorkout && !uiState.watchControlPending && !uiState.isSavingWorkout,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = palette.primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    tr(lang, "Új sorozat", "New set", "Neuer Satz", "Serie nouă"),
                    color = palette.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Canonical history-based preview and explicit approval; no local progress formula.
        item(key = "active_exercise_feedback") { exerciseFeedback() }

        }

        // Fixed Sticky Bottom Dock:
        // 1) Sticky Rest Timer above Previous/Next (Compact flat mode by default; transitions to Large mode when scrolling down or swiping/tapping)
        // 2) Sticky < Előző | Következő > buttons
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = palette.glassCardFill(palette.background), // 2776 (A): darker dock; 2810/3: glass bar in Liquid Glass
            modifier = Modifier
                .fillMaxWidth()
                .activeWorkoutDockFrame(palette.background, palette.primary, 20.dp, glass = palette.isLiquidGlass)
                .trainPilotGlassMaterial(palette, RoundedCornerShape(20.dp))
                .pointerInput(isRestTimerLargeMode) {
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount < -12f) {
                            restTimerLarge = true
                        } else if (dragAmount > 12f) {
                            restTimerLarge = false
                        }
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 2778: shared rest dock (same component on every measurement screen).
                com.example.ui.components.StudioRestTimerDock(uiState, isRestTimerLargeMode,
                    onLarge = { restTimerLarge = it }, onAdjust = onAdjustRestTimer,
                    onToggle = onToggleRestRunning, onSkip = onSkipRest, skipLabel = strings.skipAction,
                    showIdle = false)

                // 2772: ‹ Previous · ⏹ Finish · Next › (Finish = save & finish / discard / keep training).
                com.example.ui.components.ActiveWorkoutBottomBar(
                    language = uiState.language,
                    enabled = uiState.phoneControlsWorkout && !uiState.watchControlPending && !uiState.isSavingWorkout,
                    hasNext = currentExerciseIdx + 1 < totalExercisesCount,
                    completedSets = uiState.activeSetsByExerciseId.values.sumOf { sets -> sets.count { it.completed } },
                    totalSets = uiState.activeSetsByExerciseId.values.sumOf { it.size },
                    elapsedLabel = "",
                    tagPrefix = "active",
                    onPrevious = onPreviousExercise,
                    onNext = onNextExercise,
                    onFinish = onFinishWorkout,
                    onDiscard = onDiscardWorkout
                )
            }
        }
    }
}

@Composable
internal fun SetValuePill(
    valueText: String,
    highlighted: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = palette.background.copy(alpha = 0.65f),
        modifier = Modifier
            .width(64.dp)
            .trainPilotBorder(
                width = 1.dp,
                color = if (highlighted) palette.primary else palette.cardBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .trainPilotClickable(RoundedCornerShape(10.dp)) { onClick() }
    ) {
        Text(
            text = valueText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = palette.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}
