package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.heightIn
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.localization.LocalTrainPilotStrings
import com.example.localization.TrainPilotStrings
import com.example.model.HomeDemoMode
import com.example.model.MainTab
import com.example.model.SubScreen
import androidx.compose.runtime.CompositionLocalProvider
import com.example.ui.components.ExerciseYouTubeVideoModalDialog
import com.example.ui.components.GoalsEditDialog
import com.example.ui.components.LocalOpenExerciseVideo
import com.example.ui.components.PalettePickerBottomSheet
import com.example.ui.components.PersonalPlannerModalDialog
import com.example.ui.components.QuickWorkoutSheet
import com.example.ui.components.TrainPilotGlobalTopHeaderAndNav
import com.example.ui.components.TrainPilotRedCloseButton
import com.example.ui.screens.ActiveBilateralWorkoutScreen
import com.example.ui.screens.ActiveOutdoorGpsScreen
import com.example.ui.screens.ActiveStrengthWorkoutScreen
import com.example.ui.screens.ActiveTimedWorkoutScreen
import com.example.ui.screens.AddCustomExerciseModalDialog
import com.example.ui.screens.BackupRestoreScreen
import com.example.ui.screens.CalendarPlannerScreen
import com.example.ui.screens.CoachDetailScreen
import com.example.ui.screens.ExerciseDetailScreen
import com.example.ui.screens.ExerciseLibraryScreen
import com.example.ui.screens.HealthTabScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.IntervalSettingsScreen
import com.example.ui.screens.LogRouteMapScreen
import com.example.ui.screens.LogTabScreen
import com.example.ui.screens.LogWorkoutDetailScreen
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.PrivatePhotosScreen
import com.example.ui.screens.ProgramEditorScreen
import com.example.ui.screens.ProgramsListScreen
import com.example.ui.screens.ProgressTabScreen
import com.example.ui.screens.SettingsMainScreen
import com.example.ui.screens.WatchAndSyncScreen
import com.example.ui.screens.WorkoutSummaryScreen
import com.example.ui.screens.WorkoutTabScreen
import com.example.ui.theme.LocalTrainPilotPalette
import com.example.ui.theme.TrainPilotPalettes
import com.example.ui.theme.TrainPilotTheme
import com.example.ui.viewmodel.TrainPilotViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val viewModel: TrainPilotViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val currentPalette = TrainPilotPalettes.getById(uiState.selectedPaletteId)

            TrainPilotTheme(
                palette = currentPalette,
                fontScaleMultiplier = uiState.fontScaleMultiplier
            ) {
                TrainPilotAppRoot(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun TrainPilotAppRoot(viewModel: TrainPilotViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = LocalTrainPilotPalette.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val strings = TrainPilotStrings.forLanguage(uiState.language)

    val canNavigateBack = uiState.currentSubScreen != SubScreen.NONE ||
        uiState.currentTab != MainTab.HOME ||
        uiState.backStack.isNotEmpty()

    BackHandler(enabled = canNavigateBack) {
        viewModel.navigateBack()
    }

    LaunchedEffect(uiState.statusBannerMessage) {
        if (uiState.statusBannerMessage != null) {
            delay(3200)
            viewModel.clearBannerMessage()
        }
    }

    CompositionLocalProvider(
        LocalTrainPilotStrings provides strings,
        LocalOpenExerciseVideo provides { exTitle -> viewModel.openExerciseVideoModal(exTitle) }
    ) {
        // 0. Embedded YouTube Exercise Video Modal Dialog
        uiState.videoModalExerciseTitle?.let { exTitle ->
            ExerciseYouTubeVideoModalDialog(
                exerciseTitle = exTitle,
                onDismiss = { viewModel.closeExerciseVideoModal() }
            )
        }

        // 1. 32 Color Palette Picker Bottom Sheet
        if (uiState.showPalettePickerSheet) {
            PalettePickerBottomSheet(
                selectedPaletteId = uiState.selectedPaletteId,
                selectedCategory = uiState.paletteFilterCategory,
                currentLanguage = uiState.language,
                fontScaleMultiplier = uiState.fontScaleMultiplier,
                compactPhoneMode = uiState.compactPhoneMode,
                onSelectPalette = { viewModel.selectPalette(it) },
                onSelectCategory = { viewModel.setPaletteFilterCategory(it) },
                onSelectLanguage = { viewModel.selectLanguage(it) },
                onSelectFontScale = { viewModel.setFontScaleMultiplier(it) },
                onToggleCompactMode = { viewModel.toggleCompactPhoneMode(it) },
                onDismiss = { viewModel.togglePalettePickerSheet(false) }
            )
        }

        // 2. Goals Edit Dialog (Célok beállítása)
        if (uiState.showGoalsDialog) {
            GoalsEditDialog(
                currentWeeklyWorkouts = uiState.weeklyGoalTarget,
                currentStepsGoal = uiState.stepsGoal,
                onSave = { weekly, steps -> viewModel.updateGoals(weekly, steps) },
                onDismiss = { viewModel.toggleGoalsDialog(false) }
            )
        }

        // 3. Personal Planner Modal Dialog (Kezdőprofil és egyéni terv A01)
        if (uiState.showPersonalPlannerModal) {
            PersonalPlannerModalDialog(
                initialHeightCm = uiState.userHeightCm,
                initialWeightKg = uiState.userWeightKg,
                initialGoalHu = uiState.userFitnessGoalHu,
                initialExperienceHu = uiState.userExperienceLevelHu,
                initialLocationHu = uiState.userWorkoutLocationHu,
                initialEquipmentHu = uiState.userSelectedEquipmentHu,
                initialDaysPerWeek = uiState.userScheduleDaysPerWeek,
                initialDurationMin = uiState.userWorkoutDurationMin,
                initialExcludedMuscles = uiState.userExcludedMusclesHu,
                initialExcludedExercises = uiState.userExcludedExercisesHu,
                onSaveAndOpenPlan = { h, w, goal, exp, loc, eq, days, dur, exMuscles, exIds ->
                    viewModel.saveInitialProfileAndPlan(
                        heightCm = h,
                        weightKg = w,
                        goalHu = goal,
                        experienceHu = exp,
                        locationHu = loc,
                        equipmentHu = eq,
                        daysPerWeek = days,
                        durationMin = dur,
                        excludedMuscles = exMuscles,
                        excludedExercises = exIds
                    )
                },
                onDismiss = { viewModel.togglePersonalPlannerModal(false) }
            )
        }

        // 3b. Global Workout Settings & Progression Modal (Edzés beállításai) when opened outside SettingsMainScreen
        if (uiState.showProgressionModal && uiState.currentSubScreen != SubScreen.SETTINGS_MAIN) {
            var selectedStyle by androidx.compose.runtime.remember(uiState.trainingStyleHu) {
                androidx.compose.runtime.mutableStateOf(uiState.trainingStyleHu)
            }
            var selectedRule by androidx.compose.runtime.remember(uiState.progressionRuleHu) {
                androidx.compose.runtime.mutableStateOf(uiState.progressionRuleHu)
            }
            var gentleMode by androidx.compose.runtime.remember(uiState.beginnerGentleProfileActive) {
                androidx.compose.runtime.mutableStateOf(uiState.beginnerGentleProfileActive)
            }
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { viewModel.toggleProgressionModal(false) },
                containerColor = palette.surface,
                titleContentColor = palette.textPrimary,
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Edzés beállításai",
                            style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        TrainPilotRedCloseButton(
                            onClick = { viewModel.toggleProgressionModal(false) }
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = palette.surfaceElevated,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, palette.cardBorder, RoundedCornerShape(12.dp))
                                .clickable { viewModel.cycleDefaultRestTimer() }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Alapértelmezett pihenőidő", color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                                Text("${uiState.defaultRestSec} mp", color = palette.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = palette.surfaceElevated,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, palette.cardBorder, RoundedCornerShape(12.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Hangjelzés pihenőidő végén", color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                                androidx.compose.material3.Switch(
                                    checked = uiState.soundAlertEnabled,
                                    onCheckedChange = { viewModel.toggleSoundAlert(it) }
                                )
                            }
                        }
                        Text("Edzésstílus:", fontWeight = FontWeight.Bold, color = palette.textPrimary)
                        listOf("Erő & Hipertrófia", "Funkcionális & Állóképesség", "Kezdő kímélő (Ízületbarát)").forEach { st ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedStyle = st }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                androidx.compose.material3.RadioButton(selected = selectedStyle == st, onClick = { selectedStyle = st })
                                Text(st, color = palette.textPrimary)
                            }
                        }
                        Text("Progressziós szabály:", fontWeight = FontWeight.Bold, color = palette.textPrimary)
                        listOf("Dupla progresszió (8-12 ism. -> +2.5 kg)", "Lineáris (+1.25 kg hetente)", "RIR-alapú (1-2 RIR megtartása)").forEach { rl ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedRule = rl }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                androidx.compose.material3.RadioButton(selected = selectedRule == rl, onClick = { selectedRule = rl })
                                Text(rl, color = palette.textPrimary)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Kezdő kímélő profil", color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                            androidx.compose.material3.Switch(checked = gentleMode, onCheckedChange = { gentleMode = it })
                        }
                    }
                },
                confirmButton = {
                    androidx.compose.material3.Button(
                        onClick = { viewModel.updateProgressionSettings(selectedStyle, selectedRule, gentleMode) },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = palette.primary,
                            contentColor = palette.onPrimary
                        )
                    ) {
                        Text(strings.saveBtn, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { viewModel.toggleProgressionModal(false) }) {
                        Text(strings.cancelBtn, color = palette.textSecondary)
                    }
                }
            )
        }

        // 4. Quick Workout Sheet (Gyors edzés indítása)
        if (uiState.showQuickWorkoutSheet) {
            QuickWorkoutSheet(
                onStartStrengthQuick = {
                    viewModel.toggleQuickWorkoutSheet(false)
                    viewModel.navigateToSubScreen(SubScreen.ACTIVE_STRENGTH, MainTab.WORKOUT)
                },
                onStartTimedQuick = {
                    viewModel.toggleQuickWorkoutSheet(false)
                    viewModel.navigateToSubScreen(SubScreen.ACTIVE_TIMED, MainTab.WORKOUT)
                },
                onStartOutdoorGps = {
                    viewModel.toggleQuickWorkoutSheet(false)
                    viewModel.navigateToSubScreen(SubScreen.ACTIVE_OUTDOOR_GPS, MainTab.WORKOUT)
                },
                onBrowseExercises = {
                    viewModel.toggleQuickWorkoutSheet(false)
                    viewModel.navigateToSubScreen(SubScreen.EXERCISE_LIBRARY, MainTab.WORKOUT)
                },
                onDismiss = { viewModel.toggleQuickWorkoutSheet(false) }
            )
        }

        // 5. Add Custom Exercise Modal Dialog (Új egyedi gyakorlat pl. Kör futás)
        if (uiState.showAddCustomExerciseDialog) {
            AddCustomExerciseModalDialog(
                onDismiss = { viewModel.toggleAddCustomExerciseDialog(false) },
                onSaveCustomExercise = { nameHu, category, equipmentHu, descriptionHu, videoUrl, isTimed, addToCurrentProgramDay ->
                    viewModel.addCustomExercise(
                        nameHu = nameHu,
                        category = category,
                        equipmentHu = equipmentHu,
                        descriptionHu = descriptionHu,
                        videoUrl = videoUrl,
                        isTimed = isTimed,
                        addToCurrentProgramDay = addToCurrentProgramDay
                    )
                }
            )
        }

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background),
            containerColor = palette.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TrainPilotGlobalTopHeaderAndNav(
                    currentTab = uiState.currentTab,
                    currentSubScreen = uiState.currentSubScreen,
                    currentLanguage = uiState.language,
                    selectedPaletteId = uiState.selectedPaletteId,
                    strings = strings,
                    onSelectTab = { tab -> viewModel.selectTab(tab) },
                    onNavigateToSubScreen = { sub, tab -> viewModel.navigateToSubScreen(sub, tab) },
                    onSelectLanguage = { lang -> viewModel.selectLanguage(lang) },
                    onOpenPalettePicker = { viewModel.togglePalettePickerSheet(true) },
                    onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) }
                )
            }
        ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background)
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            val contentMaxWidth = if (uiState.compactPhoneMode) 360.dp else 600.dp

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = contentMaxWidth)
            ) {
                when (uiState.currentSubScreen) {
                    SubScreen.PROGRAMS_LIST -> {
                        ProgramsListScreen(
                            state = uiState,
                            onBack = { viewModel.navigateBack() },
                            onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                            onToggleCustomTab = { viewModel.setProgramsTabCustomOnly(it) },
                            onActivateProgram = { viewModel.activateProgram(it) },
                            onOpenProgramEditor = { progId -> viewModel.openProgramEditor(progId, 0) },
                            onOpenExerciseLibrary = { viewModel.navigateToSubScreen(SubScreen.EXERCISE_LIBRARY) },
                            onOpenPersonalPlanner = { viewModel.togglePersonalPlannerModal(true) },
                            onMoveProgramOrder = { fromIdx, toIdx -> viewModel.moveProgramOrder(fromIdx, toIdx) },
                            onCreateCustomProgram = { name, sub -> viewModel.createNewCustomProgram(name, sub) },
                            onOpenAddCustomExercise = { viewModel.toggleAddCustomExerciseDialog(true) }
                        )
                    }

                    SubScreen.PROGRAM_EDITOR -> {
                        ProgramEditorScreen(
                            state = uiState,
                            onBack = { viewModel.navigateBack() },
                            onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                            onSelectDayIndex = { viewModel.selectEditingDayIndex(it) },
                            onToggleExpandExercise = { viewModel.toggleExpandedEditorExercise(it) },
                            onSaveExercisePrescription = { exId, sets, reps, kg, rest ->
                                viewModel.updateProgramExercisePrescription(exId, sets, reps, kg, rest)
                            },
                            onOpenExerciseDetail = { exId -> viewModel.openExerciseDetail(exId) },
                            onOpenExerciseLibrary = { viewModel.navigateToSubScreen(SubScreen.EXERCISE_LIBRARY) },
                            onMoveExercise = { fromIdx, toIdx -> viewModel.moveEditorExercise(fromIdx, toIdx) },
                            onOpenAddCustomExercise = { viewModel.toggleAddCustomExerciseDialog(true) }
                        )
                    }

                    SubScreen.CALENDAR_PLANNER -> {
                        CalendarPlannerScreen(
                            state = uiState,
                            onBack = { viewModel.navigateBack() },
                            onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                            onSelectDay = { viewModel.selectCalendarDay(it) },
                            onOpenReplanModal = { viewModel.toggleReplanModal(it) },
                            onApplyReplan = { dateStr, firstDay ->
                                viewModel.applyCalendarReplan(dateStr, firstDay)
                            },
                            onOpenWorkoutDetail = {
                                viewModel.openLogWorkoutDetail("log_4")
                            },
                            onUpdatePlanningParams = { rhythm, firstDay, startDate, time, dur, weeks ->
                                viewModel.updatePlanningParameters(rhythm, firstDay, startDate, time, dur, weeks)
                            },
                            onPlanMonthlyWorkouts = { viewModel.planMonthlyWorkouts() },
                            onModifyCalendarEvent = { id, title, split, time, dur ->
                                viewModel.modifyCalendarEvent(id, title, split, time, dur)
                            },
                            onMoveCalendarEvent = { id, targetDay, cascade ->
                                viewModel.moveCalendarEvent(id, targetDay, cascade)
                            },
                            onDeleteCalendarEvent = { id -> viewModel.deleteCalendarEvent(id) },
                            onAddPlannedWorkoutOnDay = { day, split, time, dur ->
                                viewModel.addPlannedWorkoutOnDay(day, split, time, dur)
                            },
                            onUndoCalendarAction = { viewModel.undoCalendarAction() },
                            onActivateProgram = { id -> viewModel.activateProgram(id) }
                        )
                    }

                    SubScreen.EXERCISE_LIBRARY -> {
                        ExerciseLibraryScreen(
                            state = uiState,
                            onBack = { viewModel.navigateBack() },
                            onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                            onSearchQueryChange = { viewModel.updateExerciseSearch(it) },
                            onSelectCategory = { viewModel.selectExerciseCategory(it) },
                            onSelectExercise = { exId -> viewModel.openExerciseDetail(exId) },
                            onOpenAddCustomExercise = { viewModel.toggleAddCustomExerciseDialog(true) }
                        )
                    }

                    SubScreen.EXERCISE_DETAIL -> {
                        ExerciseDetailScreen(
                            state = uiState,
                            onBack = { viewModel.navigateBack() },
                            onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                            onAddExerciseToWorkout = { ex ->
                                viewModel.addExerciseToActiveProgramDay(ex)
                            },
                            onStartDemoWithExercise = {
                                viewModel.navigateToSubScreen(SubScreen.ACTIVE_STRENGTH, MainTab.WORKOUT)
                            }
                        )
                    }

                    SubScreen.ACTIVE_STRENGTH -> {
                        ActiveStrengthWorkoutScreen(
                            uiState = uiState,
                            strings = strings,
                            onBack = { viewModel.navigateBack() },
                            onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                            onSelectMeasurementMode = { mode -> viewModel.navigateToSubScreen(mode, MainTab.WORKOUT) },
                            onToggleSetCompleted = { setNum -> viewModel.toggleSetCompleted(setNum) },
                            onAdjustSetValues = { setNum, dKg, dReps, dRir ->
                                viewModel.adjustSetValues(setNum, dKg, dReps, dRir)
                            },
                            onUndoLastSet = { viewModel.undoLastCompletedSet() },
                            onAdjustRestTimer = { delta -> viewModel.adjustRestTimer(delta) },
                            onToggleRestRunning = { viewModel.toggleRestTimerRunning() },
                            onSkipRest = { viewModel.skipRestTimer() },
                            onOpenExerciseDetail = { exId -> viewModel.openExerciseDetail(exId) },
                            onUpdateSetValuesExact = { setNum, kg, reps, rir ->
                                viewModel.updateSetValuesExact(setNum, kg, reps, rir)
                            },
                            onDeleteSet = { setNum -> viewModel.deleteSet(setNum) },
                            onAddSet = { viewModel.addSet() },
                            onToggleAutoMode = { enabled -> viewModel.toggleAutoMode(enabled) }
                        )
                    }

                    SubScreen.ACTIVE_TIMED -> {
                        ActiveTimedWorkoutScreen(
                            uiState = uiState,
                            strings = strings,
                            onBack = { viewModel.navigateBack() },
                            onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                            onSelectMeasurementMode = { mode -> viewModel.navigateToSubScreen(mode, MainTab.WORKOUT) }
                        )
                    }

                    SubScreen.ACTIVE_BILATERAL -> {
                        ActiveBilateralWorkoutScreen(
                            uiState = uiState,
                            strings = strings,
                            onBack = { viewModel.navigateBack() },
                            onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                            onSelectMeasurementMode = { mode -> viewModel.navigateToSubScreen(mode, MainTab.WORKOUT) }
                        )
                    }

                    SubScreen.ACTIVE_OUTDOOR_GPS -> {
                        ActiveOutdoorGpsScreen(
                            uiState = uiState,
                            strings = strings,
                            onBack = { viewModel.navigateBack() },
                            onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                            onSelectMeasurementMode = { mode -> viewModel.navigateToSubScreen(mode, MainTab.WORKOUT) }
                        )
                    }

                    SubScreen.INTERVAL_SETTINGS -> {
                        IntervalSettingsScreen(
                            uiState = uiState,
                            strings = strings,
                            onBack = { viewModel.navigateBack() },
                            onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                            onSelectMeasurementMode = { mode -> viewModel.navigateToSubScreen(mode, MainTab.WORKOUT) },
                            onSaveAndStartIntervals = { work, rest, rounds, sound, vib, auto ->
                                viewModel.updateIntervalSettings(work, rest, rounds, sound, vib, auto)
                            }
                        )
                    }

                    SubScreen.WORKOUT_SUMMARY -> {
                        WorkoutSummaryScreen(
                            uiState = uiState,
                            strings = strings,
                            onBack = { viewModel.navigateBack() },
                            onSelectMood = { viewModel.selectWorkoutSummaryMood(it) },
                            onUpdateNote = { viewModel.updateWorkoutSummaryNote(it) },
                            onSaveWorkout = { viewModel.finishAndSaveWorkout() },
                            onPauseAndExit = {
                                viewModel.cycleHomeDemoMode(HomeDemoMode.PAUSED_WORKOUT)
                                viewModel.selectTab(MainTab.HOME)
                            },
                            onDiscardWorkout = {
                                viewModel.showBannerMessage("Edzés elvetve")
                                viewModel.selectTab(MainTab.HOME)
                            }
                        )
                    }

                    SubScreen.LOG_WORKOUT_DETAIL -> {
                        LogWorkoutDetailScreen(
                            uiState = uiState,
                            strings = strings,
                            onBack = { viewModel.navigateBack() },
                            onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                            onSelectDetailTab = { viewModel.selectLogDetailTab(it) },
                            onToggleExpandedExercise = { viewModel.toggleExpandedLogExercise(it) },
                            onOpenPhotos = { viewModel.navigateToSubScreen(SubScreen.PRIVATE_PHOTOS) },
                            onOpenRouteMap = { viewModel.navigateToSubScreen(SubScreen.LOG_ROUTE_MAP) },
                            onOpenWatchSync = { viewModel.navigateToSubScreen(SubScreen.WATCH_AND_SYNC) },
                            onOpenHealthTab = { viewModel.selectTab(MainTab.HEALTH) }
                        )
                    }

                    SubScreen.LOG_ROUTE_MAP -> {
                        LogRouteMapScreen(
                            uiState = uiState,
                            strings = strings,
                            onBack = { viewModel.navigateBack() }
                        )
                    }

                    SubScreen.COACH_DETAIL -> {
                        CoachDetailScreen(
                            uiState = uiState,
                            strings = strings,
                            onBack = { viewModel.navigateBack() },
                            onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                            onStartSuggestedWorkout = {
                                viewModel.navigateToSubScreen(SubScreen.ACTIVE_STRENGTH, MainTab.WORKOUT)
                            },
                            onOpenExerciseDetail = { exId -> viewModel.openExerciseDetail(exId) }
                        )
                    }

                    SubScreen.SETTINGS_MAIN -> {
                        SettingsMainScreen(
                            uiState = uiState,
                            strings = strings,
                            onBack = { viewModel.navigateBack() },
                            onOpenPalettePicker = { viewModel.togglePalettePickerSheet(true) },
                            onSelectPalette = { viewModel.selectPalette(it) },
                            onSelectLanguage = { viewModel.selectLanguage(it) },
                            onCycleRestTimer = { viewModel.cycleDefaultRestTimer() },
                            onToggleSoundAlert = { viewModel.toggleSoundAlert(it) },
                            onToggleProgressionModal = { viewModel.toggleProgressionModal(it) },
                            onSaveProgression = { style, rule, gentle ->
                                viewModel.updateProgressionSettings(style, rule, gentle)
                            },
                            onNavigateToSubScreen = { viewModel.navigateToSubScreen(it) },
                            onShowBanner = { viewModel.showBannerMessage(it) }
                        )
                    }

                    SubScreen.WATCH_AND_SYNC -> {
                        WatchAndSyncScreen(
                            uiState = uiState,
                            strings = strings,
                            onBack = { viewModel.navigateBack() },
                            onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                            onToggleControlMode = { viewModel.toggleWatchControlMode() },
                            onShowBanner = { viewModel.showBannerMessage(it) }
                        )
                    }

                    SubScreen.BACKUP_RESTORE -> {
                        BackupRestoreScreen(
                            uiState = uiState,
                            strings = strings,
                            onBack = { viewModel.navigateBack() },
                            onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                            onSelectFormat = { viewModel.selectBackupFormat(it) },
                            onToggleIncludeHealth = { viewModel.toggleIncludeHealthInBackup(it) },
                            onShowBanner = { viewModel.showBannerMessage(it) },
                            onExportBackup = { uri ->
                                viewModel.exportBackup(context, uri, uiState.selectedBackupFormat)
                            },
                            onImportBackup = { uri ->
                                viewModel.importBackup(context, uri)
                            }
                        )
                    }

                    SubScreen.PERMISSIONS_SCREEN -> {
                        PermissionsScreen(
                            uiState = uiState,
                            strings = strings,
                            onBack = { viewModel.navigateBack() },
                            onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                            onTogglePermission = { viewModel.togglePermission(it) }
                        )
                    }

                    SubScreen.PRIVATE_PHOTOS -> {
                        PrivatePhotosScreen(
                            uiState = uiState,
                            strings = strings,
                            onBack = { viewModel.navigateBack() },
                            onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                            onShowBanner = { viewModel.showBannerMessage(it) }
                        )
                    }

                    SubScreen.NONE -> {
                        when (uiState.currentTab) {
                            MainTab.HOME -> {
                                HomeScreen(
                                    state = uiState,
                                    strings = strings,
                                    onOpenPalettePicker = { viewModel.togglePalettePickerSheet(true) },
                                    onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                                    onCycleDemoMode = { mode -> viewModel.cycleHomeDemoMode(mode) },
                                    onStartOrContinueWorkout = {
                                        viewModel.navigateToSubScreen(SubScreen.ACTIVE_STRENGTH, MainTab.WORKOUT)
                                    },
                                    onOpenProgramDayDetails = {
                                        viewModel.openProgramEditor("home_ab_basic", 0)
                                    },
                                    onOpenQuickWorkout = { viewModel.toggleQuickWorkoutSheet(true) },
                                    onOpenWatchAndSync = { viewModel.navigateToSubScreen(SubScreen.WATCH_AND_SYNC) },
                                    onOpenHealthSleep = {
                                        viewModel.openHealthAndExpand(journal = true)
                                    },
                                    onOpenHealthHeart = {
                                        viewModel.openHealthAndExpand(pulse = true)
                                    },
                                    onOpenHealthSteps = {
                                        viewModel.openHealthAndExpand(journal = true)
                                    },
                                    onOpenCoachReadiness = {
                                        viewModel.navigateToSubScreen(SubScreen.COACH_DETAIL)
                                    },
                                    onOpenProgramsList = {
                                        viewModel.navigateToSubScreen(SubScreen.PROGRAMS_LIST, MainTab.WORKOUT)
                                    },
                                    onOpenCalendarPlanner = {
                                        viewModel.navigateToSubScreen(SubScreen.CALENDAR_PLANNER)
                                    },
                                    onOpenGoalsDialog = { viewModel.toggleGoalsDialog(true) },
                                    onOpenPersonalPlanner = { viewModel.togglePersonalPlannerModal(true) }
                                )
                            }

                            MainTab.PROGRAMS -> {
                                ProgramsListScreen(
                                    state = uiState,
                                    onBack = { viewModel.selectTab(MainTab.HOME) },
                                    onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                                    onToggleCustomTab = { viewModel.setProgramsTabCustomOnly(it) },
                                    onActivateProgram = { viewModel.activateProgram(it) },
                                    onOpenProgramEditor = { progId -> viewModel.openProgramEditor(progId, 0) },
                                    onOpenExerciseLibrary = { viewModel.navigateToSubScreen(SubScreen.EXERCISE_LIBRARY) },
                                    onOpenPersonalPlanner = { viewModel.togglePersonalPlannerModal(true) },
                                    onMoveProgramOrder = { fromIdx, toIdx -> viewModel.moveProgramOrder(fromIdx, toIdx) },
                                    onCreateCustomProgram = { name, sub -> viewModel.createNewCustomProgram(name, sub) },
                                    onOpenAddCustomExercise = { viewModel.toggleAddCustomExerciseDialog(true) }
                                )
                            }

                            MainTab.WORKOUT -> {
                                WorkoutTabScreen(
                                    state = uiState,
                                    onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                                    onOpenQuickWorkout = { viewModel.toggleQuickWorkoutSheet(true) },
                                    onOpenExerciseLibrary = { viewModel.navigateToSubScreen(SubScreen.EXERCISE_LIBRARY) },
                                    onOpenOutdoorWorkout = { viewModel.navigateToSubScreen(SubScreen.ACTIVE_OUTDOOR_GPS) },
                                    onOpenProgramsList = { viewModel.navigateToSubScreen(SubScreen.PROGRAMS_LIST) },
                                    onOpenProgramEditor = { progId, dayIdx -> viewModel.openProgramEditor(progId, dayIdx) },
                                    onStartWorkoutDay = { _ ->
                                        viewModel.navigateToSubScreen(SubScreen.ACTIVE_STRENGTH)
                                    },
                                    onOpenCalendar = { viewModel.selectTab(MainTab.CALENDAR) },
                                    onOpenCoach = { viewModel.selectTab(MainTab.COACH) },
                                    onOpenProgressionModal = { viewModel.toggleProgressionModal(true) }
                                )
                            }

                            MainTab.CALENDAR -> {
                                CalendarPlannerScreen(
                                    state = uiState,
                                    onBack = { viewModel.selectTab(MainTab.HOME) },
                                    onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                                    onSelectDay = { viewModel.selectCalendarDay(it) },
                                    onOpenReplanModal = { viewModel.toggleReplanModal(it) },
                                    onApplyReplan = { dateStr, firstDay ->
                                        viewModel.applyCalendarReplan(dateStr, firstDay)
                                    },
                                    onOpenWorkoutDetail = {
                                        viewModel.openLogWorkoutDetail("log_4")
                                    },
                                    onUpdatePlanningParams = { rhythm, firstDay, startDate, time, dur, weeks ->
                                        viewModel.updatePlanningParameters(rhythm, firstDay, startDate, time, dur, weeks)
                                    },
                                    onPlanMonthlyWorkouts = { viewModel.planMonthlyWorkouts() },
                                    onModifyCalendarEvent = { id, title, split, time, dur ->
                                        viewModel.modifyCalendarEvent(id, title, split, time, dur)
                                    },
                                    onMoveCalendarEvent = { id, targetDay, cascade ->
                                        viewModel.moveCalendarEvent(id, targetDay, cascade)
                                    },
                                    onDeleteCalendarEvent = { id -> viewModel.deleteCalendarEvent(id) },
                                    onAddPlannedWorkoutOnDay = { day, split, time, dur ->
                                        viewModel.addPlannedWorkoutOnDay(day, split, time, dur)
                                    },
                                    onUndoCalendarAction = { viewModel.undoCalendarAction() },
                                    onActivateProgram = { id -> viewModel.activateProgram(id) }
                                )
                            }

                            MainTab.COACH -> {
                                CoachDetailScreen(
                                    uiState = uiState,
                                    strings = strings,
                                    onBack = { viewModel.selectTab(MainTab.HOME) },
                                    onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                                    onStartSuggestedWorkout = {
                                        viewModel.navigateToSubScreen(SubScreen.ACTIVE_STRENGTH, MainTab.WORKOUT)
                                    },
                                    onOpenExerciseDetail = { exId -> viewModel.openExerciseDetail(exId) }
                                )
                            }

                            MainTab.LOG -> {
                                ProgressTabScreen(
                                    uiState = uiState.copy(progressMainSection = 2),
                                    strings = strings,
                                    onSelectMainSection = { viewModel.selectProgressMainSection(it) },
                                    onSelectExercise = { viewModel.selectProgressExercise(it) },
                                    onSelectRange = { viewModel.selectProgressRange(it) },
                                    onSelectVolumeBar = { viewModel.selectVolumeBar(it) },
                                    onSelectExpandedStatsCard = { viewModel.selectExpandedStatsCard(it) },
                                    onSelectStatsSubTab = { viewModel.selectStatsSubTab(it) },
                                    onToggleCardioFilterStepsMode = { viewModel.toggleCardioFilterStepsMode(it) },
                                    onOpenWorkoutDetail = { id -> viewModel.openLogWorkoutDetail(id) },
                                    onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                                    onOpenPalettePicker = { viewModel.togglePalettePickerSheet(true) },
                                    onSearchChange = { viewModel.updateLogSearch(it) },
                                    onOpenRouteMap = { viewModel.navigateToSubScreen(SubScreen.LOG_ROUTE_MAP) }
                                )
                            }

                            MainTab.PROGRESS -> {
                                ProgressTabScreen(
                                    uiState = uiState,
                                    strings = strings,
                                    onSelectMainSection = { viewModel.selectProgressMainSection(it) },
                                    onSelectExercise = { viewModel.selectProgressExercise(it) },
                                    onSelectRange = { viewModel.selectProgressRange(it) },
                                    onSelectVolumeBar = { viewModel.selectVolumeBar(it) },
                                    onSelectExpandedStatsCard = { viewModel.selectExpandedStatsCard(it) },
                                    onSelectStatsSubTab = { viewModel.selectStatsSubTab(it) },
                                    onToggleCardioFilterStepsMode = { viewModel.toggleCardioFilterStepsMode(it) },
                                    onOpenWorkoutDetail = { id -> viewModel.openLogWorkoutDetail(id) },
                                    onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                                    onOpenPalettePicker = { viewModel.togglePalettePickerSheet(true) },
                                    onSearchChange = { viewModel.updateLogSearch(it) },
                                    onOpenRouteMap = { viewModel.navigateToSubScreen(SubScreen.LOG_ROUTE_MAP) }
                                )
                            }

                            MainTab.HEALTH -> {
                                HealthTabScreen(
                                    uiState = uiState,
                                    strings = strings,
                                    onToggleHealthSection = { viewModel.toggleHealthSection(it) },
                                    onSelectWeightRange = { viewModel.selectWeightRange(it) },
                                    onToggleAddWeightDialog = { viewModel.toggleAddWeightDialog(it) },
                                    onSaveNewWeight = { viewModel.saveNewBodyWeight(it) },
                                    onTogglePrimaryHealthSource = { viewModel.togglePrimaryHealthSource() },
                                    onTriggerHealthSync = { viewModel.triggerHealthSyncNow() },
                                    onNavigateToSubScreen = { viewModel.navigateToSubScreen(it) },
                                    onOpenPalettePicker = { viewModel.togglePalettePickerSheet(true) },
                                    onOpenGoalsDialog = { viewModel.toggleGoalsDialog(true) }
                                )
                            }
                        }
                    }
                }
            }

            // Status / Toast Banner Pill at bottom-top
            AnimatedVisibility(
                visible = uiState.statusBannerMessage != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = palette.surfaceElevated,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .border(1.5.dp, palette.primary, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { viewModel.clearBannerMessage() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = uiState.statusBannerMessage.orEmpty(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = strings.closeAction,
                            tint = palette.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
    }
}
