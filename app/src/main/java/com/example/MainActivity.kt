package com.example

import com.repforge.app.ui.theme.TrainPilotFrames

import com.repforge.app.ui.theme.trainPilotBorder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import com.repforge.app.ui.theme.trainPilotClickable
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
import androidx.compose.ui.platform.testTag
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
import com.example.ui.components.TrainPilotGlobalTopHeaderAndNav
import com.example.ui.components.TrainPilotRedCloseButton
import com.example.ui.screens.ActiveBilateralWorkoutScreen
import com.example.ui.screens.ActiveOutdoorGpsScreen
import com.example.ui.screens.ActiveStrengthWorkoutScreen
import com.example.ui.screens.ActiveTimedWorkoutScreen
import com.example.ui.screens.AddCustomExerciseModalDialog
import com.example.ui.screens.BackupRestoreScreen
import com.example.ui.screens.CalendarPlannerScreen
import com.example.ui.components.sheetHostState
import com.example.ui.screens.ProgramEditorContent
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
import com.example.ui.screens.StudioWorkoutModesDialog
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

    override fun onStop() {
        viewModel.onAppBackground()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settingsStore = com.repforge.app.data.store.TrainPilotSettingsStore(this)
        val database = com.repforge.app.data.db.TrainPilotDatabase.getInstance(this)
        viewModel.bindNativeEnvironment(
            context = applicationContext,
            settingsStore = settingsStore,
            database = database,
            scope = lifecycleScope
        )
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val currentPalette = TrainPilotPalettes.getById(uiState.selectedPaletteId)

            TrainPilotTheme(
                palette = currentPalette,
                fontScaleMultiplier = uiState.fontScaleMultiplier
            ) {
                com.repforge.app.ui.onboarding.NativeFirstLaunchGate(viewModel) {
                    TrainPilotAppRoot(viewModel = viewModel)
                }
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
    val requestPlatformPermission = com.example.ui.components.rememberStudioPermissionRequest(context, viewModel)
    val nativeHealth: com.repforge.app.ui.health.HealthDashboardViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val health by nativeHealth.uiState.collectAsStateWithLifecycle()
    val nativeSettings: com.repforge.app.ui.settings.SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val nativeSettingsState by nativeSettings.uiState.collectAsStateWithLifecycle()
    val hcBackgroundAccess = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) nativeHealth.toggleHcBackground(true)
        else viewModel.showBannerMessage(
            com.repforge.app.NativePrivacyControls.backgroundPermissionMessage(uiState.language.name)
        )
    }
    val hcWorkoutWriteAccess = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) viewModel.showBannerMessage(
            com.repforge.app.NativePrivacyControls.writePermissionMessage(uiState.language.name))
    }
    LaunchedEffect(uiState.permissionHealthConnectEnabled, uiState.permissionSamsungHealthEnabled) {
        nativeHealth.refreshBackgroundStatus()
    }
    val googleConsent by nativeSettings.googleConsent.collectAsStateWithLifecycle()
    com.repforge.app.ui.settings.NativeGoogleConsentLauncher(googleConsent, nativeSettings::takeGoogleConsent,
        nativeSettings::completeGoogleConsent, nativeSettings::failGoogleConsent)
    val cloudInstallRevision by com.repforge.app.NativeCloudMergeGuard.localInstallRevision.collectAsStateWithLifecycle()
    LaunchedEffect(cloudInstallRevision) {
        if (cloudInstallRevision > 0) viewModel.refreshAfterCloudSync(context)
    }
    val activity = context as? android.app.Activity
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner, activity) {
        val lifecycle = lifecycleOwner.lifecycle
        val observer = androidx.lifecycle.LifecycleEventObserver { _, _ ->
            if (lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) nativeHealth.appVisible(activity)
            else nativeHealth.appHidden(activity)
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) nativeHealth.appVisible(activity)
        onDispose { lifecycle.removeObserver(observer); nativeHealth.appHidden(activity) }
    }
    androidx.compose.runtime.LaunchedEffect(health.healthRefreshing, health.syncMessage, health.syncFailed, health.syncPartial) {
        viewModel.updateHealthSyncStatus(health.healthRefreshing, partial = health.syncPartial,
            failed = health.syncFailed, message = health.syncMessage)
    }
    androidx.compose.runtime.LaunchedEffect(health.refreshRevision, health.provider) {
        if (!health.healthRefreshing) viewModel.reloadHealthJournal()
    }

    var showLiveHealth by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

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
        com.example.ui.components.LocalOpenWorkoutHealth provides { showLiveHealth = true },
        com.example.ui.components.LocalRequestWorkoutHealthPermission provides { provider -> requestPlatformPermission(if (provider == com.repforge.app.HealthJournalStore.SAMSUNG) "samsung_permission" else "hc_permission") },
        com.example.ui.components.LocalSyncDailyHealth provides { provider -> requestPlatformPermission(if (provider == com.repforge.app.HealthJournalStore.SAMSUNG) "samsung" else "hc") },
        LocalOpenExerciseVideo provides { exTitle, exId ->
            val requestedId = exId?.takeIf { it.isNotBlank() }
            val nameMatches = if (requestedId == null) {
                uiState.exercises.filter { ex ->
                    listOf(ex.nameHu, ex.nameEn, ex.nameDe, ex.nameRo)
                        .any { it.equals(exTitle, ignoreCase = true) }
                }
            } else emptyList()
            val selectedExercise = if (requestedId != null) {
                uiState.exercises.firstOrNull { it.id == requestedId }
            } else nameMatches.singleOrNull()
            val details = com.example.ui.components.resolveExerciseVideoAndNotes(
                exerciseTitle = exTitle,
                exerciseId = selectedExercise?.id ?: requestedId,
                storedVideoUrl = selectedExercise?.videoUrl,
                isCustom = selectedExercise?.isCustom == true,
                selectedNotesHu = selectedExercise?.localizedSteps(uiState.language)
                    ?.joinToString(" ")
                    ?.takeIf { it.isNotBlank() },
                allowCatalogByTitle = nameMatches.size <= 1,
                language = uiState.language.code.lowercase()
            )
            val externalUrl = details.link.externalUrl
            val openedExternally = if (externalUrl != null) {
                com.example.ui.components.openExternalExerciseVideoUrl(context, externalUrl)
            } else false
            if (!openedExternally) {
                viewModel.openExerciseVideoModal(exTitle, exId)
            }
        }
    ) {
        if (showLiveHealth) com.example.ui.components.StudioLiveWorkoutHealthDialog { showLiveHealth = false }

        // 0. Fallback Exercise Video Modal Dialog (only when missing/invalid URL or external browser launch fails)
        uiState.videoModalExerciseTitle?.let { exTitle ->
            val requestedId = uiState.videoModalExerciseId
            val nameMatches = if (requestedId == null) {
                uiState.exercises.filter { ex ->
                    listOf(ex.nameHu, ex.nameEn, ex.nameDe, ex.nameRo)
                        .any { it.equals(exTitle, ignoreCase = true) }
                }
            } else emptyList()
            val selectedExercise = if (requestedId != null) {
                uiState.exercises.firstOrNull { it.id == requestedId }
            } else nameMatches.singleOrNull()
            ExerciseYouTubeVideoModalDialog(
                exerciseTitle = selectedExercise?.localizedName(uiState.language) ?: exTitle,
                exerciseId = selectedExercise?.id ?: requestedId,
                storedVideoUrl = selectedExercise?.videoUrl,
                isCustom = selectedExercise?.isCustom == true,
                allowCatalogByTitle = nameMatches.size <= 1,
                selectedNotesHu = selectedExercise?.localizedSteps(uiState.language)
                    ?.joinToString(" ")
                    ?.takeIf { it.isNotBlank() },
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
                currentWorkouts = uiState.workoutGoalTarget,
                currentPeriod = uiState.workoutGoalPeriod,
                currentStepsGoal = uiState.stepsGoal,
                onSave = { workouts, period, steps -> viewModel.updateGoals(workouts, period, steps) },
                onDismiss = { viewModel.toggleGoalsDialog(false) }
            )
        }

        // 3. Personal Planner Modal Dialog (Kezdőprofil és egyéni terv A01)
        if (uiState.showPersonalPlannerModal) {
            com.example.ui.components.StudioPersonalPlannerDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.togglePersonalPlannerModal(false) }
            )
        }

        // One shared Studio editor for Settings/global and Workout/active-program modes.
        if (uiState.showProgressionModal && uiState.currentSubScreen != SubScreen.SETTINGS_MAIN) {
            val modeScope = "program"
            StudioWorkoutModesDialog(
                state = uiState,
                scope = modeScope,
                onDismiss = { viewModel.toggleProgressionModal(false) },
                onCycleRest = { viewModel.cycleDefaultRestTimer() },
                onToggleSound = { viewModel.toggleSoundAlert(it) },
                onToggleVibration = { viewModel.toggleVibrationAlert(it) },
                onPreview = { style, progression, inherit ->
                    viewModel.previewProgressionSettings(style, progression, modeScope, inherit)
                },
                onApprove = { viewModel.approveProgressionSettings() },
                onEditPreview = { viewModel.discardProgressionPreview() },
                onUndo = { viewModel.undoProgressionSettings(modeScope) }
            )
        }

        // 4. Quick Workout Sheet (Gyors edzés indítása)
        if (uiState.showQuickWorkoutSheet) {
            // 2772: one sheet, three tabs; the exercise picker is hosted 1:1 instead of a separate page.
            val library: @Composable (Boolean) -> Unit = { timed ->
                ExerciseLibraryScreen(
                    state = uiState,
                    onBack = { viewModel.toggleQuickWorkoutSheet(false) },
                    onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                    onSearchQueryChange = { viewModel.updateExerciseSearch(it) },
                    onSelectCategory = { viewModel.selectExerciseCategory(it) },
                    onSelectExercise = { exId -> viewModel.openExerciseDetail(exId) },
                    onOpenAddCustomExercise = { viewModel.toggleAddCustomExerciseDialog(true) },
                    onStartExerciseNow = { ex -> viewModel.toggleQuickWorkoutSheet(false); viewModel.selectLibraryExercise(ex) },
                    // 2779: the quick-workout queue never edits a program day.
                    onAddExerciseToQueue = viewModel::addExerciseToWorkoutQueue,
                    onRemoveExerciseFromQueue = viewModel::removeExerciseFromWorkoutQueue,
                    onStartQueuedWorkout = { viewModel.toggleQuickWorkoutSheet(false); viewModel.startQueuedWorkout() },
                    onToggleFavoritesOnly = viewModel::toggleFavoriteExercisesOnly,
                    onToggleFavorite = viewModel::toggleExerciseFavorite,
                    embedded = true,
                    timedOnly = timed
                )
            }
            com.example.ui.components.QuickWorkoutHubSheet(
                language = uiState.language,
                tab = uiState.quickWorkoutTab,
                onSelectTab = viewModel::selectQuickWorkoutTab,
                onDismiss = { viewModel.toggleQuickWorkoutSheet(false) },
                onStartOutdoor = { activity -> viewModel.startOutdoorWorkout(activity) },
                strengthContent = { library(false) },
                timedContent = { library(true) }
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
                Column {
                TrainPilotGlobalTopHeaderAndNav(
                    currentTab = uiState.currentTab,
                    currentSubScreen = uiState.sheetHostState().currentSubScreen,
                    currentLanguage = uiState.language,
                    selectedPaletteId = uiState.selectedPaletteId,
                    strings = strings,
                    onSelectTab = { tab -> viewModel.selectTab(tab) },
                    onNavigateToSubScreen = { sub, tab -> viewModel.navigateToSubScreen(sub, tab) },
                    onSelectLanguage = { lang -> viewModel.selectLanguage(lang) },
                    onOpenPalettePicker = { viewModel.togglePalettePickerSheet(true) },
                    onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) }
                )
                    if (com.example.ui.components.isActiveMeasurementScreen(uiState.currentSubScreen)) {
                        com.example.ui.components.StudioWorkoutHandoverBar(uiState, strings) {
                            viewModel.toggleWatchControlMode(context as? android.app.Activity)
                        }
                    }
                }
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
                    .padding(4.dp)
                    .border(TrainPilotFrames.page(palette.cardBorder, palette.primary), RoundedCornerShape(24.dp))
                    .padding(1.dp)
            ) {
                val nextWithFeedback = if (com.example.ui.components.isActiveMeasurementScreen(uiState.currentSubScreen))
                    com.example.ui.components.rememberExerciseFeedbackNext(uiState,
                        onNext = viewModel::nextActiveExercise,
                        onEffort = { id, index -> viewModel.updateExerciseFeedback(id, index = index) },
                        onNote = { id, note -> viewModel.updateExerciseFeedback(id, note = note) })
                    else ({})
                // Inline program editor shared by the Programs list and the Home card: same
                // callbacks, drafts and persistence path as the retired separate editor page.
                val inlineProgramEditor: @Composable (
                    com.example.model.TrainingProgramModel,
                    androidx.compose.runtime.snapshots.SnapshotStateMap<Pair<String, String>, com.example.model.StudioProgramPrescriptionDraft>
                ) -> Unit = { program, pending ->
                    ProgramEditorContent(
                        state = uiState,
                        program = program,
                        pendingPrescriptions = pending,
                        showNameField = false,
                        onSelectDayIndex = { viewModel.selectEditingDayIndex(it) },
                        onToggleExpandExercise = { viewModel.toggleExpandedEditorExercise(it) },
                        onSaveExercisePrescription = { exId, sets, reps, kg, rest ->
                            viewModel.updateProgramExercisePrescription(exId, sets, reps, kg, rest)
                        },
                        onOpenExerciseDetail = { exId -> viewModel.openExerciseDetail(exId) },
                        onOpenExerciseLibrary = { viewModel.openExerciseLibraryForProgramDay() },
                        onMoveExercise = { fromIdx, toIdx -> viewModel.moveEditorExercise(fromIdx, toIdx) },
                        onCopyProgram = viewModel::copyEditorProgram,
                        onDeleteProgram = viewModel::deleteEditorProgram,
                        onAddDay = viewModel::addEditorDay,
                        onDeleteDay = viewModel::deleteEditorDay,
                        onRemoveExercise = viewModel::removeEditorExercise,
                        onRenameProgram = viewModel::renameEditorProgram,
                        onSaveProgram = viewModel::saveEditorProgram
                    )
                }
                // Sheet routes (#269) render over the screen they were opened from; the host keeps
                // its call site, saved state and scroll position while the sheet is open.
                val sheetRouteActive = uiState.currentSubScreen in com.example.ui.components.TrainPilotSheetRoutes
                val routeHostState = uiState.sheetHostState()
                val renderRoute: @Composable (com.example.ui.viewmodel.TrainPilotUiState) -> Unit = { uiState ->
                when (uiState.currentSubScreen) {
                    SubScreen.PROGRAMS_LIST -> {
                        ProgramsListScreen(
                            state = uiState,
                            onBack = { viewModel.navigateBack() },
                            onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                            onToggleCustomTab = { viewModel.setProgramsTabCustomOnly(it) },
                            onActivateProgram = { viewModel.activateProgram(it) },
                            onOpenProgramEditor = { progId -> viewModel.openProgramEditor(progId, 0) },
                            onCopyProgram = { progId -> viewModel.copyProgram(progId) },
                            onDeleteProgram = { progId -> viewModel.deleteProgram(progId) },
                            onOpenExerciseLibrary = { viewModel.navigateToSubScreen(SubScreen.EXERCISE_LIBRARY) },
                            onOpenPersonalPlanner = { viewModel.togglePersonalPlannerModal(true) },
                            onMoveProgramOrder = { fromIdx, toIdx -> viewModel.moveProgramOrder(fromIdx, toIdx) },
                            onCreateCustomProgram = { name, sub -> viewModel.createNewCustomProgram(name, sub) },
                            onOpenAddCustomExercise = { viewModel.toggleAddCustomExerciseDialog(true) },
                            onToggleInlineEditor = { id, host -> viewModel.toggleInlineProgramEditor(id, 0, host) },
                            onSaveInlineDrafts = viewModel::saveEditorProgram,
                            onRenameProgram = viewModel::renameProgram,
                            inlineEditor = inlineProgramEditor
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
                            onOpenExerciseLibrary = { viewModel.openExerciseLibraryForProgramDay() },
                            onMoveExercise = { fromIdx, toIdx -> viewModel.moveEditorExercise(fromIdx, toIdx) },
                            onCopyProgram = viewModel::copyEditorProgram,
                            onDeleteProgram = viewModel::deleteEditorProgram,
                            onAddDay = viewModel::addEditorDay,
                            onDeleteDay = viewModel::deleteEditorDay,
                            onRemoveExercise = viewModel::removeEditorExercise,
                            onRenameProgram = viewModel::renameEditorProgram,
                            onSaveProgram = viewModel::saveEditorProgram,
                            onOpenAddCustomExercise = { viewModel.toggleAddCustomExerciseDialog(true) }
                        )
                    }

                    SubScreen.CALENDAR_PLANNER -> {
                        CalendarPlannerScreen(
                            state = uiState,
                            onBack = { viewModel.navigateBack() },
                            onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                            onSelectDay = { viewModel.selectCalendarDay(it) },
                                    onSelectMonth = { viewModel.selectCalendarMonth(it) },
                            onOpenReplanModal = { viewModel.toggleReplanModal(it) },
                            onApplyReplan = { dateStr, firstDay, programId ->
                                viewModel.applyCalendarReplan(dateStr, firstDay, programId)
                            },
                            onOpenWorkoutDetail = { eventId ->
                                viewModel.openCalendarEventWorkoutDetail(eventId)
                            },
                            onUpdatePlanningParams = { rhythm, firstDay, startDate, time, dur, weeks ->
                                viewModel.updatePlanningParameters(rhythm, firstDay, startDate, time, dur, weeks)
                            },
                            onPlanMonthlyWorkouts = { viewModel.planMonthlyWorkouts() },
                            onRequestClearCalendar = viewModel::requestCalendarClear,
                            onConfirmClearCalendar = viewModel::clearCalendarPlans,
                            onCancelClearCalendar = viewModel::cancelCalendarClear,
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
                            onOpenAddCustomExercise = { viewModel.toggleAddCustomExerciseDialog(true) },
                            onStartExerciseNow = { ex -> viewModel.selectLibraryExercise(ex) },
                            onAddExerciseToQueue = { ex ->
                                viewModel.addExerciseToWorkoutQueue(ex)
                                viewModel.addExerciseToActiveProgramDay(ex)
                            },
                            onRemoveExerciseFromQueue = viewModel::removeExerciseFromWorkoutQueue,
                            onStartQueuedWorkout = { viewModel.startQueuedWorkout() },
                            onToggleFavoritesOnly = viewModel::toggleFavoriteExercisesOnly,
                            onToggleFavorite = viewModel::toggleExerciseFavorite
                        )
                    }

                    SubScreen.EXERCISE_DETAIL -> {
                        val currentDetailExercise = uiState.exercises.firstOrNull { it.id == uiState.selectedExerciseId }
                            ?: uiState.exercises.firstOrNull()
                        ExerciseDetailScreen(
                            state = uiState,
                            onToggleFavorite = viewModel::toggleExerciseFavorite,
                            customExerciseActions = { currentDetailExercise?.takeIf { it.isCustom }?.let { ex ->
                                com.example.ui.components.StudioCustomExerciseActions(ex.id, uiState.language.name.lowercase(),
                                    beforeWrite = viewModel::awaitStudioProgramWrites,
                                    onChanged = { deleted ->
                                        if (deleted) viewModel.navigateBack()
                                        viewModel.refreshAfterCloudSync(context)
                                    })
                            } },
                            onBack = { viewModel.navigateBack() },
                            onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                            onAddExerciseToWorkout = { ex ->
                                viewModel.addExerciseToWorkoutQueue(ex)
                                viewModel.addExerciseToActiveProgramDay(ex)
                            },
                            onStartDemoWithExercise = {
                                if (currentDetailExercise != null) viewModel.selectLibraryExercise(currentDetailExercise)
                                else viewModel.startPlannedWorkout()
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
                            onToggleAutoMode = { enabled -> viewModel.toggleAutoMode(enabled) },
                            onPreviousExercise = { viewModel.previousActiveExercise() },
                            onNextExercise = nextWithFeedback,
                            onFinishWorkout = { viewModel.navigateToSubScreen(SubScreen.WORKOUT_SUMMARY) },
                            onDiscardWorkout = { viewModel.stopActiveWorkoutSession(uiState.activeWorkoutId, uiState.activeWorkoutGeneration) },
                            exerciseFeedback = { com.example.ui.components.StudioActiveExerciseFeedback(uiState,
                                onEffort = { id, index -> viewModel.updateExerciseFeedback(id, index = index) },
                                onNote = { id, note -> viewModel.updateExerciseFeedback(id, note = note) }) },
                            onSwitchExercise = { ex -> viewModel.switchCurrentActiveExercise(ex) },
                            onOpenExerciseLibrary = { viewModel.openActiveExerciseLibrary() }
                        )
                    }

                    SubScreen.ACTIVE_TIMED -> {
                        ActiveTimedWorkoutScreen(
                            uiState = uiState,
                            consumeIntervalStart = viewModel::consumeIntervalStart,
                            strings = strings,
                            onBack = { viewModel.navigateBack() },
                            onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                            onSelectMeasurementMode = { mode -> viewModel.navigateToSubScreen(mode, MainTab.WORKOUT) },
                            onPreviousExercise = { viewModel.previousActiveExercise() },
                            onNextExercise = nextWithFeedback,
                            onFinishWorkout = { viewModel.navigateToSubScreen(SubScreen.WORKOUT_SUMMARY) },
                            onDiscardWorkout = { viewModel.stopActiveWorkoutSession(uiState.activeWorkoutId, uiState.activeWorkoutGeneration) },
                            exerciseFeedback = { com.example.ui.components.StudioActiveExerciseFeedback(uiState,
                                onEffort = { id, index -> viewModel.updateExerciseFeedback(id, index = index) },
                                onNote = { id, note -> viewModel.updateExerciseFeedback(id, note = note) }) },
                            onSwitchExercise = { ex -> viewModel.switchCurrentActiveExercise(ex) },
                            onOpenExerciseDetail = { exId -> viewModel.openExerciseDetail(exId) },
                            onAdjustRestTimer = { delta -> viewModel.adjustRestTimer(delta) },
                            onToggleRestRunning = { viewModel.toggleRestTimerRunning() },
                            onSkipRest = { viewModel.skipRestTimer() },
                            onOpenExerciseLibrary = { viewModel.openActiveExerciseLibrary() }
                        )
                    }

                    SubScreen.ACTIVE_BILATERAL -> {
                        ActiveBilateralWorkoutScreen(
                            uiState = uiState,
                            strings = strings,
                            onBack = { viewModel.navigateBack() },
                            onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                            onSelectMeasurementMode = { mode -> viewModel.navigateToSubScreen(mode, MainTab.WORKOUT) },
                            onRecordBilateralSide = { isLeft, measuredSec, targetSetNum ->
                                viewModel.recordBilateralSideStop(isLeft, measuredSec, targetSetNum)
                            },
                            onUpdateBilateralSetExact = { setNum, lSec, rSec, rir ->
                                viewModel.updateBilateralSetExact(setNum, lSec, rSec, rir)
                            },
                            onToggleSetCompleted = { setNum -> viewModel.toggleSetCompleted(setNum) },
                            onAddSet = { viewModel.addSet() },
                            onDeleteSet = { setNum -> viewModel.deleteSet(setNum) },
                            onAdjustRestTimer = { delta -> viewModel.adjustRestTimer(delta) },
                            onToggleRestRunning = { viewModel.toggleRestTimerRunning() },
                            onSkipRest = { viewModel.skipRestTimer() },
                            onPreviousExercise = { viewModel.previousActiveExercise() },
                            onNextExercise = nextWithFeedback,
                            onFinishWorkout = { viewModel.navigateToSubScreen(SubScreen.WORKOUT_SUMMARY) },
                            onDiscardWorkout = { viewModel.stopActiveWorkoutSession(uiState.activeWorkoutId, uiState.activeWorkoutGeneration) },
                            exerciseFeedback = { com.example.ui.components.StudioActiveExerciseFeedback(uiState,
                                onEffort = { id, index -> viewModel.updateExerciseFeedback(id, index = index) },
                                onNote = { id, note -> viewModel.updateExerciseFeedback(id, note = note) }) },
                            onSwitchExercise = { ex -> viewModel.switchCurrentActiveExercise(ex) },
                            onOpenExerciseDetail = { exId -> viewModel.openExerciseDetail(exId) },
                            onOpenExerciseLibrary = { viewModel.openActiveExerciseLibrary() }
                        )
                    }

                    SubScreen.ACTIVE_OUTDOOR_GPS -> {
                        ActiveOutdoorGpsScreen(
                            uiState = uiState,
                            strings = strings,
                            onBack = { viewModel.navigateBack() },
                            onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                            onSelectMeasurementMode = { mode -> viewModel.navigateToSubScreen(mode, MainTab.WORKOUT) },
                            onUpdateOutdoorMetrics = { elapsed, dist, pace ->
                                viewModel.updateOutdoorWorkoutMetrics(elapsed, dist, pace)
                            },
                            onSelectOutdoorActivity = { activityName ->
                                viewModel.selectOutdoorActivityType(activityName)
                            },
                            onStartGpsTracking = { activityName, key -> viewModel.startGpsTrackingSession(activityName, key) },
                            onPauseMeasurement = { paused -> viewModel.pauseOutdoorMeasurement(paused) },
                            onAdjustRestTimer = { delta -> viewModel.adjustRestTimer(delta) },
                            onToggleRestRunning = { viewModel.toggleRestTimerRunning() },
                            onSkipRest = { viewModel.skipRestTimer() }
                        )
                    }

                    SubScreen.INTERVAL_SETTINGS -> StudioIntervalSettingsRoute(viewModel, uiState, strings)

                    SubScreen.WORKOUT_SUMMARY -> {
                        WorkoutSummaryScreen(
                            uiState = uiState,
                            strings = strings,
                            onBack = { viewModel.navigateBack() },
                            onSelectMood = { viewModel.selectWorkoutSummaryMood(it) },
                            onExerciseEffort = { id, index -> viewModel.updateExerciseFeedback(id, index = index) },
                            onExerciseNote = { id, note -> viewModel.updateExerciseFeedback(id, note = note) },
                            onUpdateNote = { viewModel.updateWorkoutSummaryNote(it) },
                            onSaveWorkout = { viewModel.finishAndSaveWorkout() },
                            onPauseAndExit = {
                                if (viewModel.pauseActiveWorkout()) viewModel.selectTab(MainTab.HOME)
                            },
                            onDiscardWorkout = {
                                viewModel.stopActiveWorkoutSession(uiState.activeWorkoutId, uiState.activeWorkoutGeneration)
                            }
                        )
                    }

                    SubScreen.LOG_WORKOUT_DETAIL -> {
                        val workoutHealth = com.example.ui.components.studioWorkoutHealthContent(
                            workoutId = uiState.selectedLogWorkoutId,
                            language = uiState.language.name.lowercase(),
                            requestPermissions = { requestPlatformPermission("hc_permission") },
                            onSources = viewModel::updateLoggedWorkoutHealthSources
                        )
                        LogWorkoutDetailScreen(
                            uiState = uiState,
                            strings = strings,
                            onBack = { viewModel.navigateBack() },
                            onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                            onSelectDetailTab = { viewModel.selectLogDetailTab(it) },
                            onToggleExpandedExercise = { viewModel.toggleExpandedLogExercise(it) },
                            onOpenPhotos = { viewModel.openLogWorkoutPhotos() },
                            onOpenRouteMap = { viewModel.navigateToSubScreen(SubScreen.LOG_ROUTE_MAP) },
                            onOpenWatchSync = { viewModel.navigateToSubScreen(SubScreen.WATCH_AND_SYNC) },
                            onOpenHealthTab = { viewModel.selectLogDetailTab(2) },
                            deleteActions = {
                                com.example.ui.components.StudioLoggedWorkoutEditActions(uiState.selectedLogWorkoutId,
                                    uiState.language.name.lowercase(), onSaved = { viewModel.refreshAfterCloudSync(context) })
                                com.example.ui.components.StudioLoggedWorkoutDeleteActions(
                                    uiState.selectedLogWorkoutId, viewModel::onLoggedWorkoutDeleted)
                            },
                            workoutHealth = workoutHealth
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
                                viewModel.startPlannedWorkout()
                            },
                            onOpenExerciseDetail = { exId -> viewModel.openExerciseDetail(exId) },
                            onStartCoachTarget = viewModel::startCoachWorkout,
                            onOpenWorkout = { viewModel.selectTab(MainTab.WORKOUT) },
                            onCoachProgramChanged = { viewModel.refreshAfterCloudSync(context) }
                        )
                    }

                    SubScreen.SETTINGS_MAIN -> {
                        // Leaving the inline global editor must not open a program dialog
                        // or leave a global preview approvable on the next screen.
                        androidx.compose.runtime.DisposableEffect(viewModel) {
                            onDispose { viewModel.toggleProgressionModal(false) }
                        }
                        SettingsMainScreen(
                            uiState = uiState,
                            strings = strings,
                            onBack = { viewModel.navigateBack() },
                            onOpenPalettePicker = { viewModel.togglePalettePickerSheet(true) },
                            onSelectPalette = { viewModel.selectPalette(it) },
                            onSelectLanguage = { viewModel.selectLanguage(it) },
                            onCycleRestTimer = { viewModel.cycleDefaultRestTimer() },
                            onToggleSoundAlert = { viewModel.toggleSoundAlert(it) },
                            onToggleVibration = { viewModel.toggleVibrationAlert(it) },
                            onToggleProgressionModal = { viewModel.toggleProgressionModal(it) },
                            onNavigateToSubScreen = { viewModel.navigateToSubScreen(it) },
                            onShowBanner = { viewModel.showBannerMessage(it) },
                            onCloudSyncCompleted = { viewModel.refreshAfterCloudSync(context) },
                            onPreviewProgression = { style, progression, inherit ->
                                viewModel.previewProgressionSettings(style, progression, "global", inherit)
                            },
                            onApproveProgression = { viewModel.approveProgressionSettings() },
                            onEditProgressionPreview = { viewModel.discardProgressionPreview() },
                            onUndoProgression = { viewModel.undoProgressionSettings("global") }
                        )
                    }

                    SubScreen.WATCH_AND_SYNC -> {
                        androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.requestWatchDeviceInfo() }
                        WatchAndSyncScreen(
                            uiState = uiState,
                            strings = strings,
                            onBack = { viewModel.navigateBack() },
                            onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                            onRefreshWatchInfo = viewModel::requestWatchDeviceInfo,
                            onToggleControlMode = { viewModel.toggleWatchControlMode(context as? android.app.Activity) },
                            onRequestPermission = requestPlatformPermission,
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
                            },
                            onCloudSyncCompleted = { viewModel.refreshAfterCloudSync(context) }
                        )
                    }

                    SubScreen.PERMISSIONS_SCREEN -> {
                        PermissionsScreen(
                            uiState = uiState,
                            strings = strings,
                            onBack = { viewModel.navigateBack() },
                            onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                            onTogglePermission = { key, enabled ->
                                requestPlatformPermission("${key}_${if (enabled) "on" else "off"}")
                            },
                            onManagePermission = requestPlatformPermission,
                            hcBackgroundEnabled = health.hcBackground,
                            hcBackgroundPermissionGranted = com.repforge.app.HealthBackgroundJob.permitted(context),
                            hcWorkoutWriteEnabled = nativeSettingsState.settings.autoWriteWorkoutToHealthConnect,
                            hcWorkoutWritePermissionGranted = android.os.Build.VERSION.SDK_INT >= 34 &&
                                context.checkSelfPermission(com.repforge.app.HealthConnectRepository.WRITE_EXERCISE_PERMISSION) ==
                                android.content.pm.PackageManager.PERMISSION_GRANTED,
                            onToggleHcBackground = { enabled ->
                                if (enabled && health.hcBackgroundBlock ==
                                        com.repforge.app.ui.health.BackgroundBlock.BACKGROUND_PERMISSION)
                                    hcBackgroundAccess.launch(com.repforge.app.HealthConnectRepository.BACKGROUND_READ_PERMISSION)
                                else nativeHealth.toggleHcBackground(enabled)
                            },
                            onToggleHcWorkoutWrite = { enabled ->
                                if (!enabled) nativeSettings.setAutoWriteHc(false)
                                else if (android.os.Build.VERSION.SDK_INT >= 34 &&
                                    com.repforge.app.HealthConnectRepository.isSupported(context)) {
                                    nativeSettings.setAutoWriteHc(true)
                                    if (context.checkSelfPermission(com.repforge.app.HealthConnectRepository.WRITE_EXERCISE_PERMISSION) !=
                                            android.content.pm.PackageManager.PERMISSION_GRANTED)
                                        hcWorkoutWriteAccess.launch(com.repforge.app.HealthConnectRepository.WRITE_EXERCISE_PERMISSION)
                                } else viewModel.showBannerMessage(
                                    com.repforge.app.HealthConnectSyncRecovery.failureMessage(
                                        UnsupportedOperationException("Health Connect unsupported"), uiState.language.name))
                            }
                        )
                    }

                    SubScreen.PRIVATE_PHOTOS -> {
                        PrivatePhotosScreen(
                            uiState = uiState,
                            strings = strings,
                            onBack = { viewModel.navigateBack() },
                            onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                            onShowBanner = { viewModel.showBannerMessage(it) },
                            onPhotosUpdated = viewModel::refreshPrivatePhotoCount
                        )
                    }

                    SubScreen.NONE -> {
                        when (uiState.currentTab) {
                            MainTab.HOME -> {
                                HomeScreen(
                                    state = uiState,
                                    strings = strings,
                                    onDiscardWorkout = { id -> viewModel.stopActiveWorkoutSession(id, uiState.activeWorkoutGeneration) },
                                    onStartOrContinueWorkout = {
                                        if (uiState.isWorkoutSessionActive) {
                                            viewModel.returnToActiveWorkout()
                                        } else {
                                            viewModel.startPlannedWorkout()
                                        }
                                    },
                                    onOpenProgramDayDetails = {
                                        if (uiState.isWorkoutSessionActive) viewModel.returnToActiveWorkout()
                                        else {
                                            viewModel.openPlannedProgramDetails()
                                        }
                                    },
                                    inlineProgramEditor = {
                                        val program = uiState.programs.firstOrNull { it.id == uiState.inlineEditorProgramId }
                                        if (program != null) {
                                            val pending = androidx.compose.runtime.remember(program.id) {
                                                androidx.compose.runtime.mutableStateMapOf<Pair<String, String>, com.example.model.StudioProgramPrescriptionDraft>()
                                            }
                                            inlineProgramEditor(program, pending)
                                        }
                                    },
                                    onOpenQuickWorkout = { viewModel.toggleQuickWorkoutSheet(true) },
                                    onOpenHealthToday = { viewModel.selectTab(MainTab.HEALTH) },
                                    onOpenWeeklyGoals = { viewModel.openHealthAndExpand(weeklyGoals = true) },
                                    onOpenWatchAndSync = { viewModel.requestWatchDeviceInfo(); viewModel.navigateToSubScreen(SubScreen.WATCH_AND_SYNC) },
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
                                    onOpenGoalsDialog = { viewModel.toggleGoalsDialog(true) },
                                    onOpenPersonalPlanner = { viewModel.togglePersonalPlannerModal(true) }
                                )
                            }

                            MainTab.PROGRAMS -> {
                                ProgramsListScreen(
                                    state = uiState,
                                    onBack = { viewModel.navigateBack() },
                                    onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                                    onToggleCustomTab = { viewModel.setProgramsTabCustomOnly(it) },
                                    onActivateProgram = { viewModel.activateProgram(it) },
                                    onOpenProgramEditor = { progId -> viewModel.openProgramEditor(progId, 0) },
                                    onCopyProgram = { progId -> viewModel.copyProgram(progId) },
                                    onDeleteProgram = { progId -> viewModel.deleteProgram(progId) },
                                    onOpenExerciseLibrary = { viewModel.navigateToSubScreen(SubScreen.EXERCISE_LIBRARY) },
                                    onOpenPersonalPlanner = { viewModel.togglePersonalPlannerModal(true) },
                                    onMoveProgramOrder = { fromIdx, toIdx -> viewModel.moveProgramOrder(fromIdx, toIdx) },
                                    onCreateCustomProgram = { name, sub -> viewModel.createNewCustomProgram(name, sub) },
                                    onOpenAddCustomExercise = { viewModel.toggleAddCustomExerciseDialog(true) },
                                    onToggleInlineEditor = { id, host -> viewModel.toggleInlineProgramEditor(id, 0, host) },
                                    onSaveInlineDrafts = viewModel::saveEditorProgram,
                                    onRenameProgram = viewModel::renameProgram,
                                    inlineEditor = inlineProgramEditor
                                )
                            }

                            MainTab.WORKOUT -> {
                                WorkoutTabScreen(
                                    state = uiState,
                                    onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                                    onOpenQuickWorkout = { viewModel.toggleQuickWorkoutSheet(true) },
                                    onOpenExerciseLibrary = { viewModel.openQuickWorkoutSheet(0) },
                                    onOpenOutdoorWorkout = { viewModel.openQuickWorkoutSheet(2) },
                                    onOpenProgramsList = { viewModel.navigateToSubScreen(SubScreen.PROGRAMS_LIST) },
                                    onOpenTimed = { viewModel.openQuickWorkoutSheet(1) },
                                    onOpenExerciseCatalog = { viewModel.navigateToSubScreen(SubScreen.EXERCISE_LIBRARY) },
                                    onOpenProgramEditor = { progId, dayIdx -> viewModel.openProgramEditor(progId, dayIdx) },
                                    onStartWorkoutDay = { dayIdx ->
                                        viewModel.startPlannedWorkout(null, dayIdx)
                                    },
                                    onOpenCalendar = { viewModel.selectTab(MainTab.CALENDAR) },
                                    onOpenCoach = { viewModel.selectTab(MainTab.COACH) },
                                    onOpenProgressionModal = { viewModel.toggleProgressionModal(true) }
                                )
                            }

                            MainTab.CALENDAR -> {
                                CalendarPlannerScreen(
                                    state = uiState,
                                    onBack = { viewModel.navigateBack() },
                                    onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                                    onSelectDay = { viewModel.selectCalendarDay(it) },
                                    onSelectMonth = { viewModel.selectCalendarMonth(it) },
                                    onOpenReplanModal = { viewModel.toggleReplanModal(it) },
                                    onApplyReplan = { dateStr, firstDay, programId ->
                                        viewModel.applyCalendarReplan(dateStr, firstDay, programId)
                                    },
                                    onOpenWorkoutDetail = { eventId ->
                                        viewModel.openCalendarEventWorkoutDetail(eventId)
                                    },
                                    onUpdatePlanningParams = { rhythm, firstDay, startDate, time, dur, weeks ->
                                        viewModel.updatePlanningParameters(rhythm, firstDay, startDate, time, dur, weeks)
                                    },
                                    onPlanMonthlyWorkouts = { viewModel.planMonthlyWorkouts() },
                            onRequestClearCalendar = viewModel::requestCalendarClear,
                            onConfirmClearCalendar = viewModel::clearCalendarPlans,
                            onCancelClearCalendar = viewModel::cancelCalendarClear,
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
                                    onBack = { viewModel.navigateBack() },
                                    onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                                    onStartSuggestedWorkout = {
                                        viewModel.startPlannedWorkout()
                                    },
                                    onOpenExerciseDetail = { exId -> viewModel.openExerciseDetail(exId) },
                                onStartCoachTarget = viewModel::startCoachWorkout,
                                onOpenWorkout = { viewModel.selectTab(MainTab.WORKOUT) },
                            onCoachProgramChanged = { viewModel.refreshAfterCloudSync(context) }
                                )
                            }

                            MainTab.LOG -> {
                                LogTabScreen(
                                    uiState = uiState,
                                    strings = strings,
                                    onSearchChange = viewModel::updateLogSearch,
                                    onOpenWorkoutDetail = viewModel::openLogWorkoutDetail,
                                    onOpenRouteMap = viewModel::openLoggedWorkoutRouteMap,
                                    onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
                                    onOpenPalettePicker = { viewModel.togglePalettePickerSheet(true) },
                                    onDeletedWorkout = viewModel::onLoggedWorkoutDeleted
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
                                    onOpenPhotos = { viewModel.navigateToSubScreen(SubScreen.PRIVATE_PHOTOS) }
                                )
                            }

                            MainTab.HEALTH -> {
                                androidx.compose.runtime.DisposableEffect(activity) {
                                    nativeHealth.healthScreenVisible(activity)
                                    nativeHealth.refreshSamsungGrants(activity)
                                    onDispose { nativeHealth.healthScreenHidden(activity) }
                                }
                                HealthTabScreen(
                                    onOpenLoggedWorkout = viewModel::openLogWorkoutDetail,
                                    uiState = uiState.copy(primaryHealthSourceSamsung = health.provider == com.repforge.app.HealthJournalStore.SAMSUNG,
                                        lastHealthSyncLabel = health.lastSyncAt, lastHealthReadLabel = health.lastReadAt,
                                        healthLastSyncByProvider = health.lastSyncByProvider,
                                        healthRefreshRevision = health.refreshRevision,
                                        healthAutoOnOpen = health.autoOnOpen,
                                        healthBackgroundOptedIn = health.hcBackground,
                                        healthBackgroundScheduled = health.hcBackgroundScheduled,
                                        samsungGrantedCount = health.samsungGrantedCount,
                                        healthProviderStatus = (if (health.provider == com.repforge.app.HealthJournalStore.SAMSUNG)
                                            if (health.samsungInstalled && health.samsungAvailable) "Samsung Health • szolgáltató elérhető" else "Samsung Health • alkalmazás vagy SDK nem érhető el"
                                        else if (health.hcSupported && health.hcGranted) "Health Connect • olvasási hozzáférés engedélyezett" else "Health Connect • hiányzó támogatás vagy engedély") + health.providerReadDetails.takeIf { it.isNotBlank() }?.let { " • $it" }.orEmpty()),
                                    strings = strings,
                                    onToggleHealthSection = { viewModel.toggleHealthSection(it) },
                                    onSelectWeightRange = { viewModel.selectWeightRange(it) },
                                    onToggleAddWeightDialog = { viewModel.toggleAddWeightDialog(it) },
                                    onSaveNewWeight = { kg, dateStr -> viewModel.saveNewBodyWeight(kg, dateStr) },
                                    onUpdateWeight = { id, kg, dateStr -> viewModel.saveNewBodyWeight(kg, dateStr, editingId = id) },
                                    onWeightPhotosUpdated = viewModel::refreshPrivatePhotoCount,
                                    onTogglePrimaryHealthSource = { nativeHealth.setProvider(
                                        if (health.provider == com.repforge.app.HealthJournalStore.SAMSUNG) com.repforge.app.HealthJournalStore.HC
                                        else com.repforge.app.HealthJournalStore.SAMSUNG, activity) },
                                    onTriggerHealthSync = { nativeHealth.syncAllHealth(activity) },
                                    onReadHealthSource = { source -> nativeHealth.syncProviderHealth(activity, source) },
                                    onNavigateToSubScreen = { viewModel.navigateToSubScreen(it) },
                                    onOpenPalettePicker = { viewModel.togglePalettePickerSheet(true) },
                                    onOpenGoalsDialog = { viewModel.toggleGoalsDialog(true) },
                                    onDeleteWeightLog = { id -> viewModel.deleteBodyWeightLog(id) },
                                    onSelectHealthSource = { samsung -> nativeHealth.setProvider(
                                        if (samsung) com.repforge.app.HealthJournalStore.SAMSUNG else com.repforge.app.HealthJournalStore.HC, activity) },
                                    onRequestHealthPermissions = {
                                        if (health.provider == com.repforge.app.HealthJournalStore.SAMSUNG) nativeHealth.requestSamsungPermissions(activity)
                                        else requestPlatformPermission("hc_permission")
                                    }
                                )
                            }
                        }
                    }
                }
                }
                com.example.ui.components.TrainPilotRouteContent(routeHostState) { renderRoute(routeHostState) }
                if (sheetRouteActive) {
                    val sheetRoute = uiState.currentSubScreen
                    val sheetDepth = uiState.backStack.size
                    com.example.ui.components.TrainPilotRouteSheet(
                        routeKey = sheetRoute to sheetDepth,
                        // 2782: closes only this sheet; a second dismiss (system back + sheet) no longer
                        // pops further and lands on Home.
                        onDismiss = { viewModel.dismissSheetRoute(sheetRoute, sheetDepth) },
                        testTag = "route_sheet_${uiState.currentSubScreen.name.lowercase()}",
                        fillHeight = sheetRoute in com.example.ui.components.TrainPilotFullHeightSheetRoutes
                    ) { renderRoute(uiState) } // retained browsing route content
                }
            }

            // Compact Flat Floating Active Workout Return Button/Bar (visible from ANY other screen/tab/settings while a workout is active)
            val isOnActiveWorkoutScreen = uiState.currentSubScreen == SubScreen.ACTIVE_STRENGTH ||
                uiState.currentSubScreen == SubScreen.ACTIVE_TIMED ||
                uiState.currentSubScreen == SubScreen.ACTIVE_BILATERAL ||
                uiState.currentSubScreen == SubScreen.ACTIVE_OUTDOOR_GPS ||
                uiState.currentSubScreen == SubScreen.WORKOUT_SUMMARY

            AnimatedVisibility(
                visible = uiState.isWorkoutSessionActive && !isOnActiveWorkoutScreen,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = if (uiState.statusBannerMessage != null) 66.dp else 14.dp
                    )
            ) {
                // 2774: one compact bar over every page: elapsed time, running/paused, Finish, Continue.
                com.example.ui.components.ActiveWorkoutLiveBar(
                    state = uiState,
                    onContinue = { viewModel.returnToActiveWorkout() },
                    onFinish = { viewModel.navigateToSubScreen(SubScreen.WORKOUT_SUMMARY, MainTab.WORKOUT) },
                    onDiscard = { viewModel.stopActiveWorkoutSession(uiState.activeWorkoutId, uiState.activeWorkoutGeneration) },
                    modifier = Modifier.widthIn(max = contentMaxWidth)
                )
            }

            // Status / Toast Banner Pill at bottom-top
            AnimatedVisibility(
                visible = uiState.statusBannerMessage != null && !com.example.ui.components.isActiveMeasurementScreen(uiState.currentSubScreen),
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
                        .trainPilotBorder(1.5.dp, palette.primary, RoundedCornerShape(16.dp))
                        .trainPilotClickable(RoundedCornerShape(16.dp)) { viewModel.clearBannerMessage() }
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

/** Shared production route: the form's callback cannot bypass durable canonical Start. */
@Composable
internal fun StudioIntervalSettingsRoute(
    viewModel: TrainPilotViewModel,
    uiState: com.example.ui.viewmodel.TrainPilotUiState,
    strings: com.example.localization.LocalizedStrings
) {
    IntervalSettingsScreen(uiState, strings,
        onBack = { viewModel.navigateBack() },
        onOpenSettings = { viewModel.navigateToSubScreen(SubScreen.SETTINGS_MAIN) },
        onSelectMeasurementMode = { viewModel.navigateToSubScreen(it, MainTab.WORKOUT) },
        onSaveAndStartIntervals = viewModel::saveAndStartIntervals)
}
