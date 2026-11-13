package com.example.ui.screens

import androidx.compose.foundation.layout.sizeIn
import com.example.ui.components.WorkoutControlAppearance

import com.example.ui.components.TrainPilotAlertDialog as AlertDialog

import com.repforge.app.ui.theme.TrainPilotCardSpacing

import com.repforge.app.ui.theme.TrainPilotOutlinedButton

import com.repforge.app.ui.theme.TrainPilotFrames

import com.repforge.app.ui.theme.trainPilotBorder

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.PopupProperties
import androidx.core.content.ContextCompat
import com.example.localization.LocalTrainPilotStrings
import com.example.localization.LocalizedStrings
import com.example.model.ExerciseItem
import com.example.model.ExerciseSetItem
import com.example.model.SubScreen
import com.example.ui.components.ExercisePostureMiniIllustration
import com.example.ui.components.NativeWorkoutRouteMapBox
import com.example.ui.components.SmallExerciseVideoButton
import com.example.ui.components.SubScreenTopBar
import com.example.model.AppLanguage
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.produceState
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.repforge.app.WorkoutPhotoRepository
import com.repforge.app.ActiveWorkoutStore
import org.json.JSONObject
import org.json.JSONArray
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private fun tr(lang: AppLanguage, hu: String, en: String, de: String, ro: String): String = when (lang) {
    AppLanguage.HU -> hu
    AppLanguage.EN -> en
    AppLanguage.DE -> de
    AppLanguage.RO -> ro
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MeasurementModeSwitcherRow(
    currentSubScreen: SubScreen,
    onSelectMode: (SubScreen) -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val strings = LocalTrainPilotStrings.current
    val modes = listOf(
        SubScreen.ACTIVE_STRENGTH to "1. ${strings.modeStrengthLabel}",
        SubScreen.ACTIVE_TIMED to "2. ${strings.modeTimedLabel}",
        SubScreen.ACTIVE_BILATERAL to "3. ${strings.modeBilateralLabel}",
        SubScreen.ACTIVE_OUTDOOR_GPS to "4. ${strings.modeOutdoorGpsLabel}",
        SubScreen.INTERVAL_SETTINGS to "5. ${strings.modeIntervalsLabel}",
        SubScreen.WORKOUT_SUMMARY to "6. ${strings.modeSummaryLabel}"
    )
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        modes.forEach { (mode, label) ->
            val selected = currentSubScreen == mode
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (selected) palette.primary else palette.surfaceElevated,
                modifier = Modifier
                    .height(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelectMode(mode) }
                    .testTag("measurement_mode_${mode.name.lowercase()}")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) palette.onPrimary else palette.textSecondary
                    )
                }
            }
        }
    }
}


