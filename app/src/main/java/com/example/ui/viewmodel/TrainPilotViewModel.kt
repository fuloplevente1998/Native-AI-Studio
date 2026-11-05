package com.example.ui.viewmodel

import androidx.room.withTransaction
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.YearMonth
import java.time.ZoneId
import java.time.LocalTime
import androidx.lifecycle.ViewModel
import com.example.data.DemoDataProvider
import com.example.model.AppLanguage
import com.example.model.BodyWeightLogItem
import com.example.model.CalendarDayEvent
import com.example.model.CalendarEventStatus
import com.example.model.ExerciseCategory
import com.example.model.ExerciseItem
import com.example.model.ExerciseSetItem
import com.example.model.HomeDemoMode
import com.example.model.LoggedWorkoutEntry
import com.example.model.workoutsInCalendarWeekToDate
import com.example.model.localWorkoutDate
import com.example.model.StudioProgressWindow
import com.example.model.progressPeriodDays
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.CancellationException
import com.repforge.app.NativeDataReplacement
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import android.content.Context
import androidx.compose.ui.graphics.toArgb
import com.repforge.app.ActiveWorkoutStore
import com.repforge.app.HealthBackgroundJob
import com.repforge.app.HealthJournalStore
import com.repforge.app.PhoneWorkoutExecution
import com.repforge.app.WearSyncRepository
import com.repforge.app.data.db.BodyWeightEntryEntity
import com.repforge.app.data.db.CalendarScheduleEntity
import com.repforge.app.data.db.ExerciseLibraryEntity
import com.repforge.app.data.db.KeyValueDocumentEntity
import com.repforge.app.data.db.ProgramDayEntity
import com.repforge.app.data.db.ProgramEntity
import com.repforge.app.data.db.TrainPilotDatabase
import com.repforge.app.data.migration.LegacyWebViewMigrator
import com.repforge.app.data.store.ThemePaletteOption
import com.repforge.app.data.store.TrainPilotSettingsStore
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import com.example.model.watchinfo.WatchInfoCardData
import com.example.model.watchinfo.WatchInfoStatus
import com.repforge.app.healthreadings.HealthReadingsProjection
import com.repforge.app.healthreadings.PulseDayReading
import com.repforge.app.healthreadings.RecoveryDayReading
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

data class WeeklyHealthDaySummary(
    val dateIso: String,
    val dayLabelHu: String,
    val steps: Int,
    val activeCaloriesKcal: Int,
    val sleepLabel: String,
    val workoutMinutes: Int,
    val workoutCount: Int,
    val sourceLabelHu: String,
    val stepOriginPackage: String = "",
    val stepsAvailable: Boolean = false,
    val stepsSourceLabelHu: String = "Nincs napi lépésadat",
    val pulse: PulseDayReading? = null
)

/** Read-only comparison for the exact saved native program and mode proposal. */
data class StudioTrainingModeRow(
    val day: String,
    val exercise: String,
    val beforeSets: Int?,
    val afterSets: Int?,
    val beforeReps: String,
    val afterReps: String,
    val restSeconds: Int?,
    val beforeWeight: Double?,
    val afterWeight: Double?,
    val rir: String
)

data class StudioTrainingModeReview(
    val beforeStyle: String,
    val beforeProgression: String,
    val afterStyle: String,
    val afterProgression: String,
    val inheritGlobal: Boolean,
    val programName: String,
    val rows: List<StudioTrainingModeRow>
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
    val maxHrBpm: Int = 0,
    val minHrBpm: Int = 0,
    val dailyPulseHistory: List<com.repforge.app.NativeDailyHealthDetails.PulsePoint> = emptyList(),
    val dailyPulseSampleCount: Int = 0,
    val hrvRmssdMs: Float = 0f,
    val bloodPressureLabel: String = "—",
    val bloodGlucoseLabel: String = "—",
    val respiratoryRateLabel: String = "—",
    val dailyDistanceKm: Float = 0f,
    val dailySteps: Int = 0,
    val stepsGoal: Int = 10000,
    val activeCaloriesKcal: Int = 0,
    val totalCaloriesKcal: Int = 0,
    val dailyHealthDate: String = "",
    val dailyHealthAvailableFields: Set<String> = emptySet(),
    val dailyHealthSourceLabel: String = "Health Connect",
    val dailyHealthLastSyncIso: String = "—",
    val healthGrantedCount: Int = 0,
    val healthPermissionCount: Int = 0,
    val lastHealthSyncLabel: String = "—",
    val healthProviderStatus: String = "A szolgáltató állapota még nem ismert",
    val healthSyncMessage: String = "", val healthSyncBusy: Boolean = false,
    val healthSyncPartial: Boolean = false, val healthSyncFailed: Boolean = false,
    val samsungGrantedCount: Int = -1, val samsungPermissionCount: Int = com.repforge.app.SamsungHealthRepository.PERMISSION_KEYS.size,
    val lastHealthReadLabel: String = "—",
    val samsungEnergyScore: Int = 0,
    val trainPilotReadinessScore: Int = 0,
    val coachReadinessScore: Int = 0,
    val todayPulseReading: PulseDayReading? = null,
    val todayRecoveryReading: RecoveryDayReading? = null,

    // Watch & Sync State (Mockup 4/4 #2 + main)
    val watchName: String = "Nincs csatlakoztatott Wear OS óra",
    val watchConnected: Boolean = false,
    val watchAppAvailable: Boolean = false,
    val watchBatteryPercent: Int = -1,
    val watchInfoLoading: Boolean = false,
    val watchInfoMessage: String = "",
    val watchDeviceDetails: String = "",
    val watchInfoCardData: WatchInfoCardData = WatchInfoCardData(),
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
    val trainingStyleCode: String = "normal",
    val progressionModeCode: String = "double",
    val programTrainingStyleCode: String = "normal",
    val programProgressionModeCode: String = "double",
    val programTrainingModeSource: String = "legacy",
    val activeTrainingModeProgramId: String = "",
    val globalTrainingModeUndo: Boolean = false,
    val programTrainingModeUndo: Boolean = false,
    val progressionModeReview: StudioTrainingModeReview? = null,
    val progressionModeBusy: Boolean = false,

    // Calendar & Replan State (Mockup 1/4 #4 + main Naptár & Újratervezés)
    val calendarYearMonth: String = YearMonth.now().toString(),
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
    val libraryMutationBusy: Boolean = false,
    val favoriteExerciseIds: Set<String> = emptySet(),
    val favoriteExercisesOnly: Boolean = false,

    // Active Workout & Measurement State (Mockup 2/4 #1..#6)
    val isSavingWorkout: Boolean = false,
    val isWorkoutSessionActive: Boolean = false,
    val activeWorkoutId: String = "",
    val activeWorkoutGeneration: Long = 0L,
    val workoutStartedEpochMs: Long = 0L,
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
    val profileComplete: Boolean = false,
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
    val outdoorRouteKey: String = "",
    val outdoorDistanceKm: Float = 0f,
    val outdoorElapsedLabel: String = "00:00",
    val outdoorPaceLabel: String = "0:00",
    val workoutSummaryMoodIndex: Int = -1, // No feedback until explicitly chosen.
    val workoutSummaryNote: String = "",
    val summaryFeedbackWorkoutId: String = "",
    val exerciseFeedbackWorkoutId: String = "",
    val exerciseFeedbackChoices: Map<String, Int> = emptyMap(),
    val exerciseFeedbackNotes: Map<String, String> = emptyMap(),

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
    val savedExerciseBests: List<com.repforge.app.SavedExerciseBest> = emptyList(),
    val savedRecordsLoading: Boolean = true,
    val savedRecordsError: Boolean = false,
    val videoModalExerciseTitle: String? = null,
    val videoModalExerciseId: String? = null,
    val realMuscleLoads: List<com.example.ui.components.MuscleLoadItem> = emptyList(),
    val muscleLoadPeriodDays: Int = 90,
    val logWorkoutMuscleLoads: List<com.example.ui.components.MuscleLoadItem> = emptyList(),

    // Health / Egészség State (Mockup 3/4 #5 + full main Egészség)
    val expandedWeeklySteps: Boolean = false,
    val expandedWeeklyWorkouts: Boolean = false,
    val expandedHealthPulseTrend: Boolean = false,
    val expandedHealthWeightLog: Boolean = false,
    val expandedHealthExtraData: Boolean = false,
    val expandedHealthJournal: Boolean = false,
    val expandedHealthConnect: Boolean = false,
    val weeklyHealthDays: List<WeeklyHealthDaySummary> = emptyList(),
    val dailyActivityDays: List<WeeklyHealthDaySummary> = emptyList(),
    val weeklyTotalSteps: Int = 0,
    val weeklyTotalCaloriesKcal: Int = 0,
    val weeklyTotalDistanceKm: Float = 0f,
    val weeklyAvgSleepLabel: String = "—",
    val weeklyDataSourcesSummary: String = "Health Connect / Samsung Health + Room edzésnapló",
    val dailySourceModeLabel: String = "Automatikus (legteljesebb napi lépésforrás)",
    val selectedWeightRangeIndex: Int = 1, // 0=7d, 1=30d, 2=3m, 3=1y, 4=All
    val currentBodyWeightKg: Float = 0f,
    val bodyWeightLogs: List<BodyWeightLogItem> = emptyList(),
    val bodyFatPercent: Float = 0f,
    val spo2Percent: Float = 0f,
    val vo2Max: Float = 0f,
    val showAddWeightDialog: Boolean = false,
    val primaryHealthSourceSamsung: Boolean = true,

    // Permissions & Backup State (Mockup 4/4 #3, #4, #5)
    val selectedBackupFormat: String = "JSON", // JSON, ZIP, DRIVE
    val includeHealthInBackup: Boolean = false,
    val backupHealthConsentSaving: Boolean = false,
    val permissionGpsEnabled: Boolean = false,
    val permissionCentralLocEnabled: Boolean = false,
    val permissionNotificationsEnabled: Boolean = false,
    val permissionHealthConnectEnabled: Boolean = false,
    val permissionSamsungHealthEnabled: Boolean = false,
    val privatePhotoCount: Int = 0,
    val privatePhotoWorkoutId: String? = null,

    // Toast / Feedback banner for immediate visual confirmation of actions
    val statusBannerMessage: String? = null
)

class TrainPilotViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(TrainPilotUiState())
    val uiState: StateFlow<TrainPilotUiState> = _uiState.asStateFlow()

    private var nativeContext: Context? = null
    private val calendarWriteMutex = Mutex()
    private val programWriteMutex = Mutex()
    @Volatile private var programWriteRevision = 0L
    private var programWriteJob: Job? = null
    @Volatile private var programWriteError: String? = null
    @Volatile private var calendarWriteRevision = 0L
    private var nativeSettingsStore: TrainPilotSettingsStore? = null
    private var nativeDatabase: TrainPilotDatabase? = null
    private var nativeScope: CoroutineScope? = null
    private var nativeReloadJob: Job? = null
    private var programEditorContinuation: com.repforge.app.NativeDataReplacement.Continuation? = null
    private var nativeObserversJob: Job? = null
    private var personalRecordsJob: Job? = null
    private var personalRecordsRevision = 0L
    @Volatile private var watchHandoffBusy = false
    private fun watchJournal(): JSONObject = nativeContext?.let { context ->
        runCatching { PhoneOwnership193.journal(context).read() }
            .getOrElse { JSONObject().put("stage", "unreadable") }
    } ?: JSONObject()


    private fun tr(
        hu: String,
        en: String,
        de: String = en,
        ro: String = en,
        lang: AppLanguage = _uiState.value.language
    ): String = when (lang) {
        AppLanguage.HU -> hu
        AppLanguage.EN -> en
        AppLanguage.DE -> de
        AppLanguage.RO -> ro
    }

    private fun translateMuscleTokenHu(token: String): String {
        return when (token.trim().lowercase()) {
            "chest", "mell", "mellkas", "push" -> "Mell"
            "back", "hát", "lats", "pull" -> "Hát"
            "shoulders", "váll", "delts" -> "Váll"
            "biceps", "bicepsz" -> "Bicepsz"
            "triceps", "tricepsz" -> "Tricepsz"
            "arms", "kar", "forearms" -> "Kar"
            "legs", "quads", "láb", "comb" -> "Láb"
            "hamstrings", "combhajlító" -> "Combhajlító"
            "glutes", "far", "farizom" -> "Farizom"
            "calves", "vádli" -> "Vádli"
            "core", "abs", "has", "törzs" -> "Törzs / Has"
            "cardio", "kardió" -> "Kardió"
            "full", "full_body", "teljes test" -> "Teljes test"
            else -> token.trim().replaceFirstChar { it.uppercase() }
        }
    }

    private fun translateLoadTypeOrEquipmentHu(rawEquip: String, loadType: String): String {
        val cleanEquip = rawEquip.trim()
        val technicalTokens = setOf(
            "per_hand", "single_dumbbell", "bodyweight", "total", "weight",
            "dumbbell", "barbell", "machine", "cable", "band", "kettlebell"
        )
        if (cleanEquip.isNotBlank() && cleanEquip.lowercase() !in technicalTokens) {
            return cleanEquip.replaceFirstChar { it.uppercase() }
        }
        return when (cleanEquip.ifBlank { loadType }.trim().lowercase()) {
            "per_hand", "dumbbell" -> "2 kézisúlyzó"
            "single_dumbbell" -> "1 kézisúlyzó"
            "bodyweight" -> "Saját testsúly"
            "barbell" -> "Kétkezes rúd"
            "machine" -> "Edzőtermi gép"
            "cable" -> "Csigás gép"
            "band" -> "Gumiszalag"
            "kettlebell" -> "Kettlebell"
            "total", "weight" -> "Szabadsúly / Gép"
            else -> "Súlyzó / Eszköz"
        }
    }

    private fun mapExerciseEntity(entity: com.repforge.app.data.db.ExerciseLibraryEntity): ExerciseItem {
        val rawObj = runCatching { JSONObject(entity.rawJson) }.getOrNull()
        val rawEquip = rawObj?.optString("equipment").orEmpty()
        val rawTarget = rawObj?.optString("target").orEmpty()
        val parsedMuscleList = runCatching {
            val arr = JSONArray(entity.muscleGroupsJson)
            (0 until arr.length()).mapNotNull { idx ->
                arr.optString(idx)?.takeIf { it.isNotBlank() }?.let { translateMuscleTokenHu(it) }
            }.distinct()
        }.getOrDefault(emptyList())

        val primaryKey = entity.primaryMuscle.lowercase()
        val cat = when (primaryKey) {
            "chest", "mell", "mellkas", "push" -> ExerciseCategory.CHEST
            "back", "hát", "pull" -> ExerciseCategory.BACK
            "shoulders", "váll" -> ExerciseCategory.SHOULDERS
            "arms", "biceps", "triceps", "bicepsz", "tricepsz", "kar" -> ExerciseCategory.ARMS
            "legs", "quads", "hamstrings", "calves", "glutes", "comb", "láb", "far", "cardio" -> ExerciseCategory.LEGS
            "core", "abs", "has", "törzs" -> ExerciseCategory.CORE
            else -> ExerciseCategory.CHEST
        }

        val cleanPrimaryHu = if (rawTarget.isNotBlank() && !rawTarget.startsWith("[")) {
            rawTarget.replaceFirstChar { it.uppercase() }
        } else {
            parsedMuscleList.firstOrNull() ?: translateMuscleTokenHu(entity.primaryMuscle.ifBlank { "Mell" })
        }

        val secondaryList = parsedMuscleList.filterNot {
            it.equals(cleanPrimaryHu, ignoreCase = true)
        }
        val cleanSecondaryHu = if (secondaryList.isNotEmpty()) {
            secondaryList.joinToString(", ")
        } else if (parsedMuscleList.size > 1) {
            parsedMuscleList.drop(1).joinToString(", ")
        } else {
            "Stabilizáló izmok"
        }

        val cleanEquipmentHu = translateLoadTypeOrEquipmentHu(rawEquip, entity.loadType)
        val parsedReps = Regex("\\d+").findAll(entity.defaultReps).lastOrNull()?.value?.toIntOrNull()
            ?: entity.defaultReps.toIntOrNull()
            ?: 10

        fun parseNotesToSteps(raw: String, fallback: String): List<String> = raw
            .split(Regex("(?<=\\.)\\s+|\\n+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty { listOf(fallback) }

        val parsedStepsHu = parseNotesToSteps(
            entity.notesHu,
            "Helyes kivitelezés: ügyelj az egyenes hátra és a kontrollált mozgástartományra."
        )
        val parsedStepsEn = parseNotesToSteps(
            entity.notesEn.ifBlank { entity.notesHu },
            "Proper execution: keep a neutral spine and controlled range of motion."
        )
        val parsedStepsDe = parseNotesToSteps(
            entity.notesDe.ifBlank { entity.notesEn.ifBlank { entity.notesHu } },
            "Saubere Ausführung: gerader Rücken und kontrollierter Bewegungsumfang."
        )
        val parsedStepsRo = parseNotesToSteps(
            entity.notesRo.ifBlank { entity.notesEn.ifBlank { entity.notesHu } },
            "Execuție corectă: menține spatele drept și mișcarea controlată."
        )
        val isTimedFlag = entity.measurementType.contains("time", ignoreCase = true) ||
            entity.repUnit.contains("mp", ignoreCase = true) ||
            entity.repUnit.contains("sec", ignoreCase = true)
        val isBilateralFlag = entity.id.contains("side-plank", ignoreCase = true) ||
            entity.nameHu.contains("oldalsó plank", ignoreCase = true) ||
            entity.measurementType.contains("bilateral", ignoreCase = true) ||
            entity.measurementType.contains("side", ignoreCase = true) ||
            entity.repUnit.contains("oldal", ignoreCase = true)
        val defaultTargetSec = if (isTimedFlag || isBilateralFlag) parsedReps.coerceIn(10, 300) else 45
        return ExerciseItem(
            id = entity.id,
            nameHu = entity.nameHu.ifBlank { entity.nameEn }.ifBlank { "Gyakorlat" },
            nameEn = entity.nameEn.ifBlank { entity.nameHu }.ifBlank { "Exercise" },
            nameDe = entity.nameDe.ifBlank { entity.nameEn.ifBlank { entity.nameHu } },
            nameRo = entity.nameRo.ifBlank { entity.nameEn.ifBlank { entity.nameHu } },
            category = cat,
            equipmentHu = cleanEquipmentHu,
            primaryMusclesHu = cleanPrimaryHu,
            secondaryMusclesHu = cleanSecondaryHu,
            defaultSets = entity.defaultSets.coerceAtLeast(1),
            defaultReps = parsedReps.coerceAtLeast(1),
            defaultWeightKg = entity.defaultWeight.toFloat(),
            defaultRestSec = entity.defaultRestSeconds.coerceAtLeast(30),
            isTimed = isTimedFlag || isBilateralFlag,
            isBilateral = isBilateralFlag,
            targetSeconds = defaultTargetSec,
            stepsHu = parsedStepsHu,
            stepsEn = parsedStepsEn,
            stepsDe = parsedStepsDe,
            stepsRo = parsedStepsRo,
            coachStatusHu = "Normál terhelés",
            coachHintHu = parsedStepsHu.firstOrNull() ?: "Kezdj könnyebben; ma ne erőltesd a progressziót.",
            videoUrl = entity.videoUrl.orEmpty(),
            isCustom = entity.custom,
            loadType = entity.loadType, repUnit = entity.repUnit, measurementType = entity.measurementType,
            targetRepsText = entity.defaultReps
        )
    }

    private fun computePrRecordsFromWorkouts(workouts: List<LoggedWorkoutEntry>): List<PrRecordEntry> {
        data class BestEntry(val weightKg: Float, val reps: Int, val e1Rm: Float, val dateLabelHu: String)
        val bestByExercise = linkedMapOf<String, BestEntry>()
        for (w in workouts) {
            for (exDetail in w.exerciseDetails) {
                for (s in exDetail.sets) {
                    if (!s.completed) continue
                    val score = if (s.weightKg > 0f) s.weightKg * (1f + s.reps / 30f) else s.reps.toFloat()
                    val prev = bestByExercise[exDetail.nameHu]
                    if (prev == null || score > prev.e1Rm || (score == prev.e1Rm && s.weightKg > prev.weightKg)) {
                        bestByExercise[exDetail.nameHu] = BestEntry(
                            weightKg = s.weightKg,
                            reps = s.reps,
                            e1Rm = score,
                            dateLabelHu = w.dateLabelHu
                        )
                    }
                }
            }
        }
        return bestByExercise.entries
            .sortedByDescending { it.value.e1Rm }
            .mapIndexed { idx, (exName, best) ->
                val valueStr = if (best.weightKg > 0f) {
                    "${best.weightKg.toString().removeSuffix(".0")} kg × ${best.reps}"
                } else {
                    "${best.reps} ism. / mp"
                }
                val e1RmLabel = if (best.weightKg > 0f) "1RM ≈ ${"%.1f".format(best.e1Rm).replace('.', ',')} kg" else "Idő / ismétlés rekord"
                PrRecordEntry(
                    id = "pr_${idx}_${exName.hashCode()}",
                    exerciseNameHu = exName,
                    typeLabelHu = e1RmLabel,
                    dateLabel = best.dateLabelHu,
                    previousLabel = "—",
                    newLabel = valueStr
                )
            }
    }

    private fun mapBodyWeightEntitiesToLogItems(entities: List<BodyWeightEntryEntity>): List<BodyWeightLogItem> {
        val dotFormatter = DateTimeFormatter.ofPattern("yyyy. MM. dd.")
        return entities
            .sortedWith(compareBy<BodyWeightEntryEntity> { it.dayKey.ifBlank { it.date.take(10) } }.thenBy { it.updatedAt })
            .map { e ->
                val cleanDayKey = e.dayKey.ifBlank { e.date.take(10) }
                val parsedDate = runCatching { LocalDate.parse(cleanDayKey.take(10)) }.getOrElse { LocalDate.now() }
                BodyWeightLogItem(
                    id = e.id,
                    kg = e.kg.toFloat(),
                    dayKey = parsedDate.toString(),
                    dateLabelHu = parsedDate.format(dotFormatter)
                )
            }
    }

    private fun persistProgramsToRoomAsync(profile: JSONObject? = null) {
        val db = nativeDatabase ?: return
        val scope = nativeScope ?: return
        val currentPrograms = _uiState.value.programs
        if (currentPrograms.isEmpty()) return
        val nowMs = System.currentTimeMillis()
        val revision = ++programWriteRevision
        programWriteError = null
        val context = nativeContext ?: return
        val ticket = try { com.repforge.app.NativeDataReplacement.continuationForCurrentData(context) }
            catch (error: Exception) {
                programWriteError = error.message ?: "A terv nem menthető."
                _uiState.update { it.copy(statusBannerMessage = programWriteError) }; return
            }
        programWriteJob = scope.launch(Dispatchers.IO) {
            programWriteMutex.withLock {
            if (revision != programWriteRevision) return@withLock
            runCatching {
                com.repforge.app.NativeDataReplacement.mutate(context) {
                ticket.requireCurrent(context)
                currentPrograms.forEachIndexed { pIdx, prog ->
                    val existing = db.programDao().getProgramWithDaysById(prog.id)?.program
                    val persistedDesc = if (!prog.isCustom) {
                        existing?.description.orEmpty()
                    } else prog.subtitleHu
                    val rawProgJson = JSONObject(existing?.rawJson ?: "{}").apply {
                        put("id", prog.id)
                        put("nameHu", prog.nameHu)
                        put("nameEn", prog.nameEn)
                        put("nameDe", prog.nameDe)
                        put("nameRo", prog.nameRo)
                        put("description", persistedDesc)
                        put("isActive", prog.isActive)
                        put("isCustom", prog.isCustom)
                    }
                    val created = ProgramEntity(
                        id = prog.id,
                        nameHu = prog.nameHu,
                        nameEn = prog.nameEn,
                        nameDe = prog.nameDe.ifBlank { prog.nameEn.ifBlank { prog.nameHu } },
                        nameRo = prog.nameRo.ifBlank { prog.nameEn.ifBlank { prog.nameHu } },
                        location = prog.tagsHu.firstOrNull() ?: "Otthon / Edzőterem",
                        level = prog.tagsHu.getOrNull(1) ?: "Kezdő",
                        description = persistedDesc,
                        builtin = !prog.isCustom,
                        generated = prog.id.startsWith("custom_plan_"),
                        sourceType = if (prog.isCustom) "user" else "builtin",
                        sourceTemplateId = null,
                        templateVersion = 1,
                        accentHex = "#D8B36A",
                        sortOrder = pIdx,
                        updatedAt = nowMs,
                        rawJson = rawProgJson.toString()
                    )
                    val progEntity = existing?.copy(
                        nameHu = prog.nameHu,
                        nameEn = prog.nameEn,
                        nameDe = prog.nameDe.ifBlank { existing.nameDe.ifBlank { prog.nameEn } },
                        nameRo = prog.nameRo.ifBlank { existing.nameRo.ifBlank { prog.nameEn } },
                        description = persistedDesc,
                        sortOrder = pIdx,
                        updatedAt = nowMs,
                        rawJson = rawProgJson.toString()
                    ) ?: created
                    val progDays = prog.days.mapIndexed { dIdx, day ->
                        val dayKey = day.id.substringAfter("${prog.id}:").ifBlank { "day_${dIdx + 1}" }
                        val compositeId = if (day.id.contains(":")) day.id else "${prog.id}:$dayKey"
                        val exArr = JSONArray()
                        day.exercises.forEach { ex ->
                            exArr.put(
                                JSONObject().apply {
                                    put("id", ex.id)
                                    put("sets", ex.defaultSets)
                                    put("reps", ex.targetRepsText.ifBlank { ex.defaultReps.toString() })
                                    put("weight", ex.defaultWeightKg.toDouble())
                                    put("rest", ex.defaultRestSec)
                                }
                            )
                        }
                        ProgramDayEntity(
                            compositeId = compositeId,
                            programId = prog.id,
                            dayId = dayKey,
                            nameHu = day.titleHu,
                            nameEn = day.titleEn,
                            nameDe = day.titleDe.ifBlank { day.titleEn.ifBlank { day.titleHu } },
                            nameRo = day.titleRo.ifBlank { day.titleEn.ifBlank { day.titleHu } },
                            sortOrder = dIdx,
                            exercisesJson = exArr.toString()
                        )
                    }
                    db.programDao().saveProgramWithDays(progEntity, progDays)
                }
                currentPrograms.firstOrNull { it.isActive }?.let { nativeSettingsStore?.setActiveProgramId(it.id) }
                if (profile != null) {
                    val store = checkNotNull(nativeSettingsStore)
                    val merged = JSONObject(store.current().profileJson)
                    profile.keys().forEach { key -> merged.put(key, profile.get(key)) }
                    store.saveTrainingProfile(merged)
                    val active = currentPrograms.first { it.isActive }
                    check(db.programDao().getProgramWithDaysById(active.id)?.days?.isNotEmpty() == true)
                    store.setActiveProgramId(active.id)
                    _uiState.update { it.copy(profileComplete = true,
                        statusBannerMessage = tr(
                            "Profil és egyéni terv tartósan elmentve.",
                            "Profile and personalized plan saved.",
                            "Profil und individueller Plan dauerhaft gespeichert.",
                            "Profilul și planul personalizat au fost salvate.",
                            it.language
                        )) }
                }
                }
            }.onFailure { failure ->
                if (failure is kotlinx.coroutines.CancellationException) throw failure
                val errMsg = tr(
                    "A terv/profil mentése nem sikerült. Próbáld újra.",
                    "Failed to save plan/profile. Please try again.",
                    "Speichern von Plan/Profil fehlgeschlagen. Bitte erneut versuchen.",
                    "Salvarea planului/profilului a eșuat. Încearcă din nou."
                )
                if (revision == programWriteRevision) programWriteError = errMsg
                _uiState.update { it.copy(statusBannerMessage = errMsg) }
            }
            }
        }
    }

    private fun persistCalendarStateToRoomAsync() {
        val db = nativeDatabase ?: return
        val scope = nativeScope ?: return
        val state = _uiState.value
        val events = state.calendarEvents
        val undoStack = state.calendarUndoStack
        val activeProg = state.programs.firstOrNull { it.isActive } ?: state.programs.firstOrNull()
        val nowMs = System.currentTimeMillis()
        val revision = ++calendarWriteRevision
        scope.launch(Dispatchers.IO) {
            runCatching {
              calendarWriteMutex.withLock {
                if (revision != calendarWriteRevision) return@withLock
                db.withTransaction {
                val oldDocument = db.keyValueDocumentDao().getDocument("native:calendarState")
                val previous = oldDocument?.json?.let { JSONObject(it).optJSONArray("events") } ?: JSONArray()
                val retainedIds = events.map { it.id }.toSet()
                for (i in 0 until previous.length()) {
                    val id = previous.getJSONObject(i).getString("id")
                    if (id !in retainedIds && db.calendarScheduleDao().getEntry(id)?.googleEventId.isNullOrBlank())
                        db.calendarScheduleDao().deleteEntry(id)
                }
                val scheduleEntities = events.filter { ev ->
                    db.calendarScheduleDao().getEntry(ev.id)?.googleEventId.isNullOrBlank()
                }.map { ev ->
                    val date = LocalDate.parse(ev.dateIso)
                    val dateStr = date.toString()
                    val (start, end) = com.example.model.StudioCalendarDates.window(ev, ZoneId.systemDefault())
                    val statusStr = when (ev.status) {
                        CalendarEventStatus.COMPLETED -> "completed"
                        CalendarEventStatus.MISSED -> "skipped"
                        else -> "planned"
                    }
                    val rawEv = JSONObject().apply {
                        put("id", ev.id)
                        put("dayOfMonth", ev.dayOfMonth)
                        put("dateIso", ev.dateIso)
                        put("timeLabel", ev.timeLabel)
                        put("titleHu", ev.titleHu)
                        put("exerciseCount", ev.exerciseCount)
                        put("durationMinutes", ev.durationMinutes)
                        put("status", ev.status.name)
                        put("splitBadge", ev.splitBadge)
                    }
                    CalendarScheduleEntity(
                        id = ev.id,
                        googleEventId = "",
                        date = dateStr,
                        startIso = start.toString(),
                        endIso = end.toString(),
                        programId = activeProg?.id ?: "program_default",
                        programName = activeProg?.nameHu ?: ev.titleHu,
                        dayId = ev.splitBadge,
                        dayName = ev.titleHu,
                        status = statusStr,
                        updatedAt = nowMs,
                        rawJson = rawEv.toString()
                    )
                }
                if (scheduleEntities.isNotEmpty()) {
                    db.calendarScheduleDao().upsertEntries(scheduleEntities)
                }
                val undoJsonArr = JSONArray()
                undoStack.forEach { snapshot ->
                    val snapArr = JSONArray()
                    snapshot.forEach { ev ->
                        snapArr.put(
                            JSONObject().apply {
                                put("id", ev.id)
                                put("dayOfMonth", ev.dayOfMonth)
                        put("dateIso", ev.dateIso)
                                put("timeLabel", ev.timeLabel)
                                put("titleHu", ev.titleHu)
                                put("exerciseCount", ev.exerciseCount)
                                put("durationMinutes", ev.durationMinutes)
                                put("status", ev.status.name)
                                put("splitBadge", ev.splitBadge)
                            }
                        )
                    }
                    undoJsonArr.put(snapArr)
                }
                val currentArr = JSONArray()
                events.forEach { ev ->
                    currentArr.put(
                        JSONObject().apply {
                            put("id", ev.id)
                            put("dayOfMonth", ev.dayOfMonth)
                        put("dateIso", ev.dateIso)
                            put("timeLabel", ev.timeLabel)
                            put("titleHu", ev.titleHu)
                            put("exerciseCount", ev.exerciseCount)
                            put("durationMinutes", ev.durationMinutes)
                            put("status", ev.status.name)
                            put("splitBadge", ev.splitBadge)
                        }
                    )
                }
                val docJson = JSONObject().apply {
                    put("events", currentArr)
                    put("undoStack", undoJsonArr)
                    put("updatedAt", nowMs)
                }
                db.keyValueDocumentDao().putDocument(
                    KeyValueDocumentEntity(
                        key = "native:calendarState",
                        json = docJson.toString(),
                        updatedAt = nowMs
                    )
                )
                }
              }
            }.onFailure { error ->
                if (error is kotlinx.coroutines.CancellationException) throw error
                _uiState.update { it.copy(statusBannerMessage = "A naptármódosítás nem menthető. Próbáld újra.") }
            }
        }
    }

    private fun mapCatalogPrograms(exercisesMap: Map<String, ExerciseItem>): List<TrainingProgramModel> {
        val (programs, days) = TrainPilotDatabase.defaultPrograms()
        return mapProgramRows(programs, days, exercisesMap)
    }

    private fun mapProgramRows(programs: List<ProgramEntity>, days: List<ProgramDayEntity>,
        exercisesMap: Map<String, ExerciseItem>, activeId: String? = null): List<TrainingProgramModel> {
        val explicitActive = programs.any { runCatching { JSONObject(it.rawJson).optBoolean("isActive") }.getOrDefault(false) }
        val (builtinProgs, builtinDays) = runCatching { TrainPilotDatabase.defaultPrograms() }.getOrElse { emptyList<ProgramEntity>() to emptyList<ProgramDayEntity>() }
        val builtinProgById = builtinProgs.associateBy { it.id }
        val builtinDayByComposite = builtinDays.associateBy { it.compositeId }
        return programs.mapIndexed { pIdx, prog ->
                val catalogProg = if (prog.builtin) builtinProgById[prog.id] else null
                val strippedNameHu = prog.nameHu.trim()
                    .removeSuffix(" (személyre szabott)")
                    .removeSuffix(" – másolat")
                    .removeSuffix(" - másolat")
                    .removeSuffix(" (másolat)")
                    .trim()
                val copiedTemplate = if (!prog.builtin) {
                    prog.sourceTemplateId?.let { builtinProgById[it] }
                        ?: builtinProgs.firstOrNull {
                            it.nameHu.equals(strippedNameHu, ignoreCase = true) ||
                                it.nameEn.equals(strippedNameHu, ignoreCase = true) ||
                                it.nameDe.equals(strippedNameHu, ignoreCase = true) ||
                                it.nameRo.equals(strippedNameHu, ignoreCase = true)
                        }
                } else null
                val localizedTemplate = catalogProg ?: copiedTemplate
                val progDays = days.filter { it.programId == prog.id }.sortedWith(compareBy<ProgramDayEntity> { it.sortOrder }.thenBy { it.dayId }).mapIndexed { dIdx, dayEntity ->
                    val catalogDay = if (prog.builtin) builtinDayByComposite[dayEntity.compositeId]
                        else localizedTemplate?.let { tpl ->
                            builtinDayByComposite["${tpl.id}:${dayEntity.dayId}"]
                                ?: builtinDays.firstOrNull { it.programId == tpl.id && it.nameHu.equals(dayEntity.nameHu, ignoreCase = true) }
                        } ?: builtinDays.firstOrNull { it.nameHu.equals(dayEntity.nameHu, ignoreCase = true) }
                    val dayExercises = runCatching {
                        val recipes = JSONObject(prog.rawJson).optJSONObject("prescriptions") ?: JSONObject()
                        val arr = JSONArray(dayEntity.exercisesJson)
                        (0 until arr.length()).mapNotNull { i ->
                            val item = arr.get(i)
                            val id = if (item is JSONObject) item.optString("id") else item.toString()
                            val source = exercisesMap[id] ?: return@mapNotNull null
                            val rx = JSONObject(recipes.optJSONObject(id)?.toString() ?: "{}")
                            if (item is JSONObject) item.keys().forEach { key -> if (key != "id") rx.put(key, item.get(key)) }
                            val target = rx.optString("reps", source.targetRepsText.ifBlank { source.defaultReps.toString() })
                            source.copy(defaultSets = rx.optInt("sets", source.defaultSets),
                                defaultReps = Regex("[0-9]+").find(target)?.value?.toIntOrNull() ?: source.defaultReps,
                                targetRepsText = target,
                                defaultWeightKg = rx.optDouble("weight", rx.optDouble("weightKg", source.defaultWeightKg.toDouble())).toFloat(),
                                defaultRestSec = rx.optInt("rest", rx.optInt("restSec", source.defaultRestSec)))
                        }
                    }.getOrElse { emptyList() }
                    val resolvedDayHu = dayEntity.nameHu.ifBlank { catalogDay?.nameHu.orEmpty() }.ifBlank { "Edzésnap ${dIdx + 1}" }
                    val rawDayEn = (catalogDay?.nameEn?.takeIf { dayEntity.nameHu == catalogDay.nameHu && (dayEntity.nameEn.isBlank() || dayEntity.nameEn == dayEntity.nameHu) }
                        ?: dayEntity.nameEn.takeIf { it.isNotBlank() && it != dayEntity.nameHu })
                        .orEmpty()
                    val resolvedDayEn = rawDayEn.ifBlank { com.example.model.translateProgramDayTitle(resolvedDayHu, AppLanguage.EN) }
                    val rawDayDe = (catalogDay?.nameDe?.takeIf { dayEntity.nameHu == catalogDay.nameHu && (dayEntity.nameDe.isBlank() || dayEntity.nameDe == dayEntity.nameHu || dayEntity.nameDe == dayEntity.nameEn) }
                        ?: dayEntity.nameDe.takeIf { it.isNotBlank() && it != dayEntity.nameHu && it != dayEntity.nameEn })
                        .orEmpty()
                    val resolvedDayDe = rawDayDe.ifBlank { com.example.model.translateProgramDayTitle(resolvedDayHu, AppLanguage.DE) }
                    val rawDayRo = (catalogDay?.nameRo?.takeIf { dayEntity.nameHu == catalogDay.nameHu && (dayEntity.nameRo.isBlank() || dayEntity.nameRo == dayEntity.nameHu || dayEntity.nameRo == dayEntity.nameEn) }
                        ?: dayEntity.nameRo.takeIf { it.isNotBlank() && it != dayEntity.nameHu && it != dayEntity.nameEn })
                        .orEmpty()
                    val resolvedDayRo = rawDayRo.ifBlank { com.example.model.translateProgramDayTitle(resolvedDayHu, AppLanguage.RO) }
                    com.example.model.ProgramDayModel(
                        id = dayEntity.compositeId,
                        dayCode = if (dIdx < 26) "${'A' + dIdx} nap" else "${dIdx + 1}. nap",
                        titleHu = resolvedDayHu,
                        titleEn = resolvedDayEn,
                        durationMinutes = 60,
                        exercises = dayExercises,
                        titleDe = resolvedDayDe,
                        titleRo = resolvedDayRo
                    )
                }
                val locHu = prog.location.ifBlank { localizedTemplate?.location.orEmpty() }.ifBlank { "Otthon / Edzőterem" }
                val lvlHu = prog.level.ifBlank { localizedTemplate?.level.orEmpty() }.ifBlank { "Kezdő" }
                val metaSource = localizedTemplate ?: prog
                val locEn = if (metaSource.builtin) com.repforge.app.data.db.NativeBuiltinCatalog.programMetadata(metaSource, "location", "en")
                    else com.example.model.translateProgramTag(locHu, AppLanguage.EN)
                val locDe = if (metaSource.builtin) com.repforge.app.data.db.NativeBuiltinCatalog.programMetadata(metaSource, "location", "de")
                    else com.example.model.translateProgramTag(locHu, AppLanguage.DE)
                val locRo = if (metaSource.builtin) com.repforge.app.data.db.NativeBuiltinCatalog.programMetadata(metaSource, "location", "ro")
                    else com.example.model.translateProgramTag(locHu, AppLanguage.RO)
                val lvlEn = if (metaSource.builtin) com.repforge.app.data.db.NativeBuiltinCatalog.programMetadata(metaSource, "level", "en")
                    else com.example.model.translateProgramTag(lvlHu, AppLanguage.EN)
                val lvlDe = if (metaSource.builtin) com.repforge.app.data.db.NativeBuiltinCatalog.programMetadata(metaSource, "level", "de")
                    else com.example.model.translateProgramTag(lvlHu, AppLanguage.DE)
                val lvlRo = if (metaSource.builtin) com.repforge.app.data.db.NativeBuiltinCatalog.programMetadata(metaSource, "level", "ro")
                    else com.example.model.translateProgramTag(lvlHu, AppLanguage.RO)

                val isAutoGeneratedDesc = prog.description.isBlank() ||
                    prog.description == localizedTemplate?.description ||
                    Regex("^.+?\\s*•\\s*.+?\\s*•\\s*\\d+\\s*(?:edzésnap|workout days|Trainingstage|zile de antrenament)$", RegexOption.IGNORE_CASE).matches(prog.description.trim())
                val customUserDesc = prog.description.takeIf {
                    !prog.builtin && !isAutoGeneratedDesc
                }
                val builtSubtitleHu = customUserDesc ?: "$locHu • $lvlHu • ${progDays.size} edzésnap"
                val builtSubtitleEn = customUserDesc?.let { com.example.model.translateProgramSubtitle(it, AppLanguage.EN) }
                    ?: "$locEn • $lvlEn • ${progDays.size} workout days"
                val builtSubtitleDe = customUserDesc?.let { com.example.model.translateProgramSubtitle(it, AppLanguage.DE) }
                    ?: "$locDe • $lvlDe • ${progDays.size} Trainingstage"
                val builtSubtitleRo = customUserDesc?.let { com.example.model.translateProgramSubtitle(it, AppLanguage.RO) }
                    ?: "$locRo • $lvlRo • ${progDays.size} zile de antrenament"

                val resolvedNameHu = prog.nameHu.ifBlank { catalogProg?.nameHu.orEmpty() }.ifBlank { prog.nameEn }.ifBlank { "Edzésterv ${pIdx + 1}" }
                val rawNameEn = (localizedTemplate?.nameEn?.takeIf { prog.nameHu == localizedTemplate.nameHu && (prog.nameEn.isBlank() || prog.nameEn == prog.nameHu) }
                    ?: prog.nameEn.takeIf { it.isNotBlank() && it != prog.nameHu })
                    .orEmpty()
                val resolvedNameEn = rawNameEn.ifBlank { com.example.model.translateProgramTitle(resolvedNameHu, AppLanguage.EN) }
                val rawNameDe = (localizedTemplate?.nameDe?.takeIf { prog.nameHu == localizedTemplate.nameHu && (prog.nameDe.isBlank() || prog.nameDe == prog.nameHu || prog.nameDe == prog.nameEn) }
                    ?: prog.nameDe.takeIf { it.isNotBlank() && it != prog.nameHu && it != prog.nameEn })
                    .orEmpty()
                val resolvedNameDe = rawNameDe.ifBlank { com.example.model.translateProgramTitle(resolvedNameHu, AppLanguage.DE) }
                val rawNameRo = (localizedTemplate?.nameRo?.takeIf { prog.nameHu == localizedTemplate.nameHu && (prog.nameRo.isBlank() || prog.nameRo == prog.nameHu || prog.nameRo == prog.nameEn) }
                    ?: prog.nameRo.takeIf { it.isNotBlank() && it != prog.nameHu && it != prog.nameEn })
                    .orEmpty()
                val resolvedNameRo = rawNameRo.ifBlank { com.example.model.translateProgramTitle(resolvedNameHu, AppLanguage.RO) }

                TrainingProgramModel(
                    id = prog.id,
                    nameHu = resolvedNameHu,
                    nameEn = resolvedNameEn,
                    subtitleHu = builtSubtitleHu,
                    subtitleEn = builtSubtitleEn,
                    tagsHu = listOf(locHu, lvlHu),
                    isCustom = !prog.builtin,
                    isActive = if (activeId != null && programs.any { it.id == activeId }) prog.id == activeId
                        else if (explicitActive) JSONObject(prog.rawJson).optBoolean("isActive") else pIdx == 0,
                    days = progDays,
                    nameDe = resolvedNameDe,
                    nameRo = resolvedNameRo,
                    subtitleDe = builtSubtitleDe,
                    subtitleRo = builtSubtitleRo,
                    tagsEn = listOf(locEn, lvlEn),
                    tagsDe = listOf(locDe, lvlDe),
                    tagsRo = listOf(locRo, lvlRo)
                )
            }
    }

    private fun targetSubScreenForExercise(ex: ExerciseItem?): SubScreen {
        if (ex == null) return SubScreen.ACTIVE_STRENGTH
        val isBi = ex.isBilateral ||
            ex.id.contains("side-plank", ignoreCase = true) ||
            ex.nameHu.contains("oldalsó plank", ignoreCase = true)
        return when {
            isBi -> SubScreen.ACTIVE_BILATERAL
            ex.isTimed -> SubScreen.ACTIVE_TIMED
            else -> SubScreen.ACTIVE_STRENGTH
        }
    }

    private fun buildDefaultSetsForExercise(ex: ExerciseItem): List<ExerciseSetItem> {
        val count = ex.defaultSets.coerceIn(1, 6)
        val defaultSideSec = if (ex.isBilateral || ex.isTimed) ex.targetSeconds.coerceAtLeast(15) else 30
        return (1..count).map { setNum ->
            ExerciseSetItem(
                setNumber = setNum,
                weightKg = ex.defaultWeightKg,
                reps = ex.defaultReps.coerceAtLeast(1),
                rir = 2,
                completed = false,
                leftSec = defaultSideSec,
                rightSec = defaultSideSec,
                leftRecorded = false,
                rightRecorded = false,
                id = UUID.randomUUID().toString()
            )
        }
    }

    private fun buildCanonicalSessionJsonFromState(state: TrainPilotUiState): JSONObject {
        val wId = state.activeWorkoutId.ifBlank { UUID.randomUUID().toString() }
        val startedMs = if (state.workoutStartedEpochMs > 0L) state.workoutStartedEpochMs else System.currentTimeMillis()
        val startedIso = Instant.ofEpochMilli(startedMs).toString()
        val workoutExs = state.activeWorkoutExercises
        val currentEx = workoutExs.getOrNull(state.activeExerciseIndex)
        val allSetsMap = state.activeSetsByExerciseId.toMutableMap()
        if (currentEx != null) {
            allSetsMap[currentEx.id] = state.activeSets
        }

        val exercisesArray = JSONArray()
        workoutExs.forEachIndexed { exIdx, exItem ->
            val setsForEx = allSetsMap[exItem.id] ?: buildDefaultSetsForExercise(exItem)
            val setsArray = JSONArray()
            setsForEx.forEachIndexed { idx, s ->
                setsArray.put(JSONObject().apply {
                    put("setId", s.id.ifBlank { "${exItem.id}_set_${s.setNumber}" })
                    put("set", idx + 1)
                    put("setNumber", s.setNumber)
                    put("weight", s.weightKg.toDouble())
                    put("reps", s.reps.toString())
                    put("done", s.completed)
                    if (s.actualRirProvided) put("actualRir", s.rir)
                    put("leftSec", s.leftSec)
                    put("rightSec", s.rightSec)
                    put("leftRecorded", s.leftRecorded)
                    put("rightRecorded", s.rightRecorded)
                })
            }
            exercisesArray.put(JSONObject().apply {
                put("id", exItem.id)
                put("exerciseId", exItem.id)
                put("name", exItem.nameHu)
                put("nameHu", exItem.nameHu)
                put("nameEn", exItem.nameEn)
                put("loadType", exItem.loadType.ifBlank { if (exItem.defaultWeightKg <= 0f) "bodyweight" else "weight" })
                put("repUnit", exItem.repUnit.ifBlank { when {
                    exItem.id == "outdoor_run_ex" && state.outdoorGpsEnabled -> "km"
                    exItem.isBilateral -> "mp/oldal"
                    exItem.isTimed -> "sec"
                    else -> "reps"
                } })
                put("measurementType", exItem.measurementType.ifBlank { when {
                    exItem.id == "outdoor_run_ex" && state.outdoorGpsEnabled -> "distance"
                    exItem.isBilateral -> "bilateral_time"
                    exItem.isTimed -> "time"
                    else -> "reps"
                } })
                put("orderIndex", exIdx)
                put("sets", setsArray)
            })
        }

        val title = state.activeWorkoutTitleHu.ifBlank { "Edzés" }
        return JSONObject().apply {
            put("schema", 1)
            put("active", state.isWorkoutSessionActive)
            put("id", wId)
            put("workoutId", wId)
            put("workoutKey", wId)
            put("syncId", wId)
            put("started", startedIso)
            put("startedEpochMs", startedMs)
            put("activeIntervals", JSONArray().put(JSONObject().put("start", startedIso).put("end", JSONObject.NULL)))
            put("programId", if (state.activeWorkoutSubScreen == SubScreen.ACTIVE_OUTDOOR_GPS) "outdoor" else state.editingProgramId.ifBlank { "program_custom" })
            put("programName", title)
            put("dayId", "session")
            put("dayName", title)
            put("workout", title)
            put("currentExercise", state.activeExerciseIndex.coerceAtLeast(0))
            put("timerOwner", if (state.phoneControlsWorkout) "phone" else "watch")
            put("originDevice", "phone")
            put("workoutPaused", false)
            if (state.activeWorkoutSubScreen == SubScreen.ACTIVE_OUTDOOR_GPS) {
                put("studioGpsKey", state.outdoorRouteKey)
                put("studioIndoor", !state.outdoorGpsEnabled)
            }
            put("restSeconds", state.defaultRestSec)
            put("exercises", exercisesArray)
        }
    }

    private fun persistCanonicalActiveSessionAsync(installNew: Boolean = false) {
        val ctx = nativeContext ?: return
        val scope = nativeScope ?: return
        val state = _uiState.value
        if (!state.isWorkoutSessionActive) return
        val candidate = buildCanonicalSessionJsonFromState(state)
        scope.launch(Dispatchers.Main.immediate) {
            try {
                com.repforge.app.StudioWorkoutBridge.persist(ctx, candidate)
            } catch (failure: Exception) {
                if (failure is kotlinx.coroutines.CancellationException) throw failure
                _uiState.update { it.copy(statusBannerMessage = failure.message ?: "Az edzés nem menthető.") }
            }
        }
    }

    fun pauseActiveWorkout(paused: Boolean = true): Boolean {
        val ctx = nativeContext ?: return !_uiState.value.isWorkoutSessionActive
        val current = ActiveWorkoutStore.current(ctx)?.takeIf { it.optBoolean("active") } ?: return true
        // A running GPS session and acknowledged watch owner intentionally continue in background.
        if (current.optString("timerOwner", "phone") != "phone" ||
            (_uiState.value.activeWorkoutSubScreen == SubScreen.ACTIVE_OUTDOOR_GPS &&
                com.repforge.app.DistanceTrackingService.snapshot(ctx).optBoolean("active"))) return true
        return try {
            com.repforge.app.StudioWorkoutBridge.pause(ctx, paused)
            _uiState.update { it.copy(homeDemoMode = if (paused) HomeDemoMode.PAUSED_WORKOUT else it.homeDemoMode) }
            true
        } catch (failure: Exception) {
            _uiState.update { it.copy(statusBannerMessage = failure.message ?: "A szünet nem menthető.") }
            false
        }
    }

    fun onAppBackground() { pauseWhenLeavingMeasurement() }

    private fun pauseWhenLeavingMeasurement(): Boolean =
        if (_uiState.value.currentSubScreen in setOf(SubScreen.ACTIVE_STRENGTH, SubScreen.ACTIVE_TIMED, SubScreen.ACTIVE_BILATERAL, SubScreen.ACTIVE_OUTDOOR_GPS))
            pauseActiveWorkout() else true

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

        // Replace observers on an explicit rebind; cloud refresh only reloads data.
        nativeObserversJob?.cancel()
        val observersJob = SupervisorJob(scope.coroutineContext[Job])
        nativeObserversJob = observersJob
        val observersScope = CoroutineScope(scope.coroutineContext + observersJob)
        observersScope.launch {
            settingsStore.settingsFlow.map { it.healthBackupConsent }.distinctUntilChanged().collect { consent ->
                _uiState.update { it.copy(includeHealthInBackup = consent) }
            }
        }

        observersScope.launch {
            settingsStore.settingsFlow.map { it.trainingModes197Json to it.activeProgramId }
                .distinctUntilChanged().collect { (raw, programId) ->
                    val global = com.repforge.app.data.store.NativeTrainingModes.globalChoice(raw)
                    val program = com.repforge.app.data.store.NativeTrainingModes.resolveSavedJson(raw, programId)
                    val changes = com.repforge.app.data.store.NativeTrainingModeChange
                    _uiState.update { it.copy(
                        trainingStyleHu = if (global.trainingStyle == "hit") "HIT-inspirált" else "Normál hipertrófia",
                        progressionRuleHu = when (global.progressionMode) {
                            "linear" -> "Lineáris progresszió"
                            "rir" -> "RIR/Coach"
                            else -> "Dupla progresszió"
                        },
                        trainingStyleCode = global.trainingStyle,
                        progressionModeCode = global.progressionMode,
                        programTrainingStyleCode = program.trainingStyle,
                        programProgressionModeCode = program.progressionMode,
                        programTrainingModeSource = program.source,
                        activeTrainingModeProgramId = programId,
                        globalTrainingModeUndo = changes.undoValue(raw, "global", null) != null,
                        programTrainingModeUndo = changes.undoValue(raw, "program", programId) != null
                    ) }
                }
        }

        observersScope.launch(Dispatchers.Main.immediate) {
            ActiveWorkoutStore.snapshotFlow.collect { frame ->
                if (frame?.optBoolean("active") != true) {
                    if (frame != null) _uiState.update { state ->
                        if (state.activeWorkoutId != frame.optString("workoutId")) state
                        else state.copy(isWorkoutSessionActive = false, activeWorkoutId = "", activeWorkoutGeneration = 0L, workoutStartedEpochMs = 0L,
                            activeWorkoutExercises = emptyList(), activeSets = emptyList(), activeSetsByExerciseId = emptyMap(),
                            timedCurrentSec = 0, homeDemoMode = HomeDemoMode.PLANNED_NEXT,
                            currentSubScreen = if (state.currentSubScreen in activeMeasurementScreens) SubScreen.NONE else state.currentSubScreen,
                            backStack = state.backStack.filterNot { it.subScreen in activeMeasurementScreens })
                    }
                } else _uiState.update { state ->
                    if (state.activeWorkoutId != frame.optString("workoutId")) return@update state
                    val nativeSets = mutableMapOf<String, List<ExerciseSetItem>>()
                    val exercises = frame.optJSONArray("exercises") ?: JSONArray()
                    for (index in 0 until exercises.length()) {
                        val exercise = exercises.getJSONObject(index)
                        val rows = exercise.optJSONArray("sets") ?: JSONArray()
                        nativeSets[exercise.optString("id")] = (0 until rows.length()).map { rowIndex ->
                            val row = rows.getJSONObject(rowIndex)
                            ExerciseSetItem(setNumber = row.optInt("setNumber", rowIndex + 1),
                                weightKg = row.optDouble("weight", 0.0).toFloat(), reps = row.optString("reps").toDoubleOrNull()?.toInt() ?: 0,
                                rir = row.optInt("actualRir", 2), completed = row.optBoolean("done"),
                                id = row.optString("setId"), actualRirProvided = row.has("actualRir") && !row.isNull("actualRir"),
                                leftSec = row.optInt("leftSeconds", row.optInt("leftSec", 0)),
                                rightSec = row.optInt("rightSeconds", row.optInt("rightSec", 0)),
                                leftRecorded = row.optInt("leftSeconds") > 0 || row.optBoolean("leftRecorded"),
                                rightRecorded = row.optInt("rightSeconds") > 0 || row.optBoolean("rightRecorded"))
                        }
                    }
                    val selected = state.activeWorkoutExercises.getOrNull(state.activeExerciseIndex)?.id
                    state.copy(phoneControlsWorkout = frame.optString("timerOwner", "phone") == "phone" &&
                            !watchHandoffBusy && !com.repforge.app.NativePhoneReturnInstallation.requiresInstallation(frame) &&
                            !com.repforge.app.NativeWatchHandoffReceipt.pending(frame, watchJournal()),
                        watchControlPending = watchHandoffBusy || com.repforge.app.NativeWatchHandoffReceipt.pending(frame, watchJournal()),
                        activeSetsByExerciseId = state.activeSetsByExerciseId + nativeSets,
                        activeSets = nativeSets[selected] ?: state.activeSets)
                }
            }
        }

        reloadNativeEnvironment(context, settingsStore, database, scope)
    }

    private fun reloadNativeEnvironment(
        context: Context,
        settingsStore: TrainPilotSettingsStore,
        database: TrainPilotDatabase,
        scope: CoroutineScope
    ) {
        nativeReloadJob?.cancel()
        nativeReloadJob = scope.launch(Dispatchers.IO) {
            // Seed catalog into Room DB
            try {
                TrainPilotDatabase.ensureBuiltinCatalogSeeded(database, context)
            } catch (_: Throwable) {}

            val savedNativeSettings = settingsStore.current()
            val savedProfile = JSONObject(savedNativeSettings.profileJson)
            // 1. Detect if a paused or active workout is running natively
            try {
                val execution = withContext(Dispatchers.Main.immediate) { PhoneWorkoutExecution.store(context).snapshot() }
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

            val loadedFavoriteIds = runCatching { com.repforge.app.NativeExerciseFavorites.load(database).toSet() }
                .getOrDefault(emptySet())
            val exercisesMap = mappedExercises.associateBy { it.id }

            // 3. Read real programs and days from Room ProgramDao (with custom exercise prescriptions & active flag)
            val mappedPrograms = try {
                val dbPrograms = database.programDao().getProgramsWithDays()
                if (dbPrograms.isNotEmpty()) mapProgramRows(dbPrograms.map { it.program },
                    dbPrograms.flatMap { it.days }, exercisesMap, savedNativeSettings.activeProgramId)
                else mapCatalogPrograms(exercisesMap)
            } catch (_: Throwable) { mapCatalogPrograms(exercisesMap) }

            // 4. Read all body weight entries from Room sorted by user-entered dayKey
            var loadedWeightLogs = emptyList<BodyWeightLogItem>()
            var latestWeightKg = 0f
            try {
                val allWeights = database.bodyWeightDao().getAllWeights()
                loadedWeightLogs = mapBodyWeightEntitiesToLogItems(allWeights)
                val latestByDayKey = loadedWeightLogs.lastOrNull()?.kg
                val weightSummary = database.bodyWeightDao().getWeightJournalSummary()
                latestWeightKg = latestByDayKey ?: weightSummary.currentKg?.toFloat() ?: 0f
            } catch (_: Throwable) {}

            var loadedWeeklyGoal = 0
            var loadedStepsGoal = 0
            try {
                val goalsDoc = database.keyValueDocumentDao().getDocument("native:userGoals")
                if (goalsDoc != null && goalsDoc.json.isNotBlank()) {
                    val gObj = JSONObject(goalsDoc.json)
                    loadedWeeklyGoal = gObj.optInt("weeklyGoalTarget", 0)
                    loadedStepsGoal = gObj.optInt("stepsGoal", 0)
                }
            } catch (_: Throwable) {}

            // 4b. Read persisted calendar schedule & undo stack from Room
            var loadedCalendarEvents = emptyList<CalendarDayEvent>()
            var loadedCalendarUndo = emptyList<List<CalendarDayEvent>>()
            try {
                val calDoc = database.keyValueDocumentDao().getDocument("native:calendarState")
                if (calDoc != null && calDoc.json.isNotBlank()) {
                    val root = JSONObject(calDoc.json)
                    val calendarRows = database.calendarScheduleDao().getAllEntries().associateBy { it.id }
                    val legacyMonth = YearMonth.from(Instant.ofEpochMilli(calDoc.updatedAt).atZone(ZoneId.systemDefault()))
                    val evArr = root.optJSONArray("events")
                    if (evArr != null) {
                        loadedCalendarEvents = (0 until evArr.length()).mapNotNull { idx ->
                            val o = evArr.optJSONObject(idx) ?: return@mapNotNull null
                            CalendarDayEvent(
                                id = o.optString("id", "ev_$idx"),
                                dayOfMonth = o.optInt("dayOfMonth", 1).coerceIn(1, 31),
                                timeLabel = o.optString("timeLabel", "10:00"),
                                titleHu = o.optString("titleHu", "Edzés"),
                                exerciseCount = o.optInt("exerciseCount", 6),
                                durationMinutes = o.optInt("durationMinutes", 60),
                                status = runCatching { CalendarEventStatus.valueOf(o.optString("status", "PLANNED")) }.getOrDefault(CalendarEventStatus.PLANNED),
                                splitBadge = o.optString("splitBadge", "A"),
                                dateIso = o.optString("dateIso").ifBlank {
                                    calendarRows[o.optString("id")]?.date ?: legacyMonth.atDay(
                                        o.optInt("dayOfMonth", 1).coerceIn(1, legacyMonth.lengthOfMonth())).toString()
                                }
                            )
                        }
                    }
                    val undoArr = root.optJSONArray("undoStack")
                    if (undoArr != null) {
                        loadedCalendarUndo = (0 until undoArr.length()).mapNotNull { uIdx ->
                            val snapArr = undoArr.optJSONArray(uIdx) ?: return@mapNotNull null
                            (0 until snapArr.length()).mapNotNull { idx ->
                                val o = snapArr.optJSONObject(idx) ?: return@mapNotNull null
                                CalendarDayEvent(
                                    id = o.optString("id", "ev_$idx"),
                                    dayOfMonth = o.optInt("dayOfMonth", 1).coerceIn(1, 31),
                                    timeLabel = o.optString("timeLabel", "10:00"),
                                    titleHu = o.optString("titleHu", "Edzés"),
                                    exerciseCount = o.optInt("exerciseCount", 6),
                                    durationMinutes = o.optInt("durationMinutes", 60),
                                    status = runCatching { CalendarEventStatus.valueOf(o.optString("status", "PLANNED")) }.getOrDefault(CalendarEventStatus.PLANNED),
                                    splitBadge = o.optString("splitBadge", "A"),
                                dateIso = o.optString("dateIso").ifBlank {
                                    calendarRows[o.optString("id")]?.date ?: legacyMonth.atDay(
                                        o.optInt("dayOfMonth", 1).coerceIn(1, legacyMonth.lengthOfMonth())).toString()
                                }
                                )
                            }
                        }
                    }
                }
            } catch (_: Throwable) {}

            // 5. Read persisted workouts from Room (with real durations and exerciseDetails)
            val mappedWorkouts = try {
                val dbWorkouts = database.workoutHistoryDao().getAllWorkoutsWithDetails()
                dbWorkouts.map { detail ->
                    val w = detail.workout
                    val exerciseNames = detail.exercises.joinToString(", ") { exWithSets ->
                        val ex = exWithSets.exercise
                        ex.nameHu.ifBlank { exercisesMap[ex.exerciseId]?.nameHu.orEmpty() }.ifBlank { "Gyakorlat" }
                    }
                    val allSets = detail.exercises.flatMap { it.sets }
                    val completedDbSets = allSets.filter { it.done }
                    val isOutdoor = detail.exercises.any {
                        val ex = it.exercise
                        ex.measurementType == "distance" || ex.repUnit == "km"
                    } || runCatching { JSONObject(w.rawJson).has(com.repforge.app.NativeGpsMeasurement.FIELD) }.getOrDefault(false)
                    // Long pauses are already excluded from the canonical Room duration.
                    val elapsedSec = w.durationSeconds.coerceAtLeast(0)
                    val realDurationMin = if (elapsedSec > 0) (elapsedSec + 59) / 60 else 0
                    val realWorkLabel = if (elapsedSec > 0) String.format("%02d:%02d", elapsedSec / 60, elapsedSec % 60) else "—"
                    val mappedDetails = detail.exercises.map { exWithSets ->
                        val ex = exWithSets.exercise
                        val exName = ex.nameHu.ifBlank { exercisesMap[ex.exerciseId]?.nameHu.orEmpty() }.ifBlank { "Gyakorlat" }
                        val mappedSets = exWithSets.sets.mapIndexed { sIdx, sEntity ->
                            ExerciseSetItem(
                                setNumber = if (sEntity.setNumber > 0) sEntity.setNumber else (sIdx + 1),
                                weightKg = (sEntity.weight ?: 0.0).toFloat(),
                                reps = sEntity.reps.toIntOrNull() ?: 0,
                                rir = sEntity.actualRir ?: 2, actualRirProvided = sEntity.actualRir != null,
                                completed = sEntity.done,
                                distanceMeters = sEntity.distanceMeters
                            )
                        }
                        com.example.model.LoggedExerciseDetail(
                            nameHu = exName,
                            summaryPrescription = if (isOutdoor || ex.measurementType == "distance") "Kültéri aktivitás" else "${mappedSets.count { it.completed }}/${mappedSets.size} sorozat",
                            sets = mappedSets,
                            measurementType = ex.measurementType,
                            repUnit = ex.repUnit
                        )
                    }
                    LoggedWorkoutEntry(
                        id = w.id,
                        dateLabelHu = w.started.take(16).replace('T', ' '),
                        titleHu = w.programName.ifBlank { w.dayName }.ifBlank { "Edzés" },
                        durationMinutes = realDurationMin,
                        workTimeLabel = realWorkLabel,
                        restTimeLabel = "—",
                        totalSets = if (w.completedSetsCount > 0) w.completedSetsCount else completedDbSets.size,
                        totalVolumeKg = if (w.totalVolumeKg > 0.0) w.totalVolumeKg.roundToInt() else completedDbSets.sumOf { (it.weight ?: 0.0) * (it.reps.toDoubleOrNull() ?: 0.0) }.roundToInt(),
                        summaryExercisesHu = if (isOutdoor) w.programName.ifBlank { "Kültéri aktivitás" } else exerciseNames.ifBlank { "Rögzített gyakorlatok" },
                        hasPr = w.hasPr226,
                        isOutdoorRun = isOutdoor,
                        routeKeys = detail.exercises.flatMap { it.sets }.flatMap {
                            listOfNotNull(it.gpsRouteKey163, it.gpsRouteKey193)
                        }.filter(String::isNotBlank).distinct(),
                        sourceWatch = com.repforge.app.NativeSavedWearHealth.read(w) != null, sourceHealthConnect = !w.health240Json.isNullOrBlank(),
                        distanceKm = (completedDbSets.sumOf { it.distanceMeters ?: 0.0 } / 1000.0).toFloat(),
                        paceLabel = completedDbSets.sumOf { it.distanceMeters ?: 0.0 }.takeIf { it > 0 && elapsedSec > 0 }?.let { meters ->
                            val secondsPerKm = (elapsedSec / (meters / 1000.0)).toInt()
                            "%d:%02d /km".format(secondsPerKm / 60, secondsPerKm % 60)
                        } ?: "—",
                        originDevice = w.originDevice,
                        savedStartIso = w.started,
                        savedFinishIso = w.finished,
                        photoCount = detail.photos.size,
                        exerciseDetails = mappedDetails
                    )
                }
            } catch (_: Throwable) { emptyList() }

            // 6. Read real daily health projections from HealthJournalStore using sleepMinutes
            val todayStr = LocalDate.now().toString()
            val healthProviderAtBind = HealthJournalStore.get(context).preferences().optString("provider", HealthJournalStore.HC)
            val healthGenerationAtBind = HealthJournalStore.get(context).generation()
            var stepsVal = 0
            var restingHrVal = 0
            var currentHrVal = 0
            var activeCalVal = 0
            var sleepStr = "—"
            var sleepDecimal = "—"

            try {
                val store = HealthJournalStore.get(context)
                val provider = store.preferences().optString("provider", HealthJournalStore.HC)
                val todayData = com.repforge.app.NativeHealthProviderData.day(store, provider, LocalDate.now())
                if (todayData != null) {
                    val stepsDbl = todayData.optDouble("steps", Double.NaN)
                    val restingDbl = todayData.optDouble("restingHeartRate", Double.NaN)
                    val currentDbl = todayData.optDouble("averageHeartRate", Double.NaN)
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
                val restoredActiveSnap = runCatching {
                    ActiveWorkoutStore.current(context)?.takeIf { it.optBoolean("active", false) }
                        ?: PhoneWorkoutExecution.store(context).snapshot()?.takeIf { it.optBoolean("active", false) }
                }.getOrNull()
                _uiState.update { st ->
                    val activeProg = mappedPrograms.firstOrNull { it.isActive } ?: mappedPrograms.firstOrNull()
                    val firstDay = activeProg?.days?.firstOrNull()
                    var restoredExercises = emptyList<ExerciseItem>()
                    val restoredSetsMap = mutableMapOf<String, List<ExerciseSetItem>>()
                    if (restoredActiveSnap != null) {
                        val exArr = restoredActiveSnap.optJSONArray("exercises")
                        if (exArr != null && exArr.length() > 0) {
                            val list = mutableListOf<ExerciseItem>()
                            for (i in 0 until exArr.length()) {
                                val exObj = exArr.optJSONObject(i) ?: continue
                                val exId = exObj.optString("id").ifBlank { exObj.optString("exerciseId") }
                                val catalogEx = exercisesMap[exId] ?: ExerciseItem(
                                    id = exId.ifBlank { "restored_$i" },
                                    nameHu = exObj.optString("nameHu").ifBlank { exObj.optString("name").ifBlank { "Gyakorlat" } },
                                    isTimed = exObj.optString("measurementType").contains("time"),
                                    isBilateral = exObj.optString("measurementType").contains("bilateral") || exObj.optString("repUnit").contains("oldal")
                                )
                                list.add(com.example.ui.components.withNativeStrengthUnits(catalogEx,
                                    exObj.opt("loadType") as? String, exObj.opt("repUnit") as? String).copy(
                                        measurementType = exObj.optString("measurementType"),
                                        isTimed = com.repforge.app.NativeSetStopwatch.timed(exObj),
                                        isBilateral = com.repforge.app.NativeSetStopwatch.bilateral(exObj)))
                                val setsArr = exObj.optJSONArray("sets")
                                if (setsArr != null && setsArr.length() > 0) {
                                    val sList = (0 until setsArr.length()).mapNotNull { sIdx ->
                                        val sObj = setsArr.optJSONObject(sIdx) ?: return@mapNotNull null
                                        val doneFlag = sObj.optBoolean("done", false)
                                        val lSec = sObj.optInt("leftSeconds", sObj.optInt("leftSec", 0))
                                        val rSec = sObj.optInt("rightSeconds", sObj.optInt("rightSec", 0))
                                        ExerciseSetItem(
                                            setNumber = sObj.optInt("setNumber", sObj.optInt("set", sIdx + 1)),
                                            weightKg = sObj.optDouble("weight", catalogEx.defaultWeightKg.toDouble()).toFloat(),
                                            reps = sObj.optString("reps").toIntOrNull() ?: catalogEx.defaultReps,
                                            rir = sObj.optInt("actualRir", 2), id = sObj.optString("setId"),
                                            actualRirProvided = sObj.has("actualRir") && !sObj.isNull("actualRir"),
                                            completed = doneFlag,
                                            leftSec = lSec,
                                            rightSec = rSec,
                                            leftRecorded = sObj.optBoolean("leftRecorded", doneFlag),
                                            rightRecorded = sObj.optBoolean("rightRecorded", doneFlag)
                                        )
                                    }
                                    if (sList.isNotEmpty()) {
                                        restoredSetsMap[catalogEx.id] = sList
                                    }
                                }
                            }
                            restoredExercises = list
                        }
                    }

                    val initialWorkoutExs = when {
                        restoredExercises.isNotEmpty() -> restoredExercises
                        st.activeWorkoutExercises.isNotEmpty() -> st.activeWorkoutExercises
                        else -> firstDay?.exercises?.ifEmpty { mappedExercises.take(6) } ?: mappedExercises.take(6)
                    }
                    val restoredIdx = if (restoredActiveSnap != null && initialWorkoutExs.isNotEmpty()) {
                        restoredActiveSnap.optInt("currentExercise", 0).coerceIn(0, initialWorkoutExs.lastIndex)
                    } else st.activeExerciseIndex.coerceIn(0, (initialWorkoutExs.size - 1).coerceAtLeast(0))
                    val currentActiveEx = initialWorkoutExs.getOrNull(restoredIdx) ?: mappedExercises.firstOrNull()
                    val activeSetsForCurrent = if (currentActiveEx != null) {
                        restoredSetsMap[currentActiveEx.id]
                            ?: st.activeSetsByExerciseId[currentActiveEx.id]
                            ?: buildDefaultSetsForExercise(currentActiveEx)
                    } else st.activeSets

                    val computedPrs = computePrRecordsFromWorkouts(mappedWorkouts)
                    val sameHealthSource = HealthJournalStore.get(context).generation() == healthGenerationAtBind &&
                        HealthJournalStore.get(context).preferences().optString("provider", HealthJournalStore.HC) == healthProviderAtBind
                    st.copy(
                        exercises = mappedExercises,
                        favoriteExerciseIds = loadedFavoriteIds,
                        profileComplete = com.example.model.StudioProfileCompletion.complete(savedProfile),
                        userHeightCm = savedProfile.optInt("height", st.userHeightCm),
                        userWeightKg = savedProfile.optDouble("weight", st.userWeightKg.toDouble()).toFloat(),
                        userFitnessGoalHu = savedProfile.optString("goal", st.userFitnessGoalHu),
                        userExperienceLevelHu = savedProfile.optString("experience", st.userExperienceLevelHu),
                        userWorkoutLocationHu = savedProfile.optString("location", st.userWorkoutLocationHu),
                        userSelectedEquipmentHu = savedProfile.optJSONArray("gear")?.let { arr ->
                            (0 until arr.length()).joinToString(", ") { arr.optString(it) }
                        } ?: st.userSelectedEquipmentHu,
                        userScheduleDaysPerWeek = savedProfile.optInt("daysPerWeek", st.userScheduleDaysPerWeek),
                        userWorkoutDurationMin = savedProfile.optInt("minutes", st.userWorkoutDurationMin),
                        userExcludedMusclesHu = savedProfile.optJSONArray("avoidAreas")?.let { arr ->
                            (0 until arr.length()).map { arr.optString(it) }.toSet()
                        } ?: st.userExcludedMusclesHu,
                        userExcludedExercisesHu = savedProfile.optJSONArray("excluded")?.let { arr ->
                            (0 until arr.length()).map { arr.optString(it) }.toSet()
                        } ?: st.userExcludedExercisesHu,
                        selectedExerciseId = if (restoredActiveSnap != null) currentActiveEx?.id ?: st.selectedExerciseId
                            else st.selectedExerciseId.takeIf { id -> mappedExercises.any { it.id == id } }
                                ?: mappedExercises.firstOrNull()?.id.orEmpty(),
                        programs = mappedPrograms,
                        editingProgramId = if (st.currentSubScreen == SubScreen.PROGRAM_EDITOR &&
                            mappedPrograms.any { it.id == st.editingProgramId }) st.editingProgramId
                            else activeProg?.id ?: st.editingProgramId,
                        calendarEvents = if (loadedCalendarEvents.isNotEmpty()) loadedCalendarEvents else st.calendarEvents,
                        calendarUndoStack = if (loadedCalendarUndo.isNotEmpty()) loadedCalendarUndo else st.calendarUndoStack,
                        isWorkoutSessionActive = restoredActiveSnap != null,
                        outdoorRouteKey = restoredActiveSnap?.optString("studioGpsKey") ?: st.outdoorRouteKey,
                        outdoorGpsEnabled = restoredActiveSnap?.takeIf { it.has("studioIndoor") }?.let { !it.optBoolean("studioIndoor") } ?: st.outdoorGpsEnabled,
                        activeWorkoutId = restoredActiveSnap?.optString("workoutId").orEmpty(),
                        activeWorkoutGeneration = restoredActiveSnap?.optLong("generation") ?: 0L,
                        workoutStartedEpochMs = restoredActiveSnap?.optLong("startedEpochMs", 0L)?.takeIf { it > 0L } ?: st.workoutStartedEpochMs,
                        activeWorkoutSubScreen = if (restoredActiveSnap?.has("studioIndoor") == true) SubScreen.ACTIVE_OUTDOOR_GPS
                            else if (restoredActiveSnap != null) targetSubScreenForExercise(currentActiveEx) else st.activeWorkoutSubScreen,
                        activeWorkoutTitleHu = restoredActiveSnap?.optString("programName")?.takeIf { it.isNotBlank() }
                            ?: if (activeProg != null) "${activeProg.nameHu} • ${firstDay?.dayCode ?: "A nap"}" else st.activeWorkoutTitleHu,
                        activeWorkoutExercises = initialWorkoutExs,
                        activeExerciseIndex = restoredIdx,
                        activeSets = activeSetsForCurrent,
                        activeSetsByExerciseId = if (restoredSetsMap.isNotEmpty()) restoredSetsMap else st.activeSetsByExerciseId,
                        loggedWorkouts = mappedWorkouts,
                        privatePhotoCount = mappedWorkouts.sumOf { it.photoCount },
                        prRecords = computedPrs,
                        weeklyGoalCompleted = mappedWorkouts.workoutsInCalendarWeekToDate(LocalDate.now()).size,
                        weeklyGoalTarget = if (loadedWeeklyGoal > 0) loadedWeeklyGoal else st.weeklyGoalTarget,
                        stepsGoal = if (loadedStepsGoal > 0) loadedStepsGoal else st.stepsGoal,
                        currentBodyWeightKg = if (latestWeightKg > 0f) latestWeightKg else st.currentBodyWeightKg,
                        bodyWeightLogs = loadedWeightLogs,
                        dailySteps = if (sameHealthSource) stepsVal else st.dailySteps,
                        restingHrBpm = if (sameHealthSource) restingHrVal else st.restingHrBpm,
                        currentHrBpm = if (sameHealthSource) currentHrVal else st.currentHrBpm,
                        activeCaloriesKcal = if (sameHealthSource) activeCalVal else st.activeCaloriesKcal,
                        sleepDurationLabel = if (sameHealthSource) sleepStr else st.sleepDurationLabel,
                        sleepHoursDecimal = if (sameHealthSource) sleepDecimal else st.sleepHoursDecimal
                    )
                }
            }

            withContext(Dispatchers.Main) { reloadHealthJournal() }

            // 7. Load real muscle load summary from Room
            loadMuscleLoadSummary(progressPeriodDays(_uiState.value.selectedProgressRangeIndex))
            _uiState.value.selectedLogWorkoutId.takeIf { it.isNotBlank() }?.let(::loadWorkoutMuscleSummary)
            reloadPersonalRecords()
        }
    }

    fun selectTab(tab: MainTab) {
        if (!pauseWhenLeavingMeasurement()) return
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
        if (subScreen !in setOf(SubScreen.EXERCISE_LIBRARY, SubScreen.EXERCISE_DETAIL)) exerciseSelection = null
        if (subScreen != _uiState.value.currentSubScreen && !pauseWhenLeavingMeasurement()) return
        val isActiveMeasurement = subScreen == SubScreen.ACTIVE_STRENGTH ||
            subScreen == SubScreen.ACTIVE_TIMED ||
            subScreen == SubScreen.ACTIVE_BILATERAL ||
            subScreen == SubScreen.ACTIVE_OUTDOOR_GPS

        if (subScreen == SubScreen.ACTIVE_OUTDOOR_GPS && _uiState.value.activeWorkoutSubScreen != SubScreen.ACTIVE_OUTDOOR_GPS) {
            startOutdoorWorkout("Kültéri futás")
            return
        }

        _uiState.update { state ->
            val updatedStack = (state.backStack + NavRoute(state.currentTab, state.currentSubScreen)).takeLast(16)
            state.copy(
                currentTab = targetTab ?: state.currentTab,
                currentSubScreen = subScreen,
                privatePhotoWorkoutId = if (subScreen == SubScreen.PRIVATE_PHOTOS) null else state.privatePhotoWorkoutId,
                isWorkoutSessionActive = if (isActiveMeasurement) true else state.isWorkoutSessionActive,
                activeWorkoutSubScreen = if (isActiveMeasurement) subScreen else state.activeWorkoutSubScreen,
                backStack = updatedStack
            )
        }
    }

    fun openLogWorkoutPhotos() {
        val id = _uiState.value.selectedLogWorkoutId ?: return
        navigateToSubScreen(SubScreen.PRIVATE_PHOTOS)
        if (_uiState.value.currentSubScreen == SubScreen.PRIVATE_PHOTOS)
            _uiState.update { it.copy(privatePhotoWorkoutId = id) }
    }

    fun returnToActiveWorkout() {
        if (!pauseActiveWorkout(false)) return
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

    fun stopActiveWorkoutSession(expectedWorkoutId: String? = null, expectedGeneration: Long? = null) {
        if (_uiState.value.isSavingWorkout) return
        val ctx = nativeContext ?: return
        val db = nativeDatabase ?: return
        val scope = nativeScope ?: return
        val targetId = expectedWorkoutId ?: _uiState.value.activeWorkoutId.takeIf(String::isNotBlank) ?: return
        val targetGeneration = expectedGeneration?.takeIf { it > 0 } ?: _uiState.value.activeWorkoutGeneration.takeIf { it > 0 }
        _uiState.update { it.copy(isSavingWorkout = true) }
        scope.launch(Dispatchers.Main.immediate) {
            try {
                check(!com.repforge.app.NativeDataReplacement.blocked(ctx)) { "Adatcsere van folyamatban." }
                val snapshot = ActiveWorkoutStore.current(ctx)
                val execution = PhoneWorkoutExecution.store(ctx).snapshot()
                val alreadyClosed = snapshot?.optString("workoutId") == targetId && !snapshot.optBoolean("active") &&
                    execution?.optString("workoutId") == targetId && !execution.optBoolean("active") &&
                    (targetGeneration == null || execution.optLong("generation") == targetGeneration)
                val active = snapshot?.takeIf { it.optBoolean("active") }
                if (!alreadyClosed) check(active?.optString("workoutId") == targetId &&
                    (targetGeneration == null || active.optLong("generation") == targetGeneration) &&
                    _uiState.value.activeWorkoutId == targetId) { "Az elvetendő edzés megváltozott. Nyisd meg újra a műveletet." }
                if (active != null) {
                    check(active.optString("workoutId") == _uiState.value.activeWorkoutId)
                    TrainPilotWorkoutFinalization(ctx, db).discardWorkout(active)
                }
                _uiState.update {
                    if (it.activeWorkoutId.isNotBlank() && it.activeWorkoutId != targetId) return@update it
                    it.copy(isWorkoutSessionActive = false, activeWorkoutId = "", activeWorkoutGeneration = 0L, workoutStartedEpochMs = 0,
                    outdoorRouteKey = "", outdoorGpsEnabled = false, homeDemoMode = HomeDemoMode.PLANNED_NEXT,
                    activeWorkoutExercises = emptyList(), activeSets = emptyList(), activeSetsByExerciseId = emptyMap(),
                    backStack = it.backStack.filterNot { route -> route.subScreen in activeMeasurementScreens },
                    currentSubScreen = SubScreen.NONE, currentTab = MainTab.HOME,
                    statusBannerMessage = if (alreadyClosed) "Az edzés már lezárult." else "Edzés elvetve.") }
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                _uiState.update { it.copy(statusBannerMessage = error.message ?: "Az edzés elvetése nem sikerült.") }
                if (targetId != null) refreshAfterCloudSync(ctx)
            } finally { _uiState.update { it.copy(isSavingWorkout = false) } }
        }
    }

    fun navigateBack(): Boolean {
        if (_uiState.value.currentSubScreen == SubScreen.EXERCISE_LIBRARY) exerciseSelection = null
        if (!pauseWhenLeavingMeasurement()) return true
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
                statusBannerMessage = tr(
                    "Színpaletta frissítve (#$paletteId)",
                    "Color palette updated (#$paletteId)",
                    "Farbpalette aktualisiert (#$paletteId)",
                    "Paletă de culori actualizată (#$paletteId)",
                    it.language
                )
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
                statusBannerMessage = tr(
                    "Nyelv: ${lang.label} (${lang.code})",
                    "Language: ${lang.label} (${lang.code})",
                    "Sprache: ${lang.label} (${lang.code})",
                    "Limbă: ${lang.label} (${lang.code})",
                    lang
                )
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
        // Demo layout selection must never fabricate health/Coach measurements.
        _uiState.update { it.copy(homeDemoMode = mode) }
    }

    fun toggleGoalsDialog(show: Boolean) {
        _uiState.update { it.copy(showGoalsDialog = show) }
    }

    fun updateGoals(weeklyTarget: Int, stepsTarget: Int) {
        val cleanWeekly = weeklyTarget.coerceIn(1, 30)
        val cleanSteps = stepsTarget.coerceIn(2000, 30000)
        val db = nativeDatabase
        val scope = nativeScope
        _uiState.update {
            it.copy(
                weeklyGoalTarget = cleanWeekly,
                stepsGoal = cleanSteps,
                showGoalsDialog = false,
                statusBannerMessage = tr(
                    "Célok elmentve: $cleanWeekly edzés, $cleanSteps lépés",
                    "Goals saved: $cleanWeekly workouts, $cleanSteps steps",
                    "Ziele gespeichert: $cleanWeekly Trainings, $cleanSteps Schritte",
                    "Obiective salvate: $cleanWeekly antrenamente, $cleanSteps pași",
                    it.language
                )
            )
        }
        if (db == null || scope == null) return
        scope.launch(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("weeklyGoalTarget", cleanWeekly)
                    put("stepsGoal", cleanSteps)
                }.toString()
                db.keyValueDocumentDao().putDocument(
                    com.repforge.app.data.db.KeyValueDocumentEntity("native:userGoals", json, System.currentTimeMillis())
                )
            } catch (e: Throwable) {
                android.util.Log.e("TrainPilotViewModel", "Failed to persist user goals to Room", e)
            }
        }
    }

    fun togglePersonalPlannerModal(show: Boolean) {
        _uiState.update { it.copy(showPersonalPlannerModal = show) }
    }

    fun toggleQuickWorkoutSheet(show: Boolean) {
        _uiState.update { it.copy(showQuickWorkoutSheet = show) }
    }

    // Program & Exercise Editor Actions
    internal suspend fun awaitStudioProgramWrites() {
        programWriteJob?.join()
        check(programWriteError == null) { requireNotNull(programWriteError) }
    }

    private fun mutateStudioLibrary(
        success: String,
        editor: Boolean = false,
        selectLastDay: Boolean = false,
        action: suspend (Context, TrainPilotDatabase, TrainPilotSettingsStore) -> String?
    ) {
        if (_uiState.value.libraryMutationBusy) return
        val context = nativeContext ?: return
        val db = nativeDatabase ?: return
        val store = nativeSettingsStore ?: return
        val scope = nativeScope ?: return
        val ticket = try { com.repforge.app.NativeDataReplacement.continuationForCurrentData(context) }
            catch (_: Exception) { return }
        val editorTicket = if (editor) programEditorContinuation ?: ticket else null
        nativeReloadJob?.cancel()
        _uiState.update { it.copy(libraryMutationBusy = true) }
        scope.launch {
            try {
                awaitStudioProgramWrites()
                val focus = withContext(Dispatchers.IO) {
                    programWriteMutex.withLock {
                        com.repforge.app.NativeDataReplacement.mutate(context) {
                            ticket.requireCurrent(context)
                            editorTicket?.requireCurrent(context)
                            action(context, db, store)
                        }
                    }
                }
                val loaded = withContext(Dispatchers.IO) {
                    Triple(db.exerciseLibraryDao().getAllExercises(), db.programDao().getProgramsWithDays(),
                        com.repforge.app.NativeExerciseFavorites.load(db).toSet())
                }
                ticket.requireCurrent(context)
                val exercises = loaded.first.map(::mapExerciseEntity)
                val programs = mapProgramRows(loaded.second.map { it.program }, loaded.second.flatMap { it.days },
                    exercises.associateBy { it.id }, store.current().activeProgramId)
                _uiState.update { state ->
                    val editing = focus ?: state.editingProgramId
                    val missing = programs.none { it.id == editing }
                    state.copy(exercises = exercises, programs = programs, favoriteExerciseIds = loaded.third,
                        editingProgramId = if (missing) programs.firstOrNull()?.id.orEmpty() else editing,
                        programsTabCustomOnly = if (focus != null && focus != state.editingProgramId &&
                            programs.any { it.id == focus && it.isCustom }) true else state.programsTabCustomOnly,
                        editingDayIndex = if (selectLastDay) (programs.firstOrNull { it.id == focus }?.days?.lastIndex ?: 0).coerceAtLeast(0)
                            else if (focus != null) state.editingDayIndex.coerceIn(0,
                            (programs.firstOrNull { it.id == focus }?.days?.lastIndex ?: 0).coerceAtLeast(0)) else state.editingDayIndex,
                        currentSubScreen = if (missing && state.currentSubScreen == SubScreen.PROGRAM_EDITOR)
                            SubScreen.PROGRAMS_LIST else state.currentSubScreen,
                        expandedEditorExerciseId = null,
                        statusBannerMessage = success)
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (error: Exception) {
                val detail = error.message.orEmpty()
                val message = when {
                    detail.contains("Deactivate", true) -> "Törlés előtt válassz másik aktív programot."
                    detail.contains("active", true) || detail.contains("transferring", true) ->
                        "Előbb fejezd be az aktív vagy átadás alatt lévő edzést. Törlés előtt válassz másik aktív programot."
                    detail.contains("history", true) || detail.contains("calendar", true) ->
                        "A naplóban vagy a naptárban használt program/nap nem törölhető."
                    detail.contains("Built-in", true) -> "A beépített sablon nem törölhető; készíts saját másolatot."
                    detail.contains("changed", true) || detail.contains("replacement", true) ->
                        "Az adatok megváltoztak. Nyisd meg újra a szerkesztőt."
                    else -> "A művelet nem sikerült. Ellenőrizd a megadott adatokat és nyisd meg újra."
                }
                _uiState.update { it.copy(statusBannerMessage = message) }
            } finally { _uiState.update { it.copy(libraryMutationBusy = false) } }
        }
    }

    private fun editStudioProgram(
        success: String,
        selectLastDay: Boolean = false,
        change: (com.repforge.app.NativeProgramEditor.Draft, String?) -> com.repforge.app.NativeProgramEditor.Draft
    ) {
        val state = _uiState.value
        val programId = state.editingProgramId
        val dayKey = state.programs.firstOrNull { it.id == programId }?.days?.getOrNull(state.editingDayIndex)?.id
        val language = state.language.name.lowercase(java.util.Locale.ROOT)
        mutateStudioLibrary(success, editor = true, selectLastDay = selectLastDay) { context, db, _ ->
            val opened = requireNotNull(db.programDao().getProgramWithDaysById(programId))
            val rawDayId = opened.days.firstOrNull { it.compositeId == dayKey || it.dayId == dayKey }?.dayId
            val draft = change(com.repforge.app.NativeProgramEditor.from(opened, language), rawDayId)
            com.repforge.app.NativeProgramEditor.save(context, db, opened, draft, language)
        }
    }

    fun renameEditorProgram(name: String) = editStudioProgram(
        tr("Programnév elmentve.", "Program name saved.", "Programmname gespeichert.", "Numele programului a fost salvat.")
    ) { draft, _ ->
        draft.copy(name = name.trim())
    }

    fun copyEditorProgram() {
        val state = _uiState.value
        val copySuffix = tr(" – másolat", " – copy", " – Kopie", " – copie", state.language)
        mutateStudioLibrary(
            tr(
                "Saját programmásolat elkészült. Aktiválás a programlistában.",
                "Custom program copy created. Activate it in the program list.",
                "Eigene Programmkopie erstellt. In der Programmliste aktivieren.",
                "Copia personalizată a programului a fost creată. Activeaz-o în listă.",
                state.language
            ),
            editor = true
        ) { context, db, _ ->
            val opened = requireNotNull(db.programDao().getProgramWithDaysById(state.editingProgramId))
            val language = state.language.name.lowercase(java.util.Locale.ROOT)
            val draft = com.repforge.app.NativeProgramEditor.from(opened, language)
            com.repforge.app.NativeProgramEditor.save(context, db, opened,
                draft.copy(name = (draft.name.take(107) + copySuffix)), language, clone = true)
        }
    }

    fun deleteEditorProgram() {
        val id = _uiState.value.editingProgramId
        mutateStudioLibrary(
            tr(
                "Program végleg törölve a tárból.",
                "Program permanently deleted.",
                "Programm dauerhaft gelöscht.",
                "Program șters definitiv."
            ),
            editor = true
        ) { context, db, store ->
            com.repforge.app.NativeProgramEditor.delete(context, db,
                requireNotNull(db.programDao().getProgramWithDaysById(id)), store)
            null
        }
    }

    fun addEditorDay(name: String) = editStudioProgram(
        tr("Edzésnap hozzáadva.", "Workout day added.", "Trainingstag hinzugefügt.", "Zi de antrenament adăugată."),
        selectLastDay = true
    ) { draft, _ ->
        com.repforge.app.NativeProgramEditor.addDay(draft, name.trim())
    }

    fun deleteEditorDay() = editStudioProgram(
        tr("Edzésnap törölve.", "Workout day deleted.", "Trainingstag gelöscht.", "Zi de antrenament ștearsă.")
    ) { draft, dayId ->
        require(dayId != null && draft.days.size > 1)
        draft.copy(days = draft.days.filterNot { it.id == dayId })
    }

    fun removeEditorExercise(id: String) = editStudioProgram(
        tr(
            "Gyakorlat eltávolítva az edzésnapból.",
            "Exercise removed from workout day.",
            "Übung aus dem Trainingstag entfernt.",
            "Exercițiu eliminat din ziua de antrenament."
        )
    ) { draft, dayId ->
        require(dayId != null)
        draft.copy(days = draft.days.map { day -> if (day.id != dayId) day else day.copy(
            entries = day.entries.filterNot { com.repforge.app.NativeProgramEditor.exerciseId(it) == id }) })
    }

    fun toggleExerciseFavorite(id: String) = mutateStudioLibrary(
        tr("Kedvencek frissítve.", "Favorites updated.", "Favoriten aktualisiert.", "Favorite actualizate.")
    ) { context, db, _ ->
        com.repforge.app.NativeExerciseFavorites.toggle(context, db, id)
        null
    }

    fun toggleFavoriteExercisesOnly() {
        _uiState.update { it.copy(favoriteExercisesOnly = !it.favoriteExercisesOnly) }
    }

    fun setProgramsTabCustomOnly(customOnly: Boolean) {
        _uiState.update { it.copy(programsTabCustomOnly = customOnly) }
    }

    fun activateProgram(programId: String) {
        if (_uiState.value.libraryMutationBusy) return
        _uiState.update { state ->
            val updated = state.programs.map { prog ->
                prog.copy(isActive = prog.id == programId)
            }
            state.copy(
                programs = updated,
                editingProgramId = programId,
                statusBannerMessage = tr(
                    "Aktív program frissítve",
                    "Active program updated",
                    "Aktives Programm aktualisiert",
                    "Program activ actualizat",
                    state.language
                )
            )
        }
        persistProgramsToRoomAsync()
    }

    fun openProgramEditor(programId: String, dayIndex: Int = 0) {
        programEditorContinuation = nativeContext?.let {
            runCatching { com.repforge.app.NativeDataReplacement.continuationForCurrentData(it) }.getOrNull()
        }
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
        if (_uiState.value.libraryMutationBusy) return
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
                                        defaultReps = reps.coerceIn(1, 50), targetRepsText = reps.coerceIn(1, 50).toString(),
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
                statusBannerMessage = tr(
                    "Gyakorlat beállításai elmentve",
                    "Exercise settings saved",
                    "Übungseinstellungen gespeichert",
                    "Setările exercițiului au fost salvate",
                    state.language
                )
            )
        }
        persistProgramsToRoomAsync()
    }

    fun addExerciseToActiveProgramDay(exercise: ExerciseItem) {
        if (_uiState.value.libraryMutationBusy) return
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
            val exName = exercise.localizedName(state.language)
            state.copy(
                programs = updatedPrograms,
                statusBannerMessage = tr(
                    "${exercise.nameHu} hozzáadva az edzésnaphoz",
                    "$exName added to workout day",
                    "$exName zum Trainingstag hinzugefügt",
                    "$exName adăugat în ziua de antrenament",
                    state.language
                )
            )
        }
        persistProgramsToRoomAsync()
    }

    fun moveEditorExercise(fromIndex: Int, toIndex: Int) {
        if (_uiState.value.libraryMutationBusy) return
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
                statusBannerMessage = tr(
                    "Gyakorlatsorrend frissítve",
                    "Exercise order updated",
                    "Übungsreihenfolge aktualisiert",
                    "Ordinea exercițiilor a fost actualizată",
                    state.language
                )
            )
        }
        persistProgramsToRoomAsync()
    }

    fun moveProgramOrder(fromIndex: Int, toIndex: Int) {
        if (_uiState.value.libraryMutationBusy) return
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
                statusBannerMessage = tr(
                    "Programok sorrendje frissítve",
                    "Program order updated",
                    "Programmreihenfolge aktualisiert",
                    "Ordinea programelor a fost actualizată",
                    state.language
                )
            )
        }
        persistProgramsToRoomAsync()
    }

    fun createNewCustomProgram(nameHu: String, subtitleHu: String) {
        if (_uiState.value.libraryMutationBusy) return
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
                statusBannerMessage = tr(
                    "Új egyedi program létrehozva: $cleanName",
                    "New custom program created: $cleanName",
                    "Neues eigenes Programm erstellt: $cleanName",
                    "Program personalizat nou creat: $cleanName",
                    state.language
                )
            )
        }
        persistProgramsToRoomAsync()
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
        if (_uiState.value.libraryMutationBusy) return
        val cleanName = nameHu.trim().ifBlank { "Egyedi gyakorlat" }
        val cleanEquip = equipmentHu.trim().ifBlank { "Saját testsúly / Egyedi" }
        val cleanDesc = descriptionHu.trim().ifBlank { "Egyedi felhasználói gyakorlat saját leírással és végrehajtási jegyzettel." }
        val cleanVideoUrl = videoUrl.trim()
        val parsedSteps = cleanDesc
            .split("\n", ".", ";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .ifEmpty { listOf(cleanDesc) }

        val resolvedCategory = if (category == ExerciseCategory.ALL) ExerciseCategory.CORE else category
        val newExercise = ExerciseItem(
            id = "custom_ex_${System.currentTimeMillis()}",
            nameHu = cleanName,
            nameEn = cleanName,
            nameDe = cleanName,
            nameRo = cleanName,
            category = resolvedCategory,
            equipmentHu = cleanEquip,
            primaryMusclesHu = resolvedCategory.labelHu,
            secondaryMusclesHu = if (cleanVideoUrl.isNotBlank()) "Videó: $cleanVideoUrl" else "Stabilizáló izmok",
            defaultSets = 3,
            defaultReps = if (isTimed) 60 else 12,
            defaultWeightKg = if (cleanEquip.contains("testsúly", ignoreCase = true) || cleanName.contains("futás", ignoreCase = true)) 0f else 20f,
            defaultRestSec = 90,
            isTimed = isTimed,
            targetSeconds = 60,
            stepsHu = parsedSteps,
            stepsEn = parsedSteps,
            stepsDe = parsedSteps,
            stepsRo = parsedSteps,
            coachStatusHu = "Egyedi gyakorlat",
            coachHintHu = cleanDesc,
            videoUrl = cleanVideoUrl,
            isCustom = true
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
                    tr(
                        "Új gyakorlat elmentve a Room adatbázisba és hozzáadva a programhoz: $cleanName",
                        "New exercise saved to Room database and added to program: $cleanName",
                        "Neue Übung in Room-Datenbank gespeichert und zum Programm hinzugefügt: $cleanName",
                        "Exercițiu nou salvat în baza de date Room și adăugat în program: $cleanName",
                        state.language
                    )
                } else {
                    tr(
                        "Új egyedi gyakorlat elmentve a Room adatbázisba: $cleanName",
                        "New custom exercise saved to Room database: $cleanName",
                        "Neue eigene Übung in Room-Datenbank gespeichert: $cleanName",
                        "Exercițiu personalizat nou salvat în baza de date Room: $cleanName",
                        state.language
                    )
                }
            )
        }

        // Persist custom exercise into Room ExerciseLibraryDao
        val db = nativeDatabase
        val scope = nativeScope
        if (db != null && scope != null) {
            val primaryKey = when (resolvedCategory) {
                ExerciseCategory.CHEST -> "chest"
                ExerciseCategory.BACK -> "back"
                ExerciseCategory.SHOULDERS -> "shoulders"
                ExerciseCategory.ARMS -> "arms"
                ExerciseCategory.LEGS -> "legs"
                ExerciseCategory.CORE, ExerciseCategory.ALL -> "core"
            }
            val rawJson = JSONObject().apply {
                put("id", newExercise.id)
                put("nameHu", cleanName)
                put("equipment", cleanEquip)
                put("target", resolvedCategory.labelHu)
                put("videoUrl", cleanVideoUrl)
                put("custom", true)
            }
            val entity = ExerciseLibraryEntity(
                id = newExercise.id,
                nameHu = cleanName,
                nameEn = cleanName,
                nameDe = cleanName,
                nameRo = cleanName,
                loadType = if (newExercise.defaultWeightKg <= 0f) "bodyweight" else "weight",
                repUnit = if (isTimed) "mp" else "ism.",
                measurementType = if (isTimed) "time" else "reps",
                defaultSets = newExercise.defaultSets,
                defaultReps = newExercise.defaultReps.toString(),
                defaultWeight = newExercise.defaultWeightKg.toDouble(),
                defaultRestSeconds = newExercise.defaultRestSec,
                primaryMuscle = primaryKey,
                muscleGroupsJson = JSONArray().put(primaryKey).toString(),
                notesHu = cleanDesc,
                notesEn = cleanDesc,
                notesDe = cleanDesc,
                notesRo = cleanDesc,
                videoUrl = cleanVideoUrl.ifBlank { null },
                videoProvider = if (cleanVideoUrl.isNotBlank()) "custom" else null,
                custom = true,
                setRir229 = true,
                rawJson = rawJson.toString()
            )
            scope.launch(Dispatchers.IO) {
                runCatching { db.exerciseLibraryDao().upsertExercise(entity) }
            }
        }
        if (addToCurrentProgramDay) {
            persistProgramsToRoomAsync()
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

    fun selectCalendarMonth(month: String) {
        val parsed = YearMonth.parse(month)
        _uiState.update { it.copy(calendarYearMonth = parsed.toString(),
            selectedCalendarDay = it.selectedCalendarDay.coerceAtMost(parsed.lengthOfMonth()),
            planningStartDate = parsed.atDay(it.selectedCalendarDay.coerceAtMost(parsed.lengthOfMonth()))
                .format(DateTimeFormatter.ofPattern("yyyy. MM. dd."))) }
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
                !it.dateIso.startsWith(state.calendarYearMonth) || it.status == CalendarEventStatus.COMPLETED || it.dayOfMonth < startDayNum
            }
            val activeProgName = state.programs.firstOrNull { it.isActive }?.nameHu
                ?: "Otthoni A/B – Alap (személyre szabott)"

            var currentSplit = if (state.planningFirstDayHu.contains("B", ignoreCase = true)) "B" else "A"
            val newPlanned = mutableListOf<CalendarDayEvent>()
            var d = startDayNum
            while (d <= YearMonth.parse(state.calendarYearMonth).lengthOfMonth()) {
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
                            dateIso = YearMonth.parse(state.calendarYearMonth).atDay(d).toString(),
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
        persistCalendarStateToRoomAsync()
    }

    fun applyCalendarReplan(newStartDate: String, firstDayHu: String) {
        _uiState.update { state ->
            val startDayNum = newStartDate
                .split(".")
                .mapNotNull { it.trim().toIntOrNull() }
                .lastOrNull()
                ?.coerceIn(1, 31) ?: 28

            val retained = state.calendarEvents.filter {
                !it.dateIso.startsWith(state.calendarYearMonth) || it.status == CalendarEventStatus.COMPLETED
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
            while (d <= YearMonth.parse(state.calendarYearMonth).lengthOfMonth()) {
                replanned.add(
                    CalendarDayEvent(
                        id = "ev_replan_${d}_${System.currentTimeMillis()}",
                        dayOfMonth = d,
                        timeLabel = state.planningTime,
                        titleHu = "$activeProgName • Alap $split",
                        exerciseCount = 6,
                        durationMinutes = state.planningDurationMin,
                        status = CalendarEventStatus.PLANNED,
                        dateIso = YearMonth.parse(state.calendarYearMonth).atDay(d).toString(),
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
        persistCalendarStateToRoomAsync()
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
        persistCalendarStateToRoomAsync()
    }

    fun moveCalendarEvent(eventId: String, targetDay: Int, shiftFollowingPlanned: Boolean = true) {
        _uiState.update { state ->
            val target = state.calendarEvents.firstOrNull { it.id == eventId } ?: return@update state
            val month = YearMonth.from(LocalDate.parse(target.dateIso))
            val clampedDay = targetDay.coerceIn(1, month.lengthOfMonth())
            val delta = clampedDay - target.dayOfMonth
            val updated = state.calendarEvents.mapNotNull { ev ->
                when {
                    ev.id == eventId -> ev.copy(dayOfMonth = clampedDay, dateIso = month.atDay(clampedDay).toString())
                    shiftFollowingPlanned && ev.status == CalendarEventStatus.PLANNED && ev.dateIso > target.dateIso && ev.dateIso.startsWith(month.toString()) -> {
                        val nextD = ev.dayOfMonth + delta
                        if (nextD in 1..month.lengthOfMonth()) ev.copy(dayOfMonth = nextD, dateIso = month.atDay(nextD).toString()) else ev
                    }
                    else -> ev
                }
            }.sortedBy { it.dayOfMonth }

            state.copy(
                calendarUndoStack = (state.calendarUndoStack + listOf(state.calendarEvents)).takeLast(12),
                calendarEvents = updated,
                selectedCalendarDay = clampedDay,
                statusBannerMessage = "Edzés áthelyezve: ${month.atDay(clampedDay)}."
            )
        }
        persistCalendarStateToRoomAsync()
    }

    fun deleteCalendarEvent(eventId: String) {
        val db = nativeDatabase
        val scope = nativeScope
        _uiState.update { state ->
            val updated = state.calendarEvents.filterNot { it.id == eventId }
            state.copy(
                calendarUndoStack = (state.calendarUndoStack + listOf(state.calendarEvents)).takeLast(12),
                calendarEvents = updated,
                statusBannerMessage = "Tervezett edzés törölve (Visszavonható)"
            )
        }
        persistCalendarStateToRoomAsync()
    }

    fun addPlannedWorkoutOnDay(day: Int, splitBadge: String, timeLabel: String, durationMin: Int) {
        _uiState.update { state ->
            val activeProgName = state.programs.firstOrNull { it.isActive }?.nameHu
                ?: "Otthoni A/B – Alap (személyre szabott)"
            val newEv = CalendarDayEvent(
                id = "ev_manual_${day}_${System.currentTimeMillis()}",
                dayOfMonth = day.coerceIn(1, YearMonth.parse(state.calendarYearMonth).lengthOfMonth()),
                timeLabel = timeLabel,
                titleHu = "$activeProgName • Alap $splitBadge",
                exerciseCount = 6,
                durationMinutes = durationMin,
                status = CalendarEventStatus.PLANNED,
                dateIso = YearMonth.parse(state.calendarYearMonth).atDay(day.coerceIn(1, YearMonth.parse(state.calendarYearMonth).lengthOfMonth())).toString(),
                splitBadge = splitBadge
            )
            state.copy(
                calendarUndoStack = (state.calendarUndoStack + listOf(state.calendarEvents)).takeLast(12),
                calendarEvents = (state.calendarEvents + newEv).sortedBy { it.dayOfMonth },
                statusBannerMessage = "Edzés hozzáadva: ${state.calendarYearMonth}-$day. (Alap $splitBadge)"
            )
        }
        persistCalendarStateToRoomAsync()
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
        persistCalendarStateToRoomAsync()
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

    // Active Workout Actions (persisted canonically to ActiveWorkoutStore)
    fun toggleSetCompleted(setNumber: Int) {
        _uiState.update { state ->
            val updated = state.activeSets.map { s ->
                if (s.setNumber == setNumber) s.copy(completed = !s.completed) else s
            }
            val currentExId = state.selectedExerciseId
            val updatedMap = if (currentExId.isNotBlank()) {
                state.activeSetsByExerciseId + (currentExId to updated)
            } else state.activeSetsByExerciseId
            state.copy(
                activeSets = updated,
                activeSetsByExerciseId = updatedMap,
                restTimerRemainingSec = state.defaultRestSec,
                restTimerRunning = true
            )
        }
        persistCanonicalActiveSessionAsync()
    }

    fun adjustSetValues(setNumber: Int, deltaWeight: Float, deltaReps: Int, deltaRir: Int) {
        _uiState.update { state ->
            val updated = state.activeSets.map { s ->
                if (s.setNumber == setNumber) {
                    s.copy(
                        weightKg = (s.weightKg + deltaWeight).coerceAtLeast(0f),
                        reps = (s.reps + deltaReps).coerceIn(1, 50),
                        rir = (s.rir + deltaRir).coerceIn(0, 10),
                        actualRirProvided = s.actualRirProvided || deltaRir != 0
                    )
                } else s
            }
            val currentExId = state.selectedExerciseId
            val updatedMap = if (currentExId.isNotBlank()) {
                state.activeSetsByExerciseId + (currentExId to updated)
            } else state.activeSetsByExerciseId
            state.copy(
                activeSets = updated,
                activeSetsByExerciseId = updatedMap
            )
        }
        persistCanonicalActiveSessionAsync()
    }

    fun updateSetValuesExact(setNumber: Int, weightKg: Float, reps: Int, rir: Int?) {
        _uiState.update { state ->
            val updated = state.activeSets.map { s ->
                if (s.setNumber == setNumber) {
                    s.copy(
                        weightKg = weightKg.coerceAtLeast(0f),
                        reps = reps.coerceIn(1, 100),
                        rir = rir?.coerceIn(0, 10) ?: s.rir, actualRirProvided = rir != null
                    )
                } else s
            }
            val currentExId = state.selectedExerciseId
            val updatedMap = if (currentExId.isNotBlank()) {
                state.activeSetsByExerciseId + (currentExId to updated)
            } else state.activeSetsByExerciseId
            state.copy(
                activeSets = updated,
                activeSetsByExerciseId = updatedMap
            )
        }
        persistCanonicalActiveSessionAsync()
    }

    fun updateBilateralSetExact(setNumber: Int, leftSec: Int, rightSec: Int, rir: Int?) {
        _uiState.update { state ->
            val cleanLeft = leftSec.coerceIn(1, 600)
            val cleanRight = rightSec.coerceIn(1, 600)
            val cleanRir = rir?.coerceIn(0, 10)
            val updated = state.activeSets.map { s ->
                if (s.setNumber == setNumber) {
                    s.copy(
                        leftSec = cleanLeft,
                        rightSec = cleanRight,
                        reps = maxOf(cleanLeft, cleanRight),
                        rir = cleanRir ?: s.rir, actualRirProvided = rir != null,
                        leftRecorded = true,
                        rightRecorded = true
                    )
                } else s
            }
            val currentExId = state.selectedExerciseId
            val updatedMap = if (currentExId.isNotBlank()) {
                state.activeSetsByExerciseId + (currentExId to updated)
            } else state.activeSetsByExerciseId
            state.copy(
                activeSets = updated,
                activeSetsByExerciseId = updatedMap,
                statusBannerMessage = tr(
                    "$setNumber. sorozat frissítve (Bal: ${cleanLeft} mp • Jobb: ${cleanRight} mp)",
                    "Set #$setNumber updated (Left: ${cleanLeft} sec • Right: ${cleanRight} sec)",
                    "Satz #$setNumber aktualisiert (Links: ${cleanLeft} Sek. • Rechts: ${cleanRight} Sek.)",
                    "Seria #$setNumber actualizată (Stânga: ${cleanLeft} sec • Dreapta: ${cleanRight} sec)",
                    state.language
                )
            )
        }
        persistCanonicalActiveSessionAsync()
    }

    fun recordBilateralSideStop(isLeftSide: Boolean, measuredSec: Int, targetSetNumber: Int? = null) {
        _uiState.update { state ->
            val recordedSec = measuredSec.coerceAtLeast(1)
            val sets = state.activeSets
            val targetSet = when {
                targetSetNumber != null && sets.any { it.setNumber == targetSetNumber } ->
                    sets.first { it.setNumber == targetSetNumber }
                isLeftSide ->
                    sets.firstOrNull { !it.leftRecorded && !it.completed }
                        ?: sets.firstOrNull { !it.completed }
                        ?: sets.lastOrNull()
                else ->
                    sets.firstOrNull { !it.rightRecorded && !it.completed }
                        ?: sets.firstOrNull { !it.completed }
                        ?: sets.lastOrNull()
            } ?: return@update state

            var bothSidesNowDone = false
            val updatedSets = sets.map { s ->
                if (s.setNumber == targetSet.setNumber) {
                    val newLeft = if (isLeftSide) recordedSec else s.leftSec
                    val newRight = if (!isLeftSide) recordedSec else s.rightSec
                    val newLeftRec = if (isLeftSide) true else s.leftRecorded
                    val newRightRec = if (!isLeftSide) true else s.rightRecorded
                    val nowCompleted = s.completed || (newLeftRec && newRightRec)
                    if (nowCompleted && !s.completed) {
                        bothSidesNowDone = true
                    }
                    s.copy(
                        leftSec = newLeft,
                        rightSec = newRight,
                        reps = maxOf(newLeft, newRight),
                        leftRecorded = newLeftRec,
                        rightRecorded = newRightRec,
                        completed = nowCompleted
                    )
                } else s
            }

            val currentExId = state.selectedExerciseId
            val updatedMap = if (currentExId.isNotBlank()) {
                state.activeSetsByExerciseId + (currentExId to updatedSets)
            } else state.activeSetsByExerciseId

            state.copy(
                activeSets = updatedSets,
                activeSetsByExerciseId = updatedMap,
                bilateralLeftSec = if (isLeftSide) recordedSec else state.bilateralLeftSec,
                bilateralRightSec = if (!isLeftSide) recordedSec else state.bilateralRightSec,
                restTimerRemainingSec = if (bothSidesNowDone) state.defaultRestSec else state.restTimerRemainingSec,
                restTimerRunning = if (bothSidesNowDone) true else state.restTimerRunning,
                statusBannerMessage = null
            )
        }
        persistCanonicalActiveSessionAsync()
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
            val currentExId = state.selectedExerciseId
            val updatedMap = if (currentExId.isNotBlank()) {
                state.activeSetsByExerciseId + (currentExId to renumbered)
            } else state.activeSetsByExerciseId
            state.copy(
                activeSets = renumbered,
                activeSetsByExerciseId = updatedMap,
                statusBannerMessage = tr(
                    "$setNumber. sorozat törölve",
                    "Set #$setNumber deleted",
                    "Satz #$setNumber gelöscht",
                    "Seria #$setNumber ștearsă",
                    state.language
                )
            )
        }
        persistCanonicalActiveSessionAsync()
    }

    fun addSet() {
        _uiState.update { state ->
            val lastSet = state.activeSets.lastOrNull()
            val nextNumber = (lastSet?.setNumber ?: 0) + 1
            val newSet = ExerciseSetItem(
                setNumber = nextNumber,
                weightKg = lastSet?.weightKg ?: 5.5f,
                reps = lastSet?.reps ?: 20,
                rir = lastSet?.rir ?: 2,
                completed = false,
                leftSec = lastSet?.leftSec ?: 30,
                rightSec = lastSet?.rightSec ?: 30,
                id = UUID.randomUUID().toString(),
                leftRecorded = false,
                rightRecorded = false
            )
            val updatedSets = state.activeSets + newSet
            val currentExId = state.selectedExerciseId
            val updatedMap = if (currentExId.isNotBlank()) {
                state.activeSetsByExerciseId + (currentExId to updatedSets)
            } else state.activeSetsByExerciseId
            state.copy(
                activeSets = updatedSets,
                activeSetsByExerciseId = updatedMap,
                statusBannerMessage = tr(
                    "$nextNumber. sorozat hozzáadva",
                    "Set #$nextNumber added",
                    "Satz #$nextNumber hinzugefügt",
                    "Seria #$nextNumber adăugată",
                    state.language
                )
            )
        }
        persistCanonicalActiveSessionAsync()
    }

    private val plannerNeedsByExerciseId: Map<String, List<String>> by lazy {
        runCatching {
            val stream = javaClass.getResourceAsStream("/native-planner-main-7473.json") ?: return@runCatching emptyMap()
            val root = JSONObject(stream.bufferedReader().use { it.readText() })
            val needsObj = root.optJSONObject("needs") ?: return@runCatching emptyMap()
            buildMap<String, List<String>> {
                val keys = needsObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val arr = needsObj.optJSONArray(k)
                    val list = (0 until (arr?.length() ?: 0)).mapNotNull { idx -> arr?.optString(idx)?.takeIf { it.isNotBlank() } }
                    put(k, list)
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun parseEquipmentSelectionToGearKeys(equipmentHu: String): Pair<Set<String>, Boolean> {
        val tokens = equipmentHu.split(',').map { it.trim().lowercase() }.filter { it.isNotBlank() }
        val gear = linkedSetOf<String>()
        var includeBodyweight = tokens.isEmpty()
        tokens.forEach { t ->
            if (t.contains("saját testsúly") || t == "bodyweight") {
                includeBodyweight = true
            }
            if (t.contains("egykezes") || t.contains("1 db") || t == "dumbbell") {
                gear.add("dumbbell")
            }
            if (t.contains("kézisúlyzó pár") || t.contains("2 db") || t.contains("kézisúlyzó és pad") || t == "dumbbells") {
                gear.add("dumbbell")
                gear.add("dumbbells")
            }
            if (t.contains("kétkezes rúd") || t.contains("súlyzórúd") || t == "barbell") {
                gear.add("barbell")
            }
            if (t.contains("pad") || t.contains("szék") || t.contains("kanapé") || t == "bench") {
                gear.add("bench")
                gear.add("support")
            }
            if (t.contains("stabil") || t.contains("támasz") || t.contains("fal") || t == "support") {
                gear.add("support")
                gear.add("bench")
            }
            if (t.contains("fellépő") || t.contains("doboz") || t.contains("lépcső") || t == "step") {
                gear.add("step")
            }
            if (t.contains("húzódzkodó") || t == "pullupbar") {
                gear.add("pullupbar")
            }
            if (t.contains("tolódzkodó") || t == "dipbars") {
                gear.add("dipbars")
            }
            if (t.contains("gumiszalag") || t.contains("ellenállás") || t == "bands") {
                gear.add("bands")
            }
            if (t.contains("kettlebell")) {
                gear.add("kettlebell")
            }
            if (t.contains("edzőtermi") || t.contains("csigás") || t.contains("gépek") || t == "machines") {
                gear.add("machines")
            }
            if (t.contains("teljes edzőterem")) {
                includeBodyweight = true
                gear.addAll(listOf("dumbbell", "dumbbells", "barbell", "bench", "support", "step", "pullupbar", "dipbars", "bands", "kettlebell", "machines"))
            }
            if (t.contains("otthoni alap")) {
                includeBodyweight = true
                gear.addAll(listOf("dumbbell", "dumbbells", "bands", "pullupbar", "support", "bench"))
            }
        }
        return gear to includeBodyweight
    }

    private fun exerciseMatchesSelectedGear(ex: ExerciseItem, gear: Set<String>, includeBodyweight: Boolean): Boolean {
        val canonicalNeeds = plannerNeedsByExerciseId[ex.id]
        if (canonicalNeeds != null) {
            if (canonicalNeeds.isEmpty()) {
                return includeBodyweight || gear.isEmpty()
            }
            return canonicalNeeds.all { req ->
                req in gear || (req == "dumbbell" && "dumbbells" in gear) || (req == "bench" && "support" in gear) || (req == "support" && "bench" in gear)
            }
        }
        val eqHu = ex.equipmentHu.lowercase()
        if (eqHu.contains("saját testsúly")) return includeBodyweight || gear.isEmpty()
        if (eqHu.contains("rúd")) return "barbell" in gear
        if (eqHu.contains("kézisúlyzó")) return "dumbbell" in gear || "dumbbells" in gear
        if (eqHu.contains("kettlebell")) return "kettlebell" in gear
        if (eqHu.contains("gumiszalag")) return "bands" in gear
        if (eqHu.contains("csigás") || eqHu.contains("gép")) return "machines" in gear
        return true
    }

    private fun exerciseEquipmentPriorityScore(ex: ExerciseItem, gear: Set<String>, includeBodyweight: Boolean): Int {
        val canonicalNeeds = plannerNeedsByExerciseId[ex.id] ?: emptyList()
        var score = 0
        if (canonicalNeeds.isNotEmpty() && canonicalNeeds.all { it in gear || (it == "dumbbell" && "dumbbells" in gear) }) {
            score += 4 + canonicalNeeds.size * 2
        } else if (canonicalNeeds.isEmpty() && includeBodyweight) {
            score += 3
        }
        if ("bench" in canonicalNeeds || "support" in canonicalNeeds) score += 2
        if ("barbell" in canonicalNeeds && "barbell" in gear) score += 3
        if (("dumbbells" in canonicalNeeds || "dumbbell" in canonicalNeeds) && ("dumbbells" in gear || "dumbbell" in gear)) score += 2
        return score
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
        val (selectedGear, includeBodyweight) = parseEquipmentSelectionToGearKeys(equipmentHu)
        val perDayCapacity = com.repforge.app.NativePlannerPolicy.capacity(durationMin).coerceIn(4, 7)
        _uiState.update { state ->
            val generatedProgramId = "custom_plan_" + System.currentTimeMillis()
            val baseFiltered = state.exercises.filter { ex ->
                ex.nameHu !in excludedExercises &&
                ex.id !in excludedExercises &&
                (excludedMuscles.isEmpty() || !excludedMuscles.any { ex.primaryMusclesHu.contains(it, ignoreCase = true) })
            }
            val equipmentMatched = baseFiltered.filter { ex ->
                exerciseMatchesSelectedGear(ex, selectedGear, includeBodyweight)
            }.sortedByDescending { ex ->
                exerciseEquipmentPriorityScore(ex, selectedGear, includeBodyweight)
            }.ifEmpty { baseFiltered }

            val dayAExercises = mutableListOf<ExerciseItem>()
            val preferredCategoriesA = listOf(
                ExerciseCategory.LEGS,
                ExerciseCategory.CHEST,
                ExerciseCategory.BACK,
                ExerciseCategory.SHOULDERS,
                ExerciseCategory.ARMS,
                ExerciseCategory.CORE
            )
            for (cat in preferredCategoriesA) {
                if (dayAExercises.size >= perDayCapacity) break
                val candidate = equipmentMatched.firstOrNull { it.category == cat && it !in dayAExercises }
                if (candidate != null) dayAExercises.add(candidate)
            }
            for (ex in equipmentMatched) {
                if (dayAExercises.size >= perDayCapacity) break
                if (ex !in dayAExercises) dayAExercises.add(ex)
            }
            val finalDayA = dayAExercises.ifEmpty { state.exercises.take(perDayCapacity) }

            val remainingForB = equipmentMatched.filter { it !in finalDayA }
            val dayBExercises = mutableListOf<ExerciseItem>()
            for (cat in preferredCategoriesA) {
                if (dayBExercises.size >= perDayCapacity) break
                val candidate = remainingForB.firstOrNull { it.category == cat && it !in dayBExercises }
                if (candidate != null) dayBExercises.add(candidate)
            }
            for (ex in remainingForB) {
                if (dayBExercises.size >= perDayCapacity) break
                if (ex !in dayBExercises) dayBExercises.add(ex)
            }
            if (dayBExercises.size < perDayCapacity) {
                for (ex in equipmentMatched) {
                    if (dayBExercises.size >= perDayCapacity) break
                    if (ex !in dayBExercises) dayBExercises.add(ex)
                }
            }
            val finalDayB = dayBExercises.ifEmpty { state.exercises.drop(perDayCapacity).take(perDayCapacity).ifEmpty { finalDayA } }

            val gearSummaryTokens = equipmentHu.split(',').map { it.trim() }.filter { it.isNotBlank() }
            val shortGearTag = when {
                gearSummaryTokens.size > 2 -> "${gearSummaryTokens.take(2).joinToString(" + ")} (+${gearSummaryTokens.size - 2})"
                gearSummaryTokens.isNotEmpty() -> gearSummaryTokens.joinToString(" + ")
                else -> locationHu
            }

            val goalEn = com.example.localization.StudioProfileOptionLabels.goal(goalHu, AppLanguage.EN)
            val goalDe = com.example.localization.StudioProfileOptionLabels.goal(goalHu, AppLanguage.DE)
            val goalRo = com.example.localization.StudioProfileOptionLabels.goal(goalHu, AppLanguage.RO)
            val locEn = com.example.localization.StudioProfileOptionLabels.location(locationHu, AppLanguage.EN)
            val locDe = com.example.localization.StudioProfileOptionLabels.location(locationHu, AppLanguage.DE)
            val locRo = com.example.localization.StudioProfileOptionLabels.location(locationHu, AppLanguage.RO)
            val expEn = com.example.localization.StudioProfileOptionLabels.experience(experienceHu, AppLanguage.EN)
            val expDe = com.example.localization.StudioProfileOptionLabels.experience(experienceHu, AppLanguage.DE)
            val expRo = com.example.localization.StudioProfileOptionLabels.experience(experienceHu, AppLanguage.RO)

            val newProgram = TrainingProgramModel(
                id = generatedProgramId,
                nameHu = "Egyéni $goalHu Terv ($locationHu)",
                nameEn = "Personalized $goalEn Plan ($locEn)",
                subtitleHu = "$experienceHu • Heti $daysPerWeek nap • $durationMin perc • $shortGearTag",
                subtitleEn = "$expEn • $daysPerWeek days/week • $durationMin min",
                tagsHu = listOf(goalHu, locationHu, shortGearTag, "Egyéni").distinct(),
                isCustom = true,
                isActive = true,
                days = listOf(
                    com.example.model.ProgramDayModel(
                        id = "${generatedProgramId}_dayA",
                        dayCode = "A nap",
                        titleHu = "A nap • Fő fókusz",
                        titleEn = "Day A • Primary Focus",
                        durationMinutes = durationMin,
                        exercises = finalDayA,
                        titleDe = "Tag A • Hauptfokus",
                        titleRo = "Ziua A • Focus principal"
                    ),
                    com.example.model.ProgramDayModel(
                        id = "${generatedProgramId}_dayB",
                        dayCode = "B nap",
                        titleHu = "B nap • Kiegészítő fókusz",
                        titleEn = "Day B • Secondary Focus",
                        durationMinutes = durationMin,
                        exercises = finalDayB,
                        titleDe = "Tag B • Zusatzfokus",
                        titleRo = "Ziua B • Focus secundar"
                    )
                ),
                nameDe = "Individueller $goalDe-Plan ($locDe)",
                nameRo = "Plan personalizat $goalRo ($locRo)",
                subtitleDe = "$expDe • $daysPerWeek Tage/Woche • $durationMin Min.",
                subtitleRo = "$expRo • $daysPerWeek zile/săpt. • $durationMin min",
                tagsEn = listOf(goalEn, locEn, "Custom").distinct(),
                tagsDe = listOf(goalDe, locDe, "Individuell").distinct(),
                tagsRo = listOf(goalRo, locRo, "Personalizat").distinct()
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
                statusBannerMessage = tr(
                    "Profil és terv mentése…",
                    "Saving profile and plan…",
                    "Profil und Plan werden gespeichert…",
                    "Se salvează profilul și planul…",
                    state.language
                )
            )
        }
        persistProgramsToRoomAsync(JSONObject()
            .put("height", heightCm).put("weight", weightKg.toDouble())
            .put("goal", goalHu).put("experience", experienceHu).put("location", locationHu)
            .put("gear", JSONArray(equipmentHu.split(',').map(String::trim).filter(String::isNotBlank)))
            .put("daysPerWeek", daysPerWeek).put("minutes", durationMin)
            .put("avoidAreas", JSONArray(excludedMuscles.toList()))
            .put("excluded", JSONArray(excludedExercises.toList())))
    }

    fun undoLastCompletedSet() {
        _uiState.update { state ->
            val lastCompleted = state.activeSets.lastOrNull { it.completed }
            if (lastCompleted == null) state
            else {
                val updated = state.activeSets.map {
                    if (it.setNumber == lastCompleted.setNumber) it.copy(completed = false) else it
                }
                val currentExId = state.selectedExerciseId
                val updatedMap = if (currentExId.isNotBlank()) {
                    state.activeSetsByExerciseId + (currentExId to updated)
                } else state.activeSetsByExerciseId
                state.copy(
                    activeSets = updated,
                    activeSetsByExerciseId = updatedMap,
                    statusBannerMessage = tr(
                        "${lastCompleted.setNumber}. sorozat visszavonva",
                        "Set #${lastCompleted.setNumber} undone",
                        "Satz #${lastCompleted.setNumber} rückgängig gemacht",
                        "Seria #${lastCompleted.setNumber} anulată",
                        state.language
                    )
                )
            }
        }
        persistCanonicalActiveSessionAsync()
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
                statusBannerMessage = tr(
                    "Intervallumok: ${workSec}mp munka / ${restSec}mp pihenő × $rounds",
                    "Intervals: ${workSec}s work / ${restSec}s rest × $rounds",
                    "Intervalle: ${workSec}s Arbeit / ${restSec}s Pause × $rounds",
                    "Intervale: ${workSec}s lucru / ${restSec}s pauză × $rounds",
                    it.language
                )
            )
        }
    }

    private var summaryFeedbackTicket: NativeDataReplacement.Continuation? = null
    private var summaryFeedbackOwnerId = ""

    private fun admitSummaryFeedback(): Boolean {
        val context = nativeContext ?: return false
        val id = _uiState.value.activeWorkoutId
        if (id.isBlank() || !_uiState.value.isWorkoutSessionActive || _uiState.value.isSavingWorkout) return false
        val same = summaryFeedbackOwnerId == id &&
            runCatching { requireNotNull(summaryFeedbackTicket).requireCurrent(context) }.isSuccess
        if (!same) {
            summaryFeedbackTicket = runCatching { NativeDataReplacement.continuationForCurrentData(context) }.getOrNull() ?: return false
            summaryFeedbackOwnerId = id
            _uiState.update { it.copy(summaryFeedbackWorkoutId = id, workoutSummaryMoodIndex = -1, workoutSummaryNote = "") }
        }
        return true
    }

    fun selectWorkoutSummaryMood(index: Int) {
        if (index !in -1..5 || !admitSummaryFeedback()) return
        _uiState.update { state ->
            if (!state.isWorkoutSessionActive || state.activeWorkoutId.isBlank()) state
            else state.copy(summaryFeedbackWorkoutId = state.activeWorkoutId, workoutSummaryMoodIndex = index,
                workoutSummaryNote = if (state.summaryFeedbackWorkoutId == state.activeWorkoutId) state.workoutSummaryNote else "")
        }
    }

    fun updateWorkoutSummaryNote(note: String) {
        if (!admitSummaryFeedback()) return
        _uiState.update { state ->
            if (!state.isWorkoutSessionActive || state.activeWorkoutId.isBlank()) state
            else state.copy(summaryFeedbackWorkoutId = state.activeWorkoutId, workoutSummaryNote = note.take(3000),
                workoutSummaryMoodIndex = if (state.summaryFeedbackWorkoutId == state.activeWorkoutId) state.workoutSummaryMoodIndex else -1)
        }
    }

    private var exerciseFeedbackTicket: NativeDataReplacement.Continuation? = null
    private var exerciseFeedbackOwnerId = ""

    fun updateExerciseFeedback(id: String, index: Int? = null, note: String? = null) {
        val context = nativeContext ?: return
        val stateBefore = _uiState.value
        val ticketValid = exerciseFeedbackOwnerId == stateBefore.activeWorkoutId &&
            runCatching { requireNotNull(exerciseFeedbackTicket).requireCurrent(context) }.isSuccess
        if (!ticketValid) {
            exerciseFeedbackTicket = runCatching { NativeDataReplacement.continuationForCurrentData(context) }.getOrNull() ?: return
            exerciseFeedbackOwnerId = stateBefore.activeWorkoutId
        }
        _uiState.update { state ->
            if (state.isSavingWorkout || !state.isWorkoutSessionActive || state.activeWorkoutId.isBlank() ||
                state.activeWorkoutExercises.none { it.id == id } || (index != null && index !in -1..5)) state
            else {
                val same = ticketValid && state.exerciseFeedbackWorkoutId == state.activeWorkoutId
                state.copy(exerciseFeedbackWorkoutId = state.activeWorkoutId,
                    exerciseFeedbackChoices = (if (same) state.exerciseFeedbackChoices else emptyMap()).let {
                        if (index != null) it + (id to index) else it
                    },
                    exerciseFeedbackNotes = (if (same) state.exerciseFeedbackNotes else emptyMap()).let {
                        if (note != null) it + (id to note.take(3000)) else it
                    })
            }
        }
    }

    private var nativeStartJob: kotlinx.coroutines.Job? = null

    fun startPlannedWorkout(programId: String? = null, dayIndex: Int = 0) {
        val state = _uiState.value
        val program = state.programs.firstOrNull { it.id == programId }
            ?: state.programs.firstOrNull { it.isActive }
        val day = program?.days?.getOrNull(dayIndex)
        if (program == null || day == null) {
            _uiState.update { it.copy(statusBannerMessage = "Az edzésprogram vagy nap nem található.") }
            return
        }
        startCanonicalWorkout {
            val db = requireNotNull(nativeDatabase)
            val saved = withContext(Dispatchers.IO) { db.programDao().getProgramWithDaysById(program.id) }
            val nativeDay = saved?.days?.singleOrNull { it.compositeId == day.id || it.dayId == day.id }
                ?: error("Az edzésnap megváltozott. Nyisd meg újra a programot.")
            com.repforge.app.NativeWorkoutSession.startProgramWorkout(requireNotNull(nativeContext),
                program.id, nativeDay.dayId, database = db, automaticEnabled = state.autoModeEnabled).snapshot
        }
    }

    fun startCoachWorkout(programId: String, dayId: String, scheduleId: String?) {
        startCanonicalWorkout {
            com.repforge.app.NativeWorkoutSession.startProgramWorkout(requireNotNull(nativeContext),
                programId, dayId, scheduleId, database = requireNotNull(nativeDatabase),
                automaticEnabled = _uiState.value.autoModeEnabled).snapshot
        }
    }

    fun startWorkoutWithExercises(exercisesToRun: List<ExerciseItem>, titleHu: String = "Gyors edzés") {
        if (exercisesToRun.isEmpty()) return
        startCanonicalWorkout {
            com.repforge.app.NativeWorkoutSession.startQuickExercises(requireNotNull(nativeContext),
                exercisesToRun.map { it.id }, titleHu, _uiState.value.autoModeEnabled,
                requireNotNull(nativeDatabase))
        }
    }

    private fun startCanonicalWorkout(start: suspend () -> JSONObject) {
        if (nativeStartJob?.isActive == true || _uiState.value.isSavingWorkout) return
        if (_uiState.value.isWorkoutSessionActive && _uiState.value.workoutStartedEpochMs > 0) {
            returnToActiveWorkout()
            return
        }
        val scope = nativeScope ?: return
        nativeStartJob = scope.launch(Dispatchers.Main.immediate) {
            try {
                programWriteJob?.join()
                check(programWriteError == null) { requireNotNull(programWriteError) }
                val frame = start() // Canonical engine commits before the UI advertises a workout.
                presentCanonicalWorkout(frame)
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (error: Exception) {
                _uiState.update { it.copy(statusBannerMessage = error.message ?: "Az edzés nem indítható.") }
            }
        }
    }

    private suspend fun presentCanonicalWorkout(requested: JSONObject) {
        val library = withContext(Dispatchers.IO) {
            requireNotNull(nativeDatabase).exerciseLibraryDao().getAllExercises().associateBy { it.id }
        }
        val frame = requireNotNull(ActiveWorkoutStore.current(requireNotNull(nativeContext)))
        check(frame.optBoolean("active") && frame.optString("workoutId") == requested.optString("workoutId") &&
            frame.optLong("generation") == requested.optLong("generation")) { "Az edzés időközben megváltozott." }
        val ids = frame.getJSONArray("exercises")
        val exercises = (0 until ids.length()).map { index ->
            val raw = ids.getJSONObject(index)
            com.example.ui.components.withNativeStrengthUnits(
                mapExerciseEntity(requireNotNull(library[raw.getString("id")])),
                raw.opt("loadType") as? String, raw.opt("repUnit") as? String).copy(
                    measurementType = raw.optString("measurementType"),
                    isTimed = com.repforge.app.NativeSetStopwatch.timed(raw),
                    isBilateral = com.repforge.app.NativeSetStopwatch.bilateral(raw))
        }
        val sets = (0 until ids.length()).associate { index ->
            val raw = ids.getJSONObject(index)
            val rows = raw.getJSONArray("sets")
            raw.getString("id") to (0 until rows.length()).map { rowIndex ->
                val row = rows.getJSONObject(rowIndex)
                ExerciseSetItem(rowIndex + 1, row.optDouble("weight", 0.0).toFloat(),
                    row.optString("reps").toIntOrNull() ?: 0, row.optInt("actualRir", 2), row.optBoolean("done"),
                    leftSec = row.optInt("leftSeconds", row.optInt("leftSec", exercises[index].targetSeconds)),
                    rightSec = row.optInt("rightSeconds", row.optInt("rightSec", exercises[index].targetSeconds)),
                    leftRecorded = row.optInt("leftSeconds") > 0 || row.optBoolean("leftRecorded"),
                    rightRecorded = row.optInt("rightSeconds") > 0 || row.optBoolean("rightRecorded"),
                    id = row.getString("setId"), actualRirProvided = row.has("actualRir") && !row.isNull("actualRir"))
            }
        }
        val currentIndex = frame.optInt("currentExercise").coerceIn(exercises.indices)
        val first = exercises[currentIndex]
        val target = targetSubScreenForExercise(first)
        _uiState.update { state -> state.copy(
            isWorkoutSessionActive = true, activeWorkoutId = frame.getString("workoutId"), activeWorkoutGeneration = frame.getLong("generation"),
            workoutStartedEpochMs = Instant.parse(frame.getString("started")).toEpochMilli(),
            activeWorkoutSubScreen = target, activeWorkoutTitleHu = frame.optString("programName"),
            activeWorkoutExercises = exercises, activeExerciseIndex = currentIndex, selectedExerciseId = first.id,
            activeSets = sets.getValue(first.id), activeSetsByExerciseId = sets,
            phoneControlsWorkout = frame.optString("timerOwner", "phone") == "phone" &&
                !com.repforge.app.NativePhoneReturnInstallation.requiresInstallation(frame) &&
                !com.repforge.app.NativeWatchHandoffReceipt.pending(frame, watchJournal()),
            watchControlPending = watchHandoffBusy || com.repforge.app.NativeWatchHandoffReceipt.pending(frame, watchJournal()),
            currentSubScreen = target, showQuickWorkoutSheet = false, timedCurrentSec = 0,
            backStack = (state.backStack + NavRoute(state.currentTab, state.currentSubScreen)).takeLast(16),
            statusBannerMessage = null) }
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
        startWorkoutWithExercises(listOf(exercise), "Gyors időalapú edzés")
    }

    fun startOutdoorWorkout(titleHu: String = "Kültéri futás") {
        val currentSub = _uiState.value.activeWorkoutSubScreen
        if (_uiState.value.isWorkoutSessionActive && currentSub != SubScreen.ACTIVE_OUTDOOR_GPS) {
            _uiState.update { it.copy(statusBannerMessage = "Előbb fejezd be vagy vesd el az aktív edzést.") }
            return
        }

        val newWorkoutId = UUID.randomUUID().toString()
        val outdoorEx = ExerciseItem(
            id = "outdoor_run_ex",
            nameHu = titleHu,
            category = ExerciseCategory.CORE,
            primaryMusclesHu = "Kardió, Láb",
            equipmentHu = "GPS / Szabadban",
            isTimed = true
        )
        val defaultSets = listOf(
            ExerciseSetItem(setNumber = 1, weightKg = 0f, reps = 0, rir = 0, completed = false)
        )
        _uiState.update { state ->
            val back = (state.backStack + NavRoute(state.currentTab, state.currentSubScreen)).takeLast(16)
            state.copy(
                isWorkoutSessionActive = false,
                activeWorkoutId = newWorkoutId,
                workoutStartedEpochMs = 0L,
                activeWorkoutSubScreen = SubScreen.ACTIVE_OUTDOOR_GPS,
                activeWorkoutTitleHu = titleHu,
                activeWorkoutExercises = listOf(outdoorEx),
                activeExerciseIndex = 0,
                selectedExerciseId = outdoorEx.id,
                activeSets = defaultSets,
                activeSetsByExerciseId = mapOf(outdoorEx.id to defaultSets),
                outdoorDistanceKm = 0f,
                outdoorElapsedLabel = "00:00",
                outdoorPaceLabel = "0:00",
                currentSubScreen = SubScreen.ACTIVE_OUTDOOR_GPS,
                currentTab = MainTab.WORKOUT,
                backStack = back,
                showQuickWorkoutSheet = false
            )
        }
    }

    fun startGpsTrackingSession(activityName: String = "Kültéri futás", routeKey: String = ""): Boolean {
        val ctx = nativeContext ?: return false
        val previous = ActiveWorkoutStore.current(ctx)
        if (previous?.optBoolean("active") == true && previous.optString("workoutId") != _uiState.value.activeWorkoutId) {
            _uiState.update { it.copy(statusBannerMessage = "Előbb fejezd be a már aktív edzést.") }
            return false
        }
        if (previous?.optBoolean("active") == true) {
            _uiState.update { it.copy(outdoorRouteKey = routeKey, outdoorGpsEnabled = routeKey.isNotBlank()) }
            persistCanonicalActiveSessionAsync()
            return true
        }
        val nowMs = System.currentTimeMillis()
        val state = _uiState.value
        val nextState = run {
            val currentTitle = activityName.ifBlank { state.activeWorkoutTitleHu.ifBlank { "Kültéri futás" } }
            val outdoorEx = ExerciseItem(
                id = "outdoor_run_ex",
                nameHu = currentTitle,
                category = ExerciseCategory.CORE,
                primaryMusclesHu = "Kardió, Láb",
                equipmentHu = "GPS / Szabadban",
                isTimed = true
            )
            val defaultSets = listOf(
                ExerciseSetItem(setNumber = 1, weightKg = 0f, reps = 0, rir = 0, completed = false)
            )
            state.copy(
                isWorkoutSessionActive = true,
                outdoorRouteKey = routeKey,
                outdoorGpsEnabled = routeKey.isNotBlank(),
                activeWorkoutId = UUID.randomUUID().toString(),
                workoutStartedEpochMs = nowMs,
                activeWorkoutSubScreen = SubScreen.ACTIVE_OUTDOOR_GPS,
                activeWorkoutTitleHu = currentTitle,
                activeWorkoutExercises = listOf(outdoorEx),
                activeExerciseIndex = 0,
                selectedExerciseId = outdoorEx.id,
                activeSets = defaultSets,
                activeSetsByExerciseId = mapOf(outdoorEx.id to defaultSets),
                statusBannerMessage = if (routeKey.isBlank()) "Beltéri natív munkamenet előkészítve." else "Natív edzés előkészítve; GPS-visszaigazolásra vár."
            )
        }
        return try {
            val saved = com.repforge.app.StudioWorkoutBridge.persist(ctx, buildCanonicalSessionJsonFromState(nextState))
            check(saved.optString("workoutId") == nextState.activeWorkoutId) { "Másik natív edzés aktív. Nyisd meg a futó edzést." }
            _uiState.value = nextState
            true
        } catch (error: Exception) {
            _uiState.update { it.copy(statusBannerMessage = error.message ?: "A GPS-edzés nem indítható.") }
            false
        }
    }

    fun pauseOutdoorMeasurement(paused: Boolean): Boolean {
        val ctx = nativeContext ?: return false
        return try { com.repforge.app.StudioWorkoutBridge.pause(ctx, paused); true }
        catch (error: Exception) { _uiState.update { it.copy(statusBannerMessage = error.message) }; false }
    }

    fun selectOutdoorActivityType(activityName: String) {
        val outdoorEx = ExerciseItem(
            id = "outdoor_run_ex",
            nameHu = activityName,
            category = ExerciseCategory.CORE,
            primaryMusclesHu = "Kardió, Láb",
            equipmentHu = "GPS / Szabadban",
            isTimed = true
        )
        _uiState.update { state ->
            val updatedExs = state.activeWorkoutExercises.map { ex ->
                if (ex.id == "outdoor_run_ex") ex.copy(nameHu = activityName) else ex
            }
            state.copy(
                activeWorkoutTitleHu = activityName,
                activeWorkoutExercises = if (updatedExs.isNotEmpty()) updatedExs else listOf(outdoorEx),
                activeWorkoutSubScreen = SubScreen.ACTIVE_OUTDOOR_GPS
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

    private val activeMeasurementScreens = setOf(SubScreen.ACTIVE_STRENGTH, SubScreen.ACTIVE_TIMED,
        SubScreen.ACTIVE_BILATERAL, SubScreen.ACTIVE_OUTDOOR_GPS)
    private data class ExerciseSelection(val target: com.repforge.app.NativeWorkoutTarget,
        val data: com.repforge.app.NativeDataReplacement.Continuation)
    private var exerciseSelection: ExerciseSelection? = null
    private var exerciseSelectionJob: Job? = null

    private fun currentExerciseSelection(): ExerciseSelection {
        val ctx = requireNotNull(nativeContext)
        val frame = com.repforge.app.NativeWorkoutEditing.project(ctx)
        check(frame.optString("workoutId") == _uiState.value.activeWorkoutId) { "Az edzés időközben megváltozott." }
        val exercise = frame.getJSONArray("exercises").getJSONObject(frame.getInt("currentExercise"))
        return ExerciseSelection(com.repforge.app.NativeWorkoutTarget(frame.getString("workoutId"), frame.getLong("generation"),
            exercise.getString("id"), exercise.getJSONArray("sets").getJSONObject(0).getString("setId")),
            com.repforge.app.NativeDataReplacement.continuationForCurrentData(ctx))
    }

    fun openActiveExerciseLibrary() {
        try {
            exerciseSelection = currentExerciseSelection()
            navigateToSubScreen(SubScreen.EXERCISE_LIBRARY)
            if (_uiState.value.currentSubScreen != SubScreen.EXERCISE_LIBRARY) exerciseSelection = null
        } catch (error: Exception) { _uiState.update { it.copy(statusBannerMessage = error.message) } }
    }

    fun selectLibraryExercise(exercise: ExerciseItem) {
        val selection = exerciseSelection
        if (selection != null) {
            exerciseSelection = null
            switchCurrentActiveExercise(exercise, selection, resume = true)
        } else if (exercise.isTimed || exercise.isBilateral) startQuickTimedWithExercise(exercise)
        else startQuickStrengthWithExercise(exercise)
    }

    fun switchCurrentActiveExercise(exercise: ExerciseItem) {
        try { switchCurrentActiveExercise(exercise, currentExerciseSelection()) }
        catch (error: Exception) { _uiState.update { it.copy(statusBannerMessage = error.message) } }
    }

    private fun switchCurrentActiveExercise(exercise: ExerciseItem, selection: ExerciseSelection, resume: Boolean = false) {
        if (exerciseSelectionJob?.isActive == true) return
        val ctx = nativeContext ?: return
        val db = nativeDatabase ?: return
        val scope = nativeScope ?: return
        exerciseSelectionJob = scope.launch(Dispatchers.Main.immediate) {
            try {
                selection.data.requireCurrent(ctx)
                val replacement = withContext(Dispatchers.IO) {
                    val source = requireNotNull(db.exerciseLibraryDao().getExerciseById(exercise.id)) { "A gyakorlat már nem található." }
                    com.repforge.app.NativeWorkoutSession.buildExercise(db, source, requireNotNull(nativeSettingsStore).current())
                }
                selection.data.requireCurrent(ctx)
                var saved = com.repforge.app.StudioWorkoutBridge.switchExercise(ctx, selection.target, replacement)
                // The library's explicit Start action resumes this exact edited session.
                if (resume && saved.optBoolean("workoutPaused")) saved = com.repforge.app.StudioWorkoutBridge.pause(ctx, false)
                presentCanonicalWorkout(saved)
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (error: Exception) { _uiState.update { it.copy(statusBannerMessage = error.message ?: "A gyakorlat nem cserélhető.") } }
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
            val targetSub = targetSubScreenForExercise(nextEx)
            _uiState.update {
                it.copy(
                    activeWorkoutExercises = workoutExs,
                    activeExerciseIndex = nextIdx,
                    selectedExerciseId = nextEx.id,
                    activeSets = nextSets,
                    activeSetsByExerciseId = updatedSetsMap,
                    timedCurrentSec = 0,
                    currentSubScreen = targetSub,
                    activeWorkoutSubScreen = targetSub,
                    statusBannerMessage = null
                )
            }
            persistCanonicalActiveSessionAsync()
        } else {
            _uiState.update {
                it.copy(
                    activeSetsByExerciseId = updatedSetsMap,
                    currentSubScreen = SubScreen.WORKOUT_SUMMARY,
                    statusBannerMessage = "Minden gyakorlat áttekintve (${workoutExs.size}/${workoutExs.size})! Összegzés."
                )
            }
            persistCanonicalActiveSessionAsync()
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
            val targetSub = targetSubScreenForExercise(prevEx)
            _uiState.update {
                it.copy(
                    activeExerciseIndex = prevIdx,
                    selectedExerciseId = prevEx.id,
                    activeSets = prevSets,
                    activeSetsByExerciseId = updatedSetsMap,
                    currentSubScreen = targetSub,
                    activeWorkoutSubScreen = targetSub,
                    statusBannerMessage = null
                )
            }
            persistCanonicalActiveSessionAsync()
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
        if (state.isSavingWorkout) return
        val ctx = nativeContext
        val db = nativeDatabase
        val scope = nativeScope
        if (ctx == null || db == null || scope == null) {
            _uiState.update { it.copy(statusBannerMessage = "A natív edzésmotor nem érhető el.") }
            return
        }
        _uiState.update { it.copy(isSavingWorkout = true) }
        scope.launch(Dispatchers.IO) {
            try {
                val activeToFinalize = withContext(Dispatchers.Main.immediate) {
                    // Never fabricate a GPS set, a start timestamp or a replacement workout.
                    if (com.repforge.app.NativePhoneReturnInstallation.requiresInstallation(ActiveWorkoutStore.current(ctx)))
                        com.repforge.app.NativePhoneReturnInstallation.complete(ctx)
                    val current = com.repforge.app.NativeWorkoutEditing.project(ctx)
                    check(current.optBoolean("active") && current.optString("workoutId") == state.activeWorkoutId)
                    val exercises = current.getJSONArray("exercises")
                    val completed = (0 until exercises.length()).sumOf { index ->
                        val sets = exercises.getJSONObject(index).getJSONArray("sets")
                        (0 until sets.length()).count { sets.getJSONObject(it).optBoolean("done") }
                    }
                    check(completed > 0 || com.repforge.app.StudioOutdoorFinish.canSaveTimeOnly(current)) {
                        "Nincs hitelesen mért idő vagy rögzített sorozat. A mérés elvethető, de üres edzés nem menthető."
                    }
                    if (state.summaryFeedbackWorkoutId == state.activeWorkoutId) {
                        requireNotNull(summaryFeedbackTicket).requireCurrent(ctx)
                        com.repforge.app.StudioWorkoutFeedback.chosen(state.workoutSummaryMoodIndex,
                            state.workoutSummaryNote, System.currentTimeMillis())?.let { current.put("feedback", it) }
                    }
                    if (state.exerciseFeedbackWorkoutId == state.activeWorkoutId &&
                        (state.exerciseFeedbackChoices.isNotEmpty() || state.exerciseFeedbackNotes.isNotEmpty())) {
                        requireNotNull(exerciseFeedbackTicket).requireCurrent(ctx)
                        com.repforge.app.StudioWorkoutFeedback.attachExerciseEdits(current, state.exerciseFeedbackWorkoutId,
                            state.exerciseFeedbackChoices, state.exerciseFeedbackNotes)
                    }
                    current
                }
                val workoutId = activeToFinalize.getString("workoutId")
                val workoutTitle = activeToFinalize.optString("programName", "Edzés")
                val isOutdoorSession = state.activeWorkoutSubScreen == SubScreen.ACTIVE_OUTDOOR_GPS
                val exercises = activeToFinalize.getJSONArray("exercises")
                val totalCompletedSets = (0 until exercises.length()).sumOf { i ->
                    val sets = exercises.getJSONObject(i).getJSONArray("sets")
                    (0 until sets.length()).count { sets.getJSONObject(it).optBoolean("done") }
                }
                val saved = TrainPilotWorkoutFinalization(ctx, db).finishWorkout(activeToFinalize)

                // Verify persistence by querying Room directly and build LoggedWorkoutEntry from Room truth
                val finalWorkoutId = saved.history.optString("workoutId", workoutId)
                val savedInDb = db.workoutHistoryDao().getWorkoutWithDetails(finalWorkoutId)
                if (savedInDb != null) {
                    saved.closedSnapshot?.let { runCatching { WearSyncRepository.publishActiveWorkoutSnapshot(ctx, it) } }
                    runCatching { WearSyncRepository.refreshWatchHomeAsync(ctx) }
                    runCatching { WearSyncRepository.publishWatchHomeSnapshot(ctx) }

                    val dbW = savedInDb.workout
                    val realElapsedMs = dbW.durationSeconds.coerceAtLeast(0).toLong() * 1000L
                    val realElapsedSec = (realElapsedMs / 1000L).toInt()
                    val realDurationMins = ((realElapsedMs + 59_999L) / 60_000L).toInt().coerceAtLeast(1)
                    val realWorkTimeLabel = String.format("%02d:%02d", realElapsedSec / 60, realElapsedSec % 60)

                    val dbDetails = savedInDb.exercises.map { exWithSets ->
                        val ex = exWithSets.exercise
                        val mappedSets = exWithSets.sets.mapIndexed { sIdx, sEntity ->
                            ExerciseSetItem(
                                setNumber = if (sEntity.setNumber > 0) sEntity.setNumber else (sIdx + 1),
                                weightKg = (sEntity.weight ?: 0.0).toFloat(),
                                reps = sEntity.reps.toIntOrNull() ?: 0,
                                rir = sEntity.actualRir ?: 2, actualRirProvided = sEntity.actualRir != null,
                                completed = sEntity.done,
                                distanceMeters = sEntity.distanceMeters
                            )
                        }
                        com.example.model.LoggedExerciseDetail(
                            nameHu = ex.nameHu.ifBlank { "Gyakorlat" },
                            summaryPrescription = if (isOutdoorSession || ex.measurementType == "distance") "Kültéri aktivitás" else "${mappedSets.count { it.completed }}/${mappedSets.size} sorozat",
                            sets = mappedSets,
                            measurementType = ex.measurementType,
                            repUnit = ex.repUnit
                        )
                    }
                    val summaryNamesFromDb = savedInDb.exercises.filter { ex -> ex.sets.any { it.done } }
                        .map { it.exercise.nameHu }.joinToString(", ").ifBlank { workoutTitle }

                    val newEntry = LoggedWorkoutEntry(
                        id = finalWorkoutId,
                        dateLabelHu = dbW.started.take(16).replace('T', ' '),
                        titleHu = dbW.programName.ifBlank { workoutTitle },
                        durationMinutes = realDurationMins,
                        workTimeLabel = realWorkTimeLabel,
                        restTimeLabel = "—",
                        totalSets = if (dbW.completedSetsCount > 0) dbW.completedSetsCount else totalCompletedSets,
                        totalVolumeKg = dbW.totalVolumeKg.roundToInt(),
                        summaryExercisesHu = summaryNamesFromDb,
                        hasPr = dbW.hasPr226,
                        isOutdoorRun = isOutdoorSession,
                        routeKeys = savedInDb.exercises.flatMap { it.sets }.flatMap {
                            listOfNotNull(it.gpsRouteKey163, it.gpsRouteKey193)
                        }.filter(String::isNotBlank).distinct(),
                        sourceWatch = com.repforge.app.NativeSavedWearHealth.read(dbW) != null, sourceHealthConnect = !dbW.health240Json.isNullOrBlank(),
                        originDevice = dbW.originDevice,
                        savedStartIso = dbW.started,
                        savedFinishIso = dbW.finished,
                        photoCount = savedInDb.photos.size,
                        distanceKm = (savedInDb.exercises.flatMap { it.sets }.filter { it.done }
                            .sumOf { it.distanceMeters ?: 0.0 } / 1000.0).toFloat(),
                        paceLabel = savedInDb.exercises.flatMap { it.sets }.filter { it.done }.sumOf { it.distanceMeters ?: 0.0 }
                            .takeIf { it > 0 }?.let { meters ->
                                val pace = (dbW.durationSeconds / (meters / 1000.0)).toInt()
                                "%d:%02d /km".format(pace / 60, pace % 60)
                            } ?: "—",
                        exerciseDetails = dbDetails
                    )

                    withContext(Dispatchers.Main) {
                        _uiState.update { st ->
                            val updatedLogs = listOf(newEntry) + st.loggedWorkouts.filterNot { it.id == finalWorkoutId }
                            st.copy(
                                isWorkoutSessionActive = false,
                                activeWorkoutId = "",
                                workoutStartedEpochMs = 0L,
                                loggedWorkouts = updatedLogs,
                                prRecords = computePrRecordsFromWorkouts(updatedLogs),
                                weeklyGoalCompleted = updatedLogs.workoutsInCalendarWeekToDate(LocalDate.now()).size,
                                quickWorkoutQueue = emptyList(),
                                currentTab = MainTab.PROGRESS,
                                progressMainSection = 1,
                                currentSubScreen = SubScreen.NONE,
                                statusBannerMessage = "Edzés sikeresen lezárva és mentve ($totalCompletedSets kész sorozat • $realWorkTimeLabel)!"
                            )
                        }
                    }
                    loadMuscleLoadSummary(progressPeriodDays(_uiState.value.selectedProgressRangeIndex))
                } else {
                    withContext(Dispatchers.Main) {
                        _uiState.update { st ->
                            st.copy(statusBannerMessage = "Hiba: Az edzés mentése nem igazolható vissza a Room adatbázisból!")
                        }
                    }
                }
            } catch (e: Throwable) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                android.util.Log.e("TrainPilotViewModel", "Failed to finalize workout canonically", e)
                withContext(Dispatchers.Main) {
                    _uiState.update { st ->
                        st.copy(statusBannerMessage = "Hiba az edzés mentésekor: ${e.localizedMessage ?: "adatbázishiba"}")
                    }
                }
            } finally { _uiState.update { it.copy(isSavingWorkout = false) } }
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
                logWorkoutMuscleLoads = emptyList(),
                currentTab = MainTab.PROGRESS,
                progressMainSection = 1,
                currentSubScreen = SubScreen.LOG_WORKOUT_DETAIL,
                backStack = back
            )
        }
        loadWorkoutMuscleSummary(workoutId)
    }

    fun onLoggedWorkoutDeleted(id: String) {
        _uiState.update { state -> state.copy(
            loggedWorkouts = state.loggedWorkouts.filterNot { it.id == id },
            selectedLogWorkoutId = if (state.selectedLogWorkoutId == id) "" else state.selectedLogWorkoutId)
        }
        if (_uiState.value.currentSubScreen == SubScreen.LOG_WORKOUT_DETAIL && _uiState.value.selectedLogWorkoutId.isBlank())
            navigateBack()
        nativeContext?.let(::refreshAfterCloudSync)
    }

    fun updateLoggedWorkoutHealthSources(workoutId: String, wear: Boolean, healthConnect: Boolean) {
        _uiState.update { state -> state.copy(loggedWorkouts = state.loggedWorkouts.map { entry ->
            if (entry.id == workoutId) entry.copy(sourceWatch = wear, sourceHealthConnect = healthConnect) else entry
        }) }
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
    fun reloadPersonalRecords() {
        val db = nativeDatabase ?: return
        val scope = nativeScope ?: return
        val revision = ++personalRecordsRevision
        personalRecordsJob?.cancel()
        _uiState.update { it.copy(savedRecordsLoading = true, savedRecordsError = false) }
        personalRecordsJob = scope.launch(Dispatchers.IO) {
            try {
                val records = com.repforge.app.PersonalRecordCalculator226.savedExerciseBests(db)
                withContext(Dispatchers.Main) {
                    if (revision == personalRecordsRevision) _uiState.update {
                        it.copy(savedExerciseBests = records, savedRecordsLoading = false, savedRecordsError = false)
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (revision == personalRecordsRevision) _uiState.update { it.copy(savedRecordsLoading = false, savedRecordsError = true) }
            }
        }
    }

    fun selectProgressMainSection(section: Int) {
        _uiState.update { it.copy(progressMainSection = section) }
        if (section == 2) reloadPersonalRecords()
    }

    fun selectProgressExercise(name: String) {
        _uiState.update { it.copy(selectedProgressExercise = name) }
    }

    fun selectProgressRange(index: Int) {
        val range = index.coerceIn(0, 3)
        val days = progressPeriodDays(range)
        _uiState.update { it.copy(selectedProgressRangeIndex = range, selectedVolumeBarIndex = -1,
            muscleLoadPeriodDays = days, realMuscleLoads = emptyList()) }
        loadMuscleLoadSummary(days)
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

    fun openExerciseVideoModal(exerciseTitle: String, exerciseId: String? = null) {
        _uiState.update {
            it.copy(videoModalExerciseTitle = exerciseTitle, videoModalExerciseId = exerciseId)
        }
    }

    fun closeExerciseVideoModal() {
        _uiState.update {
            it.copy(videoModalExerciseTitle = null, videoModalExerciseId = null)
        }
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
                "weekly_steps" -> state.copy(expandedWeeklySteps = !state.expandedWeeklySteps)
                "weekly_workouts" -> state.copy(expandedWeeklyWorkouts = !state.expandedWeeklyWorkouts)
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
            _uiState.update {
                it.copy(
                    statusBannerMessage = tr(
                        "Érvénytelen testsúly: 20 és 350 kg között adható meg!",
                        "Invalid body weight: enter a value between 20 and 350 kg!",
                        "Ungültiges Körpergewicht: bitte zwischen 20 und 350 kg eingeben!",
                        "Greutate corporală invalidă: introdu o valoare între 20 și 350 kg!",
                        it.language
                    )
                )
            }
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
                    statusBannerMessage = tr(
                        "Testsúlymérés rögzítve ($dayKey): $weightKg kg",
                        "Body weight recorded ($dayKey): $weightKg kg",
                        "Körpergewicht erfasst ($dayKey): $weightKg kg",
                        "Greutate corporală înregistrată ($dayKey): $weightKg kg",
                        it.language
                    )
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
                val allWeights = db.bodyWeightDao().getAllWeights()
                val mappedLogs = mapBodyWeightEntitiesToLogItems(allWeights)
                val verified = db.bodyWeightDao().getWeightJournalSummary()
                val latestByDayKey = mappedLogs.lastOrNull()?.kg ?: verified.currentKg?.toFloat() ?: weightKg
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            currentBodyWeightKg = latestByDayKey,
                            bodyWeightLogs = mappedLogs,
                            showAddWeightDialog = false,
                            statusBannerMessage = tr(
                                "Testsúlymérés sikeresen elmentve ($dayKey): $weightKg kg",
                                "Body weight saved ($dayKey): $weightKg kg",
                                "Körpergewicht gespeichert ($dayKey): $weightKg kg",
                                "Greutate corporală salvată ($dayKey): $weightKg kg",
                                it.language
                            )
                        )
                    }
                }
            } catch (e: Throwable) {
                android.util.Log.e("TrainPilotViewModel", "Failed to save body weight to Room", e)
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(statusBannerMessage = tr(
                            "Nem sikerült elmenteni a testsúlymérést: ${e.localizedMessage ?: "adatbázishiba"}",
                            "Failed to save body weight: ${e.localizedMessage ?: "database error"}",
                            "Speichern des Körpergewichts fehlgeschlagen: ${e.localizedMessage ?: "Datenbankfehler"}",
                            "Salvarea greutății a eșuat: ${e.localizedMessage ?: "eroare bază de date"}",
                            it.language
                        ))
                    }
                }
            }
        }
    }

    fun deleteBodyWeightLog(id: String) {
        val db = nativeDatabase
        val scope = nativeScope
        if (db == null || scope == null) {
            _uiState.update {
                it.copy(
                    statusBannerMessage = tr(
                        "A testsúly-adatbázis nem érhető el.",
                        "Body weight database is unavailable.",
                        "Körpergewichts-Datenbank nicht verfügbar.",
                        "Baza de date pentru greutate nu este disponibilă.",
                        it.language
                    )
                )
            }
            return
        }
        scope.launch(Dispatchers.IO) {
            try {
                db.bodyWeightDao().deleteWeightById(id)
                db.bodyWeightDao().deleteByDateOrId(id)
                val allWeights = db.bodyWeightDao().getAllWeights()
                val mappedLogs = mapBodyWeightEntitiesToLogItems(allWeights)
                val summary = db.bodyWeightDao().getWeightJournalSummary()
                val latestKg = mappedLogs.lastOrNull()?.kg ?: summary.currentKg?.toFloat() ?: 0f
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            bodyWeightLogs = mappedLogs,
                            currentBodyWeightKg = latestKg,
                            statusBannerMessage = tr(
                                "Testsúlymérés törölve.",
                                "Body weight entry deleted.",
                                "Körpergewichtseintrag gelöscht.",
                                "Înregistrare greutate ștearsă.",
                                it.language
                            )
                        )
                    }
                }
            } catch (e: Throwable) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.update {
                    it.copy(
                        statusBannerMessage = tr(
                            "A testsúly törlése nem sikerült; a mérés megmaradt.",
                            "Failed to delete body weight entry.",
                            "Löschen des Körpergewichts fehlgeschlagen.",
                            "Ștergerea greutății a eșuat.",
                            it.language
                        )
                    )
                }
                android.util.Log.e("TrainPilotViewModel", "Failed to delete body weight log from Room", e)
            }
        }
    }

    fun togglePrimaryHealthSource() {
        _uiState.update {
            val next = !it.primaryHealthSourceSamsung
            it.copy(
                primaryHealthSourceSamsung = next,
                statusBannerMessage = if (next) {
                    tr("Elsődleges forrás: Samsung Health", "Primary source: Samsung Health", "Primärquelle: Samsung Health", "Sursă principală: Samsung Health", it.language)
                } else {
                    tr("Elsődleges forrás: Health Connect", "Primary source: Health Connect", "Primärquelle: Health Connect", "Sursă principală: Health Connect", it.language)
                }
            )
        }
    }

    private fun formatProviderLabelHu(channel: String, originPkg: String = ""): String {
        val base = when (channel) {
            HealthJournalStore.SAMSUNG -> "Samsung Health"
            HealthJournalStore.HC -> "Health Connect"
            HealthJournalStore.BLE -> "Wear OS / BLE óra"
            HealthJournalStore.LEGACY -> "Helyi egészségnapló"
            else -> "Nincs szinkronizált adat"
        }
        val cleanPkg = originPkg.trim()
        if (cleanPkg.isBlank()) return base
        val shortOrigin = when {
            cleanPkg.contains("shealth", ignoreCase = true) -> "Samsung Health"
            cleanPkg.contains("google.android.apps.fitness", ignoreCase = true) -> "Google Fit"
            cleanPkg.contains("wear", ignoreCase = true) -> "Wear OS"
            cleanPkg.contains("garmin", ignoreCase = true) -> "Garmin"
            else -> cleanPkg.substringAfterLast('.')
        }
        return if (shortOrigin.equals(base, ignoreCase = true)) base else "$base ($shortOrigin)"
    }

    fun updateHealthSyncStatus(busy: Boolean, partial: Boolean = false, failed: Boolean = false, message: String) {
        _uiState.update { it.copy(healthSyncBusy = busy, healthSyncPartial = partial,
            healthSyncFailed = failed, healthSyncMessage = message) }
    }

    fun reloadHealthJournal() {
        val ctx = nativeContext
        val scope = nativeScope
        if (ctx == null || scope == null) {
            _uiState.update {
                it.copy(statusBannerMessage = "Az egészségügyi szolgáltatások nem elérhetők.")
            }
            return
        }
        _uiState.update {
            it.copy(
                dailySteps = 0,
                currentHrBpm = 0,
                restingHrBpm = 0,
                maxHrBpm = 0,
                minHrBpm = 0,
                dailyPulseHistory = emptyList(), dailyPulseSampleCount = 0,
                activeCaloriesKcal = 0, totalCaloriesKcal = 0, dailyHealthDate = "", dailyHealthAvailableFields = emptySet(),
                sleepDurationLabel = "—",
                sleepHoursDecimal = "—",
                samsungEnergyScore = 0,
                trainPilotReadinessScore = 0,
                coachReadinessScore = 0,
                todayPulseReading = null,
                todayRecoveryReading = null,
                weeklyHealthDays = emptyList(),
                spo2Percent = 0f,
                vo2Max = 0f,
                hrvRmssdMs = 0f,
                bloodPressureLabel = "—",
                bloodGlucoseLabel = "—",
                respiratoryRateLabel = "—",
                dailyDistanceKm = 0f
            )
        }
        scope.launch(Dispatchers.IO) {
            try {
                val store = HealthJournalStore.get(ctx)
                val prefs = store.preferences()
                val provider = prefs.optString("provider", HealthJournalStore.HC)
                val hcDailySourceRaw = prefs.optString("hcDailySource", "auto")
                val dailySourceModeHu = when (hcDailySourceRaw) {
                    "auto", "" -> "Automatikus (legteljesebb egyedi forrás, duplikáció nélkül)"
                    "priority" -> "Prioritásos forrásválasztás"
                    com.repforge.app.HealthDailySource.SAMSUNG -> "Rögzített forrás: Samsung Health"
                    else -> "Rögzített csomag: $hcDailySourceRaw"
                }
                val generation = store.generation()
                val zone = ZoneId.systemDefault()
                val now = Instant.now()
                val today = now.atZone(zone).toLocalDate()
                val projectedProviderDays = mutableListOf<JSONObject>()
                val todayData = com.repforge.app.NativeHealthProviderData.day(store, provider, today)

                val todaySleepMin = com.repforge.app.NativeDailyHealthDetails.sleepMinutes(
                    todayData, today, java.time.ZoneId.systemDefault(), Instant.now()) ?: Double.NaN
                val todayPulseHistory = com.repforge.app.NativeDailyHealthDetails.pulseSeries(todayData)
                val availableFields = (setOf("steps", "activeCalories", "totalCalories", "averageHeartRate",
                    "restingHeartRate", "minHeartRate", "maxHeartRate", "heartRateSamples")
                    .filter { key -> todayData?.optDouble(key, Double.NaN)?.let { it.isFinite() && it >= 0 } == true } +
                    (if (todaySleepMin.isFinite() && todaySleepMin > 0) listOf("sleepMinutes") else emptyList()) +
                    (if (todayPulseHistory.isNotEmpty()) listOf("heartRateSeries") else emptyList())).toSet()
                val lastProviderSync = store.nativeSyncLastSuccess(provider)
                val stepsDbl = todayData?.optDouble("steps", Double.NaN) ?: Double.NaN
                val pulseDbl = todayData?.optDouble("averageHeartRate", Double.NaN) ?: Double.NaN
                val restingDbl = todayData?.optDouble("restingHeartRate", Double.NaN) ?: Double.NaN
                val maxHrDbl = todayData?.optDouble("maxHeartRate", Double.NaN) ?: Double.NaN
                val minHrDbl = todayData?.optDouble("minHeartRate", Double.NaN) ?: Double.NaN
                val caloriesDbl = todayData?.optDouble("activeCalories", Double.NaN) ?: Double.NaN
                val sleepMinDbl = todaySleepMin
                val hrvDbl = todayData?.optDouble("hrvRmssdMs", Double.NaN) ?: Double.NaN
                val sysDbl = todayData?.optDouble("bloodPressureSystolic", Double.NaN) ?: Double.NaN
                val diaDbl = todayData?.optDouble("bloodPressureDiastolic", Double.NaN) ?: Double.NaN
                val gluDbl = todayData?.optDouble("bloodGlucoseMmolL", Double.NaN) ?: Double.NaN
                val respDbl = todayData?.optDouble("respiratoryRate", Double.NaN) ?: Double.NaN
                val distMetersDbl = todayData?.optDouble("distanceMeters", Double.NaN) ?: Double.NaN
                val bodyFatDbl = todayData?.optDouble("bodyFatPercent", Double.NaN) ?: Double.NaN
                val energyDbl = todayData?.optDouble("energyScore", Double.NaN) ?: Double.NaN

                val bpLabel = if (sysDbl.isFinite() && sysDbl > 0 && diaDbl.isFinite() && diaDbl > 0) {
                    "${sysDbl.roundToInt()}/${diaDbl.roundToInt()} Hgmm"
                } else "—"
                val gluLabel = if (gluDbl.isFinite() && gluDbl > 0) {
                    String.format(java.util.Locale.US, "%.1f mmol/l", gluDbl).replace('.', ',')
                } else "—"
                val respLabel = if (respDbl.isFinite() && respDbl > 0) {
                    "${respDbl.roundToInt()} /perc"
                } else "—"
                val distKmToday = if (distMetersDbl.isFinite() && distMetersDbl > 0) (distMetersDbl / 1000.0).toFloat() else 0f

                // Build 7-day breakdown & source attribution
                val currentWorkouts = _uiState.value.loggedWorkouts
                val dayFmt = DateTimeFormatter.ofPattern("MM. dd. (EEE)", java.util.Locale("hu", "HU"))
                val weeklyDaysList = mutableListOf<WeeklyHealthDaySummary>()
                var sumSteps = 0
                var sumKcal = 0
                var sumDistMeters = 0.0
                var sumSleepMin = 0.0
                var sleepDaysCount = 0
                val discoveredSources = linkedSetOf<String>()

                for (offset in 13 downTo 0) {
                    val d = today.minusDays(offset.toLong())
                    val dayObj = runCatching { com.repforge.app.NativeHealthProviderData.day(store, provider, d) }.getOrNull()
                    // Never project another provider's cached row as the selected source.
                    val ownDay = dayObj?.takeIf { raw ->
                        raw.optString("provider").let { it.isBlank() || it == provider } &&
                            raw.optString("channel").let { it.isBlank() || it == provider }
                    }?.let { JSONObject(it.toString()).put("day", d.toString()) }
                    if (ownDay != null) projectedProviderDays.add(ownDay)
                    val dStepsRaw = dayObj?.optDouble("steps", Double.NaN) ?: Double.NaN
                    val stepsAvailable = dStepsRaw.isFinite() && dStepsRaw >= 0
                    val dSteps = if (stepsAvailable) dStepsRaw.toInt() else 0
                    val dKcal = dayObj?.optDouble("activeCalories", Double.NaN)?.takeIf { it.isFinite() && it >= 0 }?.toInt() ?: 0
                    val dDistM = dayObj?.optDouble("distanceMeters", Double.NaN)?.takeIf { it.isFinite() && it >= 0 } ?: 0.0
                    val dSleepMin = com.repforge.app.NativeDailyHealthDetails.sleepMinutes(
                        dayObj, d, java.time.ZoneId.systemDefault(), Instant.now()) ?: Double.NaN
                    val dSleepLabel = if (dSleepMin.isFinite()) {
                        String.format("%d ó %d p", (dSleepMin / 60).toInt(), (dSleepMin % 60).toInt())
                    } else "—"

                    val workoutsForDay = currentWorkouts.filter { w ->
                        w.localWorkoutDate() == d
                    }
                    val workoutMinForDay = workoutsForDay.sumOf { it.durationMinutes.coerceAtLeast(0) }
                    val workoutCountForDay = workoutsForDay.size

                    val hasHealthRow = dayObj != null && (dSteps > 0 || dKcal > 0 || dSleepMin.isFinite() ||
                        dayObj.optDouble("averageHeartRate", 0.0) > 0.0 || dayObj.optDouble("restingHeartRate", 0.0) > 0.0)
                    val stepOriginPkg = dayObj?.optString("stepsOrigin", "")?.ifBlank { dayObj.optString("activityOrigin", "") }.orEmpty()
                    val dayChannel = dayObj?.optString("channel", provider).orEmpty()
                    val sourceLabelHu = when {
                        hasHealthRow && workoutCountForDay > 0 -> "${formatProviderLabelHu(dayChannel, stepOriginPkg)} + Room edzésnapló"
                        hasHealthRow -> formatProviderLabelHu(dayChannel, stepOriginPkg)
                        workoutCountForDay > 0 -> "Room edzésnapló (aktív edzésintervallumok)"
                        else -> "Nincs rögzített adat"
                    }
                    if (offset <= 6) {
                    if (hasHealthRow) discoveredSources.add(formatProviderLabelHu(dayChannel, stepOriginPkg))
                    if (workoutCountForDay > 0) discoveredSources.add("Room edzésnapló (aktív edzésidő)")

                    sumSteps += dSteps
                    sumKcal += dKcal
                    sumDistMeters += dDistM
                    if (dSleepMin.isFinite() && dSleepMin > 0) {
                        sumSleepMin += dSleepMin
                        sleepDaysCount++
                    }

                    }

                    weeklyDaysList.add(
                        WeeklyHealthDaySummary(
                            dateIso = d.toString(),
                            dayLabelHu = if (d == today) "Ma (${d.format(dayFmt)})" else d.format(dayFmt),
                            steps = dSteps,
                            activeCaloriesKcal = dKcal,
                            sleepLabel = dSleepLabel,
                            workoutMinutes = workoutMinForDay,
                            workoutCount = workoutCountForDay,
                            stepsAvailable = stepsAvailable,
                            stepsSourceLabelHu = if (stepsAvailable) formatProviderLabelHu(dayChannel, stepOriginPkg) else "Nincs napi lépésadat",
                            sourceLabelHu = sourceLabelHu,
                            stepOriginPackage = stepOriginPkg,
                            pulse = if (provider == HealthJournalStore.HC || provider == HealthJournalStore.SAMSUNG)
                                HealthReadingsProjection.pulseDay(provider, d, ownDay, now, zone) else null
                        )
                    )
                }

                val avgSleepHu = if (sleepDaysCount > 0) {
                    val avgMin = (sumSleepMin / sleepDaysCount).roundToInt()
                    String.format("%d ó %d p", avgMin / 60, avgMin % 60)
                } else "—"

                val primaryProviderHu = if (provider == HealthJournalStore.SAMSUNG) "Samsung Health" else "Health Connect"
                val sourcesSummaryHu = if (discoveredSources.isNotEmpty()) {
                    discoveredSources.joinToString(" • ")
                } else {
                    "$primaryProviderHu (nincs szinkronizált mérés az elmúlt 7 napban)"
                }

                // Use the same selected-provider, day-validated projections for Home and Health.
                val todayPulse = if (provider == HealthJournalStore.HC || provider == HealthJournalStore.SAMSUNG)
                    HealthReadingsProjection.pulseDay(
                        provider, today, projectedProviderDays.firstOrNull { it.optString("day") == today.toString() },
                        now, zone) else null
                val roomCoachHistory = runCatching {
                    nativeDatabase?.workoutHistoryDao()?.getAllWorkoutsWithDetails()
                        ?.mapNotNull { entry -> runCatching { JSONObject(entry.workout.rawJson) }.getOrNull() }
                        ?: emptyList()
                }.getOrDefault(emptyList())
                val todayRecovery = if (provider == HealthJournalStore.HC || provider == HealthJournalStore.SAMSUNG)
                    HealthReadingsProjection.recoveryToday(provider, projectedProviderDays, roomCoachHistory, now, zone)
                    else null

                withContext(Dispatchers.Main) {
                    if (store.generation() != generation || store.preferences().optString("provider", HealthJournalStore.HC) != provider) return@withContext
                    _uiState.update { st ->
                        st.copy(
                            primaryHealthSourceSamsung = provider == HealthJournalStore.SAMSUNG,
                            dailyHealthDate = if (todayData != null) today.toString() else "",
                            dailyHealthAvailableFields = availableFields,
                            dailyHealthSourceLabel = formatProviderLabelHu(provider, todayData?.optString("activityOrigin").orEmpty()),
                            dailyHealthLastSyncIso = lastProviderSync,
                            totalCaloriesKcal = todayData?.optDouble("totalCalories", Double.NaN)?.takeIf { it.isFinite() && it >= 0 }?.toInt() ?: 0,
                            dailySteps = if (stepsDbl.isFinite() && stepsDbl >= 0) stepsDbl.toInt() else 0,
                            currentHrBpm = todayPulse?.averageBpm?.roundToInt() ?: 0,
                            restingHrBpm = todayPulse?.restingBpm?.roundToInt() ?: 0,
                            maxHrBpm = if (maxHrDbl.isFinite() && maxHrDbl > 0) maxHrDbl.toInt() else 0,
                            minHrBpm = if (minHrDbl.isFinite() && minHrDbl > 0) minHrDbl.toInt() else 0,
                            dailyPulseHistory = todayPulseHistory,
                            dailyPulseSampleCount = todayData?.optInt("heartRateSamples", 0)?.coerceAtLeast(0) ?: 0,
                            activeCaloriesKcal = if (caloriesDbl.isFinite() && caloriesDbl > 0) caloriesDbl.toInt() else 0,
                            sleepDurationLabel = if (sleepMinDbl.isFinite() && sleepMinDbl > 0) String.format("%d ó %d p", (sleepMinDbl / 60).toInt(), (sleepMinDbl % 60).toInt()) else "—",
                            sleepHoursDecimal = if (sleepMinDbl.isFinite() && sleepMinDbl > 0) "${((sleepMinDbl / 60.0) * 10.0).roundToInt() / 10.0} h" else "—",
                            samsungEnergyScore = todayRecovery?.samsungEnergy?.roundToInt() ?: 0,
                            spo2Percent = todayData?.optDouble("oxygenSaturationPercent", Double.NaN)?.takeIf { it.isFinite() && it > 0 }?.toFloat() ?: 0f,
                            vo2Max = todayData?.optDouble("vo2Max", Double.NaN)?.takeIf { it.isFinite() && it > 0 }?.toFloat() ?: 0f,
                            bodyFatPercent = if (bodyFatDbl.isFinite() && bodyFatDbl > 0) bodyFatDbl.toFloat() else st.bodyFatPercent,
                            hrvRmssdMs = if (hrvDbl.isFinite() && hrvDbl > 0) hrvDbl.toFloat() else 0f,
                            bloodPressureLabel = bpLabel,
                            bloodGlucoseLabel = gluLabel,
                            respiratoryRateLabel = respLabel,
                            dailyDistanceKm = distKmToday,
                            coachReadinessScore = todayRecovery?.coachScore?.roundToInt() ?: 0,
                            trainPilotReadinessScore = todayRecovery?.trainPilotEstimate ?: 0,
                            todayPulseReading = todayPulse,
                            todayRecoveryReading = todayRecovery,
                            weeklyHealthDays = weeklyDaysList.takeLast(7),
                            dailyActivityDays = weeklyDaysList,
                            weeklyTotalSteps = sumSteps,
                            weeklyTotalCaloriesKcal = sumKcal,
                            weeklyTotalDistanceKm = (sumDistMeters / 1000.0).toFloat(),
                            weeklyAvgSleepLabel = avgSleepHu,
                            weeklyDataSourcesSummary = sourcesSummaryHu,
                            dailySourceModeLabel = dailySourceModeHu,
                            statusBannerMessage = st.statusBannerMessage
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

    // Watch, Settings, Backup & Permissions Actions (P0-04: real Wear OS node & ACK check)
    fun toggleWatchControlMode(activity: android.app.Activity? = null) {
        val ctx = nativeContext ?: return
        val scope = nativeScope ?: return
        if (_uiState.value.watchControlPending || _uiState.value.isSavingWorkout) return
        scope.launch(Dispatchers.Main.immediate) {
            val active = ActiveWorkoutStore.current(ctx)?.takeIf { it.optBoolean("active") }
            if (active == null) { showBannerMessage("Nincs átadható aktív edzés."); return@launch }
            val id = active.optString("workoutId")
            if (active.optInt("ownershipProtocol193") != 1) {
                showBannerMessage("Ez a korábbi edzés nem támogatja az órás átadást; a telefonon biztonságosan folytatható.")
                return@launch
            }
            watchHandoffBusy = true
            suspend fun awaitOwner(owner: String) {
                kotlinx.coroutines.withTimeout(32_000) {
                    while (!com.repforge.app.NativeWatchHandoffReceipt.confirmed(
                            ActiveWorkoutStore.current(ctx), PhoneOwnership193.journal(ctx).read(), id, owner)) {
                        kotlinx.coroutines.delay(200)
                    }
                }
            }
            _uiState.update { it.copy(watchControlPending = true, statusBannerMessage = "Óra kapcsolat és tulajdonosi ACK ellenőrzése…") }
            try {
                if (com.repforge.app.NativePhoneReturnInstallation.requiresInstallation(active)) {
                    com.repforge.app.NativePhoneReturnInstallation.complete(ctx)
                    showBannerMessage("A telefon visszavette az edzést. Az edzés szünetel; folytatható vagy menthető.")
                    return@launch
                }
                val nodes = kotlinx.coroutines.withTimeout(5_000) {
                    com.google.android.gms.wearable.Wearable.getNodeClient(ctx).connectedNodes.await()
                }
                check(nodes.size == 1) { if (nodes.isEmpty()) "Nincs csatlakoztatott óra; a tulajdonos változatlan." else "Több óra csatlakozik; nem egyértelmű az átadás célja." }
                check(kotlinx.coroutines.withTimeout(5_000) {
                    com.repforge.app.NativeWearCompanion.reachable(ctx, nodes.single().id)
                }) { "A TrainPilot Wear OS alkalmazás nem érhető el. Telepítsd a telefonos APK-val párosított óraalkalmazást." }
                if (active.optString("timerOwner", "phone") == "phone") {
                    check(!com.repforge.app.DistanceTrackingService.snapshot(ctx).optBoolean("active")) {
                        "Aktív GPS-mérés közben nem adható át a vezérlés. Előbb fejezd be a GPS-mérést."
                    }
                    checkNotNull(activity) { "Az óra indításához látható telefonos felület szükséges." }
                    com.repforge.app.StudioWorkoutBridge.pauseForHandover(ctx, id)
                    kotlinx.coroutines.withTimeout(15_000) {
                        WearSyncRepository.startWatchMeasurementRemotely(activity, id, requestOwnership = true).getOrThrow()
                    }
                    // Remote activity launch is a request, not an ownership receipt.
                    awaitOwner("watch")
                } else {
                    val local = com.google.android.gms.wearable.Wearable.getNodeClient(ctx).localNode.await()
                    val journal = PhoneOwnership193.journal(ctx)
                    journal.recover()
                    journal.request(local.id, nodes.single().id, System.currentTimeMillis())
                    com.repforge.shared.OwnershipTransport193.send(ctx, journal)
                    awaitOwner("phone")
                    com.repforge.app.NativePhoneReturnInstallation.complete(ctx)
                }
                val current = checkNotNull(ActiveWorkoutStore.current(ctx))
                check(current.optString("workoutId") == id && !com.repforge.shared.WorkoutOwnership193.pending(current))
                _uiState.update { it.copy(watchConnected = true, watchName = nodes.single().displayName,
                    phoneControlsWorkout = current.optString("timerOwner", "phone") == "phone",
                    lastWatchSyncLabel = "Tulajdonosi ACK igazolva", statusBannerMessage = "Az edzésvezérlés átadása visszaigazolva; az edzés szünetel, explicit Folytatás szükséges.") }
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException && error !is kotlinx.coroutines.TimeoutCancellationException) throw error
                runCatching { PhoneOwnership193.journal(ctx).recover() }
                showBannerMessage(error.message ?: "Nem érkezett igazolt tulajdonosi ACK.")
            } finally {
                watchHandoffBusy = false
                val current = ActiveWorkoutStore.current(ctx)
                val pending = com.repforge.app.NativeWatchHandoffReceipt.pending(current, watchJournal())
                _uiState.update { it.copy(watchControlPending = pending,
                    phoneControlsWorkout = (current == null || current.optString("timerOwner", "phone") == "phone") && !pending && !com.repforge.app.NativePhoneReturnInstallation.requiresInstallation(current),
                    lastWatchSyncLabel = if (pending) "Átadás függőben; vezérlés zárolva" else it.lastWatchSyncLabel) }
            }
        }
    }

    private fun muscleItems(rows: List<com.repforge.app.data.db.ProgressMuscleSummary>) =
        NativeProgressQuery.muscle(rows, _uiState.value.language.name.lowercase()).map { m ->
            com.example.ui.components.MuscleLoadItem(
                id = m.muscleKey, nameHu = m.label,
                setsText = "${m.setsCount} sorozat", percentage = (m.loadPercentage * 100).toInt(),
                ratio = m.loadPercentage,
                levelLabelHu = when {
                    m.setsCount == 0 -> "Nincs terhelés"
                    m.loadPercentage >= 0.75f -> "Nagyobb terhelés"
                    m.loadPercentage >= 0.4f -> "Közepes terhelés"
                    else -> "Kisebb terhelés"
                }, topExercisesHu = emptyList(), exerciseIdToOpen = "", setsCount = m.setsCount)
        }

    fun loadMuscleLoadSummary(days: Int = progressPeriodDays(_uiState.value.selectedProgressRangeIndex)) {
        val db = nativeDatabase ?: return
        val scope = nativeScope ?: return
        val window = StudioProgressWindow(days)
        scope.launch(Dispatchers.IO) {
            try {
                val items = muscleItems(db.workoutHistoryDao().getProgressMuscleSummary(window.fromEpoch(), window.toEpoch()))
                withContext(Dispatchers.Main) {
                    _uiState.update { state ->
                        if (progressPeriodDays(state.selectedProgressRangeIndex) == days)
                            state.copy(realMuscleLoads = items, muscleLoadPeriodDays = days) else state
                    }
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { android.util.Log.e("TrainPilotViewModel", "Failed to load real muscle summary", e) }
        }
    }

    private fun loadWorkoutMuscleSummary(workoutId: String) {
        val db = nativeDatabase ?: return
        val scope = nativeScope ?: return
        scope.launch(Dispatchers.IO) {
            try {
                val items = muscleItems(db.workoutHistoryDao().getWorkoutMuscleSummary(workoutId))
                withContext(Dispatchers.Main) {
                    _uiState.update { state ->
                        if (state.selectedLogWorkoutId == workoutId) state.copy(logWorkoutMuscleLoads = items) else state
                    }
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { android.util.Log.e("TrainPilotViewModel", "Failed to load workout muscle summary", e) }
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
                val result = mgr.importBackupFromUri(uri, recoverInterrupted = false)
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

    fun refreshAfterCloudSync(context: Context) {
        val scope = nativeScope ?: return
        val db = nativeDatabase ?: return
        val store = nativeSettingsStore ?: return
        reloadNativeEnvironment(context, store, db, scope)
    }

    private fun studioModesText(hu: String, en: String, de: String, ro: String): String =
        when (_uiState.value.language) {
            AppLanguage.HU -> hu
            AppLanguage.EN -> en
            AppLanguage.DE -> de
            AppLanguage.RO -> ro
        }

    // Rest/sound/haptic settings are immediate, unlike the separately approved modes.
    // Serialize writes and preserve the user's interval work/round defaults.
    private val studioTimerWriteMutex = Mutex()

    private fun persistStudioTimerSettings() {
        val store = nativeSettingsStore ?: return
        val scope = nativeScope ?: return
        scope.launch(Dispatchers.IO) {
            try {
                studioTimerWriteMutex.withLock {
                    val saved = store.current()
                    val ui = _uiState.value
                    store.setTimerDefaults(
                        workSeconds = saved.workSeconds,
                        restSeconds = ui.defaultRestSec,
                        rounds = saved.rounds,
                        sound = ui.soundAlertEnabled,
                        vibration = ui.vibrationEnabled,
                        autoAdvance = saved.intervalAutoAdvance
                    )
                }
            } catch (error: kotlinx.coroutines.CancellationException) { throw error }
            catch (error: Exception) {
                _uiState.update { it.copy(statusBannerMessage = studioModesText(
                    "Az időzítő beállítása nem menthető.", "Could not save timer settings.",
                    "Timereinstellungen konnten nicht gespeichert werden.",
                    "Setările temporizatorului nu au putut fi salvate.")) }
            }
        }
    }

    fun toggleSoundAlert(enabled: Boolean) {
        _uiState.update { it.copy(soundAlertEnabled = enabled) }
        persistStudioTimerSettings()
    }

    fun cycleDefaultRestTimer() {
        _uiState.update {
            val next = when (it.defaultRestSec) {
                60 -> 90
                90 -> 120
                120 -> 180
                else -> 60
            }
            it.copy(defaultRestSec = next, statusBannerMessage = studioModesText(
                "Alapértelmezett pihenőidő: $next mp", "Default rest time: $next sec",
                "Standard-Pausenzeit: $next Sek.", "Timp implicit de pauză: $next sec"))
        }
        persistStudioTimerSettings()
    }

    private var pendingStudioMode: com.repforge.app.data.store.NativeTrainingModeChange.Preview? = null
    private var pendingStudioComparison: com.repforge.app.ui.settings.NativeTrainingModeComparison.Snapshot? = null
    private var studioModeGeneration = 0L

    fun toggleProgressionModal(show: Boolean) {
        studioModeGeneration++
        pendingStudioMode = null
        pendingStudioComparison = null
        _uiState.update {
            it.copy(showProgressionModal = show, progressionModeReview = null,
                progressionModeBusy = false)
        }
    }

    fun discardProgressionPreview() {
        studioModeGeneration++
        pendingStudioMode = null
        pendingStudioComparison = null
        _uiState.update { it.copy(progressionModeReview = null, progressionModeBusy = false) }
    }

    /** No writes in preview: approval uses exactly this prepared DataStore snapshot. */
    fun previewProgressionSettings(style: String, rule: String, scopeName: String, inherit: Boolean) {
        val store = nativeSettingsStore ?: return
        val scope = nativeScope ?: return
        if (_uiState.value.progressionModeBusy || !_uiState.value.showProgressionModal) return
        if (scopeName !in setOf("global", "program") || style !in setOf("normal", "hit") ||
            rule !in setOf("double", "linear", "rir") || (inherit && scopeName != "program")) return
        val token = ++studioModeGeneration
        pendingStudioMode = null
        pendingStudioComparison = null
        _uiState.update { it.copy(progressionModeBusy = true, progressionModeReview = null) }
        scope.launch {
            try {
                val saved = store.current()
                val mode = com.repforge.app.data.store.NativeTrainingModeChange
                val proposal = if (inherit) {
                    mode.prepareInherit(saved.trainingModes197Json, saved.activeProgramId, saved.activeProgramId)
                } else {
                    mode.prepare(saved.trainingModes197Json, saved.activeProgramId, scopeName,
                        if (scopeName == "program") saved.activeProgramId else null, style, rule)
                }
                val snapshot = withContext(Dispatchers.IO) {
                    com.repforge.app.ui.settings.NativeTrainingModeComparison.read(
                        requireNotNull(nativeContext), proposal)
                }
                if (studioModeGeneration != token || !_uiState.value.showProgressionModal) return@launch
                pendingStudioMode = proposal
                pendingStudioComparison = snapshot
                _uiState.update { it.copy(
                    progressionModeBusy = false,
                    progressionModeReview = StudioTrainingModeReview(
                        beforeStyle = proposal.before.trainingStyle,
                        beforeProgression = proposal.before.progressionMode,
                        afterStyle = proposal.after.trainingStyle,
                        afterProgression = proposal.after.progressionMode,
                        inheritGlobal = proposal.inheritGlobal,
                        programName = snapshot.programName,
                        rows = snapshot.rows.map { row ->
                            StudioTrainingModeRow(
                                day = row.day, exercise = row.exercise,
                                beforeSets = row.beforeSets, afterSets = row.afterSets,
                                beforeReps = row.beforeReps, afterReps = row.afterReps,
                                restSeconds = row.restSeconds,
                                beforeWeight = row.beforeWeight, afterWeight = row.afterWeight,
                                rir = row.rir)
                        })
                ) }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (studioModeGeneration == token) _uiState.update { it.copy(
                    progressionModeBusy = false,
                    statusBannerMessage = studioModesText(
                        "Nem sikerült elkészíteni a program-összehasonlítást.",
                        "Could not prepare the program comparison.",
                        "Programmvergleich konnte nicht erstellt werden.",
                        "Nu s-a putut pregăti comparația programului.")) }
            }
        }
    }

    fun approveProgressionSettings() {
        val proposal = pendingStudioMode ?: return
        val comparison = pendingStudioComparison ?: return
        val context = nativeContext ?: return
        val store = nativeSettingsStore ?: return
        val scope = nativeScope ?: return
        if (_uiState.value.progressionModeBusy || _uiState.value.progressionModeReview == null) return
        val token = ++studioModeGeneration
        pendingStudioMode = null
        pendingStudioComparison = null
        _uiState.update { it.copy(progressionModeBusy = true) }
        scope.launch {
            try {
                check(withContext(Dispatchers.IO) {
                    com.repforge.app.ui.settings.NativeTrainingModeComparison.isCurrent(
                        context, proposal, comparison)
                }) { "Az előnézetben használt program időközben megváltozott." }
                check(com.repforge.app.data.store.NativeTrainingModeChange.approve(store, proposal)) {
                    "A beállítás időközben módosult. Nyisd meg újra az előnézetet."
                }
                if (studioModeGeneration == token) _uiState.update { it.copy(
                    showProgressionModal = false, progressionModeReview = null,
                    progressionModeBusy = false, statusBannerMessage = studioModesText(
                        "Edzésmód elmentve.", "Training mode saved.",
                        "Trainingsmodus gespeichert.", "Mod de antrenament salvat.")) }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (studioModeGeneration == token) _uiState.update { it.copy(
                    progressionModeReview = null, progressionModeBusy = false,
                    statusBannerMessage = if (error.message?.contains("Finish the active workout") == true)
                        studioModesText(
                            "Az edzésmód csak az aktív edzés befejezése után módosítható.",
                            "Finish the active workout before changing training mode.",
                            "Trainingsmodus erst nach Abschluss des aktiven Trainings ändern.",
                            "Finalizează antrenamentul activ înainte de schimbarea modului.")
                    else studioModesText(
                        "Az edzésmód nem menthető. Nyisd meg újra az előnézetet.",
                        "Could not save the training mode. Reopen the preview.",
                        "Trainingsmodus konnte nicht gespeichert werden. Vorschau erneut öffnen.",
                        "Modul de antrenament nu a putut fi salvat. Redeschide previzualizarea.")) }
            }
        }
    }

    fun undoProgressionSettings(scopeName: String) {
        val store = nativeSettingsStore ?: return
        val scope = nativeScope ?: return
        if (scopeName !in setOf("global", "program") || _uiState.value.progressionModeBusy) return
        val token = ++studioModeGeneration
        pendingStudioMode = null
        pendingStudioComparison = null
        _uiState.update { it.copy(progressionModeBusy = true, progressionModeReview = null) }
        scope.launch {
            try {
                val saved = store.current()
                check(com.repforge.app.data.store.NativeTrainingModeChange.undo(store, scopeName,
                    if (scopeName == "program") saved.activeProgramId else null)) {
                    "Nincs visszavonható módváltás."
                }
                if (studioModeGeneration == token) _uiState.update { it.copy(
                    progressionModeBusy = false, showProgressionModal = false,
                    statusBannerMessage = studioModesText(
                        "Az előző edzésmód visszaállítva.", "Previous training mode restored.",
                        "Vorheriger Trainingsmodus wiederhergestellt.",
                        "Modul de antrenament anterior a fost restaurat.")) }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (studioModeGeneration == token) _uiState.update { it.copy(
                    progressionModeBusy = false,
                    statusBannerMessage = studioModesText(
                        "A módváltás nem vonható vissza.", "Could not undo training mode change.",
                        "Trainingsmoduswechsel konnte nicht rückgängig gemacht werden.",
                        "Schimbarea modului nu a putut fi anulată.")) }
            }
        }
    }

    fun toggleVibrationAlert(enabled: Boolean) {
        _uiState.update { it.copy(vibrationEnabled = enabled) }
        persistStudioTimerSettings()
    }

    fun selectBackupFormat(format: String) {
        _uiState.update { it.copy(selectedBackupFormat = format) }
    }

    fun toggleIncludeHealthInBackup(include: Boolean) {
        val ctx = nativeContext ?: return
        val store = nativeSettingsStore ?: return
        val scope = nativeScope ?: return
        if (_uiState.value.backupHealthConsentSaving) return
        _uiState.update { it.copy(backupHealthConsentSaving = true) }
        scope.launch(Dispatchers.IO) {
            try {
                NativeDataReplacement.mutate(ctx) { store.setHealthBackupConsent(include) }
                _uiState.update { it.copy(includeHealthInBackup = include) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _uiState.update { it.copy(statusBannerMessage = "A mentési hozzájárulás nem menthető. Próbáld újra.") } }
            finally { _uiState.update { it.copy(backupHealthConsentSaving = false) } }
        }
    }

    fun requestWatchDeviceInfo() {
        val context = nativeContext ?: return
        val scope = nativeScope ?: return
        // Keep the single in-flight native request and its node/nonce/timeout protections.
        if (_uiState.value.watchInfoLoading) return
        _uiState.update { it.copy(
            watchInfoLoading = true, watchInfoMessage = "Óraadatok lekérése…",
            watchInfoCardData = it.watchInfoCardData.copy(
                loading = true, status = "Óraadatok lekérése…", statusKind = WatchInfoStatus.IDLE)
        ) }
        scope.launch {
            try {
                val result = com.repforge.app.NativeWatchDeviceInfo.request(context)
                val data = result.data
                val battery = if (data.isNull("battery")) -1 else
                    com.repforge.shared.WatchDeviceInfo.battery(data.optInt("battery", -1)) ?: -1
                val time = java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                val verifiedCard = WatchInfoCardData.fromVerifiedReply(
                    nodeName = result.nodeName, reply = data,
                    connected = true, appAvailable = true,
                    status = "Óraadatok frissítve: $time",
                    statusKind = WatchInfoStatus.SUCCESS, lastSync = time)
                _uiState.update { it.copy(
                    watchInfoCardData = verifiedCard,
                    watchName = result.nodeName, watchConnected = true, watchAppAvailable = true,
                    watchBatteryPercent = battery, watchInfoMessage = "Óraadatok frissítve: $time",
                    watchDeviceDetails = listOf(data.optString("model"),
                        data.optString("os").takeIf { it.isNotBlank() }?.let { "Android $it" },
                        data.optString("appVersion").takeIf { it.isNotBlank() && it != "null" }?.let { "TrainPilot $it" })
                        .filterNotNull().filter(String::isNotBlank).joinToString(" • "),
                    lastWatchSyncLabel = "Óraadatok: $time") }
            } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
                val msg = "Az óra nem válaszolt. Ellenőrizd az órakapcsolatot és frissítsd az óraappot is."
                _uiState.update { it.copy(watchInfoMessage = msg,
                    watchInfoCardData = it.watchInfoCardData.copy(
                        status = msg, statusKind = WatchInfoStatus.TIMEOUT, loading = false)) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                val msg = error.message ?: "Az óraadatok nem kérhetők le."
                _uiState.update { it.copy(watchInfoMessage = msg,
                    watchInfoCardData = it.watchInfoCardData.copy(
                        status = msg, statusKind = WatchInfoStatus.ERROR, loading = false)) }
            } finally {
                _uiState.update { it.copy(watchInfoLoading = false,
                    watchInfoCardData = it.watchInfoCardData.copy(loading = false)) }
            }
        }
    }

    fun updatePlatformAccess(access: JSONObject, watchNames: List<String>, companionAvailable: Boolean = false) {
        val frame = nativeContext?.let { ActiveWorkoutStore.current(it) }
        val journal = watchJournal()
        val pending = watchHandoffBusy || com.repforge.app.NativeWatchHandoffReceipt.pending(frame, journal)
        val confirmed = frame?.let { com.repforge.app.NativeWatchHandoffReceipt.confirmed(
            it, journal, it.optString("workoutId"), it.optString("timerOwner")) } == true
        val companionReady = companionAvailable && watchNames.size == 1
        _uiState.update {
            val oldCard = it.watchInfoCardData
            val sameNode = companionReady && oldCard.name == watchNames.singleOrNull() && oldCard.appAvailable
            val nextCard = if (sameNode) oldCard.copy(connected = true, appAvailable = true)
                else WatchInfoCardData(
                    connected = watchNames.isNotEmpty(),
                    appAvailable = companionReady,
                    name = watchNames.singleOrNull()
                )
            it.copy(permissionGpsEnabled = access.optBoolean("gps"),
                permissionCentralLocEnabled = access.optBoolean("location"),
                permissionNotificationsEnabled = access.optBoolean("notifications"),
                permissionHealthConnectEnabled = access.optInt("healthGranted") > 0,
                healthGrantedCount = access.optInt("healthGranted"),
                healthPermissionCount = access.optInt("healthTotal"),
                watchControlPending = pending,
                phoneControlsWorkout = (frame == null || frame.optString("timerOwner", "phone") == "phone") &&
                    !pending && !com.repforge.app.NativePhoneReturnInstallation.requiresInstallation(frame),
                watchAppAvailable = companionReady,
                watchConnected = watchNames.isNotEmpty(),
                watchName = if (companionReady) watchNames.single() else "Nincs csatlakoztatott Wear OS óra",
                watchInfoCardData = nextCard,
                watchBatteryPercent = if (sameNode) it.watchBatteryPercent else -1,
                lastWatchSyncLabel = if (!companionReady) "Nincs órakapcsolat"
                    else if (confirmed) "Tulajdonosi ACK igazolva"
                    else if (it.lastWatchSyncLabel in setOf("—", "Nincs órakapcsolat", "Nincs óra kapcsolat"))
                        "Még nincs igazolt adatszinkron" else it.lastWatchSyncLabel)
        }
    }

    fun updateSamsungAccess(granted: Boolean) { _uiState.update { it.copy(permissionSamsungHealthEnabled = granted) } }

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
