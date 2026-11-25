package com.example.ui.screens

import com.example.ui.theme.trainPilotScreenBackdrop
import com.example.ui.theme.glassTile

import com.example.ui.theme.glassCardFill
import com.example.ui.theme.trainPilotGlassMaterial
import com.repforge.app.ui.theme.trainPilotClickable
import com.example.ui.components.activeWorkoutDockFrame
import com.example.ui.components.StudioCompactAction
import com.example.ui.components.StudioCompactActionRow
import com.example.ui.components.StudioAdaptiveControlRow
import com.example.ui.components.StudioBilateralSetControls
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription

import com.repforge.app.ui.theme.TrainPilotCardSpacing

import com.repforge.app.ui.theme.trainPilotBorder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import com.repforge.app.ui.theme.TrainPilotButton as Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.DisposableEffect
import com.example.localization.LocalizedStrings
import com.example.model.ExerciseItem
import com.example.model.ExerciseSetItem
import com.example.model.SubScreen
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState

/**
 * 3. KÉTOLDALI MÉRÉS (2 STOPPER OLDALANKÉNT: Bal és Jobb oldal külön-külön stopperrel + oldalankénti sorozat-adatrögzítő)
 */
@OptIn(ExperimentalLayoutApi::class)
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
    onOpenExerciseDetail: (String) -> Unit = {},
    exerciseFeedback: @Composable () -> Unit = {},
    onFinishWorkout: () -> Unit = { onSelectMeasurementMode(SubScreen.WORKOUT_SUMMARY) },
    onDiscardWorkout: () -> Unit = {}
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
    val coachOwner = com.example.ui.components.rememberStudioWorkoutCoachOwner()

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
    var restTimerLargeMode by remember(com.repforge.app.StudioRestTimer.visible(uiState)) { mutableStateOf(false) }
    // 2808/9: same scroll sizing as the strength screen (user scroll only; ticking never opens large).
    val bilateralListState = rememberLazyListState()
    val userScrollSizesRest = com.example.ui.components.rememberRestDockScrollConnection(bilateralListState,
        com.repforge.app.StudioRestTimer.visible(uiState), restTimerLargeMode) { restTimerLargeMode = it }
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
                        com.example.ui.components.TrainPilotTextField(
                            value = editLeftText,
                            onValueChange = { editLeftText = it.filter { ch -> ch.isDigit() } },
                            label = { Text(tr(lang, "Bal oldal (mp)", "Left side (s)", "Linke Seite (Sek.)", "Partea stângă (sec)")) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        com.example.ui.components.TrainPilotTextField(
                            value = editRightText,
                            onValueChange = { editRightText = it.filter { ch -> ch.isDigit() } },
                            label = { Text(tr(lang, "Jobb oldal (mp)", "Right side (s)", "Rechte Seite (Sek.)", "Partea dreaptă (sec)")) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    StudioCompactActionRow(listOf(
                        StudioCompactAction(tr(lang, "Bal −5 $secWord", "Left −5 $secWord", "Links −5 $secWord", "Stg. −5 $secWord")) { editLeftText = ((editLeftText.toIntOrNull() ?: 30) - 5).coerceAtLeast(1).toString() },
                        StudioCompactAction(tr(lang, "Bal +5 $secWord", "Left +5 $secWord", "Links +5 $secWord", "Stg. +5 $secWord")) { editLeftText = ((editLeftText.toIntOrNull() ?: 30) + 5).coerceAtMost(600).toString() },
                        StudioCompactAction(tr(lang, "Jobb −5 $secWord", "Right −5 $secWord", "Rechts −5 $secWord", "Dr. −5 $secWord")) { editRightText = ((editRightText.toIntOrNull() ?: 30) - 5).coerceAtLeast(1).toString() },
                        StudioCompactAction(tr(lang, "Jobb +5 $secWord", "Right +5 $secWord", "Rechts +5 $secWord", "Dr. +5 $secWord")) { editRightText = ((editRightText.toIntOrNull() ?: 30) + 5).coerceAtMost(600).toString() }
                    ))
                    com.example.ui.components.TrainPilotTextField(
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
                com.repforge.app.ui.theme.TrainPilotTextButton(onClick = { editingBilateralSet = null }) {
                    Text(strings.cancelBtn, color = palette.textSecondary)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .trainPilotScreenBackdrop(palette)
            .testTag("active_bilateral_screen")
    ) {
        uiState.statusBannerMessage?.let { Text(it, color = palette.danger) }
        native.statusMessage?.let { Text(it, color = palette.danger) }
        if (target == null) Text(tr(uiState.language, "Nincs mérhető sorozat az aktuális natív gyakorlatban.",
            "No measurable set in the current native exercise.", "Kein messbarer Satz in der aktuellen nativen Übung.",
            "Nu există o serie măsurabilă în exercițiul nativ curent."), color = palette.textSecondary)

        LazyColumn(
            state = bilateralListState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .nestedScroll(userScrollSizesRest),
            contentPadding = PaddingValues(horizontal = TrainPilotCardSpacing.ScreenGutter, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(TrainPilotCardSpacing.CardGap)
        ) {
            // 2775: same header as the strength screen; the duplicated title/name/target texts are gone.
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.testTag("bilateral_header")) {
                    com.example.ui.components.StudioActiveWorkoutTop(uiState, workoutExercises, uiState.activeExerciseIndex,
                        onBack, onOpenSettings, onSwitchExercise)
                    com.example.ui.components.StudioActiveExerciseTitleRow(lang, exerciseTitle,
                        tr(lang,
                            "Kétoldali stopper • $selectedTargetSetNumber. sorozat • $defaultTargetSec mp / oldal",
                            "Both sides • set $selectedTargetSetNumber • $defaultTargetSec s / side",
                            "Beidseitig • Satz $selectedTargetSetNumber • $defaultTargetSec Sek. / Seite",
                            "Bilateral • seria $selectedTargetSetNumber • $defaultTargetSec sec / parte"),
                        currentExercise,
                        currentExercise?.id ?: uiState.exercises.firstOrNull { it.localizedName(lang) == exerciseTitle || it.nameHu == exerciseTitle }?.id,
                        onOpenExerciseLibrary, onOpenExerciseDetail, currentExercise?.id ?: uiState.selectedExerciseId, coachOwner)
                }
            }

            // 1. BAL OLDAL STOPPER (Left Side Stopwatch)
            item {
                val targetSetObj = uiState.activeSets.find { it.setNumber == selectedTargetSetNumber }
                val isLeftRecordedInTarget = targetSetObj?.leftRecorded == true || targetSetObj?.completed == true
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = palette.glassTile(palette.surfaceCard, elevated = true),
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
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
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
                                Column(modifier = Modifier.weight(1f)) {
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
                                                "Mérés folyamatban ●",
                                                "Measuring ●",
                                                "Messung läuft ●",
                                                "Măsurare în curs ●"
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
                                        style = MaterialTheme.typography.bodySmall,
                                        color = when {
                                            leftRunning -> palette.primary
                                            isLeftRecordedInTarget -> palette.success
                                            else -> palette.textSecondary
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))
                            // Reserve the readout before measuring the side description.
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
                                    modifier = Modifier.widthIn(min = 98.dp, max = 112.dp)
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                        .wrapContentWidth(Alignment.CenterHorizontally).testTag("bilateral_left_live_readout")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Left stopwatch controls
                        StudioAdaptiveControlRow {
                            Button(
                                onClick = { target?.let { if (leftRunning) nativeModel.pauseSetStopwatch(it) else nativeModel.startSetStopwatch(it, "left") } },
                            enabled = target != null,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.primary, // 2775
                                    contentColor = palette.onPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 44.dp)
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
                                    .weight(1f)
                                    .heightIn(min = 44.dp)
                                    .semantics { contentDescription = tr(lang, "Leállítás és rögzítés", "Stop and record", "Stoppen & speichern", "Oprește și înregistrează") }
                                    .testTag("bilateral_left_stop_record_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = tr(lang, "Rögzítés", "Record", "Speichern", "Salvează"),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        StudioCompactActionRow(listOf(
                            StudioCompactAction(tr(lang, "Nulláz", "Reset", "Reset", "Reset"), target != null) { target?.let(nativeModel::resetSetStopwatch) },
                            StudioCompactAction("+5 $secWord", target != null) { target?.let { nativeModel.adjustSetStopwatch(it, "left", 5) } },
                            StudioCompactAction("+15 $secWord", target != null) { target?.let { nativeModel.adjustSetStopwatch(it, "left", 15) } }
                        ))
                    }
                }
            }

            // 2. JOBB OLDAL STOPPER (Right Side Stopwatch)
            item {
                val targetSetObj = uiState.activeSets.find { it.setNumber == selectedTargetSetNumber }
                val isRightRecordedInTarget = targetSetObj?.rightRecorded == true || targetSetObj?.completed == true
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = palette.glassTile(palette.surfaceCard, elevated = true),
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
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
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
                                Column(modifier = Modifier.weight(1f)) {
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
                                                "Mérés folyamatban ●",
                                                "Measuring ●",
                                                "Messung läuft ●",
                                                "Măsurare în curs ●"
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
                                        style = MaterialTheme.typography.bodySmall,
                                        color = when {
                                            rightRunning -> palette.primary
                                            isRightRecordedInTarget -> palette.success
                                            else -> palette.textSecondary
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))
                            // Reserve the readout before measuring the side description.
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
                                    modifier = Modifier.widthIn(min = 98.dp, max = 112.dp)
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                        .wrapContentWidth(Alignment.CenterHorizontally).testTag("bilateral_right_live_readout")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Right stopwatch controls
                        StudioAdaptiveControlRow {
                            Button(
                                onClick = { target?.let { if (rightRunning) nativeModel.pauseSetStopwatch(it) else nativeModel.startSetStopwatch(it, "right") } },
                            enabled = target != null,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = palette.primary, // 2775
                                    contentColor = palette.onPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 44.dp)
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
                                    .weight(1f)
                                    .heightIn(min = 44.dp)
                                    .semantics { contentDescription = tr(lang, "Leállítás és rögzítés", "Stop and record", "Stoppen & speichern", "Oprește și înregistrează") }
                                    .testTag("bilateral_right_stop_record_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = tr(lang, "Rögzítés", "Record", "Speichern", "Salvează"),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        StudioCompactActionRow(listOf(
                            StudioCompactAction(tr(lang, "Nulláz", "Reset", "Reset", "Reset"), target != null) { target?.let(nativeModel::resetSetStopwatch) },
                            StudioCompactAction("+5 $secWord", target != null) { target?.let { nativeModel.adjustSetStopwatch(it, "right", 5) } },
                            StudioCompactAction("+15 $secWord", target != null) { target?.let { nativeModel.adjustSetStopwatch(it, "right", 15) } }
                        ))
                    }
                }
            }

            // 3. SOROZATOK – KÉTOLDALI ADATRÖGZÍTŐ ABLAK (Sets table with separate Left & Right recording windows)
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = palette.glassTile(palette.surfaceCard, elevated = true),
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
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = palette.primary.copy(alpha = 0.16f),
                                modifier = Modifier
                                    .trainPilotClickable(RoundedCornerShape(10.dp)) { onAddSet() }
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
                                    .trainPilotClickable(RoundedCornerShape(14.dp)) { selectedTargetSetNumber = setItem.setNumber }
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
                                StudioBilateralSetControls(setItem, isSelectedTarget, lang,
                                    onEdit = { selectedTargetSetNumber = setItem.setNumber; editingBilateralSet = setItem },
                                    onRir = { editingBilateralSet = setItem },
                                    onToggle = { onToggleSetCompleted(setItem.setNumber) })
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
                    color = palette.glassTile(palette.surfaceCard, elevated = true),
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
                        if (compareLeft != 0 || compareRight != 0) Text(
                            text = when {
                                compareLeft == 0 && compareRight == 0 -> tr(
                                    lang,
                                    "", "", "", "" // 2778: no hint before any measurement
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
            shape = RoundedCornerShape(18.dp),
            color = palette.glassCardFill(palette.background), // 2776 (A): darker dock; 2810/3: glass bar in Liquid Glass
            modifier = Modifier
                .fillMaxWidth()
                .activeWorkoutDockFrame(palette.background, palette.primary, 18.dp, glass = palette.isLiquidGlass)
                .trainPilotGlassMaterial(palette, RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 2778: same rest dock as the strength screen (compact <-> large card), not a hint line.
                com.example.ui.components.StudioRestTimerDock(uiState, restTimerLargeMode,
                    onLarge = { restTimerLargeMode = it }, onAdjust = onAdjustRestTimer,
                    onToggle = onToggleRestRunning, onSkip = onSkipRest, skipLabel = strings.skipAction,
                    showIdle = false)

                // 2772: ‹ Previous · ⏹ Finish · Next › (Finish = save & finish / discard / keep training).
                com.example.ui.components.ActiveWorkoutBottomBar(
                    language = uiState.language,
                    enabled = uiState.phoneControlsWorkout && !uiState.watchControlPending && !uiState.isSavingWorkout,
                    hasNext = uiState.activeExerciseIndex + 1 < workoutExercises.size,
                    completedSets = uiState.activeSetsByExerciseId.values.sumOf { sets -> sets.count { it.completed } },
                    totalSets = uiState.activeSetsByExerciseId.values.sumOf { it.size },
                    elapsedLabel = "",
                    tagPrefix = "bilateral",
                    onPrevious = onPreviousExercise,
                    onNext = onNextExercise,
                    onFinish = onFinishWorkout,
                    onDiscard = onDiscardWorkout
                )
            }
        }
    }
}
