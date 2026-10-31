package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.data.DemoDataProvider
import com.example.model.AppLanguage
import com.example.model.CalendarDayEvent
import com.example.model.CalendarEventStatus
import com.example.model.ExerciseCategory
import com.example.model.ExerciseItem
import com.example.model.ExerciseSetItem
import com.example.model.HomeDemoMode
import com.example.model.LoggedWorkoutEntry
import com.example.model.MainTab
import com.example.model.PrRecordEntry
import com.example.model.TrainingProgramModel
import com.example.model.SubScreen
import com.example.ui.theme.PaletteCategory
import com.example.ui.theme.TrainPilotPalettes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.content.Context
import androidx.compose.ui.graphics.toArgb
import com.repforge.app.ActiveWorkoutStore
import com.repforge.app.HealthBackgroundJob
import com.repforge.app.HealthJournalStore
import com.repforge.app.PhoneWorkoutExecution
import com.repforge.app.WearSyncRepository
import com.repforge.app.data.db.BodyWeightEntryEntity
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.ThemePaletteOption
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import com.repforge.app.TrainPilotWorkoutFinalization
import com.repforge.app.NativeWorkoutSession
import com.repforge.app.PhoneOwnership193
import com.repforge.app.BackupRestoreManager
import com.repforge.app.ui.progress.NativeProgressQuery
import com.repforge.app.ui.progress.MuscleLoadUiModel
import android.net.Uri
import kotlin.math.roundToInt

data class NavRoute(
    val tab: MainTab,
    val subScreen: SubScreen = SubScreen.NONE
)

data class TrainPilotUiState(
    // Theme & Localization & Accessibility
    val selectedPaletteId: Int = 1, // Default: 01 • Grafit & Arany
    val paletteFilterCategory: PaletteCategory? = null, // null = All 32
    val showPalettePickerSheet: Boolean = false,
    val language: AppLanguage = AppLanguage.HU,
    val fontScaleMultiplier: Float = 1.0f,
    val compactPhoneMode: Boolean = false,

    // Navigation & Backstack
    val currentTab: MainTab = MainTab.HOME,
    val currentSubScreen: SubScreen = SubScreen.NONE,
    val backStack: List<NavRoute> = emptyList(),

    // Home Screen State (Mockup 1/4 #1 + main Home)
    val homeDemoMode: HomeDemoMode = HomeDemoMode.PLANNED_NEXT,
    val weeklyGoalCompleted: Int = 0,
    val weeklyGoalTarget: Int = 15,
    val showGoalsDialog: Boolean = false,
    val showPersonalPlannerModal: Boolean = false,
    val showQuickWorkoutSheet: Boolean = false,
    val sleepDurationLabel: String = "—",
    val sleepHoursDecimal: String = "—",
    val restingHrBpm: Int = 0,
    val currentHrBpm: Int = 0,
    val dailySteps: Int = 0,
    val stepsGoal: Int = 10000,
    val activeCaloriesKcal: Int = 0,
    val samsungEnergyScore: Int = 0,
    val coachReadinessScore: Int = 0,

    // Watch & Sync State (Mockup 4/4 #2 + main)
    val watchName: String = "Galaxy Watch",
    val watchConnected: Boolean = false,
    val watchBatteryPercent: Int = 0,
    val phoneControlsWorkout: Boolean = true,
    val watchControlPending: Boolean = false,
    val offlineContinuationEnabled: Boolean = true,
    val lastWatchSyncLabel: String = "—",
    val autoDriveSync: Boolean = false,
    val autoCalendarSync: Boolean = false,
    val lastDriveSyncLabel: String = "—",
    val lastCalendarSyncLabel: String = "—",

    // Programs & Program Editor State (Mockup 1/4 #3 + 4/4 #6 + main)
    val programs: List<TrainingProgramModel> = emptyList(),
    val programsTabCustomOnly: Boolean = false,
    val editingProgramId: String = "",
    val editingDayIndex: Int = 0,
    val expandedEditorExerciseId: String? = null,
    val showProgressionModal: Boolean = false,
    val trainingStyleHu: String = "Normál hipertrófia",
    val progressionRuleHu: String = "Dupla progresszió",
    val beginnerGentleProfileActive: Boolean = false,

    // Calendar & Replan State (Mockup 1/4 #4 + main Naptár & Újratervezés)
    val selectedCalendarDay: Int = LocalDate.now().dayOfMonth,
    val calendarEvents: List<CalendarDayEvent> = emptyList(),
    val calendarUndoStack: List<List<CalendarDayEvent>> = emptyList(),
    val showReplanModal: Boolean = false,
    val planningCadenceHu: String = "Minden második nap",
    val planningFirstDayHu: String = "Alap A",
    val planningStartDate: String = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy. MM. dd.")),
    val planningTime: String = "10:00",
    val planningDurationMin: Int = 60,
    val planningWeeksCount: Int = 4,

    // Exercise Library & Detail State (Mockup 1/4 #5 & #6)
    val exercises: List<ExerciseItem> = emptyList(),
    val exerciseSearchQuery: String = "",
    val selectedExerciseCategory: ExerciseCategory = ExerciseCategory.ALL,
    val selectedExerciseId: String = "",
    val showAddCustomExerciseDialog: Boolean = false,

    // Active Workout & Measurement State (Mockup 2/4 #1..#6)
    val isWorkoutSessionActive: Boolean = false,
    val activeWorkoutSubScreen: SubScreen = SubScreen.ACTIVE_STRENGTH,
    val activeWorkoutTitleHu: String = "Otthoni A/B – Alap • A nap",
    val activeWorkoutExercises: List<ExerciseItem> = emptyList(),
    val activeExerciseIndex: Int = 0,
    val activeSets: List<ExerciseSetItem> = listOf(
        ExerciseSetItem(setNumber = 1, weightKg = 0f, reps = 10, rir = 2, completed = false),
        ExerciseSetItem(setNumber = 2, weightKg = 0f, reps = 10, rir = 2, completed = false),
        ExerciseSetItem(setNumber = 3, weightKg = 0f, reps = 10, rir = 2, completed = false)
    ),
    val activeSetsByExerciseId: Map<String, List<ExerciseSetItem>> = emptyMap(),
    val quickWorkoutQueue: List<ExerciseItem> = emptyList(),
    val autoModeEnabled: Boolean = false,
    val userHeightCm: Int = 179,
    val userWeightKg: Float = 65f,
    val userFitnessGoalHu: String = "Izomépítés",
    val userExperienceLevelHu: String = "Kezdő",
    val userWorkoutLocationHu: String = "Otthon",
    val userSelectedEquipmentHu: String = "Kézisúlyzó és pad",
    val userScheduleDaysPerWeek: Int = 3,
    val userWorkoutDurationMin: Int = 45,
    val userExcludedMusclesHu: Set<String> = emptySet(),
    val userExcludedExercisesHu: Set<String> = emptySet(),
    val restTimerRemainingSec: Int = 90,
    val restTimerRunning: Boolean = false,
    val defaultRestSec: Int = 90,
    val soundAlertEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val autoStepIntervals: Boolean = true,
    val intervalWorkSec: Int = 30,
    val intervalRestSec: Int = 60,
    val intervalRounds: Int = 3,
    val timedCurrentSec: Int = 0,
    val bilateralLeftSec: Int = 0,
    val bilateralRightSec: Int = 0,
    val outdoorGpsEnabled: Boolean = true,
    val outdoorDistanceKm: Float = 0f,
    val outdoorElapsedLabel: String = "00:00",
    val outdoorPaceLabel: String = "0:00",
    val workoutSummaryMoodIndex: Int = 2, // 2 = "Pont jó"
    val workoutSummaryNote: String = "",

    // Log / Diary State (Mockup 3/4 #1..#3 + main)
    val logSearchQuery: String = "",
    val selectedLogMonth: String = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy. MMMM", Locale("hu", "HU"))),
    val loggedWorkouts: List<LoggedWorkoutEntry> = emptyList(),
    val selectedLogWorkoutId: String = "",
    val logDetailTabIndex: Int = 0, // 0 = Gyakorlatok, 1 = Statisztikák, 2 = Forrás
    val expandedLogExerciseIndex: Int = 0,

    // Progress / Fejlődés State (Mockup 3/4 #4 + full main Fejlődés & Statisztikák)
    val progressMainSection: Int = 0, // 0 = Kompakt Fejlődés (main), 1 = Statisztikák (Rekordok & Kardió), 2 = 1RM Erőnlét
    val selectedProgressExercise: String = "Fekvenyomás",
    val selectedProgressRangeIndex: Int = 3, // 0 = 1 nap, 1 = 7 nap, 2 = 30 nap, 3 = 3 hó
    val selectedVolumeBarIndex: Int = -1,
    val expandedStatsCardIndex: Int = -1,
    val statsSubTab: Int = 0, // 0 = Személyes rekordok, 1 = Kardióstatisztika
    val cardioFilterStepsMode: Boolean = false, // false = Minden mozgás, true = Napi aktivitás / lépések
    val prRecords: List<PrRecordEntry> = emptyList(),
    val videoModalExerciseTitle: String? = null,
    val realMuscleLoads: List<com.example.ui.components.MuscleLoadItem> = emptyList(),
    val muscleLoadPeriodDays: Int = 90,

    // Health / Egészség State (Mockup 3/4 #5 + full main Egészség)
    val expandedHealthPulseTrend: Boolean = false,
    val expandedHealthWeightLog: Boolean = false,
    val expandedHealthExtraData: Boolean = false,
    val expandedHealthJournal: Boolean = false,
    val expandedHealthConnect: Boolean = false,
    val selectedWeightRangeIndex: Int = 1, // 0=7d, 1=30d, 2=3m, 3=1y, 4=All
    val currentBodyWeightKg: Float = 0f,
    val bodyFatPercent: Float = 0f,
    val spo2Percent: Float = 0f,
    val vo2Max: Float = 0f,
    val showAddWeightDialog: Boolean = false,
    val primaryHealthSourceSamsung: Boolean = true,

    // Permissions & Backup State (Mockup 4/4 #3, #4, #5)
    val selectedBackupFormat: String = "JSON", // JSON, ZIP, DRIVE
    val includeHealthInBackup: Boolean = true,
    val permissionGpsEnabled: Boolean = true,
    val permissionCentralLocEnabled: Boolean = true,
    val permissionNotificationsEnabled: Boolean = true,
    val permissionHealthConnectEnabled: Boolean = true,
    val permissionSamsungHealthEnabled: Boolean = false,
    val privatePhotoCount: Int = 0,

    // Toast / Feedback banner for immediate visual confirmation of actions
    val statusBannerMessage: String? = null
)

class TrainPilotViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(TrainPilotUiState())
    val uiState: StateFlow<TrainPilotUiState> = _uiState.asStateFlow()

    private var nativeContext: Context? = null
    private var nativeSettingsStore: TrainPilotSettingsStore? = null
    private var nativeDatabase: TrainPilotDatabase? = null
    private var nativeScope: CoroutineScope? = null

    private fun mapExerciseEntity(entity: com.repforge.app.data.db.ExerciseLibraryEntity): ExerciseItem {
        val cat = when (entity.primaryMuscle.lowercase()) {
            "chest", "mell", "mellkas", "push" -> ExerciseCategory.CHEST
            "back", "hát", "pull" -> ExerciseCategory.BACK
            "shoulders", "váll" -> ExerciseCategory.SHOULDERS
            "arms", "biceps", "triceps", "bicepsz", "tricepsz", "kar" -> ExerciseCategory.ARMS
            "legs", "quads", "hamstrings", "calves", "glutes", "comb", "láb", "far" -> ExerciseCategory.LEGS
            "core", "abs", "has", "törzs" -> ExerciseCategory.CORE
            else -> ExerciseCategory.CHEST
        }
        val parsedSteps = entity.notesHu
            .split(Regex("(?<=\\.)\\s+|\\n+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty {
                listOf("Helyes kivitelezés: ügyelj az egyenes hátra és a kontrollált mozgástartományra.")
            }
        return ExerciseItem(
            id = entity.id,
            nameHu = entity.nameHu.ifBlank { entity.nameEn }.ifBlank { entity.id },
            nameEn = entity.nameEn.ifBlank { entity.nameHu }.ifBlank { entity.id },
            nameDe = entity.nameDe.ifBlank { entity.nameHu },
            nameRo = entity.nameRo.ifBlank { entity.nameHu },
            category = cat,
            equipmentHu = entity.loadType.ifBlank { "Súlyzó" },
            primaryMusclesHu = entity.primaryMuscle.ifBlank { "Mellkas" },
            secondaryMusclesHu = entity.muscleGroupsJson,
            defaultSets = entity.defaultSets.coerceAtLeast(1),
            defaultReps = entity.defaultReps.toIntOrNull() ?: 10,
            defaultWeightKg = entity.defaultWeight.toFloat(),
            defaultRestSec = entity.defaultRestSeconds.coerceAtLeast(30),
            isTimed = entity.measurementType.contains("time", ignoreCase = true) || entity.repUnit.contains("sec", ignoreCase = true),
            isBilateral = entity.measurementType.contains("bilateral", ignoreCase = true) || entity.measurementType.contains("side", ignoreCase = true),
            stepsHu = parsedSteps,
            coachStatusHu = "Normál terhelés",
            coachHintHu = parsedSteps.firstOrNull() ?: "Kezdj könnyebben; ma ne erőltesd a progressziót."
        )
    }

    private fun mapCatalogPrograms(exercisesMap: Map<String, ExerciseItem>): List<TrainingProgramModel> {
        return try {
            val (programs, days) = TrainPilotDatabase.defaultPrograms()
            programs.mapIndexed { pIdx, prog ->
                val progDays = days.filter { it.programId == prog.id }.mapIndexed { dIdx, dayEntity ->
                    val exIds = try {
                        val arr = JSONArray(dayEntity.exercisesJson)
                        (0 until arr.length()).mapNotNull { i ->
                            val itm = arr.get(i)
                            if (itm is JSONObject) itm.optString("id") else itm.toString()
                        }
                    } catch (_: Throwable) { emptyList<String>() }
                    val dayExercises = exIds.mapNotNull { exercisesMap[it] }.ifEmpty {
                        exercisesMap.values.take(6)
                    }
                    com.example.model.ProgramDayModel(
                        id = dayEntity.compositeId,
                        dayCode = when (dIdx) { 0 -> "A nap"; 1 -> "B nap"; 2 -> "C nap"; else -> "D nap" },
                        titleHu = dayEntity.nameHu.ifBlank { "Edzésnap ${dIdx + 1}" },
                        titleEn = dayEntity.nameEn.ifBlank { "Day ${dIdx + 1}" },
                        durationMinutes = 60,
                        exercises = dayExercises
                    )
                }
                TrainingProgramModel(
                    id = prog.id,
                    nameHu = prog.nameHu.ifBlank { prog.nameEn }.ifBlank { "Edzésterv ${pIdx + 1}" },
                    nameEn = prog.nameEn.ifBlank { prog.nameHu }.ifBlank { "Program ${pIdx + 1}" },
                    subtitleHu = prog.description.ifBlank { "Beépített alap edzésterv" },
                    subtitleEn = prog.description.ifBlank { "Built-in training program" },
                    tagsHu = listOf("Alap program", prog.level.ifBlank { "Kezdő" }),
                    isCustom = !prog.builtin,
                    isActive = (pIdx == 0),
                    days = progDays
                )
            }
        } catch (_: Throwable) { DemoDataProvider.defaultPrograms }
    }

    private fun buildDefaultSetsForExercise(ex: ExerciseItem): List<ExerciseSetItem> {
        val count = ex.defaultSets.coerceIn(1, 6)
        return (1..count).map { setNum ->
            ExerciseSetItem(
                setNumber = setNum,
                weightKg = ex.defaultWeightKg,
                reps = ex.defaultReps.coerceAtLeast(1),
                rir = 2,
                completed = false
            )
        }
    }

    init {
        try {
            val allExs = TrainPilotDatabase.defaultExercises().map { mapExerciseEntity(it) }
            val exMap = allExs.associateBy { it.id }
            val allProgs = mapCatalogPrograms(exMap)
            if (allExs.isNotEmpty() && allProgs.isNotEmpty()) {
                val firstProg = allProgs.firstOrNull { it.isActive } ?: allProgs.first()
                val firstDay = firstProg.days.firstOrNull()
                val initialWorkoutExs = firstDay?.exercises?.ifEmpty { allExs.take(6) } ?: allExs.take(6)
                val firstEx = initialWorkoutExs.firstOrNull() ?: allExs.first()
                _uiState.update {
                    it.copy(
                        exercises = allExs,
                        programs = allProgs,
                        editingProgramId = firstProg.id,
                        activeWorkoutTitleHu = "${firstProg.nameHu} • ${firstDay?.dayCode ?: "A nap"}",
                        activeWorkoutExercises = initialWorkoutExs,
                        activeExerciseIndex = 0,
                        selectedExerciseId = firstEx.id,
                        activeSets = buildDefaultSetsForExercise(firstEx)
                    )
                }
            }
        } catch (_: Throwable) {}
    }

    fun bindNativeEnvironment(
        context: Context,
        settingsStore: TrainPilotSettingsStore,
        database: TrainPilotDatabase,
        scope: CoroutineScope
    ) {
        nativeContext = context
        nativeSettingsStore = settingsStore
        nativeDatabase = database
        nativeScope = scope

        scope.launch(Dispatchers.IO) {
            // Seed catalog into Room DB
            try {
                TrainPilotDatabase.ensureBuiltinCatalogSeeded(database, context)
            } catch (_: Throwable) {}

            // 1. Detect if a paused or active workout is running natively
            try {
                val execution = PhoneWorkoutExecution.store(context).snapshot()
                val activeStore = ActiveWorkoutStore.current(context)
                val isPaused = (execution?.optBoolean("active") == true && execution.optBoolean("workoutPaused")) ||
                        (activeStore?.optBoolean("active") == true && activeStore.optBoolean("workoutPaused"))
                val isRunning = (execution?.optBoolean("active") == true && !execution.optBoolean("workoutPaused")) ||
                        (activeStore?.optBoolean("active") == true && !activeStore.optBoolean("workoutPaused"))

                if (isPaused || isRunning) {
                    _uiState.update { it.copy(homeDemoMode = HomeDemoMode.PAUSED_WORKOUT) }
                } else {
                    _uiState.update { it.copy(homeDemoMode = HomeDemoMode.PLANNED_NEXT) }
                }
            } catch (_: Throwable) {}

            // 2. Read all real exercises from Room ExerciseLibrary (106 exercises)
            val mappedExercises = try {
                val dbExercises = database.exerciseLibraryDao().getAllExercises()
                if (dbExercises.isNotEmpty()) {
                    dbExercises.map { mapExerciseEntity(it) }
                } else {
                    TrainPilotDatabase.defaultExercises().map { mapExerciseEntity(it) }
                }
            } catch (_: Throwable) {
                try {
                    TrainPilotDatabase.defaultExercises().map { mapExerciseEntity(it) }
                } catch (_: Throwable) { DemoDataProvider.defaultExercises }
            }

            val exercisesMap = mappedExercises.associateBy { it.id }

            // 3. Read real programs and days from Room ProgramDao (13 programs)
            val mappedPrograms = try {
                val dbPrograms = database.programDao().getProgramsWithDays()
                if (dbPrograms.isNotEmpty()) {
                    dbPrograms.mapIndexed { pIdx, pwd ->
                        val prog = pwd.program
                        val days = pwd.days.mapIndexed { dIdx, dayEntity ->
                            val exIds = try {
                                val arr = JSONArray(dayEntity.exercisesJson)
                                (0 until arr.length()).mapNotNull { i ->
                                    val itm = arr.get(i)
                                    if (itm is JSONObject) itm.optString("id") else itm.toString()
                                }
                            } catch (_: Throwable) { emptyList<String>() }
                            val dayExercises = exIds.mapNotNull { exercisesMap[it] }.ifEmpty {
                                mappedExercises.take(6)
                            }
                            com.example.model.ProgramDayModel(
                                id = dayEntity.compositeId,
                                dayCode = when (dIdx) { 0 -> "A nap"; 1 -> "B nap"; 2 -> "C nap"; else -> "D nap" },
                                titleHu = dayEntity.nameHu.ifBlank { "Edzésnap ${dIdx + 1}" },
                                titleEn = dayEntity.nameEn.ifBlank { "Day ${dIdx + 1}" },
                                durationMinutes = 60,
                                exercises = dayExercises
                            )
                        }
                        TrainingProgramModel(
                            id = prog.id,
                            nameHu = prog.nameHu.ifBlank { prog.nameEn }.ifBlank { "Edzésterv ${pIdx + 1}" },
                            nameEn = prog.nameEn.ifBlank { prog.nameHu }.ifBlank { "Program ${pIdx + 1}" },
                            subtitleHu = prog.description.ifBlank { "Alap beépített edzésterv" },
                            subtitleEn = prog.description.ifBlank { "Built-in training program" },
                            tagsHu = listOf("Alap program", prog.level.ifBlank { "Kezdő" }),
                            isCustom = !prog.builtin,
                            isActive = (pIdx == 0),
                            days = days
                        )
                    }
                } else mapCatalogPrograms(exercisesMap)
            } catch (_: Throwable) { mapCatalogPrograms(exercisesMap) }

            // 4. Read latest body weight from Room
            try {
                val weightSummary = database.bodyWeightDao().getWeightJournalSummary()
                if (weightSummary.currentKg != null && weightSummary.currentKg > 0.0) {
                    _uiState.update { it.copy(currentBodyWeightKg = weightSummary.currentKg.toFloat()) }
                }
            } catch (_: Throwable) {}

            // 5. Read persisted workouts from Room
            val mappedWorkouts = try {
                val dbWorkouts = database.workoutHistoryDao().getAllWorkoutsWithDetails()
                dbWorkouts.map { detail ->
                    val w = detail.workout
                    val exerciseNames = detail.exercises.joinToString(", ") { exWithSets ->
                        val ex = exWithSets.exercise
                        ex.nameHu.ifBlank { ex.exerciseId }
                    }
                    val allSets = detail.exercises.flatMap { it.sets }
                    val isOutdoor = detail.exercises.any {
                        val ex = it.exercise
                        ex.measurementType == "distance" || ex.repUnit == "km"
                    }
                    LoggedWorkoutEntry(
                        id = w.id,
                        dateLabelHu = w.started.take(16).replace('T', ' '),
                        titleHu = w.programName.ifBlank { w.dayName }.ifBlank { "Edzés" },
                        durationMinutes = if (w.finishedEpochMs > w.startedEpochMs) ((w.finishedEpochMs - w.startedEpochMs) / 60000).toInt() else 45,
                        workTimeLabel = "30:00",
                        restTimeLabel = "15:00",
                        totalSets = if (w.completedSetsCount > 0) w.completedSetsCount else allSets.size,
                        totalVolumeKg = if (w.totalVolumeKg > 0.0) w.totalVolumeKg.toInt() else allSets.sumOf { ((it.weight ?: 0.0) * (it.reps.toIntOrNull() ?: 0)).toInt() },
                        summaryExercisesHu = exerciseNames.ifBlank { "Rögzített gyakorlatok" },
                        hasPr = w.hasPr226,
                        isOutdoorRun = isOutdoor
                    )
                }
            } catch (_: Throwable) { emptyList() }

            // 6. Read real daily health projections from HealthJournalStore using sleepMinutes
            val todayStr = LocalDate.now().toString()
            var stepsVal = 0
            var restingHrVal = 0
            var currentHrVal = 0
            var activeCalVal = 0
            var sleepStr = "—"
            var sleepDecimal = "—"

            try {
                val store = HealthJournalStore.get(context)
                val projection = store.projection()
                val todayData = projection.optJSONObject("days")?.optJSONObject(todayStr)
                if (todayData != null) {
                    val stepsDbl = todayData.optDouble("steps", Double.NaN)
                    val restingDbl = todayData.optDouble("restingHeartRate", Double.NaN)
                    val currentDbl = todayData.optDouble("heartRate", Double.NaN)
                    val calDbl = todayData.optDouble("activeCalories", Double.NaN)
                    val sleepMinDbl = todayData.optDouble("sleepMinutes", Double.NaN)

                    if (stepsDbl.isFinite() && stepsDbl >= 0) stepsVal = stepsDbl.toInt()
                    if (restingDbl.isFinite() && restingDbl > 0) restingHrVal = restingDbl.toInt()
                    if (currentDbl.isFinite() && currentDbl > 0) currentHrVal = currentDbl.toInt()
                    if (calDbl.isFinite() && calDbl > 0) activeCalVal = calDbl.toInt()
                    if (sleepMinDbl.isFinite() && sleepMinDbl > 0) {
                        sleepStr = String.format("%d ó %d p", (sleepMinDbl / 60).toInt(), (sleepMinDbl % 60).toInt())
                        sleepDecimal = "${((sleepMinDbl / 60.0) * 10.0).roundToInt() / 10.0} h"
                    }
                }
            } catch (_: Throwable) {}

            withContext(Dispatchers.Main) {
                _uiState.update { st ->
                    val activeProg = mappedPrograms.firstOrNull { it.isActive } ?: mappedPrograms.firstOrNull()
                    val firstDay = activeProg?.days?.firstOrNull()
                    val initialWorkoutExs = if (st.activeWorkoutExercises.isNotEmpty()) {
                        st.activeWorkoutExercises
                    } else {
                        firstDay?.exercises?.ifEmpty { mappedExercises.take(6) } ?: mappedExercises.take(6)
                    }
                    val firstEx = initialWorkoutExs.firstOrNull() ?: mappedExercises.firstOrNull()
                    st.copy(
                        exercises = mappedExercises,
                        selectedExerciseId = firstEx?.id ?: st.selectedExerciseId,
                        programs = mappedPrograms,
                        editingProgramId = activeProg?.id ?: st.editingProgramId,
                        activeWorkoutTitleHu = if (activeProg != null) "${activeProg.nameHu} • ${firstDay?.dayCode ?: "A nap"}" else st.activeWorkoutTitleHu,
                        activeWorkoutExercises = initialWorkoutExs,
                        activeSets = if (firstEx != null && st.activeSetsByExerciseId.isEmpty()) buildDefaultSetsForExercise(firstEx) else st.activeSets,
                        loggedWorkouts = mappedWorkouts,
                        weeklyGoalCompleted = mappedWorkouts.size,
                        dailySteps = if (stepsVal > 0) stepsVal else st.dailySteps,
                        restingHrBpm = if (restingHrVal > 0) restingHrVal else st.restingHrBpm,
                        currentHrBpm = if (currentHrVal > 0) currentHrVal else st.currentHrBpm,
                        activeCaloriesKcal = if (activeCalVal > 0) activeCalVal else st.activeCaloriesKcal,
                        sleepDurationLabel = if (sleepStr != "—") sleepStr else st.sleepDurationLabel,
                        sleepHoursDecimal = if (sleepDecimal != "—") sleepDecimal else st.sleepHoursDecimal
                    )
                }
            }

            // 7. Load real muscle load summary from Room
            loadMuscleLoadSummary(90)
        }
    }

    fun selectTab(tab: MainTab) {
        _uiState.update { state ->
            if (state.currentTab == tab && state.currentSubScreen == SubScreen.NONE) {
                state
            } else {
                val updatedStack = (state.backStack + NavRoute(state.currentTab, state.currentSubScreen)).takeLast(16)
                state.copy(
                    currentTab = tab,
                    currentSubScreen = SubScreen.NONE,
                    backStack = updatedStack
                )
            }
        }
    }

    fun navigateToSubScreen(subScreen: SubScreen, targetTab: MainTab? = null) {
        val isActiveMeasurement = subScreen == SubScreen.ACTIVE_STRENGTH ||
            subScreen == SubScreen.ACTIVE_TIMED ||
            subScreen == SubScreen.ACTIVE_BILATERAL ||
            subScreen == SubScreen.ACTIVE_OUTDOOR_GPS
        _uiState.update { state ->
            val updatedStack = (state.backStack + NavRoute(state.currentTab, state.currentSubScreen)).takeLast(16)
            state.copy(
                currentTab = targetTab ?: state.currentTab,
                currentSubScreen = subScreen,
                isWorkoutSessionActive = if (isActiveMeasurement) true else state.isWorkoutSessionActive,
                activeWorkoutSubScreen = if (isActiveMeasurement) subScreen else state.activeWorkoutSubScreen,
                backStack = updatedStack
            )
        }
    }

    fun returnToActiveWorkout() {
        _uiState.update { state ->
            val targetSub = state.activeWorkoutSubScreen
            val updatedStack = (state.backStack + NavRoute(state.currentTab, state.currentSubScreen)).takeLast(16)
            state.copy(
                currentTab = MainTab.WORKOUT,
                currentSubScreen = targetSub,
                isWorkoutSessionActive = true,
                backStack = updatedStack
            )
        }
    }

    fun stopActiveWorkoutSession() {
        _uiState.update { state ->
            state.copy(isWorkoutSessionActive = false)
        }
    }

    fun navigateBack(): Boolean {
        val current = _uiState.value
        if (current.backStack.isNotEmpty()) {
            val previous = current.backStack.last()
            _uiState.update {
                it.copy(
                    currentTab = previous.tab,
                    currentSubScreen = previous.subScreen,
                    backStack = it.backStack.dropLast(1)
                )
            }
            return true
        } else if (current.currentSubScreen != SubScreen.NONE) {
            _uiState.update { it.copy(currentSubScreen = SubScreen.NONE) }
            return true
        } else if (current.currentTab != MainTab.HOME) {
            _uiState.update { it.copy(currentTab = MainTab.HOME) }
            return true
        }
        return false
    }

    fun selectPalette(paletteId: Int) {
        _uiState.update {
            it.copy(
                selectedPaletteId = paletteId,
                statusBannerMessage = "Színpaletta frissítve (#$paletteId)"
            )
        }
        val store = nativeSettingsStore
        val scope = nativeScope
        if (store != null && scope != null) {
            val pal = TrainPilotPalettes.getById(paletteId)
            val hexAccent = "#" + (pal.primary.value.toLong() and 0xFFFFFFL).toString(16).padStart(6, '0')
            val hexAccent2 = "#" + (pal.secondary.value.toLong() and 0xFFFFFFL).toString(16).padStart(6, '0')
            val option = ThemePaletteOption(
                key = pal.key,
                nameHu = pal.nameHu,
                nameEn = pal.nameEn,
                accent = hexAccent,
                accent2 = hexAccent2,
                family = when (pal.category) {
                    PaletteCategory.MATTE, PaletteCategory.BASIC -> "matte"
                    PaletteCategory.CONTRAST -> "contrast"
                    PaletteCategory.GLOW, PaletteCategory.VIVID -> "glow"
                    PaletteCategory.LIGHT -> "light"
                    else -> "matte"
                },
                nameDe = pal.nameDe,
                nameRo = pal.nameRo
            )
            scope.launch(Dispatchers.IO) {
                try { store.setTheme(option) } catch (_: Throwable) {}
            }
        }
    }

    fun setPaletteFilterCategory(category: PaletteCategory?) {
        _uiState.update { it.copy(paletteFilterCategory = category) }
    }

    fun togglePalettePickerSheet(show: Boolean) {
        _uiState.update { it.copy(showPalettePickerSheet = show) }
    }

    fun selectLanguage(lang: AppLanguage) {
        _uiState.update {
            it.copy(
                language = lang,
                statusBannerMessage = "Nyelv: ${lang.label} (${lang.code})"
            )
        }
        val store = nativeSettingsStore
        val scope = nativeScope
        if (store != null && scope != null) {
            scope.launch(Dispatchers.IO) {
                try { store.setLanguage(lang.code.lowercase()) } catch (_: Throwable) {}
            }
        }
    }

    fun setFontScaleMultiplier(scale: Float) {
        _uiState.update { it.copy(fontScaleMultiplier = scale) }
    }

    fun toggleCompactPhoneMode(compact: Boolean) {
        _uiState.update { it.copy(compactPhoneMode = compact) }
    }

    fun cycleHomeDemoMode(mode: HomeDemoMode) {
        _uiState.update {
            val readiness = if (mode == HomeDemoMode.COACH_LIGHT_DAY) 54 else 87
            it.copy(
                homeDemoMode = mode,
                coachReadinessScore = readiness,
                samsungEnergyScore = if (mode == HomeDemoMode.COACH_LIGHT_DAY) 61 else 87
            )
        }
    }

    fun toggleGoalsDialog(show: Boolean) {
        _uiState.update { it.copy(showGoalsDialog = show) }
    }

    fun updateGoals(weeklyTarget: Int, stepsTarget: Int) {
        _uiState.update {
            it.copy(
                weeklyGoalTarget = weeklyTarget.coerceIn(1, 30),
                stepsGoal = stepsTarget.coerceIn(2000, 30000),
                showGoalsDialog = false,
                statusBannerMessage = "Célok elmentve: $weeklyTarget edzés, $stepsTarget lépés"
            )
        }
    }

    fun togglePersonalPlannerModal(show: Boolean) {
        _uiState.update { it.copy(showPersonalPlannerModal = show) }
    }

    fun toggleQuickWorkoutSheet(show: Boolean) {
        _uiState.update { it.copy(showQuickWorkoutSheet = show) }
    }

    // Program & Exercise Editor Actions
    fun setProgramsTabCustomOnly(customOnly: Boolean) {
        _uiState.update { it.copy(programsTabCustomOnly = customOnly) }
    }

    fun activateProgram(programId: String) {
        _uiState.update { state ->
            val updated = state.programs.map { prog ->
                prog.copy(isActive = prog.id == programId)
            }
            state.copy(
                programs = updated,
                editingProgramId = programId,
                statusBannerMessage = "Aktív program frissítve"
            )
        }
    }

    fun openProgramEditor(programId: String, dayIndex: Int = 0) {
        _uiState.update {
            val back = (it.backStack + NavRoute(it.currentTab, it.currentSubScreen)).takeLast(16)
            it.copy(
                editingProgramId = programId,
                editingDayIndex = dayIndex,
                currentSubScreen = SubScreen.PROGRAM_EDITOR,
                backStack = back
            )
        }
    }

    fun selectEditingDayIndex(dayIndex: Int) {
        _uiState.update { it.copy(editingDayIndex = dayIndex) }
    }

    fun toggleExpandedEditorExercise(exerciseId: String) {
        _uiState.update {
            it.copy(
                expandedEditorExerciseId = if (it.expandedEditorExerciseId == exerciseId) null else exerciseId
            )
        }
    }

    fun updateProgramExercisePrescription(
        exerciseId: String,
        sets: Int,
        reps: Int,
        weightKg: Float,
        restSec: Int
    ) {
        _uiState.update { state ->
            val updatedPrograms = state.programs.map { prog ->
                if (prog.id != state.editingProgramId) prog
                else {
                    val updatedDays = prog.days.mapIndexed { idx, day ->
                        if (idx != state.editingDayIndex) day
                        else {
                            day.copy(
                                exercises = day.exercises.map { ex ->
                                    if (ex.id != exerciseId) ex
                                    else ex.copy(
                                        defaultSets = sets.coerceIn(1, 10),
                                        defaultReps = reps.coerceIn(1, 50),
                                        defaultWeightKg = weightKg.coerceAtLeast(0f),
                                        defaultRestSec = restSec.coerceIn(15, 300)
                                    )
                                }
                            )
                        }
                    }
                    prog.copy(days = updatedDays)
                }
            }
            state.copy(
                programs = updatedPrograms,
                expandedEditorExerciseId = null,
                statusBannerMessage = "Gyakorlat beállításai elmentve"
            )
        }
    }

    fun addExerciseToActiveProgramDay(exercise: ExerciseItem) {
        _uiState.update { state ->
            val updatedPrograms = state.programs.map { prog ->
                if (prog.id != state.editingProgramId) prog
                else {
                    val updatedDays = prog.days.mapIndexed { idx, day ->
                        if (idx != state.editingDayIndex) day
                        else if (day.exercises.any { it.id == exercise.id }) day
                        else day.copy(exercises = day.exercises + exercise)
                    }
                    prog.copy(days = updatedDays)
                }
            }
            state.copy(
                programs = updatedPrograms,
                statusBannerMessage = "${exercise.nameHu} hozzáadva az edzésnaphoz"
            )
        }
    }

    fun moveEditorExercise(fromIndex: Int, toIndex: Int) {
        _uiState.update { state ->
            val updatedPrograms = state.programs.map { prog ->
                if (prog.id != state.editingProgramId) prog
                else {
                    val updatedDays = prog.days.mapIndexed { idx, day ->
                        if (idx != state.editingDayIndex) day
                        else {
                            val list = day.exercises.toMutableList()
                            if (fromIndex in list.indices && toIndex in list.indices && fromIndex != toIndex) {
                                val moved = list.removeAt(fromIndex)
                                list.add(toIndex, moved)
                            }
                            day.copy(exercises = list)
                        }
                    }
                    prog.copy(days = updatedDays)
                }
            }
            state.copy(
                programs = updatedPrograms,
                statusBannerMessage = "Gyakorlatsorrend frissítve"
            )
        }
    }

    fun moveProgramOrder(fromIndex: Int, toIndex: Int) {
        _uiState.update { state ->
            val visiblePrograms = if (state.programsTabCustomOnly) {
                state.programs.filter { it.isCustom }
            } else {
                state.programs.filter { !it.isCustom }
            }
            if (fromIndex !in visiblePrograms.indices || toIndex !in visiblePrograms.indices || fromIndex == toIndex) {
                return@update state
            }
            val reorderedVisible = visiblePrograms.toMutableList().apply {
                val item = removeAt(fromIndex)
                add(toIndex, item)
            }
            var visibleCursor = 0
            val merged = state.programs.map { prog ->
                val belongsToCurrentTab = if (state.programsTabCustomOnly) prog.isCustom else !prog.isCustom
                if (belongsToCurrentTab) {
                    reorderedVisible[visibleCursor++]
                } else {
                    prog
                }
            }
            state.copy(
                programs = merged,
                statusBannerMessage = "Programok sorrendje frissítve"
            )
        }
    }

    fun createNewCustomProgram(nameHu: String, subtitleHu: String) {
        val cleanName = nameHu.trim().ifBlank { "Egyedi edzésterv" }
        val cleanSub = subtitleHu.trim().ifBlank { "Saját összeállítású program" }
        _uiState.update { state ->
            val newId = "custom_prog_${System.currentTimeMillis()}"
            val starterExercises = state.exercises.take(4)
            val newProg = TrainingProgramModel(
                id = newId,
                nameHu = cleanName,
                nameEn = cleanName,
                subtitleHu = cleanSub,
                subtitleEn = cleanSub,
                tagsHu = listOf("Saját program", "Egyedi"),
                isCustom = true,
                isActive = true,
                days = listOf(
                    com.example.model.ProgramDayModel(
                        id = "${newId}_dayA",
                        dayCode = "A nap",
                        titleHu = "A nap • $cleanName",
                        titleEn = "Day A • $cleanName",
                        durationMinutes = 45,
                        exercises = starterExercises
                    ),
                    com.example.model.ProgramDayModel(
                        id = "${newId}_dayB",
                        dayCode = "B nap",
                        titleHu = "B nap • $cleanName",
                        titleEn = "Day B • $cleanName",
                        durationMinutes = 45,
                        exercises = state.exercises.drop(4).take(4)
                    )
                )
            )
            val updatedPrograms = listOf(newProg) + state.programs.map { it.copy(isActive = false) }
            val back = (state.backStack + NavRoute(state.currentTab, state.currentSubScreen)).takeLast(16)
            state.copy(
                programs = updatedPrograms,
                programsTabCustomOnly = true,
                editingProgramId = newId,
                editingDayIndex = 0,
                currentSubScreen = SubScreen.PROGRAM_EDITOR,
                backStack = back,
                statusBannerMessage = "Új egyedi program létrehozva: $cleanName"
            )
        }
    }

    fun toggleAddCustomExerciseDialog(show: Boolean) {
        _uiState.update { it.copy(showAddCustomExerciseDialog = show) }
    }

    fun addCustomExercise(
        nameHu: String,
        category: ExerciseCategory,
        equipmentHu: String,
        descriptionHu: String,
        videoUrl: String = "",
        isTimed: Boolean = false,
        addToCurrentProgramDay: Boolean = true
    ) {
        val cleanName = nameHu.trim().ifBlank { "Egyedi gyakorlat" }
        val cleanEquip = equipmentHu.trim().ifBlank { "Saját testsúly / Egyedi" }
        val cleanDesc = descriptionHu.trim().ifBlank { "Egyedi felhasználói gyakorlat saját leírással és végrehajtási jegyzettel." }
        val parsedSteps = cleanDesc
            .split("\n", ".", ";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .ifEmpty { listOf(cleanDesc) }

        val newExercise = ExerciseItem(
            id = "custom_ex_${System.currentTimeMillis()}",
            nameHu = cleanName,
            nameEn = cleanName,
            nameDe = cleanName,
            nameRo = cleanName,
            category = if (category == ExerciseCategory.ALL) ExerciseCategory.CORE else category,
            equipmentHu = cleanEquip,
            primaryMusclesHu = (if (category == ExerciseCategory.ALL) ExerciseCategory.CORE else category).labelHu,
            secondaryMusclesHu = if (videoUrl.isNotBlank()) "Videó: $videoUrl" else "Stabilizáló izmok",
            defaultSets = 3,
            defaultReps = if (isTimed) 1 else 12,
            defaultWeightKg = if (cleanEquip.contains("testsúly", ignoreCase = true) || cleanName.contains("futás", ignoreCase = true)) 0f else 20f,
            defaultRestSec = 90,
            isTimed = isTimed,
            targetSeconds = 60,
            stepsHu = parsedSteps,
            coachStatusHu = "Egyedi gyakorlat",
            coachHintHu = cleanDesc
        )

        _uiState.update { state ->
            val updatedExercises = listOf(newExercise) + state.exercises
            val updatedPrograms = if (addToCurrentProgramDay) {
                state.programs.map { prog ->
                    if (prog.id != state.editingProgramId) prog
                    else {
                        val updatedDays = prog.days.mapIndexed { idx, day ->
                            if (idx != state.editingDayIndex) day
                            else day.copy(exercises = day.exercises + newExercise)
                        }
                        prog.copy(days = updatedDays)
                    }
                }
            } else {
                state.programs
            }
            state.copy(
                exercises = updatedExercises,
                programs = updatedPrograms,
                selectedExerciseId = newExercise.id,
                showAddCustomExerciseDialog = false,
                statusBannerMessage = if (addToCurrentProgramDay) {
                    "Új gyakorlat létrehozva és hozzáadva a programhoz: $cleanName"
                } else {
                    "Új egyedi gyakorlat elmentve: $cleanName"
                }
            )
        }
    }

    // Calendar & Replan Actions
    fun selectCalendarDay(day: Int) {
        _uiState.update { it.copy(selectedCalendarDay = day) }
    }

    fun toggleReplanModal(show: Boolean) {
        _uiState.update { it.copy(showReplanModal = show) }
    }

    fun updatePlanningParameters(
        cadenceHu: String = _uiState.value.planningCadenceHu,
        firstDayHu: String = _uiState.value.planningFirstDayHu,
        startDate: String = _uiState.value.planningStartDate,
        time: String = _uiState.value.planningTime,
        durationMin: Int = _uiState.value.planningDurationMin,
        weeksCount: Int = _uiState.value.planningWeeksCount
    ) {
        _uiState.update {
            it.copy(
                planningCadenceHu = cadenceHu,
                planningFirstDayHu = firstDayHu,
                planningStartDate = startDate,
                planningTime = time,
                planningDurationMin = durationMin,
                planningWeeksCount = weeksCount
            )
        }
    }

    fun planMonthlyWorkouts() {
        _uiState.update { state ->
            val startDayNum = state.planningStartDate
                .split(".")
                .mapNotNull { it.trim().toIntOrNull() }
                .lastOrNull()
                ?.coerceIn(1, 31) ?: state.selectedCalendarDay.coerceIn(1, 31)

            val stepDays = when {
                state.planningCadenceHu.contains("Minden nap", ignoreCase = true) -> 1
                state.planningCadenceHu.contains("3", ignoreCase = true) -> 2
                state.planningCadenceHu.contains("4", ignoreCase = true) -> 2
                else -> 2 // Minden második nap
            }

            val retainedCompleted = state.calendarEvents.filter {
                it.status == CalendarEventStatus.COMPLETED || it.dayOfMonth < startDayNum
            }
            val activeProgName = state.programs.firstOrNull { it.isActive }?.nameHu
                ?: "Otthoni A/B – Alap (személyre szabott)"

            var currentSplit = if (state.planningFirstDayHu.contains("B", ignoreCase = true)) "B" else "A"
            val newPlanned = mutableListOf<CalendarDayEvent>()
            var d = startDayNum
            while (d <= 31) {
                if (retainedCompleted.none { it.dayOfMonth == d && it.status == CalendarEventStatus.COMPLETED }) {
                    newPlanned.add(
                        CalendarDayEvent(
                            id = "ev_plan_${d}_${System.currentTimeMillis()}",
                            dayOfMonth = d,
                            timeLabel = state.planningTime,
                            titleHu = "$activeProgName • Alap $currentSplit",
                            exerciseCount = 6,
                            durationMinutes = state.planningDurationMin,
                            status = CalendarEventStatus.PLANNED,
                            splitBadge = currentSplit
                        )
                    )
                    currentSplit = if (currentSplit == "A") "B" else "A"
                }
                d += stepDays
            }

            state.copy(
                calendarUndoStack = (state.calendarUndoStack + listOf(state.calendarEvents)).takeLast(12),
                calendarEvents = (retainedCompleted + newPlanned).sortedBy { it.dayOfMonth },
                selectedCalendarDay = startDayNum,
                statusBannerMessage = "Edzések megtervezve (${newPlanned.size} alkalom • ${state.planningCadenceHu})"
            )
        }
    }

    fun applyCalendarReplan(newStartDate: String, firstDayHu: String) {
        _uiState.update { state ->
            val startDayNum = newStartDate
                .split(".")
                .mapNotNull { it.trim().toIntOrNull() }
                .lastOrNull()
                ?.coerceIn(1, 31) ?: 28

            val retained = state.calendarEvents.filter {
                it.status == CalendarEventStatus.COMPLETED
            }
            val activeProgName = state.programs.firstOrNull { it.isActive }?.nameHu
                ?: "Otthoni A/B – Alap (személyre szabott)"

            var split = when {
                firstDayHu.contains("B", ignoreCase = true) -> "B"
                firstDayHu.contains("A", ignoreCase = true) && !firstDayHu.contains("Automatikus", ignoreCase = true) -> "A"
                else -> "A"
            }
            val replanned = mutableListOf<CalendarDayEvent>()
            var d = startDayNum
            while (d <= 31) {
                replanned.add(
                    CalendarDayEvent(
                        id = "ev_replan_${d}_${System.currentTimeMillis()}",
                        dayOfMonth = d,
                        timeLabel = state.planningTime,
                        titleHu = "$activeProgName • Alap $split",
                        exerciseCount = 6,
                        durationMinutes = state.planningDurationMin,
                        status = CalendarEventStatus.PLANNED,
                        splitBadge = split
                    )
                )
                split = if (split == "A") "B" else "A"
                d += 2
            }

            state.copy(
                calendarUndoStack = (state.calendarUndoStack + listOf(state.calendarEvents)).takeLast(12),
                planningStartDate = newStartDate,
                planningFirstDayHu = firstDayHu,
                calendarEvents = (retained + replanned).sortedBy { it.dayOfMonth },
                selectedCalendarDay = startDayNum,
                showReplanModal = false,
                statusBannerMessage = "Terv frissítve ($newStartDate • Alap ${replanned.firstOrNull()?.splitBadge ?: "A"})"
            )
        }
    }

    fun modifyCalendarEvent(
        eventId: String,
        newTitleHu: String,
        newSplitBadge: String,
        newTimeLabel: String,
        newDurationMin: Int
    ) {
        _uiState.update { state ->
            val updated = state.calendarEvents.map { ev ->
                if (ev.id == eventId) {
                    ev.copy(
                        titleHu = newTitleHu,
                        splitBadge = newSplitBadge,
                        timeLabel = newTimeLabel,
                        durationMinutes = newDurationMin
                    )
                } else ev
            }
            state.copy(
                calendarUndoStack = (state.calendarUndoStack + listOf(state.calendarEvents)).takeLast(12),
                calendarEvents = updated,
                statusBannerMessage = "Edzésnap módosítva ($newSplitBadge nap • $newTimeLabel)"
            )
        }
    }

    fun moveCalendarEvent(eventId: String, targetDay: Int, shiftFollowingPlanned: Boolean = true) {
        _uiState.update { state ->
            val target = state.calendarEvents.firstOrNull { it.id == eventId } ?: return@update state
            val clampedDay = targetDay.coerceIn(1, 31)
            val delta = clampedDay - target.dayOfMonth
            val updated = state.calendarEvents.mapNotNull { ev ->
                when {
                    ev.id == eventId -> ev.copy(dayOfMonth = clampedDay)
                    shiftFollowingPlanned && ev.status == CalendarEventStatus.PLANNED && ev.dayOfMonth > target.dayOfMonth -> {
                        val nextD = ev.dayOfMonth + delta
                        if (nextD in 1..31) ev.copy(dayOfMonth = nextD) else null
                    }
                    else -> ev
                }
            }.sortedBy { it.dayOfMonth }

            state.copy(
                calendarUndoStack = (state.calendarUndoStack + listOf(state.calendarEvents)).takeLast(12),
                calendarEvents = updated,
                selectedCalendarDay = clampedDay,
                statusBannerMessage = "Edzés áthelyezve: 2026. 10. ${clampedDay}."
            )
        }
    }

    fun deleteCalendarEvent(eventId: String) {
        _uiState.update { state ->
            val updated = state.calendarEvents.filterNot { it.id == eventId }
            state.copy(
                calendarUndoStack = (state.calendarUndoStack + listOf(state.calendarEvents)).takeLast(12),
                calendarEvents = updated,
                statusBannerMessage = "Tervezett edzés törölve (Visszavonható)"
            )
        }
    }

    fun addPlannedWorkoutOnDay(day: Int, splitBadge: String, timeLabel: String, durationMin: Int) {
        _uiState.update { state ->
            val activeProgName = state.programs.firstOrNull { it.isActive }?.nameHu
                ?: "Otthoni A/B – Alap (személyre szabott)"
            val newEv = CalendarDayEvent(
                id = "ev_manual_${day}_${System.currentTimeMillis()}",
                dayOfMonth = day.coerceIn(1, 31),
                timeLabel = timeLabel,
                titleHu = "$activeProgName • Alap $splitBadge",
                exerciseCount = 6,
                durationMinutes = durationMin,
                status = CalendarEventStatus.PLANNED,
                splitBadge = splitBadge
            )
            state.copy(
                calendarUndoStack = (state.calendarUndoStack + listOf(state.calendarEvents)).takeLast(12),
                calendarEvents = (state.calendarEvents + newEv).sortedBy { it.dayOfMonth },
                statusBannerMessage = "Edzés hozzáadva: 2026. 10. $day. (Alap $splitBadge)"
            )
        }
    }

    fun undoCalendarAction() {
        _uiState.update { state ->
            val previous = state.calendarUndoStack.lastOrNull() ?: return@update state.copy(
                statusBannerMessage = "Nincs visszavonható naptárművelet"
            )
            state.copy(
                calendarEvents = previous,
                calendarUndoStack = state.calendarUndoStack.dropLast(1),
                statusBannerMessage = "Utolsó naptárművelet visszavonva"
            )
        }
    }

    // Exercise Library Actions
    fun updateExerciseSearch(query: String) {
        _uiState.update { it.copy(exerciseSearchQuery = query) }
    }

    fun selectExerciseCategory(cat: ExerciseCategory) {
        _uiState.update { it.copy(selectedExerciseCategory = cat) }
    }

    fun openExerciseDetail(exerciseId: String) {
        _uiState.update {
            val back = (it.backStack + NavRoute(it.currentTab, it.currentSubScreen)).takeLast(16)
            it.copy(
                selectedExerciseId = exerciseId,
                currentSubScreen = SubScreen.EXERCISE_DETAIL,
                backStack = back
            )
        }
    }

    // Active Workout Actions
    fun toggleSetCompleted(setNumber: Int) {
        _uiState.update { state ->
            val updated = state.activeSets.map { s ->
                if (s.setNumber == setNumber) s.copy(completed = !s.completed) else s
            }
            state.copy(
                activeSets = updated,
                restTimerRemainingSec = state.defaultRestSec,
                restTimerRunning = true
            )
        }
    }

    fun adjustSetValues(setNumber: Int, deltaWeight: Float, deltaReps: Int, deltaRir: Int) {
        _uiState.update { state ->
            val updated = state.activeSets.map { s ->
                if (s.setNumber == setNumber) {
                    s.copy(
                        weightKg = (s.weightKg + deltaWeight).coerceAtLeast(0f),
                        reps = (s.reps + deltaReps).coerceIn(1, 50),
                        rir = (s.rir + deltaRir).coerceIn(0, 10)
                    )
                } else s
            }
            state.copy(activeSets = updated)
        }
    }

    fun updateSetValuesExact(setNumber: Int, weightKg: Float, reps: Int, rir: Int) {
        _uiState.update { state ->
            val updated = state.activeSets.map { s ->
                if (s.setNumber == setNumber) {
                    s.copy(
                        weightKg = weightKg.coerceAtLeast(0f),
                        reps = reps.coerceIn(1, 100),
                        rir = rir.coerceIn(0, 10)
                    )
                } else s
            }
            state.copy(activeSets = updated)
        }
    }

    fun toggleAutoMode(enabled: Boolean) {
        _uiState.update { it.copy(autoModeEnabled = enabled) }
    }

    fun deleteSet(setNumber: Int) {
        _uiState.update { state ->
            val remaining = state.activeSets.filter { it.setNumber != setNumber }
            val renumbered = remaining.mapIndexed { idx, item ->
                item.copy(setNumber = idx + 1)
            }
            state.copy(
                activeSets = renumbered,
                statusBannerMessage = "$setNumber. sorozat törölve"
            )
        }
    }

    fun addSet() {
        _uiState.update { state ->
            val lastSet = state.activeSets.lastOrNull()
            val nextNumber = (lastSet?.setNumber ?: 0) + 1
            val newSet = ExerciseSetItem(
                setNumber = nextNumber,
                weightKg = lastSet?.weightKg ?: 5.5f,
                reps = lastSet?.reps ?: 20,
                rir = lastSet?.rir ?: 0,
                completed = false
            )
            state.copy(
                activeSets = state.activeSets + newSet,
                statusBannerMessage = "$nextNumber. sorozat hozzáadva"
            )
        }
    }

    fun saveInitialProfileAndPlan(
        heightCm: Int,
        weightKg: Float,
        goalHu: String,
        experienceHu: String,
        locationHu: String,
        equipmentHu: String,
        daysPerWeek: Int,
        durationMin: Int,
        excludedMuscles: Set<String>,
        excludedExercises: Set<String>
    ) {
        _uiState.update { state ->
            val generatedProgramId = "custom_plan_" + System.currentTimeMillis()
            val dayAExercises = state.exercises.filter { ex ->
                ex.nameHu !in excludedExercises &&
                (excludedMuscles.isEmpty() || !excludedMuscles.any { ex.primaryMusclesHu.contains(it, ignoreCase = true) })
            }.take(5).ifEmpty { state.exercises.take(5) }

            val dayBExercises = state.exercises.filter { ex ->
                ex.nameHu !in excludedExercises &&
                ex !in dayAExercises &&
                (excludedMuscles.isEmpty() || !excludedMuscles.any { ex.primaryMusclesHu.contains(it, ignoreCase = true) })
            }.take(5).ifEmpty { state.exercises.drop(5).take(5) }

            val newProgram = TrainingProgramModel(
                id = generatedProgramId,
                nameHu = "Egyéni $goalHu Terv ($locationHu)",
                nameEn = "Personalized $goalHu Plan",
                subtitleHu = "$experienceHu • Heti $daysPerWeek nap • $durationMin perc",
                subtitleEn = "$experienceHu • $daysPerWeek days/week",
                tagsHu = listOf(goalHu, locationHu, "Egyéni"),
                isCustom = true,
                isActive = true,
                days = listOf(
                    com.example.model.ProgramDayModel(
                        id = "${generatedProgramId}_dayA",
                        dayCode = "A nap",
                        titleHu = "A nap • Fő fókusz",
                        titleEn = "Day A",
                        durationMinutes = durationMin,
                        exercises = dayAExercises
                    ),
                    com.example.model.ProgramDayModel(
                        id = "${generatedProgramId}_dayB",
                        dayCode = "B nap",
                        titleHu = "B nap • Kiegészítő fókusz",
                        titleEn = "Day B",
                        durationMinutes = durationMin,
                        exercises = dayBExercises
                    )
                )
            )

            val updatedPrograms = listOf(newProgram) + state.programs.map { it.copy(isActive = false) }

            state.copy(
                userHeightCm = heightCm,
                userWeightKg = weightKg,
                userFitnessGoalHu = goalHu,
                userExperienceLevelHu = experienceHu,
                userWorkoutLocationHu = locationHu,
                userSelectedEquipmentHu = equipmentHu,
                userScheduleDaysPerWeek = daysPerWeek,
                userWorkoutDurationMin = durationMin,
                userExcludedMusclesHu = excludedMuscles,
                userExcludedExercisesHu = excludedExercises,
                programs = updatedPrograms,
                editingProgramId = generatedProgramId,
                showPersonalPlannerModal = false,
                statusBannerMessage = "Egyéni terv elmentve és aktiválva!"
            )
        }
    }

    fun undoLastCompletedSet() {
        _uiState.update { state ->
            val lastCompleted = state.activeSets.lastOrNull { it.completed }
            if (lastCompleted == null) state
            else {
                state.copy(
                    activeSets = state.activeSets.map {
                        if (it.setNumber == lastCompleted.setNumber) it.copy(completed = false) else it
                    },
                    statusBannerMessage = "${lastCompleted.setNumber}. sorozat visszavonva"
                )
            }
        }
    }

    fun adjustRestTimer(deltaSec: Int) {
        _uiState.update {
            it.copy(restTimerRemainingSec = (it.restTimerRemainingSec + deltaSec).coerceIn(0, 600))
        }
    }

    fun toggleRestTimerRunning() {
        _uiState.update { it.copy(restTimerRunning = !it.restTimerRunning) }
    }

    fun skipRestTimer() {
        _uiState.update { it.copy(restTimerRemainingSec = 0, restTimerRunning = false) }
    }

    fun updateIntervalSettings(workSec: Int, restSec: Int, rounds: Int, sound: Boolean, vibration: Boolean, autoStep: Boolean) {
        _uiState.update {
            it.copy(
                intervalWorkSec = workSec.coerceIn(10, 300),
                intervalRestSec = restSec.coerceIn(10, 300),
                intervalRounds = rounds.coerceIn(1, 20),
                soundAlertEnabled = sound,
                vibrationEnabled = vibration,
                autoStepIntervals = autoStep,
                statusBannerMessage = "Intervallumok: ${workSec}mp munka / ${restSec}mp pihenő × $rounds"
            )
        }
    }

    fun selectWorkoutSummaryMood(index: Int) {
        _uiState.update { it.copy(workoutSummaryMoodIndex = index) }
    }

    fun updateWorkoutSummaryNote(note: String) {
        _uiState.update { it.copy(workoutSummaryNote = note) }
    }

    fun startPlannedWorkout(programId: String? = null, dayIndex: Int = 0) {
        _uiState.update { state ->
            val prog = if (programId != null) {
                state.programs.find { it.id == programId }
            } else {
                state.programs.firstOrNull { it.isActive } ?: state.programs.firstOrNull()
            }
            val day = prog?.days?.getOrNull(dayIndex) ?: prog?.days?.firstOrNull()
            val dayExercises = day?.exercises?.ifEmpty { state.exercises.take(6) } ?: state.exercises.take(6)
            val firstEx = dayExercises.firstOrNull()
            val setsMap = dayExercises.associate { ex -> ex.id to buildDefaultSetsForExercise(ex) }
            val firstSets = if (firstEx != null) setsMap[firstEx.id] ?: buildDefaultSetsForExercise(firstEx) else state.activeSets
            val targetSub = if (firstEx?.isTimed == true) SubScreen.ACTIVE_TIMED else SubScreen.ACTIVE_STRENGTH
            val back = (state.backStack + NavRoute(state.currentTab, state.currentSubScreen)).takeLast(16)
            state.copy(
                isWorkoutSessionActive = true,
                activeWorkoutSubScreen = targetSub,
                activeWorkoutTitleHu = if (prog != null && day != null) "${prog.nameHu} • ${day.dayCode}" else "Tervezett edzés",
                activeWorkoutExercises = dayExercises,
                activeExerciseIndex = 0,
                selectedExerciseId = firstEx?.id ?: state.selectedExerciseId,
                activeSets = firstSets,
                activeSetsByExerciseId = setsMap,
                timedCurrentSec = 0,
                currentSubScreen = targetSub,
                backStack = back,
                showQuickWorkoutSheet = false,
                statusBannerMessage = "Tervezett edzés elindítva: ${dayExercises.size} gyakorlat a sorban"
            )
        }
    }

    fun startWorkoutWithExercises(exercisesToRun: List<ExerciseItem>, titleHu: String = "Gyors edzés") {
        if (exercisesToRun.isEmpty()) return
        _uiState.update { state ->
            val firstEx = exercisesToRun.first()
            val setsMap = exercisesToRun.associate { ex -> ex.id to buildDefaultSetsForExercise(ex) }
            val firstSets = setsMap[firstEx.id] ?: buildDefaultSetsForExercise(firstEx)
            val targetSub = if (firstEx.isTimed) SubScreen.ACTIVE_TIMED else SubScreen.ACTIVE_STRENGTH
            val back = (state.backStack + NavRoute(state.currentTab, state.currentSubScreen)).takeLast(16)
            state.copy(
                isWorkoutSessionActive = true,
                activeWorkoutSubScreen = targetSub,
                activeWorkoutTitleHu = titleHu,
                activeWorkoutExercises = exercisesToRun,
                activeExerciseIndex = 0,
                selectedExerciseId = firstEx.id,
                activeSets = firstSets,
                activeSetsByExerciseId = setsMap,
                timedCurrentSec = 0,
                currentSubScreen = targetSub,
                backStack = back,
                showQuickWorkoutSheet = false,
                statusBannerMessage = "$titleHu elindítva: ${firstEx.nameHu} (1 / ${exercisesToRun.size})"
            )
        }
    }

    fun startQuickStrengthWithExercise(exercise: ExerciseItem) {
        val currentQueue = _uiState.value.quickWorkoutQueue
        val listToRun = if (currentQueue.any { it.id == exercise.id }) {
            listOf(exercise) + currentQueue.filter { it.id != exercise.id }
        } else if (currentQueue.isNotEmpty()) {
            listOf(exercise) + currentQueue
        } else {
            listOf(exercise)
        }
        startWorkoutWithExercises(listToRun, "Gyors súlyzós edzés")
    }

    fun startQuickTimedWithExercise(exercise: ExerciseItem) {
        val timedEx = if (exercise.isTimed) exercise else exercise.copy(isTimed = true, targetSeconds = 45)
        _uiState.update { state ->
            val back = (state.backStack + NavRoute(state.currentTab, state.currentSubScreen)).takeLast(16)
            state.copy(
                isWorkoutSessionActive = true,
                activeWorkoutSubScreen = SubScreen.ACTIVE_TIMED,
                activeWorkoutTitleHu = "Időalapú edzés • ${timedEx.nameHu}",
                activeWorkoutExercises = listOf(timedEx),
                activeExerciseIndex = 0,
                selectedExerciseId = timedEx.id,
                timedCurrentSec = 0,
                currentSubScreen = SubScreen.ACTIVE_TIMED,
                backStack = back,
                showQuickWorkoutSheet = false,
                statusBannerMessage = "Időalapú edzés elindítva: ${timedEx.nameHu}"
            )
        }
    }

    fun addExerciseToWorkoutQueue(exercise: ExerciseItem) {
        _uiState.update { state ->
            val updatedQueue = if (state.quickWorkoutQueue.any { it.id == exercise.id }) {
                state.quickWorkoutQueue
            } else {
                state.quickWorkoutQueue + exercise
            }
            val updatedActive = if (state.activeWorkoutExercises.any { it.id == exercise.id }) {
                state.activeWorkoutExercises
            } else {
                state.activeWorkoutExercises + exercise
            }
            val updatedSetsMap = state.activeSetsByExerciseId + (exercise.id to buildDefaultSetsForExercise(exercise))
            state.copy(
                quickWorkoutQueue = updatedQueue,
                activeWorkoutExercises = updatedActive,
                activeSetsByExerciseId = updatedSetsMap,
                statusBannerMessage = "„${exercise.nameHu}” hozzáadva az edzéshez (${updatedQueue.size} gyakorlat kiválasztva)"
            )
        }
    }

    fun removeExerciseFromWorkoutQueue(exerciseId: String) {
        _uiState.update { state ->
            state.copy(
                quickWorkoutQueue = state.quickWorkoutQueue.filterNot { it.id == exerciseId }
            )
        }
    }

    fun startQueuedWorkout() {
        val queue = _uiState.value.quickWorkoutQueue
        if (queue.isNotEmpty()) {
            startWorkoutWithExercises(queue, "Egyéni összeállított edzés")
        } else {
            startPlannedWorkout()
        }
    }

    fun switchCurrentActiveExercise(exercise: ExerciseItem) {
        _uiState.update { state ->
            val currentList = state.activeWorkoutExercises.toMutableList()
            val existingIdx = currentList.indexOfFirst { it.id == exercise.id }
            val newIndex: Int
            if (existingIdx >= 0) {
                newIndex = existingIdx
            } else {
                val replaceAt = state.activeExerciseIndex.coerceIn(0, currentList.size)
                if (replaceAt < currentList.size) {
                    currentList[replaceAt] = exercise
                    newIndex = replaceAt
                } else {
                    currentList.add(exercise)
                    newIndex = currentList.lastIndex
                }
            }
            val savedSetsMap = state.activeSetsByExerciseId.toMutableMap()
            val prevEx = state.activeWorkoutExercises.getOrNull(state.activeExerciseIndex)
            if (prevEx != null) {
                savedSetsMap[prevEx.id] = state.activeSets
            }
            val nextSets = savedSetsMap[exercise.id] ?: buildDefaultSetsForExercise(exercise)
            savedSetsMap[exercise.id] = nextSets
            state.copy(
                activeWorkoutExercises = currentList,
                activeExerciseIndex = newIndex,
                selectedExerciseId = exercise.id,
                activeSets = nextSets,
                activeSetsByExerciseId = savedSetsMap,
                timedCurrentSec = 0,
                statusBannerMessage = "Aktuális gyakorlat: ${exercise.nameHu} (${newIndex + 1} / ${currentList.size})"
            )
        }
    }

    fun nextActiveExercise() {
        val state = _uiState.value
        val workoutExs = state.activeWorkoutExercises.ifEmpty { state.exercises.take(6) }
        val currentEx = workoutExs.getOrNull(state.activeExerciseIndex)
        val updatedSetsMap = state.activeSetsByExerciseId.toMutableMap()
        if (currentEx != null) {
            updatedSetsMap[currentEx.id] = state.activeSets
        }

        if (state.activeExerciseIndex + 1 < workoutExs.size) {
            val nextIdx = state.activeExerciseIndex + 1
            val nextEx = workoutExs[nextIdx]
            val nextSets = updatedSetsMap[nextEx.id] ?: buildDefaultSetsForExercise(nextEx)
            updatedSetsMap[nextEx.id] = nextSets
            val targetSub = if (nextEx.isTimed) SubScreen.ACTIVE_TIMED else SubScreen.ACTIVE_STRENGTH
            _uiState.update {
                it.copy(
                    activeWorkoutExercises = workoutExs,
                    activeExerciseIndex = nextIdx,
                    selectedExerciseId = nextEx.id,
                    activeSets = nextSets,
                    activeSetsByExerciseId = updatedSetsMap,
                    timedCurrentSec = 0,
                    currentSubScreen = targetSub,
                    statusBannerMessage = "Következő gyakorlat (${nextIdx + 1}/${workoutExs.size}): ${nextEx.nameHu}"
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    activeSetsByExerciseId = updatedSetsMap,
                    currentSubScreen = SubScreen.WORKOUT_SUMMARY,
                    statusBannerMessage = "Minden gyakorlat teljesítve (${workoutExs.size}/${workoutExs.size})! Összegzés."
                )
            }
        }
    }

    fun previousActiveExercise() {
        val state = _uiState.value
        val workoutExs = state.activeWorkoutExercises.ifEmpty { state.exercises.take(6) }
        val currentEx = workoutExs.getOrNull(state.activeExerciseIndex)
        val updatedSetsMap = state.activeSetsByExerciseId.toMutableMap()
        if (currentEx != null) {
            updatedSetsMap[currentEx.id] = state.activeSets
        }

        if (state.activeExerciseIndex > 0) {
            val prevIdx = state.activeExerciseIndex - 1
            val prevEx = workoutExs[prevIdx]
            val prevSets = updatedSetsMap[prevEx.id] ?: buildDefaultSetsForExercise(prevEx)
            val targetSub = if (prevEx.isTimed) SubScreen.ACTIVE_TIMED else SubScreen.ACTIVE_STRENGTH
            _uiState.update {
                it.copy(
                    activeExerciseIndex = prevIdx,
                    selectedExerciseId = prevEx.id,
                    activeSets = prevSets,
                    activeSetsByExerciseId = updatedSetsMap,
                    currentSubScreen = targetSub,
                    statusBannerMessage = "Előző gyakorlat (${prevIdx + 1}/${workoutExs.size}): ${prevEx.nameHu}"
                )
            }
        } else {
            navigateBack()
        }
    }

    fun updateOutdoorWorkoutMetrics(elapsedLabel: String, distanceKm: Float, paceLabel: String) {
        _uiState.update {
            it.copy(
                outdoorElapsedLabel = elapsedLabel,
                outdoorDistanceKm = distanceKm,
                outdoorPaceLabel = paceLabel
            )
        }
    }

    fun finishAndSaveWorkout() {
        val state = _uiState.value
        val now = Instant.now()
        val nowMs = System.currentTimeMillis()
        val workoutId = UUID.randomUUID().toString()
        val workoutExs = state.activeWorkoutExercises.ifEmpty {
            listOfNotNull(state.exercises.getOrNull(state.activeExerciseIndex) ?: state.exercises.firstOrNull())
        }
        val currentEx = workoutExs.getOrNull(state.activeExerciseIndex)
        val allSetsMap = state.activeSetsByExerciseId.toMutableMap()
        if (currentEx != null) {
            allSetsMap[currentEx.id] = state.activeSets
        }

        var totalSetsSaved = 0
        var totalVolume = 0
        val exercisesArray = JSONArray()
        workoutExs.forEachIndexed { exIdx, exItem ->
            val setsForEx = allSetsMap[exItem.id] ?: buildDefaultSetsForExercise(exItem)
            val completedSets = setsForEx.filter { it.completed }
            val setsToSave = if (completedSets.isNotEmpty()) completedSets else setsForEx
            totalSetsSaved += setsToSave.size
            totalVolume += setsToSave.sumOf { (it.weightKg * it.reps).toInt() }

            val exObj = JSONObject().apply {
                put("id", exItem.id)
                put("exerciseId", exItem.id)
                put("nameHu", exItem.nameHu)
                put("nameEn", exItem.nameEn)
                put("loadType", "weight")
                put("repUnit", if (exItem.isTimed) "sec" else "reps")
                put("measurementType", if (exItem.isTimed) "time" else "reps")
                put("orderIndex", exIdx)
                val setsArray = JSONArray()
                setsToSave.forEachIndexed { idx, s ->
                    setsArray.put(JSONObject().apply {
                        put("setId", UUID.randomUUID().toString())
                        put("set", idx + 1)
                        put("setNumber", s.setNumber)
                        put("weight", s.weightKg.toDouble())
                        put("reps", s.reps.toString())
                        put("done", s.completed)
                        put("actualRir", s.rir)
                    })
                }
                put("sets", setsArray)
            }
            exercisesArray.put(exObj)
        }

        val summaryNames = workoutExs.joinToString(", ") { it.nameHu }.ifBlank { "Edzés" }
        val workoutTitle = state.activeWorkoutTitleHu.ifBlank { workoutExs.firstOrNull()?.nameHu ?: "Edzés" }
        val durationMins = maxOf(15, state.timedCurrentSec / 60)

        val ctx = nativeContext
        val db = nativeDatabase
        val store = nativeSettingsStore
        val scope = nativeScope

        if (ctx == null || db == null || store == null || scope == null) {
            _uiState.update { it.copy(statusBannerMessage = "Hiba: A natív edzésmotor nem elérhető!") }
            return
        }

        scope.launch(Dispatchers.IO) {
            try {
                val startedMs = nowMs - (durationMins * 60 * 1000L)
                val candidateJson = JSONObject().apply {
                    put("schema", 1)
                    put("active", true)
                    put("id", workoutId)
                    put("workoutId", workoutId)
                    put("workoutKey", workoutId)
                    put("syncId", workoutId)
                    put("started", Instant.ofEpochMilli(startedMs).toString())
                    put("finished", now.toString())
                    put("startedEpochMs", startedMs)
                    put("finishedEpochMs", nowMs)
                    put("programId", state.editingProgramId.ifBlank { "program_custom" })
                    put("programName", workoutTitle)
                    put("dayId", "session")
                    put("dayName", workoutTitle)
                    put("workout", workoutTitle)
                    put("totalVolumeKg", totalVolume.toDouble())
                    put("completedSetsCount", totalSetsSaved)
                    put("totalSetsCount", totalSetsSaved)
                    put("timerOwner", "phone")
                    put("originDevice", "phone")
                    put("workoutPaused", false)
                    put("exercises", exercisesArray)
                    if (state.workoutSummaryNote.isNotBlank() || state.workoutSummaryMoodIndex >= 0) {
                        put("feedback", JSONObject().apply {
                            put("effort", when (state.workoutSummaryMoodIndex) {
                                0 -> "könnyű"
                                1 -> "mérsékelt"
                                2 -> "pont jó"
                                3 -> "nehéz"
                                else -> "maximális"
                            })
                            put("note", state.workoutSummaryNote)
                            put("updatedAt", nowMs)
                        })
                    }
                }

                // Check or install session in ActiveWorkoutStore & PhoneWorkoutExecution
                val existingActive = ActiveWorkoutStore.current(ctx)
                val activeToFinalize = if (existingActive?.optBoolean("active") == true && existingActive.optString("timerOwner", "phone") == "phone") {
                    existingActive.put("finished", now.toString())
                    existingActive.put("finishedEpochMs", nowMs)
                    if (candidateJson.has("feedback")) existingActive.put("feedback", candidateJson.getJSONObject("feedback"))
                    if (!existingActive.has("exercises") || existingActive.getJSONArray("exercises").length() == 0) {
                        existingActive.put("exercises", exercisesArray)
                    }
                    existingActive
                } else {
                    withContext(Dispatchers.Main.immediate) {
                        NativeWorkoutSession.installPrepared(ctx, candidateJson)
                    }
                }

                val finalizer = TrainPilotWorkoutFinalization(ctx, db)
                val saved = finalizer.finishWorkout(activeToFinalize)

                // Verify persistence by querying Room directly
                val finalWorkoutId = saved.history.optString("workoutId", workoutId)
                val savedInDb = db.workoutHistoryDao().getWorkoutWithDetails(finalWorkoutId)
                if (savedInDb != null) {
                    saved.closedSnapshot?.let { runCatching { WearSyncRepository.publishActiveWorkoutSnapshot(ctx, it) } }
                    runCatching { WearSyncRepository.refreshWatchHomeAsync(ctx) }
                    runCatching { WearSyncRepository.publishWatchHomeSnapshot(ctx) }

                    val newEntry = LoggedWorkoutEntry(
                        id = finalWorkoutId,
                        dateLabelHu = DateTimeFormatter.ofPattern("MMM d., E • HH:mm", Locale.getDefault()).format(java.time.LocalDateTime.now()),
                        titleHu = workoutTitle,
                        durationMinutes = durationMins,
                        workTimeLabel = String.format("%02d:%02d", durationMins, 0),
                        restTimeLabel = "00:00",
                        totalSets = totalSetsSaved,
                        totalVolumeKg = totalVolume,
                        summaryExercisesHu = summaryNames,
                        hasPr = savedInDb.workout.hasPr226,
                        isOutdoorRun = false
                    )

                    withContext(Dispatchers.Main) {
                        _uiState.update { st ->
                            st.copy(
                                isWorkoutSessionActive = false,
                                loggedWorkouts = listOf(newEntry) + st.loggedWorkouts,
                                weeklyGoalCompleted = st.weeklyGoalCompleted + 1,
                                quickWorkoutQueue = emptyList(),
                                currentTab = MainTab.LOG,
                                currentSubScreen = SubScreen.NONE,
                                statusBannerMessage = "Edzés sikeresen lezárva és mentve a Room adatbázisba!"
                            )
                        }
                    }
                    loadMuscleLoadSummary(90)
                } else {
                    withContext(Dispatchers.Main) {
                        _uiState.update { st ->
                            st.copy(statusBannerMessage = "Hiba: Az edzés mentése nem igazolható vissza a Room adatbázisból!")
                        }
                    }
                }
            } catch (e: Throwable) {
                android.util.Log.e("TrainPilotViewModel", "Failed to finalize workout canonically", e)
                withContext(Dispatchers.Main) {
                    _uiState.update { st ->
                        st.copy(statusBannerMessage = "Hiba az edzés mentésekor: ${e.localizedMessage ?: "adatbázishiba"}")
                    }
                }
            }
        }
    }

    // Log / Diary Actions
    fun updateLogSearch(query: String) {
        _uiState.update { it.copy(logSearchQuery = query) }
    }

    fun openLogWorkoutDetail(workoutId: String) {
        _uiState.update {
            val back = (it.backStack + NavRoute(it.currentTab, it.currentSubScreen)).takeLast(16)
            it.copy(
                selectedLogWorkoutId = workoutId,
                currentSubScreen = SubScreen.LOG_WORKOUT_DETAIL,
                backStack = back
            )
        }
    }

    fun selectLogDetailTab(index: Int) {
        _uiState.update { it.copy(logDetailTabIndex = index) }
    }

    fun toggleExpandedLogExercise(index: Int) {
        _uiState.update {
            it.copy(expandedLogExerciseIndex = if (it.expandedLogExerciseIndex == index) -1 else index)
        }
    }

    // Progress / Fejlődés Actions
    fun selectProgressMainSection(section: Int) {
        _uiState.update { it.copy(progressMainSection = section) }
    }

    fun selectProgressExercise(name: String) {
        _uiState.update { it.copy(selectedProgressExercise = name) }
    }

    fun selectProgressRange(index: Int) {
        _uiState.update { it.copy(selectedProgressRangeIndex = index) }
    }

    fun selectVolumeBar(index: Int) {
        _uiState.update { it.copy(selectedVolumeBarIndex = index) }
    }

    fun selectExpandedStatsCard(index: Int) {
        _uiState.update {
            it.copy(expandedStatsCardIndex = if (it.expandedStatsCardIndex == index) -1 else index)
        }
    }

    fun selectStatsSubTab(index: Int) {
        _uiState.update { it.copy(statsSubTab = index) }
    }

    fun toggleCardioFilterStepsMode(stepsMode: Boolean) {
        _uiState.update { it.copy(cardioFilterStepsMode = stepsMode) }
    }

    fun openExerciseVideoModal(exerciseTitle: String) {
        _uiState.update { it.copy(videoModalExerciseTitle = exerciseTitle) }
    }

    fun closeExerciseVideoModal() {
        _uiState.update { it.copy(videoModalExerciseTitle = null) }
    }

    // Health / Egészség Actions
    fun openHealthAndExpand(
        pulse: Boolean = false,
        weight: Boolean = false,
        extra: Boolean = false,
        journal: Boolean = false,
        connect: Boolean = false
    ) {
        _uiState.update { state ->
            val back = (state.backStack + NavRoute(state.currentTab, state.currentSubScreen)).takeLast(16)
            state.copy(
                currentTab = MainTab.HEALTH,
                currentSubScreen = SubScreen.NONE,
                backStack = back,
                expandedHealthPulseTrend = pulse || state.expandedHealthPulseTrend,
                expandedHealthWeightLog = weight || state.expandedHealthWeightLog,
                expandedHealthExtraData = extra || state.expandedHealthExtraData,
                expandedHealthJournal = journal || state.expandedHealthJournal,
                expandedHealthConnect = connect || state.expandedHealthConnect
            )
        }
    }

    fun toggleHealthSection(section: String) {
        _uiState.update { state ->
            when (section) {
                "pulse" -> state.copy(expandedHealthPulseTrend = !state.expandedHealthPulseTrend)
                "weight" -> state.copy(expandedHealthWeightLog = !state.expandedHealthWeightLog)
                "extra" -> state.copy(expandedHealthExtraData = !state.expandedHealthExtraData)
                "journal" -> state.copy(expandedHealthJournal = !state.expandedHealthJournal)
                "connect" -> state.copy(expandedHealthConnect = !state.expandedHealthConnect)
                else -> state
            }
        }
    }

    fun selectWeightRange(index: Int) {
        _uiState.update { it.copy(selectedWeightRangeIndex = index) }
    }

    fun toggleAddWeightDialog(show: Boolean) {
        _uiState.update { it.copy(showAddWeightDialog = show) }
    }

    private fun parseUserEnteredDateToLocalDate(rawDate: String): LocalDate {
        val cleaned = rawDate.trim()
        if (cleaned.isBlank()) return LocalDate.now()
        val digits = Regex("\\d+").findAll(cleaned).mapNotNull { it.value.toIntOrNull() }.toList()
        if (digits.size >= 3) {
            val y = if (digits[0] in 1970..2100) digits[0] else LocalDate.now().year
            val m = digits[1].coerceIn(1, 12)
            val d = digits[2].coerceIn(1, 28)
            return runCatching { LocalDate.of(y, m, digits[2].coerceIn(1, 31)) }.getOrElse { LocalDate.of(y, m, d) }
        }
        return LocalDate.now()
    }

    fun saveNewBodyWeight(weightKg: Float, dateStr: String = "") {
        if (weightKg <= 20f || weightKg >= 350f) {
            _uiState.update { it.copy(statusBannerMessage = "Érvénytelen testsúly: 20 és 350 kg között adható meg!") }
            return
        }
        val parsedLocalDate = parseUserEnteredDateToLocalDate(dateStr)
        val dayKey = parsedLocalDate.toString()
        val isoDate = parsedLocalDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toString()
        val db = nativeDatabase
        val scope = nativeScope
        if (db == null || scope == null) {
            _uiState.update {
                it.copy(
                    currentBodyWeightKg = weightKg,
                    showAddWeightDialog = false,
                    statusBannerMessage = "Testsúlymérés rögzítve ($dayKey): $weightKg kg"
                )
            }
            return
        }
        scope.launch(Dispatchers.IO) {
            try {
                val entry = BodyWeightEntryEntity(
                    id = UUID.randomUUID().toString(),
                    date = isoDate,
                    dayKey = dayKey,
                    kg = weightKg.toDouble(),
                    updatedAt = parsedLocalDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    rawJson = JSONObject().put("kg", weightKg.toDouble()).put("date", isoDate).put("dayKey", dayKey).toString()
                )
                db.bodyWeightDao().upsertWeight(entry)
                val verified = db.bodyWeightDao().getWeightJournalSummary()
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            currentBodyWeightKg = verified.currentKg?.toFloat() ?: weightKg,
                            showAddWeightDialog = false,
                            statusBannerMessage = "Testsúlymérés sikeresen elmentve ($dayKey): $weightKg kg"
                        )
                    }
                }
            } catch (e: Throwable) {
                android.util.Log.e("TrainPilotViewModel", "Failed to save body weight to Room", e)
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(statusBannerMessage = "Nem sikerült elmenteni a testsúlymérést: ${e.localizedMessage ?: "adatbázishiba"}")
                    }
                }
            }
        }
    }

    fun togglePrimaryHealthSource() {
        _uiState.update {
            val next = !it.primaryHealthSourceSamsung
            it.copy(
                primaryHealthSourceSamsung = next,
                statusBannerMessage = if (next) "Elsődleges forrás: Samsung Health" else "Elsődleges forrás: Health Connect"
            )
        }
    }

    fun triggerHealthSyncNow() {
        val ctx = nativeContext
        val scope = nativeScope
        if (ctx == null || scope == null) {
            _uiState.update {
                it.copy(statusBannerMessage = "Az egészségügyi szolgáltatások nem elérhetők.")
            }
            return
        }
        scope.launch(Dispatchers.IO) {
            try {
                HealthBackgroundJob.schedule(ctx)
                WearSyncRepository.publishWatchHomeSnapshot(ctx)
                val store = HealthJournalStore.get(ctx)
                val todayStr = LocalDate.now().toString()
                val page = store.page(LocalDate.now().minusDays(1).toString(), todayStr, "journal", "", 0L, 0L, 1)
                val summaries = page.optJSONArray("days")
                val daysList = (0 until (summaries?.length() ?: 0)).mapNotNull { summaries?.optJSONObject(it) }
                val todayData = daysList.firstOrNull { it.optString("day") == todayStr } ?: store.projection().optJSONObject("days")?.optJSONObject(todayStr)

                val stepsDbl = todayData?.optDouble("steps", Double.NaN) ?: Double.NaN
                val pulseDbl = todayData?.optDouble("averageHeartRate", Double.NaN) ?: Double.NaN
                val restingDbl = todayData?.optDouble("restingHeartRate", Double.NaN) ?: Double.NaN
                val caloriesDbl = todayData?.optDouble("activeCalories", Double.NaN) ?: Double.NaN
                val sleepMinDbl = todayData?.optDouble("sleepMinutes", Double.NaN) ?: Double.NaN

                val hasData = (stepsDbl.isFinite() && stepsDbl > 0) || (pulseDbl.isFinite() && pulseDbl > 0) || (sleepMinDbl.isFinite() && sleepMinDbl > 0)

                withContext(Dispatchers.Main) {
                    _uiState.update { st ->
                        st.copy(
                            dailySteps = if (stepsDbl.isFinite() && stepsDbl >= 0) stepsDbl.toInt() else st.dailySteps,
                            currentHrBpm = if (pulseDbl.isFinite() && pulseDbl > 0) pulseDbl.toInt() else st.currentHrBpm,
                            restingHrBpm = if (restingDbl.isFinite() && restingDbl > 0) restingDbl.toInt() else st.restingHrBpm,
                            activeCaloriesKcal = if (caloriesDbl.isFinite() && caloriesDbl > 0) caloriesDbl.toInt() else st.activeCaloriesKcal,
                            sleepDurationLabel = if (sleepMinDbl.isFinite() && sleepMinDbl > 0) String.format("%d ó %d p", (sleepMinDbl / 60).toInt(), (sleepMinDbl % 60).toInt()) else st.sleepDurationLabel,
                            sleepHoursDecimal = if (sleepMinDbl.isFinite() && sleepMinDbl > 0) "${((sleepMinDbl / 60.0) * 10.0).roundToInt() / 10.0} h" else st.sleepHoursDecimal,
                            statusBannerMessage = if (hasData) "Egészségügyi adatok sikeresen szinkronizálva!" else "Szinkronizáció lefutott. Nincs új elérhető adat a szolgáltatótól."
                        )
                    }
                }
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(statusBannerMessage = "Hiba az egészségszinkron során: ${e.localizedMessage ?: "szolgáltatói hiba"}")
                    }
                }
            }
        }
    }

    // Watch, Settings, Backup & Permissions Actions
    fun toggleWatchControlMode() {
        val ctx = nativeContext
        val scope = nativeScope
        if (ctx == null || scope == null) {
            _uiState.update { it.copy(statusBannerMessage = "A Wear OS szolgáltatás jelenleg nem elérhető.") }
            return
        }
        scope.launch(Dispatchers.IO) {
            try {
                val active = ActiveWorkoutStore.current(ctx)
                if (active == null || !active.optBoolean("active", false)) {
                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(
                                phoneControlsWorkout = true,
                                watchControlPending = false,
                                statusBannerMessage = "Nincs aktív edzés folyamatban az órára történő átadáshoz."
                            )
                        }
                    }
                    return@launch
                }
                val currentOwner = active.optString("timerOwner", "phone")
                if (currentOwner == "phone") {
                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(
                                watchControlPending = true,
                                statusBannerMessage = "Átadási kérelem elküldve az órának... Várakozás a nyugtázásra."
                            )
                        }
                    }
                    WearSyncRepository.publishWatchHomeSnapshot(ctx)
                    kotlinx.coroutines.delay(600)
                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(
                                phoneControlsWorkout = false,
                                watchControlPending = false,
                                lastWatchSyncLabel = "Átadva az órára",
                                statusBannerMessage = "Az edzésvezérlés sikeresen átadva az órának."
                            )
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(
                                watchControlPending = true,
                                statusBannerMessage = "Edzésvezérlés visszakérése a telefonra..."
                            )
                        }
                    }
                    runCatching {
                        PhoneOwnership193.journal(ctx).recover()
                        val resume = PhoneOwnership193.prepareResume(ctx)
                        val reqId = resume.optJSONObject("snapshot")?.optJSONObject("ownershipImport193")?.optString("id") ?: ""
                        val epoch = resume.optJSONObject("snapshot")?.optLong("ownerEpoch193") ?: 0L
                        PhoneOwnership193.installResume(ctx, reqId, epoch)
                    }
                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(
                                phoneControlsWorkout = true,
                                watchControlPending = false,
                                lastWatchSyncLabel = "Telefon vezérel",
                                statusBannerMessage = "Edzésvezérlés visszavéve a telefonra."
                            )
                        }
                    }
                }
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            watchControlPending = false,
                            statusBannerMessage = "Hiba az óra átadásakor: ${e.localizedMessage ?: "kapcsolati hiba"}"
                        )
                    }
                }
            }
        }
    }

    fun loadMuscleLoadSummary(days: Int = 90) {
        val db = nativeDatabase ?: return
        val scope = nativeScope ?: return
        val lang = _uiState.value.language.name.lowercase()
        scope.launch(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                val fromEpoch = now - (days * 24L * 3600L * 1000L)
                val summaries = db.workoutHistoryDao().getProgressMuscleSummary(fromEpoch, now)
                val models = NativeProgressQuery.muscle(summaries, lang)
                val items = models.map { m ->
                    val percentage = (m.loadPercentage * 100).toInt()
                    val level = when {
                        m.setsCount == 0 -> "Nincs terhelés"
                        m.loadPercentage >= 0.75f -> "Nagyobb terhelés"
                        m.loadPercentage >= 0.4f -> "Közepes terhelés"
                        else -> "Kisebb terhelés"
                    }
                    com.example.ui.components.MuscleLoadItem(
                        id = m.muscleKey,
                        nameHu = m.label,
                        setsText = if (m.setsCount > 0) "${m.setsCount} sorozat" else "0 sorozat",
                        percentage = percentage,
                        ratio = m.loadPercentage,
                        levelLabelHu = level,
                        topExercisesHu = emptyList(),
                        exerciseIdToOpen = ""
                    )
                }
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(realMuscleLoads = items, muscleLoadPeriodDays = days) }
                }
            } catch (e: Throwable) {
                android.util.Log.e("TrainPilotViewModel", "Failed to load real muscle summary", e)
            }
        }
    }

    fun exportBackup(context: Context, uri: Uri, format: String) {
        val scope = nativeScope ?: return
        scope.launch(Dispatchers.IO) {
            try {
                val mgr = BackupRestoreManager(context)
                val result = if (format.lowercase().contains("zip")) {
                    mgr.exportZipArchiveToUri(uri).map { "${it.first} (${it.second / 1024} KB)" }
                } else {
                    mgr.exportJsonBackupToUri(uri).map { "${it.first} (${it.second} bájt)" }
                }
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            statusBannerMessage = if (result.isSuccess) "Biztonsági mentés sikeresen elkészült: ${result.getOrNull()}"
                            else "Hiba a mentés során: ${result.exceptionOrNull()?.localizedMessage}"
                        )
                    }
                }
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(statusBannerMessage = "Nem sikerült a mentés: ${e.localizedMessage ?: "fájlírási hiba"}")
                    }
                }
            }
        }
    }

    fun importBackup(context: Context, uri: Uri) {
        val scope = nativeScope ?: return
        scope.launch(Dispatchers.IO) {
            try {
                val mgr = BackupRestoreManager(context)
                val isZip = uri.toString().endsWith(".zip", ignoreCase = true) || context.contentResolver.getType(uri)?.contains("zip") == true
                val result = if (isZip) {
                    mgr.importZipArchiveFromUri(uri, recoverInterrupted = false)
                } else {
                    mgr.importJsonBackupFromUri(uri, recoverInterrupted = false)
                }
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            statusBannerMessage = if (result.isSuccess) "Biztonsági mentés sikeresen visszaállítva!"
                            else "Hiba a visszaállítás során: ${result.exceptionOrNull()?.localizedMessage}"
                        )
                    }
                }
                nativeDatabase?.let { db ->
                    nativeSettingsStore?.let { store ->
                        bindNativeEnvironment(context, store, db, scope)
                    }
                }
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(statusBannerMessage = "Nem sikerült a visszaállítás: ${e.localizedMessage ?: "olvasási hiba"}")
                    }
                }
            }
        }
    }

    fun toggleSoundAlert(enabled: Boolean) {
        _uiState.update { it.copy(soundAlertEnabled = enabled) }
        val store = nativeSettingsStore
        val scope = nativeScope
        if (store != null && scope != null) {
            val current = _uiState.value
            scope.launch(Dispatchers.IO) {
                try {
                    store.setTimerDefaults(
                        workSeconds = 30,
                        restSeconds = current.defaultRestSec,
                        rounds = 3,
                        sound = enabled,
                        vibration = current.vibrationEnabled
                    )
                } catch (_: Throwable) {}
            }
        }
    }

    fun cycleDefaultRestTimer() {
        _uiState.update {
            val next = when (it.defaultRestSec) {
                60 -> 90
                90 -> 120
                120 -> 180
                else -> 60
            }
            it.copy(defaultRestSec = next, statusBannerMessage = "Alapértelmezett pihenőidő: $next mp")
        }
        val store = nativeSettingsStore
        val scope = nativeScope
        if (store != null && scope != null) {
            val current = _uiState.value
            scope.launch(Dispatchers.IO) {
                try {
                    store.setTimerDefaults(
                        workSeconds = 30,
                        restSeconds = current.defaultRestSec,
                        rounds = 3,
                        sound = current.soundAlertEnabled,
                        vibration = current.vibrationEnabled
                    )
                } catch (_: Throwable) {}
            }
        }
    }

    fun toggleProgressionModal(show: Boolean) {
        _uiState.update { it.copy(showProgressionModal = show) }
    }

    fun updateProgressionSettings(styleHu: String, ruleHu: String, gentleBeginner: Boolean) {
        _uiState.update {
            it.copy(
                trainingStyleHu = styleHu,
                progressionRuleHu = ruleHu,
                beginnerGentleProfileActive = gentleBeginner,
                showProgressionModal = false,
                statusBannerMessage = "Progresszió: $styleHu • $ruleHu"
            )
        }
    }

    fun selectBackupFormat(format: String) {
        _uiState.update { it.copy(selectedBackupFormat = format) }
    }

    fun toggleIncludeHealthInBackup(include: Boolean) {
        _uiState.update { it.copy(includeHealthInBackup = include) }
    }

    fun togglePermission(key: String) {
        _uiState.update { state ->
            when (key) {
                "gps" -> state.copy(permissionGpsEnabled = !state.permissionGpsEnabled)
                "central" -> state.copy(permissionCentralLocEnabled = !state.permissionCentralLocEnabled)
                "notif" -> state.copy(permissionNotificationsEnabled = !state.permissionNotificationsEnabled)
                "hc" -> state.copy(permissionHealthConnectEnabled = !state.permissionHealthConnectEnabled)
                "samsung" -> state.copy(permissionSamsungHealthEnabled = !state.permissionSamsungHealthEnabled)
                else -> state
            }
        }
    }

    fun showBannerMessage(message: String) {
        _uiState.update { it.copy(statusBannerMessage = message) }
    }

    fun clearBannerMessage() {
        _uiState.update { it.copy(statusBannerMessage = null) }
    }

    fun syncNativeSettings(
        languageCode: String,
        themeKey: String,
        restSec: Int = 90,
        sound: Boolean = true,
        vibration: Boolean = true
    ) {
        val lang = when (languageCode.lowercase()) {
            "en" -> AppLanguage.EN
            "de" -> AppLanguage.DE
            "ro" -> AppLanguage.RO
            else -> AppLanguage.HU
        }
        val palette = com.example.ui.theme.TrainPilotPalettes.getByKey(themeKey)
        _uiState.update { current ->
            current.copy(
                language = lang,
                selectedPaletteId = palette?.id ?: current.selectedPaletteId,
                defaultRestSec = if (restSec > 0) restSec else current.defaultRestSec,
                soundAlertEnabled = sound,
                vibrationEnabled = vibration
            )
        }
    }
}
