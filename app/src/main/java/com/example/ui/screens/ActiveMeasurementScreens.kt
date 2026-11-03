package com.example.ui.screens

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
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState

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
    onOpenExerciseLibrary: () -> Unit = {}
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
    val currentExerciseName = currentExercise?.nameHu ?: "Goblet guggolás"
    val currentExerciseId = currentExercise?.id ?: "goblet_squat"
    val currentExerciseSubtitle = if (currentExercise != null) {
        "${currentExercise.primaryMusclesHu} • ${currentExercise.equipmentHu} • ${currentExercise.defaultSets}×${currentExercise.defaultReps} • Pihenő: ${currentExercise.defaultRestSec} mp"
    } else {
        "Láb • Kézisúlyzó • 3×8–20 • Pihenő: 90 mp"
    }
    var exerciseDropdownExpanded by remember { mutableStateOf(false) }

    // Interactive edit dialog state for filling in reps and weight
    var editingSet by remember { mutableStateOf<ExerciseSetItem?>(null) }
    var editWeightText by remember { mutableStateOf("") }
    var editRepsText by remember { mutableStateOf("") }
    var editRirText by remember { mutableStateOf("") }

    val targetSet = editingSet
    if (targetSet != null) {
        AlertDialog(
            onDismissRequest = { editingSet = null },
            containerColor = palette.surfaceCard,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textSecondary,
            title = {
                Text(
                    text = "✏️ ${targetSet.setNumber}. sorozat kitöltése",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Súly (kg):",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.primary,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = editWeightText,
                        onValueChange = { editWeightText = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("edit_set_weight_field")
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("5.5", "8", "10", "14", "16", "20").forEach { presetKg ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = palette.surfaceElevated,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { editWeightText = presetKg }
                            ) {
                                Text(
                                    text = "$presetKg kg",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ismétlések száma:",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.primary,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = editRepsText,
                        onValueChange = { editRepsText = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("edit_set_reps_field")
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("8", "10", "12", "15", "20").forEach { presetReps ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = palette.surfaceElevated,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { editRepsText = presetReps }
                            ) {
                                Text(
                                    text = "$presetReps ism.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "RIR (Ráspórolt ismétlések):",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textSecondary
                    )
                    OutlinedTextField(
                        value = editRirText,
                        onValueChange = { editRirText = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("edit_set_rir_field")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val kg = editWeightText.toFloatOrNull() ?: targetSet.weightKg
                        val reps = editRepsText.toIntOrNull() ?: targetSet.reps
                        val rir = editRirText.toIntOrNull()
                        onUpdateSetValuesExact(targetSet.setNumber, kg, reps, rir)
                        editingSet = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primary,
                        contentColor = palette.onPrimary
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Mentés", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TrainPilotOutlinedButton(
                    onClick = { editingSet = null },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Mégse", color = palette.textPrimary)
                }
            }
        )
    }

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

    // Live 1-second countdown for the Rest Timer when running
    LaunchedEffect(uiState.restTimerRunning, uiState.restTimerRemainingSec) {
        if (uiState.restTimerRunning && uiState.restTimerRemainingSec > 0) {
            delay(1000L)
            onAdjustRestTimer(-1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("active_strength_screen")
    ) {
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
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SubScreenTopBar(
                    title = "${uiState.activeWorkoutTitleHu} • ${currentExerciseIdx + 1} / $totalExercisesCount gyakorlat",
                    onBack = onBack,
                    onOpenSettings = onOpenSettings,
                    rightExtraContent = { com.example.ui.components.StudioWorkoutHealthAction() }
                )

                // Horizontal Exercise Sequence Pills (shows all exercises in the planned/queued workout)
                if (workoutExercises.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        workoutExercises.forEachIndexed { idx, exItem ->
                            val isCurrent = idx == currentExerciseIdx
                            val isDone = idx < currentExerciseIdx ||
                                (uiState.activeSetsByExerciseId[exItem.id]?.any { it.completed } == true)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = when {
                                    isCurrent -> palette.primary
                                    isDone -> palette.success.copy(alpha = 0.18f)
                                    else -> palette.surfaceElevated
                                },
                                modifier = Modifier
                                    .trainPilotBorder(
                                        width = 1.dp,
                                        color = when {
                                            isCurrent -> palette.primary
                                            isDone -> palette.success
                                            else -> palette.cardBorder
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSwitchExercise(exItem) }
                            ) {
                                Text(
                                    text = "${idx + 1}. ${exItem.nameHu}" + if (isDone && !isCurrent) " ✓" else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        isCurrent -> palette.onPrimary
                                        isDone -> palette.success
                                        else -> palette.textSecondary
                                    },
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

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
                                    contentDescription = "Gyakorlat váltása",
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
                                            text = "📚 Választás a 106+ gyakorlatkönyvtárból...",
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
                                .take(25)
                            switchableList.forEach { exOption ->
                                val isSel = exOption.id == currentExerciseId
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = exOption.nameHu,
                                                color = if (isSel) palette.primary else palette.textPrimary,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                            )
                                            Text(
                                                text = "${exOption.primaryMusclesHu} • ${exOption.equipmentHu}",
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
                            shape = RoundedCornerShape(12.dp),
                            color = palette.surfaceElevated,
                            modifier = Modifier
                                .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onOpenExerciseDetail(currentExerciseId) }
                        ) {
                            Text(
                                text = "Tárcsa & Bemelegítés",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.textPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Section header
        item {
            Text(
                text = "Sorozatok",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
        }

        // Table column header matching image 3: # | Előző | kg/kar | /láb | RIR | ✓ | —
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("#", style = MaterialTheme.typography.labelSmall, color = palette.textMuted, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                Text("Előző", style = MaterialTheme.typography.labelSmall, color = palette.textMuted, modifier = Modifier.width(56.dp), textAlign = TextAlign.Center)
                Text("kg/kar", style = MaterialTheme.typography.labelSmall, color = palette.textMuted, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("/láb", style = MaterialTheme.typography.labelSmall, color = palette.textMuted, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("RIR", style = MaterialTheme.typography.labelSmall, color = palette.textMuted, modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center)
                Text("✓", style = MaterialTheme.typography.labelSmall, color = palette.textMuted, modifier = Modifier.width(42.dp), textAlign = TextAlign.Center)
                Text("—", style = MaterialTheme.typography.labelSmall, color = palette.textMuted, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
            }
        }

        // Set rows (matching image 3)
        items(uiState.activeSets, key = { it.setNumber }) { setItem ->
            val isActiveSet = !setItem.completed && uiState.activeSets.firstOrNull { !it.completed }?.setNumber == setItem.setNumber
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .trainPilotBorder(
                        width = if (isActiveSet) 1.5.dp else 1.dp,
                        color = if (isActiveSet) palette.primary else palette.cardBorder,
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    // Top row with values
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Yellow circle number
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(palette.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${setItem.setNumber}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = palette.onPrimary
                            )
                        }

                        // 2. Previous column text
                        Text(
                            text = "${setItem.weightKg}×${setItem.reps}",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary,
                            modifier = Modifier.width(56.dp),
                            textAlign = TextAlign.Center
                        )

                        // 3. kg/kar input pill
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = palette.background,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .padding(horizontal = 2.dp)
                                .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(8.dp))
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    editingSet = setItem
                                    editWeightText = setItem.weightKg.toString()
                                    editRepsText = setItem.reps.toString()
                                    editRirText = if (setItem.actualRirProvided) setItem.rir.toString() else ""
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (setItem.weightKg % 1f == 0f) "${setItem.weightKg.toInt()}" else "${setItem.weightKg}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary
                                )
                            }
                        }

                        // 4. /láb (reps) input pill
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = palette.background,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .padding(horizontal = 2.dp)
                                .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(8.dp))
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    editingSet = setItem
                                    editWeightText = setItem.weightKg.toString()
                                    editRepsText = setItem.reps.toString()
                                    editRirText = if (setItem.actualRirProvided) setItem.rir.toString() else ""
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${setItem.reps}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary
                                )
                            }
                        }

                        // 5. RIR input pill
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = palette.background,
                            modifier = Modifier
                                .weight(0.9f)
                                .height(38.dp)
                                .padding(horizontal = 2.dp)
                                .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(8.dp))
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val nextRir = if (!setItem.actualRirProvided || setItem.rir >= 3) 0 else setItem.rir + 1
                                    onUpdateSetValuesExact(setItem.setNumber, setItem.weightKg, setItem.reps, nextRir)
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (setItem.actualRirProvided) "${setItem.rir}" else "—",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (setItem.rir > 0) palette.textPrimary else palette.textSecondary
                                )
                            }
                        }

                        // 6. OK button
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (setItem.completed) palette.primary else palette.surfaceElevated,
                            modifier = Modifier
                                .size(width = 40.dp, height = 38.dp)
                                .trainPilotBorder(1.dp, if (setItem.completed) palette.primary else palette.cardBorder, RoundedCornerShape(8.dp))
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onToggleSetCompleted(setItem.setNumber) }
                                .testTag("toggle_set_${setItem.setNumber}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "OK",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (setItem.completed) palette.onPrimary else palette.textPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // 7. Red circle minus button (removes set)
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF6B1A1A),
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable { onDeleteSet(setItem.setNumber) }
                                .testTag("delete_set_${setItem.setNumber}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "—",
                                    color = Color(0xFFFF8A80),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Row of quick adjustment chips matching screenshot 3
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SmallAdjustChip(String.format(java.util.Locale.forLanguageTag(uiState.language.name.lowercase()), "%+.1f kg", -0.5f)) { onAdjustSetValues(setItem.setNumber, -0.5f, 0, 0) }
                        SmallAdjustChip(String.format(java.util.Locale.forLanguageTag(uiState.language.name.lowercase()), "%+.1f kg", 0.5f)) { onAdjustSetValues(setItem.setNumber, 0.5f, 0, 0) }
                        SmallAdjustChip("-1 ism.") { onAdjustSetValues(setItem.setNumber, 0f, -1, 0) }
                        SmallAdjustChip("+1 ism.") { onAdjustSetValues(setItem.setNumber, 0f, 1, 0) }
                        SmallAdjustChip("RIR 0") { onUpdateSetValuesExact(setItem.setNumber, setItem.weightKg, setItem.reps, 0) }
                        SmallAdjustChip("RIR 1") { onUpdateSetValuesExact(setItem.setNumber, setItem.weightKg, setItem.reps, 1) }
                        SmallAdjustChip("RIR 2") { onUpdateSetValuesExact(setItem.setNumber, setItem.weightKg, setItem.reps, 2) }
                    }
                }
            }
        }

        // Add new set button
        item {
            TrainPilotOutlinedButton(
                onClick = onAddSet,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = palette.primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("+ Új sorozat hozzáadása", color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
            }
        }

        // Canonical history-based preview and explicit approval; no local progress formula.
        item { com.example.ui.components.StudioWorkoutCoachPanel(uiState.selectedExerciseId) }

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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
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
                                    text = "Pihenő: $formattedRest",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = palette.textPrimary
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                SmallAdjustChip("-15") { onAdjustRestTimer(-15) }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (uiState.restTimerRunning) palette.primary else palette.success,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onToggleRestRunning() }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (uiState.restTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = palette.onPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = if (uiState.restTimerRunning) "Szünet" else "Indít",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.onPrimary
                                        )
                                    }
                                }
                                SmallAdjustChip("+15") { onAdjustRestTimer(15) }
                                SmallAdjustChip("Kihagy") { onSkipRest() }
                                Icon(
                                    imageVector = Icons.Default.ExpandLess,
                                    contentDescription = "Nagy pihenő mód",
                                    tint = palette.primary,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable { restTimerManualOverride = true }
                                )
                            }
                        }
                    }
                } else {
                    // LARGE EXPANDED STICKY REST TIMER CARD (shown when scrolled down or expanded)
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = palette.surfaceCard,
                        modifier = Modifier
                            .fillMaxWidth()
                            .trainPilotBorder(1.5.dp, palette.primary.copy(alpha = 0.65f), RoundedCornerShape(18.dp))
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
                                            text = "Pihenőidő (Nagy mód)",
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
                                                    text = "Lapos mód",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = palette.textSecondary
                                                )
                                                Icon(
                                                    imageVector = Icons.Default.ExpandMore,
                                                    contentDescription = "Kis lapos mód",
                                                    tint = palette.textSecondary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = formattedRest,
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = palette.textPrimary
                                    )
                                    Text(
                                        text = if (uiState.restTimerRunning) "Visszaszámlálás fut • Rezgés a végén" else "Koppints a körre vagy az Indításra",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = palette.textSecondary
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(58.dp)
                                        .clip(CircleShape)
                                        .clickable { onToggleRestRunning() },
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
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("-15 mp", color = palette.textPrimary, fontWeight = FontWeight.Bold)
                                }
                                TrainPilotOutlinedButton(
                                    onClick = { onAdjustRestTimer(15) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("+15 mp", color = palette.textPrimary, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = onSkipRest,
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

                // Sticky < Előző | Következő > Navigation Row
                val hasNextExercise = currentExerciseIdx + 1 < totalExercisesCount
                val nextExName = workoutExercises.getOrNull(currentExerciseIdx + 1)?.nameHu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TrainPilotOutlinedButton(
                        onClick = onPreviousExercise,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("active_prev_exercise_btn")
                    ) {
                        Text(
                            text = if (currentExerciseIdx > 0) "< Előző (${currentExerciseIdx}/$totalExercisesCount)" else "< Előző",
                            color = palette.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                                "Következő: ${nextExName ?: "${currentExerciseIdx + 2}. gyakorlat"} >"
                            } else {
                                "Edzés befejezése >"
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
    onClick: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = palette.background,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSecondary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
    onOpenExerciseLibrary: () -> Unit = {}
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
    val exerciseNameHu = currentExercise?.nameHu ?: "Plank"
    val defaultTargetSec = (currentExercise?.targetSeconds ?: 45).coerceAtLeast(15)

    val nativeModel: com.repforge.app.ui.workout.ActiveWorkoutViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val native by nativeModel.uiState.collectAsState()
    LaunchedEffect(uiState.activeWorkoutId) { nativeModel.onEditorVisible() }
    val nativeExercise = native.exercises.firstOrNull { it.exerciseId == currentExercise?.id }
    val nativeSet = nativeExercise?.sets?.firstOrNull { !it.isCompleted }
    val target = nativeSet?.let { com.repforge.app.NativeWorkoutTarget(native.workoutId, native.generation,
        checkNotNull(nativeExercise).exerciseId, it.setId) }
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
            ExerciseItem(id = "plank", nameHu = "Plank", nameEn = "Plank", isTimed = true, targetSeconds = 45, primaryMusclesHu = "Törzs / Has"),
            ExerciseItem(id = "side_plank", nameHu = "Oldalsó plank", nameEn = "Side Plank", isTimed = true, targetSeconds = 40, primaryMusclesHu = "Oldalsó hasizom"),
            ExerciseItem(id = "wall_sit", nameHu = "Falnál ülés (Wall sit)", nameEn = "Wall Sit", isTimed = true, targetSeconds = 60, primaryMusclesHu = "Comb"),
            ExerciseItem(id = "dead_hang", nameHu = "Függeszkedés (Dead hang)", nameEn = "Dead Hang", isTimed = true, targetSeconds = 45, primaryMusclesHu = "Alkar / Hát"),
            ExerciseItem(id = "hollow_hold", nameHu = "Hollow body tartás", nameEn = "Hollow Hold", isTimed = true, targetSeconds = 30, primaryMusclesHu = "Hasizom"),
            ExerciseItem(id = "l_sit", nameHu = "L-tartás (L-sit)", nameEn = "L-Sit", isTimed = true, targetSeconds = 30, primaryMusclesHu = "Törzs"),
            ExerciseItem(id = "track_run", nameHu = "Kör futás (Időalapú)", nameEn = "Track Run", isTimed = true, targetSeconds = 60, primaryMusclesHu = "Kardió / Láb")
        )
        (fromDb + presets).distinctBy { it.nameHu.lowercase() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("active_timed_screen")
    ) {
        native.statusMessage?.let { Text(it, color = palette.danger) }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = TrainPilotCardSpacing.ScreenGutter, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)
        ) {
            item {
                SubScreenTopBar(
                    title = "${uiState.activeWorkoutTitleHu} • ${currentIdx + 1} / $totalCount gyakorlat",
                    onBack = onBack,
                    onOpenSettings = onOpenSettings,
                    rightExtraContent = { com.example.ui.components.StudioWorkoutHealthAction() }
                )
            }
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = palette.surfaceCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ExercisePostureMiniIllustration(
                                exerciseName = exerciseNameHu,
                                size = 54.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
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
                                            contentDescription = "Időalapú gyakorlat váltása",
                                            tint = palette.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Text(
                                        text = "Cél: $currentSetIndex. sorozat • Célidő: $targetSec mp • Koppints a váltáshoz",
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
                                                text = if (isRunning) "Stopper fut ●" else "Stopper készenlétben",
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
                                                text = "📚 Gyakorlatkönyvtár megnyitása...",
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
                                        val isSel = option.nameHu.equals(exerciseNameHu, ignoreCase = true)
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "${option.nameHu} (${option.targetSeconds.coerceAtLeast(30)} mp)",
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
                                exerciseId = timedExerciseOptions.firstOrNull { it.nameHu == exerciseNameHu }?.id
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
                                text = if (currentSec >= targetSec) "Cél teljesítve! 🎉" else "Hátra: $remaining mp",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (currentSec >= targetSec) palette.success else palette.textSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Cél: 00:$targetSec",
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
                        SmallAdjustChip("-5 mp") { target?.let { nativeModel.adjustSetStopwatch(it, "single", -5) } }
                        SmallAdjustChip("+5 mp") { target?.let { nativeModel.adjustSetStopwatch(it, "single", 5) } }
                        SmallAdjustChip("+15 mp") { target?.let { nativeModel.adjustSetStopwatch(it, "single", 15) } }
                        SmallAdjustChip("+30 mp") { target?.let { nativeModel.adjustSetStopwatch(it, "single", 30) } }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Stopwatch Controls: Start/Pause + Reset
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { target?.let { if (isRunning) nativeModel.pauseSetStopwatch(it) else nativeModel.toggleSetStopwatch(it, "single") } },
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
                                text = if (isRunning) "Szünet" else "Stopper indítása",
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
                    Button(
                        onClick = {
                            target?.let { nativeModel.recordSetStopwatch(it, "single") }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.surfaceElevated,
                            contentColor = palette.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sorozat rögzítése (${String.format("%02d:%02d", currentSec / 60, currentSec % 60)}) ✓",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Target Preset selector
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Célidő beállítása:",
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
                                    text = "$sec mp",
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
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Eddigi sorozatok:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.primary
                    )
                    completedSets.forEachIndexed { index, duration ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${index + 1}. sorozat", color = palette.textSecondary)
                            Text(
                                text = String.format("%02d:%02d ✓", duration / 60, duration % 60),
                                color = palette.success,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            text = "$currentSetIndex. sorozat (Aktuális)",
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

        }

        // Fixed Sticky Bottom navigation buttons
        Surface(
            color = palette.surfaceElevated,
            modifier = Modifier
                .fillMaxWidth()
                .trainPilotBorder(1.dp, palette.cardBorder, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
        ) {
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
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("< Előző", color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
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
                        text = if (hasNext) "Következő (${currentIdx + 2}/$totalCount) >" else "Edzés befejezése >",
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
    onOpenExerciseLibrary: () -> Unit = {}
) {
    val palette = LocalTrainPilotPalette.current

    val workoutExercises = uiState.activeWorkoutExercises.ifEmpty { uiState.exercises.take(6) }
    val currentExercise = workoutExercises.getOrNull(uiState.activeExerciseIndex)
        ?: uiState.exercises.find { it.id == uiState.selectedExerciseId }
        ?: uiState.exercises.firstOrNull { it.isBilateral || it.nameHu.contains("oldalsó plank", ignoreCase = true) }

    val exerciseTitle = currentExercise?.nameHu ?: "Oldalsó plank"
    val defaultTargetSec = currentExercise?.targetSeconds ?: 30

    // Determine target set for stopwatch recording (defaults to first uncompleted set)
    val firstUncompletedSetNum = uiState.activeSets.firstOrNull { !it.completed }?.setNumber
        ?: uiState.activeSets.lastOrNull()?.setNumber
        ?: 1
    var selectedTargetSetNumber by remember(firstUncompletedSetNum, uiState.activeSets.size) {
        mutableIntStateOf(firstUncompletedSetNum)
    }

    val nativeModel: com.repforge.app.ui.workout.ActiveWorkoutViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val native by nativeModel.uiState.collectAsState()
    LaunchedEffect(uiState.activeWorkoutId) { nativeModel.onEditorVisible() }
    val nativeExercise = native.exercises.firstOrNull { it.exerciseId == currentExercise?.id }
    val nativeSet = nativeExercise?.sets?.firstOrNull { it.setNumber == selectedTargetSetNumber }
    val target = nativeSet?.let { com.repforge.app.NativeWorkoutTarget(native.workoutId, native.generation,
        checkNotNull(nativeExercise).exerciseId, it.setId) }
    val leftSec = if (native.stopwatchSetId == nativeSet?.setId && native.stopwatchSide == "left") native.stopwatchSeconds.toInt() else nativeSet?.leftSeconds ?: 0
    val rightSec = if (native.stopwatchSetId == nativeSet?.setId && native.stopwatchSide == "right") native.stopwatchSeconds.toInt() else nativeSet?.rightSeconds ?: 0
    val leftRunning = native.stopwatchSetId == nativeSet?.setId && native.stopwatchSide == "left" && native.stopwatchRunning
    val rightRunning = native.stopwatchSetId == nativeSet?.setId && native.stopwatchSide == "right" && native.stopwatchRunning

    // Modal dialog for manual bilateral set editing
    var editingBilateralSet by remember { mutableStateOf<com.example.model.ExerciseSetItem?>(null) }
    var showExerciseDropdown by remember { mutableStateOf(false) }
    var restTimerLargeMode by remember { mutableStateOf(false) }

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
                    text = "${setToEdit.setNumber}. sorozat • Kétoldali adatrögzítés",
                    color = palette.textPrimary,
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "$exerciseTitle – állítsd be külön a Bal és Jobb oldal másodperceit:",
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
                            label = { Text("Bal oldal (mp)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        androidx.compose.material3.OutlinedTextField(
                            value = editRightText,
                            onValueChange = { editRightText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Jobb oldal (mp)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SmallAdjustChip("Bal -5mp") {
                            val v = (editLeftText.toIntOrNull() ?: 30) - 5
                            editLeftText = v.coerceAtLeast(1).toString()
                        }
                        SmallAdjustChip("Bal +5mp") {
                            val v = (editLeftText.toIntOrNull() ?: 30) + 5
                            editLeftText = v.coerceAtMost(600).toString()
                        }
                        SmallAdjustChip("Jobb -5mp") {
                            val v = (editRightText.toIntOrNull() ?: 30) - 5
                            editRightText = v.coerceAtLeast(1).toString()
                        }
                        SmallAdjustChip("Jobb +5mp") {
                            val v = (editRightText.toIntOrNull() ?: 30) + 5
                            editRightText = v.coerceAtMost(600).toString()
                        }
                    }
                    androidx.compose.material3.OutlinedTextField(
                        value = editRirText,
                        onValueChange = { editRirText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("RIR (Tartalék: 0–10)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (uiState.activeSets.size > 1) {
                        androidx.compose.material3.TextButton(
                            onClick = {
                                onDeleteSet(setToEdit.setNumber)
                                editingBilateralSet = null
                            }
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = palette.danger, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sorozat törlése", color = palette.danger, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val lSec = editLeftText.toIntOrNull() ?: setToEdit.leftSec
                        val rSec = editRightText.toIntOrNull() ?: setToEdit.rightSec
                        val rirVal = editRirText.toIntOrNull()
                        onUpdateBilateralSetExact(setToEdit.setNumber, lSec, rSec, rirVal)
                        editingBilateralSet = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.primary,
                        contentColor = palette.onPrimary
                    )
                ) {
                    Text("Mentés és rögzítés", fontWeight = FontWeight.Bold)
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
        native.statusMessage?.let { Text(it, color = palette.danger) }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = TrainPilotCardSpacing.ScreenGutter, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)
        ) {
            item {
                SubScreenTopBar(
                    title = "$exerciseTitle • Kétoldali stopper",
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
                            ExercisePostureMiniIllustration(
                                exerciseName = exerciseTitle,
                                size = 50.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = exerciseTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = "2 külön stopper oldalanként • A „Leállítás és rögzítés” gomb a kiválasztott (${selectedTargetSetNumber}.) sorozatba menti az időt",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary
                                )
                            }
                            SmallExerciseVideoButton(
                                exerciseName = exerciseTitle,
                                exerciseId = uiState.exercises.firstOrNull { it.nameHu == exerciseTitle }?.id
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
                                    text = "${uiState.activeExerciseIndex + 1} / ${workoutExercises.size.coerceAtLeast(1)} gyakorlat • Aktív cél: ${selectedTargetSetNumber}. sorozat",
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
                                        text = "Gyakorlat váltása ▾",
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
                                                    text = "${idx + 1}. ${ex.nameHu}",
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
                                                text = "📚 Gyakorlatkönyvtár megnyitása...",
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
                                            text = "B",
                                            fontWeight = FontWeight.Bold,
                                            color = if (leftRunning) palette.onPrimary else palette.textPrimary,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Bal oldal stopper",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.textPrimary
                                    )
                                    Text(
                                        text = when {
                                            leftRunning -> "Mérés folyamatban (${selectedTargetSetNumber}. sorozat - Bal) ●"
                                            isLeftRecordedInTarget -> "${selectedTargetSetNumber}. sorozat Bal oldal rögzítve (${targetSetObj?.leftSec ?: defaultTargetSec} mp) ✓"
                                            else -> "Cél: ${selectedTargetSetNumber}. sorozat Bal oldal (${defaultTargetSec} mp)"
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
                                onClick = { target?.let { if (leftRunning) nativeModel.pauseSetStopwatch(it) else nativeModel.toggleSetStopwatch(it, "left") } },
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
                                Text(if (leftRunning) "Szünet" else "Indítás", fontWeight = FontWeight.Bold)
                            }

                            // Leállítás és rögzítés button -> writes directly into selectedTargetSetNumber's Left slot!
                            Button(
                                onClick = {
                                    target?.let { nativeModel.recordSetStopwatch(it, "left") }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.primary,
                                    contentColor = palette.onPrimary
                                ),
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
                                    text = "Leállítás és rögzítés",
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
                            SmallAdjustChip("Nulláz") {
                                target?.let(nativeModel::resetSetStopwatch)
                            }
                            SmallAdjustChip("+5 mp") { target?.let { nativeModel.adjustSetStopwatch(it, "left", 5) } }
                            SmallAdjustChip("+15 mp") { target?.let { nativeModel.adjustSetStopwatch(it, "left", 15) } }
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
                                            text = "J",
                                            fontWeight = FontWeight.Bold,
                                            color = if (rightRunning) palette.onPrimary else palette.textPrimary,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Jobb oldal stopper",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.textPrimary
                                    )
                                    Text(
                                        text = when {
                                            rightRunning -> "Mérés folyamatban (${selectedTargetSetNumber}. sorozat - Jobb) ●"
                                            isRightRecordedInTarget -> "${selectedTargetSetNumber}. sorozat Jobb oldal rögzítve (${targetSetObj?.rightSec ?: defaultTargetSec} mp) ✓"
                                            else -> "Cél: ${selectedTargetSetNumber}. sorozat Jobb oldal (${defaultTargetSec} mp)"
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
                                onClick = { target?.let { if (rightRunning) nativeModel.pauseSetStopwatch(it) else nativeModel.toggleSetStopwatch(it, "right") } },
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
                                Text(if (rightRunning) "Szünet" else "Indítás", fontWeight = FontWeight.Bold)
                            }

                            // Leállítás és rögzítés button -> writes directly into selectedTargetSetNumber's Right slot!
                            Button(
                                onClick = {
                                    target?.let { nativeModel.recordSetStopwatch(it, "right") }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.primary,
                                    contentColor = palette.onPrimary
                                ),
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
                                    text = "Leállítás és rögzítés",
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
                            SmallAdjustChip("Nulláz") {
                                target?.let(nativeModel::resetSetStopwatch)
                            }
                            SmallAdjustChip("+5 mp") { target?.let { nativeModel.adjustSetStopwatch(it, "right", 5) } }
                            SmallAdjustChip("+15 mp") { target?.let { nativeModel.adjustSetStopwatch(it, "right", 15) } }
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
                            Column {
                                Text(
                                    text = "Sorozatok • Oldalankénti adatrögzítés",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = "Koppints egy sorozatra a cél-kijelöléshez, vagy az oldal mezőre a kézi szerkesztéshez",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.textSecondary
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = palette.primary.copy(alpha = 0.16f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onAddSet() }
                            ) {
                                Text(
                                    text = "+ Sorozat",
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
                                                text = "CÉL",
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
                                            Text(
                                                text = if (setItem.leftRecorded || setItem.completed) "Bal oldal ✓" else "Bal oldal",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = if (setItem.leftRecorded || setItem.completed) palette.success else palette.textSecondary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "${setItem.leftSec} mp",
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
                                            Text(
                                                text = if (setItem.rightRecorded || setItem.completed) "Jobb oldal ✓" else "Jobb oldal",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = if (setItem.rightRecorded || setItem.completed) palette.success else palette.textSecondary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "${setItem.rightSec} mp",
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
                                                contentDescription = "Sorozat kész",
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
                                text = "Oldalak egyensúlya (${selectedTargetSetNumber}. sorozat)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = palette.textPrimary
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (diff <= 3) palette.success.copy(alpha = 0.2f) else palette.primary.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = if (diff == 0) "Különbség: 0 mp" else "Eltérés: $diff mp",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (diff <= 3) palette.success else palette.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = when {
                                compareLeft == 0 && compareRight == 0 -> "Indítsd el a stoppereket a bal és jobb oldal méréséhez."
                                diff == 0 -> "Tökéletes szimmetria a bal ($compareLeft mp) és jobb ($compareRight mp) oldal között!"
                                diff <= 5 -> "Kiváló stabilitási egyensúly (Bal: $compareLeft mp • Jobb: $compareRight mp)."
                                else -> "Figyelem: $diff másodperc különbség (Bal: $compareLeft mp • Jobb: $compareRight mp)."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary
                        )
                    }
                }
            }
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
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
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
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = if (uiState.restTimerRunning) palette.primary else palette.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Pihenő: " + String.format("%02d:%02d", uiState.restTimerRemainingSec / 60, uiState.restTimerRemainingSec % 60),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (uiState.restTimerRunning) palette.primary else palette.textPrimary
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SmallAdjustChip("-15") { onAdjustRestTimer(-15) }
                                SmallAdjustChip("+15") { onAdjustRestTimer(15) }
                                SmallAdjustChip(if (uiState.restTimerRunning) "Szünet" else "Indít") { onToggleRestRunning() }
                                if (uiState.restTimerRunning || uiState.restTimerRemainingSec < uiState.defaultRestSec) {
                                    SmallAdjustChip("Átugrás") { onSkipRest() }
                                }
                            }
                        }
                        if (restTimerLargeMode) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Húzd felfelé a kompakt módhoz • Mindkét oldal rögzítése után a pihenő automatikusan elindul.",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
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
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("bilateral_prev_exercise_btn")
                    ) {
                        Text("< Előző", color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
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
                            text = if (hasNextExercise) "Következő (${uiState.activeExerciseIndex + 2}/${workoutExercises.size}) >" else "Edzés befejezése ✓",
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
    var distanceMeters by remember { mutableFloatStateOf(activeSnap.optDouble("distanceMeters", 0.0).toFloat()) }
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

    val startNativeGpsTracking = {
        if (!startRequested && (!isStarted || isPaused)) {
            startRequested = true
            serviceErrorMsg = "GPS-szolgáltatás indul; visszaigazolásra vár…"
            screenScope.launch {
                var confirmed = false
                try {
                    val activityName = uiState.activeWorkoutTitleHu.ifBlank { "Kültéri futás" }
                    check(onStartGpsTracking(activityName, routeOwnerKey)) { "A natív munkamenet nem készíthető elő." }
                    nativeMeasurement.onEditorVisible()
                    val frame = checkNotNull(com.repforge.app.PhoneWorkoutExecution.store(context).snapshot())
                    check(kotlinx.coroutines.withTimeoutOrNull(5_000) {
                        while (!com.repforge.app.IntervalTimerService.armed(frame.getString("workoutId"), frame.getLong("generation"))) delay(100)
                        true
                    } == true) { "A natív háttérvezérlő nem igazolta vissza az indítást." }
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
                    check(receipt) { "A GPS-szolgáltatás nem igazolta vissza az indítást." }
                    check(onStartGpsTracking(activityName, routeOwnerKey)) { "A GPS-mérés natív edzéshez kötése nem sikerült." }
                    confirmed = true
                    isStarted = true
                    isPaused = false
                    serviceErrorMsg = ""
                } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (error: Exception) { serviceErrorMsg = error.message ?: "A GPS-mérés nem indult el." }
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
            check(onStartGpsTracking(uiState.activeWorkoutTitleHu, "")) { "A beltéri munkamenet nem menthető." }
            val frame = com.repforge.app.StudioWorkoutBridge.startIndoor(context)
            isStarted = frame.optJSONObject(com.repforge.app.NativeSetStopwatch.FIELD)?.let {
                com.repforge.app.NativeSetStopwatch.running(it)
            } == true
            isPaused = !isStarted
            serviceErrorMsg = ""
        } catch (error: Exception) { serviceErrorMsg = error.message ?: "A beltéri időmérés nem indult el." }
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

    val finishMeasurement = {
        finishErrorMsg = ""
        var recorded = false
        try {
            var frame = com.repforge.app.NativeWorkoutEditing.project(context)
            if (!gpsActive && frame.optBoolean("workoutPaused")) {
                check(onPauseMeasurement(false))
                frame = com.repforge.app.NativeWorkoutEditing.project(context)
            }
            val saved = if (gpsActive) {
                val binding = frame.getJSONObject(com.repforge.app.NativeGpsMeasurement.FIELD)
                if (binding.optBoolean("applied")) frame
                else com.repforge.app.NativeWorkoutEditing.finishGps(context,
                    com.repforge.app.NativeGpsMeasurement.target(frame), binding.getString("id"))
            } else {
                val clock = frame.getJSONObject(com.repforge.app.NativeSetStopwatch.FIELD)
                if (!com.repforge.app.NativeSetStopwatch.running(clock)) frame = com.repforge.app.StudioWorkoutBridge.startIndoor(context)
                val current = frame.getJSONObject(com.repforge.app.NativeSetStopwatch.FIELD)
                com.repforge.app.NativeWorkoutEditing.stopStopwatch(context, com.repforge.app.NativeWorkoutTarget(
                    frame.getString("workoutId"), frame.getLong("generation"), current.getString("exerciseId"), current.getString("setId")))
            }
            val exercises = saved.getJSONArray("exercises")
            recorded = (0 until exercises.length()).any { i ->
                val sets = exercises.getJSONObject(i).getJSONArray("sets")
                (0 until sets.length()).any { sets.getJSONObject(it).optBoolean("done") }
            }
            if (gpsActive && !recorded) {
                com.repforge.app.StudioOutdoorFinish.receipt(saved)?.let { receipt ->
                    saved.put(com.repforge.app.StudioOutdoorFinish.FIELD, receipt)
                    com.repforge.app.NativeWorkoutEditing.commitEdit(context, saved, gpsEdit = true)
                }
            }
            // Stopping and opening Summary never requires inventing a completed set.
            // Save still requires a completed native set or a verified positive-time receipt.
            recorded = true
            isPaused = true
        } catch (error: Exception) { finishErrorMsg = error.message ?: "A mérés nem rögzíthető." }
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
            serviceErrorMsg = "A GPS-méréshez pontos helymeghatározási engedély szükséges. Engedély nélkül 0 m kerül rögzítésre."
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
            val distKm = distanceMeters / 1000f
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
    val distanceKm = distanceMeters / 1000f
    val formattedDistance = String.format(java.util.Locale("hu", "HU"), "%.2f km", distanceKm)
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
            !gpsActive -> "Beltéri időmérés • GPS kikapcsolva"
            !hasLocationPermission -> "Helymeghatározási engedély szükséges"
            isStarted && isPaused -> "GPS szüneteltetve"
            isStarted && hasServiceFix -> "Élő GPS-jel ($realGpsFixesCount pont • ±${gpsAccuracyMeters.toInt()} m)"
            isStarted -> "GPS-jelre vár…"
            else -> "Indításra kész"
        },
        gpsEnabled = gpsActive, started = isStarted, paused = isPaused,
        elapsed = formattedElapsed, distance = formattedDistance, pace = formattedPace,
        calories = "— kcal", heartRate = if (realHrBpm > 0) "♥ $realHrBpm bpm" else "♥ — bpm", pointCount = realGpsFixesCount,
        lapCount = lapCount, latestLap = lapSplits.lastOrNull().orEmpty(),
        error = listOf(finishErrorMsg, serviceErrorMsg).filter(String::isNotBlank).distinct().joinToString("\n"),
        language = uiState.language
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
            lapSplits = lapSplits + "${lapCount}. kör: $formattedElapsed ($formattedDistance)"
            lapCount += 1
        },
        finish = {
            if (finishMeasurement()) {
                isFullscreenMapOpen = false
                onSelectMeasurementMode(SubScreen.WORKOUT_SUMMARY)
            }
        },
        expandMap = { isFullscreenMapOpen = true }
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
        item {
            SubScreenTopBar(
                title = "Intervallumok beállítása",
                onBack = onBack,
                onOpenSettings = onOpenSettings,
                    rightExtraContent = { com.example.ui.components.StudioWorkoutHealthAction() }
            )
        }
        item {
            IntervalStepperCard(
                label = "Munkaidő",
                valueText = "$workSec mp",
                onMinus = { workSec = (workSec - 5).coerceAtLeast(10) },
                onPlus = { workSec = (workSec + 5).coerceAtMost(300) }
            )
        }
        item {
            IntervalStepperCard(
                label = "Pihenőidő",
                valueText = "$restSec mp",
                onMinus = { restSec = (restSec - 5).coerceAtLeast(10) },
                onPlus = { restSec = (restSec + 5).coerceAtMost(300) }
            )
        }
        item {
            IntervalStepperCard(
                label = "Ismétlések (körök)",
                valueText = "$rounds kör",
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
                        Text("Hangjelzés", color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                        Switch(checked = sound, onCheckedChange = { sound = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Rezgés", color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                        Switch(checked = vibration, onCheckedChange = { vibration = it })
                    }
                }
            }
        }
        item {
            Button(
                onClick = {
                    onSaveAndStartIntervals(workSec, restSec, rounds, sound, vibration, autoStep)
                    onSelectMeasurementMode(SubScreen.ACTIVE_TIMED)
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.primary,
                    contentColor = palette.onPrimary
                ),
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
    onDiscardWorkout: () -> Unit
) {
    val palette = LocalTrainPilotPalette.current
    val moods = listOf(
        "😄 Túl könnyű",
        "🙂 Könnyű",
        "💪 Pont jó",
        "😤 Nehéz",
        "🥵 Túl nehéz",
        "⚠️ Fájdalom"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .testTag("workout_summary_screen"),
        contentPadding = PaddingValues(horizontal = TrainPilotCardSpacing.ScreenGutter, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)
    ) {
        val isOutdoor = uiState.activeWorkoutSubScreen == SubScreen.ACTIVE_OUTDOOR_GPS || uiState.outdoorDistanceKm > 0f

        val summarySubtitle = if (isOutdoor) {
            val distLabel = if (uiState.outdoorDistanceKm > 0f) {
                String.format(java.util.Locale("hu", "HU"), "%.2f km", uiState.outdoorDistanceKm)
            } else {
                uiState.outdoorElapsedLabel.ifBlank { "00:00" }
            }
            val titleLabel = if (uiState.activeWorkoutTitleHu.contains("Kültéri", ignoreCase = true) || uiState.activeWorkoutTitleHu.contains("Futás", ignoreCase = true)) {
                uiState.activeWorkoutTitleHu
            } else {
                "Kültéri futás"
            }
            "$titleLabel • $distLabel"
        } else {
            val completedExsCount = uiState.activeWorkoutExercises.count { ex ->
                (uiState.activeSetsByExerciseId[ex.id] ?: emptyList()).any { it.completed }
            }
            val totalExsCount = uiState.activeWorkoutExercises.size.coerceAtLeast(1)
            val displayTitle = uiState.activeWorkoutTitleHu.ifBlank { "Tervezett edzés" }
            "$displayTitle • $completedExsCount / $totalExsCount gyakorlat"
        }

        val elapsedMs = if (uiState.workoutStartedEpochMs > 0L) {
            (System.currentTimeMillis() - uiState.workoutStartedEpochMs).coerceAtLeast(0L)
        } else 0L
        val elapsedSec = (elapsedMs / 1000L).toInt()
        val durationLabel = if (isOutdoor) {
            uiState.outdoorElapsedLabel.ifBlank { "00:00" }
        } else if (elapsedSec > 0) {
            String.format("%02d:%02d", elapsedSec / 60, elapsedSec % 60)
        } else {
            "00:00"
        }

        val volumeOrDistLabel: String
        val volumeOrDistHeader: String
        if (isOutdoor) {
            volumeOrDistHeader = "Távolság"
            volumeOrDistLabel = String.format(java.util.Locale("hu", "HU"), "%.2f km", uiState.outdoorDistanceKm)
        } else {
            val totalVolKg = uiState.activeSetsByExerciseId.values.flatten().filter { it.completed }.sumOf { (it.weightKg * it.reps).toDouble() }
            val totalReps = uiState.activeSetsByExerciseId.values.flatten().filter { it.completed }.sumOf { it.reps }
            if (totalVolKg > 0.0) {
                volumeOrDistHeader = "Volumen"
                volumeOrDistLabel = "${totalVolKg.toInt()} kg"
            } else {
                volumeOrDistHeader = "Ismétlések"
                volumeOrDistLabel = "$totalReps ism."
            }
        }

        // Daily calories and generic time/distance estimates are NOT workout sensor measurements.
        // Show an explicit unavailable state until a verified workout-specific Health record exists.
        val caloriesLabel = "— kcal (nincs hiteles edzésadat)"

        item {
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
        item { com.example.ui.components.StudioWorkoutHealthAction() }


        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryStatTile("Időtartam", durationLabel, Modifier.weight(1f))
                SummaryStatTile(volumeOrDistHeader, volumeOrDistLabel, Modifier.weight(1f))
                SummaryStatTile("Kalória", caloriesLabel, Modifier.weight(1f))
            }
        }

        // Mood selector ("Milyen volt az edzés?")
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = palette.surfaceCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Milyen volt az edzés?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        moods.forEachIndexed { idx, moodLabel ->
                            val selected = uiState.workoutSummaryMoodIndex == idx
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) palette.primary else palette.surfaceElevated,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSelectMood(idx) }
                                    .testTag("mood_chip_$idx")
                            ) {
                                Text(
                                    text = moodLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) palette.onPrimary else palette.textPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = uiState.workoutSummaryNote,
                        onValueChange = onUpdateNote,
                        placeholder = { Text("Megjegyzés hozzáadása az edzéshez...", color = palette.textMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }
        }

        // Action buttons: Mentés, Szüneteltetés és kilépés, Edzés elvetése
        item {
            Button(
                onClick = onSaveWorkout,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.primary,
                    contentColor = palette.onPrimary
                ),
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
            TrainPilotOutlinedButton(
                onClick = onDiscardWorkout,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null, tint = palette.danger)
                Spacer(modifier = Modifier.width(8.dp))
                Text(strings.discardWorkoutAction, color = palette.danger, fontWeight = FontWeight.SemiBold)
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
