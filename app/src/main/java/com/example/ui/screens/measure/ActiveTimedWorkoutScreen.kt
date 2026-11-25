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
import androidx.compose.ui.semantics.contentDescription

import com.repforge.app.ui.theme.TrainPilotCardSpacing

import com.repforge.app.ui.theme.TrainPilotOutlinedButton

import com.repforge.app.ui.theme.trainPilotBorder

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.DisposableEffect
import com.example.localization.LocalizedStrings
import com.example.model.ExerciseItem
import com.example.model.SubScreen
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.viewmodel.TrainPilotUiState

/**
 * 2. IDŐALAPÚ EDZÉS (Mockup 2/4 #2 - Plank)
 */
@OptIn(ExperimentalLayoutApi::class)
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
    onOpenExerciseDetail: (String) -> Unit = {},
    onAdjustRestTimer: (Int) -> Unit = {},
    onToggleRestRunning: () -> Unit = {},
    onSkipRest: () -> Unit = {},
    exerciseFeedback: @Composable () -> Unit = {},
    consumeIntervalStart: (com.repforge.app.NativeWorkoutTarget) -> Boolean = { false },
    onFinishWorkout: () -> Unit = { onSelectMeasurementMode(SubScreen.WORKOUT_SUMMARY) },
    onDiscardWorkout: () -> Unit = {}
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
    val coachOwner = com.example.ui.components.rememberStudioWorkoutCoachOwner()
    // 2778: rest timer on the timed screen too (same countdown + dock as the strength screen).
    StudioRestCountdown(uiState, onAdjustRestTimer)
    var restTimerLargeMode by remember(com.repforge.app.StudioRestTimer.visible(uiState)) { mutableStateOf(false) }
    val defaultTargetSec = (currentExercise?.targetSeconds ?: 45).coerceAtLeast(15)

    val nativeModel: com.repforge.app.ui.workout.ActiveWorkoutViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val native by nativeModel.uiState.collectAsState()
    LaunchedEffect(uiState.activeWorkoutId) { nativeModel.onEditorVisible() }
    val nativeExercise = native.exercises.firstOrNull { it.exerciseId == currentExercise?.id }
    val nativeSet = nativeExercise?.sets?.firstOrNull { !it.isCompleted }
    val target = nativeSet?.let { com.repforge.app.NativeWorkoutTarget(native.workoutId, native.generation,
        checkNotNull(nativeExercise).exerciseId, it.setId) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(target, lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) nativeModel.cancelPendingStopwatchStart()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            nativeModel.cancelPendingStopwatchStart()
        }
    }
    LaunchedEffect(uiState.pendingIntervalStart, target, native.workoutId, native.generation) {
        val requested = uiState.pendingIntervalStart
        if (requested != null && native.workoutId == requested.workoutId && native.generation == requested.generation &&
            consumeIntervalStart(requested) && requested == target &&
            lifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED))
            nativeModel.startIntervalsWhenReady(requested, uiState.pendingIntervalOwnerEpoch, uiState.pendingIntervalPauseRevision)
    }
    val intervalActive = native.timerWorkoutId == native.workoutId && native.timerGeneration == native.generation &&
        !native.timerRestOnly && native.timerKey.isNotEmpty() && !native.timerStopped &&
        !native.timerInterrupted && native.timerPhase != "complete"
    // Keep the live stopwatch in view; an active native interval opens its own controls.
    var intervalsExpanded by remember(native.workoutId, native.generation) { mutableStateOf(false) }
    LaunchedEffect(intervalActive) { if (intervalActive) intervalsExpanded = true }
    val currentSec = if (native.stopwatchSetId == nativeSet?.setId) native.stopwatchSeconds.toInt() else 0
    val isRunning = native.stopwatchSetId == nativeSet?.setId && native.stopwatchRunning
    var targetSec by remember(currentExercise?.id) { mutableIntStateOf(defaultTargetSec) }
    val currentSetIndex = nativeSet?.setNumber ?: ((nativeExercise?.sets?.size ?: 0) + 1)
    val completedSets = nativeExercise?.sets?.filter { it.isCompleted }?.map { it.reps.toIntOrNull() ?: 0 }.orEmpty()

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
            .trainPilotScreenBackdrop(palette)
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
                // 2775: same header as the strength screen + shared exercise title row.
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    com.example.ui.components.StudioActiveWorkoutTop(uiState, workoutExercises, currentIdx,
                        onBack, onOpenSettings, onSwitchExercise)
                    com.example.ui.components.StudioActiveExerciseTitleRow(uiState.language, exerciseNameHu,
                        tr(uiState.language,
                            "Stopper • $currentSetIndex. sorozat • célidő $targetSec mp",
                            "Stopwatch • set $currentSetIndex • target $targetSec s",
                            "Stoppuhr • Satz $currentSetIndex • Ziel $targetSec Sek.",
                            "Cronometru • seria $currentSetIndex • țintă $targetSec sec"),
                        currentExercise, currentExercise?.id ?: timedExerciseOptions.firstOrNull { it.localizedName(uiState.language) == exerciseNameHu }?.id,
                        onOpenExerciseLibrary, onOpenExerciseDetail, currentExercise?.id ?: uiState.selectedExerciseId, coachOwner)
                }
            }
            item {
                val lang = uiState.language
                val secWord = tr(lang, "mp", "s", "Sek.", "sec")
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = palette.glassTile(palette.surfaceCard, elevated = true),
                    modifier = Modifier
                        .fillMaxWidth()
                        .trainPilotBorder(1.25.dp, palette.sectionBorder, RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // 2775: name, target and switching live in the shared title row above; only the state stays here.
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isRunning) palette.primary.copy(alpha = 0.2f) else palette.secondary.copy(alpha = 0.18f),
                            modifier = Modifier.align(Alignment.CenterHorizontally).testTag("timed_stopwatch_state")
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
                                    text = if (intervalActive) {
                                        tr(lang, "Intervallummérés", "Interval measurement", "Intervallmessung", "Măsurare pe intervale")
                                    } else if (isRunning) {
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

                    if (nativeExercise != null) {
                        val intervalTarget = target ?: nativeExercise.sets.firstOrNull()?.let {
                            com.repforge.app.NativeWorkoutTarget(native.workoutId, native.generation, nativeExercise.exerciseId, it.setId)
                        }
                        if (intervalTarget != null) {
                            val timerTarget = com.repforge.app.NativeTimerTarget(native.workoutId, native.generation, native.timerKey)
                            com.repforge.app.ui.workout.IntervalOptions(native, nativeExercise, intervalActive, lang.code,
                                expanded = intervalsExpanded, onToggleExpanded = { intervalsExpanded = !intervalsExpanded },
                                onSave = { work, rest, rounds, sound, vibration, advance, global ->
                                    nativeModel.configureIntervals(intervalTarget, work, rest, rounds, sound, vibration, advance, global) },
                                onStart = { nativeModel.startIntervalsWhenReady(intervalTarget) },
                                onStop = { nativeModel.stopIntervals(timerTarget) },
                                onSkip = { nativeModel.skipRestTimer(timerTarget) })
                        }
                    }
                    if (!intervalActive) {
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
                                color = palette.textPrimary,
                                modifier = Modifier.widthIn(min = 150.dp, max = 190.dp)
                                    .wrapContentWidth(Alignment.CenterHorizontally).testTag("timed_live_readout")
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
                    StudioCompactActionRow(listOf(
                            StudioCompactAction("-5 $secWord", target != null) { target?.let { nativeModel.adjustSetStopwatch(it, "single", -5) } },
                            StudioCompactAction("+5 $secWord", target != null) { target?.let { nativeModel.adjustSetStopwatch(it, "single", 5) } },
                            StudioCompactAction("+15 $secWord", target != null) { target?.let { nativeModel.adjustSetStopwatch(it, "single", 15) } },
                            StudioCompactAction("+30 $secWord", target != null) { target?.let { nativeModel.adjustSetStopwatch(it, "single", 30) } }
                        ))

                    Spacer(modifier = Modifier.height(12.dp))

                    // Stopwatch Controls: Start/Pause + Reset
                    StudioAdaptiveControlRow {
                        Button(
                            onClick = { target?.let { if (isRunning) nativeModel.pauseSetStopwatch(it) else nativeModel.startSetStopwatch(it, "single") } },
                            enabled = target != null,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.primary, // 2775: theme primary like Save/Next
                                contentColor = palette.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
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
                                    tr(lang, "Indítás", "Start", "Starten", "Pornește")
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
                                .heightIn(min = 44.dp)
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
                            .heightIn(min = 44.dp)
                            .testTag("record_timed_stopper")
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
        }

        // Target Preset selector
        if (!intervalActive) item {
            val lang = uiState.language
            val secWord = tr(lang, "mp", "s", "Sek.", "sec")
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.glassTile(palette.surfaceCard, elevated = true),
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
                                    .trainPilotClickable(RoundedCornerShape(10.dp)) { targetSec = sec }
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
        if (!intervalActive) item {
            val lang = uiState.language
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = palette.glassTile(palette.surfaceCard, elevated = true),
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
            shape = RoundedCornerShape(18.dp),
            color = palette.glassCardFill(palette.background), // 2776 (A): darker dock; 2810/3: glass bar in Liquid Glass
            modifier = Modifier
                .fillMaxWidth()
                .activeWorkoutDockFrame(palette.background, palette.primary, 18.dp, glass = palette.isLiquidGlass)
                .trainPilotGlassMaterial(palette, RoundedCornerShape(18.dp))
        ) {
            val lang = uiState.language
            // 2772: ‹ Previous · ⏹ Finish · Next › (Finish = save & finish / discard / keep training).
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                com.example.ui.components.StudioRestTimerDock(uiState, restTimerLargeMode,
                    onLarge = { restTimerLargeMode = it }, onAdjust = onAdjustRestTimer,
                    onToggle = onToggleRestRunning, onSkip = onSkipRest, skipLabel = strings.skipAction,
                    // Timed sets: the rest appears once it runs or was changed, not as an idle 90 s before the first set.
                    showIdle = false)
                com.example.ui.components.ActiveWorkoutBottomBar(
                    language = uiState.language,
                    enabled = uiState.phoneControlsWorkout && !uiState.watchControlPending && !uiState.isSavingWorkout,
                    hasNext = currentIdx + 1 < totalCount,
                    completedSets = uiState.activeSetsByExerciseId.values.sumOf { sets -> sets.count { it.completed } },
                    totalSets = uiState.activeSetsByExerciseId.values.sumOf { it.size },
                    elapsedLabel = "",
                    tagPrefix = "timed",
                    onPrevious = onPreviousExercise,
                    onNext = onNextExercise,
                    onFinish = onFinishWorkout,
                    onDiscard = onDiscardWorkout
                )
            }
        }
    }
}