@Composable
private fun StudioRestCountdown(state: TrainPilotUiState, onAdjust: (Int) -> Unit) {
    val frame by ActiveWorkoutStore.snapshotFlow.collectAsState()
    val allowed = com.repforge.app.StudioRestTimer.allowed(state, frame) ||
        com.repforge.app.StudioRestTimer.mirrorAllowed(state, frame)
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, allowed, state.restTimerRunning, state.restTimerRemainingSec) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME &&
                allowed && state.restTimerRunning && state.restTimerRemainingSec > 0
            ) {
                onAdjust(-1)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(
        state.activeWorkoutId,
        state.activeWorkoutGeneration,
        allowed,
        state.restTimerRunning,
        state.restTimerRemainingSec,
        state.restTimerEndRealtimeMs
    ) {
        if (allowed && state.restTimerRunning && state.restTimerRemainingSec > 0) {
            val nowRealtime = android.os.SystemClock.elapsedRealtime()
            if (state.restTimerEndRealtimeMs > 0L &&
                state.restTimerLastTickRealtimeMs > 0L &&
                nowRealtime - state.restTimerLastTickRealtimeMs >= 1500L
            ) {
                onAdjust(-1)
                return@LaunchedEffect
            }
            delay(1000L)
            onAdjust(-1)
        }
    }
}

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
    exerciseFeedback: @Composable () -> Unit = {}
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
    val restWord = tr(lang, "Pihenő", "Rest", "Pause", "Pauză")
    val secWord = tr(lang, "mp", "s", "Sek.", "sec")
    val currentExerciseSubtitle = if (currentExercise != null) {
        "${currentExercise.localizedPrimaryMuscles(lang)} • ${currentExercise.localizedEquipment(lang)} • ${currentExercise.defaultSets}×${currentExercise.defaultReps} • $restWord: ${currentExercise.defaultRestSec} $secWord"
    } else {
        tr(lang, "Láb • Kézisúlyzó • 3×8–20 • Pihenő: 90 mp", "Legs • Dumbbell • 3×8–20 • Rest: 90 s", "Beine • Kurzhantel • 3×8–20 • Pause: 90 Sek.", "Picioare • Ganteră • 3×8–20 • Pauză: 90 sec")
    }
    val coachOwner = com.example.ui.components.rememberStudioWorkoutCoachOwner()
    val columnLabels = com.example.ui.components.strengthColumnLabels(currentExercise, uiState.language)
    var exerciseDropdownExpanded by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val isScrolledDown by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 130
        }
    }
    var restTimerManualOverride by remember { mutableStateOf<Boolean?>(null) }
    // When scrolling changes, allow automatic compact <-> large switching unless manually pinned
    LaunchedEffect(isScrolledDown) {
        restTimerManualOverride = null
    }
    val isRestTimerLargeMode = restTimerManualOverride ?: isScrolledDown

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
                .fillMaxWidth(),
            contentPadding = PaddingValues(start = TrainPilotCardSpacing.ScreenGutter, end = TrainPilotCardSpacing.ScreenGutter, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)
        ) {
            // Top Workout Progress & Exercise Sequence Strip + Floating Exercise Switcher
        item {
            val localizedWorkoutTitle = remember(uiState.activeWorkoutTitleHu, lang, uiState.programs, uiState.exercises) {
                com.example.model.LoggedWorkoutEntry(
                    id = "",
                    titleHu = uiState.activeWorkoutTitleHu
                ).localizedTitle(lang, uiState.programs, uiState.exercises)
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                com.example.ui.components.StudioActiveWorkoutHeaderSequence(
                    state = uiState,
                    header = {
                        SubScreenTopBar(
                        title = tr(
                        lang,
                        "$localizedWorkoutTitle • ${currentExerciseIdx + 1} / $totalExercisesCount gyakorlat",
                        "$localizedWorkoutTitle • ${currentExerciseIdx + 1} / $totalExercisesCount exercises",
                        "$localizedWorkoutTitle • ${currentExerciseIdx + 1} / $totalExercisesCount Übungen",
                        "$localizedWorkoutTitle • ${currentExerciseIdx + 1} / $totalExercisesCount exerciții"
                        ),
                        onBack = onBack,
                        onOpenSettings = onOpenSettings,
                        rightExtraContent = { com.example.ui.components.StudioWorkoutHealthAction() }
                        )
                    },
                    exerciseSequence = {
                        com.example.ui.components.StudioExerciseSequenceStrip(
                            state = uiState, exercises = workoutExercises,
                            currentIndex = currentExerciseIdx, onSelect = onSwitchExercise)
                    }
                )

                // Current Exercise Header + Floating Exercise Switcher Dropdown + Library Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { exerciseDropdownExpanded = true }
                                .padding(vertical = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = currentExerciseName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = tr(lang, "Gyakorlat váltása", "Switch exercise", "Übung wechseln", "Schimbă exercițiul"),
                                    tint = palette.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = currentExerciseSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                        }

                        DropdownMenu(
                            expanded = exerciseDropdownExpanded,
                            onDismissRequest = { exerciseDropdownExpanded = false },
                            properties = PopupProperties(focusable = true),
                            modifier = Modifier
                                .background(palette.surfaceElevated)
                                .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MenuBook,
                                            contentDescription = null,
                                            tint = palette.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = tr(
                                                lang,
                                                "Választás a ${uiState.exercises.size} gyakorlatkönyvtárból...",
                                                "Choose from the ${uiState.exercises.size} exercise library...",
                                                "Aus der ${uiState.exercises.size} Übungsbibliothek wählen...",
                                                "Alege din biblioteca de ${uiState.exercises.size} exerciții..."
                                            ),
                                            color = palette.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                },
                                onClick = {
                                    exerciseDropdownExpanded = false
                                    onOpenExerciseLibrary()
                                }
                            )
                            HorizontalDivider(color = palette.cardBorder)
                            val switchableList = (workoutExercises + uiState.exercises.filter { !it.isTimed })
                                .distinctBy { it.id }

                            switchableList.forEach { exOption ->
                                val isSel = exOption.id == currentExerciseId
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = exOption.localizedName(lang),
                                                color = if (isSel) palette.primary else palette.textPrimary,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                            )
                                            Text(
                                                text = "${exOption.localizedPrimaryMuscles(lang)} • ${exOption.localizedEquipment(lang)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = palette.textSecondary
                                            )
                                        }
                                    },
                                    onClick = {
                                        exerciseDropdownExpanded = false
                                        onSwitchExercise(exOption)
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SmallExerciseVideoButton(exerciseName = currentExerciseName, exerciseId = currentExerciseId)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = palette.surfaceElevated,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onOpenExerciseDetail(currentExerciseId) }
                        ) {
                            Text(
                                text = tr(lang, "Tárcsa & Bemelegítés", "Plates & Warm-up", "Scheiben & Aufwärmen", "Discuri & Încălzire"),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.textPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
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
                onDelete = { onDeleteSet(setItem.setNumber) }
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
                    tr(lang, "+ Új sorozat hozzáadása", "+ Add new set", "+ Neuen Satz hinzufügen", "+ Adaugă o serie nouă"),
                    color = palette.textPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Canonical history-based preview and explicit approval; no local progress formula.
        item(key = "active_exercise_feedback") { exerciseFeedback() }
        item(key = "active_coach") { com.example.ui.components.StudioWorkoutCoachPanel(uiState.selectedExerciseId, coachOwner) }

        }

        // Fixed Sticky Bottom Dock:
        // 1) Sticky Rest Timer above Previous/Next (Compact flat mode by default; transitions to Large mode when scrolling down or swiping/tapping)
        // 2) Sticky < Előző | Következő > buttons
        Surface(
            color = palette.surfaceElevated,
            modifier = Modifier
                .fillMaxWidth()
                .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .pointerInput(isRestTimerLargeMode) {
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount < -12f) {
                            restTimerManualOverride = true
                        } else if (dragAmount > 12f) {
                            restTimerManualOverride = false
                        }
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (com.repforge.app.StudioRestTimer.visible(uiState)) {
                val progress = (uiState.restTimerRemainingSec.toFloat() / uiState.defaultRestSec.coerceAtLeast(1)).coerceIn(0f, 1f)
                if (!isRestTimerLargeMode) {
                    // COMPACT FLAT STICKY REST TIMER BAR
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = palette.surfaceCard,
                        modifier = Modifier
                            .fillMaxWidth()
                            .trainPilotBorder(
                                width = 1.dp,
                                color = if (uiState.restTimerRunning) palette.primary else palette.cardBorder,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { restTimerManualOverride = true }
                            .testTag("sticky_rest_timer_compact")
                    ) {
                        com.example.ui.components.StudioCompactRestControls(
                            language = lang, remainingSec = uiState.restTimerRemainingSec,
                            running = uiState.restTimerRunning,
                            enabled = uiState.phoneControlsWorkout && !uiState.watchControlPending,
                            onAdjust = onAdjustRestTimer, onToggle = onToggleRestRunning,
                            onSkip = onSkipRest, onExpand = { restTimerManualOverride = true },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp))
                    }
                } else {
                    // LARGE EXPANDED STICKY REST TIMER CARD (shown when scrolled down or expanded)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = palette.surfaceCard,
                        modifier = Modifier
                            .fillMaxWidth()
                            .trainPilotBorder(0.5.dp, palette.primary.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                            .testTag("sticky_rest_timer_large")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Timer,
                                            contentDescription = null,
                                            tint = palette.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = tr(lang, "Pihenőidő (Nagy mód)", "Rest timer (Expanded)", "Pausenzeit (Groß)", "Timp de pauză (Extins)"),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = palette.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = palette.surfaceElevated,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { restTimerManualOverride = false }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = tr(lang, "Lapos mód", "Compact mode", "Kompaktmodus", "Mod compact"),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = palette.textSecondary
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.ExpandMore,
                                                    contentDescription = tr(lang, "Kis lapos mód", "Compact rest timer", "Kompakter Pausenmodus", "Mod pauză compact"),
                                                    tint = palette.textSecondary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = formattedRest,
                                        style = MaterialTheme.typography.headlineLarge.copy(fontFeatureSettings = "tnum"),
                                        fontWeight = FontWeight.ExtraBold,
                                        color = palette.textPrimary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Text(
                                        text = if (uiState.restTimerRunning) {
                                            tr(lang, "Visszaszámlálás fut • Rezgés a végén", "Countdown running • Vibrates at end", "Countdown läuft • Vibration am Ende", "Numărătoare inversă • Vibrație la final")
                                        } else {
                                            tr(lang, "Koppints a körre vagy az Indításra", "Tap the ring or Start", "Tippe auf den Kreis oder Start", "Atinge inelul sau Start")
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.textSecondary
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(58.dp)
                                        .clip(CircleShape)
                                        .clickable(enabled = uiState.phoneControlsWorkout && !uiState.watchControlPending) { onToggleRestRunning() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    val ringColor = palette.primary
                                    val trackColor = palette.surfaceElevated
                                    Canvas(modifier = Modifier.size(54.dp)) {
                                        drawArc(
                                            color = trackColor,
                                            startAngle = 0f,
                                            sweepAngle = 360f,
                                            useCenter = false,
                                            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                        drawArc(
                                            color = ringColor,
                                            startAngle = -90f,
                                            sweepAngle = 360f * progress,
                                            useCenter = false,
                                            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                    }
                                    Icon(
                                        imageVector = if (uiState.restTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = palette.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TrainPilotOutlinedButton(
                                    onClick = { onAdjustRestTimer(-15) },
                                    enabled = uiState.phoneControlsWorkout && !uiState.watchControlPending,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("-15 $secWord", color = palette.textPrimary, fontWeight = FontWeight.Bold)
                                }
                                TrainPilotOutlinedButton(
                                    onClick = { onAdjustRestTimer(15) },
                                    enabled = uiState.phoneControlsWorkout && !uiState.watchControlPending,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("+15 $secWord", color = palette.textPrimary, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = onSkipRest,
                                    enabled = uiState.phoneControlsWorkout && !uiState.watchControlPending,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = palette.primary,
                                        contentColor = palette.onPrimary
                                    ),
                                    modifier = Modifier.weight(1.2f).height(40.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(strings.skipAction, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                }

                // Sticky < Előző | Következő > Navigation Row
                val hasNextExercise = currentExerciseIdx + 1 < totalExercisesCount
                val nextExName = workoutExercises.getOrNull(currentExerciseIdx + 1)?.localizedName(lang)
                val prevWord = tr(lang, "Előző", "Previous", "Zurück", "Anterior")
                val nextWord = tr(lang, "Következő", "Next", "Weiter", "Următorul")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TrainPilotOutlinedButton(
                        onClick = onPreviousExercise,
                        colors = WorkoutControlAppearance.secondaryColors(),
                        border = WorkoutControlAppearance.secondaryBorder(),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("active_prev_exercise_btn")
                    ) {
                        Text(
                            text = if (currentExerciseIdx > 0) "< $prevWord (${currentExerciseIdx}/$totalExercisesCount)" else "< $prevWord",
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = onNextExercise,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.primary,
                            contentColor = palette.onPrimary
                        ),
                        modifier = Modifier
                            .weight(1.45f)
                            .height(48.dp)
                            .testTag("active_next_exercise_btn")
                    ) {
                        Text(
                            text = if (hasNextExercise) {
                                "$nextWord: ${nextExName ?: tr(lang, "${currentExerciseIdx + 2}. gyakorlat", "Exercise #${currentExerciseIdx + 2}", "${currentExerciseIdx + 2}. Übung", "Exercițiul #${currentExerciseIdx + 2}")} >"
                            } else {
                                tr(lang, "Edzés befejezése >", "Finish workout >", "Training beenden >", "Finalizare antrenament >")
                            },
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SetValuePill(
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
            .clickable { onClick() }
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

@Composable
private fun SmallAdjustChip(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = palette.surfaceElevated,
        modifier = Modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled) { onClick() }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
            color = palette.textPrimary.copy(alpha = if (enabled) 1f else .38f),
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
        )
    }
}

/**
 * 2. IDŐALAPÚ EDZÉS (Mockup 2/4 #2 - Plank)
 */
@Composable
fun ActiveTimedWorkoutScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectMeasurementMode: (SubScreen) -> Unit,
    onPreviousExercise: () -> Unit = onBack,
    onNextExercise: () -> Unit = { onSelectMeasurementMode(SubScreen.WORKOUT_SUMMARY) },
    onSwitchExercise: (ExerciseItem) -> Unit = {},
    onOpenExerciseLibrary: () -> Unit = {},
    exerciseFeedback: @Composable () -> Unit = {}
) {
    val palette = LocalTrainPilotPalette.current
    val workoutExercises = remember(uiState.activeWorkoutExercises, uiState.exercises) {
        uiState.activeWorkoutExercises.ifEmpty { uiState.exercises.filter { it.isTimed }.ifEmpty { uiState.exercises.take(4) } }
    }
    val totalCount = workoutExercises.size.coerceAtLeast(1)
    val currentIdx = uiState.activeExerciseIndex.coerceIn(0, totalCount - 1)
    val currentExercise = workoutExercises.getOrNull(currentIdx)
        ?: uiState.exercises.find { it.id == uiState.selectedExerciseId }
        ?: uiState.exercises.firstOrNull { it.isTimed }
    val exerciseNameHu = currentExercise?.localizedName(uiState.language) ?: "Plank"
    val defaultTargetSec = (currentExercise?.targetSeconds ?: 45).coerceAtLeast(15)

    val nativeModel: com.repforge.app.ui.workout.ActiveWorkoutViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val native by nativeModel.uiState.collectAsState()
    LaunchedEffect(uiState.activeWorkoutId) { nativeModel.onEditorVisible() }
    val nativeExercise = native.exercises.firstOrNull { it.exerciseId == currentExercise?.id }
    val nativeSet = nativeExercise?.sets?.firstOrNull { !it.isCompleted }
    val target = nativeSet?.let { com.repforge.app.NativeWorkoutTarget(native.workoutId, native.generation,
        checkNotNull(nativeExercise).exerciseId, it.setId) }
    DisposableEffect(target) { onDispose { nativeModel.cancelPendingStopwatchStart() } }
    val currentSec = if (native.stopwatchSetId == nativeSet?.setId) native.stopwatchSeconds.toInt() else 0
    val isRunning = native.stopwatchSetId == nativeSet?.setId && native.stopwatchRunning
    var targetSec by remember(currentExercise?.id) { mutableIntStateOf(defaultTargetSec) }
    val currentSetIndex = nativeSet?.setNumber ?: ((nativeExercise?.sets?.size ?: 0) + 1)
    val completedSets = nativeExercise?.sets?.filter { it.isCompleted }?.map { it.reps.toIntOrNull() ?: 0 }.orEmpty()
    var timedDropdownExpanded by remember { mutableStateOf(false) }

    val timedExerciseOptions = remember(uiState.exercises) {
        val fromDb = uiState.exercises.filter {
            it.isTimed ||
                it.nameHu.contains("plank", ignoreCase = true) ||
                it.nameHu.contains("tartás", ignoreCase = true) ||
                it.nameHu.contains("futás", ignoreCase = true) ||
                it.nameHu.contains("séta", ignoreCase = true) ||
                it.nameHu.contains("függeszkedés", ignoreCase = true)
        }
        val presets = listOf(
            ExerciseItem(id = "plank", nameHu = "Plank", nameEn = "Plank", nameDe = "Plank (Unterarmstütz)", nameRo = "Plank", isTimed = true, targetSeconds = 45, primaryMusclesHu = "Törzs / Has"),
            ExerciseItem(id = "side_plank", nameHu = "Oldalsó plank", nameEn = "Side Plank", nameDe = "Seitlicher Plank", nameRo = "Plank lateral", isTimed = true, targetSeconds = 40, primaryMusclesHu = "Oldalsó hasizom"),
            ExerciseItem(id = "wall_sit", nameHu = "Falnál ülés (Wall sit)", nameEn = "Wall Sit", nameDe = "Wandsitzen (Wall Sit)", nameRo = "Stat la perete (Wall sit)", isTimed = true, targetSeconds = 60, primaryMusclesHu = "Comb"),
            ExerciseItem(id = "dead_hang", nameHu = "Függeszkedés (Dead hang)", nameEn = "Dead Hang", nameDe = "Aushängen (Dead Hang)", nameRo = "Atârnare la bară (Dead hang)", isTimed = true, targetSeconds = 45, primaryMusclesHu = "Alkar / Hát"),
            ExerciseItem(id = "hollow_hold", nameHu = "Hollow body tartás", nameEn = "Hollow Hold", nameDe = "Hollow-Body-Halten", nameRo = "Menținere Hollow body", isTimed = true, targetSeconds = 30, primaryMusclesHu = "Hasizom"),
            ExerciseItem(id = "l_sit", nameHu = "L-tartás (L-sit)", nameEn = "L-Sit", nameDe = "L-Sitz (L-Sit)", nameRo = "Menținere în L (L-sit)", isTimed = true, targetSeconds = 30, primaryMusclesHu = "Törzs"),
            ExerciseItem(id = "track_run", nameHu = "Kör futás (Időalapú)", nameEn = "Track Run", nameDe = "Bahnlauf (Zeitbasiert)", nameRo = "Alergare pe pistă (Cronometrat)", isTimed = true, targetSeconds = 60, primaryMusclesHu = "Kardió / Láb")
        )
        (fromDb + presets).distinctBy { it.nameHu.lowercase() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("active_timed_screen")
    ) {
        uiState.statusBannerMessage?.let { Text(it, color = palette.danger) }
        native.statusMessage?.let { Text(it, color = palette.danger) }
        if (target == null) Text(tr(uiState.language, "Nincs mérhető sorozat az aktuális natív gyakorlatban.",
            "No measurable set in the current native exercise.", "Kein messbarer Satz in der aktuellen nativen Übung.",
            "Nu există o serie măsurabilă în exercițiul nativ curent."), color = palette.textSecondary)

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = TrainPilotCardSpacing.ScreenGutter, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)
        ) {
            item {
                val localizedWorkoutTitle = com.example.model.LoggedWorkoutEntry(
                    id = "",
                    titleHu = uiState.activeWorkoutTitleHu
                ).localizedTitle(uiState.language, uiState.programs, uiState.exercises)
                SubScreenTopBar(
                    title = tr(
                        uiState.language,
                        "$localizedWorkoutTitle • ${currentIdx + 1} / $totalCount gyakorlat",
                        "$localizedWorkoutTitle • ${currentIdx + 1} / $totalCount exercises",
                        "$localizedWorkoutTitle • ${currentIdx + 1} / $totalCount Übungen",
                        "$localizedWorkoutTitle • ${currentIdx + 1} / $totalCount exerciții"
                    ),
                    onBack = onBack,
                    onOpenSettings = onOpenSettings,
                    rightExtraContent = { com.example.ui.components.StudioWorkoutHealthAction() }
                )
            }
            item {
                val lang = uiState.language
                val secWord = tr(lang, "mp", "s", "Sek.", "sec")
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = palette.surfaceCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.weight(1f)) {
                                Column(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { timedDropdownExpanded = true }
                                        .padding(vertical = 2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = exerciseNameHu,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.textPrimary
                                        )
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = tr(lang, "Időalapú gyakorlat váltása", "Switch timed exercise", "Zeitbasierte Übung wechseln", "Schimbă exercițiul cronometrat"),
                                            tint = palette.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Text(
                                        text = tr(
                                            lang,
                                            "Cél: $currentSetIndex. sorozat • Célidő: $targetSec mp • Koppints a váltáshoz",
                                            "Target: Set #$currentSetIndex • Target time: $targetSec s • Tap to switch",
                                            "Ziel: $currentSetIndex. Satz • Zielzeit: $targetSec Sek. • Tippen zum Wechseln",
                                            "Țintă: Seria #$currentSetIndex • Timp țintă: $targetSec sec • Atinge pentru schimbare"
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.textSecondary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isRunning) palette.primary.copy(alpha = 0.2f) else palette.secondary.copy(alpha = 0.18f)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Timer,
                                                contentDescription = null,
                                                tint = if (isRunning) palette.primary else palette.secondary,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (isRunning) {
                                                    tr(lang, "Stopper fut ●", "Stopwatch running ●", "Stoppuhr läuft ●", "Cronometrul rulează ●")
                                                } else {
                                                    tr(lang, "Stopper készenlétben", "Stopwatch ready", "Stoppuhr bereit", "Cronometru pregătit")
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isRunning) palette.primary else palette.secondary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                DropdownMenu(
                                    expanded = timedDropdownExpanded,
                                    onDismissRequest = { timedDropdownExpanded = false },
                                    properties = PopupProperties(focusable = true),
                                    modifier = Modifier
                                        .background(palette.surfaceElevated)
                                        .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(14.dp))
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = tr(
                                                    lang,
                                                    "Gyakorlatkönyvtár megnyitása...",
                                                    "Open Exercise Library...",
                                                    "Übungsbibliothek öffnen...",
                                                    "Deschide biblioteca de exerciții..."
                                                ),
                                                color = palette.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        },
                                        onClick = {
                                            timedDropdownExpanded = false
                                            onOpenExerciseLibrary()
                                        }
                                    )
                                    HorizontalDivider(color = palette.cardBorder)
                                    timedExerciseOptions.forEach { option ->
                                        val isSel = option.id == currentExercise?.id || option.localizedName(lang).equals(exerciseNameHu, ignoreCase = true)
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "${option.localizedName(lang)} (${option.targetSeconds.coerceAtLeast(30)} $secWord)",
                                                    color = if (isSel) palette.primary else palette.textPrimary,
                                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                                )
                                            },
                                            onClick = {
                                                timedDropdownExpanded = false
                                                onSwitchExercise(option)
                                            }
                                        )
                                    }
                                }
                            }
                            SmallExerciseVideoButton(
                                exerciseName = exerciseNameHu,
                                exerciseId = currentExercise?.id ?: timedExerciseOptions.firstOrNull { it.localizedName(lang) == exerciseNameHu }?.id
                            )
                        }

                    Spacer(modifier = Modifier.height(20.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        val progress = if (targetSec > 0) (currentSec.toFloat() / targetSec).coerceIn(0f, 1f) else 0f
                        val primaryColor = if (currentSec >= targetSec) palette.success else palette.primary
                        val trackColor = palette.surfaceElevated
                        Canvas(modifier = Modifier.size(190.dp)) {
                            drawArc(
                                color = trackColor,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = primaryColor,
                                startAngle = -90f,
                                sweepAngle = 360f * progress,
                                useCenter = false,
                                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            StudioTimerReadout(
                                seconds = currentSec,
                                style = MaterialTheme.typography.displayLarge,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                            val remaining = (targetSec - currentSec).coerceAtLeast(0)
                            Text(
                                text = if (currentSec >= targetSec) {
                                    tr(lang, "Cél teljesítve! 🎉", "Target reached! 🎉", "Ziel erreicht! 🎉", "Țintă atinsă! 🎉")
                                } else {
                                    tr(lang, "Hátra: $remaining mp", "Remaining: $remaining s", "Verbleibend: $remaining Sek.", "Rămas: $remaining sec")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (currentSec >= targetSec) palette.success else palette.textSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = tr(lang, "Cél: ${com.repforge.app.StudioWorkoutSessionPresentation.durationLabel(targetSec.toLong())}", "Target: ${com.repforge.app.StudioWorkoutSessionPresentation.durationLabel(targetSec.toLong())}", "Ziel: ${com.repforge.app.StudioWorkoutSessionPresentation.durationLabel(targetSec.toLong())}", "Țintă: ${com.repforge.app.StudioWorkoutSessionPresentation.durationLabel(targetSec.toLong())}"),
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Quick adjustment chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SmallAdjustChip("-5 $secWord") { target?.let { nativeModel.adjustSetStopwatch(it, "single", -5) } }
                        SmallAdjustChip("+5 $secWord") { target?.let { nativeModel.adjustSetStopwatch(it, "single", 5) } }
                        SmallAdjustChip("+15 $secWord") { target?.let { nativeModel.adjustSetStopwatch(it, "single", 15) } }
                        SmallAdjustChip("+30 $secWord") { target?.let { nativeModel.adjustSetStopwatch(it, "single", 30) } }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Stopwatch Controls: Start/Pause + Reset
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { target?.let { if (isRunning) nativeModel.pauseSetStopwatch(it) else nativeModel.startSetStopwatch(it, "single") } },
                            enabled = target != null,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRunning) palette.primary else palette.success,
                                contentColor = palette.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1.4f)
                                .height(48.dp)
                                .testTag("toggle_timed_stopper")
                        ) {
                            Icon(
                                imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isRunning) {
                                    tr(lang, "Szünet", "Pause", "Pause", "Pauză")
                                } else {
                                    tr(lang, "Stopper indítása", "Start stopwatch", "Stoppuhr starten", "Pornește cronometrul")
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TrainPilotOutlinedButton(
                            onClick = {
                                target?.let(nativeModel::resetSetStopwatch)
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = palette.textPrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.resetAction, color = palette.textPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Set complete button
                    com.repforge.app.ui.theme.TrainPilotSaveButton(
                        onClick = {
                            target?.let { nativeModel.recordSetStopwatch(it, "single") }
                        },
                        enabled = target != null && currentSec > 0,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tr(
                                lang,
                                "Sorozat rögzítése (${String.format("%02d:%02d", currentSec / 60, currentSec % 60)}) ✓",
                                "Record set (${String.format("%02d:%02d", currentSec / 60, currentSec % 60)}) ✓",
                                "Satz speichern (${String.format("%02d:%02d", currentSec / 60, currentSec % 60)}) ✓",
                                "Salvează seria (${String.format("%02d:%02d", currentSec / 60, currentSec % 60)}) ✓"
                            ),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Target Preset selector
        item {
            val lang = uiState.language
            val secWord = tr(lang, "mp", "s", "Sek.", "sec")
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = tr(lang, "Célidő beállítása:", "Set target time:", "Zielzeit einstellen:", "Setează timpul țintă:"),
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(30, 45, 60, 90).forEach { sec ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (targetSec == sec) palette.primary else palette.surfaceElevated,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { targetSec = sec }
                            ) {
                                Text(
                                    text = "$sec $secWord",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (targetSec == sec) palette.onPrimary else palette.textPrimary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Completed sets list
        item {
            val lang = uiState.language
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = tr(lang, "Eddigi sorozatok:", "Sets so far:", "Bisherige Sätze:", "Serii până acum:"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.primary
                    )
                    completedSets.forEachIndexed { index, duration ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(tr(lang, "${index + 1}. sorozat", "Set #${index + 1}", "${index + 1}. Satz", "Seria #${index + 1}"), color = palette.textSecondary)
                            Text(
                                text = String.format("%02d:%02d ✓", duration / 60, duration % 60),
                                color = palette.success,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = tr(lang, "$currentSetIndex. sorozat (Aktuális)", "Set #$currentSetIndex (Current)", "$currentSetIndex. Satz (Aktuell)", "Seria #$currentSetIndex (Curentă)"),
                            color = palette.primary,
                            fontWeight = FontWeight.Bold
                        )
                        StudioTimerReadout(
                            seconds = currentSec,
                            style = MaterialTheme.typography.bodyLarge,
                            color = palette.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

            item(key = "active_exercise_feedback") { exerciseFeedback() }
        }

        // Fixed Sticky Bottom navigation buttons
        Surface(
            color = palette.surfaceElevated,
            modifier = Modifier
                .fillMaxWidth()
                .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
        ) {
            val lang = uiState.language
            val hasNext = currentIdx + 1 < totalCount
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TrainPilotOutlinedButton(
                    onClick = onPreviousExercise,
                    colors = WorkoutControlAppearance.secondaryColors(),
                    border = WorkoutControlAppearance.secondaryBorder(),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                ) {
                    Text(tr(lang, "< Előző", "< Previous", "< Zurück", "< Anterior"), fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onNextExercise,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primary,
                        contentColor = palette.onPrimary
                    ),
                    modifier = Modifier.weight(1.3f).height(48.dp)
                ) {
                    Text(
                        text = if (hasNext) {
                            tr(
                                lang,
                                "Következő (${currentIdx + 2}/$totalCount) >",
                                "Next (${currentIdx + 2}/$totalCount) >",
                                "Weiter (${currentIdx + 2}/$totalCount) >",
                                "Următorul (${currentIdx + 2}/$totalCount) >"
                            )
                        } else {
                            tr(lang, "Edzés befejezése >", "Finish workout >", "Training beenden >", "Finalizare antrenament >")
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * 3. KÉTOLDALI MÉRÉS (2 STOPPER OLDALANKÉNT: Bal és Jobb oldal külön-külön stopperrel + oldalankénti sorozat-adatrögzítő)
 */
@Composable
fun ActiveBilateralWorkoutScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectMeasurementMode: (SubScreen) -> Unit,
    onRecordBilateralSide: (Boolean, Int, Int?) -> Unit = { _, _, _ -> },
    onUpdateBilateralSetExact: (Int, Int, Int, Int?) -> Unit = { _, _, _, _ -> },
    onToggleSetCompleted: (Int) -> Unit = {},
    onAddSet: () -> Unit = {},
    onDeleteSet: (Int) -> Unit = {},
    onAdjustRestTimer: (Int) -> Unit = {},
    onToggleRestRunning: () -> Unit = {},
    onSkipRest: () -> Unit = {},
    onPreviousExercise: () -> Unit = onBack,
    onNextExercise: () -> Unit = { onSelectMeasurementMode(SubScreen.WORKOUT_SUMMARY) },
    onSwitchExercise: (com.example.model.ExerciseItem) -> Unit = {},
    onOpenExerciseLibrary: () -> Unit = {},
    exerciseFeedback: @Composable () -> Unit = {}
) {
    val palette = LocalTrainPilotPalette.current

    val workoutExercises = uiState.activeWorkoutExercises.ifEmpty { uiState.exercises.take(6) }
    val currentExercise = workoutExercises.getOrNull(uiState.activeExerciseIndex)
        ?: uiState.exercises.find { it.id == uiState.selectedExerciseId }
        ?: uiState.exercises.firstOrNull { it.isBilateral || it.nameHu.contains("oldalsó plank", ignoreCase = true) }

    val lang = uiState.language
    val secWord = tr(lang, "mp", "s", "Sek.", "sec")
    val exerciseTitle = currentExercise?.localizedName(lang) ?: tr(lang, "Oldalsó plank", "Side Plank", "Seitlicher Plank", "Plank lateral")
    val defaultTargetSec = currentExercise?.targetSeconds ?: 30

    // Determine target set for stopwatch recording (defaults to first uncompleted set)
    val firstUncompletedSetNum = uiState.activeSets.firstOrNull { !it.completed }?.setNumber
        ?: uiState.activeSets.lastOrNull()?.setNumber
        ?: 1
    var selectedTargetSetNumber by remember(uiState.activeWorkoutId, currentExercise?.id, firstUncompletedSetNum, uiState.activeSets.size) {
        mutableIntStateOf(firstUncompletedSetNum)
    }

    val nativeModel: com.repforge.app.ui.workout.ActiveWorkoutViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val native by nativeModel.uiState.collectAsState()
    LaunchedEffect(uiState.activeWorkoutId) { nativeModel.onEditorVisible() }
    val nativeExercise = native.exercises.firstOrNull { it.exerciseId == currentExercise?.id }
    val nativeSet = nativeExercise?.sets?.firstOrNull { it.setNumber == selectedTargetSetNumber }
    val target = nativeSet?.let { com.repforge.app.NativeWorkoutTarget(native.workoutId, native.generation,
        checkNotNull(nativeExercise).exerciseId, it.setId) }
    DisposableEffect(target) { onDispose { nativeModel.cancelPendingStopwatchStart() } }
    val leftSec = if (native.stopwatchSetId == nativeSet?.setId && native.stopwatchSide == "left") native.stopwatchSeconds.toInt() else nativeSet?.leftSeconds ?: 0
    val rightSec = if (native.stopwatchSetId == nativeSet?.setId && native.stopwatchSide == "right") native.stopwatchSeconds.toInt() else nativeSet?.rightSeconds ?: 0
    val leftRunning = native.stopwatchSetId == nativeSet?.setId && native.stopwatchSide == "left" && native.stopwatchRunning
    val rightRunning = native.stopwatchSetId == nativeSet?.setId && native.stopwatchSide == "right" && native.stopwatchRunning

    // Modal dialog for manual bilateral set editing
    var editingBilateralSet by remember { mutableStateOf<com.example.model.ExerciseSetItem?>(null) }
    var showExerciseDropdown by remember { mutableStateOf(false) }
    var restTimerLargeMode by remember(com.repforge.app.StudioRestTimer.visible(uiState)) { mutableStateOf(false) }
    StudioRestCountdown(uiState, onAdjustRestTimer)

    // Dedicated Bilateral Set Editor Modal ("külön ablak adat rögzítésre")
    editingBilateralSet?.let { setToEdit ->
        var editLeftText by remember(setToEdit) { mutableStateOf(setToEdit.leftSec.toString()) }
        var editRightText by remember(setToEdit) { mutableStateOf(setToEdit.rightSec.toString()) }
        var editRirText by remember(setToEdit) { mutableStateOf(if (setToEdit.actualRirProvided) setToEdit.rir.toString() else "") }

        com.example.ui.components.TrainPilotAlertDialog(
            onDismissRequest = { editingBilateralSet = null },
            containerColor = palette.surfaceElevated,
            title = {
                Text(
                    text = tr(
                        lang,
                        "${setToEdit.setNumber}. sorozat • Kétoldali adatrögzítés",
                        "Set #${setToEdit.setNumber} • Bilateral entry",
                        "${setToEdit.setNumber}. Satz • Beidseitige Eingabe",
                        "Seria #${setToEdit.setNumber} • Înregistrare bilaterală"
                    ),
                    color = palette.textPrimary,
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = tr(
                            lang,
                            "$exerciseTitle – állítsd be külön a Bal és Jobb oldal másodperceit:",
                            "$exerciseTitle – set Left and Right side seconds separately:",
                            "$exerciseTitle – Sekunden für linke und rechte Seite getrennt einstellen:",
                            "$exerciseTitle – setează separat secundele pentru partea stângă și dreaptă:"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        androidx.compose.material3.OutlinedTextField(
                            value = editLeftText,
                            onValueChange = { editLeftText = it.filter { ch -> ch.isDigit() } },
                            label = { Text(tr(lang, "Bal oldal (mp)", "Left side (s)", "Linke Seite (Sek.)", "Partea stângă (sec)")) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        androidx.compose.material3.OutlinedTextField(
                            value = editRightText,
                            onValueChange = { editRightText = it.filter { ch -> ch.isDigit() } },
                            label = { Text(tr(lang, "Jobb oldal (mp)", "Right side (s)", "Rechte Seite (Sek.)", "Partea dreaptă (sec)")) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val leftShort = tr(lang, "Bal", "Left", "Links", "Stg.")
                        val rightShort = tr(lang, "Jobb", "Right", "Rechts", "Dr.")
                        SmallAdjustChip("$leftShort -5$secWord") {
                            val v = (editLeftText.toIntOrNull() ?: 30) - 5
                            editLeftText = v.coerceAtLeast(1).toString()
                        }
                        SmallAdjustChip("$leftShort +5$secWord") {
                            val v = (editLeftText.toIntOrNull() ?: 30) + 5
                            editLeftText = v.coerceAtMost(600).toString()
                        }
                        SmallAdjustChip("$rightShort -5$secWord") {
                            val v = (editRightText.toIntOrNull() ?: 30) - 5
                            editRightText = v.coerceAtLeast(1).toString()
                        }
                        SmallAdjustChip("$rightShort +5$secWord") {
                            val v = (editRightText.toIntOrNull() ?: 30) + 5
                            editRightText = v.coerceAtMost(600).toString()
                        }
                    }
                    androidx.compose.material3.OutlinedTextField(
                        value = editRirText,
                        onValueChange = { editRirText = it.filter { ch -> ch.isDigit() } },
                        label = { Text(tr(lang, "RIR (Tartalék: 0–10)", "RIR (Reserve: 0–10)", "RIR (Reserve: 0–10)", "RIR (Rezervă: 0–10)")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (uiState.activeSets.size > 1) {
                        com.repforge.app.ui.theme.TrainPilotNegativeButton(
                            onClick = {
                                onDeleteSet(setToEdit.setNumber)
                                editingBilateralSet = null
                            }
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = androidx.compose.material3.LocalContentColor.current, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr(lang, "Sorozat törlése", "Delete set", "Satz löschen", "Șterge seria"), color = androidx.compose.material3.LocalContentColor.current, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                com.repforge.app.ui.theme.TrainPilotSaveButton(
                    onClick = {
                        val lSec = editLeftText.toIntOrNull() ?: setToEdit.leftSec
                        val rSec = editRightText.toIntOrNull() ?: setToEdit.rightSec
                        val rirVal = editRirText.toIntOrNull()
                        onUpdateBilateralSetExact(setToEdit.setNumber, lSec, rSec, rirVal)
                        editingBilateralSet = null
                    }) {
                    Text(tr(lang, "Mentés és rögzítés", "Save and record", "Speichern und erfassen", "Salvează și înregistrează"), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { editingBilateralSet = null }) {
                    Text(strings.cancelBtn, color = palette.textSecondary)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("active_bilateral_screen")
    ) {
        uiState.statusBannerMessage?.let { Text(it, color = palette.danger) }
        native.statusMessage?.let { Text(it, color = palette.danger) }
        if (target == null) Text(tr(uiState.language, "Nincs mérhető sorozat az aktuális natív gyakorlatban.",
            "No measurable set in the current native exercise.", "Kein messbarer Satz in der aktuellen nativen Übung.",
            "Nu există o serie măsurabilă în exercițiul nativ curent."), color = palette.textSecondary)

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = TrainPilotCardSpacing.ScreenGutter, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)
        ) {
            item {
                SubScreenTopBar(
                    title = "$exerciseTitle • ${tr(lang, "Kétoldali stopper", "Bilateral stopwatch", "Beidseitige Stoppuhr", "Cronometru bilateral")}",
                    onBack = onBack,
                    onOpenSettings = onOpenSettings,
                    rightExtraContent = { com.example.ui.components.StudioWorkoutHealthAction() }
                )
            }

            // Header Card + Exercise Switcher
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = palette.surfaceCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = exerciseTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = tr(
                                        lang,
                                        "2 külön stopper oldalanként • A „Leállítás és rögzítés” gomb a kiválasztott (${selectedTargetSetNumber}.) sorozatba menti az időt",
                                        "2 separate stopwatches per side • \"Stop & record\" saves the time into the selected (#${selectedTargetSetNumber}) set",
                                        "2 separate Stoppuhren pro Seite • „Stoppen & speichern“ sichert die Zeit im ausgewählten (${selectedTargetSetNumber}.) Satz",
                                        "2 cronometre separate pe parte • „Oprește și înregistrează” salvează timpul în seria selectată (#${selectedTargetSetNumber})"
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary
                                )
                            }
                            SmallExerciseVideoButton(
                                exerciseName = exerciseTitle,
                                exerciseId = currentExercise?.id ?: uiState.exercises.firstOrNull { it.localizedName(lang) == exerciseTitle || it.nameHu == exerciseTitle }?.id
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = palette.primary.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = tr(
                                        lang,
                                        "${uiState.activeExerciseIndex + 1} / ${workoutExercises.size.coerceAtLeast(1)} gyakorlat • Aktív cél: ${selectedTargetSetNumber}. sorozat",
                                        "${uiState.activeExerciseIndex + 1} / ${workoutExercises.size.coerceAtLeast(1)} exercises • Active target: Set #${selectedTargetSetNumber}",
                                        "${uiState.activeExerciseIndex + 1} / ${workoutExercises.size.coerceAtLeast(1)} Übungen • Aktives Ziel: ${selectedTargetSetNumber}. Satz",
                                        "${uiState.activeExerciseIndex + 1} / ${workoutExercises.size.coerceAtLeast(1)} exerciții • Țintă activă: Seria #${selectedTargetSetNumber}"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Box {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = palette.surfaceElevated,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { showExerciseDropdown = true }
                                        .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(8.dp))
                                ) {
                                    Text(
                                        text = tr(lang, "Gyakorlat váltása ▾", "Switch exercise ▾", "Übung wechseln ▾", "Schimbă exercițiul ▾"),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.textPrimary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = showExerciseDropdown,
                                    onDismissRequest = { showExerciseDropdown = false },
                                    modifier = Modifier
                                        .background(palette.surfaceElevated)
                                        .heightIn(max = 300.dp)
                                ) {
                                    workoutExercises.forEachIndexed { idx, ex ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "${idx + 1}. ${ex.localizedName(lang)}",
                                                    color = if (idx == uiState.activeExerciseIndex) palette.primary else palette.textPrimary,
                                                    fontWeight = if (idx == uiState.activeExerciseIndex) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                showExerciseDropdown = false
                                                onSwitchExercise(ex)
                                            }
                                        )
                                    }
                                    HorizontalDivider(color = palette.cardBorder)
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = tr(
                                                    lang,
                                                    "Gyakorlatkönyvtár megnyitása...",
                                                    "Open Exercise Library...",
                                                    "Übungsbibliothek öffnen...",
                                                    "Deschide biblioteca de exerciții..."
                                                ),
                                                color = palette.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        },
                                        onClick = {
                                            showExerciseDropdown = false
                                            onOpenExerciseLibrary()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 1. BAL OLDAL STOPPER (Left Side Stopwatch)
            item {
                val targetSetObj = uiState.activeSets.find { it.setNumber == selectedTargetSetNumber }
                val isLeftRecordedInTarget = targetSetObj?.leftRecorded == true || targetSetObj?.completed == true
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = palette.surfaceCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .trainPilotBorder(
                            width = if (leftRunning) 2.dp else 1.dp,
                            color = if (leftRunning) palette.primary else palette.cardBorder,
                            shape = RoundedCornerShape(20.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (leftRunning) palette.primary else palette.surfaceElevated,
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = tr(lang, "B", "L", "L", "S"),
                                            fontWeight = FontWeight.Bold,
                                            color = if (leftRunning) palette.onPrimary else palette.textPrimary,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = tr(lang, "Bal oldal stopper", "Left side stopwatch", "Stoppuhr linke Seite", "Cronometru partea stângă"),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.textPrimary
                                    )
                                    Text(
                                        text = when {
                                            leftRunning -> tr(
                                                lang,
                                                "Mérés folyamatban (${selectedTargetSetNumber}. sorozat - Bal) ●",
                                                "Measuring (Set #${selectedTargetSetNumber} - Left) ●",
                                                "Messung läuft (${selectedTargetSetNumber}. Satz - Links) ●",
                                                "Măsurare în curs (Seria #${selectedTargetSetNumber} - Stânga) ●"
                                            )
                                            isLeftRecordedInTarget -> tr(
                                                lang,
                                                "${selectedTargetSetNumber}. sorozat Bal oldal rögzítve (${targetSetObj?.leftSec ?: defaultTargetSec} mp) ✓",
                                                "Set #${selectedTargetSetNumber} Left side recorded (${targetSetObj?.leftSec ?: defaultTargetSec} s) ✓",
                                                "${selectedTargetSetNumber}. Satz Linke Seite erfasst (${targetSetObj?.leftSec ?: defaultTargetSec} Sek.) ✓",
                                                "Seria #${selectedTargetSetNumber} Partea stângă salvată (${targetSetObj?.leftSec ?: defaultTargetSec} sec) ✓"
                                            )
                                            else -> tr(
                                                lang,
                                                "Cél: ${selectedTargetSetNumber}. sorozat Bal oldal (${defaultTargetSec} mp)",
                                                "Target: Set #${selectedTargetSetNumber} Left side (${defaultTargetSec} s)",
                                                "Ziel: ${selectedTargetSetNumber}. Satz Linke Seite (${defaultTargetSec} Sek.)",
                                                "Țintă: Seria #${selectedTargetSetNumber} Partea stângă (${defaultTargetSec} sec)"
                                            )
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = when {
                                            leftRunning -> palette.primary
                                            isLeftRecordedInTarget -> palette.success
                                            else -> palette.textSecondary
                                        }
                                    )
                                }
                            }

                            // Digital display
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = palette.surfaceElevated,
                                modifier = Modifier.trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(10.dp))
                            ) {
                                StudioTimerReadout(
                                    seconds = leftSec,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (leftRunning) palette.primary else palette.textPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Left stopwatch controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { target?.let { if (leftRunning) nativeModel.pauseSetStopwatch(it) else nativeModel.startSetStopwatch(it, "left") } },
                            enabled = target != null,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (leftRunning) palette.warning else palette.success,
                                    contentColor = palette.onPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("bilateral_left_start_pause_btn")
                            ) {
                                Icon(
                                    imageVector = if (leftRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (leftRunning) tr(lang, "Szünet", "Pause", "Pause", "Pauză") else tr(lang, "Indítás", "Start", "Starten", "Pornește"), fontWeight = FontWeight.Bold)
                            }

                            // Leállítás és rögzítés button -> writes directly into selectedTargetSetNumber's Left slot!
                            com.repforge.app.ui.theme.TrainPilotSaveButton(
                                onClick = {
                                    target?.let { nativeModel.recordSetStopwatch(it, "left") }
                                },
                                enabled = target != null && native.stopwatchSetId == nativeSet?.setId && native.stopwatchSide == "left" && native.stopwatchSeconds > 0 && nativeSet?.isCompleted == false,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.45f)
                                    .height(44.dp)
                                    .testTag("bilateral_left_stop_record_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = tr(lang, "Leállítás és rögzítés", "Stop and record", "Stoppen & speichern", "Oprește și înregistrează"),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SmallAdjustChip(tr(lang, "Nulláz", "Reset", "Reset", "Reset")) {
                                target?.let(nativeModel::resetSetStopwatch)
                            }
                            SmallAdjustChip("+5 $secWord") { target?.let { nativeModel.adjustSetStopwatch(it, "left", 5) } }
                            SmallAdjustChip("+15 $secWord") { target?.let { nativeModel.adjustSetStopwatch(it, "left", 15) } }
                        }
                    }
                }
            }

            // 2. JOBB OLDAL STOPPER (Right Side Stopwatch)
            item {
                val targetSetObj = uiState.activeSets.find { it.setNumber == selectedTargetSetNumber }
                val isRightRecordedInTarget = targetSetObj?.rightRecorded == true || targetSetObj?.completed == true
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = palette.surfaceCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .trainPilotBorder(
                            width = if (rightRunning) 2.dp else 1.dp,
                            color = if (rightRunning) palette.primary else palette.cardBorder,
                            shape = RoundedCornerShape(20.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (rightRunning) palette.primary else palette.surfaceElevated,
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = tr(lang, "J", "R", "R", "D"),
                                            fontWeight = FontWeight.Bold,
                                            color = if (rightRunning) palette.onPrimary else palette.textPrimary,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = tr(lang, "Jobb oldal stopper", "Right side stopwatch", "Stoppuhr rechte Seite", "Cronometru partea dreaptă"),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.textPrimary
                                    )
                                    Text(
                                        text = when {
                                            rightRunning -> tr(
                                                lang,
                                                "Mérés folyamatban (${selectedTargetSetNumber}. sorozat - Jobb) ●",
                                                "Measuring (Set #${selectedTargetSetNumber} - Right) ●",
                                                "Messung läuft (${selectedTargetSetNumber}. Satz - Rechts) ●",
                                                "Măsurare în curs (Seria #${selectedTargetSetNumber} - Dreapta) ●"
                                            )
                                            isRightRecordedInTarget -> tr(
                                                lang,
                                                "${selectedTargetSetNumber}. sorozat Jobb oldal rögzítve (${targetSetObj?.rightSec ?: defaultTargetSec} mp) ✓",
                                                "Set #${selectedTargetSetNumber} Right side recorded (${targetSetObj?.rightSec ?: defaultTargetSec} s) ✓",
                                                "${selectedTargetSetNumber}. Satz Rechte Seite erfasst (${targetSetObj?.rightSec ?: defaultTargetSec} Sek.) ✓",
                                                "Seria #${selectedTargetSetNumber} Partea dreaptă salvată (${targetSetObj?.rightSec ?: defaultTargetSec} sec) ✓"
                                            )
                                            else -> tr(
                                                lang,
                                                "Cél: ${selectedTargetSetNumber}. sorozat Jobb oldal (${defaultTargetSec} mp)",
                                                "Target: Set #${selectedTargetSetNumber} Right side (${defaultTargetSec} s)",
                                                "Ziel: ${selectedTargetSetNumber}. Satz Rechte Seite (${defaultTargetSec} Sek.)",
                                                "Țintă: Seria #${selectedTargetSetNumber} Partea dreaptă (${defaultTargetSec} sec)"
                                            )
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = when {
                                            rightRunning -> palette.primary
                                            isRightRecordedInTarget -> palette.success
                                            else -> palette.textSecondary
                                        }
                                    )
                                }
                            }

                            // Digital display
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = palette.surfaceElevated,
                                modifier = Modifier.trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(10.dp))
                            ) {
                                StudioTimerReadout(
                                    seconds = rightSec,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (rightRunning) palette.primary else palette.textPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Right stopwatch controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { target?.let { if (rightRunning) nativeModel.pauseSetStopwatch(it) else nativeModel.startSetStopwatch(it, "right") } },
                            enabled = target != null,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (rightRunning) palette.warning else palette.success,
                                    contentColor = palette.onPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("bilateral_right_start_pause_btn")
                            ) {
                                Icon(
                                    imageVector = if (rightRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (rightRunning) tr(lang, "Szünet", "Pause", "Pause", "Pauză") else tr(lang, "Indítás", "Start", "Starten", "Pornește"), fontWeight = FontWeight.Bold)
                            }

                            // Leállítás és rögzítés button -> writes directly into selectedTargetSetNumber's Right slot!
                            com.repforge.app.ui.theme.TrainPilotSaveButton(
                                onClick = {
                                    target?.let { nativeModel.recordSetStopwatch(it, "right") }
                                },
                                enabled = target != null && native.stopwatchSetId == nativeSet?.setId && native.stopwatchSide == "right" && native.stopwatchSeconds > 0 && nativeSet?.isCompleted == false,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1.45f)
                                    .height(44.dp)
                                    .testTag("bilateral_right_stop_record_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = tr(lang, "Leállítás és rögzítés", "Stop and record", "Stoppen & speichern", "Oprește și înregistrează"),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SmallAdjustChip(tr(lang, "Nulláz", "Reset", "Reset", "Reset")) {
                                target?.let(nativeModel::resetSetStopwatch)
                            }
                            SmallAdjustChip("+5 $secWord") { target?.let { nativeModel.adjustSetStopwatch(it, "right", 5) } }
                            SmallAdjustChip("+15 $secWord") { target?.let { nativeModel.adjustSetStopwatch(it, "right", 15) } }
                        }
                    }
                }
            }

            // 3. SOROZATOK – KÉTOLDALI ADATRÖGZÍTŐ ABLAK (Sets table with separate Left & Right recording windows)
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = palette.surfaceCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tr(lang, "Sorozatok • Oldalankénti adatrögzítés", "Sets • Per-side recording", "Sätze • Seitliche Erfassung", "Serii • Înregistrare pe parte"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = tr(
                                        lang,
                                        "Koppints egy sorozatra a cél-kijelöléshez, vagy az oldal mezőre a kézi szerkesztéshez",
                                        "Tap a set to select target, or tap a side field for manual editing",
                                        "Tippe auf einen Satz für die Zielauswahl oder auf ein Seitenfeld zur manuellen Bearbeitung",
                                        "Atinge o serie pentru a o selecta ca țintă sau câmpul părții pentru editare manuală"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = palette.primary.copy(alpha = 0.16f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onAddSet() }
                            ) {
                                Text(
                                    text = tr(lang, "+ Sorozat", "+ Set", "+ Satz", "+ Serie"),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.primary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        uiState.activeSets.forEach { setItem ->
                            val isSelectedTarget = setItem.setNumber == selectedTargetSetNumber
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = when {
                                    setItem.completed -> palette.success.copy(alpha = 0.10f)
                                    isSelectedTarget -> palette.primary.copy(alpha = 0.10f)
                                    else -> palette.surfaceElevated
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { selectedTargetSetNumber = setItem.setNumber }
                                    .trainPilotBorder(
                                        width = if (isSelectedTarget) 1.5.dp else 1.dp,
                                        color = when {
                                            setItem.completed -> palette.success.copy(alpha = 0.5f)
                                            isSelectedTarget -> palette.primary
                                            else -> palette.cardBorder
                                        },
                                        shape = RoundedCornerShape(14.dp)
                                    )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Set number + Target indicator
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.width(44.dp)
                                    ) {
                                        Text(
                                            text = "${setItem.setNumber}.",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isSelectedTarget) palette.primary else palette.textPrimary
                                        )
                                        if (isSelectedTarget && !setItem.completed) {
                                            Text(
                                                text = tr(lang, "CÉL", "TARGET", "ZIEL", "ȚINTĂ"),
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                fontWeight = FontWeight.Bold,
                                                color = palette.primary
                                            )
                                        }
                                    }

                                    // BAL OLDAL RÖGZÍTŐ ABLAK
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (setItem.leftRecorded || setItem.completed) palette.success.copy(alpha = 0.16f) else palette.surfaceCard,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                selectedTargetSetNumber = setItem.setNumber
                                                editingBilateralSet = setItem
                                            }
                                            .trainPilotBorder(
                                                1.dp,
                                                if (setItem.leftRecorded || setItem.completed) palette.success else palette.cardBorder,
                                                RoundedCornerShape(10.dp)
                                            )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            val leftTitle = tr(lang, "Bal oldal", "Left side", "Linke Seite", "Partea stângă")
                                            Text(
                                                text = if (setItem.leftRecorded || setItem.completed) "$leftTitle ✓" else leftTitle,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = if (setItem.leftRecorded || setItem.completed) palette.success else palette.textSecondary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "${setItem.leftSec} $secWord",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = palette.textPrimary
                                            )
                                        }
                                    }

                                    // JOBB OLDAL RÖGZÍTŐ ABLAK
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (setItem.rightRecorded || setItem.completed) palette.success.copy(alpha = 0.16f) else palette.surfaceCard,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                selectedTargetSetNumber = setItem.setNumber
                                                editingBilateralSet = setItem
                                            }
                                            .trainPilotBorder(
                                                1.dp,
                                                if (setItem.rightRecorded || setItem.completed) palette.success else palette.cardBorder,
                                                RoundedCornerShape(10.dp)
                                            )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            val rightTitle = tr(lang, "Jobb oldal", "Right side", "Rechte Seite", "Partea dreaptă")
                                            Text(
                                                text = if (setItem.rightRecorded || setItem.completed) "$rightTitle ✓" else rightTitle,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = if (setItem.rightRecorded || setItem.completed) palette.success else palette.textSecondary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "${setItem.rightSec} $secWord",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = palette.textPrimary
                                            )
                                        }
                                    }

                                    // RIR badge (tap to edit)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = palette.surfaceCard,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { editingBilateralSet = setItem }
                                            .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(8.dp))
                                    ) {
                                        Text(
                                            text = if (setItem.actualRirProvided) "RIR ${setItem.rir}" else "RIR —",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.textSecondary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
                                        )
                                    }

                                    // Complete toggle button
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (setItem.completed) palette.success else palette.surfaceCard,
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { onToggleSetCompleted(setItem.setNumber) }
                                            .trainPilotBorder(
                                                1.dp,
                                                if (setItem.completed) palette.success else palette.cardBorder,
                                                RoundedCornerShape(10.dp)
                                            )
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = tr(lang, "Sorozat kész", "Set complete", "Satz fertig", "Serie completă"),
                                                tint = if (setItem.completed) palette.onPrimary else palette.textSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. EGYENSÚLY ÉS SZIMMETRIA ELEMZÉS (Side balance comparison based on active target set or live stopwatches)
            item {
                val targetSetObj = uiState.activeSets.find { it.setNumber == selectedTargetSetNumber }
                val compareLeft = if (leftSec > 0) leftSec else (targetSetObj?.leftSec ?: 0)
                val compareRight = if (rightSec > 0) rightSec else (targetSetObj?.rightSec ?: 0)
                val diff = kotlin.math.abs(compareLeft - compareRight)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = palette.surfaceCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(18.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tr(
                                    lang,
                                    "Oldalak egyensúlya (${selectedTargetSetNumber}. sorozat)",
                                    "Side balance (Set #${selectedTargetSetNumber})",
                                    "Seitenbalance (${selectedTargetSetNumber}. Satz)",
                                    "Echilibru lateral (Seria #${selectedTargetSetNumber})"
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (diff <= 3) palette.success.copy(alpha = 0.2f) else palette.primary.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = if (diff == 0) {
                                        tr(lang, "Különbség: 0 mp", "Difference: 0 s", "Differenz: 0 Sek.", "Diferență: 0 sec")
                                    } else {
                                        tr(lang, "Eltérés: $diff mp", "Difference: $diff s", "Abweichung: $diff Sek.", "Abatere: $diff sec")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (diff <= 3) palette.success else palette.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = when {
                                compareLeft == 0 && compareRight == 0 -> tr(
                                    lang,
                                    "Indítsd el a stoppereket a bal és jobb oldal méréséhez.",
                                    "Start the stopwatches to measure left and right sides.",
                                    "Starte die Stoppuhren, um die linke und rechte Seite zu messen.",
                                    "Pornește cronometrele pentru a măsura partea stângă și dreaptă."
                                )
                                diff == 0 -> tr(
                                    lang,
                                    "Tökéletes szimmetria a bal ($compareLeft mp) és jobb ($compareRight mp) oldal között!",
                                    "Perfect symmetry between left ($compareLeft s) and right ($compareRight s) sides!",
                                    "Perfekte Symmetrie zwischen linker ($compareLeft Sek.) und rechter ($compareRight Sek.) Seite!",
                                    "Simetrie perfectă între partea stângă ($compareLeft sec) și dreaptă ($compareRight sec)!"
                                )
                                diff <= 5 -> tr(
                                    lang,
                                    "Kiváló stabilitási egyensúly (Bal: $compareLeft mp • Jobb: $compareRight mp).",
                                    "Great stability balance (Left: $compareLeft s • Right: $compareRight s).",
                                    "Ausgezeichnetes Stabilitätsgleichgewicht (Links: $compareLeft Sek. • Rechts: $compareRight Sek.).",
                                    "Echilibru excelent de stabilitate (Stânga: $compareLeft sec • Dreapta: $compareRight sec)."
                                )
                                else -> tr(
                                    lang,
                                    "Figyelem: $diff másodperc különbség (Bal: $compareLeft mp • Jobb: $compareRight mp).",
                                    "Note: $diff seconds difference (Left: $compareLeft s • Right: $compareRight s).",
                                    "Hinweis: $diff Sekunden Unterschied (Links: $compareLeft Sek. • Rechts: $compareRight Sek.).",
                                    "Atenție: diferență de $diff secunde (Stânga: $compareLeft sec • Dreapta: $compareRight sec)."
                                )
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                }
            }
            item(key = "active_exercise_feedback") { exerciseFeedback() }
        }

        // Fixed Sticky Bottom Area: Rest Timer Dock + Előző / Következő Navigation
        Surface(
            color = palette.surfaceElevated,
            modifier = Modifier
                .fillMaxWidth()
                .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Sticky compact/expandable Rest Timer strip
                if (com.repforge.app.StudioRestTimer.visible(uiState)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = palette.surfaceCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .trainPilotBorder(
                            1.dp,
                            if (uiState.restTimerRunning) palette.primary else palette.cardBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { _, dragAmount ->
                                if (dragAmount > 12f) restTimerLargeMode = true
                                else if (dragAmount < -12f) restTimerLargeMode = false
                            }
                        }
                        .clickable { restTimerLargeMode = !restTimerLargeMode }
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                        com.example.ui.components.StudioCompactRestControls(
                            language = lang, remainingSec = uiState.restTimerRemainingSec,
                            running = uiState.restTimerRunning,
                            enabled = uiState.phoneControlsWorkout && !uiState.watchControlPending,
                            onAdjust = onAdjustRestTimer, onToggle = onToggleRestRunning,
                            onSkip = onSkipRest, onExpand = { restTimerLargeMode = !restTimerLargeMode },
                            showSkip = uiState.restTimerRunning || uiState.restTimerRemainingSec < uiState.defaultRestSec)
                        if (restTimerLargeMode) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = tr(
                                    lang,
                                    "Húzd felfelé a kompakt módhoz • Mindkét oldal rögzítése után a pihenő automatikusan elindul.",
                                    "Swipe up for compact mode • Rest timer starts automatically after both sides are recorded.",
                                    "Nach oben wischen für Kompaktmodus • Pause startet automatisch nach Erfassung beider Seiten.",
                                    "Glisează în sus pentru modul compact • Pauza pornește automat după înregistrarea ambelor părți."
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                        }
                    }
                }

                }

                val hasNextExercise = uiState.activeExerciseIndex + 1 < workoutExercises.size
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TrainPilotOutlinedButton(
                        onClick = onPreviousExercise,
                        colors = WorkoutControlAppearance.secondaryColors(),
                        border = WorkoutControlAppearance.secondaryBorder(),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("bilateral_prev_exercise_btn")
                    ) {
                        Text(tr(lang, "< Előző", "< Previous", "< Zurück", "< Anterior"), fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = onNextExercise,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.primary,
                            contentColor = palette.onPrimary
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp)
                            .testTag("bilateral_next_exercise_btn")
                    ) {
                        Text(
                            text = if (hasNextExercise) {
                                tr(
                                    lang,
                                    "Következő (${uiState.activeExerciseIndex + 2}/${workoutExercises.size}) >",
                                    "Next (${uiState.activeExerciseIndex + 2}/${workoutExercises.size}) >",
                                    "Weiter (${uiState.activeExerciseIndex + 2}/${workoutExercises.size}) >",
                                    "Următorul (${uiState.activeExerciseIndex + 2}/${workoutExercises.size}) >"
                                )
                            } else {
                                tr(lang, "Edzés befejezése ✓", "Finish workout ✓", "Training beenden ✓", "Finalizare antrenament ✓")
                            },
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * 4. KÜLTÉRI MÉRÉS (Mockup 2/4 #4 - Futás GPS - Valós DistanceTrackingService & LocationManager,
 * szimulált +2,75 m/mp és képletes pulzus NÉLKÜL)
 */
@OptIn(ExperimentalLayoutApi::class)
@SuppressLint("MissingPermission")
@Composable
fun ActiveOutdoorGpsScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectMeasurementMode: (SubScreen) -> Unit,
    onUpdateOutdoorMetrics: (String, Float, String) -> Unit = { _, _, _ -> },
    onSelectOutdoorActivity: (String) -> Unit = {},
    onStartGpsTracking: (String, String) -> Boolean = { _, _ -> false },
    onPauseMeasurement: (Boolean) -> Boolean = { false }
) {
    val palette = LocalTrainPilotPalette.current
    val context = LocalContext.current

    // Observe canonical DistanceTrackingService state flow
    val serviceState by com.repforge.app.DistanceTrackingService.gpsStateFlow.collectAsState()
    val initialSnap = remember { com.repforge.app.DistanceTrackingService.snapshot(context) }
    val candidateSnap = if (serviceState.length() > 0) serviceState else initialSnap
    val activeSnap = if (uiState.outdoorRouteKey.isNotBlank() && candidateSnap.optString("key") == uiState.outdoorRouteKey)
        candidateSnap else org.json.JSONObject()

    val screenScope = androidx.compose.runtime.rememberCoroutineScope()
    val nativeMeasurement: com.repforge.app.ui.workout.ActiveWorkoutViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    var startRequested by remember { mutableStateOf(false) }
    var gpsActive by remember { mutableStateOf(uiState.outdoorGpsEnabled) }
    var isStarted by remember { mutableStateOf(activeSnap.optBoolean("active", false)) }
    var isPaused by remember { mutableStateOf(activeSnap.optBoolean("paused", false)) }
    var elapsedSec by remember { mutableIntStateOf(activeSnap.optLong("elapsedSeconds", 0L).toInt()) }
    val initialFrame = remember(uiState.activeWorkoutId) { com.repforge.app.ActiveWorkoutStore.current(context) }
    val initialManualMeters = remember(uiState.activeWorkoutId) {
        com.repforge.app.StudioWorkoutBridge.readOutdoorManualDistanceMeters(initialFrame)?.toFloat()
    }
    var distanceMeters by remember { mutableFloatStateOf(activeSnap.optDouble("distanceMeters", 0.0).toFloat()) }
    var manualDistanceMetersState by remember(uiState.activeWorkoutId) { mutableStateOf<Float?>(initialManualMeters) }
    var manualDistanceInput by androidx.compose.runtime.saveable.rememberSaveable(uiState.activeWorkoutId) {
        mutableStateOf(
            initialManualMeters?.let { String.format(java.util.Locale.US, "%.2f", it / 1000f).trimEnd('0').trimEnd('.') } ?: ""
        )
    }
    var manualDistanceError by androidx.compose.runtime.saveable.rememberSaveable(uiState.activeWorkoutId) { mutableStateOf("") }
    var realGpsFixesCount by remember { mutableIntStateOf(activeSnap.optInt("routePointCount", 0)) }
    var hasServiceFix by remember { mutableStateOf(activeSnap.optBoolean("hasFix", false)) }
    var gpsAccuracyMeters by remember { mutableFloatStateOf(activeSnap.optDouble("accuracyMeters", 0.0).toFloat()) }
    var finishErrorMsg by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var serviceErrorMsg by remember { mutableStateOf(activeSnap.optString("error", "")) }
    var lastGpsLocation by remember { mutableStateOf<Location?>(null) }
    var lapCount by remember { mutableIntStateOf(1) }
    var lapSplits by remember { mutableStateOf(listOf<String>()) }
    var isFullscreenMapOpen by remember { mutableStateOf(false) }
    var routeOwnerKey by remember { mutableStateOf(
        uiState.outdoorRouteKey.takeIf(String::isNotBlank)
            ?: initialSnap.optString("key").takeIf { initialSnap.optBoolean("active") }
            ?: "outdoor_run_${java.util.UUID.randomUUID()}"
    ) }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    LaunchedEffect(uiState.activeWorkoutId) {
        nativeMeasurement.onEditorVisible()
        val currentFrame = com.repforge.app.ActiveWorkoutStore.current(context)
        val persistedManual = com.repforge.app.StudioWorkoutBridge.readOutdoorManualDistanceMeters(currentFrame)?.toFloat()
        if (persistedManual != null && persistedManual > 0f) {
            manualDistanceMetersState = persistedManual
            if (manualDistanceInput.isBlank()) {
                manualDistanceInput = String.format(java.util.Locale.US, "%.2f", persistedManual / 1000f).trimEnd('0').trimEnd('.')
            }
        }
    }

    // Sync from DistanceTrackingService whenever its state updates
    LaunchedEffect(serviceState) {
        if (gpsActive && serviceState.optString("key") == routeOwnerKey) {
            val binding = com.repforge.app.ActiveWorkoutStore.current(context)?.optJSONObject(com.repforge.app.NativeGpsMeasurement.FIELD)
            val baseMeters = binding?.optDouble("baseMeters", 0.0) ?: 0.0
            val baseSeconds = binding?.optLong("baseSeconds", 0) ?: 0
            val srvActive = serviceState.optBoolean("active", false)
            if (srvActive) {
                isStarted = true
                isPaused = serviceState.optBoolean("paused", false)
            }
            val srvDist = serviceState.optDouble("distanceMeters", Double.NaN)
            if (srvDist.isFinite() && srvDist >= 0.0) {
                distanceMeters = (baseMeters + srvDist).toFloat()
            }
            val srvElapsed = serviceState.optLong("elapsedSeconds", -1L)
            if (srvElapsed >= 0L && srvElapsed.toInt() > elapsedSec) {
                elapsedSec = (baseSeconds + srvElapsed).toInt()
            }
            realGpsFixesCount = serviceState.optInt("routePointCount", realGpsFixesCount)
            hasServiceFix = serviceState.optBoolean("hasFix", false)
            gpsAccuracyMeters = serviceState.optDouble("accuracyMeters", 0.0).toFloat()
            serviceErrorMsg = serviceState.optString("error", "")
        }
    }

    LaunchedEffect(uiState.activeWorkoutId, gpsActive) {
        if (!gpsActive) {
            val frame = com.repforge.app.PhoneWorkoutExecution.store(context).snapshot()
            if (frame?.optString("workoutId") == uiState.activeWorkoutId) {
                val clock = frame.optJSONObject(com.repforge.app.NativeSetStopwatch.FIELD)
                isStarted = clock != null
                isPaused = frame.optBoolean("workoutPaused") || clock?.let { !com.repforge.app.NativeSetStopwatch.running(it) } == true
                elapsedSec = clock?.let { (com.repforge.app.NativeSetStopwatch.elapsed(it) / 1000).toInt() } ?: 0
            }
        }
    }

    val lang = uiState.language
    val startNativeGpsTracking = {
        if (!startRequested && (!isStarted || isPaused)) {
            startRequested = true
            serviceErrorMsg = tr(
                lang,
                "GPS-szolgáltatás indul; visszaigazolásra vár…",
                "Starting GPS service; waiting for confirmation…",
                "GPS-Dienst startet; warte auf Bestätigung…",
                "Serviciul GPS pornește; se așteaptă confirmarea…"
            )
            screenScope.launch {
                var confirmed = false
                try {
                    val activityName = uiState.activeWorkoutTitleHu.ifBlank { "Kültéri futás" }
                    check(onStartGpsTracking(activityName, routeOwnerKey)) {
                        tr(lang, "A natív munkamenet nem készíthető elő.", "Native session could not be prepared.", "Native Sitzung konnte nicht vorbereitet werden.", "Sesiunea nativă nu a putut fi pregătită.")
                    }
                    nativeMeasurement.onEditorVisible()
                    val frame = checkNotNull(com.repforge.app.PhoneWorkoutExecution.store(context).snapshot())
                    check(kotlinx.coroutines.withTimeoutOrNull(5_000) {
                        while (!com.repforge.app.IntervalTimerService.armed(frame.getString("workoutId"), frame.getLong("generation"))) delay(100)
                        true
                    } == true) {
                        tr(lang, "A natív háttérvezérlő nem igazolta vissza az indítást.", "Native background controller did not confirm startup.", "Nativer Hintergrunddienst hat den Start nicht bestätigt.", "Controlerul nativ de fundal nu a confirmat pornirea.")
                    }
                    val exercise = frame.getJSONArray("exercises").getJSONObject(frame.optInt("currentExercise"))
                    val sets = exercise.getJSONArray("sets")
                    val set = (0 until sets.length()).map { sets.getJSONObject(it) }.first { !it.optBoolean("done") }
                    val target = com.repforge.app.NativeWorkoutTarget(frame.getString("workoutId"), frame.getLong("generation"), exercise.getString("id"), set.getString("setId"))
                    val prepared = com.repforge.app.NativeWorkoutEditing.startGps(context, target)
                    val binding = prepared.getJSONObject(com.repforge.app.NativeGpsMeasurement.FIELD)
                    routeOwnerKey = binding.getString("key")
                    val receipt = kotlinx.coroutines.withTimeoutOrNull(10_000) {
                        while (true) {
                            val snap = com.repforge.app.DistanceTrackingService.snapshot(context)
                            if (snap.optString("key") == routeOwnerKey && snap.optBoolean("active")) break
                            delay(100)
                        }
                        true
                    } == true
                    check(receipt) {
                        tr(lang, "A GPS-szolgáltatás nem igazolta vissza az indítást.", "GPS service did not confirm startup.", "GPS-Dienst hat den Start nicht bestätigt.", "Serviciul GPS nu a confirmat pornirea.")
                    }
                    check(onStartGpsTracking(activityName, routeOwnerKey)) {
                        tr(lang, "A GPS-mérés natív edzéshez kötése nem sikerült.", "Binding GPS tracking to native workout failed.", "Verknüpfung der GPS-Messung mit dem nativen Training fehlgeschlagen.", "Asocierea măsurătorii GPS cu antrenamentul nativ a eșuat.")
                    }
                    confirmed = true
                    isStarted = true
                    isPaused = false
                    serviceErrorMsg = ""
                } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (error: Exception) {
                    serviceErrorMsg = error.message ?: tr(lang, "A GPS-mérés nem indult el.", "GPS tracking did not start.", "GPS-Messung wurde nicht gestartet.", "Măsurătoarea GPS nu a pornit.")
                }
                finally {
                    startRequested = false
                    if (!confirmed) {
                        isStarted = false
                        runCatching { com.repforge.app.NativeWorkoutEditing.interruptGps(context) }
                    }
                }
            }
        }
    }

    val startIndoorMeasurement = {
        try {
            check(onStartGpsTracking(uiState.activeWorkoutTitleHu.ifBlank { "Kültéri futás" }, "")) {
                tr(lang, "A beltéri munkamenet nem menthető.", "Indoor session could not be saved.", "Indoor-Sitzung konnte nicht gespeichert werden.", "Sesiunea indoor nu a putut fi salvată.")
            }
            nativeMeasurement.onEditorVisible()
            val frame = com.repforge.app.StudioWorkoutBridge.startIndoor(context)
            manualDistanceMetersState?.let { manualM ->
                if (manualM > 0f) {
                    com.repforge.app.StudioWorkoutBridge.setOutdoorManualDistance(context, manualM.toDouble(), markDoneIfPositive = false)
                }
            }
            isStarted = frame.optJSONObject(com.repforge.app.NativeSetStopwatch.FIELD)?.let {
                com.repforge.app.NativeSetStopwatch.running(it)
            } == true
            isPaused = !isStarted
            serviceErrorMsg = ""
        } catch (error: Exception) {
            serviceErrorMsg = error.message ?: tr(lang, "A beltéri időmérés nem indult el.", "Indoor timer did not start.", "Indoor-Zeitmessung wurde nicht gestartet.", "Cronometrul indoor nu a pornit.")
        }
    }

    val pauseMeasurement = {
        val paused = !isPaused
        if (onPauseMeasurement(paused)) {
            if (gpsActive && !paused) startNativeGpsTracking()
            else {
                if (!gpsActive && !paused) com.repforge.app.StudioWorkoutBridge.startIndoor(context)
                isPaused = paused
            }
        }
    }

    val applyManualDistanceInput = { markDone: Boolean ->
        val validMeters = com.repforge.app.StudioWorkoutBridge.validateManualDistanceKm(manualDistanceInput)
        if (validMeters == null) {
            manualDistanceError = tr(
                lang,
                "Adj meg érvényes, pozitív távolságot (0,01 – 2000 km).",
                "Enter a valid positive distance (0.01 – 2000 km).",
                "Gib eine gültige positive Distanz ein (0,01 – 2000 km).",
                "Introdu o distanță pozitivă validă (0,01 – 2000 km)."
            )
            false
        } else {
            val activityName = uiState.activeWorkoutTitleHu.ifBlank { "Kültéri futás" }
            if (com.repforge.app.ActiveWorkoutStore.current(context) == null) {
                onStartGpsTracking(activityName, if (gpsActive) routeOwnerKey else "")
            }
            val updated = com.repforge.app.StudioWorkoutBridge.setOutdoorManualDistance(
                context,
                validMeters,
                markDoneIfPositive = markDone
            )
            if (updated == null) {
                manualDistanceError = tr(
                    lang,
                    "A távolság nem menthető ebben az edzésállapotban.",
                    "Distance could not be saved in this workout state.",
                    "Distanz konnte in diesem Trainingszustand nicht gespeichert werden.",
                    "Distanța nu a putut fi salvată în această stare."
                )
                false
            } else {
                manualDistanceMetersState = validMeters.toFloat()
                manualDistanceError = ""
                val min = elapsedSec / 60
                val sec = elapsedSec % 60
                val formattedTime = String.format("%02d:%02d", min, sec)
                val distKm = (validMeters / 1000.0).toFloat()
                val formattedPace = if (distKm >= 0.01f && elapsedSec > 0) {
                    val paceSecPerKm = (elapsedSec / distKm).toInt()
                    String.format("%d:%02d", (paceSecPerKm / 60).coerceIn(1, 59), (paceSecPerKm % 60).coerceIn(0, 59))
                } else {
                    "—"
                }
                onUpdateOutdoorMetrics(formattedTime, distKm, formattedPace)
                true
            }
        }
    }

    val clearManualDistance = {
        com.repforge.app.StudioWorkoutBridge.setOutdoorManualDistance(context, null, markDoneIfPositive = false)
        manualDistanceMetersState = null
        manualDistanceInput = ""
        manualDistanceError = ""
        val min = elapsedSec / 60
        val sec = elapsedSec % 60
        val formattedTime = String.format("%02d:%02d", min, sec)
        val distKm = distanceMeters / 1000f
        val formattedPace = if (distKm >= 0.01f && elapsedSec > 0) {
            val paceSecPerKm = (elapsedSec / distKm).toInt()
            String.format("%d:%02d", (paceSecPerKm / 60).coerceIn(1, 59), (paceSecPerKm % 60).coerceIn(0, 59))
        } else {
            "—"
        }
        onUpdateOutdoorMetrics(formattedTime, distKm, formattedPace)
    }

    val finishMeasurement = finish@{
        finishErrorMsg = ""
        if (manualDistanceInput.isNotBlank() && manualDistanceMetersState == null) {
            if (!applyManualDistanceInput(false)) return@finish false
        }
        var recorded = false
        try {
            nativeMeasurement.onEditorVisible()
            if (com.repforge.app.ActiveWorkoutStore.current(context) == null) {
                check(onStartGpsTracking(uiState.activeWorkoutTitleHu.ifBlank { "Kültéri futás" }, if (gpsActive) routeOwnerKey else ""))
            }
            var frame = com.repforge.app.NativeWorkoutEditing.project(context)
            if (!gpsActive && frame.optBoolean("workoutPaused")) {
                check(onPauseMeasurement(false))
                frame = com.repforge.app.NativeWorkoutEditing.project(context)
            }
            val saved = if (gpsActive && frame.optJSONObject(com.repforge.app.NativeGpsMeasurement.FIELD) != null) {
                val binding = frame.getJSONObject(com.repforge.app.NativeGpsMeasurement.FIELD)
                if (binding.optBoolean("applied")) frame
                else com.repforge.app.NativeWorkoutEditing.finishGps(context,
                    com.repforge.app.NativeGpsMeasurement.target(frame), binding.getString("id"))
            } else if (!gpsActive && (isStarted || frame.optJSONObject(com.repforge.app.NativeSetStopwatch.FIELD) != null)) {
                val clock = frame.optJSONObject(com.repforge.app.NativeSetStopwatch.FIELD)
                if (clock == null || !com.repforge.app.NativeSetStopwatch.running(clock)) {
                    frame = com.repforge.app.StudioWorkoutBridge.startIndoor(context)
                }
                val current = frame.getJSONObject(com.repforge.app.NativeSetStopwatch.FIELD)
                com.repforge.app.NativeWorkoutEditing.stopStopwatch(context, com.repforge.app.NativeWorkoutTarget(
                    frame.getString("workoutId"), frame.getLong("generation"), current.getString("exerciseId"), current.getString("setId")))
            } else {
                frame
            }
            val manualToCommit = manualDistanceMetersState?.takeIf { it > 0f }?.toDouble()
                ?: com.repforge.app.StudioWorkoutBridge.readOutdoorManualDistanceMeters(saved)
            val afterManual = if (manualToCommit != null && manualToCommit > 0.0) {
                com.repforge.app.StudioWorkoutBridge.setOutdoorManualDistance(context, manualToCommit, markDoneIfPositive = true) ?: saved
            } else {
                saved
            }
            val exercises = afterManual.getJSONArray("exercises")
            recorded = (0 until exercises.length()).any { i ->
                val sets = exercises.getJSONObject(i).getJSONArray("sets")
                (0 until sets.length()).any { sets.getJSONObject(it).optBoolean("done") }
            }
            if (gpsActive && !recorded) {
                com.repforge.app.StudioOutdoorFinish.receipt(afterManual)?.let { receipt ->
                    afterManual.put(com.repforge.app.StudioOutdoorFinish.FIELD, receipt)
                    com.repforge.app.NativeWorkoutEditing.commitEdit(context, afterManual, gpsEdit = true)
                }
            }
            val finalDistMeters = (manualToCommit ?: distanceMeters.toDouble()).toFloat()
            val finalDistKm = finalDistMeters / 1000f
            val finalTimeLabel = String.format("%02d:%02d", elapsedSec / 60, elapsedSec % 60)
            val finalPaceLabel = if (finalDistKm >= 0.01f && elapsedSec > 0) {
                val paceSecPerKm = (elapsedSec / finalDistKm).toInt()
                String.format("%d:%02d", (paceSecPerKm / 60).coerceIn(1, 59), (paceSecPerKm % 60).coerceIn(0, 59))
            } else "—"
            onUpdateOutdoorMetrics(finalTimeLabel, finalDistKm, finalPaceLabel)
            // Stopping and opening Summary never requires inventing a completed set.
            // Save still requires a completed native set or a verified positive-time receipt.
            recorded = true
            isPaused = true
        } catch (error: Exception) {
            finishErrorMsg = error.message ?: tr(lang, "A mérés nem rögzíthető.", "Measurement could not be recorded.", "Messung konnte nicht erfasst werden.", "Măsurătoarea nu a putut fi înregistrată.")
        }
        recorded
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val grantedFine = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
        hasLocationPermission = grantedFine
        if (grantedFine && gpsActive) {
            startNativeGpsTracking()
        } else if (!grantedFine && gpsActive) {
            serviceErrorMsg = tr(
                lang,
                "A GPS-méréshez pontos helymeghatározási engedély szükséges. Engedély nélkül 0 m kerül rögzítésre.",
                "Precise location permission is required for GPS tracking. Without permission, 0 m will be recorded.",
                "Für die GPS-Messung ist die genaue Standortberechtigung erforderlich. Ohne Berechtigung werden 0 m erfasst.",
                "Pentru măsurătoarea GPS este necesară permisiunea de locație precisă. Fără permisiune se vor înregistra 0 m."
            )
        }
    }

    // Live 1-second stopwatch loop — STRICTLY NO FAKE DISTANCE OR FAKE HEART RATE
    LaunchedEffect(isStarted, isPaused) {
        while (isStarted && !isPaused) {
            delay(1000L)
            val frame = com.repforge.app.PhoneWorkoutExecution.store(context).snapshot()
            if (!gpsActive && frame != null) {
                com.repforge.app.PhoneWorkoutExecution.store(context).checkpointStopwatch(
                    frame.optString("workoutId"), frame.optLong("generation"), android.os.SystemClock.elapsedRealtime())
                val current = com.repforge.app.PhoneWorkoutExecution.store(context).snapshot()
                val clock = current?.optJSONObject(com.repforge.app.NativeSetStopwatch.FIELD)
                elapsedSec = clock?.let { (com.repforge.app.NativeSetStopwatch.elapsed(it) / 1000).toInt() } ?: 0
                isPaused = current?.optBoolean("workoutPaused") == true
            }
            val snap = com.repforge.app.DistanceTrackingService.snapshot(context)
            if (gpsActive && snap.optString("key") == routeOwnerKey && snap.optBoolean("active", false)) {
                val binding = com.repforge.app.ActiveWorkoutStore.current(context)?.optJSONObject(com.repforge.app.NativeGpsMeasurement.FIELD)
                elapsedSec = ((binding?.optLong("baseSeconds", 0) ?: 0) + snap.optLong("elapsedSeconds", 0)).toInt()
                val srvMeters = ((binding?.optDouble("baseMeters", 0.0) ?: 0.0) + snap.optDouble("distanceMeters", 0.0)).toFloat()
                if (srvMeters > distanceMeters) {
                    distanceMeters = srvMeters
                }
                realGpsFixesCount = maxOf(realGpsFixesCount, snap.optInt("routePointCount", 0))
                hasServiceFix = snap.optBoolean("hasFix", hasServiceFix)
            }
            val min = elapsedSec / 60
            val sec = elapsedSec % 60
            val formattedTime = String.format("%02d:%02d", min, sec)
            val effectiveMeters = manualDistanceMetersState ?: distanceMeters
            val distKm = effectiveMeters / 1000f
            val formattedPace = if (distKm >= 0.01f && elapsedSec > 0) {
                val paceSecPerKm = (elapsedSec / distKm).toInt()
                String.format("%d:%02d", (paceSecPerKm / 60).coerceIn(1, 59), (paceSecPerKm % 60).coerceIn(0, 59))
            } else {
                "—"
            }
            onUpdateOutdoorMetrics(formattedTime, distKm, formattedPace)
        }
    }

    val formattedElapsed = String.format("%02d:%02d", elapsedSec / 60, elapsedSec % 60)
    val effectiveDistanceMeters = manualDistanceMetersState ?: distanceMeters
    val distanceKm = effectiveDistanceMeters / 1000f
    val formattedDistance = String.format(com.example.localization.StudioLocale.forLanguage(lang), "%.2f km", distanceKm)
    val formattedPace = if (distanceKm >= 0.01f && elapsedSec > 0) {
        val paceSecPerKm = (elapsedSec / distanceKm).toInt()
        String.format("%d:%02d /km", (paceSecPerKm / 60).coerceIn(1, 59), (paceSecPerKm % 60).coerceIn(0, 59))
    } else {
        "— /km"
    }

    // Real sensor HR only (from WearLiveHealthStore or Health sync) — never fabricated
    val wearLiveHr = remember(elapsedSec) {
        com.repforge.app.WearLiveHealthStore.current(context)?.optInt("heartRate", 0) ?: 0
    }
    val realHrBpm = if (wearLiveHr > 0) wearLiveHr else uiState.currentHrBpm
    val hrLabel = if (realHrBpm > 0) "♥ $realHrBpm bpm" else "♥ — bpm (nincs szenzor)"
    val caloriesLabel = "— kcal (edzésadat nem érhető el)"

    val display = OutdoorGpsDisplay(
        title = uiState.activeWorkoutTitleHu.ifBlank { "Kültéri futás" },
        status = when {
            !gpsActive -> tr(lang, "Beltéri időmérés • GPS kikapcsolva", "Indoor timer • GPS disabled", "Indoor-Zeitmessung • GPS deaktiviert", "Cronometru indoor • GPS dezactivat")
            !hasLocationPermission -> tr(lang, "Helymeghatározási engedély szükséges", "Location permission required", "Standortberechtigung erforderlich", "Permisiune de locație necesară")
            isStarted && isPaused -> tr(lang, "GPS szüneteltetve", "GPS paused", "GPS pausiert", "GPS în pauză")
            isStarted && hasServiceFix -> tr(
                lang,
                "Élő GPS-jel ($realGpsFixesCount pont • ±${gpsAccuracyMeters.toInt()} m)",
                "Live GPS signal ($realGpsFixesCount pts • ±${gpsAccuracyMeters.toInt()} m)",
                "Live-GPS-Signal ($realGpsFixesCount Pkt. • ±${gpsAccuracyMeters.toInt()} m)",
                "Semnal GPS live ($realGpsFixesCount pct. • ±${gpsAccuracyMeters.toInt()} m)"
            )
            isStarted -> tr(lang, "GPS-jelre vár…", "Waiting for GPS signal…", "Warte auf GPS-Signal…", "Se așteaptă semnalul GPS…")
            else -> tr(lang, "Indításra kész", "Ready to start", "Bereit zum Starten", "Pregătit de start")
        },
        gpsEnabled = gpsActive, started = isStarted, paused = isPaused,
        elapsed = formattedElapsed, distance = formattedDistance, pace = formattedPace,
        calories = "— kcal", heartRate = if (realHrBpm > 0) "♥ $realHrBpm bpm" else "♥ — bpm", pointCount = realGpsFixesCount,
        lapCount = lapCount, latestLap = lapSplits.lastOrNull().orEmpty(),
        error = listOf(finishErrorMsg, serviceErrorMsg).filter(String::isNotBlank).distinct().joinToString("\n"),
        language = uiState.language,
        manualDistanceInput = manualDistanceInput,
        manualDistanceError = manualDistanceError,
        hasManualDistance = manualDistanceMetersState != null && manualDistanceMetersState!! > 0f
    )
    val actions = OutdoorGpsActions(
        back = onBack, settings = onOpenSettings, selectActivity = onSelectOutdoorActivity,
        setGpsEnabled = { enabled ->
            gpsActive = enabled
            if (!enabled && isStarted) com.repforge.app.DistanceTrackingService.stop(context)
        },
        start = {
            serviceErrorMsg = ""
            if (gpsActive) {
                hasLocationPermission = ContextCompat.checkSelfPermission(context,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                if (!hasLocationPermission) permissionLauncher.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                else startNativeGpsTracking()
            } else startIndoorMeasurement()
        },
        pause = { pauseMeasurement() },
        lap = {
            val lapWord = tr(lang, "kör", "lap", "Runde", "tură")
            lapSplits = lapSplits + "${lapCount}. $lapWord: $formattedElapsed ($formattedDistance)"
            lapCount += 1
        },
        finish = {
            if (finishMeasurement()) {
                isFullscreenMapOpen = false
                onSelectMeasurementMode(SubScreen.WORKOUT_SUMMARY)
            }
        },
        expandMap = { isFullscreenMapOpen = true },
        onManualDistanceInputChange = { next ->
            manualDistanceInput = next
            if (manualDistanceError.isNotBlank()) manualDistanceError = ""
        },
        onSaveManualDistance = { applyManualDistanceInput(false) },
        onClearManualDistance = { clearManualDistance() }
    )
    val routeMap: @Composable (Modifier) -> Unit = { modifier ->
        val mapPadding = LocalOutdoorMapContentPadding.current
        NativeWorkoutRouteMapBox(modifier = modifier, interactiveControls = true,
            routeKeys = listOf(routeOwnerKey), refreshToken = realGpsFixesCount,
            showZoomControls = mapPadding.calculateTopPadding() > 0.dp,
            mapContentPadding = mapPadding)
    }
    OutdoorGpsDashboard(display, strings, actions, mapContent = routeMap)
    if (isFullscreenMapOpen) {
        OutdoorGpsFullscreenDialog(
            state = display,
            strings = strings,
            actions = actions,
            onDismissRequest = { isFullscreenMapOpen = false },
            mapContent = routeMap
        )
    }
}

/**
 * 5. INTERVALLUMOK BEÁLLÍTÁSA (Mockup 2/4 #5)
 */
@Composable
fun IntervalSettingsScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onSelectMeasurementMode: (SubScreen) -> Unit,
    onSaveAndStartIntervals: (Int, Int, Int, Boolean, Boolean, Boolean) -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    var workSec by remember { mutableIntStateOf(uiState.intervalWorkSec) }
    var restSec by remember { mutableIntStateOf(uiState.intervalRestSec) }
    var rounds by remember { mutableIntStateOf(uiState.intervalRounds) }
    var sound by remember { mutableStateOf(uiState.soundAlertEnabled) }
    var vibration by remember { mutableStateOf(uiState.vibrationEnabled) }
    var autoStep by remember { mutableStateOf(uiState.autoStepIntervals) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("interval_settings_screen"),
        contentPadding = PaddingValues(horizontal = TrainPilotCardSpacing.ScreenGutter, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)
    ) {
        val lang = uiState.language
        val secWord = tr(lang, "mp", "s", "Sek.", "sec")
        item {
            SubScreenTopBar(
                title = tr(lang, "Intervallumok beállítása", "Interval Settings", "Intervalleinstellungen", "Setări intervale"),
                onBack = onBack,
                onOpenSettings = onOpenSettings,
                    rightExtraContent = { com.example.ui.components.StudioWorkoutHealthAction() }
            )
        }
        item {
            IntervalStepperCard(
                label = tr(lang, "Munkaidő", "Work time", "Arbeitszeit", "Timp de lucru"),
                valueText = "$workSec $secWord",
                onMinus = { workSec = (workSec - 5).coerceAtLeast(10) },
                onPlus = { workSec = (workSec + 5).coerceAtMost(300) }
            )
        }
        item {
            IntervalStepperCard(
                label = tr(lang, "Pihenőidő", "Rest time", "Pausenzeit", "Timp de pauză"),
                valueText = "$restSec $secWord",
                onMinus = { restSec = (restSec - 5).coerceAtLeast(10) },
                onPlus = { restSec = (restSec + 5).coerceAtMost(300) }
            )
        }
        item {
            IntervalStepperCard(
                label = tr(lang, "Ismétlések (körök)", "Repetitions (rounds)", "Wiederholungen (Runden)", "Repetări (runde)"),
                valueText = tr(lang, "$rounds kör", "$rounds rounds", "$rounds Runden", "$rounds runde"),
                onMinus = { rounds = (rounds - 1).coerceAtLeast(1) },
                onPlus = { rounds = (rounds + 1).coerceAtMost(20) }
            )
        }
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tr(lang, "Hangjelzés", "Sound alert", "Tonhinweis", "Alertă sonoră"), color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                        Switch(checked = sound, onCheckedChange = { sound = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tr(lang, "Rezgés", "Vibration", "Vibration", "Vibrație"), color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                        Switch(checked = vibration, onCheckedChange = { vibration = it })
                    }
                }
            }
        }
        item {
            com.repforge.app.ui.theme.TrainPilotSaveButton(
                onClick = {
                    onSaveAndStartIntervals(workSec, restSec, rounds, sound, vibration, autoStep)
                    onSelectMeasurementMode(SubScreen.ACTIVE_TIMED)
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.startIntervalsAction, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun IntervalStepperCard(
    label: String,
    valueText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val strings = com.example.localization.LocalTrainPilotStrings.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = palette.surfaceCard,
        modifier = Modifier
            .fillMaxWidth()
            .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(18.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(label, style = MaterialTheme.typography.labelMedium, color = palette.textSecondary)
                Text(valueText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = palette.textPrimary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onMinus,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.surfaceElevated)
                ) {
                    Icon(imageVector = Icons.Default.Remove, contentDescription = strings.decreaseValueDesc, tint = palette.textPrimary)
                }
                IconButton(
                    onClick = onPlus,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.primary)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = strings.increaseValueDesc, tint = palette.onPrimary)
                }
            }
        }
    }
}

data class SummaryAttachedPhoto(
    val id: String,
    val label: String,
    val width: Int,
    val height: Int,
    val bytes: Long
)

/**
 * 6. EDZÉS BEFEJEZÉSE / ÖSSZEGZÉSE (Mockup 2/4 #6)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkoutSummaryScreen(
    uiState: TrainPilotUiState,
    strings: LocalizedStrings,
    onBack: () -> Unit,
    onSelectMood: (Int) -> Unit,
    onUpdateNote: (String) -> Unit,
    onSaveWorkout: () -> Unit,
    onPauseAndExit: () -> Unit,
    onDiscardWorkout: () -> Unit,
    onExerciseEffort: (String, Int) -> Unit = { _, _ -> },
    onExerciseNote: (String, String) -> Unit = { _, _ -> }
) {
    val palette = LocalTrainPilotPalette.current
    val lang = uiState.language
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val photoRepo = remember(context) { WorkoutPhotoRepository(context) }
    var selectedPhotoCategory by remember { mutableStateOf("after") }
    var exerciseFeedbackExpanded by androidx.compose.runtime.saveable.rememberSaveable(uiState.activeWorkoutId) { mutableStateOf(false) }

    val attachedPhotos = remember(uiState.activeWorkoutId) {
        val list = mutableStateListOf<SummaryAttachedPhoto>()
        val current = ActiveWorkoutStore.current(context)
        val arr = current?.optJSONArray("photos")
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("id")
                if (id.isNotBlank()) {
                    list.add(
                        SummaryAttachedPhoto(
                            id = id,
                            label = obj.optString("label", "after"),
                            width = obj.optInt("width", 0),
                            height = obj.optInt("height", 0),
                            bytes = obj.optLong("bytes", 0L)
                        )
                    )
                }
            }
        }
        list
    }

    var photoError by remember { mutableStateOf(false) }
    var photoBusy by remember { mutableStateOf(false) }
    var pendingPhoto by remember { mutableStateOf<com.repforge.app.NativeWorkoutPhotos.Request?>(null) }

    fun showSavedPhotos(photos: JSONArray) {
        attachedPhotos.clear()
        for (i in 0 until photos.length()) {
            val p = photos.getJSONObject(i)
            attachedPhotos.add(SummaryAttachedPhoto(p.getString("id"), p.optString("label", "after"),
                p.optInt("width"), p.optInt("height"), p.optLong("bytes")))
        }
    }

    fun receivePhoto(importPhoto: suspend () -> Result<com.repforge.app.StoredPhotoMetadata?>) {
        val request = pendingPhoto ?: return
        pendingPhoto = null
        scope.launch {
            try {
                val photo = importPhoto().getOrThrow() ?: return@launch
                showSavedPhotos(com.repforge.app.NativeWorkoutPhotos.attach(context, request, photo))
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                photoError = true
            } finally {
                photoBusy = false
            }
        }
    }

    val summaryCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { res -> receivePhoto { photoRepo.handleCameraResult(res.resultCode, res.data) } }

    val summaryGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { res -> receivePhoto {
        if (res.resultCode == android.app.Activity.RESULT_OK) photoRepo.importPickedUri(res.data?.data)
        else Result.success(null)
    } }


    val summaryListState = androidx.compose.foundation.lazy.rememberLazyListState()
    remember(context) { ActiveWorkoutStore.current(context); com.repforge.app.WearLiveHealthStore.current(context) }
    val summaryFrame by ActiveWorkoutStore.snapshotFlow.collectAsState()
    val summaryLiveHealth by com.repforge.app.WearLiveHealthStore.liveHealthFlow.collectAsState()
    val activeSnapshotForSummary = com.repforge.app.StudioWorkoutSummaryPresentation.current(
        summaryFrame, uiState.activeWorkoutId, uiState.activeWorkoutGeneration)
    val summaryNow = java.time.Instant.now()
    val persistedOutdoorDistKm = remember(uiState.activeWorkoutId, uiState.outdoorDistanceKm) {
        (com.repforge.app.StudioWorkoutBridge.readOutdoorEffectiveDistanceMeters(
            activeSnapshotForSummary,
            (uiState.outdoorDistanceKm * 1000f).toDouble()
        ) / 1000.0).toFloat()
    }
    val isOutdoor = uiState.activeWorkoutSubScreen == SubScreen.ACTIVE_OUTDOOR_GPS || persistedOutdoorDistKm > 0f || uiState.outdoorDistanceKm > 0f
    val effectiveOutdoorDistanceKm = maxOf(uiState.outdoorDistanceKm, persistedOutdoorDistKm)

    com.example.ui.components.TrainPilotScrollableListBox(
        lazyListState = summaryListState,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = summaryListState,
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background)
                .testTag("workout_summary_screen"),
            contentPadding = PaddingValues(horizontal = TrainPilotCardSpacing.ScreenGutter, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)
        ) {

        val summarySubtitle = if (isOutdoor) {
            val distLabel = if (effectiveOutdoorDistanceKm > 0f) {
                String.format(com.example.localization.StudioLocale.forLanguage(lang), "%.2f km", effectiveOutdoorDistanceKm)
            } else {
                uiState.outdoorElapsedLabel.ifBlank { "00:00" }
            }
            val titleLabel = com.example.model.LoggedWorkoutEntry(
                id = "",
                titleHu = uiState.activeWorkoutTitleHu.ifBlank { "Kültéri futás" }
            ).localizedTitle(lang, uiState.programs, uiState.exercises)
            "$titleLabel • $distLabel"
        } else {
            val completedExsCount = uiState.activeWorkoutExercises.count { ex ->
                (uiState.activeSetsByExerciseId[ex.id] ?: emptyList()).any { it.completed }
            }
            val totalExsCount = uiState.activeWorkoutExercises.size.coerceAtLeast(1)
            val displayTitle = uiState.activeWorkoutTitleHu.takeIf { it.isNotBlank() }?.let { raw ->
                com.example.model.LoggedWorkoutEntry(
                    id = "",
                    titleHu = raw
                ).localizedTitle(lang, uiState.programs, uiState.exercises)
            } ?: tr(lang, "Tervezett edzés", "Planned workout", "Geplantes Training", "Antrenament planificat")
            tr(
                lang,
                "$displayTitle • $completedExsCount / $totalExsCount gyakorlat",
                "$displayTitle • $completedExsCount / $totalExsCount exercises",
                "$displayTitle • $completedExsCount / $totalExsCount Übungen",
                "$displayTitle • $completedExsCount / $totalExsCount exerciții"
            )
        }

        val elapsedSec = activeSnapshotForSummary?.let {
            com.repforge.app.StudioWorkoutSessionPresentation.elapsedSeconds(it, summaryNow)
        }
        val durationLabel = elapsedSec?.let(com.repforge.app.StudioWorkoutSessionPresentation::durationLabel)
            ?: "—"

        val volumeOrDistLabel: String
        val volumeOrDistHeader: String
        if (isOutdoor) {
            volumeOrDistHeader = strings.distanceLabel
            volumeOrDistLabel = String.format(com.example.localization.StudioLocale.forLanguage(lang), "%.2f km", effectiveOutdoorDistanceKm)
        } else {
            val totalVolKg = uiState.activeSetsByExerciseId.values.flatten().filter { it.completed }.sumOf { (it.weightKg * it.reps).toDouble() }
            val totalReps = uiState.activeSetsByExerciseId.values.flatten().filter { it.completed }.sumOf { it.reps }
            if (totalVolKg > 0.0) {
                volumeOrDistHeader = tr(lang, "Volumen", "Volume", "Volumen", "Volum")
                volumeOrDistLabel = "${totalVolKg.toInt()} kg"
            } else {
                volumeOrDistHeader = tr(lang, "Ismétlések", "Repetitions", "Wiederholungen", "Repetări")
                volumeOrDistLabel = "$totalReps ${tr(lang, "ism.", "reps", "Wdh.", "rep.")}"
            }
        }

        val calories = com.repforge.app.StudioWorkoutSummaryPresentation.calories(
            activeSnapshotForSummary, summaryLiveHealth, summaryNow)
        val caloriesLabel = calories?.let { "${it.toInt()} kcal" } ?: "— kcal"

        item {
            Row(
                modifier = Modifier.fillMaxWidth().testTag("workout_summary_header_row"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.workoutSummaryTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                    Text(
                        text = summarySubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                }
                com.example.ui.components.StudioWorkoutHealthAction()
            }
        }


        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryStatTile(tr(lang, "Időtartam", "Duration", "Dauer", "Durată"), durationLabel, Modifier.weight(1f))
                SummaryStatTile(volumeOrDistHeader, volumeOrDistLabel, Modifier.weight(1f))
                SummaryStatTile(tr(lang, "Kalória", "Calories", "Kalorien", "Calorii"), caloriesLabel, Modifier.weight(1f))
            }
        }

        if (uiState.activeWorkoutExercises.isNotEmpty()) item(key = "exercise_feedback_heading") {
            TrainPilotOutlinedButton(onClick = { exerciseFeedbackExpanded = !exerciseFeedbackExpanded },
                modifier = Modifier.fillMaxWidth().testTag("summary_exercise_feedback_toggle")) {
                Text(tr(lang, "Gyakorlatok visszajelzései", "Exercise feedback", "Übungsfeedback", "Feedback exerciții") +
                    " (${uiState.activeWorkoutExercises.size})", modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Icon(if (exerciseFeedbackExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
            }
        }
        if (exerciseFeedbackExpanded) uiState.activeWorkoutExercises.forEach { exercise ->
            item(key = "exercise_feedback_${exercise.id}") {
                val same = uiState.exerciseFeedbackWorkoutId == uiState.activeWorkoutId
                com.example.ui.components.StudioExerciseFeedbackReview(
                    effort = if (same) com.repforge.app.StudioWorkoutFeedback.efforts.getOrNull(
                        uiState.exerciseFeedbackChoices[exercise.id] ?: -1) else null,
                    note = if (same) uiState.exerciseFeedbackNotes[exercise.id].orEmpty() else "",
                    language = uiState.language.name.lowercase(), title = exercise.localizedName(lang),
                    enabled = !uiState.isSavingWorkout,
                    onEffort = { onExerciseEffort(exercise.id, com.repforge.app.StudioWorkoutFeedback.efforts.indexOf(it)) },
                    onNote = { onExerciseNote(exercise.id, it) }, tag = "summary_feedback_${exercise.id}")
            }
        }

        // Optional overall feeling is separate from the exercise answers.
        item(key = "overall_feedback") {
            val same = uiState.summaryFeedbackWorkoutId == uiState.activeWorkoutId
            com.example.ui.components.StudioWorkoutOverallFeedback(
                effort = if (same) com.repforge.app.StudioWorkoutFeedback.efforts.getOrNull(uiState.workoutSummaryMoodIndex) else null,
                note = if (same) uiState.workoutSummaryNote else "",
                language = lang.name.lowercase(), enabled = !uiState.isSavingWorkout,
                onEffort = { onSelectMood(com.repforge.app.StudioWorkoutFeedback.efforts.indexOf(it)) },
                onNote = onUpdateNote, tag = "workout_overall_${uiState.activeWorkoutId}")
        }

        // Dedicated Workout Photos Card ("Edzésfotók")
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(20.dp))
                    .testTag("workout_summary_photos_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = tr(lang, "Edzésfotók", "Workout Photos", "Trainingsfotos", "Fotografii de antrenament"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                        }
                        if (attachedPhotos.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = palette.surfaceElevated
                            ) {
                                Text(
                                    text = "${attachedPhotos.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Category chips: Edzés előtt / Edzés után / Egyéb
                    Text(
                        text = tr(lang, "Kategória kiválasztása", "Select category", "Kategorie wählen", "Selectează categoria"),
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val categories = listOf(
                            "before" to tr(lang, "Edzés előtt", "Before", "Vorher", "Înainte"),
                            "after" to tr(lang, "Edzés után", "After", "Nachher", "După"),
                            "other" to tr(lang, "Egyéb", "Other", "Sonstiges", "Altele")
                        )
                        categories.forEach { (catKey, catLabel) ->
                            val isSelected = selectedPhotoCategory == catKey
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) palette.primary else palette.surfaceElevated,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedPhotoCategory = catKey }
                                    .testTag("summary_photo_cat_$catKey")
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = catLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) palette.onPrimary else palette.textPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Camera and Gallery buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                try {
                                    pendingPhoto = com.repforge.app.NativeWorkoutPhotos.prepare(context,
                                        uiState.activeWorkoutId, selectedPhotoCategory)
                                    photoError = false
                                    photoBusy = true
                                    summaryCameraLauncher.launch(photoRepo.prepareCameraCaptureIntent())
                                } catch (_: Exception) {
                                    pendingPhoto = null
                                    photoBusy = false
                                    photoError = true
                                }
                            },
                            enabled = !uiState.isSavingWorkout && !photoBusy,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.surfaceElevated,
                                contentColor = palette.textPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("workout_summary_camera_button")
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = palette.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr(lang, "Kamera", "Camera", "Kamera", "Cameră"), fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                try {
                                    pendingPhoto = com.repforge.app.NativeWorkoutPhotos.prepare(context,
                                        uiState.activeWorkoutId, selectedPhotoCategory)
                                    photoError = false
                                    photoBusy = true
                                    summaryGalleryLauncher.launch(photoRepo.createPickImageIntent())
                                } catch (_: Exception) {
                                    pendingPhoto = null
                                    photoBusy = false
                                    photoError = true
                                }
                            },
                            enabled = !uiState.isSavingWorkout && !photoBusy,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.surfaceElevated,
                                contentColor = palette.textPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("workout_summary_gallery_button")
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = palette.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr(lang, "Galéria", "Gallery", "Galerie", "Galerie"), fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (photoError) {
                        Text(tr(lang, "A fotóművelet nem sikerült. Próbáld újra.",
                            "Photo operation failed. Please try again.",
                            "Fotoaktion fehlgeschlagen. Bitte erneut versuchen.",
                            "Operațiunea foto a eșuat. Încearcă din nou."),
                            color = palette.danger, modifier = Modifier.testTag("summary_photo_error"))
                    }

                    // Photos preview row or empty prompt
                    if (attachedPhotos.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(attachedPhotos, key = { it.id }) { photo ->
                                val bitmap by produceState<Bitmap?>(initialValue = null, key1 = photo.id) {
                                    value = withContext(Dispatchers.IO) {
                                        try {
                                            val file = photoRepo.photoFile(photo.id)
                                            if (file.isFile && file.length() > 0) {
                                                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                                BitmapFactory.decodeFile(file.absolutePath, bounds)
                                                val sample = maxOf(1, maxOf(bounds.outWidth / 240, bounds.outHeight / 240))
                                                val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                                                BitmapFactory.decodeFile(file.absolutePath, opts)
                                            } else null
                                        } catch (_: Exception) {
                                            null
                                        }
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = palette.surfaceElevated,
                                    modifier = Modifier
                                        .size(width = 96.dp, height = 120.dp)
                                        .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(12.dp))
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap!!.asImageBitmap(),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PhotoLibrary,
                                                    contentDescription = null,
                                                    tint = palette.textMuted
                                                )
                                            }
                                        }

                                        // Category badge on bottom
                                        val catText = when (photo.label) {
                                            "before" -> tr(lang, "Előtte", "Before", "Vorher", "Înainte")
                                            "after" -> tr(lang, "Utána", "After", "Nachher", "După")
                                            else -> tr(lang, "Egyéb", "Other", "Sonstiges", "Altele")
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(bottomStart = 12.dp, topEnd = 8.dp),
                                            color = palette.background.copy(alpha = 0.85f),
                                            modifier = Modifier.align(Alignment.BottomStart)
                                        ) {
                                            Text(
                                                text = catText,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = palette.textPrimary,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        // Delete button top right
                                        Surface(
                                            shape = CircleShape,
                                            color = com.repforge.app.ui.theme.TrainPilotActionAppearance.negativeBackground,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(48.dp)
                                                .border(1.dp, com.repforge.app.ui.theme.TrainPilotActionAppearance.negativeOutline, CircleShape)
                                                .clip(CircleShape)
                                                .clickable(enabled = !uiState.isSavingWorkout && !photoBusy) {
                                                    scope.launch {
                                                        photoBusy = true
                                                        try {
                                                            val request = com.repforge.app.NativeWorkoutPhotos.prepare(
                                                                context, uiState.activeWorkoutId, selectedPhotoCategory)
                                                            showSavedPhotos(com.repforge.app.NativeWorkoutPhotos.remove(
                                                                context, request, photo.id))
                                                        } catch (cancelled: kotlinx.coroutines.CancellationException) {
                                                            throw cancelled
                                                        } catch (_: Exception) {
                                                            photoError = true
                                                        } finally {
                                                            photoBusy = false
                                                        }
                                                    }
                                                }
                                                .testTag("summary_delete_photo_${photo.id}")
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = tr(lang, "Törlés", "Delete", "Löschen", "Șterge"),
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = tr(
                                lang,
                                "Csatolj fotót az edzésről a fejlődés követéséhez (opcionális).",
                                "Attach a photo to track your workout progress (optional).",
                                "Hänge ein Foto an, um deinen Trainingsfortschritt zu verfolgen (optional).",
                                "Atașează o fotografie pentru a urmări progresul antrenamentului (opțional)."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textMuted
                        )
                    }
                }
            }
        }

        // Action buttons: Mentés, Szüneteltetés és kilépés, Edzés elvetése
        item {
            com.repforge.app.ui.theme.TrainPilotSaveButton(
                onClick = onSaveWorkout,
                enabled = !uiState.isSavingWorkout && !photoBusy,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_workout_summary_button")
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.saveAction, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            TrainPilotOutlinedButton(
                onClick = onPauseAndExit,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(imageVector = Icons.Default.Pause, contentDescription = null, tint = palette.textPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.pauseAndExitAction, color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            com.repforge.app.ui.theme.TrainPilotNegativeButton(
                onClick = onDiscardWorkout,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.discardWorkoutAction, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
}

@Composable
private fun SummaryStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = palette.surfaceCard,
        modifier = modifier.trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
        }
    }
}
